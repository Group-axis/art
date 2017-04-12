package com.groupaxis.groupsuite.user.service.interpreter

import com.groupaxis.groupsuite.persistence.datastore.jdbc.Database
import com.groupaxis.groupsuite.user.UserAuthRepo
import com.groupaxis.groupsuite.user.model.UserAuth
import com.groupaxis.groupsuite.user.model.common._
import com.groupaxis.groupsuite.user.repository.UserAuthRepository
import com.groupaxis.groupsuite.user.service.UserAuthService
import org.joda.time.DateTime

import scalaz.-\/


object UserAuthServiceInterpreter
//
//class UserAuthServiceInterpreter(database : Database) extends UserAuthRepository with UserAuthService[UserAuth]  {
//
//  override def create(login: ID[String], password: String): UserAuthRepo[UserAuth] = for {
//    created <- save(UserAuth(login, password, None, None, None).encryptPassword)
//    found <- findUserAuthById(login)
//  } yield {
//    found match {case ou : Option[UserAuth] => ou.get}
//  }
//
//  private def resetPasswordImpl(userAuth: UserAuth, newPassword: String, userUpdate: String, dateUpdate: DateTime) = {
//    userAuth.copy(password = newPassword, resetUserId = Some(userUpdate), resetDate = Some(dateUpdate))
//  }
//
//  override def resetPassword(login: ID[String], userUpdate: ID[String], dateUpdate: DateTime): UserAuthRepo[UserAuth] = for {
//    stored <- resetPassword(login, userUpdate, dateUpdate, resetPasswordImpl)
//    found <- findUserAuthById(login)
//  } yield {
//    found match {case ou : Option[UserAuth] => ou.get}
//  }
//
//  private def updatePasswordImpl(userAuth: UserAuth, newPassword: String) = {
//    userAuth.copy(password = newPassword)
//  }
//
//  override def updatePassword(login: ID[String], newPassword: String): UserAuthRepo[UserAuth] = for {
//    stored <- updatePassword(login, newPassword, updatePasswordImpl)
//    found <- findUserAuthById(login)
//  } yield {
//    found match {case ou : Option[UserAuth] => ou.get}
//  }
//
//  private def updateLastConnectionImpl(userAuth: UserAuth, newLastConnection: DateTime) = {
//    userAuth.copy(lastConnection = Some(newLastConnection))
//  }
//
//  override def updateLastConnection(login: ID[String], newLastConnection: DateTime): UserAuthRepo[UserAuth] = for {
//    stored <- updateLastConnection(login, newLastConnection, updateLastConnectionImpl)
//    found <- findUserAuthById(login)
//  } yield {
//    found match {case ou : Option[UserAuth] => ou.get}
//  }
//
//  private def updateLockStatusImpl(userAuth: UserAuth, lockStatus: String) =
//    lockStatus match {
//      case "N" => userAuth.copy(lockStatus = lockStatus)
//      case "Y" => userAuth.copy(lockStatus = lockStatus)
//      case _ => throw new IllegalStateException(s"Lock status $lockStatus is not valid")
//    }
//
//
//  override def updateLockStatus(login: ID[String], newStatus: String): UserAuthRepo[UserAuth] = for {
//    updated <- updateLockStatus(login, newStatus, updateLockStatusImpl)
//    found <- findUserAuthById(login)
//  } yield {
//    found match {case ou : Option[UserAuth] => ou.get}
//  }
//
//  private def updateFailedConnectionImpl(userAuth: UserAuth): UserAuth =
//    userAuth match {
//      case UserAuth(_, _, _, _, _, _, numConnectionFailed) if numConnectionFailed < 2 =>
//        userAuth.copy(numConnectionFailed = numConnectionFailed + 1)
//      case UserAuth(_, _, _, _, _, _, numConnectionFailed) if numConnectionFailed == 2 =>
//        userAuth.copy(lockStatus = "Y", numConnectionFailed = 3)
//      case _ => userAuth
//    }
//
//  override def updateFailedConnection(login: ID[String]): UserAuthRepo[UserAuth] = for {
//    updated <- updateFailedConnection(login, updateFailedConnectionImpl)
//    found <- findUserAuthById(login)
//  } yield {
//    found match {case ou : Option[UserAuth] => ou.get}
//  }
//
//  def validUser(login: ID[String], password: String): UserAuthRepo[UserAuth] =
//    for {
//      found <- findUserAuthById(login)
//    } yield {
//      found match {case ou : Option[UserAuth] => ou.get}
//    }
//
//}
