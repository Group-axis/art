package com.groupaxis.groupsuite.authentication.write.domain.model.profile

import slick.driver.JdbcProfile

/*
CREATE TABLE "SYS_PROFILE"
(
  "ID_PROFILE" serial NOT NULL, -- Unique serial id
  "MODULE" character varying(20) NOT NULL, -- Module beloings to this id_profile
  "ENV" character varying(20), -- Enviornment level segration
  "NAME" character varying(50), -- Display Name
  "ACTIVE" character varying NOT NULL DEFAULT 'Y'::character varying, -- Default this profile is actve
  CONSTRAINT "SYS_PROFILE_pkey" PRIMARY KEY ("ID_PROFILE"),
  CONSTRAINT "SYS_PROFILE_MODULE_fkey" FOREIGN KEY ("MODULE")
      REFERENCES "SYS_MODULE" ("MODULE") MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
* */

class ProfileDAO(val driver: JdbcProfile) {
  import slick.driver.PostgresDriver.api._
  class Profiles(tag: Tag) extends Table[ProfileEntity](tag, "SYS_PROFILE") {

    def id = column[Int]("ID_PROFILE",O.AutoInc ,O.PrimaryKey)

    def module = column[String]("MODULE")

    def name = column[Option[String]]("NAME")

    def active = column[Char]("ACTIVE")

    private type ProfileEntityTupleType = (Int, String, Option[String], Char)

    private val profileShapedValue = ( id, module, name, active).shaped[ProfileEntityTupleType]

    private val toProfileRow: (ProfileEntityTupleType => ProfileEntity) = { ProfileTuple => {
      ProfileEntity(ProfileTuple._1, ProfileTuple._2, ProfileTuple._3, ProfileTuple._4)
     }
    }

    private val toProfileTuple: (ProfileEntity => Option[ProfileEntityTupleType]) = { profileRow =>
      Some((profileRow.id, profileRow.module, profileRow.name, profileRow.active))
    }

    def * = profileShapedValue <> (toProfileRow, toProfileTuple)

  }

  val profiles : TableQuery[Profiles] = TableQuery[Profiles]

}

/*
CREATE TABLE "SYS_PROFILE_PERMISSION"
(
  "ID_PERMISSION" integer NOT NULL,
  "ID_PROFILE" integer NOT NULL,
  "ACTIVE" character varying(1) NOT NULL DEFAULT 'N'::character varying,
  CONSTRAINT "SYS_PROFILE_PERMISSION_pkey" PRIMARY KEY ("ID_PERMISSION", "ID_PROFILE")
)
* */
class ProfilePermissionDAO(val driver: JdbcProfile) {
  import slick.driver.PostgresDriver.api._
  class ProfilePermissions(tag: Tag) extends Table[ProfilePermissionEntity](tag, "SYS_PROFILE_PERMISSION") {

    def idProfile = column[Int]("ID_PROFILE",O.PrimaryKey)

    def idPermission = column[Int]("ID_PERMISSION",O.PrimaryKey)

    def active = column[Char]("ACTIVE")

    private type ProfilePermissionEntityTupleType = (Int, Int, Char)

    private val profilePermissionShapedValue = ( idProfile, idPermission, active).shaped[ProfilePermissionEntityTupleType]

    private val toProfilePermissionRow: (ProfilePermissionEntityTupleType => ProfilePermissionEntity) = { ProfileTuple => {
      ProfilePermissionEntity(ProfileTuple._1, ProfileTuple._2, ProfileTuple._3)
    }
    }

    private val toProfilePermissionTuple: (ProfilePermissionEntity => Option[ProfilePermissionEntityTupleType]) = { profilePermissionRow =>
      Some((profilePermissionRow.idProfile, profilePermissionRow.idPermission, profilePermissionRow.active))
    }

    def * = profilePermissionShapedValue <> (toProfilePermissionRow, toProfilePermissionTuple)

  }

  val profilePermissions : TableQuery[ProfilePermissions] = TableQuery[ProfilePermissions]

}