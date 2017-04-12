package com.groupaxis.groupsuite.authentication.write.domain.model.user

trait UserESRequest
trait UserESResponse
trait UserRequest
trait UserResponse

object UserMessages {

  //commands
  case class CreateUser(id: String, user: UserEntityUpdate) extends UserRequest
  case class UpdateRule(id : String, user: UserEntityUpdate) extends UserRequest
  case class FindAllUsers() extends UserRequest
  case class FindUserByUsername(id : String) extends UserRequest
  case class InsertUserES(user: UserEntityES) extends UserESRequest
  case class UpdateUserES(user: UserEntityES) extends UserESRequest

  //events
  case class UserFound(user: Option[UserEntity]) extends UserResponse
  case class UserCreated(user : UserEntity) extends UserResponse
  case class UserUpdated(user : UserEntity) extends UserResponse
  case class UsersFound(users: Seq[UserEntity]) extends UserResponse
  case class UserESInserted(user: UserEntityES) extends UserESResponse
  case class UserESUpdated(user: UserEntityES) extends UserESResponse

}