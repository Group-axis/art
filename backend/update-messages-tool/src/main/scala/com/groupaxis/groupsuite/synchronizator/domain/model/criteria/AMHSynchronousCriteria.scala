package com.groupaxis.groupsuite.synchronizator.domain.model.criteria

import com.fasterxml.jackson.databind.SerializationFeature
import com.groupaxis.groupsuite.routing.write.domain.model.amh.criteria.{AMHRuleCriteriaEntity, AMHRuleCriteriaEntityES}
import com.groupaxis.groupsuite.synchronizator.domain.model.{AMHESRepository, AMHRepository, AMHService}
import com.groupaxis.groupsuite.synchronizator.file.GPFileHelper._
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.jackson.{ElasticJackson, JacksonJson}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait AMHRuleCriteriaRepositoryT extends AMHRepository[AMHRuleCriteriaEntity, String] {
}

class AMHRuleCriteriaFileRepository(val filePath: String) extends AMHRuleCriteriaRepositoryT with Logging {
  import scala.concurrent.ExecutionContext.Implicits.global

  override def query(id: String): Future[Option[AMHRuleCriteriaEntity]] = {
    Future{None}
  }

  override def store(criterion: AMHRuleCriteriaEntity): Future[AMHRuleCriteriaEntity] = {
    Future{criterion}
  }

  override def all: Future[Seq[AMHRuleCriteriaEntity]] = {
    import JacksonJson._
    val (source, lines) = readFileAsIterator(filePath)

    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)

    val fileAsString = lines.map(_.mkString).getOrElse("[{}]")

    val allAMHRuleCriteria = Try(mapper.readValue[Seq[AMHRuleCriteriaEntity]](fileAsString))

    source.close()

    allAMHRuleCriteria match {
      case Success(criteria) => Future{criteria}
      case Failure(ex) => Future.failed(ex)
    }

  }

  override def storeAll(criteria: Seq[AMHRuleCriteriaEntity]): Future[Seq[AMHRuleCriteriaEntity]] =
    Future{criteria}
}

object AMHRuleCriteriaFileRepository {
  def apply(filePath: String) = {
    new AMHRuleCriteriaFileRepository(filePath: String)
  }
}




trait AMHRuleCriteriaESRepositoryT extends AMHESRepository[AMHRuleCriteriaEntityES] {

  implicit override def indexName: String = "amhreference/criteria"

}

class AMHRuleCriteriaESRepository(val client: ElasticClient) extends AMHRuleCriteriaESRepositoryT {
  import ElasticJackson.Implicits._
  import com.sksamuel.elastic4s.ElasticDsl._

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit override def esClient = client

  override def query(id: String): Future[Option[AMHRuleCriteriaEntityES]] = Future { None }

  override def store(a: AMHRuleCriteriaEntityES): Future[AMHRuleCriteriaEntityES] = Future { a }

  override def all: Future[Seq[AMHRuleCriteriaEntityES]] = Future { Seq() }

  override def storeAll(as: Seq[AMHRuleCriteriaEntityES]): Future[Seq[AMHRuleCriteriaEntityES]] = {
    val f = (criteria : AMHRuleCriteriaEntityES) => index into indexName source criteria id criteria.code
    withBulk(f)(as)
  }

}

object AMHRuleCriteriaESRepository {
  def apply(esClient: ElasticClient) = {
    new AMHRuleCriteriaESRepository(esClient)
  }
}

sealed trait AMHSynchronousRuleCriteria {
  type AMHRuleCriteria = Seq[AMHRuleCriteriaEntity]
  type AMHRulesCriteriaES = Seq[AMHRuleCriteriaEntityES]
}

object AMHSynchronousRuleCriteria extends AMHSynchronousRuleCriteria with AMHService[AMHRuleCriteriaEntity,String, AMHRuleCriteriaEntityES] with Logging {

  def synchronize(implicit ec: ExecutionContext) : (AMHRepository[AMHRuleCriteriaEntity, String], AMHRepository[AMHRuleCriteriaEntityES, String]) => Future[AMHRulesCriteriaES]
  = (repo, repoES) => {
    updateIntoES(toES).apply(repo, repoES)
  }

  private def toES(criteria: AMHRuleCriteria)(implicit ec: ExecutionContext): Future[AMHRulesCriteriaES] = {
    Future {
     criteria.map(criterion => criterion.toES)
    }
  }

}