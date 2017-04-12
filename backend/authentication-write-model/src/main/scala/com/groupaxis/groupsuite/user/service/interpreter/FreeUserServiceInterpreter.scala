package com.groupaxis.groupsuite.user.service.interpreter

import com.groupaxis.groupsuite.user.FREEUserRepo
import com.groupaxis.groupsuite.user.model.common.ID
import com.groupaxis.groupsuite.user.model.{Permission, Profile, User}
import com.groupaxis.groupsuite.user.repository.FreeUserRepository
import com.groupaxis.groupsuite.user.service.FreeUserService

object FreeUserServiceInterpreter extends FreeUserService[User, Profile, Permission]  with FreeUserRepository {
  override def create(login: ID[String], firstName: String, lastName: String, active: Boolean, profiles: Option[Seq[Profile]], email: Option[String])
  : FREEUserRepo[User] =
//  { (userRepo: UserRepository) =>
//    userRepo.query(login) match {
//    case \/-(Some(a)) => NonEmptyList(s"Already existing user with login $login").left[User]
//  }
    for {
    _ <- store (User (login, firstName, lastName, active, profiles, email) )
    found <- query (login)
   } yield found
//  }

  private def updateImpl(user : User) : User = user

  override def update(login: ID[String], f: User => User): FREEUserRepo[User] = for {
    _ <- updateUser(login, updateImpl)
    found <- query(login)
  } yield found

  private def activeImpl(user : User, active : Boolean) : User = user.copy(active = active)

  override def activateUser(login: ID[String]): FREEUserRepo[User] = for {
    _ <- updateActive(login, active = true, activeImpl)
    found <- query(login)
  } yield found

  override def deactivateUser(login: ID[String]): FREEUserRepo[User] = for {
    _ <- updateActive(login, active = false, activeImpl)
    found <- query(login)
  } yield found
}
