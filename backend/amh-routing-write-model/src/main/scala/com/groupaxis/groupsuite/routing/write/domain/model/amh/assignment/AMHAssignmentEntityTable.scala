package com.groupaxis.groupsuite.routing.write.domain.model.amh.assignment

import com.groupaxis.groupsuite.persistence.driver.DBDriver
import com.groupaxis.groupsuite.persistence.util.TableAudit
import slick.driver.JdbcProfile

trait AssignmentDAO extends DBDriver {
  // Import the query language features from the driver
  import driver.api._

  class AMHAssignments(tag: Tag) extends Table[AMHAssignmentEntity](tag, "AMH_BE_CH_AS_SEL_TAB") with TableAudit {

    def code = column[String]("CODE", O.PrimaryKey)

    def sequence = column[Long]("SEQUENCENUMBER")

    def backCode = column[String]("BCKENDCH_CODE")

    def backDirection = column[String]("BCKENDCH_DIRECTION")

    def active = column[Option[String]]("ACTIVE")

    def dataOwner = column[Option[String]]("DATAOWNER")

    def lockCode = column[Option[String]]("LOCKCODE")

    def description = column[Option[String]]("DESCRIPTION")

    def fileName = column[String]("FILENAME")

    def env = column[String]("ENV")

    def version = column[String]("VERSION")


    // for mapping AMHAssignmentEntity to tuples and back
    private type AMHAssignmentEntityTupleType = (String, Long, String, String, Option[String], Option[String], Option[String], Option[String], String, String, String, AuditEntityTupleType)

    private val amhAssignmentShapedValue = (
      code,
      sequence,
      backCode,
      backDirection,
      active,
      dataOwner,
      lockCode,
      description,
      fileName,
      env,
      version,
      auditEntityTupleType).shaped[AMHAssignmentEntityTupleType]

    private val toAssignmentRow: (AMHAssignmentEntityTupleType => AMHAssignmentEntity) = { assignmentTuple => {
      AMHAssignmentEntity(assignmentTuple._1, assignmentTuple._2, assignmentTuple._3, assignmentTuple._4, assignmentTuple._5, assignmentTuple._6, assignmentTuple._7, assignmentTuple._8, assignmentTuple._10, assignmentTuple._11, assignmentTuple._9, List(),assignmentTuple._12._1,toDateTime(assignmentTuple._12._2), assignmentTuple._12._3, toOptionDateTime(assignmentTuple._12._4))
     }
    }

    private val toAssignmentTuple: (AMHAssignmentEntity => Option[AMHAssignmentEntityTupleType]) = { assignmentRow =>
      Some((assignmentRow.code, assignmentRow.sequence, assignmentRow.backCode, assignmentRow.backDirection, assignmentRow.active, assignmentRow.dataOwner, assignmentRow.lockCode, assignmentRow.description, assignmentRow.fileName, assignmentRow.environment, assignmentRow.version, assignmentRow.audit))
    }

    def * = amhAssignmentShapedValue <>(toAssignmentRow, toAssignmentTuple)

  }

  val amhAssignments : TableQuery[AMHAssignments] = TableQuery[AMHAssignments]

}

object AssignmentDAO extends AssignmentDAO {
  def apply(driver: JdbcProfile) = AssignmentDAO
}

trait AssignmentRuleDAO extends DBDriver {

  import driver.api._

  class AMHAssignmentRules(tag: Tag) extends Table[AMHAssignmentRuleEntity](tag, "AMH_BE_CH_AS_SEL_RU") with TableAudit {

    def code = column[String]("CODE", O.PrimaryKey)

    def sequence = column[Long]("RC_SEQUENCENUMBER", O.PrimaryKey)

    def ruleCode = column[String]("RC_CODE", O.PrimaryKey)

    def dataOwner = column[Option[String]]("RC_DATAOWNER")

    def lockCode = column[Option[String]]("RC_LOCKCODE")

    def env = column[String]("ENV")

    def version = column[String]("VERSION")

    private type AMHAssignmentRuleEntityTupleType = (String, Long, String, Option[String], Option[String], String, String, AuditEntityTupleType)

    private val amhAssignmentRuleShapedValue = (
      code,
      sequence,
      ruleCode,
      dataOwner,
      lockCode,
      env,
      version,
      auditEntityTupleType).shaped[AMHAssignmentRuleEntityTupleType]

    private val toAssignmentRuleRow: (AMHAssignmentRuleEntityTupleType => AMHAssignmentRuleEntity) = { assignmentTuple => {
      AMHAssignmentRuleEntity(assignmentTuple._1, assignmentTuple._2, assignmentTuple._3, assignmentTuple._4, assignmentTuple._5, assignmentTuple._6, assignmentTuple._7,assignmentTuple._8._1,toDateTime(assignmentTuple._8._2), assignmentTuple._8._3, toOptionDateTime(assignmentTuple._8._4))
    }
    }

    private val toAssignmentRuleTuple: (AMHAssignmentRuleEntity => Option[AMHAssignmentRuleEntityTupleType]) = { assignmentRuleRow =>
      Some((assignmentRuleRow.code, assignmentRuleRow.sequence, assignmentRuleRow.ruleCode, assignmentRuleRow.dataOwner, assignmentRuleRow.lockCode, assignmentRuleRow.environment, assignmentRuleRow.version, assignmentRuleRow.audit))
    }

    def * = amhAssignmentRuleShapedValue <>(toAssignmentRuleRow, toAssignmentRuleTuple)

  }

  val amhAssignmentRules = TableQuery[AMHAssignmentRules]

}

object AssignmentRuleDAO extends AssignmentRuleDAO {
  def apply(driver: JdbcProfile) = AssignmentRuleDAO
}
