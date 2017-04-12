package com.groupaxis.groupsuite.simulator.write.domain.model.mapping

trait MappingRequest
trait MappingResponse

object MappingMessages {

  //commands
  case class CreateMapping(keyword: String, message: MappingEntityUpdate) extends MappingRequest
  case class UpdateMapping(keyword : String, message: MappingEntityUpdate) extends MappingRequest
  case class FindAllMappings() extends MappingRequest
  case class FindMappingById(keyword : String) extends MappingRequest

  //events
  case class MappingFound(mapping: Option[MappingEntity]) extends MappingResponse
  case class MappingCreated(mapping : MappingEntity) extends MappingResponse
  case class MappingUpdated(mapping : MappingEntity) extends MappingResponse
  case class MappingsFound(mappings: Seq[MappingEntity]) extends MappingResponse

}
