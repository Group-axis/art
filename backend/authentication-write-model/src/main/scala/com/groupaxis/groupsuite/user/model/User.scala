package com.groupaxis.groupsuite.user.model

import java.util.Calendar

import com.groupaxis.groupsuite.user.toMDS5
import org.joda.time.DateTime

object common {
  type Amount = BigDecimal
  type ID[T] = T

  val today = Calendar.getInstance.getTime
}

import com.groupaxis.groupsuite.user.model.common._

case class Permission(permissionId: ID[Long], name: String, active: Boolean)

case class Profile(profileId: ID[Long], name: String, active: Boolean, permissions: Option[Seq[Permission]])

case class User(login: ID[String], firstName: String, lastName: String, active: Boolean, profiles: Option[Seq[Profile]], email: Option[String]) {
  def update(user: User) = User(login, user.firstName, user.lastName, user.active, user.profiles.orElse(profiles), user.email.orElse(email))
}

case class UserAuth(login: ID[String], password: String, resetUserId: Option[String], resetDate: Option[DateTime], lastConnection: Option[DateTime], lockStatus: String = "N", numConnectionFailed: Integer = 0) {

  def encryptPassword: UserAuth = copy(password = toMDS5(password))

}
