package com.groupaxis.groupsuite.routing.write.domain.model.amh.distribution.copy

import com.groupaxis.groupsuite.persistence.util.{EntityAudit, EntityUpdateAudit}
import com.groupaxis.groupsuite.routing.amh.read.domain.model.view.assignment.overview.AMHAssignmentOverviewES
import com.groupaxis.groupsuite.routing.write.domain.model.amh.rule.AMHRule
import org.joda.time.DateTime

case class AMHDistributionCpyEntity(code: String, sequence: Long,
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
                                    backends: Seq[AMHDistributionCpyBackendEntity] = List(),
                                    rules: Seq[AMHDistributionCpyRuleEntity] = List(),
                                    creationUserId : Option[String] = Some("undefined_user"), creationDate : DateTime = DateTime.now, modificationUserId : Option[String] = Some("undefined_user"), modificationDate : Option[DateTime] = None)

  extends EntityAudit {


  def toES(backends: Seq[AMHDistributionCpyBackES], rules: Seq[AMHDistributionCpyRuleES]) = AMHDistributionCpyES(active.exists(value => value.toBoolean),
    code, dataOwner, description, lockCode, sequence, environment, version, copies, name, backends, rules)
}

case class AMHDistributionCpyEntityUpdate(sequence: Long, environment: String = "UNKNOWN",
                                          version: String = "DEFAULT",
                                          active: Option[String] = None, dataOwner: Option[String] = None,
                                          lockCode: Option[String] = None, description: Option[String] = None,
                                          selectionGroup: Option[String] = None, layoutTemplate: Option[String] = None,
                                          copies: Option[Long] = None, name: Option[String] = None,
                                          backends: Seq[AMHDistributionCpyBackendEntityUpdate] = List(), rules: Seq[AMHDistributionCpyRuleEntityUpdate] = List(),
                                          creationUserId : Option[String] = Some("undefined_user"), creationDate : Option[DateTime] = None,
                                          modificationUserId : Option[String] = Some("undefined_user"), modificationDate : Option[DateTime] = None)

  extends EntityUpdateAudit {


  def merge(distribution: AMHDistributionCpyEntity): AMHDistributionCpyEntity =
    AMHDistributionCpyEntity(distribution.code, sequence, environment, version, "Noname.xml",
      active.orElse(distribution.active), dataOwner.orElse(distribution.dataOwner),
      lockCode.orElse(distribution.lockCode), description.orElse(distribution.description),
      selectionGroup.orElse(distribution.selectionGroup), layoutTemplate.orElse(distribution.layoutTemplate),
      copies.orElse(distribution.copies), name.orElse(distribution.name),
      backends.map(_.merge(distribution.backends)), rules.map(_.merge(distribution.rules)),creationUserId.orElse(distribution.creationUserId),distribution.creationDate
      ,modificationUserId.orElse(distribution.modificationUserId),modificationDate.orElse(distribution.modificationDate))

  def merge(code: String): AMHDistributionCpyEntity =
    AMHDistributionCpyEntity(code,
      sequence, environment, version, "Noname.xml",
      active, dataOwner, lockCode, description,
      selectionGroup, layoutTemplate, copies, name,
      backends.map(_.merge), rules.map(_.merge),
      creationUserId, getCreationDateValue, modificationUserId, modificationDate)
}

case class AMHDistributionCpyRuleEntity(code: String, sequence: Long, ruleCode: String,
                                        dataOwner: Option[String] = None, lockCode: Option[String] = None,
                                        environment: String,
                                        version: String,
                                        creationUserId : Option[String] = Some("undefined_user"), creationDate : DateTime = DateTime.now, modificationUserId : Option[String] = Some("undefined_user"), modificationDate : Option[DateTime] = None
                                       ) extends AMHRule with EntityAudit {

  def toES(expression: String = ""): AMHDistributionCpyRuleES = AMHDistributionCpyRuleES(ruleCode, dataOwner.getOrElse(""), lockCode.getOrElse(""), sequence, expression)
}

case class AMHDistributionCpyRuleEntityUpdate(code: String, sequence: Long, ruleCode: String,
                                        dataOwner: Option[String] = None, lockCode: Option[String] = None,
                                        environment: String,
                                        version: String,
                                        creationUserId : Option[String] = Some("undefined_user"), creationDate : Option[DateTime] = None, modificationUserId : Option[String] = Some("undefined_user"), modificationDate : Option[DateTime] = None
                                       ) extends AMHRule with EntityUpdateAudit {


  def merge(assignRules: Seq[AMHDistributionCpyRuleEntity]): AMHDistributionCpyRuleEntity = {
    merge(assignRules.find(_.ruleCode == ruleCode))
  }

  private def merge(assignOrigRule: Option[AMHDistributionCpyRuleEntity]) : AMHDistributionCpyRuleEntity = assignOrigRule match {
    case Some(assignRule) =>
      AMHDistributionCpyRuleEntity(code, sequence, ruleCode, dataOwner.orElse(assignRule.dataOwner)
        , lockCode.orElse(assignRule.lockCode), environment, version,
        creationUserId.orElse(assignRule.creationUserId), assignRule.creationDate, modificationUserId.orElse(assignRule.modificationUserId), modificationDate.orElse(assignRule.modificationDate))
    case None => merge
  }

  def merge: AMHDistributionCpyRuleEntity =
    AMHDistributionCpyRuleEntity(code, sequence, ruleCode, dataOwner
      , lockCode, environment, version,
      creationUserId, getCreationDateValue, modificationUserId, modificationDate)

}

case class AMHDistributionCpyBackendEntity(code: String, backCode: String, backDirection: String,
                                           dataOwner: Option[String] = None, lockCode: Option[String] = None,
                                           environment: String = "UNKNOWN",
                                           version: String = "DEFAULT",
                                           creationUserId : Option[String] = Some("undefined_user"), creationDate : DateTime = DateTime.now, modificationUserId : Option[String] = Some("undefined_user"), modificationDate : Option[DateTime] = None
                                          ) extends EntityAudit {

  def toES: AMHDistributionCpyBackES = AMHDistributionCpyBackES(backCode,
    backDirection, dataOwner.getOrElse(""), lockCode.getOrElse(""))
}

case class AMHDistributionCpyBackendEntityUpdate(code: String, backCode: String, backDirection: String,
                                           dataOwner: Option[String] = None, lockCode: Option[String] = None,
                                           environment: String = "UNKNOWN",
                                           version: String = "DEFAULT",
                                           creationUserId : Option[String] = Some("undefined_user"), creationDate : Option[DateTime] = None, modificationUserId : Option[String] = Some("undefined_user"), modificationDate : Option[DateTime] = None
                                          ) extends EntityUpdateAudit {
  def merge(assignBackends: Seq[AMHDistributionCpyBackendEntity]): AMHDistributionCpyBackendEntity = {
    merge(assignBackends.find(_.backCode == backCode))
  }

  private def merge(assignOrigRule: Option[AMHDistributionCpyBackendEntity]) : AMHDistributionCpyBackendEntity = assignOrigRule match {
    case Some(assignRule) =>
      AMHDistributionCpyBackendEntity(code, backCode, backDirection, dataOwner.orElse(assignRule.dataOwner)
        , lockCode.orElse(assignRule.lockCode), environment, version,
        creationUserId.orElse(assignRule.creationUserId), assignRule.creationDate, modificationUserId.orElse(assignRule.modificationUserId), modificationDate.orElse(assignRule.modificationDate))
    case None => merge
  }

  def merge: AMHDistributionCpyBackendEntity =
    AMHDistributionCpyBackendEntity(code, backCode, backDirection, dataOwner
      , lockCode, environment, version,
      creationUserId, getCreationDateValue, modificationUserId, modificationDate)

}

case class AMHDistributionCpyBackES(code: String, direction: String,
                                    dataOwner: String, lockCode: String)

case class AMHDistributionCpyRuleES(code: String, dataOwner: String, lockCode: String, sequence: Long, expression: String)

case class AMHDistributionCpyES(active: Boolean,
                                code: String, dataOwner: Option[String], description: Option[String], lockCode: Option[String]
                                , sequence: Long, environment: String, version: String, copies: Option[Long], name: Option[String], backends: Seq[AMHDistributionCpyBackES], rules: Seq[AMHDistributionCpyRuleES]) {

  def toOverview: Seq[AMHAssignmentOverviewES] = {

    def next(assign: AMHAssignmentOverviewES, backends: Seq[AMHDistributionCpyBackES], rules: Seq[AMHDistributionCpyRuleES], assigns: Seq[AMHAssignmentOverviewES]): Seq[AMHAssignmentOverviewES] = {
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
    next(AMHAssignmentOverviewES(active, code, sequence),orderedBackends, orderedRules, Seq())
  }


}

//object testingOver extends  App {
//  val backends = Seq(AMHDistributionCpyBackES("codeB1","d","",""), AMHDistributionCpyBackES("codeB2","d","",""), AMHDistributionCpyBackES("codeB3","d","",""), AMHDistributionCpyBackES("codeB6","d","",""), AMHDistributionCpyBackES("codeB5","d","",""))
//  val rules = Seq(AMHDistributionCpyRuleES("r1","","",10,"expre1"), AMHDistributionCpyRuleES("r2","","",20,"expre2"), AMHDistributionCpyRuleES("r3","","",30,"expre3"), AMHDistributionCpyRuleES("r4","","",40,"expre4"))
//  val dcES = AMHDistributionCpyES(true,"a1",None, None, None, 20,"","",None,None,backends, rules)
//  logger.debug("salida")
//  dcES.toOverview.foreach(logger.debug(_))
//
//}
