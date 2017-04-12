package com.groupaxis.groupsuite.routing.write.domain.model.routing.point

import com.groupaxis.groupsuite.routing.write.domain.model.routing.rule.{RuleEntity, RuleEntityES}
import org.apache.logging.log4j.scala.Logging

//trait PointID {
//  def pointId: Long
//}

//case class Point(pointId: Long, description: Option[String], lastModification: Date, creationDate: Date, createdBy: String) extends PointID

case class Point(pointName: String, full: Boolean, rules : Seq[RuleEntity]) {
  
  def toES = {
     PointES(pointName, full, rules.map( rule => rule.toES))
  }
}

case class PointES(pointName : String, full : Boolean, rules : Seq[RuleEntityES])

object test extends App with Logging {
  val optional = if (false) Some("key3" -> "value3") else None
  val entities = Seq("key1" -> "value1", "key2" -> "value2") ++ optional
  logger.debug(entities.toMap)

}