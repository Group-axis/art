package com.groupaxis.groupsuite.amh.routing.infrastructor.es

import akka.actor.{Actor, Props}
import com.groupaxis.groupsuite.routing.write.domain.model.amh.rule.AMHRuleEntityES
import com.groupaxis.groupsuite.routing.write.domain.model.amh.rule.AMHRuleMessages._
import com.groupaxis.groupsuite.synchronizator.date.GPDateHelper
import com.groupaxis.groupsuite.synchronizator.file.GPFileHelper
import com.groupaxis.groupsuite.xml.parser.amh.writer.es.{ESAMHAssignmentRepository, ESAMHDistributionCpyRepository, ESAMHFeedbackDtnCpyRepository, ESAMHRuleRepository}
import com.sksamuel.elastic4s.ElasticClient
import com.typesafe.config.Config
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object ESRuleWriteRepository {

  final val Name = "es-amh-rule-write-repository"

  def props(esClient: ElasticClient): Props = Props(classOf[ESRuleWriteRepository], esClient)

}

class ESRuleWriteRepository(esClient: ElasticClient) extends Actor with Logging {
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

  def csvContent(rules: Seq[AMHRuleEntityES]): Option[String] = {
    val header = "Code;Expression"
    Some(rules.map(rule => rule.toLineOverview)
      .foldLeft(header)((acc, curr) => {
        acc + "\n" + curr
      }))
  }

  def createCSVFile(assignType: Option[Boolean], username: Option[String]): Future[RuleOverviewCSVFileCreated] = {
    val (tmpFilePath, tmpFile) = tmpFileInfo(username)
    logger.info(s"creating rule overview file $tmpFilePath")
    esRuleRepo.findAllAsOverview(assignType)
      .map(rules => csvContent(rules))
      .map(fileContent => GPFileHelper.writeFile(tmpFilePath, fileContent))
      .map({
        case None =>
          logger.info(s" $tmpFile successfully created ")
          RuleOverviewCSVFileCreated(Some(tmpFile))
        case Some(error) =>
          logger.warn(s"On CreateCSVFile($username) : $error")
          RuleOverviewCSVFileCreated(None)
      })
      .recover { case ex: Throwable => RuleOverviewCSVFileCreated(None) }
  }


  def receive: Receive = {
    case SetRulesAsUnassigned(removedRules) =>
      Thread.sleep(1500) //Wait for E-S to update assignment
      Future.sequence(for {rule <- removedRules} yield {
        esRuleRepo.updateAssignedFromRemove(rule.ruleCode)
      }) onComplete {
        case Success(updates) => updates.foreach(resp => logger.info(s" ${resp.getIndex}/${resp.getType}/${resp.getId} assigned ? false"))
        case Failure(t) => logger.warn(s"On SetRulesAsUnassigned($removedRules) E-S synchronization : ${t.getLocalizedMessage}")
      }
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

    case CreateRuleOverviewCSVFile(assignmentType, username, date) =>
      logger.info(s"received CreateRuleOverviewCSVFile($assignmentType, $username, $date) message")
      val originalSender = sender()
      createCSVFile(assignmentType, username) onComplete {
        case Success(csvFileCreatedResponse) =>
          originalSender ! csvFileCreatedResponse
      }
  }
}