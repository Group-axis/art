package com.groupaxis.groupsuite.user.service

import com.groupaxis.groupsuite.user._
import com.groupaxis.groupsuite.user.model.common._
import org.joda.time.DateTime

trait UserAuthService[UserAuth] {

  def create(login: ID[String], password: String): UserAuthRepo[UserAuth]

  def resetPassword(login: ID[String], userUpdate: ID[String], dateUpdate: DateTime): UserAuthRepo[UserAuth]

  def updatePassword(login: ID[String], newPassword: String): UserAuthRepo[UserAuth]

  def updateLastConnection(login: ID[String], newLastConnection: DateTime): UserAuthRepo[UserAuth]

  def updateLockStatus(login: ID[String], newStatus: String): UserAuthRepo[UserAuth]

  def updateFailedConnection(login: ID[String]): UserAuthRepo[UserAuth]

  def validUser(login: ID[String], password: String): UserAuthRepo[UserAuth]
}
