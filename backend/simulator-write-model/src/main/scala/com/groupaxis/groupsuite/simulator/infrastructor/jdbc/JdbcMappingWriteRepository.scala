package com.groupaxis.groupsuite.simulator.infrastructor.jdbc

import java.util.concurrent.TimeoutException

import com.groupaxis.groupsuite.persistence.datastore.jdbc.Database
import com.groupaxis.groupsuite.simulator.write.domain.model.mapping.{MessageMappingDAO, MappingEntity, MappingEntityUpdate, MappingWriteRepository}
import com.groupaxis.groupsuite.simulator.write.domain.model.mapping.MappingMessages.{MappingCreated, MappingFound, MappingUpdated, MappingsFound}

import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration


object JdbcMappingWriteRepository {
}

class JdbcMappingWriteRepository(dao: MessageMappingDAO, database: Database, timeout: FiniteDuration) extends MappingWriteRepository {

  import database._
  import slick.driver.PostgresDriver.api._
  import scala.concurrent.ExecutionContext.Implicits.global

  def getMappingsBySystem(forSystem : String) : Either[String, MappingsFound] = {
    try {
      val result = Await.result(db.run(dao.mappings.filter(_.forSystem === forSystem).result), timeout)
      Right(MappingsFound(result))
    } catch {
      case timeEx: TimeoutException => Left(s" Error while looking for all $forSystem mappings DB not responding")
      case e: Exception             => Left(s" Error while looking for all $forSystem mappings msg[$e.getMessage]")
    }
  }

  def getMappingByKeyword(keyword: String, forSystem : String): Either[String, MappingFound] = {
    try {
      val result: Option[MappingEntity] = Await.result(db.run(dao.mappings.filter(_.keyword === keyword).filter(_.forSystem === forSystem).result), timeout).headOption
      Right(MappingFound(result))
    } catch {
      case timeEx: TimeoutException => Left(s" Error while looking for a mapping with key $keyword and system $forSystem DB not responding")
      case e: Exception             => Left(s" Error while looking for a mapping with key $keyword and system $forSystem msg[$e.getMessage]")
    }
  }

  def createMapping(mapping: MappingEntity): Either[String, MappingCreated] = {
    try {
      Await.result(db.run(dao.mappings returning dao.mappings += mapping).map { mapping => Right(MappingCreated(mapping)) }, timeout)
    } catch {
      case timeEx: TimeoutException => Left(s" Error while creating message DB not responding")
      case e: Exception             => Left(s" Error while creating message msg[$e.getMessage]")
    }
  }

  def updateMapping(keyword: String, forSystem : String, messageUpdate: MappingEntityUpdate): Either[String, MappingUpdated] = {
    val eitherResponse = getMappingByKeyword(keyword, forSystem)

    if (eitherResponse.isLeft) {
      Left(eitherResponse.left.get)
    } else {
      val messageFound = eitherResponse.right.get
      messageFound.mapping match {
        case Some(mapping) =>
          try {
            val updatedMessage = messageUpdate.merge(mapping)
            Await.result(db.run(dao.mappings.filter(_.keyword === keyword).filter(_.forSystem === forSystem).update(updatedMessage)), timeout)
            Right(MappingUpdated(updatedMessage))
          } catch {
            case timeEx: TimeoutException => Left(s" Error while updating mapping key $keyword and system $forSystem  DB not responding")
            case e: Exception             => Left(s" Error while updating mapping key $keyword and system $forSystem msg[$e.getMessage]")
          }
        case None => Left("")
      }
    }
  }
  def deleteMapping(keyword: String, forSystem : String): Either[String, Int] = {
   try {
    val result = Await.result(db.run(dao.mappings.filter(_.keyword === keyword).filter(_.forSystem === forSystem).delete), timeout)
    Right(result)
   } catch {
     case timeEx: TimeoutException => Left(s" Error while deleting mapping key $keyword and system $forSystem DB not responding")
     case e: Exception             => Left(s" Error while deleting mapping key $keyword and system $forSystem msg[$e.getMessage]")
   }
  }

}