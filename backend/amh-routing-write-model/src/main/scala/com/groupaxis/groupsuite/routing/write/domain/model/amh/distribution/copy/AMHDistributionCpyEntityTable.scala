package com.groupaxis.groupsuite.routing.write.domain.model.amh.distribution.copy

import com.groupaxis.groupsuite.persistence.driver.DBDriver
import com.groupaxis.groupsuite.persistence.util.TableAudit
import slick.driver.JdbcProfile

trait DistributionCpyDAO extends DBDriver {

  import driver.api._

  protected class AMHDistributionCps(tag: Tag) extends Table[AMHDistributionCpyEntity](tag, "AMH_DT_CP_SEL_TAB") with TableAudit {
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

    // for mapping AMHDistributionCpyEntity to tuples and back
    private type AMHDistributionCpyEntityTupleType = (
      String, Long, String,
        String, String,  Option[String], Option[String], Option[String],
        Option[String], Option[String], Option[String], Option[Long], Option[String], AuditEntityTupleType)

    private val AMHDistributionShapedValue = (
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
      copies, name, auditEntityTupleType).shaped[AMHDistributionCpyEntityTupleType]

    private val toDistributionCpyRow: (AMHDistributionCpyEntityTupleType => AMHDistributionCpyEntity) = {
      distributionCpyTuple => {
      AMHDistributionCpyEntity(distributionCpyTuple._1, distributionCpyTuple._2, distributionCpyTuple._3,
        distributionCpyTuple._4, distributionCpyTuple._5, distributionCpyTuple._6, distributionCpyTuple._7,
        distributionCpyTuple._8, distributionCpyTuple._9, distributionCpyTuple._10, distributionCpyTuple._11,
          distributionCpyTuple._12, distributionCpyTuple._13, List(), List(),
        distributionCpyTuple._14._1, toDateTime(distributionCpyTuple._14._2), distributionCpyTuple._14._3, toOptionDateTime(distributionCpyTuple._14._4))
    }
    }

    private val toDistributionCpyTuple: (AMHDistributionCpyEntity => Option[AMHDistributionCpyEntityTupleType]) = {
      distributionCpyRow =>
      Some((distributionCpyRow.code, distributionCpyRow.sequence,
        distributionCpyRow.environment, distributionCpyRow.version, distributionCpyRow.fileName,
        distributionCpyRow.active, distributionCpyRow.dataOwner, distributionCpyRow.lockCode, distributionCpyRow.description,
        distributionCpyRow.selectionGroup, distributionCpyRow.layoutTemplate,distributionCpyRow.copies,distributionCpyRow.name,distributionCpyRow.audit))
    }

    def * = AMHDistributionShapedValue <>(toDistributionCpyRow, toDistributionCpyTuple)

  }

  val amhDistributionCps : TableQuery[AMHDistributionCps] = TableQuery[AMHDistributionCps]

}
object DistributionCpyDAO extends DistributionCpyDAO {
  def apply(driver: JdbcProfile) = DistributionCpyDAO
}

trait DistributionCpyRuleDAO extends DBDriver {

  import driver.api._

  protected class AMHDistributionCpyRules(tag: Tag) extends Table[AMHDistributionCpyRuleEntity](tag, "AMH_DT_CP_SEL_TAB_RU") with TableAudit {
    def code = column[String]("CODE", O.PrimaryKey)
    def sequence = column[Long]("RC_SEQUENCENUMBER", O.PrimaryKey)
    def ruleCode = column[String]("RC_CODE", O.PrimaryKey)
    def dataOwner = column[Option[String]]("RC_DATAOWNER")
    def lockCode = column[Option[String]]("RC_LOCKCODE")
    def env = column[String]("ENV")
    def version = column[String]("VERSION")

    // for mapping AMHDistributionCpyEntity to tuples and back
    private type AMHDistributionCpyRuleEntityTupleType = (String, Long, String, Option[String], Option[String], String, String, AuditEntityTupleType)

    private val amhDistributionCpyRuleShapedValue = (
      code,
      sequence,
      ruleCode,
      dataOwner,
      lockCode,
      env,
      version,
      auditEntityTupleType).shaped[AMHDistributionCpyRuleEntityTupleType]

    private val toDistributionCpyRuleRow: (AMHDistributionCpyRuleEntityTupleType => AMHDistributionCpyRuleEntity) = { assignmentTuple => {
      AMHDistributionCpyRuleEntity(assignmentTuple._1, assignmentTuple._2, assignmentTuple._3
       ,assignmentTuple._4, assignmentTuple._5, assignmentTuple._6, assignmentTuple._7,
        assignmentTuple._8._1, toDateTime(assignmentTuple._8._2), assignmentTuple._8._3, toOptionDateTime(assignmentTuple._8._4))
     }
    }

    private val toDistributionCpyRuleTuple: (AMHDistributionCpyRuleEntity => Option[AMHDistributionCpyRuleEntityTupleType]) = { distributionCpyRuleRow =>
      Some((distributionCpyRuleRow.code, distributionCpyRuleRow.sequence, distributionCpyRuleRow.ruleCode, distributionCpyRuleRow.dataOwner,
        distributionCpyRuleRow.lockCode, distributionCpyRuleRow.environment, distributionCpyRuleRow.version, distributionCpyRuleRow.audit))
    }

    def * = amhDistributionCpyRuleShapedValue <>(toDistributionCpyRuleRow, toDistributionCpyRuleTuple)

  }

  val amhDistributionCpyRules = TableQuery[AMHDistributionCpyRules]

}

object DistributionCpyRuleDAO extends DistributionCpyRuleDAO {
  def apply(driver: JdbcProfile) = DistributionCpyRuleDAO
}

trait DistributionCpyBackendDAO extends DBDriver {

  import driver.api._

  protected  class AMHDistributionCpyBackends(tag: Tag) extends Table[AMHDistributionCpyBackendEntity](tag, "AMH_DT_CP_SEL_TAB_MCD") with TableAudit {
    def code = column[String]("CODE", O.PrimaryKey)
    def backCode = column[String]("BCKENDCH_CODE", O.PrimaryKey)
    def backDirection = column[String]("BCKENDCH_DIRECTION", O.PrimaryKey)
    def env = column[String]("ENV", O.PrimaryKey)
    def version = column[String]("VERSION", O.PrimaryKey)
    def dataOwner = column[Option[String]]("DATAOWNER")
    def lockCode = column[Option[String]]("LOCKCODE")

    // for mapping AMHDistributionCpyEntity to tuples and back
    private type AMHDistributionCpyBackendEntityTupleType = (String, String, String, String, String, Option[String], Option[String], AuditEntityTupleType)

    private val AMHDistributionCpyBackendShapedValue = (
      code,
      backCode,
      backDirection,
      env,
      version,
      dataOwner,
      lockCode,
      auditEntityTupleType).shaped[AMHDistributionCpyBackendEntityTupleType]

    private val toDistributionCpyBackendRow: (AMHDistributionCpyBackendEntityTupleType => AMHDistributionCpyBackendEntity) =
    { distributionTuple => {
      AMHDistributionCpyBackendEntity(distributionTuple._1, distributionTuple._2, distributionTuple._3
        , distributionTuple._6,distributionTuple._7 , distributionTuple._4, distributionTuple._5
        , distributionTuple._8._1, toDateTime(distributionTuple._8._2), distributionTuple._8._3, toOptionDateTime(distributionTuple._8._4))
    }
    }

    private val toDistributionCpyBackendTuple: (AMHDistributionCpyBackendEntity => Option[AMHDistributionCpyBackendEntityTupleType]) = { DistributionCpyBackRow =>
      Some((DistributionCpyBackRow.code, DistributionCpyBackRow.backCode, DistributionCpyBackRow.backDirection, DistributionCpyBackRow.environment, DistributionCpyBackRow.version, DistributionCpyBackRow.dataOwner,
        DistributionCpyBackRow.lockCode, DistributionCpyBackRow.audit))
    }

    def * = AMHDistributionCpyBackendShapedValue <>(toDistributionCpyBackendRow, toDistributionCpyBackendTuple)

  }

  val amhDistributionCpyBackends = TableQuery[AMHDistributionCpyBackends]

}

object DistributionCpyBackendDAO extends DistributionCpyBackendDAO {
  def apply(driver: JdbcProfile) = DistributionCpyBackendDAO
}