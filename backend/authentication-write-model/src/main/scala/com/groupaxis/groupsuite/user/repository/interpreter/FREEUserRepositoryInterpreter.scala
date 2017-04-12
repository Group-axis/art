package com.groupaxis.groupsuite.user.repository.interpreter

import com.groupaxis.groupsuite.persistence.driver.DBDriver
import com.groupaxis.groupsuite.user.model.User
import com.groupaxis.groupsuite.user.repository.{QueryUser, StoreUser, UserRepoF}

trait FREEUserRepositoryInterpreter extends DBDriver {

  import com.groupaxis.groupsuite.user.repository.interpreter.slick.UsersDAO._
  import driver.api._
  import scalaz._

  type UserRepo[A] = Free[UserRepoF, A]
  type Di[A] = NonEmptyList[String] \/ A

  def apply[A](action: UserRepo[A]): DBIOAction[\/[NonEmptyList[String], Seq[User]], NoStream, Effect.All]
}

case class FREEUserRepositorySlickInterpreter[E]() { //extends FREEUserRepositoryInterpreter {

  import com.groupaxis.groupsuite.user.repository.interpreter.slick.UsersDAO._
  import driver.api._

  import scalaz._
  import Scalaz._

  type Action[A] = DBIOAction[\/[NonEmptyList[String], A], NoStream, Effect.All]

//  val step: UserRepoF ~> Action = new (UserRepoF ~> Action) {
//
//    import scala.concurrent.ExecutionContext.Implicits.global
//
//    override def apply[A](fa: UserRepoF[A]): Action[A] = fa match {
//      case QueryUser(login) =>
//        users.filter(_.login === login).result
//          .map(v => v.right[NonEmptyList[String]])
//      //        users.filter(_.login === login).result.headOption
//      //          .map(e => e.right[NonEmptyList[String]])
//
//      //.getOrElse { NonEmptyList[String](s"User login $login not found").left}
//      //        table.get(login).map { a => now(a) }
//      //                     .getOrElse { fail(new RuntimeException(s"Account no $no not found")) }
//      case StoreUser(user) => (users returning users += user).
//        map(e => Seq(e).right[NonEmptyList[String]])
//      //      case StoreUser(user) => now(table += ((account.no, account))).void
//      //      case DeleteUser(login) => now(table -= no).void
//    }
//  }
//[DBIOAction[\/[NonEmptyList[String], Seq[User]], NoStream, Effect.All]]

  //: DBIOAction[\/[NonEmptyList[String], Seq[User]], NoStream, Effect.All]
//  type rr = \/[ NonEmptyList[String], Seq[User]]
//  val tt = Unapply.unapplyMAB2[DBIOAction, \/, NonEmptyList[String], Seq[User]] //NoStream, Effect.All
//  val ttt = Unapply.unapplyMAB2[DBIOAction, DBIOAction, tt.M[tt.A], NoStream] //NoStream, Effect.All

//  trait DBContainer[A] {
////    def point[A](a: => A): F[A]
//def bind[B](fa: DBContainer[A])(f: (A) => DBContainer[B]): DBContainer[B]
//  }
//  case class SlickContainer[V, F[V] <: DBIOAction[V,NoStream,Effect.All]](a: F) extends Monad[F] {
//    override def bind[V, B](fa: F[V])(f: (V) => F[B]): F[B] =
//      fa.flatMap(ee : DBIOAction[V,NoStream,Effect.All] => f(ee))
//  }
//
//  implicit object mm extends Monad[DBContainer] {
//    val f = driver.api.DBIOAction.successful
//  override def point[A](a: => A): Action[A] = {
//    import com.groupaxis.groupsuite.user.repository.interpreter.slick.UsersDAO._
    import driver.api.DBIOAction
//    f(a)
//  }
//  override def bind[A, B](fa: DBContainer[A])(f: (A) => DBContainer[B]): DBContainer[B] = fa.flatMap(aa => f(aa))
//}

//  override def apply[A](action: UserRepo[A]) =
//    action.foldMap[({type λ[α] = Action[α]})#λ](step)
/*
Error:(50, 12) no type parameters for method foldMap: (f: scalaz.~>[com.groupaxis.groupsuite.user.repository.UserRepoF,M])(implicit M: scalaz.Monad[M])M[A] exist so that it can be applied to arguments (scalaz.~>[com.groupaxis.groupsuite.user.repository.UserRepoF,FREEUserRepositorySlickInterpreter.this.Action])
 --- because ---
argument expression's type is not compatible with formal parameter type;
 found   : scalaz.~>[com.groupaxis.groupsuite.user.repository.UserRepoF,FREEUserRepositorySlickInterpreter.this.Action]
    (which expands to)  scalaz.NaturalTransformation[com.groupaxis.groupsuite.user.repository.UserRepoF,FREEUserRepositorySlickInterpreter.this.Action]
 required: scalaz.~>[com.groupaxis.groupsuite.user.repository.UserRepoF,?M]
    (which expands to)  scalaz.NaturalTransformation[com.groupaxis.groupsuite.user.repository.UserRepoF,?M]
    action.foldMap(step)
* */

}
