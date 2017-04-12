package com.groupaxis.groupsuite.routing.write.domain.model.routing.schema

trait SchemaID {
  def name : String
  def description: Option[String]
}

case class Schema(name: String, description: Option[String]) extends SchemaID  {
    def toES : SchemaES = SchemaES(name, description)
  }

case class SchemaES(name: String, description: Option[String]) extends SchemaID

////SML_SAA_MESSAGEPARTNER..
//SML_BACKEND_CHANNEL

//sbs_exitpoint
//sbs_messagepartner
//sbs_routingrulesdata