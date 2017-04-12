package com.groupaxis.groupsuite.user.service


import com.groupaxis.groupsuite.user._
import com.groupaxis.groupsuite.user.model.common._

trait UserService[User, Profile, Permission] {

  def create(login : ID[String], firstName : String, lastName : String, active : Boolean, profiles : Option [Seq[Profile]], email : Option[String]) : UserOperation[User]

  def update(login : ID[String], f: User => User) : UserOperation[User]

  def activateUser(login : ID[String]) : UserOperation[User]

  def deactivateUser(login : ID[String]) : UserOperation[User]

}
