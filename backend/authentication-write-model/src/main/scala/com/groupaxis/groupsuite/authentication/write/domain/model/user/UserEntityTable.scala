package com.groupaxis.groupsuite.authentication.write.domain.model.user

import java.sql.Timestamp

import org.joda.time.DateTime
import slick.driver.JdbcProfile

class UserDAO(val driver: JdbcProfile) {

  import slick.driver.PostgresDriver.api._
  /*
  /*
REATE TABLE "SYS_USER"
(
  "ID_USER" character varying(50) NOT NULL, -- Unqiue ID_USER
  "FIRSTNAME" character varying(32) NOT NULL, -- Import Origin file Name
  "LASTNAME" character varying(32) NOT NULL, --
  "EMAIL" character varying(62),
  "ACTIVE" character varying(1) NOT NULL DEFAULT 'Y'::character varying,
  "ID_USER_CREATION" character varying(50), -- Id of user who created this entity
  "DATE_CREATION" timestamp without time zone DEFAULT timezone('utc'::text, now()), -- Date of creation
  "ID_USER_MODIFICATION" character varying(50), -- user last modified this entity
  "DATE_MODIFICATION" timestamp without time zone, -- Last date of modification
  CONSTRAINT "PK_SYS_USER" PRIMARY KEY ("ID_USER")
)
* */
  * */

//  implicit def mapDate = MappedColumnType.base[Option[Date], Timestamp](
//    d => new Timestamp(d.map(_.getTime).getOrElse(new Date().getTime)),
//    time => Some(new Date(time.getTime))
//  )
  implicit def mapDate = MappedColumnType.base[Option[DateTime], Timestamp](
    d => new Timestamp(d.map(_.getMillis).getOrElse(new DateTime().getMillis)),
    time => Some(new DateTime(time.getTime))
  )

  class Users(tag: Tag) extends Table[UserEntity](tag, "SYS_USER") {
    def id = column[String]("ID_USER", O.PrimaryKey)
    def firstName = column[String]("FIRSTNAME")
    def lastName = column[String]("LASTNAME")
    def email = column[Option[String]]("EMAIL")
    def active = column[Char]("ACTIVE")
    def userCreationId = column[Option[String]]("ID_USER_CREATION")
    def creationDate = column[Option[DateTime]]("DATE_CREATION")//(DateMapper.utilDate2SqlTimestampMapperOption)
    def userModificationId = column[Option[String]]("ID_USER_MODIFICATION")
    def modificationDate = column[Option[DateTime]]("DATE_MODIFICATION")//(DateMapper.utilDate2SqlTimestampMapperOption)


    private type UserEntityTupleType = (String, String, String, Option[String], Char, Option[String], Option[DateTime], Option[String], Option[DateTime])

    private val userShapedValue = (
      id,
      firstName,
      lastName,
      email,
      active,
      userCreationId,
      creationDate,
      userModificationId,
      modificationDate).shaped[UserEntityTupleType]

    private val toUserRow: (UserEntityTupleType => UserEntity) = { userTuple =>
      {
        UserEntity(userTuple._1, userTuple._2, userTuple._3, userTuple._4, userTuple._5, userTuple._6, userTuple._7, userTuple._8, userTuple._9)
      }
    }

    private val toUserTuple: (UserEntity => Option[UserEntityTupleType]) = { userRow =>
      Some(userRow.id, userRow.firstName, userRow.lastName, userRow.email, userRow.active, userRow.userIdCreation,userRow.creationDate, userRow.userIdModification, userRow.modificationDate)
    }

    def * = userShapedValue <> (toUserRow, toUserTuple)

  }

  val users = TableQuery[Users]

}

/**************  USER_DETAIL ************************/
/*
CREATE TABLE "SYS_USER_DETAIL"
(
  "ID_USER" character varying(50) NOT NULL,
  "ID_USER_RESET" character varying(50), -- The admin user resetted the account
  "DATE_RESET" timestamp without time zone DEFAULT timezone('utc'::text, now()),
  "LAST_CONNECTION_DATE" timestamp without time zone,
  "MD5" character varying(1024), -- MD5 converted pwd
  "LOCK_STATUS" character varying(1) NOT NULL DEFAULT 'N'::character varying,
  "NB_FAILED_CONNECTION" integer NOT NULL DEFAULT 3,
  CONSTRAINT "SYS_USER_DETAIL_pkey" PRIMARY KEY ("ID_USER"),
  CONSTRAINT "FK_SYS_USER_DETAIL_SYS_USER" FOREIGN KEY ("ID_USER")
      REFERENCES "SYS_USER" ("ID_USER") MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
* */
class UserDetailDAO(val driver: JdbcProfile) {

  import slick.driver.PostgresDriver.api._

  implicit def mapDate = MappedColumnType.base[Option[DateTime], Timestamp](
    d => new Timestamp(d.map(_.getMillis).getOrElse(new DateTime().getMillis)),
    time => Some(new DateTime(time.getTime))
  )

  class UserDetails(tag: Tag) extends Table[UserDetailEntity](tag, "SYS_USER_DETAIL") {
    def id = column[String]("ID_USER", O.PrimaryKey)
    def userResetId = column[Option[String]]("ID_USER_RESET")
    def resetDate = column[Option[DateTime]]("DATE_RESET")//(DateMapper.utilDate2SqlTimestampMapperOption)
    def lastConnectionDate = column[Option[DateTime]]("LAST_CONNECTION_DATE")//(DateMapper.utilDate2SqlTimestampMapperOption)
    def pwd = column[Option[String]]("MD5")
    def lockStatus = column[Char]("LOCK_STATUS")
    def failedConnections = column[Int]("NB_FAILED_CONNECTION")

    private type UserDetailEntityTupleType = (String, Option[String], Option[DateTime], Option[DateTime], Option[String], Char, Int)

    private val userDetailShapedValue = (
      id,
      userResetId,
      resetDate,
      lastConnectionDate,
      pwd,
      lockStatus,
      failedConnections).shaped[UserDetailEntityTupleType]

    private val toUserDetailRow: (UserDetailEntityTupleType => UserDetailEntity) = { userDetailTuple =>
    {
      UserDetailEntity(userDetailTuple._1, userDetailTuple._2, userDetailTuple._3, userDetailTuple._4, userDetailTuple._5, userDetailTuple._6, userDetailTuple._7)
    }
    }

    private val toUserDetailTuple: (UserDetailEntity => Option[UserDetailEntityTupleType]) = { userDetailRow =>
      Some(userDetailRow.id, userDetailRow.userIdReset, userDetailRow.resetDate, userDetailRow.lastConnectionDate, userDetailRow.pwd, userDetailRow.lockStatus, userDetailRow.failedConnection )
    }

    def * = userDetailShapedValue <> (toUserDetailRow, toUserDetailTuple)

  }

  val userDetails = TableQuery[UserDetails]

}

/**************  USER - PROFILES ************************/
/*
CREATE TABLE "SYS_USER_PROFILE"
(
  "ID_PROFILE" integer NOT NULL,
  "ID_USER" character varying(50) NOT NULL,
  CONSTRAINT "PK_SYS_USER_PROFILE" PRIMARY KEY ("ID_PROFILE", "ID_USER")
)
* */
class UserProfileDAO(val driver: JdbcProfile) {

  import slick.driver.PostgresDriver.api._


  class UserProfiles(tag: Tag) extends Table[UserProfileEntity](tag, "SYS_USER_DETAIL") {
    def userId = column[Int]("ID_USER", O.PrimaryKey)
    def profileId = column[Int]("ID_PROFILE", O.PrimaryKey)

    private type UserProfileEntityTupleType = (Int, Int)

    private val userProfileShapedValue = (
      userId,
      profileId).shaped[UserProfileEntityTupleType]

    private val toUserProfileRow: (UserProfileEntityTupleType => UserProfileEntity) = { userProfileTuple =>
    {
      UserProfileEntity(userProfileTuple._1, userProfileTuple._2)
    }
    }

    private val toUserProfileTuple: (UserProfileEntity => Option[UserProfileEntityTupleType]) = { userProfileRow =>
      Some(userProfileRow.userId, userProfileRow.profileId)
    }

    def * = userProfileShapedValue <> (toUserProfileRow, toUserProfileTuple)

  }

  val userProfiles = TableQuery[UserProfiles]

}
