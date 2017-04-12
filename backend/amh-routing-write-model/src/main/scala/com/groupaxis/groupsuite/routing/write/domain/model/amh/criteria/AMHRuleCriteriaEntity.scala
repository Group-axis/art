package com.groupaxis.groupsuite.routing.write.domain.model.amh.criteria

import com.groupaxis.groupsuite.persistence.util.{EntityAudit, EntityUpdateAudit}
import org.apache.logging.log4j.scala.Logging
import org.joda.time.DateTime

case class AMHRuleCriteriaEntity(code: String, searchCode: String,  description: String, children : Seq[AMHRuleCriteriaEntity] = Seq(),
                         creationUserId : Option[String] = Some("undefined_user"), creationDate : DateTime = DateTime.now, modificationUserId : Option[String] = None, modificationDate : Option[DateTime] = None)

  extends EntityAudit with Logging {

  def toES: AMHRuleCriteriaEntityES = AMHRuleCriteriaEntityES(code, searchCode, description, children.map(_.toES))

}

case class AMHRuleCriteriaEntityES(code: String, searchCode: String,  description: String, children : Seq[AMHRuleCriteriaEntityES] = Seq()) {

}

case class AMHRuleCriteriaEntityUpdate( searchCode: String,  description: String, children : Seq[AMHRuleCriteriaEntity] = Seq(),
                                creationUserId : Option[String] = Some(""),
                                creationDate : Option[DateTime] = None,
                                modificationUserId : Option[String] = Some(""),
                                modificationDate : Option[DateTime] = None)
  extends EntityUpdateAudit  {

  def merge(criteria: AMHRuleCriteriaEntity): AMHRuleCriteriaEntity = {
    AMHRuleCriteriaEntity(criteria.code, searchCode, description,children,
      creationUserId.orElse(criteria.creationUserId), criteria.creationDate,
      modificationUserId.orElse(criteria.modificationUserId), modificationDate.orElse(criteria.modificationDate))
  }

  def merge(code: String): AMHRuleCriteriaEntity = {
    AMHRuleCriteriaEntity(code, searchCode, description,children,
      creationUserId, getCreationDateValue, modificationUserId, modificationDate)
  }
}
