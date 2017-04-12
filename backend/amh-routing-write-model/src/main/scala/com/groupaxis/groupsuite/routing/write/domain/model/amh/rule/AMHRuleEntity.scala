package com.groupaxis.groupsuite.routing.write.domain.model.amh.rule

import com.groupaxis.groupsuite.persistence.util.{EntityAudit, EntityUpdateAudit}
import org.joda.time.DateTime

case class AMHRuleEntity(code: String, environment: String = "UNKNOWN",
                         version: String = "DEFAULT", dataOwner: Option[String] = None, expression: Option[String] = None,
                         lockCode: Option[String] = None, ruleType: Option[String] = None,
                         deleted : String = "N", originalCode : Option[String] = None,
                         creationUserId : Option[String] = Some("undefined_user"), creationDate : DateTime = DateTime.now, modificationUserId : Option[String] = None, modificationDate : Option[DateTime] = None)

  extends EntityAudit {

  def toES(assigned : Option[Boolean]): AMHRuleEntityES = AMHRuleEntityES(code, environment, version, dataOwner, expression, lockCode, ruleType, assigned)

  //  require(!code.isEmpty, "code.empty")
}

case class AMHRuleEntityES(code: String,
                           environment: String = "UNKNOWN",
                           version: String = "DEFAULT",
                           dataOwner: Option[String] = None,
                           expression: Option[String] = None,
                           lockCode: Option[String] = None,
                           ruleType: Option[String] = None,
                           assigned : Option[Boolean] = None,
                           valid : Option[Boolean] = None,
                           validMessage : Option[String] = None
                           ) {
  def toLineOverview = s"$code;${expression.getOrElse("")}"
}

case class AMHRuleEntityUpdate( environment: String = "UNKNOWN",
                                version: String = "DEFAULT",
                                dataOwner: Option[String] = None,
                                expression: Option[String] = None,
                                lockCode: Option[String] = None,
                                ruleType: Option[String] = None,
                                creationUserId : Option[String] = Some(""),
                                creationDate : Option[DateTime] = None,
                                modificationUserId : Option[String] = Some(""),
                                modificationDate : Option[DateTime] = None)
  extends EntityUpdateAudit  {

  def merge(rule: AMHRuleEntity): AMHRuleEntity = {
    AMHRuleEntity(rule.code, environment, version, dataOwner.orElse(rule.dataOwner), expression.orElse(rule.expression),
      lockCode.orElse(rule.lockCode), ruleType.orElse(rule.ruleType), rule.deleted, rule.originalCode,
      creationUserId.orElse(rule.creationUserId), rule.creationDate,
      modificationUserId.orElse(rule.modificationUserId), modificationDate.orElse(rule.modificationDate))
  }

  def merge(code: String): AMHRuleEntity = {
    AMHRuleEntity(code, environment, version, dataOwner, expression, lockCode, ruleType, "N", None,
      creationUserId, getCreationDateValue, modificationUserId, modificationDate)
  }
}
