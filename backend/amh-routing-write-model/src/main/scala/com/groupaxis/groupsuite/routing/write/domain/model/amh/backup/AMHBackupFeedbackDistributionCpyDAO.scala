package com.groupaxis.groupsuite.routing.write.domain.model.amh.backup


import com.groupaxis.groupsuite.persistence.driver.DBDriver
import com.groupaxis.groupsuite.persistence.util.TableAudit
import com.groupaxis.groupsuite.routing.write.domain.model.amh.feedback.distribution.copy.{AMHFeedbackDistributionCpyBackendEntity, AMHFeedbackDistributionCpyEntity, AMHFeedbackDistributionCpyRuleEntity}
import slick.driver.JdbcProfile

trait BackupFeedbackDtnCpyDAO extends DBDriver {

  import driver.api._

  protected class AMHBackupFeedbackDistributionCps(tag: Tag) extends Table[AMHFeedbackDistributionCpyEntity](tag, "BAK_AMH_FB_DT_CP_SEL_TAB") with TableAudit {
    def code = column[String]("CODE", O.PrimaryKey)
    def sequence = column[Long]("SEQUENCENUMBER", O.PrimaryKey)
    def env = column[String]("ENV", O.PrimaryKey)
    def version = column[String]("VERSION", O.PrimaryKey)
    def fileName = column[String]("FILENAME")
    def active = column[Option[String]]("ACTIVE")
    def dataOwner = column[Option[String]]("DATAOWNER")
    def lockCode = column[Option[String]]("LOCKCODE")
    def description = column[Option[String]]("DESCRIPTION")
    def selectionGroup = column[Option[String]]("SELECTIONGROUP")
    def layoutTemplate = column[Option[String]]("PRINTLAYOUTTEMPLATE")
    def copies = column[Option[Long]]("NBOFCOPIES")
    def name = column[Option[String]]("NAME")

    private type AMHFeedbackDistributionCpyEntityTupleType = (
      String, Long, String,
        String, String,  Option[String], Option[String], Option[String],
        Option[String], Option[String], Option[String], Option[Long], Option[String]
      , AuditEntityTupleType)

    private val AMHFeedbackDistributionShapedValue = (
      code,
      sequence,
      env,
      version,
      fileName,
      active,
      dataOwner,
      lockCode,
      description,
      selectionGroup,
      layoutTemplate,
      copies, name,
      auditEntityTupleType).shaped[AMHFeedbackDistributionCpyEntityTupleType]

    private val toFeedbackDtnCpyRow: (AMHFeedbackDistributionCpyEntityTupleType => AMHFeedbackDistributionCpyEntity) = {
      assignmentTuple => {
        AMHFeedbackDistributionCpyEntity(assignmentTuple._1, assignmentTuple._2, assignmentTuple._3,
          assignmentTuple._4, assignmentTuple._5, assignmentTuple._6, assignmentTuple._7,
          assignmentTuple._8, assignmentTuple._9, assignmentTuple._10, assignmentTuple._11,
          assignmentTuple._12, assignmentTuple._13, List(), List()
          ,assignmentTuple._14._1, toDateTime(assignmentTuple._14._2), assignmentTuple._14._3, toOptionDateTime(assignmentTuple._14._4))
      }
    }

    private val toFeedbackDtnCpyTuple: (AMHFeedbackDistributionCpyEntity => Option[AMHFeedbackDistributionCpyEntityTupleType]) = {
      assignmentRow =>
        Some((assignmentRow.code, assignmentRow.sequence,
          assignmentRow.environment, assignmentRow.version, assignmentRow.fileName,
          assignmentRow.active, assignmentRow.dataOwner, assignmentRow.lockCode, assignmentRow.description,
          assignmentRow.selectionGroup, assignmentRow.layoutTemplate,assignmentRow.copies,assignmentRow.name,assignmentRow.audit))
    }

    def * = AMHFeedbackDistributionShapedValue <>(toFeedbackDtnCpyRow, toFeedbackDtnCpyTuple)

  }

  protected class AMHBackupFeedbackDtnCpyRules(tag: Tag) extends Table[AMHFeedbackDistributionCpyRuleEntity](tag, "BAK_AMH_FB_DT_CP_SEL_TAB_RU") with TableAudit {
    def code = column[String]("CODE", O.PrimaryKey)
    def sequence = column[Long]("RC_SEQUENCENUMBER", O.PrimaryKey)
    def ruleCode = column[String]("RC_CODE", O.PrimaryKey)
    def dataOwner = column[Option[String]]("RC_DATAOWNER")
    def lockCode = column[Option[String]]("RC_LOCKCODE")
    def env = column[String]("ENV")
    def version = column[String]("VERSION")

    private type AMHFeedbackDistributionCpyRuleEntityTupleType = (String, Long, String, Option[String], Option[String], String, String, AuditEntityTupleType)

    private val amhFeedbackDtnCpyRuleShapedValue = (
      code,
      sequence,
      ruleCode,
      dataOwner,
      lockCode,
      env,
      version,
      auditEntityTupleType).shaped[AMHFeedbackDistributionCpyRuleEntityTupleType]

    private val toFeedbackDtnCpyRuleRow: (AMHFeedbackDistributionCpyRuleEntityTupleType => AMHFeedbackDistributionCpyRuleEntity) = { assignmentTuple => {
      AMHFeedbackDistributionCpyRuleEntity(assignmentTuple._1, assignmentTuple._2, assignmentTuple._3
        ,assignmentTuple._4, assignmentTuple._5, assignmentTuple._6, assignmentTuple._7
        ,assignmentTuple._8._1, toDateTime(assignmentTuple._8._2), assignmentTuple._8._3, toOptionDateTime(assignmentTuple._8._4))
    }
    }

    private val toFeedbackDtnCpyRuleTuple: (AMHFeedbackDistributionCpyRuleEntity => Option[AMHFeedbackDistributionCpyRuleEntityTupleType]) = { feedbackDtnCpyRuleRow =>
      Some((feedbackDtnCpyRuleRow.code, feedbackDtnCpyRuleRow.sequence, feedbackDtnCpyRuleRow.ruleCode, feedbackDtnCpyRuleRow.dataOwner,
        feedbackDtnCpyRuleRow.lockCode, feedbackDtnCpyRuleRow.environment, feedbackDtnCpyRuleRow.version, feedbackDtnCpyRuleRow.audit))
    }

    def * = amhFeedbackDtnCpyRuleShapedValue <>(toFeedbackDtnCpyRuleRow, toFeedbackDtnCpyRuleTuple)

  }

  protected class AMHBackupFeedbackDtnCpyBackends(tag: Tag) extends Table[AMHFeedbackDistributionCpyBackendEntity](tag, "BAK_AMH_FB_DT_CP_SEL_TAB_MFD") with TableAudit {
    def code = column[String]("CODE", O.PrimaryKey)
    def backCode = column[String]("BCKENDCH_CODE", O.PrimaryKey)
    def backDirection = column[String]("BCKENDCH_DIRECTION", O.PrimaryKey)
    def env = column[String]("ENV", O.PrimaryKey)
    def version = column[String]("VERSION", O.PrimaryKey)
    def dataOwner = column[Option[String]]("DATAOWNER")
    def lockCode = column[Option[String]]("LOCKCODE")

    private type AMHFeedbackDistributionCpyBackendEntityTupleType = (String, String, String, String, String, Option[String], Option[String], AuditEntityTupleType)

    private val AMHFeedbackDtnCpyBackendShapedValue = (
      code,
      backCode,
      backDirection,
      env,
      version,
      dataOwner,
      lockCode,
      auditEntityTupleType).shaped[AMHFeedbackDistributionCpyBackendEntityTupleType]

    private val toFeedbackDtnCpyBackendRow: (AMHFeedbackDistributionCpyBackendEntityTupleType => AMHFeedbackDistributionCpyBackendEntity) =
    { feedbackTuple => {
      AMHFeedbackDistributionCpyBackendEntity(feedbackTuple._1, feedbackTuple._2, feedbackTuple._3
        , feedbackTuple._6,feedbackTuple._7 , feedbackTuple._4, feedbackTuple._5
        ,feedbackTuple._8._1, toDateTime(feedbackTuple._8._2), feedbackTuple._8._3, toOptionDateTime(feedbackTuple._8._4))
    }
    }

    private val toFeedbackDtnCpyBackendTuple: (AMHFeedbackDistributionCpyBackendEntity => Option[AMHFeedbackDistributionCpyBackendEntityTupleType]) = { feedbackDtnCpyBackRow =>
      Some((feedbackDtnCpyBackRow.code, feedbackDtnCpyBackRow.backCode, feedbackDtnCpyBackRow.backDirection, feedbackDtnCpyBackRow.environment, feedbackDtnCpyBackRow.version, feedbackDtnCpyBackRow.dataOwner,
        feedbackDtnCpyBackRow.lockCode, feedbackDtnCpyBackRow.audit))
    }

    def * = AMHFeedbackDtnCpyBackendShapedValue <>(toFeedbackDtnCpyBackendRow, toFeedbackDtnCpyBackendTuple)

  }

  val amhBackupFeedbackDtnCpyBackends = TableQuery[AMHBackupFeedbackDtnCpyBackends]
  val amhBackupFeedbackDtnCpyRules = TableQuery[AMHBackupFeedbackDtnCpyRules]
  val amhBackupFeedbackDistributionCps = TableQuery[AMHBackupFeedbackDistributionCps]

}

object BackupFeedbackDtnCpyDAO extends BackupFeedbackDtnCpyDAO {
  def apply(driver: JdbcProfile) = BackupFeedbackDtnCpyDAO
}
