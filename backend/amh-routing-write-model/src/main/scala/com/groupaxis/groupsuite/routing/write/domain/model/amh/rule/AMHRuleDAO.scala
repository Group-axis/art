package com.groupaxis.groupsuite.routing.write.domain.model.amh.rule

import com.groupaxis.groupsuite.persistence.driver.DBDriver
import com.groupaxis.groupsuite.persistence.util.TableAudit

trait AMHRuleDAO extends DBDriver {

  // Import the query language features from the driver
  import driver.api._

  protected class AMHRules(tag: Tag) extends Table[AMHRuleEntity](tag, "AMH_GW_RU_CRIT") with TableAudit {
    def code = column[String]("CODE", O.PrimaryKey)
    def env = column[String]("ENV")
    def version = column[String]("VERSION")
    def dataOwner = column[Option[String]]("DATAOWNER")
    def expression = column[Option[String]]("CRITERIA")
    def lockCode = column[Option[String]]("LOCKCODE")
    def ruleType = column[Option[String]]("TYPE")
    def deleted = column[String]("INDICATOR_DELETE")
    def originalCode = column[Option[String]]("ORIGINAL_CODE")

    // for mapping AMHRuleEntity to tuples and back
    private type AMHRuleEntityTupleType = (String, String, String, Option[String], Option[String], Option[String], Option[String], String, Option[String],  AuditEntityTupleType)

    private val amhRuleShapedValue = (
      code,
      env,
      version,
      dataOwner,
      expression,
      lockCode,
      ruleType,
      deleted,
      originalCode,
      auditEntityTupleType
      ).shaped[AMHRuleEntityTupleType]

    private val toRuleRow: (AMHRuleEntityTupleType => AMHRuleEntity) = { ruleTuple => {

      AMHRuleEntity(ruleTuple._1, ruleTuple._2, ruleTuple._3, ruleTuple._4, ruleTuple._5, ruleTuple._6, ruleTuple._7, ruleTuple._8, ruleTuple._9,ruleTuple._10._1,toDateTime(ruleTuple._10._2), ruleTuple._10._3, toOptionDateTime(ruleTuple._10._4))
      }
    }

    private val toRuleTuple: (AMHRuleEntity => Option[AMHRuleEntityTupleType]) = { ruleRow =>
      Some((ruleRow.code, ruleRow.environment, ruleRow.version, ruleRow.dataOwner, ruleRow.expression, ruleRow.lockCode, ruleRow.ruleType, ruleRow.deleted, ruleRow.originalCode, ruleRow.audit))
    }

    def * = amhRuleShapedValue <> (toRuleRow, toRuleTuple)

  }

  val amhRules = TableQuery[AMHRules]
}

object AMHRuleDAO extends AMHRuleDAO {
  def apply = AMHRuleDAO
}

