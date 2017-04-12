package com.groupaxis.groupsuite.authentication.write.domain.model.permission

import slick.driver.JdbcProfile

/*
CREATE TABLE "SYS_PERMISSION"
(
  "MODULE" character varying(20) NOT NULL, -- Module Name
  "NAME" character varying(255), -- Display Name
  "TAG" character varying(255) NOT NULL, -- Action string(technical one
  "ID_PERMISSION" serial NOT NULL,
  CONSTRAINT "PK_SYS_PERMISSION" PRIMARY KEY ("ID_PERMISSION"),
  CONSTRAINT "SYS_PERMISSION_MODULE_fkey" FOREIGN KEY ("MODULE")
      REFERENCES "SYS_MODULE" ("MODULE") MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
* */

class PermissionDAO(val driver: JdbcProfile) {
  import slick.driver.PostgresDriver.api._
  class Permissions(tagg: Tag) extends Table[PermissionEntity](tagg, "SYS_PERMISSION") {

    def id = column[Int]("ID_PERMISSION", O.AutoInc, O.PrimaryKey)

    def tag = column[String]("TAG")

    def module = column[String]("MODULE")

    def name = column[Option[String]]("NAME")

    private type PermissionEntityTupleType = (Int, String, String, Option[String])

    private val permissionShapedValue = (id, tag, module, name).shaped[PermissionEntityTupleType]

    private val toPermissionRow: (PermissionEntityTupleType => PermissionEntity) = { permissionTuple => {
      PermissionEntity(permissionTuple._1, permissionTuple._2, permissionTuple._3, permissionTuple._4)
     }
    }

    private val toPermissionTuple: (PermissionEntity => Option[PermissionEntityTupleType]) = { permissionRow =>
      Some((permissionRow.id, permissionRow.tag, permissionRow.module, permissionRow.name))
    }

    def * = permissionShapedValue <> (toPermissionRow, toPermissionTuple)

  }

  val permissions : TableQuery[Permissions] = TableQuery[Permissions]

}


