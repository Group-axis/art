package com.groupaxis.groupsuite.routing.write.domain.model.amh.backend

import com.groupaxis.groupsuite.persistence.driver.DBDriver
import slick.driver.JdbcProfile

trait BackendDAO extends DBDriver {
  // Import the query language features from the driver
  import driver.api._

  class AMHBackends(tag: Tag) extends Table[AMHBackendEntity](tag, "AMH_BECONFIG") {
    def pkCode = column[Option[String]]("BCODE")

    def pkDirection = column[Option[String]]("DIRECTION")

    def code = column[String]("CODE")

    def dataOwner = column[Option[String]]("DATAOWNER")

    def description = column[Option[String]]("DESCRIPTION")

    def lockCode = column[Option[String]]("LOCKCODE")

    def name = column[Option[String]]("NAME")

    def env = column[String]("ENV")

    def version = column[String]("VERSION")

    // for mapping AMHBackendEntity to tuples and back
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

//  import
  val amhBackends = TableQuery[AMHBackends]


}

object BackendDAO extends BackendDAO {
  def apply(driver: JdbcProfile) = BackendDAO
}
