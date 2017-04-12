package com.groupaxis.groupsuite.synchronizator.application.services

import akka.actor.SupervisorStrategy.{Restart, Resume, Stop}
import akka.actor.{Actor, OneForOneStrategy, Props}
import akka.pattern.ask
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Materializer, Supervision}
import akka.util.Timeout
import com.groupaxis.groupsuite.simulation.util.MessageUtil
import com.groupaxis.groupsuite.simulator.infrastructor.es.ESSwiftMessageWriteRepository
import com.groupaxis.groupsuite.simulator.infrastructor.jdbc.{JdbcMappingWriteRepository, JdbcMessageWriteRepository}
import com.groupaxis.groupsuite.simulator.write.domain.model.swift.msg.SwiftMessages._
import com.groupaxis.groupsuite.synchronizator.file.GPFileHelper
import com.groupaxis.groupsuite.synchronizator.parser.GPParserHelper
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import com.typesafe.config.Config
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

//object Test extends App {
//  def toStringJson(map : Map[String, String]) : String = {
//    import io.circe.syntax._
//    map.asJson.noSpaces
//  }
//  logger.debug(toStringJson(Map("senderAddress" -> "ou=funds,ou=live,o=parbitmm,o=swift")))
//}
object UpdateMessagesService {

  final val Name = "update-messages-service"

  def props(messageRepo: JdbcMessageWriteRepository, mappingRepo: JdbcMappingWriteRepository): Props = Props(classOf[UpdateMessagesService], messageRepo, mappingRepo)
}

class UpdateMessagesService(messageRepo: JdbcMessageWriteRepository,
                            mappingRepo: JdbcMappingWriteRepository) extends Actor with Logging {

  import com.groupaxis.groupsuite.simulator.write.domain.model.mapping.MappingForSystem.AMH_SYSTEM

  implicit val ec: ExecutionContext = context.dispatcher
  implicit val timeout = Timeout(5.seconds)
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

  //TODO: Move this to another actor to make the denormalization asynchronous
  val config: Config = context.system.settings.config

  import org.elasticsearch.common.settings.Settings

  val settings = Settings.settingsBuilder().put("cluster.name", config.getString("elastic.cluster.name")).build()
  val client: ElasticClient = ElasticClient.transport(settings, ElasticsearchClientUri(s"elasticsearch://" + config.getString("elastic.url")))
  val esMessageWriteRepository = context.actorOf(ESSwiftMessageWriteRepository.props(client), ESSwiftMessageWriteRepository.Name)
/*
Either[String, MappingsFound]
* */
  private def getItemMap(content: Option[String], forSystem:  String): String = {
    val msgType = GPFileHelper.findMsgType(content)
    val mappingsFromDB = mappingRepo.getMappingsBySystem(forSystem).fold(ex => Left(ex), mappings => Right(mappings.mappings))
    val mappings = MessageUtil.getMappings(msgType, mappingsFromDB)
    GPParserHelper.findMatches(msgType, content, mappings)
  }

  def receive: Receive = {
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
            val esResult = Await.result((esMessageWriteRepository ? UpdateSwiftMessageES(msgUpdated.swiftMsg.toES(itemMap))).mapTo[Either[String, SwiftMessageESInserted]], 5.seconds)
            esResult.fold(
              errorMsg => {
                logger.error(s"swift message was not updated into ES : $errorMsg")
                sender() ! esResult
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
  }

}