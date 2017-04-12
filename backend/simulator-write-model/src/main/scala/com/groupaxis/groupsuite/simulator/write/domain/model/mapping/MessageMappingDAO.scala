package com.groupaxis.groupsuite.simulator.write.domain.model.mapping

import com.groupaxis.groupsuite.persistence.driver.DBDriver

trait MessageMappingDAO extends DBDriver {

  import driver.api._

  class Mappings(tag: Tag) extends Table[MappingEntity](tag, "sml_mapping") {

    def keyword = column[String]("keyword", O.PrimaryKey)

    def forSystem = column[String]("for_system", O.PrimaryKey)

    def mxRegExp = column[Option[String]]("mx_pattern")

    def mtRegExp = column[Option[String]]("mt_pattern")

    private type MappingEntityTupleType = (String, String, Option[String], Option[String])

    private val mappingShapedValue = (keyword, forSystem, mxRegExp, mtRegExp).shaped[MappingEntityTupleType]

    private val toMessageMapping: (MappingEntityTupleType => MappingEntity) = { mappingTuple => {
      MappingEntity(mappingTuple._1, mappingTuple._2, mappingTuple._3, mappingTuple._4)
    }
    }

    private val toMappingTuple: (MappingEntity => Option[MappingEntityTupleType]) = { mappingRow =>
      Some((mappingRow.keyword, mappingRow.forSystem, mappingRow.mxRegExp, mappingRow.mtRegExp))
    }

    def * = mappingShapedValue <> (toMessageMapping, toMappingTuple)

  }

  val mappings: TableQuery[Mappings] = TableQuery[Mappings]

}

object MessageMappingDAO extends MessageMappingDAO {
  def apply = MessageMappingDAO
}