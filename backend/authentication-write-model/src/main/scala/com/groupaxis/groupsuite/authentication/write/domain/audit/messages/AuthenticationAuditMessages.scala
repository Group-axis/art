package com.groupaxis.groupsuite.authentication.write.domain.audit.messages

import com.groupaxis.groupsuite.authentication.write.domain.model.profile.ProfileEntity
import com.groupaxis.groupsuite.authentication.write.domain.model.user.{UserDetailEntity, UserEntity}
import org.joda.time.DateTime


object AuthenticationAuditMessages {

  trait AuthenticationAuditRequest

  trait AuthenticationAuditResponse


  //commands
  case class CreateUser(date: DateTime, newUserEntity: UserEntity, newUserProfiles: Seq[ProfileEntity]) extends AuthenticationAuditRequest

  case class UpdateUser(date: DateTime, oldUserEntity: UserEntity, oldUserProfiles: Seq[ProfileEntity], newUserEntity: UserEntity, newUserProfiles: Seq[ProfileEntity]) extends AuthenticationAuditRequest

  case class DeleteUser(date: DateTime, deleteUserEntity: UserEntity, deleteUserDetailEntity: UserDetailEntity) extends AuthenticationAuditRequest

  case class CreateBlockUser(date: DateTime, blockUserEntity: UserDetailEntity, newUserDetailEntity: UserDetailEntity) extends AuthenticationAuditRequest

  case class CreateUnblockUser(date: DateTime, unblockUserEntity: UserDetailEntity, newUserDetailEntity: UserDetailEntity) extends AuthenticationAuditRequest

  case class CreateLoginUser(date: DateTime, connUserEntity: UserEntity, newUserDetailEntity: UserDetailEntity) extends AuthenticationAuditRequest

  case class CreateLogoutUser(date: DateTime, discConnUserEntity: UserEntity, newUserDetailEntity: UserDetailEntity) extends AuthenticationAuditRequest

  case class CreateResetPassword(date: DateTime, resetUserEntity: UserEntity) extends AuthenticationAuditRequest

  case class CreateFailedConnUser(date: DateTime, failedUser: UserEntity) extends AuthenticationAuditRequest

  //events
  case class AuthenticationUpdateDone() extends AuthenticationAuditResponse

  case class AuthenticationUpdateFailed(msg: String) extends AuthenticationAuditResponse

  case class AuthenticationCreationDone() extends AuthenticationAuditResponse

  case class AuthenticationCreationFailed(msg: String) extends AuthenticationAuditResponse

  case class AuthenticationDeletionDone() extends AuthenticationAuditResponse

  case class AuthenticationDeletionFailed(msg: String) extends AuthenticationAuditResponse

}
