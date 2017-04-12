package com.groupaxis.groupsuite

import com.groupaxis.groupsuite.persistence.datastore.jdbc.Database
import com.groupaxis.groupsuite.user.repository.{UserAuthRepoF, UserAuthRepository, UserRepoF, UserRepository}
import slick.dbio.{DBIOAction, Effect, NoStream}

import scalaz._
import Scalaz._
import \/._
import Kleisli._
import scala.concurrent.Future

package object user {

  type ActionResult[T] = NonEmptyList[String] \/ T
  val ActionSuccess = \/-
  val ActionFailure = -\/

  type Valid[A] = NonEmptyList[String] \/ A
  //type toto[A] = Kleisli[Valid,UserRepository,A]
 type ValidT[A] = EitherT[Future, NonEmptyList[String], A]
  type FREEUserRepo[A] = Free[UserRepoF, A]
  type FREEUserAuthRepo[A] = Free[UserAuthRepoF, A]
  type UserRepo[A] = NonEmptyList[String] \/ A
//  type UserRepo[A] = EitherT[Future, NonEmptyList[String], A]

  type UserAuthRepo[A] = NonEmptyList[String] \/ A

  type SlickIOAction[A] = DBIOAction[\/[NonEmptyList[String], A], NoStream, Effect.All]

  type UserOperation[A] = Kleisli[ValidT, Database, A]

  def toMDS5(value : String) = ""

  import scalaz.concurrent.Task
import scala.concurrent.{ Promise, Future }

final class FutureExtensionOps[A](x: => Future[A]) {
  import scalaz.Scalaz._
  import scala.concurrent.ExecutionContext.Implicits.global
  def asTask: Task[A] = {
    Task.async {
      register =>
        x.onComplete {
          case scala.util.Success(v) => register(v.right)
          case scala.util.Failure(ex) => register(ex.left)
        }//(Implicits.trampoline)
    }
  }
}
/*
final class TaskExtensionOps[A](x: => Task[A]) {
  import scalaz.{ \/-, -\/ }
  val p: Promise[A] = Promise()
  def runFuture(): Future[A] = {
    x.unsafePerformAsync {
      case -\/(ex) =>
        p.failure(ex); ()
      case \/-(r) => p.success(r); ()
    }
    p.future
  }
}
  * */
}
