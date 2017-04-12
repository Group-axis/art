package com.groupaxis.groupsuite.user.repository

import com.groupaxis.groupsuite.user.UserRepo
import com.groupaxis.groupsuite.user.model.UserAuth
import org.joda.time.DateTime

trait UserAuthRepository {
  import scala.concurrent.ExecutionContext.Implicits.global
  import scalaz._
  import Scalaz._
  import \/._
  import OptionT._

  def findUserAuthById(login: String): UserRepo[Option[UserAuth]]

  def save(user: UserAuth): UserRepo[UserAuth]

  def all: UserRepo[Seq[UserAuth]]

  def deleteById(login: String): UserRepo[UserAuth]

  def updateUser(login: String, f: UserAuth => UserAuth): UserRepo[UserAuth] = {
    for {
      user <- findUserAuthById(login)
      updated <- save(f(user.get))
    } yield updated
  }
  private def update3[A, B, C](login: String, value1: A, value2: B, value3: C, f: (UserAuth, A, B, C) => UserAuth): UserRepo[UserAuth] =
    for {
      user <- findUserAuthById(login)
      updated <- save(f(user.get, value1, value2, value3))
    } yield updated

  private def update1[A](login: String, value: A, f: (UserAuth, A) => UserAuth): UserRepo[UserAuth] =
    for {
      user <- findUserAuthById(login)
      updated <- save(f(user.get, value))
    } yield updated

  def updatePassword(login: String, password: String, f: (UserAuth, String) => UserAuth): UserRepo[UserAuth] =
    update1[String](login, password, f)

  def updateLastConnection(login: String, lastConnection: DateTime, f: (UserAuth, DateTime) => UserAuth): UserRepo[UserAuth] =
    update1[DateTime](login, lastConnection, f)

  def updateLockStatus(login: String, lockStatus: String, f: (UserAuth, String) => UserAuth): UserRepo[UserAuth] =
    update1[String](login, lockStatus, f)

  def updateFailedConnection(login: String, f: (UserAuth) => UserAuth): UserRepo[UserAuth] =
    updateUser(login, f)

  def resetPassword(login: String, userUpdate: String, updateDate: DateTime, f: (UserAuth, String, String, DateTime) => UserAuth): UserRepo[UserAuth] =
    update3[String, String, DateTime](login, login, userUpdate, updateDate, f)
}


