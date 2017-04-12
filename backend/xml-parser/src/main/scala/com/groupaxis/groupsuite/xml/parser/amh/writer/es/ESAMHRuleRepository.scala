package com.groupaxis.groupsuite.xml.parser.amh.writer.es

import com.groupaxis.groupsuite.routing.write.domain.model.amh.rule.{AMHRuleAssignmentResponse, AMHRuleEntityES}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.analyzers.StandardAnalyzer
import com.sksamuel.elastic4s.jackson.ElasticJackson
import org.apache.logging.log4j.scala.Logging
import org.elasticsearch.action.update.UpdateResponse
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.sort.SortOrder

import scala.collection.mutable.ListBuffer
import scala.concurrent._
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class ESAMHRuleRepository(client: ElasticClient) extends Logging {

  def findAllAsOverview(assignType: Option[Boolean])(implicit ec: ExecutionContext): Future[Seq[AMHRuleEntityES]] = {
    import ElasticJackson.Implicits._

    def withTerm(simpleQuery: SearchDefinition, term: String) = simpleQuery query termQuery("assigned", term)
    def withSort(midleQuery: SearchDefinition) = midleQuery sort (field sort "code" order SortOrder.ASC)

    val overviewQuery = assignType
      .map(assigned => withTerm(search in "amhrouting/rules" from 0 size 20000, assignType.toString))
      .map(mq => withSort(mq))
      .getOrElse(withSort(search in "amhrouting/rules" from 0 size 20000))

    client.execute {
      overviewQuery
    }.map(resp => resp.as[AMHRuleEntityES].toSeq)
  }

  def deleteByCode(ruleCode: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    client.execute {
      delete id ruleCode from "amhrouting" / "rules"
    }
      .map(dr => dr.isFound)
  }

  def findAssingmentsByRuleCode(ruleCode: String)(implicit ec: ExecutionContext): Future[Seq[AMHRuleAssignmentResponse]] = {
    import ElasticJackson.Implicits._

    val response = findRuleInAllTypes(ruleCode)
      .map(r => r.as[AMHRuleAssignmentResponse].toSeq)

    response onComplete {
      case Success(sequence) => logger.debug(s"Rule '$ruleCode' was found ${sequence.size} time(s) in all assignments.")
      case Failure(t) => logger.debug(s"Error while searching for rule assignments ${t.getLocalizedMessage}")
    }

    response
  }

  private def predicate(condition: Boolean)(fail: Exception)(implicit ec: ExecutionContext): Future[Unit] =
    if (condition) Future(()) else Future.failed(fail)

  private def findRuleInAllTypes(ruleCode: String): Future[RichSearchResponse] = {
    client.execute {
      search in "amhrouting" types("assignments", "distributionCopies", "feedbackDtnCopies") rawQuery {
        s"""{ "query": { "bool": { "should": [ { "nested": { "path": "rules", "query": { "bool": { "must": [ { "match":  { "rules.code": { "query": "$ruleCode", "analyzer": "standard", "operator": "and" } } } ] } } } } ] } } }"""
      }
    }
  }

  private def findRule(ruleCode: String)(implicit ec: ExecutionContext): Future[Option[AMHRuleEntityES]] = {
    import ElasticJackson.Implicits._
    client.execute {
      search in "amhrouting/rules" query termQuery("_id", ruleCode)
    }.map(resp => resp.as[AMHRuleEntityES].toSeq).map(rules => rules.headOption)
  }

  def updateAssignedFromRemove(ruleCode: String)(implicit ec: ExecutionContext): Future[UpdateResponse] = {
    for {
      hits <- findRuleInAllTypes(ruleCode).map(r => r.totalHits)
      _ <- predicate(hits <= 0)(new Exception(s"Rule $ruleCode is still assigned"))
      response <- updateAssignedStatus(ruleCode, status = false)
    } yield response
  }

  def updateAssignedFromAdd(ruleCode: String)(implicit ec: ExecutionContext): Future[UpdateResponse] = {
    updateAssignedStatus(ruleCode, status = true)
  }

  private def updateAssignedStatus(ruleCode: String, status: Boolean)(implicit ec: ExecutionContext): Future[UpdateResponse] = {
    val updateResp = for {
      ruleOpt <- findRule(ruleCode)
      _ <- predicate(ruleOpt.isDefined)(new Exception(s"Rule $ruleCode not found"))
      response <- updateRule(ruleOpt.get.copy(assigned = Some(status)))
    } yield response

    updateResp.recover {
      case ex: Exception =>
        logger.debug(s"An error has occurred while updating assigned status on rule $ruleCode : ${ex.getLocalizedMessage}")
        new UpdateResponse("error", ex.getLocalizedMessage, ruleCode, -1, false)
    }

    updateResp
  }

  def insert(rules: Seq[AMHRuleEntityES]) {
    logger.debug("----------- BEFORE RULES ----------")
    withBulk(rules)
    //    withoutBulk(rules)
    logger.debug("----------- AFTER RULES ----------")
  }

  private def withBulk(rules: Seq[AMHRuleEntityES]) {
    import ElasticJackson.Implicits._

    val bulkOps = new ListBuffer[BulkCompatibleDefinition]()
    for (rule <- rules) yield {
      bulkOps += index into "amhrouting" -> "rules" source rule id rule.code
    }
    if (bulkOps.nonEmpty) {
      val res = client.execute(bulk(bulkOps)).await
      logger.debug("rules BULK DONE!! " + res)
    } else {
      logger.debug("No rules to import!! ")
    }
  }

  //  private def bulkRules(rules: Seq[AMHRuleEntityES]): Iterable[BulkCompatibleDefinition] = {
  //    import com.sksamuel.elastic4s.jackson.ElasticJackson.Implicits._
  //    var bulkRules: List[IndexDefinition] = List()
  //    rules.foreach(rule => { bulkRules.+:(index into "routing/rules" source rule id rule.code) })
  //    bulkRules
  //  }

  def insert(rule: AMHRuleEntityES): Future[IndexResult] = {
    //import com.sksamuel.elastic4s.jackson.ElasticJackson.Implicits._
    import ElasticJackson.Implicits._

    import scala.concurrent.ExecutionContext.Implicits.global

    val res = client.execute {
      index into "amhrouting/rules" source rule id rule.code
    }

    res onComplete {
      case Success(s) => logger.debug(s" success $s")
      case Failure(t) => logger.debug(s"An error has occured: $t")
    }
    res
  }

  def updateRule(rule: AMHRuleEntityES)(implicit ec: ExecutionContext): Future[UpdateResponse] = {
    import ElasticJackson.Implicits._

    val res = client.execute {
      update id rule.code in "amhrouting/rules" docAsUpsert rule
    }

    res onComplete {
      case Success(s) => logger.debug(s" Rule ${s.getId} successfully updated.")
      case Failure(t) => logger.debug(s"An error has occurred: $t")
    }
    res
  }

  def findExpressionByCode(code: String): String = {
    val res = client.execute {
      search in "amhrouting/rules" sourceInclude("code", "expression") query {
        matchQuery("code", code) analyzer StandardAnalyzer operator "AND"
      }
    }
    val response: RichSearchResponse = Await.result(res, 5.seconds)

    if (response.hits.length > 0) {
      val expressionFound = response.hits.head.sourceAsMap.get("expression").map(ref => ref.asInstanceOf[String]).getOrElse("")
      logger.debug(s" success " + expressionFound)
      return expressionFound
    }

    logger.debug(s" Expression not found for rule code " + code)
    ""
  }

  def findRuleIdByCode(ruleCode: String): String = {

    val res = client.execute {
      search in "amhrouting/rules" sourceInclude "code" size 50
    }

    //    res onComplete {
    //      case Success(s) =>
    //        val found : Option[SearchHit]= s.getHits.getHits.find(searchHit =>
    //          searchHit.getSource().get("code") equals (ruleCode)
    //        )
    //
    //        logger.debug(s" success $found")
    //        id = found.map(hit => hit.getId).getOrElse("")
    //      case Failure(t) => logger.debug(s"An error has occured: $t")
    //    }

    val response: RichSearchResponse = Await.result(res, 5.seconds)

    val found: Option[SearchHit] = response.getHits.getHits.find(searchHit => {
      searchHit.getSource.get("code") equals ruleCode
    })

    logger.debug(s" success $found")
    found.map(hit => hit.getId).getOrElse("NOT_FOUND")
  }

}
