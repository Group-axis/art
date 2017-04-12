package com.groupaxis.groupsuite.user.service.interpreter

import com.groupaxis.groupsuite.user.{FREEUserAuthRepo, UserAuthRepo}
import com.groupaxis.groupsuite.user.model.UserAuth
import com.groupaxis.groupsuite.user.model.common._
import com.groupaxis.groupsuite.user.repository.FreeUserAuthRepository
import com.groupaxis.groupsuite.user.service.{FreeUserAuthService, UserAuthService}
import org.joda.time.DateTime


object FreeUserAuthServiceInterpreter extends FreeUserAuthService[UserAuth] with FreeUserAuthRepository {

  override def create(login: ID[String], password: String): FREEUserAuthRepo[UserAuth] = for {
    created <- store(UserAuth(login, password, None, None, None).encryptPassword)
    found <- query(login)
  } yield found

  private def resetPasswordImpl(userAuth: UserAuth, newPassword: String, userUpdate: String, dateUpdate: DateTime) = {
    userAuth.copy(password = newPassword, resetUserId = Some(userUpdate), resetDate = Some(dateUpdate))
  }

  override def resetPassword(login: ID[String], userUpdate: ID[String], dateUpdate: DateTime): FREEUserAuthRepo[UserAuth] = for {
    stored <- resetPassword(login, userUpdate, dateUpdate, resetPasswordImpl)
    found <- query(login)
  } yield found

  private def updatePasswordImpl(userAuth: UserAuth, newPassword: String) = {
    userAuth.copy(password = newPassword)
  }

  override def updatePassword(login: ID[String], newPassword: String): FREEUserAuthRepo[UserAuth] = for {
    stored <- updatePassword(login, newPassword, updatePasswordImpl)
    found <- query(login)
  } yield found

  private def updateLastConnectionImpl(userAuth: UserAuth, newLastConnection: DateTime) = {
    userAuth.copy(lastConnection = Some(newLastConnection))
  }

  override def updateLastConnection(login: ID[String], newLastConnection: DateTime): FREEUserAuthRepo[UserAuth] = for {
    stored <- updateLastConnection(login, newLastConnection, updateLastConnectionImpl)
    found <- query(login)
  } yield found

  private def updateLockStatusImpl(userAuth: UserAuth, lockStatus: String) =
    lockStatus match {
      case "N" => userAuth.copy(lockStatus = lockStatus)
      case "Y" => userAuth.copy(lockStatus = lockStatus)
      case _ => throw new IllegalStateException(s"Lock status $lockStatus is not valid")
    }


  override def updateLockStatus(login: ID[String], newStatus: String): FREEUserAuthRepo[UserAuth] = for {
    updated <- updateLockStatus(login, newStatus, updateLockStatusImpl)
    found <- query(login)
  } yield found

  private def updateFailedConnectionImpl(userAuth: UserAuth): UserAuth =
    userAuth match {
      case UserAuth(_, _, _, _, _, _, numConnectionFailed) if numConnectionFailed < 2 =>
        userAuth.copy(numConnectionFailed = numConnectionFailed + 1)
      case UserAuth(_, _, _, _, _, _, numConnectionFailed) if numConnectionFailed == 2 =>
        userAuth.copy(lockStatus = "Y", numConnectionFailed = 3)
      case _ => userAuth
    }

  override def updateFailedConnection(login: ID[String]): FREEUserAuthRepo[UserAuth] = for {
    updated <- updateFailedConnection(login, updateFailedConnectionImpl)
    found <- query(login)
  } yield found

  def validUser(login: ID[String], password: String): FREEUserAuthRepo[UserAuth] =
    for {
      found <- validate(login, password)
    } yield found

}
