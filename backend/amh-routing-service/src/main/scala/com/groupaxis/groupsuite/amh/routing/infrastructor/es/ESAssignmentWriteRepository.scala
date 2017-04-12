package com.groupaxis.groupsuite.amh.routing.infrastructor.es

import akka.actor.{Actor, Props}
import com.groupaxis.groupsuite.routing.amh.read.domain.model.view.assignment.overview.AMHAssignmentOverviewES
import com.groupaxis.groupsuite.routing.write.domain.global.messages.AMHRoutingGlobalMessages.{CreateOverviewCSVFile, OverviewCSVFileCreated}
import com.groupaxis.groupsuite.routing.write.domain.model.amh.rule.AMHRuleMessages.{SetRulesAsAssigned, UnAssignRule}
import com.groupaxis.groupsuite.synchronizator.date.GPDateHelper
import com.groupaxis.groupsuite.synchronizator.file.GPFileHelper
import com.groupaxis.groupsuite.xml.parser.amh.writer.es.{ESAMHAssignmentRepository, ESAMHDistributionCpyRepository, ESAMHFeedbackDtnCpyRepository, ESAMHRuleRepository}
import com.sksamuel.elastic4s.ElasticClient
import com.typesafe.config.Config
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object ESAssignmentWriteRepository {

  final val Name = "es-amh-assignments-write-repository"

  def props(esClient: ElasticClient): Props = Props(classOf[ESAssignmentWriteRepository], esClient)

}

class ESAssignmentWriteRepository(esClient: ElasticClient) extends Actor with Logging {
  val esRuleRepo: ESAMHRuleRepository = new ESAMHRuleRepository(esClient)

  val esAssignmentRepo: ESAMHAssignmentRepository = new ESAMHAssignmentRepository(esClient)
  val esDistributionRepo: ESAMHDistributionCpyRepository = new ESAMHDistributionCpyRepository(esClient)
  val esFeedDtnCopyRepo: ESAMHFeedbackDtnCpyRepository = new ESAMHFeedbackDtnCpyRepository(esClient)
  implicit val ec: ExecutionContext = context.dispatcher
  val config: Config = context.system.settings.config

  private def unAssignRule(ruleCode: String): Future[String] = {
    implicit val ec: ExecutionContext = context.dispatcher

    val process = esRuleRepo.findAssingmentsByRuleCode(ruleCode)
      .flatMap(assignsFound => {
        Future.sequence(
          assignsFound.map(assignFound => {
            assignFound._type match {
              case "assignments" => esAssignmentRepo.unassignRuleByCode(assignFound.code, ruleCode)
              case "distributionCopies" => esDistributionRepo.unassignRuleByCode(assignFound.code, ruleCode)
              case "feedbackDtnCopies" => esFeedDtnCopyRepo.unassignRuleByCode(assignFound.code, ruleCode)
            }
          })
        )
      })

    for {
      responses <- process
      allFound <- Future {
        if (responses.isEmpty) "OK" else responses.map(_.getId).reduce((acc, r) => if (acc equals "not_found") acc else r)
      }
      _ <- Future {
        logger.info(s" allFound = '$allFound' not_found ? ==> ${!allFound.equals("not_found")} ")
      }
      ruleDeleted <- esRuleRepo.deleteByCode(ruleCode) if !allFound.equals("not_found")
    } yield {
      logger.info(s"final response for ES synchronization $allFound, then rule deleted = $ruleDeleted")
      if (ruleDeleted) s"Deletion of rule $ruleCode O.K. :)" else s"Deletion of rule $ruleCode K.O. :/"
    }
  }

  def tmpFileInfo(username: Option[String], ext: String = ".csv") = {
    val workDirectory = config.getString("amh.export.dir")
    val tmpFile = username.getOrElse("no_name") + "_" + GPDateHelper.currentDate + ext
    (workDirectory + "/" + tmpFile, tmpFile)
  }

  def csvContent(assignments: Seq[AMHAssignmentOverviewES]): Option[String] = {
    val header = "Active;Assign Sequence;Assign Code;Backend Code;Rule Sequence;Rule Code;Rule Expressions"
    Some(assignments.map(assignment => assignment.toLine)
      .foldLeft(header)((acc, curr) => {
        acc + "\n" + curr
      }))
  }

  def createCSVFile(username: Option[String], assignmentOverview: Future[Seq[AMHAssignmentOverviewES]]): Future[OverviewCSVFileCreated] = {
    val (tmpFilePath, tmpFile) = tmpFileInfo(username)
    logger.info(s"creating file $tmpFilePath")
    assignmentOverview
      .map(assignments => csvContent(assignments))
      .map(fileContent => GPFileHelper.writeFile(tmpFilePath, fileContent))
      .map({
        case None =>
          logger.info(s" $tmpFile successfully created ")
          OverviewCSVFileCreated(Some(tmpFile))
        case Some(error) =>
          logger.warn(s"On CreateCSVFile($username) : $error")
          OverviewCSVFileCreated(None)
      })
      .recover { case ex: Throwable => OverviewCSVFileCreated(None) }
  }

  def receive: Receive = {
    case CreateOverviewCSVFile(assignmentType, username, date) =>
      logger.info(s" CreateAssignmentCSVFile($username) received.")
      val originalSender = sender()
      val createFile = assignmentType match {
        case 1 => createCSVFile(username, esAssignmentRepo.findAllAsOverview)
        case 2 => createCSVFile(username, esDistributionRepo.findAllAsOverview)
        case 4 => createCSVFile(username, esFeedDtnCopyRepo.findAllAsOverview)
       }

      createFile onComplete {
          case Success(fileCreated) => originalSender ! fileCreated
        }

    /*
    case CreateCSVFile(jobId, username) =>
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
    * */


    case SetRulesAsAssigned(addedRules) =>
      Thread.sleep(1500) //Wait for E-S to update assignment
      Future.sequence(for {rule <- addedRules} yield {
        esRuleRepo.updateAssignedFromAdd(rule.ruleCode)
      }) onComplete {
        case Success(updates) => updates.foreach(resp => logger.info(s" ${resp.getIndex}/${resp.getType}/${resp.getId}  assigned ? true"))
        case Failure(t) => logger.warn(s"On SetRulesAsAssigned($addedRules) E-S synchronization : ${t.getLocalizedMessage}")
      }

    case UnAssignRule(ruleCode, username, date) =>
      unAssignRule(ruleCode) onComplete {
        case Success(msg) => logger.info(msg)
        case Failure(t) => logger.warn(s"On unAssignRule($ruleCode) rule deletion E-S synchronization : ${t.getLocalizedMessage}")
      }
  }


}