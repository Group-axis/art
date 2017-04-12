package com.groupaxis.groupsuite.xml.parser.amh.writer.jdbc

import com.groupaxis.groupsuite.commons.persistence.jdbc.DatabaseService

import scala.concurrent.Await
import java.util.concurrent.TimeoutException

import com.groupaxis.groupsuite.routing.write.domain.model.amh.backend.{AMHBackendEntity, BackendDAO}
import org.apache.logging.log4j.scala.Logging

object JdbcAMHBackendRepository {

  def apply(dao: BackendDAO, databaseService: DatabaseService) { new JdbcAMHBackendRepository(dao, databaseService) }
}
case class AMHBackendsCreated(rules: Seq[AMHBackendEntity])

class JdbcAMHBackendRepository(dao : BackendDAO, databaseService : DatabaseService) extends Logging { //extends AMHBackendEntityTable

  import scala.concurrent.duration._
  import scala.concurrent.ExecutionContext.Implicits.global
  import databaseService._
  import databaseService.driver.api._

  def createBackends(newBackends: Seq[AMHBackendEntity]): Either[String, AMHBackendsCreated] = {
    try {
      val resp = Await.result(db.run(dao.amhBackends ++= newBackends).map { _ => Right(AMHBackendsCreated(newBackends)) }, 15.seconds)
      logger.debug(" finish with " + resp.b)
      resp
    } catch {
      case timeEx: TimeoutException => Left(s" Error while creating backend DB not responding")
      case e: Exception             => {
        e.printStackTrace()
        e.getLocalizedMessage()
        Left(s" Error while creating backend msg[$e.getMessage] - ${e.getCause} - ${e.getStackTrace}")
      }
    }
  }

  def findAllBackends: Option[Seq[AMHBackendEntity]] = {
    val result = Await.result(db.run(dao.amhBackends.result), 10.seconds)
    Some(result)
  }

  def deleteAll(): Option[Int] = {
    import slick.driver.PostgresDriver.api._
    val result = Await.result(db.run(dao.amhBackends.delete), 10.seconds)
    Some(result)
  }

}