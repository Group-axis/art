package com.groupaxis.groupsuite.synchronizator.domain.model.rules

import com.groupaxis.groupsuite.persistence.datastore.jdbc.Database
import com.groupaxis.groupsuite.routing.write.domain.model.amh.rule.{AMHRuleEntity, AMHRuleEntityES}
import com.groupaxis.groupsuite.synchronizator.domain.model.{AMHESRepository, AMHRepository, AMHService}
import com.sksamuel.elastic4s.jackson.ElasticJackson
import com.sksamuel.elastic4s.{BulkCompatibleDefinition, ElasticClient}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait AMHRuleRepositoryT extends AMHRepository[AMHRuleEntity, String] {
  def ruleExpressionMap: Map[String, String]

  def ruleAssignedMap: Map[String, Boolean]
}

class AMHRuleRepository(val db: Database) extends AMHRuleRepositoryT {


  import com.groupaxis.groupsuite.routing.write.domain.model.amh.assignment.AssignmentRuleDAO.amhAssignmentRules
  import com.groupaxis.groupsuite.routing.write.domain.model.amh.distribution.copy.DistributionCpyRuleDAO.amhDistributionCpyRules
  import com.groupaxis.groupsuite.routing.write.domain.model.amh.feedback.distribution.copy.FeedbackDtnCpyRuleDAO.amhFeedbackDtnCpyRules
  import com.groupaxis.groupsuite.routing.write.domain.model.amh.rule.AMHRuleDAO._
  import driver.api._

  import scala.concurrent.ExecutionContext.Implicits.global

  override def ruleExpressionMap: Map[String, String] = {
    val timeout: Duration = 2.seconds

    val values = Try(Await.result(db.db.run(amhRules.map(rule => (rule.code, rule.expression)).result), timeout))
      .recoverWith({ case _ => Try(Seq()) }).get

    values
      .foldLeft(Map[String, String]())((a, v) => v._2.map(expr => a + (v._1 -> expr))
        .getOrElse(a))
  }

  override def ruleAssignedMap: Map[String, Boolean] = {
    val timeout: Duration = 2.seconds

    val action = for {
      backendAssignCodes <- amhAssignmentRules.map(_.ruleCode).result
      distributionAssignCodes <- amhDistributionCpyRules.map(_.ruleCode).result
      feedbackAssignCodes <- amhFeedbackDtnCpyRules.map(_.ruleCode).result
    } yield backendAssignCodes ++ distributionAssignCodes ++ feedbackAssignCodes

    val ruleCodes = Try(Await.result(db.db.run(action), timeout))
      .recoverWith({ case _ => Try(Seq())}).get

    ruleCodes.distinct map (_ -> true) toMap
  }

  override def query(id: String): Future[Option[AMHRuleEntity]] = {
    db.db.run(amhRules.filter(_.code === id).result.headOption)
  }

  override def store(rule: AMHRuleEntity): Future[AMHRuleEntity] = {
    db.db.run(amhRules returning amhRules += rule)
  }

  override def all: Future[Seq[AMHRuleEntity]] =
    db.db.run(amhRules.result)

  override def storeAll(rules: Seq[AMHRuleEntity]): Future[Seq[AMHRuleEntity]] =
    db.db.run(amhRules returning amhRules ++= rules)
}

object AMHRuleRepository {
  def apply(db: Database) = {
    new AMHRuleRepository(db)
  }
}




trait AMHRuleESRepositoryT extends AMHESRepository[AMHRuleEntityES] {

  implicit override def indexName: String = "amhrouting/rules"


}

class AMHRuleESRepository(val client: ElasticClient) extends AMHRuleESRepositoryT {
  import com.sksamuel.elastic4s.ElasticDsl._
  import ElasticJackson.Implicits._
  import scala.concurrent.ExecutionContext.Implicits.global

  implicit override def esClient = client

  override def query(id: String): Future[Option[AMHRuleEntityES]] = Future { None }

  override def store(a: AMHRuleEntityES): Future[AMHRuleEntityES] = Future { a }

  override def all: Future[Seq[AMHRuleEntityES]] = Future { Seq() }

  override def storeAll(as: Seq[AMHRuleEntityES]): Future[Seq[AMHRuleEntityES]] = {
    val f = (rule : AMHRuleEntityES) => index into indexName source rule id rule.code
    withBulk(f)(as)
  }

}

object AMHRuleESRepository {
  def apply(esClient: ElasticClient) = {
    new AMHRuleESRepository(esClient)
  }
}

sealed trait AMHSynchronousRules {
  type AMHRules = Seq[AMHRuleEntity]
  type AMHRulesES = Seq[AMHRuleEntityES]
  type RuleAssignmentStatus = Map[String, Boolean]
}

object AMHSynchronousRules extends AMHSynchronousRules with AMHService[AMHRuleEntity, String, AMHRuleEntityES] {

  def synchronize(implicit ec: ExecutionContext) : (AMHRepository[AMHRuleEntity, String], AMHRepository[AMHRuleEntityES, String]) => Future[Seq[AMHRuleEntityES]]
  = (repo, repoES) => {
    val ruleAssignmentStatus: RuleAssignmentStatus = repo.asInstanceOf[AMHRuleRepositoryT].ruleAssignedMap
    updateIntoES(toES(ruleAssignmentStatus)).apply(repo, repoES)
  }


  private def toES(rulesStatus: RuleAssignmentStatus)(rules: Seq[AMHRuleEntity])(implicit ec: ExecutionContext): Future[Seq[AMHRuleEntityES]] = {
    Future {
     rules.map(rule => rule.toES(rulesStatus.get(rule.code)))
    }
  }

}
