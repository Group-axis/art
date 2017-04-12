package com.groupaxis.groupsuite.xml.parser.amh.writer.jdbc

import java.util.concurrent.TimeoutException

import com.groupaxis.groupsuite.commons.persistence.jdbc.DatabaseService
import com.groupaxis.groupsuite.routing.write.domain.model.amh.distribution.copy.AMHDistributionCpyMessages.AMHDistributionCpyBackendsCreated
import com.groupaxis.groupsuite.routing.write.domain.model.amh.distribution.copy.{AMHDistributionCpyBackendEntity, DistributionCpyBackendDAO}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.Await
import scala.util.Try

object JdbcAMHDistributionCpyBackendRepository {

  def apply(dao: DistributionCpyBackendDAO, databaseService: DatabaseService) { new JdbcAMHDistributionCpyBackendRepository(dao,databaseService) }
}

class JdbcAMHDistributionCpyBackendRepository(dao : DistributionCpyBackendDAO, databaseService: DatabaseService) extends Logging {

  import databaseService._

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._
  import slick.driver.PostgresDriver.api._

  def createDistributionCpyBackends(newDistributionBackends: Seq[AMHDistributionCpyBackendEntity]): Either[String, AMHDistributionCpyBackendsCreated] = {
    try {
      val resp = Await.result(db.run(dao.amhDistributionCpyBackends ++= newDistributionBackends).map { _ => Right(AMHDistributionCpyBackendsCreated(newDistributionBackends)) }, 15.seconds)
      logger.debug(" finish with " + resp.b)
      resp
    } catch {
      case timeEx: TimeoutException => Left(s" Error while creating distribution copy backends DB not responding")
      case e: Exception             => Left(s" Error while creating distribution copy backends msg[$e.getMessage]")
    }
  }

  def findAllDistributionCpyBackends: Option[Seq[AMHDistributionCpyBackendEntity]] = {
    val result = Await.result(db.run(dao.amhDistributionCpyBackends.result), 10.seconds)
    Some(result)
  }

  def deleteDistributionBackendsByDistributionCode(code: String): Option[Int] = {
    val result = Await.result(db.run(dao.amhDistributionCpyBackends.filter(_.code === code).delete), 20.seconds)
    logger.debug("deleteDistributionBackendsByDistributionCode => " + result)
    Some(result)
  }

  def insertDistributionBackends(disitributionBackends: Seq[AMHDistributionCpyBackendEntity]): Either[String, AMHDistributionCpyBackendsCreated] = {
    try {
      val result = Await.result(db.run(dao.amhDistributionCpyBackends ++= disitributionBackends).map { _ => Right(AMHDistributionCpyBackendsCreated(disitributionBackends)) }, 20.seconds)
      logger.debug("insertDistributionBackends => " + result)
      result
    } catch {
      case timeEx: TimeoutException => Left(s" Error while creating amh distribution backends DB not responding")
      case e: Exception => Left(s" Error while creating amh distribution backends msg[$e.getMessage]")
    }
  }

  def findAllDistributionBackendsByEnvAndVersion(env : String, version : String): Option[Seq[AMHDistributionCpyBackendEntity]] = {
    val result = Await.result(db.run(dao.amhDistributionCpyBackends.filter( backend => backend.env === env && backend.version === version).result), 20.seconds)
    Some(result)
  }

  def findAllDistributionBackendsByCode(code : String): Seq[AMHDistributionCpyBackendEntity] = {
    Try(Await.result(db.run(dao.amhDistributionCpyBackends.filter( _.code === code).result), 20.seconds))
      .recover{
        case e => logger.debug(s"findAllDistributionBackendsByCode error: ${e.getMessage}");List()
      }
      .get
  }

  def deleteAll(): Option[Int] = {
    val result = Await.result(db.run(dao.amhDistributionCpyBackends.delete), 20.seconds)
    Some(result)
  }

}