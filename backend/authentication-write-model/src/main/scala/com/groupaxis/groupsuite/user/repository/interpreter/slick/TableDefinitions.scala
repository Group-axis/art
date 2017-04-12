package com.groupaxis.groupsuite.user.repository.interpreter.slick

import java.sql.Timestamp

import com.groupaxis.groupsuite.persistence.driver.DBDriver
import com.groupaxis.groupsuite.user.model.User
import org.joda.time.DateTime

trait BooleanT {

  def fromBooleanToString : Boolean => String = (x) => if (x) "Y" else "N"

  def fromStringToBoolean : String => Boolean = (x) => x == "Y"
}

trait UsersDAO extends DBDriver with BooleanT {

  import driver.api._

  implicit def mapDate = MappedColumnType.base[Option[DateTime], Timestamp](
    d => new Timestamp(d.map(_.getMillis).getOrElse(new DateTime().getMillis)),
    time => Some(new DateTime(time.getTime))
  )
  class Users (tag: Tag) extends Table[User](tag, "sys_user") {
/*
* id_user character varying(50) NOT NULL, -- Unqiue ID_USER
  firstname character varying(32) NOT NULL, -- Import Origin file Name
  lastname character varying(32) NOT NULL, --
  email character varying(62),
  active character varying(1) NOT NULL DEFAULT 'Y'::character varying,
  id_user_creation character varying(50), -- Id of user who created this entity
  date_creation timestamp without time zone NOT NULL DEFAULT timezone('utc'::text, now()), -- Date of creation
  id_user_modification character varying(50), -- user last modified this entity
  date_modification timestamp without time zone, -- Last date of modification*/
  def login = column[String]("id_user", O.PrimaryKey)
  def firstName = column[String]("firstname")
  def lastName = column[String]("lastname")
  def active = column[String]("active", O.Default("Y"))
  def email = column[Option[String]]("email")

  private type UserEntityTupleType = (String, String, String, String,Option[String])

    private val userShapedValue = (login, firstName, lastName, active, email).shaped[UserEntityTupleType]

    private val toUserRow: (UserEntityTupleType => User) = { userTuple => {
      User(userTuple._1, userTuple._2, userTuple._3, fromStringToBoolean(userTuple._4), None, userTuple._5)
     }
    }

    private val toUserTuple: (User => Option[UserEntityTupleType]) = { userRow =>
      Some((userRow.login, userRow.firstName, userRow.lastName, fromBooleanToString(userRow.active), userRow.email))

    }

    def * = userShapedValue <> (toUserRow, toUserTuple)

}
 val users : TableQuery[Users] = TableQuery[Users]
}


object UsersDAO extends UsersDAO {
  def apply = UsersDAO
}

