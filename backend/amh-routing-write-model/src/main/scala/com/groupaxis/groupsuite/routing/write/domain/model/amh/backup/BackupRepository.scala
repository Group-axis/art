package com.groupaxis.groupsuite.routing.write.domain.model.amh.backup

import com.groupaxis.groupsuite.persistence.driver.DBDriver
import com.groupaxis.groupsuite.routing.write.domain.model.amh.assignment.{AssignmentDAO, AssignmentRuleDAO}
import com.groupaxis.groupsuite.routing.write.domain.model.amh.backend.BackendDAO
import com.groupaxis.groupsuite.routing.write.domain.model.amh.distribution.copy.{DistributionCpyBackendDAO, DistributionCpyDAO, DistributionCpyRuleDAO}
import com.groupaxis.groupsuite.routing.write.domain.model.amh.feedback.distribution.copy.{FeedbackDtnCpyBackDAO, FeedbackDtnCpyDAO, FeedbackDtnCpyRuleDAO}
import com.groupaxis.groupsuite.routing.write.domain.model.amh.rule.AMHRuleDAO
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait BackupRepository extends AMHRuleDAO with FeedbackDtnCpyDAO with FeedbackDtnCpyRuleDAO
  with FeedbackDtnCpyBackDAO with DistributionCpyBackendDAO with DistributionCpyRuleDAO
  with DistributionCpyDAO with BackendDAO with AssignmentDAO with AssignmentRuleDAO
  with AMHBackupAssignmentDAO
  with AMHBackupBackendDAO
  with AMHBackupDistributionCpyDAO
  with BackupFeedbackDtnCpyDAO
  with AMHBackupRuleDAO
  with DBDriver
  with Logging{

  import driver.api._

  def initialize(implicit ec: ExecutionContext, database : com.groupaxis.groupsuite.persistence.datastore.jdbc.Database ) : Future[Try[Boolean]] = {

    val sentences = for {
      _ <- amhBackupAssignmentRules.delete
      _ <- amhBackupAssignments.delete
      _ <- amhBackupDistributionCpyRules.delete
      _ <- amhBackupDistributionCpyBackends.delete
      _ <- amhBackupDistributionCps.delete
      _ <- amhBackupFeedbackDtnCpyRules.delete
      _ <- amhBackupFeedbackDtnCpyBackends.delete
      _ <- amhBackupFeedbackDistributionCps.delete
      _ <- amhBackupRules.delete
      _ <- amhBackupBackends.delete
    } yield ()

    database.db.run(sentences.transactionally)
      .map(f => Success(true))
      .recover {
        case ex: java.sql.SQLException =>
          val msg = s"An error has occurred while deleting backup tables: ${ex.getLocalizedMessage}"
          logger.debug(msg)
          Failure(new Exception(msg))
      }

  }

  def fillBackup(implicit ec: ExecutionContext, database : com.groupaxis.groupsuite.persistence.datastore.jdbc.Database ) : Future[Try[Boolean]] = {

    val sentences = for {
      b <- amhBackends.result.flatMap(amhBackupBackends ++= _)
      r <- amhRules.result.flatMap(amhBackupRules ++= _)
      ar <- amhAssignmentRules.result.flatMap(amhBackupAssignmentRules ++= _)
      assign <- amhAssignments.result.flatMap(amhAssignments ++= _)
      dr <- amhDistributionCpyRules.result.flatMap(amhBackupDistributionCpyRules ++= _)
      db <- amhDistributionCpyBackends.result.flatMap(amhBackupDistributionCpyBackends ++= _)
      d <- amhDistributionCps.result.flatMap(amhBackupDistributionCps ++= _)
      fr <- amhFeedbackDtnCpyRules.result.flatMap(amhBackupFeedbackDtnCpyRules ++= _)
      fb <- amhFeedbackDtnCpyBackends.result.flatMap(amhBackupFeedbackDtnCpyBackends ++= _)
      f <- amhFeedbackDistributionCps.result.flatMap(amhBackupFeedbackDistributionCps ++= _)
    } yield ()

    database.db.run(sentences.transactionally)
      .map(f => Success(true))
      .recover {
        case ex: java.sql.SQLException =>
          val msg = s"An error has occurred while filling up backup tables: ${ex.getLocalizedMessage}"
          logger.debug(msg)
          Failure(new Exception(msg))
      }

  }

  def restoreFromBackup(implicit ec: ExecutionContext, database : com.groupaxis.groupsuite.persistence.datastore.jdbc.Database) : Future[Try[Boolean]] = {

//    def copy[E , B](t1 : TableQuery[E], t2 : TableQuery[B]) = t2.result
//        .map(r => r.asInstanceOf[E]).flatMap(t1 ++= _)

    val sentences = for {
      _ <- amhAssignmentRules.delete
      _ <- amhAssignments.delete
      _ <- amhDistributionCpyRules.delete
      _ <- amhDistributionCpyBackends.delete
      _ <- amhDistributionCps.delete
      _ <- amhFeedbackDtnCpyRules.delete
      _ <- amhFeedbackDtnCpyBackends.delete
      _ <- amhFeedbackDistributionCps.delete
      _ <- amhRules.delete
      _ <- amhBackends.delete
      b <- amhBackupBackends.result.flatMap(amhBackends ++= _)
      r <- amhBackupRules.result.flatMap(amhRules ++= _)
      ar <- amhBackupAssignmentRules.result.flatMap(amhAssignmentRules ++= _)
      assign <- amhAssignments.result.flatMap(amhAssignments ++= _)
      dr <- amhBackupDistributionCpyRules.result.flatMap(amhDistributionCpyRules ++= _)
      db <- amhBackupDistributionCpyBackends.result.flatMap(amhDistributionCpyBackends ++= _)
      d <- amhBackupDistributionCps.result.flatMap(amhDistributionCps ++= _)
      fr <- amhBackupFeedbackDtnCpyRules.result.flatMap(amhFeedbackDtnCpyRules ++= _)
      fb <- amhBackupFeedbackDtnCpyBackends.result.flatMap(amhFeedbackDtnCpyBackends ++= _)
      f <- amhBackupFeedbackDistributionCps.result.flatMap(amhFeedbackDistributionCps ++= _)
    } yield ()

    database.db.run(sentences.transactionally)
      .map(f => Success(true))
      .recover {
        case ex: java.sql.SQLException =>
          val msg = s"An error has occurred while restoring amh tables: ${ex.getLocalizedMessage}"
          logger.debug(msg)
          Failure(new Exception(msg))
      }

  }

}

object BackupRepository extends BackupRepository {

}
