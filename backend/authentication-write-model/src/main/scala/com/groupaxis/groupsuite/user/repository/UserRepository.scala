package com.groupaxis.groupsuite.user.repository

import com.groupaxis.groupsuite.user.{UserOperation, ValidT}
import com.groupaxis.groupsuite.user.model.User

import scalaz._
import Scalaz._
import scala.concurrent.Future

trait UserRepository {

  implicit object m extends Monad[ValidT] {
    import scala.concurrent.ExecutionContext.Implicits.global
    override def map[A, B](fa: ValidT[A])(f: (A) => B): ValidT[B] = fa.map(f)

    override def bind[A, B](fa: ValidT[A])(f: (A) => ValidT[B]): ValidT[B] = fa.flatMap(f)

    override def point[A](a: => A): ValidT[A] = EitherT { Future { a.right } }
  }

//type UserRepo[A] = NonEmptyList[String] \/ A
  def findUserById(login: String): UserOperation[User]

  def save(user: User): UserOperation[User]

  def all: UserOperation[Seq[User]]

  def deleteById(login: String): UserOperation[Int]

  private def update1[A](login: String, value: A, f: (User, A) => User): UserOperation[User] =
    for {
      user <- findUserById(login)
      updated <-  save(f(user, value))
    } yield updated

  def updateUser(login: String, f: User => User): UserOperation[User] =
    for {
      user <- findUserById(login)
      updated <- save(f(user))
    } yield updated

  def updateActive(login: String, active : Boolean,f: (User, Boolean) => User) : UserOperation[User] =
    update1[Boolean](login, active, f)


}


