package com.groupaxis.groupsuite.synchronizator.domain.model.assignment.distribution

import com.groupaxis.groupsuite.persistence.datastore.jdbc.Database
import com.groupaxis.groupsuite.routing.write.domain.model.amh.distribution.copy.{AMHDistributionCpyBackES, AMHDistributionCpyES, AMHDistributionCpyEntity, AMHDistributionCpyRuleES}
import com.groupaxis.groupsuite.synchronizator.domain.model.{AMHESRepository, AMHRepository, AMHService}
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.jackson.ElasticJackson

import scala.concurrent.{ExecutionContext, Future}

trait AMHDistributionAssignmentRepositoryT extends AMHRepository[AMHDistributionCpyEntity, String] {
}

class AMHDistributionAssignmentRepository(val db: Database) extends AMHDistributionAssignmentRepositoryT {

  import com.groupaxis.groupsuite.routing.write.domain.model.amh.distribution.copy.DistributionCpyBackendDAO.amhDistributionCpyBackends
  import com.groupaxis.groupsuite.routing.write.domain.model.amh.distribution.copy.DistributionCpyDAO._
  import com.groupaxis.groupsuite.routing.write.domain.model.amh.distribution.copy.DistributionCpyRuleDAO.amhDistributionCpyRules
  import driver.api._

  import scala.concurrent.ExecutionContext.Implicits.global

  override def query(id: String): Future[Option[AMHDistributionCpyEntity]] = {

    val action = for {
      assignments <- amhDistributionCps.filter(_.code === id).result
      backends <- amhDistributionCpyBackends.filter(_.code === id).result
      rules <- amhDistributionCpyRules.filter(_.code === id).result
    } yield {
      assignments.headOption
        .map(assignment => assignment.copy(backends = backends, rules = rules))
    }

    db.db.run(action)
  }

  override def store(assignment: AMHDistributionCpyEntity): Future[AMHDistributionCpyEntity] = {
    db.db.run(amhDistributionCps returning amhDistributionCps += assignment)
  }

  override def all: Future[Seq[AMHDistributionCpyEntity]] = {
    val action = for {
      assignments <- amhDistributionCps.result
      backends <- amhDistributionCpyBackends.result
      rules <- amhDistributionCpyRules.result
    } yield {
      assignments.map(assignment => {
        val backendsAssignment = backends.filter(backend => {
          backend.code == assignment.code
        })
        val rulesAssignment = rules.filter(rule => {
          rule.code == assignment.code
        })
        assignment.copy(backends = backendsAssignment, rules = rulesAssignment)
      })
    }

    db.db.run(action)
  }

  override def storeAll(assignments: Seq[AMHDistributionCpyEntity]): Future[Seq[AMHDistributionCpyEntity]] =
    db.db.run(amhDistributionCps returning amhDistributionCps ++= assignments)
}

object AMHDistributionAssignmentRepository {
  def apply(db: Database) = {
    new AMHDistributionAssignmentRepository(db)
  }
}

trait AMHDistributionAssignmentRepositoryEST extends AMHESRepository[AMHDistributionCpyES] {
  implicit override def indexName: String = "amhrouting/distributionCopies"
}

class AMHDistributionAssignmentRepositoryES(val client: ElasticClient) extends AMHDistributionAssignmentRepositoryEST {

  import ElasticJackson.Implicits._
  import com.sksamuel.elastic4s.ElasticDsl._

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit override def esClient = client

  override def query(id: String): Future[Option[AMHDistributionCpyES]] = Future {
      None
    }

  override def store(a: AMHDistributionCpyES): Future[AMHDistributionCpyES] = Future {
      a
    }

  override def all: Future[Seq[AMHDistributionCpyES]] = Future {
      Seq()
    }

  override def storeAll(as: Seq[AMHDistributionCpyES]): Future[Seq[AMHDistributionCpyES]] = {
    val f = (distribution: AMHDistributionCpyES) => index into indexName source distribution id distribution.code
    withBulk(f)(as)
  }

}
object AMHDistributionAssignmentESRepository {
  def apply(es: ElasticClient) = {
    new AMHDistributionAssignmentRepositoryES(es)
  }
}

sealed trait AMHSynchronousDistributionAssignments {
  type AMHDistributionAssignments = Seq[AMHDistributionCpyEntity]
  type AMHDistributionAssignmentsES = Seq[AMHDistributionCpyES]
  type AMHDistributionAssignmentBacksES = Seq[AMHDistributionCpyBackES]
  type AMHDistributionAssignmentRulesES = Seq[AMHDistributionCpyRuleES]
  type AMHRuleExpressionMap = Map[String, String]
}

object AMHSynchronousDistributionAssignments extends AMHSynchronousDistributionAssignments with AMHService[AMHDistributionCpyEntity, String, AMHDistributionCpyES] {

  def synchronize(ruleExpressionMap: AMHRuleExpressionMap)(implicit ec: ExecutionContext) = {
    updateIntoES(toES(ruleExpressionMap))
  }

  def toES(ruleExpressionMap: AMHRuleExpressionMap)(distributionAssignments: AMHDistributionAssignments)(implicit ec: ExecutionContext): Future[AMHDistributionAssignmentsES] = {
    Future {
      distributionAssignments
        .map(distributionAssignment => distributionAssignment
          .toES(backends = distributionAssignment.backends.map(_.toES),
            rules = distributionAssignment.rules.map(rule => rule.toES(ruleExpressionMap.getOrElse(rule.ruleCode, "")))))
    }
  }
}