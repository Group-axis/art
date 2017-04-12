package com.groupaxis.groupsuite.authentication.write.domain.model.user

import com.groupaxis.groupsuite.authentication.write.domain.model.user.UserMessages.{UserCreated, UserFound, UserUpdated, UsersFound}

trait UserWriteRepository {

  def getUsers(): Either[String, UsersFound]

  def getUserByUsername(id : String): Either[String, UserFound]

  def createUser(user: UserEntity): Either[String, UserCreated]

  def updateUser(id: String, userUpdate: UserEntityUpdate): Either[String, UserUpdated]

  def deleteUser(id: String): Either[String, Int]

}