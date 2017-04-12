package com.groupaxis.groupsuite.xml.parser.amh.writer.jdbc

import com.groupaxis.groupsuite.commons.persistence.jdbc.DatabaseService

import scala.concurrent.Await
import java.util.concurrent.TimeoutException

import com.groupaxis.groupsuite.routing.write.domain.model.amh.assignment.{AMHAssignmentEntity, AssignmentDAO}
import com.groupaxis.groupsuite.routing.write.domain.model.amh.backend.AMHBackendEntity
import org.apache.logging.log4j.scala.Logging

object JdbcAMHAssignmentRepository {

  def apply(dao: AssignmentDAO, databaseService: DatabaseService) { new JdbcAMHAssignmentRepository(dao,databaseService) }
}
case class AMHAssignmentsCreated(rules: Seq[AMHAssignmentEntity])

class JdbcAMHAssignmentRepository(dao : AssignmentDAO, databaseService: DatabaseService) extends Logging {

  import scala.concurrent.duration._
  import scala.concurrent.ExecutionContext.Implicits.global
  import databaseService._
//  import databaseService.driver.api._
  import slick.driver.PostgresDriver.api._

  def createAssignments(newAssignments: Seq[AMHAssignmentEntity]): Either[String, AMHAssignmentsCreated] = {
    try {
      val resp = Await.result(db.run(dao.amhAssignments ++= newAssignments).map { _ => Right(AMHAssignmentsCreated(newAssignments)) }, 15.seconds)
      logger.debug(" finish with " + resp.b)
      resp
    } catch {
      case timeEx: TimeoutException => Left(s" Error while creating backend DB not responding")
      case e: Exception             => Left(s" Error while creating backend msg[$e.getMessage]")
    }
  }

  def findAllAssignments: Option[Seq[AMHAssignmentEntity]] = {
    val result = Await.result(db.run(dao.amhAssignments.result), 10.seconds)
    Some(result)
  }

  def deleteAll(): Option[Int] = {
    val result = Await.result(db.run(dao.amhAssignments.delete), 10.seconds)
    Some(result)
  }
}