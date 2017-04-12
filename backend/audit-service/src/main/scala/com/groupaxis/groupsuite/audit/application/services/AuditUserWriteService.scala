package com.groupaxis.groupsuite.audit.application.services

import akka.actor.SupervisorStrategy.{Restart, Resume, Stop}
import akka.actor.{Actor, OneForOneStrategy, Props}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Materializer, Supervision}
import akka.util.Timeout
import com.groupaxis.groupsuite.audit.infrastructor.diff.util.DiffHelper
import com.groupaxis.groupsuite.audit.infrastructor.jdbc.JdbcDifferenceWriteRepository
import com.groupaxis.groupsuite.authentication.write.domain.audit.messages.AuthenticationAuditMessages._
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import com.typesafe.config.Config
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

object AuditUserWriteService {

  final val Name = "audit-user-write-service"

  def props(diffRepo: JdbcDifferenceWriteRepository): Props = Props(classOf[AuditUserWriteService], diffRepo)
}

class AuditUserWriteService(diffRepo: JdbcDifferenceWriteRepository) extends Actor with Logging {

  implicit val ec: ExecutionContext = context.dispatcher
  implicit val timeout = Timeout(5.seconds)

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1.minute) {
      case _: ArithmeticException => Resume
      case _: NullPointerException => Restart
      case _: IllegalArgumentException => Stop
      case e: Exception =>
        logger.warn("[AuditUserWriteService] Exception has been received, so restarting the actor " + e.getMessage)
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
  //  val esAuditWriteRepository = context.actorOf(ESSwiftMessageWriteRepository.props(client), ESSwiftMessageWriteRepository.Name)

  //  private def getMappings(msgType: Int): Map[String, String] = {
  //    val mappings: Either[String, MappingsFound] = mappingRepo.getMappings
  //
  //    //Mappings from DB
  //    mappings.fold(
  //      error => {
  //        logger.error(error)
  //        Map[String, String]()
  //      },
  //      mappingsFound => mappingsFound.mappings
  //        .foldLeft(Map[String, String]()) {
  //          (m, me) => m.updated(me.keyword, msgType match {
  //            case 1 => me.mxRegExp.getOrElse("no_value")
  //            case 2 => me.mtRegExp.getOrElse("no_value")
  //            case _ => "no_value"
  //          })
  //        }
  //    )
  //
  //
  //  }
  //
  //  private def getItemMap(content: Option[String]): String = {
  //    val msgType = GPFileHelper.findMsgType(content)
  //    val mappings = getMappings(msgType)
  //    GPParserHelper.findMatches2(msgType, content, mappings)
  //  }
  //
  //  private def importMessages(message: SwiftMessageEntity, fileMap: scala.collection.mutable.HashMap[String, (Int, String)]): Either[String, Seq[SwiftMessageCreated]] = {
  //    val futures = new ListBuffer[Future[Either[String, SwiftMessageCreated]]]()
  //    fileMap //map to messages
  //      .map(item => {
  //      val (msgType, content) = item._2
  //      message.copy(fileName = Some(item._1), content = Some(content), messageType = msgType)
  //    }) //map to future Either[String,SwiftMessageCreated]
  //      .map(message => Future {
  //      //      insertNewMessage(message)
  //      messageRepo.createMessage(message)
  //    })
  //      //TODO: Read about CanBuildFrom to convert Map to List
  //      .foreach(insert => futures += insert)
  //
  //    val processFutures =
  //      for {
  //        list: ListBuffer[Either[String, SwiftMessageCreated]] <- Future.sequence(futures)
  //      } yield list
  //
  //
  //    val responses = Await.result(processFutures, 30.seconds)
  //    val response = responses.reduceLeft[Either[String, SwiftMessageCreated]](
  //      (acc, response) =>
  //        if (acc.isLeft) acc
  //        else response
  //    )
  //
  //    response.fold(
  //      error => Left(error),
  //      created => {
  //        logger.info(" all files inserted !!")
  //        Right(responses.map(resp => resp.right.get))
  //      })
  //
  //  }
  //
  //  def insertNewMessage(message: SwiftMessageEntity): Either[String, SwiftMessageCreated] = {
  //    val result = messageRepo.createMessage(message)
  //    result.fold(
  //      errorMsg => {
  //        logger.info("Swift message creation failed with " + errorMsg)
  //        result
  //      },
  //      messageCreated => {
  //        try {
  //          logger.info(s"Swift message $messageCreated created, now it will be inserted into ES")
  //          val itemMap = getItemMap(messageCreated.swiftMsg.content)
  //          val esResult = Await.result((esMessageWriteRepository ? InsertSwiftMessageES(messageCreated.swiftMsg.toES(itemMap))).mapTo[Either[String, SwiftMessageESCreated]], 5.seconds)
  //          esResult.fold(
  //            errorMsg => {
  //              logger.error(s"Swift message was not inserted into ES : $errorMsg")
  //              Left(errorMsg)
  //            },
  //            created => {
  //              logger.info("Swift message was created into ES ")
  //              result
  //            }
  //          )
  //        } catch {
  //          case e: Exception =>
  //            logger.error("Swift message was not inserted into ES : " + e.getMessage)
  //            Left("Swift message was not inserted into ES : " + e.getMessage)
  //        }
  //      })
  //  }
  //
  //  def createSwiftMessageEntity(formData: Map[String, String]): SwiftMessageEntity = {
  //
  //    val username = formData.get("username")
  //    val creationDate = formData.get("creationDate")
  //    val group = formData.get("group")
  //    SwiftMessageEntity(-1, username, GPDateHelper.mapToDateTime(creationDate), None, None, group)
  //  }
  //
  //  def removeAllUserJobs(username: String) = {
  //    jobRepo.getJobsByUsername(username)
  //      .fold(
  //        error => logger.error(s" not jobs found $error"),
  //        response => {
  //          response.jobs.foreach(job => {
  //            jobRepo.deleteJob(job.id)
  //          })
  //        }
  //      )
  //  }

  private def userCreationAudit(createUser: CreateUser): Future[AuthenticationAuditResponse] = {

    diffRepo.createDifference(DiffHelper.toCreateUserDiff(createUser))
      .map(_=>AuthenticationCreationDone())
      .recover({
        case t : Throwable => AuthenticationCreationFailed(t.getLocalizedMessage)
      })
  }

  def receive: Receive = {
    case createUser: CreateUser =>
      userCreationAudit(createUser) onSuccess { case response =>
        sender() ! response
      }
    case UpdateUser(date, oldUserEntity, oldUserProfiles, newUserEntity, newUserProfiles) =>
    case DeleteUser(date, deleteUserEntity, deleteUserDetailEntity) =>
    case CreateBlockUser(date, blockUserEntity, newUserDetailEntity) =>
    case CreateUnblockUser(date, unblockUserEntity, newUserDetailEntity) =>
    case CreateLoginUser(date, connUserEntity, newUserDetailEntity) =>
    case CreateLogoutUser(date, discConnUserEntity, newUserDetailEntity) =>
    case CreateResetPassword(date, resetUserEntity) =>
    case CreateFailedConnUser(date, failedUser) =>
    case "Ping" =>
      logger.info("ping received!!!, sending pong")
      sender() ! "Pong"
    case "AreYouThere" =>
      logger.info("Are you there received!!!, sending I am here")
      sender() ! "IamHere"
  }

}