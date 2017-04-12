package com.groupaxis.groupsuite.routing.write.domain.model.amh.backend

case class AMHBackendEntity(pkCode: Option[String], pkDirection: Option[String], code: String, dataOwner: Option[String], description: Option[String],
                            lockCode: Option[String], name: Option[String], environment : String, version : String) {

  def toES = AMHBackendEntityES(pkCode, pkDirection, code, dataOwner, description, lockCode, name, environment, version)
}

case class AMHBackendEntityUpdate(code: String, dataOwner: Option[String] = None, description: Option[String] = None,
                                  lockCode: Option[String] = None, name: Option[String] = None, environment : String, version : String) {

  def merge(backend: AMHBackendEntity): AMHBackendEntity = {
    AMHBackendEntity(backend.pkCode,backend.pkDirection, code, dataOwner.orElse(backend.dataOwner), description.orElse(backend.description)
      , lockCode.orElse(backend.lockCode) , name.orElse(backend.name), environment, version)
  }

  def merge(pkCode: Option[String], pkDirection: Option[String]): AMHBackendEntity = {
    AMHBackendEntity(pkCode, pkDirection, code, dataOwner, description, lockCode, name,environment, version)
  }
}

case class AMHBackendEntityES(pkCode: Option[String], pkDirection: Option[String], code: String, dataOwner: Option[String], description: Option[String], lockCode: Option[String], name: Option[String], environment : String, version : String)
