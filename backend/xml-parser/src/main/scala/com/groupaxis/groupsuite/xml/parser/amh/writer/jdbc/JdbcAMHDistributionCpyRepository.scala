package com.groupaxis.groupsuite.xml.parser.amh.writer.jdbc

import java.util.concurrent.TimeoutException

import com.groupaxis.groupsuite.commons.persistence.jdbc.DatabaseService
import com.groupaxis.groupsuite.routing.write.domain.model.amh.distribution.copy.AMHDistributionCpyMessages.{AMHDistributionCpsCreated, AMHDistributionCpyCreated, AMHDistributionCpyFound, AMHDistributionCpyUpdated}
import com.groupaxis.groupsuite.routing.write.domain.model.amh.distribution.copy._
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.Await

object JdbcAMHDistributionCpyRepository {

  def apply(dao: DistributionCpyDAO, databaseService: DatabaseService) { new JdbcAMHDistributionCpyRepository(dao,databaseService) }
}

class JdbcAMHDistributionCpyRepository(dao : DistributionCpyDAO, databaseService: DatabaseService) extends Logging {

  import databaseService._

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._
//  import databaseService.driver.api._
  import slick.driver.PostgresDriver.api._

  def createDistributionCps(newDistributionCps: Seq[AMHDistributionCpyEntity]): Either[String, AMHDistributionCpsCreated] = {
    try {
      val resp = Await.result(db.run(dao.amhDistributionCps ++= newDistributionCps).map { _ => Right(AMHDistributionCpsCreated(newDistributionCps)) }, 15.seconds)
      logger.debug(" finish with " + resp.b)
      resp
    } catch {
      case timeEx: TimeoutException => Left(s" Error while creating distribution copies DB not responding")
      case e: Exception             => Left(s" Error while creating distribution copies msg[$e.getMessage]")
    }
  }

  def createDistributionCopy(newDistributionCopy: AMHDistributionCpyEntity): Either[String, AMHDistributionCpyCreated] = {
    try {
      val resp = Await.result(db.run(dao.amhDistributionCps += newDistributionCopy).map { _ => Right(AMHDistributionCpyCreated(newDistributionCopy)) }, 15.seconds)
      logger.debug(" finish with " + resp.b)
      resp
    } catch {
      case timeEx: TimeoutException => Left(s" Error while creating distribution copy DB not responding")
      case e: Exception             => Left(s" Error while creating distribution copy msg[$e.getMessage]")
    }
  }


  def getDistributionCopyByCode(code: String): Either[String, AMHDistributionCpyFound] = {
    try {
      val result: Option[AMHDistributionCpyEntity] = Await.result(db.run(dao.amhDistributionCps.filter(_.code === code).result), 5.seconds).headOption
      logger.debug("getDistributionCopyByCode => " + result)
      Right(AMHDistributionCpyFound(result))
    } catch {
      case timeEx: TimeoutException => Left(s" Error while looking for a distribution copy $code DB not responding")
      case e: Exception => Left(s" Error while looking for a distribution copy $code msg[$e.getMessage]")
    }
  }



  def updateDistributionCopy(code: String, distributionCopyUpdate: AMHDistributionCpyEntityUpdate, rules : Seq[AMHDistributionCpyRuleEntity], backends : Seq[AMHDistributionCpyBackendEntity]): Either[String, AMHDistributionCpyUpdated] = {
    val eitherResponse = getDistributionCopyByCode(code)

    if (eitherResponse.isLeft) {
      Left(eitherResponse.left.get)
    } else {
      val distributionCopyFound = eitherResponse.right.get
      distributionCopyFound.distributionCpy match {
        case Some(distributionCpy) =>
          try {
            val updatedDistrubtionCopy = distributionCpy.copy(rules = rules, backends = backends)
            val updatedAssignment = distributionCopyUpdate.merge(updatedDistrubtionCopy)
            Await.result(db.run(dao.amhDistributionCps.filter(_.code === code).update(updatedAssignment)), 10.seconds)
            Right(AMHDistributionCpyUpdated(updatedAssignment))
          } catch {
            case timeEx: TimeoutException => Left(s" Error while updating amh distribution copy DB not responding")
            case e: Exception => Left(s" Error while updating amh distribution copy msg[$e.getMessage]")
          }
        case None => Left("")
      }
    }
  }


  def findAllDistributionCps: Option[Seq[AMHDistributionCpyEntity]] = {
    val result = Await.result(db.run(dao.amhDistributionCps.result), 10.seconds)
    Some(result)
  }

  def findAllDistributionsByEnvAndVersion(env : String, version : String): Option[Seq[AMHDistributionCpyEntity]] = {
    val result = Await.result(db.run(dao.amhDistributionCps.filter( dist => dist.env === env && dist.version === version).result), 10.seconds)
    Some(result)
  }

  def deleteAll(): Option[Int] = {
    val result = Await.result(db.run(dao.amhDistributionCps.delete), 10.seconds)
    Some(result)
  }
}