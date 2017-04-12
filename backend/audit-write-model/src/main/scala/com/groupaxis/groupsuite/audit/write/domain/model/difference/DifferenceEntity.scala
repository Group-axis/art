package com.groupaxis.groupsuite.audit.write.domain.model.difference

import com.groupaxis.groupsuite.authentication.write.domain.model.user.UserEntity
import org.joda.time.DateTime


/** ******  DIFF  *************/

case class DifferenceEntity(id: Int, user: String, date: DateTime, entityType: String, entityId: String, changes: Option[Seq[ChangeEntity]] = None) {

}

case class ChangeEntity(id: Int, diffId: Int, changeType: String, propertyName: String, containerChanges : Option[Seq[ContainerChangeEntity]] = None, valueChange : Option[ValueChangeEntity] = None) {

}

case class ContainerChangeEntity(id: Int, changeId: Int, value: String, addOrRemove: String) {

}

case class ValueChangeEntity(id: Int, changeId: Int, oldValue: String, newValue: String) {

}


