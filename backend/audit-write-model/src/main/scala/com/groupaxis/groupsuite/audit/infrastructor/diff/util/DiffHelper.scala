package com.groupaxis.groupsuite.audit.infrastructor.diff.util

import java.lang.reflect.Field

import com.groupaxis.groupsuite.audit.write.domain.model.difference.{ChangeEntity, DifferenceEntity, DifferenceMessages, ValueChangeEntity}
import com.groupaxis.groupsuite.authentication.write.domain.audit.messages.AuthenticationAuditMessages.CreateUser
import com.groupaxis.groupsuite.authentication.write.domain.model.profile.ProfileEntity
import com.groupaxis.groupsuite.authentication.write.domain.model.user.UserEntity
import com.groupaxis.groupsuite.routing.write.domain.model.amh.assignment.AMHAssignmentEntity
import com.groupaxis.groupsuite.routing.write.domain.model.amh.distribution.copy.AMHDistributionCpyEntity
import com.groupaxis.groupsuite.routing.write.domain.model.amh.feedback.distribution.copy.AMHFeedbackDistributionCpyEntity
import com.groupaxis.groupsuite.routing.write.domain.model.amh.rule.AMHRule
import com.groupaxis.groupsuite.simulator.write.domain.model.job.JobEntity
import com.groupaxis.groupsuite.simulator.write.domain.model.swift.msg.SwiftMessageEntity
import org.apache.logging.log4j.scala.Logging
import org.joda.time.DateTime

object DiffHelper extends Logging {



  def compareRule(original : AMHRule, modified : AMHRule, username: String = "", now : DateTime =  DateTime.now) : DifferenceEntity = {

    DifferenceEntity(-1,username, now, "","")
  }

  def compareBackend(original : AMHAssignmentEntity, modified : AMHAssignmentEntity, username: String = "", now : DateTime =  DateTime.now) : DifferenceEntity = {

    DifferenceEntity(-1,username, now, "","")
  }

  def compareFeedback(original : AMHFeedbackDistributionCpyEntity, modified : AMHFeedbackDistributionCpyEntity, username: String = "", now : DateTime =  DateTime.now) : DifferenceEntity = {

    DifferenceEntity(-1,username, now, "","")
  }

  def compareDistribution(original : AMHDistributionCpyEntity, modified : AMHDistributionCpyEntity, username: String = "", now : DateTime =  DateTime.now) : DifferenceEntity = {

    DifferenceEntity(-1,username, now, "","")
  }

  def compareUser(original : UserEntity, originalProfiles : Seq[ProfileEntity], modified : UserEntity, modifiedProfiles : Seq[ProfileEntity], username: String = "", now : DateTime =  DateTime.now) : DifferenceEntity = {

    DifferenceEntity(-1,username, now, "","")
  }

  def compareSwiftMessage(original : SwiftMessageEntity, modified : SwiftMessageEntity, username: String = "", now : DateTime =  DateTime.now) : DifferenceEntity = {

    DifferenceEntity(-1,username, now, "","")
  }

  def compareGroupMessage(original : SwiftMessageEntity, modified : SwiftMessageEntity, username: String = "", now : DateTime =  DateTime.now) : DifferenceEntity = {

    DifferenceEntity(-1,username, now, "","")
  }

  def compareJob(original : JobEntity, modified : JobEntity, username: String = "", now : DateTime =  DateTime.now) : DifferenceEntity = {

    DifferenceEntity(-1,username, now, "","")
  }


  def newChanges(changeType: String, obj : Any, fieldsTofilter : Seq[String]) : Seq[ChangeEntity] = {
    val fields = obj.getClass.getDeclaredFields.filter(f => fieldsTofilter.contains(f.getName))
    val change = ChangeEntity(-1,-1,changeType,"")
    fields.map(field => {
        change.copy(propertyName = field.getName, valueChange = newValueChange(obj, field))
      })
  }

  def newValueChange(obj : Any, field : Field) : Option[ValueChangeEntity] = {
    //val fields = obj.getClass.getDeclaredFields.find(f => field equals f.getName)
    val change = ValueChangeEntity(-1,-1,"","")
    val value = try {
      field.setAccessible(true)
      (field.get(obj) match {
        case null => ""
        case a : Object => a
      }).toString
    } catch { case e : IllegalArgumentException =>logger.debug(s"error ${e.getMessage}");""}

    if (value.isEmpty) None else Some(change.copy(newValue = value.toString))

  }

  def newValueChanges(obj : Any, fieldsTofilter : Seq[String]) : Seq[ValueChangeEntity] = {
    val fields = obj.getClass.getDeclaredFields.filter(f => fieldsTofilter.contains(f.getName))
    val change = ValueChangeEntity(-1,-1,"","")
    fields
      .map(field => {
        field.setAccessible(true)
        change.copy(newValue = field.get(obj).toString)
      })

  }

  def toCreateUserDiff(createUser: CreateUser) : DifferenceEntity = {
    val userEntity = createUser.newUserEntity
    val profiles = createUser.newUserProfiles
    val changes = newChanges(DifferenceMessages.NEW_CHANGE_TYPE, userEntity ,Seq("id", "firstName", "lastName", "active", "email"))
   DifferenceEntity(-1, userEntity.id, DateTime.now, DifferenceMessages.USER_ENTITY_TYPE, userEntity.id, Some(changes))
 //TODO: profiles is missing.
  }

}

object test extends App with Logging {
  /*
  * id: String,
    firstName: String,
    lastName: String,
    email: Option[String],
    active : Char,
  * */

  val user = UserEntity("irachname","irach", "ramos",Some("iilish@hotmail.com"),'Y', None, None, None, None )

  val changesFound = DiffHelper.newChanges(DifferenceMessages.NEW_CHANGE_TYPE, user ,Seq("id", "firstName", "lastName", "active", "email"))

  logger.debug(changesFound)
}