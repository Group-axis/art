package com.groupaxis.groupsuite.simulator.write.domain.model.mapping

import com.groupaxis.groupsuite.simulator.write.domain.model.mapping.MappingMessages.{MappingCreated, MappingFound, MappingUpdated, MappingsFound}

trait MappingWriteRepository {

  def getMappingsBySystem(forSystem : String) : Either[String, MappingsFound]

  def getMappingByKeyword(keyword : String, forSystem : String): Either[String, MappingFound]

  def createMapping(user: MappingEntity): Either[String, MappingCreated]

  def updateMapping(keyword: String, forSystem : String, mappingUpdate: MappingEntityUpdate): Either[String, MappingUpdated]

  def deleteMapping(keyword: String, forSystem : String): Either[String, Int]

}