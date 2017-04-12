package com.groupaxis.groupsuite.user.service.interpreter

import com.groupaxis.groupsuite.persistence.datastore.jdbc.Database
import com.groupaxis.groupsuite.user._
import com.groupaxis.groupsuite.user.model.common.ID
import com.groupaxis.groupsuite.user.model.{Permission, Profile, User}
import com.groupaxis.groupsuite.user.repository.interpreter.UserRepositoryInterpreter
import com.groupaxis.groupsuite.user.service.UserService

class UserServiceInterpreter extends UserRepositoryInterpreter with UserService[User, Profile, Permission] {

  override def create
  (login: ID[String], firstName: String, lastName: String, active: Boolean,
   profiles: Option[Seq[Profile]],
   email: Option[String]): UserOperation[User] = for {
      _ <- save(User(login, firstName, lastName, active, profiles, email))
      found <- findUserById(login)
  } yield found

  private def updateImpl(user : User) : User = user

  override def update(login: ID[String], f: User => User): UserOperation[User] = for {
    _ <- updateUser(login, updateImpl)
    found <- findUserById(login)
  } yield {
    found
  }

  private def activeImpl(user : User, active : Boolean) : User = user.copy(active = active)

  override def activateUser(login: ID[String]): UserOperation[User] = for {
    _ <- updateActive(login, active = true, activeImpl)
    found <- findUserById(login)
  } yield {
    found
  }

  override def deactivateUser(login: ID[String]): UserOperation[User] = for {
    _ <- updateActive(login, active = false, activeImpl)
    found <- findUserById(login)
  } yield {
    found
  }

}

object UserServiceInterpreter {
  def apply() = new UserServiceInterpreter
}
