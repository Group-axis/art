package com.groupaxis.groupsuite.synchronizator.domain.model.assignment.backend

import com.groupaxis.groupsuite.persistence.datastore.jdbc.Database
import com.groupaxis.groupsuite.routing.write.domain.model.amh.assignment.{AMHAssignmentES, AMHAssignmentEntity, AMHAssignmentRuleES}
import com.groupaxis.groupsuite.synchronizator.domain.model.{AMHESRepository, AMHRepository, AMHService}
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.jackson.ElasticJackson

import scala.concurrent.{ExecutionContext, Future}

trait AMHBackendAssignmentRepositoryT extends AMHRepository[AMHAssignmentEntity, String] {
}

class AMHBackendAssignmentRepository(val db: Database) extends AMHBackendAssignmentRepositoryT {

  import com.groupaxis.groupsuite.routing.write.domain.model.amh.assignment.AssignmentDAO._
  import com.groupaxis.groupsuite.routing.write.domain.model.amh.assignment.AssignmentRuleDAO.amhAssignmentRules
  import driver.api._

  import scala.concurrent.ExecutionContext.Implicits.global

  override def query(id: String): Future[Option[AMHAssignmentEntity]] = {

    val action = for {
      assignments <- amhAssignments.filter(_.code === id).result
      rules <- amhAssignmentRules.filter(_.code === id).result
    } yield {
      assignments.headOption
        .map(assignment => assignment.copy(rules = rules))
    }

    db.db.run(action)
  }

  override def store(assignment: AMHAssignmentEntity): Future[AMHAssignmentEntity] = {
    db.db.run(amhAssignments returning amhAssignments += assignment)
  }

  override def all: Future[Seq[AMHAssignmentEntity]] = {
    val action = for {
      assignments <- amhAssignments.result
      rules <- amhAssignmentRules.result
    } yield {
      assignments.map(assignment => {
        val rulesAssignment = rules.filter(rule => {
          rule.code == assignment.code
        })
        assignment.copy(rules = rulesAssignment)
      })
    }

    db.db.run(action)
  }

  override def storeAll(assignments: Seq[AMHAssignmentEntity]): Future[Seq[AMHAssignmentEntity]] =
    db.db.run(amhAssignments returning amhAssignments ++= assignments)
}

object AMHBackendAssignmentRepository {
  def apply(db: Database) = {
    new AMHBackendAssignmentRepository(db)
  }
}

trait AMHBackendAssignmentRepositoryEST extends AMHESRepository[AMHAssignmentES] {
  implicit override def indexName: String = "amhrouting/assignments"
}

class AMHBackendAssignmentRepositoryES(val client: ElasticClient) extends AMHBackendAssignmentRepositoryEST {

  import ElasticJackson.Implicits._
  import com.sksamuel.elastic4s.ElasticDsl._
  import scala.concurrent.ExecutionContext.Implicits.global
  implicit override def esClient = client

  override def query(id: String): Future[Option[AMHAssignmentES]] = Future {
      None
    }

  override def store(a: AMHAssignmentES): Future[AMHAssignmentES] = Future {
      a
    }

  override def all: Future[Seq[AMHAssignmentES]] = Future {
      Seq()
    }

  override def storeAll(as: Seq[AMHAssignmentES]): Future[Seq[AMHAssignmentES]] = {
    val f = (backendAssignment: AMHAssignmentES) => index into indexName source backendAssignment id backendAssignment.code
    withBulk(f)(as)
  }

}

object AMHBackendAssignmentESRepository {
  def apply(esClient: ElasticClient) = {
    new AMHBackendAssignmentRepositoryES(esClient)
  }
}

sealed trait AMHSynchronousBackendAssignments {
  type AMHBackendAssignments = Seq[AMHAssignmentEntity]
  type AMHBackendAssignmentsES = Seq[AMHAssignmentES]
  type AMHBackendAssignmentRulesES = Seq[AMHAssignmentRuleES]
  type AMHRuleExpressionMap = Map[String, String]
}

object AMHSynchronousBackendAssignments extends AMHSynchronousBackendAssignments with AMHService[AMHAssignmentEntity, String, AMHAssignmentES] {

  def synchronize(ruleExpressionMap: AMHRuleExpressionMap)(implicit ec: ExecutionContext) = {
    updateIntoES(toES(ruleExpressionMap))
  }

  def toES(ruleExpressionMap: AMHRuleExpressionMap)(backendAssignments: AMHBackendAssignments)(implicit ec: ExecutionContext): Future[AMHBackendAssignmentsES] = {
    Future {
      backendAssignments.map(backendAssignment => backendAssignment.toES(backendAssignment.rules.map(r => r.toES(ruleExpressionMap.getOrElse(r.ruleCode, "")))))
    }
  }
}
