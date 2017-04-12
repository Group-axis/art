package com.groupaxis.groupsuite.user.service

import com.groupaxis.groupsuite.user.{FREEUserRepo, UserRepo}
import com.groupaxis.groupsuite.user.model.common._

trait FreeUserService[User, Profile, Permission] {

  def create(login : ID[String], firstName : String, lastName : String, active : Boolean, profiles : Option [Seq[Profile]], email : Option[String]) : FREEUserRepo[User]

  def update(login : ID[String], f: User => User) : FREEUserRepo[User]

  def activateUser(login : ID[String]) : FREEUserRepo[User]

  def deactivateUser(login : ID[String]) : FREEUserRepo[User]

}
