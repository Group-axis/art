package com.groupaxis.groupsuite.routing.write.domain.model.routing.schema

import com.groupaxis.groupsuite.persistence.driver.DBDriver

trait SchemaDAO extends DBDriver {

  // Import the query language features from the driver
  import driver.api._

  protected class Schemas(tag: Tag) extends Table[Schema](tag, "sbs_routingschema") {
    def name = column[String]("name", O.PrimaryKey)
    def env = column[String]("env", O.PrimaryKey)
    def version = column[String]("version", O.PrimaryKey)
    def description = column[Option[String]]("description")

    private type SchemaEntityTupleType = (String, String, String, Option[String])

    private val schemaShapedValue = (
      name,
      env,
      version,
      description
      ).shaped[SchemaEntityTupleType]

    private val toSchemaRow: (SchemaEntityTupleType => Schema) = schemaTuple =>  Schema(schemaTuple._1, schemaTuple._4)

    private val toSchemaTuple: (Schema => Option[SchemaEntityTupleType]) = { schemaRow =>
      Some((schemaRow.name, "UNKNOWN", "UNKNOWN", schemaRow.description))
    }

    def * = schemaShapedValue <> (toSchemaRow, toSchemaTuple)

  }

  val schemas = TableQuery[Schemas]
}

object SchemaDAO extends SchemaDAO {
  def apply = SchemaDAO
}