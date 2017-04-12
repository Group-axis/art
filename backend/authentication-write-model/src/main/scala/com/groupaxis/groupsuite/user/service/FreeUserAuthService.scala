package com.groupaxis.groupsuite.user.service

import com.groupaxis.groupsuite.user._
import com.groupaxis.groupsuite.user.model.common._
import org.joda.time.DateTime

trait FreeUserAuthService[UserAuth] {

  def create(login: ID[String], password: String): FREEUserAuthRepo[UserAuth]

  def resetPassword(login: ID[String], userUpdate: ID[String], dateUpdate: DateTime): FREEUserAuthRepo[UserAuth]

  def updatePassword(login: ID[String], newPassword: String): FREEUserAuthRepo[UserAuth]

  def updateLastConnection(login: ID[String], newLastConnection: DateTime): FREEUserAuthRepo[UserAuth]

  def updateLockStatus(login: ID[String], newStatus: String): FREEUserAuthRepo[UserAuth]

  def updateFailedConnection(login: ID[String]): FREEUserAuthRepo[UserAuth]

  def validUser(login: ID[String], password: String): FREEUserAuthRepo[UserAuth]
}
