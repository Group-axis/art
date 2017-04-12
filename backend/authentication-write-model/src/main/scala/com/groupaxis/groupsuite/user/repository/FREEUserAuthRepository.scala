package com.groupaxis.groupsuite.user.repository

import com.groupaxis.groupsuite.user.{FREEUserAuthRepo, UserAuthRepo}
import com.groupaxis.groupsuite.user.model.UserAuth
import org.joda.time.DateTime

import scalaz.Free

sealed trait UserAuthRepoF[A]

case class FindUserAuthById(login: String) extends UserAuthRepoF[UserAuth]

case class FindUserAuthByIdAndPassword(login: String, password : String) extends UserAuthRepoF[UserAuth]

case class SaveUserAuth(user: UserAuth) extends UserAuthRepoF[Unit]

case class DeleteUserAuth(login: String) extends UserAuthRepoF[Unit]

trait FreeUserAuthRepository {

  def store(user: UserAuth) : FREEUserAuthRepo[Unit] =
  Free.liftF(SaveUserAuth(user))

  def query(login: String): FREEUserAuthRepo[UserAuth] =
    Free.liftF(FindUserAuthById(login))

  def delete(login: String): FREEUserAuthRepo[Unit] =
    Free.liftF(DeleteUserAuth(login))

  def validate(login: String, password : String) : FREEUserAuthRepo[UserAuth] =
    Free.liftF(FindUserAuthByIdAndPassword(login, password))

  def update(login: String, f: UserAuth => UserAuth) : FREEUserAuthRepo[Unit] =
    for {
      user <- query(login)
      store <- store(f(user))
    } yield ()

  def updatePassword(login: String, password : String ,f: (UserAuth, String) => UserAuth) : FREEUserAuthRepo[Unit] =
    for {
      user <- query(login)
      store <- store(f(user, password))
    } yield ()

  def updateLastConnection(login: String, lastConnection: DateTime ,f: (UserAuth, DateTime) => UserAuth) : FREEUserAuthRepo[Unit] =
    for {
      user <- query(login)
      store <- store(f(user, lastConnection))
    } yield ()

  def updateLockStatus(login: String, lockStatus: String ,f: (UserAuth, String) => UserAuth) : FREEUserAuthRepo[Unit] =
    for {
      user <- query(login)
      store <- store(f(user, lockStatus))
    } yield ()

  def updateFailedConnection(login: String, f: (UserAuth) => UserAuth) : FREEUserAuthRepo[Unit] =
    for {
      user <- query(login)
      store <- store(f(user))
    } yield ()

  def resetPassword(login: String, userUpdate: String , updateDate : DateTime ,f: (UserAuth, String, String, DateTime) => UserAuth) : FREEUserAuthRepo[Unit] =
    for {
      user <- query(login)
      store <- store(f(user, login, userUpdate, updateDate))
    } yield ()

}
