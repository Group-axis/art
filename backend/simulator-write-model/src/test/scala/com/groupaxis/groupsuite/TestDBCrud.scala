package com.groupaxis.groupsuite

import java.util.Calendar

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import com.groupaxis.groupsuite.persistence.datastore.jdbc.Database
import com.groupaxis.groupsuite.simulator.infrastructor.es.ESSwiftMessageWriteRepository
import com.groupaxis.groupsuite.simulator.infrastructor.jdbc.{JdbcJobWriteRepository, JdbcMessageWriteRepository}
import com.groupaxis.groupsuite.simulator.write.domain.model.job.{JobDAO, JobEntity}
import com.groupaxis.groupsuite.simulator.write.domain.model.swift.msg.SwiftMessages.{FindAllSwiftMessages, InsertSwiftMessageES, SwiftMessageESCreated}
import com.groupaxis.groupsuite.simulator.write.domain.model.swift.msg.{SwiftMessageDAO, SwiftMessageES, SwiftMessageEntity}
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import org.apache.logging.log4j.scala.Logging
import org.joda.time.DateTime

import scala.collection.immutable.HashMap
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object esHelper {

  def client = {
    import org.elasticsearch.common.settings.Settings
    val settings = Settings.settingsBuilder().put("cluster.name", "groupsuite").build()
    val client = ElasticClient.transport(settings, ElasticsearchClientUri("elasticsearch://127.0.0.1:9300"))
    client
  }

}

object dbHelper {

  val database = new Database(slick.driver.PostgresDriver, "jdbc:postgresql://localhost:5432/postgres", "postgres", "postgres")

}

object TestJobDB extends App with Logging {
 // val jobDao: JobDAO = new JobDAO(slick.driver.PostgresDriver)
  val jobRepo: JdbcJobWriteRepository = new JdbcJobWriteRepository( dbHelper.database, 10.seconds)
  val now = org.joda.time.DateTime.now()
  val newJob = JobEntity(-1, Some("username"), Some(now), Some(now), Some(now), Some(2), Some(2),
    Some("fileName"), Some("comment1"), Some("params"), Some("good result!"))
  val resp = jobRepo.createJob(newJob)
  logger.debug("result: " + resp.fold(e => e, g => g))

}

object TestGetAllMessagesES extends App with Logging {

  import scala.concurrent._
  import ExecutionContext.Implicits.global

  val system = ActorSystem("TestSimulationDB")
  implicit val timeout: Timeout = 15.seconds
  val start = Calendar.getInstance.getTime.getTime
  val esMessageRepo = system.actorOf(ESSwiftMessageWriteRepository.props(esHelper.client), ESSwiftMessageWriteRepository.Name)

  //  val esResp = (esMessageRepo ? FindAllSwiftMessages).mapTo[Future[Seq[SwiftMessageES]]]
  val esResp = (esMessageRepo ? FindAllSwiftMessages).flatMap(f => f.asInstanceOf[Future[Seq[SwiftMessageES]]])
  logger.debug("just after the esMessageRepo call")

  @volatile var dummy: Any = _

  def timed[T](body: =>T): Double = {
    val start = System.nanoTime
    dummy = body
    val end = System.nanoTime
    ((end - start) / 1000) / 1000.0
  }

  def updateItemMap(message : SwiftMessageES, mappings : Map[String, String]) = {


    def updateMsgs(msgs : Seq[SwiftMessageES], mappings : Map[String, String]) : Seq[SwiftMessageES] = {
      if(msgs.length < 5) {
        msgs.map(msg => msg.copy(name = Some(msg.name.get+"-Mod")))
      } else {
        val half = msgs.length / 2
        val (left, right) = Parallel.parallel(updateMsgs(msgs.slice(0, half), mappings), updateMsgs(msgs.slice(half, msgs.length), mappings))
        left ++ right
      }
    }

    message.group.map(group => message.copy(messages = updateMsgs(message.messages, mappings))).getOrElse(message.copy(name = Some(message.name.get+"-Mod")))
  }

  esResp onComplete {
    case Success(ff) =>
      val partime = timed {
        logger.debug(" found " + ff.size + " messages")
        val updated = ff.map(msg => updateItemMap(msg,new HashMap[String, String]()))
        //      val allMsgs = ff.flatMap(msg => msg.group.map(g => msg.messages).getOrElse(Seq(msg)))
        logger.debug(" finally total is " + updated.size)
        updated.foreach(m => {
          logger.debug(m.name)
          if (m.group.isDefined) {
            m.messages.foreach(mm => logger.debug("     " + mm.name))
          }
        })
        system.terminate()
      }
      logger.debug(s"time $partime ms")
    case Failure(e) =>
      logger.debug(" second level " + e)
      system.terminate()
  }

  //  val r = esResp transform (f => f transform (ff => ff.size, ee=> ee), e => e)

  //  esResp onComplete {
  //    case Success(f) => f onComplete {
  //      case Success(ff) =>
  //        logger.debug(" found "+ff.size+" messages")
  //        system.terminate()
  //      case Failure(e) =>
  //        logger.debug(" second level "+e)
  //        system.terminate()
  //    }
  //    case Failure(e) => logger.debug("first level "+e)
  //  }
  Await.result(esResp, 60.second)

}

object TestDBCrud extends App with Logging {
  val system = ActorSystem("TestSimulationDB")
  implicit val timeout: Timeout = 15.seconds
  val start = Calendar.getInstance.getTime.getTime
  val esMessageRepo = system.actorOf(ESSwiftMessageWriteRepository.props(esHelper.client), ESSwiftMessageWriteRepository.Name)


  val messageDao: SwiftMessageDAO = SwiftMessageDAO
  val messageRepo: JdbcMessageWriteRepository = new JdbcMessageWriteRepository(messageDao, dbHelper.database, 10.seconds)
  //SwiftMessageEntity(id : Int, userId : Option[String], creationDate: Option[Date], fileName : Option[String], content : Option[String], group : Option[String]) {
  val message = SwiftMessageEntity(-1, Some("irach-ilish"), Some(new DateTime()), Some("file_name"), Some("text"), Some("group - axis :)"))
  //  val message  = SwiftMessageEntity(-1,Some("irach"),Some("21321351351"), Some("file_name"), Some("text"), Some("group - axis :)"))

  import scala.concurrent.ExecutionContext.Implicits.global

  insert(message).fold(
    error => logger.error("error " + error),
    resp => {
      val esResp = (esMessageRepo ? InsertSwiftMessageES(resp.swiftMsg.toES("params = {'xxx'}"))).mapTo[Either[String, SwiftMessageESCreated]]
      esResp onComplete {
        case Success(either) =>
          either.fold(
            error => logger.error(s"An error has occurred $error"),
            msg => logger.debug("OK ES inserted!")
          )
          system.terminate()

        case Failure(cause) =>
          logger.error(s"ES Insert failed $cause")
          system.terminate()

      }
      Await.result(esResp, 10.seconds)
    }
  )

  def insert(swiftMessageEntity: SwiftMessageEntity) = {
    val resp = messageRepo.createMessage(swiftMessageEntity)
    resp.fold(
      error => logger.error(s"error $error"),
      ok => logger.debug(s"O.K. $ok")
    )

    resp
  }
}
