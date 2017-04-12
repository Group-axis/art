package com.groupaxis.groupsuite.persistence.util

import java.sql.Timestamp

import com.groupaxis.groupsuite.persistence.driver.DBDriver
import org.apache.logging.log4j.scala.Logging
import org.joda.time.DateTime
import slick.ast.{ColumnOption, TypedType}
//import slick.lifted.Rep

trait TableAudit extends DBDriver {
  import driver.api._
//  import slick.ast.ScalaBaseType._
  def column[C](n: String, options: ColumnOption[C]*)(implicit tt: TypedType[C]): Rep[C]

  def creationUserId = column[Option[String]]("ID_USER_CREATION")
  def creationDate = column[Timestamp]("DATE_CREATION")
  def modificationUserId = column[Option[String]]("ID_USER_MODIFICATION")
  def modificationDate = column[Option[Timestamp]]("DATE_MODIFICATION")

  protected type AuditEntityTupleType = (Option[String], Timestamp, Option[String], Option[Timestamp])

  protected def auditEntityTupleType = {
    (creationUserId, creationDate, modificationUserId, modificationDate)
  }
  protected def toOptionDateTime(time : Option[Timestamp]) : Option[DateTime] = time.map(e => new DateTime(e.getTime))
  protected def toDateTime(time : Timestamp) : DateTime = new DateTime(time.getTime)
}

trait EntityAudit extends Logging {
  def creationUserId : Option[String]
  def creationDate : DateTime
  def modificationUserId : Option[String]
  def modificationDate : Option[DateTime]

  def creationDateInTimestamp : Timestamp= new Timestamp(creationDate.getMillis)
  def modificationDateInTimestamp : Option[Timestamp]= modificationDate.map(a => new Timestamp(a.getMillis))
  def today = DateTime.now
  def audit : (Option[String], Timestamp, Option[String], Option[Timestamp]) =
    {
      logger.debug(s" FROM ENTITY ==> ${(creationUserId, creationDate, modificationUserId, modificationDate)}")
      (creationUserId , creationDateInTimestamp, modificationUserId, modificationDateInTimestamp)
    }

}

trait EntityUpdateAudit {
  def creationUserId : Option[String]
  def creationDate : Option[DateTime]
  def modificationUserId : Option[String]
  def modificationDate : Option[DateTime]

  def getCreationDateValue : DateTime = creationDate.getOrElse(DateTime.now)

}