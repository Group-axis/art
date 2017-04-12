package com.groupaxis.groupsuite.synchronizator.domain.model.assignment.feedback

import com.groupaxis.groupsuite.persistence.datastore.jdbc.Database
import com.groupaxis.groupsuite.routing.write.domain.model.amh.feedback.distribution.copy.{AMHFeedbackDistributionCpyBackES, AMHFeedbackDistributionCpyES, AMHFeedbackDistributionCpyEntity, AMHFeedbackDistributionCpyRuleES}
import com.groupaxis.groupsuite.synchronizator.domain.model.{AMHESRepository, AMHRepository, AMHService}
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.jackson.ElasticJackson

import scala.concurrent.{ExecutionContext, Future}

trait AMHFeedbackDistributionAssignmentRepositoryT extends AMHRepository[AMHFeedbackDistributionCpyEntity, String] {
}

class AMHFeedbackDistributionAssignmentRepository(val db: Database) extends AMHFeedbackDistributionAssignmentRepositoryT {

  import com.groupaxis.groupsuite.routing.write.domain.model.amh.feedback.distribution.copy.FeedbackDtnCpyBackDAO.amhFeedbackDtnCpyBackends
  import com.groupaxis.groupsuite.routing.write.domain.model.amh.feedback.distribution.copy.FeedbackDtnCpyDAO._
  import com.groupaxis.groupsuite.routing.write.domain.model.amh.feedback.distribution.copy.FeedbackDtnCpyRuleDAO.amhFeedbackDtnCpyRules
  import driver.api._

  import scala.concurrent.ExecutionContext.Implicits.global

  override def query(id: String): Future[Option[AMHFeedbackDistributionCpyEntity]] = {

    val action = for {
      assignments <- amhFeedbackDistributionCps.filter(_.code === id).result
      backends <- amhFeedbackDtnCpyBackends.filter(_.code === id).result
      rules <- amhFeedbackDtnCpyRules.filter(_.code === id).result
    } yield {
      assignments.headOption
        .map(assignment => assignment.copy(backends = backends, rules = rules))
    }

    db.db.run(action)
  }

  override def store(assignment: AMHFeedbackDistributionCpyEntity): Future[AMHFeedbackDistributionCpyEntity] = {
    db.db.run(amhFeedbackDistributionCps returning amhFeedbackDistributionCps += assignment)
  }

  override def all: Future[Seq[AMHFeedbackDistributionCpyEntity]] = {
    val action = for {
      assignments <- amhFeedbackDistributionCps.result
      backends <- amhFeedbackDtnCpyBackends.result
      rules <- amhFeedbackDtnCpyRules.result
    } yield {
      assignments.map(assignment => {
        val backendsAssignment = backends.filter(backend => {
          backend.code == assignment.code
        })
        val rulesAssignment = rules.filter(rule => {
          rule.code == assignment.code
        })
        assignment.copy(backends = backendsAssignment, rules = rulesAssignment) //
      })
    }

    db.db.run(action)
  }

  override def storeAll(assignments: Seq[AMHFeedbackDistributionCpyEntity]): Future[Seq[AMHFeedbackDistributionCpyEntity]] =
    db.db.run(amhFeedbackDistributionCps returning amhFeedbackDistributionCps ++= assignments)
}

object AMHFeedbackDistributionAssignmentRepository {
  def apply(db: Database) = {
    new AMHFeedbackDistributionAssignmentRepository(db)
  }
}

trait AMHFeedbackDistributionAssignmentESRepositoryT extends AMHESRepository[AMHFeedbackDistributionCpyES] {
  implicit override def indexName: String = "amhrouting/feedbackDtnCopies"
}

class AMHFeedbackDistributionAssignmentESRepository(val client: ElasticClient) extends AMHFeedbackDistributionAssignmentESRepositoryT {

  import ElasticJackson.Implicits._
  import com.sksamuel.elastic4s.ElasticDsl._

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit override def esClient = client

  override def query(id: String): Future[Option[AMHFeedbackDistributionCpyES]] = Future {
    None
  }

  override def store(a: AMHFeedbackDistributionCpyES): Future[AMHFeedbackDistributionCpyES] = Future {
    a
  }

  override def all: Future[Seq[AMHFeedbackDistributionCpyES]] = Future {
    Seq()
  }

  override def storeAll(as: Seq[AMHFeedbackDistributionCpyES]): Future[Seq[AMHFeedbackDistributionCpyES]] = {
    val f = (feedback: AMHFeedbackDistributionCpyES) => index into indexName source feedback id feedback.code
    withBulk(f)(as)

  }

}

object AMHFeedbackDistributionAssignmentESRepository {
  def apply(es: ElasticClient) = {
    new AMHFeedbackDistributionAssignmentESRepository(es)
  }
}

sealed trait AMHSynchronousFeedbackAssignments {
  type AMHFeedbackAssignments = Seq[AMHFeedbackDistributionCpyEntity]
  type AMHFeedbackAssignmentsES = Seq[AMHFeedbackDistributionCpyES]
  type AMHFeedbackAssignmentBacksES = Seq[AMHFeedbackDistributionCpyBackES]
  type AMHFeedbackAssignmentRulesES = Seq[AMHFeedbackDistributionCpyRuleES]
  type AMHRuleExpressionMap = Map[String, String]
}

object AMHSynchronousFeedbackAssignments extends AMHSynchronousFeedbackAssignments with AMHService[AMHFeedbackDistributionCpyEntity, String, AMHFeedbackDistributionCpyES] {

  def synchronize(ruleExpressionMap: AMHRuleExpressionMap)(implicit ec: ExecutionContext) = {
    updateIntoES(toES(ruleExpressionMap))
  }

  def toES(ruleExpressionMap: AMHRuleExpressionMap)(feedbackAssignments: AMHFeedbackAssignments)(implicit ec: ExecutionContext): Future[AMHFeedbackAssignmentsES] = {
    Future {
      feedbackAssignments
        .map(feedbackAssignment => feedbackAssignment
          .toES(backends = feedbackAssignment.backends.map(_.toES),
            rules = feedbackAssignment.rules
              .map(rule => rule.toES(ruleExpressionMap.getOrElse(rule.ruleCode, "")))))
    }
  }
}
