package com.groupaxis.groupsuite.synchronizator.domain.model.simulation.swift.mapping

import com.groupaxis.groupsuite.persistence.datastore.jdbc.Database
import com.groupaxis.groupsuite.simulator.write.domain.model.mapping.MappingEntity
import com.groupaxis.groupsuite.synchronizator.domain.model.AMHRepository

import scala.concurrent.Future

trait AMHMessageMappingRepositoryT extends AMHRepository[MappingEntity, String] {
  def allMappingsBySystem(forSystem : String) : Future[Seq[MappingEntity]]
}

class AMHMessageMappingRepository(val db: Database) extends AMHMessageMappingRepositoryT {


  import com.groupaxis.groupsuite.simulator.write.domain.model.mapping.MessageMappingDAO._
  import driver.api._

  //  import scala.concurrent.ExecutionContext.Implicits.global

  override def query(id: String): Future[Option[MappingEntity]] = {
    db.db.run(mappings.filter(_.keyword === id).result.headOption)
  }

  override def store(message: MappingEntity): Future[MappingEntity] = {
    db.db.run(mappings returning mappings += message)
  }

  override def all: Future[Seq[MappingEntity]] = {
    db.db.run(mappings.result)
  }


  override def storeAll(newMappings: Seq[MappingEntity]): Future[Seq[MappingEntity]] =
    db.db.run(mappings returning mappings ++= newMappings)

  def allMappingsBySystem(forSystem : String) : Future[Seq[MappingEntity]] =
    db.db.run(mappings.filter(_.forSystem === forSystem).result)

}

object AMHMessageMappingRepository {
  def apply(db: Database) = {
    new AMHMessageMappingRepository(db)
  }
}


sealed trait AMHSynchronousMessageMappings {
  type AMHMessageMappings = Seq[MappingEntity]
}

object AMHSynchronousMessageMappings extends AMHSynchronousMessageMappings {

}
