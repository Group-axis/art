package com.groupaxis.groupsuite.authentication.write.domain.model.user

import java.sql.Timestamp
import java.util.Date

import slick.driver.PostgresDriver

object Digest {
  import java.security.MessageDigest
  def md5(text: String) : String =
    MessageDigest.getInstance("MD5")
      .digest(text.getBytes())
      .map(0xFF & _)
      .map { "%02x".format(_) }
      .foldLeft(""){_ + _}
}

object DateMapper {
  implicit val time : java.sql.Timestamp = new java.sql.Timestamp(new Date().getTime)
  implicit val date : java.util.Date = new Date()
  implicit val timeOption : Option[java.sql.Timestamp] = Some(new java.sql.Timestamp(new Date().getTime))
  implicit val dateOption : Option[java.util.Date] = Some(new Date())

  import slick.driver.PostgresDriver.api._

   val utilDate2SqlTimestampMapper = PostgresDriver.MappedColumnType.base[java.util.Date, java.sql.Timestamp](
 utilDate => new java.sql.Timestamp(utilDate.getTime) ,
 sqlTimestamp => new java.util.Date(sqlTimestamp.getTime) )

   val utilDate2SqlTimestampMapperOption =
     PostgresDriver.MappedColumnType.base[Option[java.util.Date], java.sql.Timestamp](
//     utilDate  => utilDate.map(date => new java.sql.Timestamp(date.getTime)).orElse(None) ,
//     sqlTimestamp => sqlTimestamp.map(time => new java.util.Date(time.getTime)).orElse(None) )
    d => new Timestamp(d.map(_.getTime).getOrElse(new Date().getTime)),
    time => Some(new Date(time.getTime)))

    val utilDate2SqlDate = PostgresDriver.MappedColumnType.base[java.util.Date, java.sql.Date](
 utilDate => new java.sql.Date(utilDate.getTime) ,
 sqlDate => new java.util.Date(sqlDate.getTime) )

}

/********  USER  *************/

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


case class UserEntity(
    id: String,
    firstName: String,
    lastName: String,
    email: Option[String],
    active : Char,
    userIdCreation : Option[String],
    creationDate : Option[Date],
    userIdModification : Option[String],
    modificationDate : Option[Date]) {

    def toES(resetPasswordNeeded : Boolean,
             lockStatus : Boolean,
             failedConnection : Int,
             permissions : Seq[String],
             profiles : Seq[Int]): UserEntityES = UserEntityES(id, firstName, lastName, email, active.equals('Y'),
      resetPasswordNeeded, lockStatus, failedConnection, permissions, profiles)

}

case class UserEntityUpdate(
             firstName: String,
             lastName: String,
             email: Option[String],
             active : Char,
             userIdCreation : Option[String],
             creationDate : Option[Date],
             userIdModification : Option[String],
             modificationDate : Option[Date]
    ) {

  def merge(user: UserEntity): UserEntity = {
    UserEntity(user.id, firstName, lastName, email.orElse(user.email), active,
      userIdCreation.orElse(user.userIdCreation), creationDate.orElse(user.creationDate), userIdModification.orElse(user.userIdModification), modificationDate.orElse(user.modificationDate))
  }
  
  def merge(id: String): UserEntity = {
    UserEntity(id, firstName, lastName, email, active, userIdCreation, creationDate, userIdModification, modificationDate)
  }
}

case class UserEntityES(
              id: String,
              firstName: String,
              lastName: String,
              email: Option[String],
              active : Boolean,
              resetPasswordNeeded : Boolean,
              lockStatus : Boolean,
              failedConnection : Int,
              permissions : Seq[String],
              profiles : Seq[Int])

/********  USER-DETAILS   *************/

case class UserDetailEntity(
             id: String,
             userIdReset: Option[String],
             resetDate : Option[Date],
             lastConnectionDate : Option[Date],
             pwd: Option[String],
             lockStatus : Char,
             failedConnection : Int) {

  //  require(!routingPointName.isEmpty, "routingpointname.empty")
}

case class UserDetailEntityUpdate(
             userIdReset: Option[String],
             resetDate : Option[Date],
             lastConnectionDate : Option[Date],
             pwd: Option[String],
             lockStatus : Option[Char],
             failedConnection : Option[Int]) {

  def merge(user: UserDetailEntity): UserDetailEntity = {
    UserDetailEntity(user.id, userIdReset.orElse(user.userIdReset), resetDate.orElse(user.resetDate), lastConnectionDate.orElse(user.lastConnectionDate), pwd.orElse(user.pwd),lockStatus.getOrElse(user.lockStatus), failedConnection.getOrElse(user.failedConnection))
  }

}

/********  USER-PROFILES   *************/

case class UserProfileEntity(userId : Int, profileId : Int)