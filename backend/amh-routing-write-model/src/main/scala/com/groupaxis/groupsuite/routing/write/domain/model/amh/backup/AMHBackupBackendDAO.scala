package com.groupaxis.groupsuite.routing.write.domain.model.amh.backup

import com.groupaxis.groupsuite.persistence.driver.DBDriver
import com.groupaxis.groupsuite.routing.write.domain.model.amh.backend.AMHBackendEntity
import slick.driver.JdbcProfile

trait AMHBackupBackendDAO extends DBDriver {

  import driver.api._

  class AMHBackupBackends(tag: Tag) extends Table[AMHBackendEntity](tag, "BAK_AMH_BECONFIG") {
    def pkCode = column[Option[String]]("BCODE")

    def pkDirection = column[Option[String]]("DIRECTION")

    def code = column[String]("CODE")

    def dataOwner = column[Option[String]]("DATAOWNER")

    def description = column[Option[String]]("DESCRIPTION")

    def lockCode = column[Option[String]]("LOCKCODE")

    def name = column[Option[String]]("NAME")

    def env = column[String]("ENV")

    def version = column[String]("VERSION")

    private type AMHBackendEntityTupleType = (Option[String], Option[String], String, Option[String], Option[String], Option[String], Option[String], String, String)

    private val amhBackendShapedValue = (
      pkCode,
      pkDirection,
      code,
      dataOwner,
      description,
      lockCode,
      name,
      env,
      version).shaped[AMHBackendEntityTupleType]

    private val toBackendRow: (AMHBackendEntityTupleType => AMHBackendEntity) = { ruleTuple =>
      AMHBackendEntity(ruleTuple._1, ruleTuple._2, ruleTuple._3, ruleTuple._4, ruleTuple._5, ruleTuple._6, ruleTuple._7, ruleTuple._8, ruleTuple._9)
    }

    private val toBackendTuple: (AMHBackendEntity => Option[AMHBackendEntityTupleType]) = { backendRow =>
      Some((backendRow.pkCode, backendRow.pkDirection, backendRow.code, backendRow.dataOwner, backendRow.description, backendRow.lockCode, backendRow.name, backendRow.environment, backendRow.version))
    }

    def * = amhBackendShapedValue <>(toBackendRow, toBackendTuple)

  }

  val amhBackupBackends = TableQuery[AMHBackupBackends]
}

object AMHBackupBackendDAO extends AMHBackupBackendDAO {
  def apply(driver: JdbcProfile) = AMHBackupBackendDAO
}