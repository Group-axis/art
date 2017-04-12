package com.groupaxis.groupsuite.simulator.write.domain.model.mapping


/********  MAPPING *************/
case class MappingEntity(keyword: String, forSystem: String, mxRegExp : Option[String], mtRegExp : Option[String]) {

}

case class MappingEntityUpdate(mxRegExp : Option[String], mtRegExp : Option[String]) {

  def merge(mapping: MappingEntity): MappingEntity = {
    MappingEntity(mapping.keyword, mapping.forSystem, mxRegExp.orElse(mapping.mxRegExp), mtRegExp.orElse(mapping.mtRegExp))
  }

  def merge(keyword : String, forSystem : String): MappingEntity = {
    MappingEntity(keyword, forSystem, mxRegExp, mtRegExp)
  }
}

object MappingForSystem {
  val AMH_SYSTEM = "AMH"
  val SAA_SYSTEM = "SAA"

  trait MappingSystem {
  }

  object AMH extends MappingSystem
  object SAA extends MappingSystem

  override def toString: String = super.toString
}
