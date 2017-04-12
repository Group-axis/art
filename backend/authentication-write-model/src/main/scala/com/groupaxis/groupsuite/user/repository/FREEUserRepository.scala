package com.groupaxis.groupsuite.user.repository

import com.groupaxis.groupsuite.user.FREEUserRepo
import com.groupaxis.groupsuite.user.model.User

import scalaz.Free

sealed trait UserRepoF[A]

case class QueryUser(login: String) extends UserRepoF[User]

case class StoreUser(user: User) extends UserRepoF[Unit]

case class DeleteUser(login: String) extends UserRepoF[Unit]

trait FreeUserRepository {

  def store(user : User) : FREEUserRepo[Unit] =
  Free.liftF(StoreUser(user))

  def query(login: String): FREEUserRepo[User] =
    Free.liftF(QueryUser(login))

  def delete(login: String): FREEUserRepo[Unit] =
    Free.liftF(DeleteUser(login))

  def updateUser(login: String, f: User => User) : FREEUserRepo[Unit] =
    for {
      user <- query(login)
      store <- store(f(user))
    } yield store

  def updateActive(login: String, active : Boolean,f: (User, Boolean) => User) : FREEUserRepo[Unit] =
    for {
      user <- query(login)
      stored <- store(f(user, active))
    } yield ()
}
