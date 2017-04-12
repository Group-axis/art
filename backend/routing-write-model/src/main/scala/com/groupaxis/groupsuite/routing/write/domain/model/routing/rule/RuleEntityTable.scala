package com.groupaxis.groupsuite.routing.write.domain.model.routing.rule

import com.groupaxis.groupsuite.persistence.driver.DBDriver

trait RuleDAO extends DBDriver {

  // Import the query language features from the driver
  import driver.api._

  protected class Rules(tag: Tag) extends Table[RuleEntity](tag, "sbs_routingrulesdata") {
    def sequence = column[Long]("seq", O.PrimaryKey)
    def routingPointName = column[String]("routingpointname", O.PrimaryKey)
    def full = column[Option[String]]("ful")
    def ruleDescription = column[Option[String]]("ruledescription")
    def schemaMap = column[Option[String]]("schemamap")
    def environment = column[String]("env")
    def version = column[String]("version")

    def actionOn = column[Option[String]]("actionon")
    def instanceAction = column[Option[String]]("instanceaction")
    def instanceInterventionType = column[Option[String]]("instanceinterventiontype")
    def instanceInterventionTypeText = column[Option[String]]("instanceinterventiontypetext")
    def instanceRoutingCode = column[Option[String]]("instanceroutingcode")
    def instanceTargetQueue = column[Option[String]]("instancetargetqueue")
    def instanceUnit = column[Option[String]]("instanceunit")
    def instancePriority = column[Option[String]]("instancepriority")
    def newInstanceAction = column[Option[String]]("newinstaction")
    def newInstanceRoutingCode = column[Option[String]]("newinstinstanceroutingcode")
    def newInstanceInterventionType = column[Option[String]]("newinstinterventiontype")
    def newInstanceInterventionTypeText = column[Option[String]]("newinstanceinterventiontyptxt")
    def newInstanceTargetQueue = column[Option[String]]("newinsttargetqueue")
    def newInstanceType = column[Option[String]]("newinsttype")
    def newInstanceUnit = column[Option[String]]("newinstanceunit")
    def newInstancePriority = column[Option[String]]("newinstancepriority")

    def conditionOn = column[Option[String]]("conditionon")
    def criteria = column[Option[String]]("criteria")
    def functionList = column[Option[String]]("functionlist")

    // for mapping RuleEntity to tuples and back
    private type RuleActionTupleType = (Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String])
    private type RuleConditionTupleType = (Option[String], Option[String], Option[String])
    private type RuleEntityTupleType = (Long, String, Option[String], Option[String], Option[String],String, String, RuleActionTupleType, RuleConditionTupleType)

    private val ruleShapedValue = (
      sequence,
      routingPointName,
      full,
      ruleDescription,
      schemaMap,
      environment,
      version,
      (actionOn,
        instanceAction,
        instanceInterventionType,
        instanceInterventionTypeText,
        instanceRoutingCode,
        instanceTargetQueue,
        instanceUnit,
        instancePriority,
        newInstanceAction,
        newInstanceRoutingCode,
        newInstanceInterventionType,
        newInstanceInterventionTypeText,
        newInstanceTargetQueue,
        newInstanceType,
        newInstanceUnit,
        newInstancePriority),
        (conditionOn, criteria, functionList)).shaped[RuleEntityTupleType]

    private val toRuleRow: (RuleEntityTupleType => RuleEntity) = { ruleTuple =>
      {
        RuleEntity(ruleTuple._1, ruleTuple._2, ruleTuple._3, ruleTuple._4, ruleTuple._5, ruleTuple._6, ruleTuple._7, action = RuleAction.tupled.apply(ruleTuple._8), condition = RuleCondition.tupled.apply(ruleTuple._9))
      }
    }

    private val toRuleTuple: (RuleEntity => Option[RuleEntityTupleType]) = { ruleRow =>
      Some((ruleRow.sequence, ruleRow.routingPointName, ruleRow.full, ruleRow.ruleDescription, ruleRow.schemaMap, ruleRow.environment, ruleRow.version, RuleAction.unapply(ruleRow.action).get, RuleCondition.unapply(ruleRow.condition).get))
    }

    def * = ruleShapedValue <> (toRuleRow, toRuleTuple)

  }

  val rules = TableQuery[Rules]

}

object RuleDAO extends RuleDAO {
  def apply = RuleDAO
}
