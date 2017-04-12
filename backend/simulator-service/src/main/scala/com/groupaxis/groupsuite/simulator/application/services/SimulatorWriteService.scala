package com.groupaxis.groupsuite.simulator.application.services

import akka.actor.SupervisorStrategy.{Restart, Resume, Stop}
import akka.actor.{Actor, OneForOneStrategy, Props}
import akka.http.scaladsl.model.Multipart
import akka.pattern.ask
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Materializer, Supervision}
import akka.util.Timeout
import com.groupaxis.groupsuite.simulator.infrastructor.es.ESSwiftMessageWriteRepository
import com.groupaxis.groupsuite.simulator.infrastructor.jdbc.{JdbcJobWriteRepository, JdbcMappingWriteRepository, JdbcMessageWriteRepository}
import com.groupaxis.groupsuite.simulator.write.domain.model.job.Hit
import com.groupaxis.groupsuite.simulator.write.domain.model.job.JobMessages._
import com.groupaxis.groupsuite.simulator.write.domain.model.mapping.MappingMessages.MappingsFound
import com.groupaxis.groupsuite.simulator.write.domain.model.swift.msg.SwiftMessageEntity
import com.groupaxis.groupsuite.simulator.write.domain.model.swift.msg.SwiftMessages._
import com.groupaxis.groupsuite.synchronizator.date.GPDateHelper
import com.groupaxis.groupsuite.synchronizator.file.GPFileHelper
import com.groupaxis.groupsuite.synchronizator.parser.GPParserHelper
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import com.typesafe.config.Config
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

object SimulatorWriteService {

  final val Name = "simulator-write-service"

  def props(messageRepo: JdbcMessageWriteRepository, mappingRepo: JdbcMappingWriteRepository, jobRepo: JdbcJobWriteRepository): Props = Props(classOf[SimulatorWriteService], messageRepo, mappingRepo, jobRepo)
}

class SimulatorWriteService(messageRepo: JdbcMessageWriteRepository,
                            mappingRepo: JdbcMappingWriteRepository,
                            jobRepo: JdbcJobWriteRepository) extends Actor with Logging {

  import com.groupaxis.groupsuite.simulator.write.domain.model.mapping.MappingForSystem.AMH_SYSTEM

  implicit val ec: ExecutionContext = context.dispatcher
  val timeoutDuration = 30.minutes
  implicit val timeout = Timeout(timeoutDuration)

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1.minute) {
      case _: ArithmeticException => Resume
      case _: NullPointerException => Restart
      case _: IllegalArgumentException => Stop
      case e: Exception =>
        logger.warn("[SimulatorWriteService] Exception has been received, so restarting the actor " + e.getMessage)
        e.printStackTrace()
        Restart
    }

  val decider: Supervision.Decider = (ex) => Supervision.Resume
  implicit val mat: Materializer = ActorMaterializer(ActorMaterializerSettings(context.system).withSupervisionStrategy(decider))

  //TODO: Move this to another actor to make the de-normalization asynchronous
  val config: Config = context.system.settings.config

  import org.elasticsearch.common.settings.Settings
  val settings = Settings.settingsBuilder().put("cluster.name", config.getString("elastic.cluster.name")).build()
  val client: ElasticClient = ElasticClient.transport(settings, ElasticsearchClientUri(s"elasticsearch://" + config.getString("elastic.url")))
  val esMessageWriteRepository = context.actorOf(ESSwiftMessageWriteRepository.props(client), ESSwiftMessageWriteRepository.Name)

  private def getMappings(msgType: Int, forSystem : String): Map[String, String] = {
    val mappings: Either[String, MappingsFound] = mappingRepo.getMappingsBySystem(forSystem)

    //Mappings from DB
    mappings.fold(
      error => {
        logger.error(error)
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

  private def getItemMap(content: Option[String], forSystem : String): String = {
    val msgType = GPFileHelper.findMsgType(content)
    val mappings = getMappings(msgType, forSystem)
    GPParserHelper.findMatches2(msgType, content, mappings)
  }

  private def importMessages(message: SwiftMessageEntity, fileMap: scala.collection.mutable.HashMap[String, (Int, String)])
    : Either[String, SwiftMessagesCreated] = {
    val messages : Seq[SwiftMessageEntity] = fileMap //map to messages
      .map(item => {
      val (msgType, content) = item._2
      message.copy(fileName = Some(item._1), content = Some(content), messageType = msgType)
    }).toSeq

    logger.info(s"saving into DB ${messages.length} message(s).")
    messageRepo.createMessages(messages)

  }

  def insertNewMessage(message: SwiftMessageEntity): Either[String, SwiftMessageCreated] = {
    val result = messageRepo.createMessage(message)
    result.fold(
      errorMsg => {
        logger.info("Swift message creation failed with " + errorMsg)
        result
      },
      messageCreated => {
        try {
          logger.info(s"Swift message ${messageCreated.swiftMsg.fileName} created, now it will be inserted into ES")
          val itemMap = getItemMap(messageCreated.swiftMsg.content, AMH_SYSTEM)
          val esResult = Await.result((esMessageWriteRepository ? InsertSwiftMessageES(messageCreated.swiftMsg.toES(itemMap))).mapTo[Either[String, SwiftMessageESCreated]], timeoutDuration)
          esResult.fold(
            errorMsg => {
              logger.error(s"Swift message was not inserted into ES : $errorMsg")
              Left(errorMsg)
            },
            created => {
              logger.info("Swift message was created into ES ")
              result
            }
          )
        } catch {
          case e: Exception =>
            logger.error("Swift message was not inserted into ES : " + e.getMessage)
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

  def removeAllUserJobs(username: String, launcherId : Int) = {
    jobRepo.getJobsByUsernameAndLauncher(username, launcherId)
      .fold(
        error => logger.error(s" not jobs found $error"),
        response => {
          response.jobs.foreach(job => {
            jobRepo.deleteJob(job.id)
          })
        }
      )
  }

  def jobReceive: Receive = {
    case CreateJob(job, username, date) =>
      // deleting all old jobs
      job.user.foreach(username => removeAllUserJobs(username, job.jobLauncher))
      // inserting new job
      val dbResponse = jobRepo.createJob(job.toJobEntity)
      dbResponse.fold(
        error => logger.error(s"Error while creating job $error"),
        createdJob => logger.info(s"Job created! $createdJob")
      )
      sender() ! dbResponse
    case FindJobById(jobId) =>
      val dbResponse = jobRepo.getJobById(jobId)
      dbResponse.fold(
        error => logger.error(s"Error while looking for job $error"),
        jobFound => logger.info(s"Job found! $jobFound")
      )
      sender() ! dbResponse
    case FindAllJobsByUsername(username, status, threshold, launcherId) =>
      val dbResponse = jobRepo.getJobsByUsernameAndLauncher(username, launcherId)
      val fr = dbResponse.fold(
        error => {
          logger.error(s"Error while looking for $username 'jobs : $error")
          Left(error)
        },
        jobsFound => {
          logger.info(s"Jobs of user '$username' found! $jobsFound")
          val jobs = jobsFound.jobs
            //.filter(job => job.output.isDefined)
            .map(job => {
            val outputArray = GPParserHelper.toArrayResult(job.output)
            threshold match {
              case Some(limit) =>
                val (arraySize, updatedArray) = outputArray
                  .map(array =>
                    if (array.length > limit) (Some(array.length),Some(Array(s"out of threshold $threshold ")))
                    else (Some(array.length),Some(array))
                  ).getOrElse((None,None))

                job.copy(outputAsArray = updatedArray, numOfMessages = arraySize)
              case None =>
                job.copy(outputAsArray = outputArray)
            }
          })
          Right(JobsFound(jobs))
        }
      )
      sender() ! fr
    case FindAllJobs =>
      val dbResponse = jobRepo.getJobs
      dbResponse.fold(
        error => logger.error(s"Error while looking for jobs : $error"),
        jobsFound => logger.info(s"Jobs found! $jobsFound")
      )
      sender() ! dbResponse
    case CancelJob(jobId, username, date) =>
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
      logger.info(s"cancel job response $response")
      sender() ! response
    case ReExecuteJob(jobId, username, date) =>
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
      logger.info(s"reExecute job response $response")
      sender() ! response
  }

  def messageReceive: Receive = {
    case CreateSwiftMessage(swiftMessageEntityUpdate) =>
      logger.info(s" receiving create($swiftMessageEntityUpdate) on SimulatorWriteService")
      sender() ! insertNewMessage(swiftMessageEntityUpdate.toMessageEntity)
    case UpdateSwiftMessage(id, swiftMessageEntityUpdate) =>
      logger.info(s" receiving update($swiftMessageEntityUpdate) on SimulatorWriteService")
      val result = messageRepo.updateMessage(id, swiftMessageEntityUpdate)
      result.fold(
        errorMsg => {
          logger.error("Swift message update failed with " + errorMsg)
          sender() ! result
        },
        msgUpdated => {
          try {
            logger.info(s"Swift message $result updated, now it will be updated into ES")
            val itemMap = getItemMap(msgUpdated.swiftMsg.content, AMH_SYSTEM)
            val esResult = Await.result((esMessageWriteRepository ? UpdateSwiftMessageES(msgUpdated.swiftMsg.toES(itemMap))).mapTo[Either[String, SwiftMessageESInserted]], timeoutDuration)
            esResult.fold(
              errorMsg => {
                logger.error(s"swift message was not updated into ES : $errorMsg")
                sender() ! Left(s"swift message was not updated into ES : $errorMsg")
              }, {
                created => logger.info("swift message was updated into ES ")
                  sender() ! result
              }
            )
          } catch {
            case e: Exception =>
              logger.error("swift message was not updated into ES : " + e.getMessage)
              sender() ! Left("swift message was not updated into ES : " + e.getMessage)
          }
        })


    case FindSwiftMessageById(id) =>
      logger.info(s" receiving FindSwiftMessageById($id) on SimulatorWriteService")
      val result = messageRepo.getMessageById(id)
      logger.info(s"retrieving message $result")
      sender() ! result

    case FindAllSwiftMessages =>
      logger.info(s" receiving FindAllSwiftMessages on SimulatorWriteService")
      val result = messageRepo.getMessages
      logger.info(s"retrieving message $result")
      sender() ! result

    case DeleteSwiftMessagesByIds(ids, userId, group, time) =>
      logger.info(s" receiving DeleteSwiftMessageById(ids= $ids, group=$group) on SimulatorWriteService")
      val dbResponse = messageRepo.deleteMessages(ids, group)
      logger.info(s"$dbResponse response from DB")
      dbResponse.fold(
        error => {
          logger.error(s"An error has occurred while deleting messages: $error")
          sender() ! Left(error)
        },
        deletedCount => {
          try {
            logger.info(s"$deletedCount Message(s) successfully deleted, now trying to delete from ES")
            if (deletedCount == 0) {
              logger.warn(s"Nothing to delete from ES")
              sender() ! Right(SwiftMessagesDeleted(deletedCount))
            } else {
              var esResult: Either[String, SwiftMessagesESDeleted] = Left("")
              if (group.isEmpty) {
                esResult = Await.result((esMessageWriteRepository ? DeleteSwiftMessagesES(ids)).mapTo[Either[String, SwiftMessagesESDeleted]], timeoutDuration)
              } else {
                esResult = Await.result((esMessageWriteRepository ? DeleteSwiftGroupMessagesES(group)).mapTo[Either[String, SwiftMessagesESDeleted]], timeoutDuration)
              }
              esResult.fold(
                errorMsg => {
                  logger.error(s"swift messages were not deleted from ES : $errorMsg")
                  sender() ! Left(errorMsg)
                }, {
                  esDeletedCount => logger.info(s"$deletedCount swift messages were deleted from ES ")
                    sender() ! Right(SwiftMessagesDeleted(deletedCount))
                }
              )
            }
          } catch {
            case e: Exception =>
              logger.error("swift message was not updated into ES : " + e.getMessage)
              sender() ! Left("swift message was not updated into ES : " + e.getMessage)
          }
        }
      )
  }

  def fileReceive: Receive = {
    case fileData: Multipart.FormData =>
      val formData: Map[String, String] = Await.result(GPFileHelper.createTempFile("SML_", ".zip", fileData), timeoutDuration)
      val filePath = formData.get("file")
      val message = createSwiftMessageEntity(formData)
      val response = filePath.map(filePath => {
        logger.info(s" receiving messages within ($filePath) on SimulatorWriteService")
        val fileMap: scala.collection.mutable.HashMap[String, (Int, String)] = GPFileHelper.unZipAll(filePath)
//        val importMessageResponse: Either[String, Seq[SwiftMessageCreated]] = importMessages(message, fileMap)
        importMessages(message, fileMap) match {
          case Right(SwiftMessagesCreated(createdMessages)) =>
            logger.info(s" Message(s) saved into DB successfully.  A temp file was created on $filePath")
            logger.info(s"Parsing ${createdMessages.length} message(s) ..." )
            val childMessages = createdMessages.map(swiftMsg => {
              swiftMsg.toGroupES(getItemMap(swiftMsg.content, AMH_SYSTEM))
            })

            val groupMessageContent = Some("The file contains " + childMessages.size + " message(s)")
            val groupMessageES = message.toES("").copy(content = groupMessageContent, name = message.group, messages = childMessages, groupCount = childMessages.size).withGroupedId

            try {
              logger.info(s"Swift message {${groupMessageES.id} ${groupMessageES.content}} will be updated into ES")
              val esResult = Await.result((esMessageWriteRepository ? InsertSwiftGroupMessageES(groupMessageES)).mapTo[Either[String, SwiftMessageESCreated]], timeoutDuration)
              esResult.fold(error => Left(error), _ => Right(SwiftMessagesCreated(createdMessages)))
            } catch {
              case e: Exception =>
                logger.error("swift group message was not inserted into ES : " + e.getMessage)
                Left("swift group message was not inserted into ES : " + e.getMessage)
            }
          case Left(s) => Left(s)
        }

//        importMessageResponse.fold(
//          ex => importMessageResponse,
//          createdMessages => {
//
//          })
      }).getOrElse(Left(s"$filePath not processed"))

      sender() ! response
    case CreateCSVFile(jobId, username, date) =>
      logger.info(s" CreateCSVFile($jobId, $username) received.")
      val response = jobRepo.getJobById(jobId).fold(
        error => Left(error),
        jobFound => {
          val (tmpFilePath, tmpFile) = tmpFileInfo(username)
          logger.info(s"creating file $tmpFilePath")
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
      logger.info(s" create csv file response: $response")
      sender() ! response
    case CreateCSVFileWithHits(hits, username, date) =>
      logger.info(s" CreateCSVFileWithHits received.")
      val (tmpFilePath, tmpFile) = tmpFileInfo(username)
      val fileContent = csvContent(hits)
      logger.info(s"creating file $tmpFilePath")
      val fileWrote = GPFileHelper.writeFile(tmpFilePath, fileContent)
      val response = fileWrote.map(Left(_)).getOrElse(Right(CSVFileCreated(tmpFile)))
      logger.info(s" create csv file response: $response")
      sender() ! response
  }

  def csvContent(hits: Seq[Hit]): Option[String] = {
    val header = "File name;Message Reference;Selection Type;Assignment Sequence;Assignment Code;Backend Channel;Rule Sequence;Rule Code;Rule Expression"
    Some(hits.map(hit => hit.toLine)
      .foldLeft(header)((acc, curr) => {
        acc + "\n" + curr
      }))
  }

  def tmpFileInfo(username: Option[String], ext: String = ".csv") = {
    val workDirectory = config.getString("simulator.export.dir")
    val tmpFile = username.getOrElse("no_name") + "_" + GPDateHelper.currentDate + ext
    (workDirectory + "/" + tmpFile, tmpFile)
  }

  def receive = jobReceive orElse messageReceive orElse fileReceive
}