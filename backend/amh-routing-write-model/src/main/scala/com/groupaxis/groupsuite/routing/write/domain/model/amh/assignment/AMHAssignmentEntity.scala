package com.groupaxis.groupsuite.routing.write.domain.model.amh.assignment

import com.groupaxis.groupsuite.persistence.util.{EntityAudit, EntityUpdateAudit}
import com.groupaxis.groupsuite.routing.amh.read.domain.model.view.assignment.overview.AMHAssignmentOverviewES
import com.groupaxis.groupsuite.routing.write.domain.model.amh.rule.AMHRule
import org.joda.time.DateTime

case class AMHAssignmentEntity(code: String, sequence: Long, backCode: String,
                               backDirection: String,
                               active: Option[String], dataOwner: Option[String], lockCode: Option[String],
                               description: Option[String], environment: String,
                               version: String,
                               fileName: String = "Noname.xml", rules: Seq[AMHAssignmentRuleEntity] = List(),
                               creationUserId : Option[String] = None, creationDate : DateTime = DateTime.now, modificationUserId : Option[String] = None, modificationDate : Option[DateTime] = None)

  extends EntityAudit {

def toES(rules: Seq[AMHAssignmentRuleES]) = AMHAssignmentES(active.exists(value => value.toBoolean), AMHAssignmentBackES(backCode, backDirection),
    code, dataOwner, description, lockCode, sequence, environment, version, rules)
}

case class AMHAssignmentEntityUpdate(sequence: Long, backCode: String, backDirection: String,
                                     active: Option[String] = None, dataOwner: Option[String] = None,
                                     lockCode: Option[String] = None, description: Option[String] = None,
                                     environment: String,
                                     version: String,
                                     rules: Seq[AMHAssignmentRuleEntityUpdate] = List(),
                                     creationUserId : Option[String] = Some("undefined_user"), creationDate : Option[DateTime] = None, modificationUserId : Option[String] = Some("undefined_user"), modificationDate : Option[DateTime] = None)

  extends EntityUpdateAudit {

  def merge(assign: AMHAssignmentEntity): AMHAssignmentEntity =
    AMHAssignmentEntity(assign.code, sequence, backCode, backDirection, active.orElse(assign.active),
      dataOwner.orElse(assign.dataOwner), lockCode.orElse(assign.lockCode), description.orElse(assign.description),
      environment, version, "Noname.xml",
       rules.map(_.merge(assign.rules)), creationUserId.orElse(assign.creationUserId),assign.creationDate
      ,modificationUserId.orElse(assign.modificationUserId),modificationDate.orElse(assign.modificationDate))

  def merge(code : String): AMHAssignmentEntity =
    AMHAssignmentEntity(code, sequence, backCode, backDirection, active,
      dataOwner, lockCode, description, environment, version, "Noname.xml", rules.map(_.merge),
      creationUserId, getCreationDateValue, modificationUserId, modificationDate)
}

  case class AMHAssignmentRuleEntity(code: String, sequence: Long, ruleCode: String,
                                    dataOwner: Option[String], lockCode: Option[String],
                                    environment: String, version: String,
                                    creationUserId : Option[String] = None, creationDate : DateTime = DateTime.now, modificationUserId : Option[String] = None, modificationDate : Option[DateTime] = None)
  extends EntityAudit with  AMHRule {

  def toES(expression: String = ""): AMHAssignmentRuleES = AMHAssignmentRuleES(ruleCode, dataOwner.getOrElse(""), lockCode.getOrElse(""), sequence, expression)
}

case class AMHAssignmentRuleEntityUpdate(code: String, sequence: Long, ruleCode: String,
                                   dataOwner: Option[String], lockCode: Option[String],
                                   environment: String, version: String,
                                   creationUserId : Option[String] = None, creationDate : Option[DateTime] = None, modificationUserId : Option[String] = None, modificationDate : Option[DateTime] = None)
  extends EntityUpdateAudit with  AMHRule {

  def merge(assignRules: Seq[AMHAssignmentRuleEntity]): AMHAssignmentRuleEntity = {
    merge(assignRules.find(_.ruleCode == ruleCode))
  }

  private def merge(assignOrigRule: Option[AMHAssignmentRuleEntity]) : AMHAssignmentRuleEntity = assignOrigRule match {
    case Some(assignRule) =>
      AMHAssignmentRuleEntity(code, sequence, ruleCode, dataOwner.orElse(assignRule.dataOwner)
        , lockCode.orElse(assignRule.lockCode), environment, version,
        creationUserId.orElse(assignRule.creationUserId), assignRule.creationDate, modificationUserId.orElse(assignRule.modificationUserId), modificationDate.orElse(assignRule.modificationDate))
    case None => merge
  }

  def merge: AMHAssignmentRuleEntity =
    AMHAssignmentRuleEntity(code, sequence, ruleCode, dataOwner
      , lockCode, environment, version,
      creationUserId, getCreationDateValue, modificationUserId, modificationDate)

}

case class AMHAssignmentBackES(code: String, direction: String)

case class AMHAssignmentRuleES(code: String, dataOwner: String, lockCode: String, sequence: Long, expression: String)

case class AMHAssignmentES( active: Boolean, backendPrimaryKey: AMHAssignmentBackES,
                           code: String, dataOwner: Option[String], description: Option[String], lockCode: Option[String]
                           , sequence: Long, environment : String, version : String, rules: Seq[AMHAssignmentRuleES]) {

  def toOverview : Seq[AMHAssignmentOverviewES] = {
     if (rules.isEmpty) {
       Seq(AMHAssignmentOverviewES(active, code, sequence, Some(backendPrimaryKey.code), Some(backendPrimaryKey.direction)))
     } else {
        rules.sortWith(_.sequence < _.sequence)
          .map(rule => {
          AMHAssignmentOverviewES(active, code, sequence, Some(backendPrimaryKey.code), Some(backendPrimaryKey.direction), Some(rule.code), Some(rule.sequence), Some(rule.expression))
        })
     }
  }
}


