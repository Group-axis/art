package com.groupaxis.groupsuite.xxxxx.application.services

import akka.actor.SupervisorStrategy.{Restart, Resume, Stop}
import akka.actor.{Actor, ActorLogging, OneForOneStrategy, Props}
import akka.http.scaladsl.model.Multipart
import akka.pattern.ask
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Materializer, Supervision}
import akka.util.Timeout
import com.groupaxis.groupsuite.xxxxx.infrastructor.es.ESSwiftMessageWriteRepository
import com.groupaxis.groupsuite.xxxxx.infrastructor.jdbc.{JdbcJobWriteRepository, JdbcMappingWriteRepository, JdbcMessageWriteRepository}
import com.groupaxis.groupsuite.xxxxx.write.domain.model.job.Hit
import com.groupaxis.groupsuite.xxxxx.write.domain.model.job.JobMessages._
import com.groupaxis.groupsuite.xxxxx.write.domain.model.mapping.MappingMessages.MappingsFound
import com.groupaxis.groupsuite.xxxxx.write.domain.model.swift.msg.SwiftMessageEntity
import com.groupaxis.groupsuite.xxxxx.write.domain.model.swift.msg.SwiftMessages._
import com.groupaxis.groupsuite.tools.date.GPDateHelper
import com.groupaxis.groupsuite.tools.file.GPFileHelper
import com.groupaxis.groupsuite.tools.parser.GPParserHelper
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import com.typesafe.config.Config

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

object SimulatorWriteService {

  final val Name = "xxxxx-write-service"

  def props(messageRepo: JdbcMessageWriteRepository, mappingRepo: JdbcMappingWriteRepository, jobRepo: JdbcJobWriteRepository): Props = Props(classOf[SimulatorWriteService], messageRepo, mappingRepo, jobRepo)
}

class SimulatorWriteService(messageRepo: JdbcMessageWriteRepository,
                            mappingRepo: JdbcMappingWriteRepository,
                            jobRepo: JdbcJobWriteRepository) extends Actor with ActorLogging {

  implicit val ec: ExecutionContext = context.dispatcher
  implicit val timeout = Timeout(5.seconds)

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1.minute) {
      case _: ArithmeticException => Resume
      case _: NullPointerException => Restart
      case _: IllegalArgumentException => Stop
      case e: Exception =>
        log.warning("[SimulatorWriteService] Exception has been received, so restarting the actor " + e.getMessage)
        e.printStackTrace()
        Restart
    }

  val decider: Supervision.Decider = (ex) => Supervision.Resume
  implicit val mat: Materializer = ActorMaterializer(ActorMaterializerSettings(context.system).withSupervisionStrategy(decider))

  //TODO: Move this to another actor to make the denormalization asynchronous
  val config: Config = context.system.settings.config

  import org.elasticsearch.common.settings.Settings

  val settings = Settings.settingsBuilder().put("cluster.name", config.getString("elastic.cluster.name")).build()
  val client: ElasticClient = ElasticClient.transport(settings, ElasticsearchClientUri(s"elasticsearch://" + config.getString("elastic.url")))
  val esMessageWriteRepository = context.actorOf(ESSwiftMessageWriteRepository.props(client), ESSwiftMessageWriteRepository.Name)

  private def getMappings(msgType: Int): Map[String, String] = {
    val mappings: Either[String, MappingsFound] = mappingRepo.getMappings

    //Mappings from DB
    mappings.fold(
      error => {
        log.error(error)
        Map[String, String]()
      },
      mappingsFound => mappingsFound.mappings
        .foldLeft(Map[String, String]()) {
          (m, me) => m.updated(me.keyword, msgType match {
            case 1 => me.mxRegExp.getOrElse("no_value")
            case 2 => me.mtRegExp.getOrElse("no_value")
            case _ => "no_value"
          })
        }
    )


  }

  private def getItemMap(content: Option[String]): String = {
    val msgType = GPFileHelper.findMsgType(content)
    val mappings = getMappings(msgType)
    GPParserHelper.findMatches2(msgType, content, mappings)
  }

  private def importMessages(message: SwiftMessageEntity, fileMap: scala.collection.mutable.HashMap[String, (Int, String)]): Either[String, Seq[SwiftMessageCreated]] = {
    val futures = new ListBuffer[Future[Either[String, SwiftMessageCreated]]]()
    fileMap //map to messages
      .map(item => {
      val (msgType, content) = item._2
      message.copy(fileName = Some(item._1), content = Some(content), messageType = msgType)
    }) //map to future Either[String,SwiftMessageCreated]
      .map(message => Future {
      //      insertNewMessage(message)
      messageRepo.createMessage(message)
    })
      //TODO: Read about CanBuildFrom to convert Map to List
      .foreach(insert => futures += insert)

    val processFutures =
      for {
        list: ListBuffer[Either[String, SwiftMessageCreated]] <- Future.sequence(futures)
      } yield list


    val responses = Await.result(processFutures, 30.seconds)
    val response = responses.reduceLeft[Either[String, SwiftMessageCreated]](
      (acc, response) =>
        if (acc.isLeft) acc
        else response
    )

    response.fold(
      error => Left(error),
      created => {
        log.info(" all files inserted !!")
        Right(responses.map(resp => resp.right.get))
      })

  }

  def insertNewMessage(message: SwiftMessageEntity): Either[String, SwiftMessageCreated] = {
    val result = messageRepo.createMessage(message)
    result.fold(
      errorMsg => {
        log.info("Swift message creation failed with " + errorMsg)
        result
      },
      messageCreated => {
        try {
          log.info(s"Swift message $messageCreated created, now it will be inserted into ES")
          val itemMap = getItemMap(messageCreated.swiftMsg.content)
          val esResult = Await.result((esMessageWriteRepository ? InsertSwiftMessageES(messageCreated.swiftMsg.toES(itemMap))).mapTo[Either[String, SwiftMessageESCreated]], 5.seconds)
          esResult.fold(
            errorMsg => {
              log.error(s"Swift message was not inserted into ES : $errorMsg")
              Left(errorMsg)
            },
            created => {
              log.info("Swift message was created into ES ")
              result
            }
          )
        } catch {
          case e: Exception =>
            log.error("Swift message was not inserted into ES : " + e.getMessage)
            Left("Swift message was not inserted into ES : " + e.getMessage)
        }
      })
  }

  def createSwiftMessageEntity(formData: Map[String, String]): SwiftMessageEntity = {

    val username = formData.get("username")
    val creationDate = formData.get("creationDate")
    val group = formData.get("group")
    SwiftMessageEntity(-1, username, GPDateHelper.mapToDateTime(creationDate), None, None, group)
  }

  def removeAllUserJobs(username: String) = {
    jobRepo.getJobsByUsername(username)
      .fold(
        error => log.error(s" not jobs found $error"),
        response => {
          response.jobs.foreach(job => {
            jobRepo.deleteJob(job.id)
          })
        }
      )
  }


  def jobReceive: Receive = {
    case CreateJob(job) =>
      // deleting all old jobs
      job.user.foreach(username => removeAllUserJobs(username))
      // inserting new job
      val dbResponse = jobRepo.createJob(job.toJobEntity)
      dbResponse.fold(
        error => log.error(s"Error while creating job $error"),
        createdJob => log.info(s"Job created! $createdJob")
      )
      sender() ! dbResponse
    case FindJobById(jobId) =>
      val dbResponse = jobRepo.getJobById(jobId)
      dbResponse.fold(
        error => log.error(s"Error while looking for job $error"),
        jobFound => log.info(s"Job found! $jobFound")
      )
      sender() ! dbResponse
    case FindAllJobsByUsername(username, status) =>
      val dbResponse = jobRepo.getJobsByUsername(username, status)
      val fr = dbResponse.fold(
        error => {
          log.error(s"Error while looking for $username 'jobs : $error")
          Left(error)
        },
        jobsFound => {
          log.info(s"Jobs of user '$username' found! $jobsFound")
          val jobs = jobsFound.jobs
            //.filter(job => job.output.isDefined)
            .map(job => {
            val array = GPParserHelper.toArrayResult(job.output)
            job.copy(outputAsArray = array)
          })
          Right(JobsFound(jobs))
        }
      )
      sender() ! fr
    case FindAllJobs =>
      val dbResponse = jobRepo.getJobs
      dbResponse.fold(
        error => log.error(s"Error while looking for jobs : $error"),
        jobsFound => log.info(s"Jobs found! $jobsFound")
      )
      sender() ! dbResponse
    case CancelJob(jobId, username) =>
      val response = jobRepo.getJobById(jobId)
        .fold(
          error => {
            Left(error)
          },
          found => {
            jobRepo.updateStatus(jobId, Some(5))
              .fold(error => Left(error), jobUpdated => Right(OperationDone(jobUpdated.job)))
          }
        )
      log.info(s"cancel job response $response")
      sender() ! response
    case ReExecuteJob(jobId, username) =>
      val response = jobRepo.getJobById(jobId)
        .fold(
          error => {
            Left(error)
          },
          found => {
            jobRepo.updateStatus(jobId, Some(6))
              .fold(error => Left(error), jobUpdated => Right(OperationDone(jobUpdated.job)))
          }
        )
      log.info(s"reExecute job response $response")
      sender() ! response
  }

  def messageReceive: Receive = {
    case CreateSwiftMessage(swiftMessageEntityUpdate) =>
      log.info(s" receiving create($swiftMessageEntityUpdate) on SimulatorWriteService")
      sender() ! insertNewMessage(swiftMessageEntityUpdate.toMessageEntity)
    case UpdateSwiftMessage(id, swiftMessageEntityUpdate) =>
      log.info(s" receiving update($swiftMessageEntityUpdate) on SimulatorWriteService")
      val result = messageRepo.updateMessage(id, swiftMessageEntityUpdate)
      result.fold(
        errorMsg => {
          log.error("Swift message update failed with " + errorMsg)
          sender() ! result
        },
        msgUpdated => {
          try {
            log.info(s"Swift message $result updated, now it will be updated into ES")
            val itemMap = getItemMap(msgUpdated.swiftMsg.content)
            val esResult = Await.result((esMessageWriteRepository ? UpdateSwiftMessageES(msgUpdated.swiftMsg.toES(itemMap))).mapTo[Either[String, SwiftMessageESInserted]], 5.seconds)
            esResult.fold(
              errorMsg => {
                log.error(s"swift message was not updated into ES : $errorMsg")
                sender() ! esResult
              }, {
                created => log.info("swift message was updated into ES ")
                  sender() ! result
              }
            )
          } catch {
            case e: Exception =>
              log.error("swift message was not updated into ES : " + e.getMessage)
              sender() ! Left("swift message was not updated into ES : " + e.getMessage)
          }
        })


    case FindSwiftMessageById(id) =>
      log.info(s" receiving FindSwiftMessageById($id) on SimulatorWriteService")
      val result = messageRepo.getMessageById(id)
      log.info(s"retrieving message $result")
      sender() ! result

    case FindAllSwiftMessages =>
      log.info(s" receiving FindAllSwiftMessages on SimulatorWriteService")
      val result = messageRepo.getMessages
      log.info(s"retrieving message $result")
      sender() ! result

    case DeleteSwiftMessagesByIds(ids, userId, group, time) =>
      log.info(s" receiving DeleteSwiftMessageById($ids) on SimulatorWriteService")
      val dbResponse = messageRepo.deleteMessages(ids)
      log.info(s"$dbResponse response from DB")
      dbResponse.fold(
        error => {
          log.info(s"An error has occurred while deleting messages $error")
          sender() ! Left(error)
        },
        deletedCount => {
          try {
            log.info(s"$deletedCount Message(s) successfully deleted, now trying to delete from ES")
            if (deletedCount == 0) {
              log.info(s"Nothing to delete from ES")
              sender() ! Right(SwiftMessagesDeleted(deletedCount))
            } else {
              var esResult: Either[String, SwiftMessagesESDeleted] = Left("")
              if (group.isEmpty) {
                esResult = Await.result((esMessageWriteRepository ? DeleteSwiftMessagesES(ids)).mapTo[Either[String, SwiftMessagesESDeleted]], 5.seconds)
              } else {
                esResult = Await.result((esMessageWriteRepository ? DeleteSwiftGroupMessagesES(group)).mapTo[Either[String, SwiftMessagesESDeleted]], 5.seconds)
              }
              esResult.fold(
                errorMsg => {
                  log.error(s"swift messages were not deleted from ES : $errorMsg")
                  sender() ! Left(errorMsg)
                }, {
                  esDeletedCount => log.info(s"$deletedCount swift messages were deleted from ES ")
                    sender() ! Right(SwiftMessagesDeleted(deletedCount))
                }
              )
            }
          } catch {
            case e: Exception =>
              log.error("swift message was not updated into ES : " + e.getMessage)
              sender() ! Left("swift message was not updated into ES : " + e.getMessage)
          }
        }
      )
  }

  def fileReceive: Receive = {
    case fileData: Multipart.FormData =>
      val formData: Map[String, String] = Await.result(GPFileHelper.createTempFile("SML_", ".zip", fileData), 10.seconds)
      val filePath = formData.get("file")
      val message = createSwiftMessageEntity(formData)
      val response = filePath.map(filePath => {
        log.info(s" receiving messages within ($filePath) on SimulatorWriteService")
        val fileMap: scala.collection.mutable.HashMap[String, (Int, String)] = GPFileHelper.unZipAll(filePath)
        val importMessageResponse: Either[String, Seq[SwiftMessageCreated]] = importMessages(message, fileMap)
        importMessageResponse.fold(
          ex => importMessageResponse,
          createdMessages => {
            log.info(s" temp file ($filePath) processed ")
            val childMessages = createdMessages.map(msg => {
              log.info(s"Swift message ${msg.swiftMsg.fileName} updated, now it will be updated into ES")
              val itemMap = getItemMap(msg.swiftMsg.content)
              msg.swiftMsg.toES(itemMap)
            })
            val groupMessageContent = Some("The file contains " + childMessages.size + " message(s)")
            val groupMessageES = message.toES("").copy(content = groupMessageContent, name = message.group, messages = childMessages).withGroupedId

            try {
              val esResult = Await.result((esMessageWriteRepository ? InsertSwiftMessageES(groupMessageES)).mapTo[Either[String, SwiftMessageESCreated]], 5.seconds)
              esResult.fold(error => Left(error), _ => importMessageResponse)
            } catch {
              case e: Exception =>
                log.error("swift group message was not inserted into ES : " + e.getMessage)
                Left("swift group message was not inserted into ES : " + e.getMessage)
            }
          })
      }).getOrElse(Left(s"$filePath not processed"))

      sender() ! response
    case CreateCSVFile(jobId, username) =>
      log.info(s" CreateCSVFile($jobId, $username) received.")
      val response = jobRepo.getJobById(jobId).fold(
        error => Left(error),
        jobFound => {
          val (tmpFilePath, tmpFile) = tmpFileInfo(username)
          log.info(s"creating file $tmpFilePath")
          jobFound.job match {
            case Some(foundJob) =>
              foundJob.status match {
                case Some(3) if foundJob.output.isDefined =>
                  val error = GPFileHelper.writeFile(tmpFilePath, foundJob.output)
                  error.map(Left(_)).getOrElse(Right(CSVFileCreated(tmpFile)))
                case _ => Left(s"Job with id $jobId not in finished status")
              }
            case _ => Left(s"Job with id $jobId not found")
          }
        }
      )
      log.info(s" create csv file response: $response")
      sender() ! response
    case CreateCSVFileWithHits(hits, username) =>
      log.info(s" CreateCSVFileWithHits received.")
      val (tmpFilePath, tmpFile) = tmpFileInfo(username)
      val fileContent = csvContent(hits)
      log.info(s"creating file $tmpFilePath")
      val fileWrote = GPFileHelper.writeFile(tmpFilePath, fileContent)
      val response = fileWrote.map(Left(_)).getOrElse(Right(CSVFileCreated(tmpFile)))
      log.info(s" create csv file response: $response")
      sender() ! response
  }

  def csvContent(hits: Seq[Hit]): Option[String] = {
    val header = "File name;Message Reference;Selection Sequence;Selection Code;Rule Sequence;Rule Name;Rule Expression"
    Some(hits.map(hit => hit.toLine)
      .foldLeft(header)((acc, curr) => {
        acc + "\n" + curr
      }))
  }

  def tmpFileInfo(username: Option[String], ext: String = ".csv") = {
    val workDirectory = config.getString("xxxxx.export.dir")
    val tmpFile = username.getOrElse("no_name") + "_" + GPDateHelper.currentDate + ext
    (workDirectory + "/" + tmpFile, tmpFile)
  }

  def receive = jobReceive orElse messageReceive orElse fileReceive
}