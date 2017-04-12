package com.groupaxis.groupsuite.authentication.write.domain.model.profile

import com.groupaxis.groupsuite.authentication.write.domain.model.permission.PermissionEntity

/********  PROFILE   *************/
case class ProfileEntity(id : Int, module : String, name : Option[String], active : Char) {
  
  def toES(permissions : Seq[PermissionEntity]) = {
     ProfileES(id, module, name, active.equals('Y'), permissions.map(_.tag))
  }
}

case class ProfileEntityUpdate(module: String = "UNKNOWN", name: Option[String] = None, active: Char = 'Y', permissions : Seq[ProfilePermissionEntity]) {

  def merge(profile: ProfileEntity): ProfileEntity = {
    ProfileEntity(profile.id, module, name.orElse(profile.name), active)
  }

  def merge(id: Int): ProfileEntity = {
    ProfileEntity(id, module, name, active)
  }
}

case class ProfileES(id : Int, module : String, name : Option[String], active : Boolean, permissions : Seq[String])

/********  PROFILE-PERMISSIONS   *************/

case class ProfilePermissionEntity(idProfile : Int, idPermission : Int, active : Char = 'N')

