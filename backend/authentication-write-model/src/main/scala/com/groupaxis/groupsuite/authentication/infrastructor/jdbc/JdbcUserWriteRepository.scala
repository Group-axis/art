package com.groupaxis.groupsuite.authentication.infrastructor.jdbc

import java.util.concurrent.TimeoutException

import com.groupaxis.groupsuite.authentication.write.domain.model.user.UserMessages.{UserCreated, UserFound, UserUpdated, UsersFound}
import com.groupaxis.groupsuite.authentication.write.domain.model.user.{UserDAO, UserEntity, UserEntityUpdate, UserWriteRepository}
import com.groupaxis.groupsuite.persistence.datastore.jdbc.Database

import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration


object JdbcUserWriteRepository {

}

class JdbcUserWriteRepository(dao: UserDAO, database: Database, timeout: FiniteDuration) extends UserWriteRepository {

  import database._
  import slick.driver.PostgresDriver.api._

  import scala.concurrent.ExecutionContext.Implicits.global


  def getUsers : Either[String, UsersFound] = {
    try {
      val result = Await.result(db.run(dao.users.result), timeout)
      Right(UsersFound(result))
    } catch {
      case timeEx: TimeoutException => Left(s" Error while looking for all users DB not responding")
      case e: Exception             => Left(s" Error while looking for all users msg[$e.getMessage]")
    }
  }

  def getUserByUsername(id: String): Either[String, UserFound] = {
    try {
      val result: Option[UserEntity] = Await.result(db.run(dao.users.filter(_.id === id).result), timeout).headOption
      Right(UserFound(result))
    } catch {
      case timeEx: TimeoutException => Left(s" Error while looking for a USER $id DB not responding")
      case e: Exception             => Left(s" Error while looking for a USER $id msg[$e.getMessage]")
    }
  }

  def createUser(user: UserEntity): Either[String, UserCreated] = {
    try {
      Await.result(db.run(dao.users returning dao.users += user).map { user => Right(UserCreated(user)) }, timeout)
    } catch {
      case timeEx: TimeoutException => Left(s" Error while creating user DB not responding")
      case e: Exception             => Left(s" Error while creating user msg[$e.getMessage]")
    }
  }

  def updateUser(id: String, userUpdate: UserEntityUpdate): Either[String, UserUpdated] = {
    val eitherResponse = getUserByUsername(id)

    if (eitherResponse.isLeft) {
      Left(eitherResponse.left.get)
    } else {
      val userFound = eitherResponse.right.get
      userFound.user match {
        case Some(user) =>
          try {
            val updatedUser = userUpdate.merge(user)
            Await.result(db.run(dao.users.filter(_.id === id).update(updatedUser)), timeout)
            Right(UserUpdated(updatedUser))
          } catch {
            case timeEx: TimeoutException => Left(s" Error while updating user DB not responding")
            case e: Exception             => Left(s" Error while updating user msg[$e.getMessage]")
          }
        case None => Left("")
      }
    }
  }
  def deleteUser(id: String): Either[String, Int] = {
   try {
    val result = Await.result(db.run(dao.users.filter(_.id === id).delete), timeout)
     Right(result)
   } catch {
     case timeEx: TimeoutException => Left(s" Error while deleting user id $id DB not responding")
     case e: Exception             => Left(s" Error while deleting user id $id msg[$e.getMessage]")
   }
  }

}