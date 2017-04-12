package com.groupaxis.groupsuite.user.repository.interpreter

import com.groupaxis.groupsuite.persistence.datastore.jdbc.Database
import com.groupaxis.groupsuite.user.repository.UserRepository
import com.groupaxis.groupsuite.user.repository.interpreter.slick.UsersDAO._
import com.groupaxis.groupsuite.user.{UserOperation, ValidT}
import driver.api._
import scala.concurrent.Future
import scalaz.Kleisli.{apply => _, _}
import scalaz.Scalaz._
import scalaz.{NonEmptyList, \/, _}

class UserRepositoryInterpreter extends UserRepository {

  import com.groupaxis.groupsuite.user.model.User

  import scala.concurrent.ExecutionContext.Implicits.global

  override def findUserById(login: String): UserOperation[User] = {

    val slickResponse: DBIOAction[Option[User], NoStream, Effect.Read] =
      users.filter(_.login === login).result.headOption

    val disjunctionResponse: DBIOAction[\/[NonEmptyList[String], User], NoStream, Effect.All] =
      slickResponse.map(e => e.map(ee => ee.right[NonEmptyList[String]])
        .getOrElse(NonEmptyList(s"No User with login $login found").left[User]))

    kleisli[ValidT, Database, User] {
      database: Database =>
        EitherT[Future, NonEmptyList[String], User] {
          database.db.run(disjunctionResponse)
        }
    }
  }

  override def save(user: User): UserOperation[User] = {
    try {
      val slickResponse = users returning users += user

      val disjunctionResponse = slickResponse.map(e => e.right[NonEmptyList[String]])

      kleisli[ValidT, Database, User] {
        database: Database =>
          EitherT[Future, NonEmptyList[String], User] {
            database.db.run(disjunctionResponse)
          }
      }

    } catch {
      case error: Exception =>
        kleisli[ValidT, Database, User] {
          database: Database =>
            EitherT[Future, NonEmptyList[String], User] {
              Future {
                NonEmptyList(s"Error while saving user: ${error.getMessage}").left[User]
              }
            }
        }
    }
  }

  override def all: UserOperation[Seq[User]] = {
    val slickResponse = users.result

    val disjunctionResponse = slickResponse.map(e => e.right[NonEmptyList[String]])

    kleisli[ValidT, Database, Seq[User]] {
      database: Database =>
        EitherT {
          database.db.run(disjunctionResponse)
        }
    }
  }

  override def deleteById(login: String): UserOperation[Int] = {
    val slickResponse = users.filter(_.login === login).delete

    val disjunctionResponse = slickResponse.map(e => e.right[NonEmptyList[String]])

    kleisli[ValidT, Database, Int] {
      database: Database =>
        EitherT[Future, NonEmptyList[String], Int] {
          database.db.run(disjunctionResponse)
        }
    }
  }
}

object UserRepositoryInterpreter {
  def apply() = new UserRepositoryInterpreter
}