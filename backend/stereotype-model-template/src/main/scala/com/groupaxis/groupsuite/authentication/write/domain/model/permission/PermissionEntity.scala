package com.groupaxis.groupsuite.authentication.write.domain.model.permission

//import java.util.Date

/********  PERMISSION *************/
case class PermissionEntity(id : Int, tag : String, module: String = "UNKNOWN", name : Option[String] = None) {

  def toES = PermissionES(id, tag, module, name)

}

case class PermissionEntityUpdate(tag: String, module: String = "UNKNOWN", name: Option[String] = None) {

  def merge(permission: PermissionEntity): PermissionEntity = {
    PermissionEntity(permission.id, tag, module, name.orElse(permission.name))
  }

  def merge(id: Int): PermissionEntity = {
    PermissionEntity(id, tag, module, name)
  }
}

case class PermissionES(id : Int, tag : String, module : String, name : Option[String])
