package com.groupaxis.groupsuite.routing.write.domain.model.amh.feedback.distribution.copy

import com.groupaxis.groupsuite.persistence.util.{EntityAudit, EntityUpdateAudit}
import com.groupaxis.groupsuite.routing.amh.read.domain.model.view.assignment.overview.AMHAssignmentOverviewES
import com.groupaxis.groupsuite.routing.write.domain.model.amh.rule.AMHRule
import org.joda.time.DateTime

case class AMHFeedbackDistributionCpyEntity(code: String, sequence: Long,
                                            environment: String = "UNKNOWN",
                                            version: String = "DEFAULT",
                                            fileName: String = "Noname.xml",
                                            active: Option[String],
                                            dataOwner: Option[String],
                                            lockCode: Option[String],
                                            description: Option[String],
                                            selectionGroup: Option[String],
                                            layoutTemplate: Option[String],
                                            copies: Option[Long],
                                            name: Option[String],
                                            backends: Seq[AMHFeedbackDistributionCpyBackendEntity] = List(),
                                            rules: Seq[AMHFeedbackDistributionCpyRuleEntity] = List(),
                                            creationUserId : Option[String] = Some("undefined_user"), creationDate : DateTime = DateTime.now, modificationUserId : Option[String] = Some("undefined_user"), modificationDate : Option[DateTime] = None)
                    extends EntityAudit {

  def toES(backends: Seq[AMHFeedbackDistributionCpyBackES], rules: Seq[AMHFeedbackDistributionCpyRuleES]) =
    AMHFeedbackDistributionCpyES(active.exists(value => value.toBoolean),
    code, dataOwner, description, lockCode, sequence,environment, version, selectionGroup, layoutTemplate, copies, name, backends, rules)

}

case class AMHFeedbackDistributionCpyEntityUpdate(sequence: Long, environment: String = "UNKNOWN",
                     version: String = "DEFAULT",
                     active: Option[String] = None, dataOwner: Option[String] = None,
                     lockCode: Option[String] = None, description: Option[String] = None,
                      selectionGroup: Option[String] = None, layoutTemplate: Option[String] = None,
                      copies: Option[Long] = None, name: Option[String] = None, //lastModification: Option[Date] = None,
                      backends: Seq[AMHFeedbackDistributionCpyBackendEntityUpdate] = List(), rules: Seq[AMHFeedbackDistributionCpyRuleEntityUpdate] = List(),
                      creationUserId : Option[String] = Some("undefined_user"), creationDate : Option[DateTime] = None, modificationUserId : Option[String] = Some("undefined_user"), modificationDate : Option[DateTime] = None)
    extends EntityUpdateAudit {

  def merge(feedback: AMHFeedbackDistributionCpyEntity): AMHFeedbackDistributionCpyEntity =
    AMHFeedbackDistributionCpyEntity(feedback.code, sequence, environment, version,
      "Noname.xml", active.orElse(feedback.active), dataOwner.orElse(feedback.dataOwner),
      lockCode.orElse(feedback.lockCode), description.orElse(feedback.description),
      selectionGroup.orElse(feedback.selectionGroup), layoutTemplate.orElse(feedback.layoutTemplate),
      copies.orElse(feedback.copies), name.orElse(feedback.name),
      backends = backends.map(_.merge(feedback.backends)), rules = rules.map(_.merge(feedback.rules)),
      creationUserId.orElse(feedback.creationUserId),feedback.creationDate,
      modificationUserId.orElse(feedback.modificationUserId),modificationDate.orElse(feedback.modificationDate))

  def merge(code : String): AMHFeedbackDistributionCpyEntity =
    AMHFeedbackDistributionCpyEntity(code,
      sequence, environment, version,
      "Noname.xml", active, dataOwner, lockCode, description,
      selectionGroup, layoutTemplate, copies,name,
      backends = backends.map(_.merge), rules = rules.map(_.merge),
      modificationUserId,getCreationDateValue, creationUserId, modificationDate)
}

case class AMHFeedbackDistributionCpyRuleEntity(code: String, sequence: Long, ruleCode: String,
                                   dataOwner: Option[String], lockCode: Option[String],
                                   environment: String = "UNKNOWN",
                                   version: String = "DEFAULT",
                                   creationUserId : Option[String] = Some("undefined_user"), creationDate : DateTime = DateTime.now, modificationUserId : Option[String] = Some("undefined_user"), modificationDate : Option[DateTime] = None)
                           extends AMHRule with EntityAudit {

  def toES(expression: String = ""): AMHFeedbackDistributionCpyRuleES = AMHFeedbackDistributionCpyRuleES(ruleCode, dataOwner.getOrElse(""), lockCode.getOrElse(""), sequence, expression)
}

case class AMHFeedbackDistributionCpyRuleEntityUpdate(code: String, sequence: Long, ruleCode: String,
                                                dataOwner: Option[String], lockCode: Option[String],
                                                environment: String = "UNKNOWN",
                                                version: String = "DEFAULT",
                                                creationUserId : Option[String] = Some("undefined_user"), creationDate : Option[DateTime] = None, modificationUserId : Option[String] = Some("undefined_user"), modificationDate : Option[DateTime] = None)
        extends AMHRule with EntityUpdateAudit {

  def merge(assignRules: Seq[AMHFeedbackDistributionCpyRuleEntity]): AMHFeedbackDistributionCpyRuleEntity = {
    merge(assignRules.find(_.ruleCode == ruleCode))
  }

  private def merge(assignOrigRule: Option[AMHFeedbackDistributionCpyRuleEntity]) : AMHFeedbackDistributionCpyRuleEntity = assignOrigRule match {
    case Some(assignRule) =>
      AMHFeedbackDistributionCpyRuleEntity(code, sequence, ruleCode, dataOwner.orElse(assignRule.dataOwner)
        , lockCode.orElse(assignRule.lockCode), environment, version,
        creationUserId.orElse(assignRule.creationUserId), assignRule.creationDate, modificationUserId.orElse(assignRule.modificationUserId), modificationDate.orElse(assignRule.modificationDate))
    case None => merge
  }

  def merge: AMHFeedbackDistributionCpyRuleEntity =
    AMHFeedbackDistributionCpyRuleEntity(code, sequence, ruleCode, dataOwner
      , lockCode, environment, version,
      creationUserId, getCreationDateValue, modificationUserId, modificationDate)


}

case class AMHFeedbackDistributionCpyBackendEntity(code: String, backCode: String, backDirection: String,
                                                dataOwner: Option[String], lockCode: Option[String],
                                                environment: String = "UNKNOWN",
                                                version: String = "DEFAULT",
                                                creationUserId : Option[String] = Some("undefined_user"), creationDate : DateTime = DateTime.now, modificationUserId : Option[String] = Some("undefined_user"), modificationDate : Option[DateTime] = None
                                               ) extends EntityAudit {

  def toES: AMHFeedbackDistributionCpyBackES = AMHFeedbackDistributionCpyBackES(backCode,
                          backDirection, dataOwner.getOrElse(""), lockCode.getOrElse(""))
}

case class AMHFeedbackDistributionCpyBackendEntityUpdate(code: String, backCode: String, backDirection: String,
                                                   dataOwner: Option[String], lockCode: Option[String],
                                                   environment: String = "UNKNOWN",
                                                   version: String = "DEFAULT",
                                                   creationUserId : Option[String] = Some("undefined_user"), creationDate : Option[DateTime] = None, modificationUserId : Option[String] = Some("undefined_user"), modificationDate : Option[DateTime] = None
                                                  ) extends EntityUpdateAudit {



 def merge(assignBackends: Seq[AMHFeedbackDistributionCpyBackendEntity]): AMHFeedbackDistributionCpyBackendEntity = {
    merge(assignBackends.find(_.backCode == backCode))
  }

  private def merge(assignOrigRule: Option[AMHFeedbackDistributionCpyBackendEntity]) : AMHFeedbackDistributionCpyBackendEntity = assignOrigRule match {
    case Some(assignRule) =>
      AMHFeedbackDistributionCpyBackendEntity(code, backCode, backDirection, dataOwner.orElse(assignRule.dataOwner)
        , lockCode.orElse(assignRule.lockCode), environment, version,
        creationUserId.orElse(assignRule.creationUserId), assignRule.creationDate, modificationUserId.orElse(assignRule.modificationUserId), modificationDate.orElse(assignRule.modificationDate))
    case None => merge
  }

  def merge: AMHFeedbackDistributionCpyBackendEntity =
    AMHFeedbackDistributionCpyBackendEntity(code, backCode, backDirection, dataOwner
      , lockCode, environment, version,
      creationUserId, getCreationDateValue, modificationUserId, modificationDate)

}


case class AMHFeedbackDistributionCpyBackES(code: String, direction: String,
                                            dataOwner: String, lockCode: String)
//backendPrimaryKey: AMHFeedbackDistributionCpyBackES,
case class AMHFeedbackDistributionCpyRuleES(code: String, dataOwner: String, lockCode: String, sequence: Long, expression: String)

case class AMHFeedbackDistributionCpyES( active: Boolean,
                           code: String, dataOwner: Option[String], description: Option[String], lockCode: Option[String]
                           , sequence: Long, environment: String, version :String, selectionGroup: Option[String],
                                         layoutTemplate: Option[String], copies : Option[Long], name: Option[String],
                                         backends: Seq[AMHFeedbackDistributionCpyBackES], rules: Seq[AMHFeedbackDistributionCpyRuleES]) {

  def toOverview: Seq[AMHAssignmentOverviewES] = {

    def next(assign: AMHAssignmentOverviewES, backends: Seq[AMHFeedbackDistributionCpyBackES], rules: Seq[AMHFeedbackDistributionCpyRuleES], assigns: Seq[AMHAssignmentOverviewES]): Seq[AMHAssignmentOverviewES] = {

      if (backends.isEmpty && rules.isEmpty) {
        assigns
      } else {
        val (newBackCode, newBackDirec) = backends.headOption.map(b => (Some(b.code), Some(b.direction))).getOrElse((None, None))
        val (newRuleCode, newRuleSeq, newRuleExpress) = rules.headOption.map(r => (Some(r.code), Some(r.sequence), Some(r.expression))).getOrElse((None, None, None))

        val newAssignment = Seq(assign.copy(backCode = newBackCode, backDirection = newBackDirec, ruleCode = newRuleCode, ruleSequence = newRuleSeq, ruleExpression = newRuleExpress))
        val ruleTail = if (rules.isEmpty) Seq() else rules.tail
        val backTail = if (backends.isEmpty) Seq() else backends.tail
        next(assign, backTail, ruleTail, assigns ++ newAssignment)
      }

    }

    val orderedRules = rules.sortWith(_.sequence < _.sequence)
    val orderedBackends = backends.sortWith(_.code < _.code)
    next(AMHAssignmentOverviewES(active, code, sequence), orderedBackends, orderedRules, Seq())
  }
}