package com.groupaxis.groupsuite.xml.parser.amh.writer.es

import com.groupaxis.groupsuite.routing.amh.read.domain.model.view.assignment.overview.AMHAssignmentOverviewES
import com.groupaxis.groupsuite.routing.write.domain.model.amh.distribution.copy.AMHDistributionCpyES
import com.groupaxis.groupsuite.routing.write.domain.model.amh.rule.AMHRuleEntityES
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.jackson.ElasticJackson
import com.sksamuel.elastic4s.{ElasticClient, _}
import org.apache.logging.log4j.scala.Logging
import org.elasticsearch.action.update.UpdateResponse
import org.elasticsearch.search.sort.SortOrder

import scala.collection.mutable.ListBuffer
import scala.concurrent._
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class ESAMHDistributionCpyRepository(client: ElasticClient) extends Logging {

  def findAllAsOverview()(implicit ec: ExecutionContext): Future[Seq[AMHAssignmentOverviewES]] = {
    import ElasticJackson.Implicits._

    client.execute {
      search in "amhrouting/distributionCopies" sort {field sort "sequence" order SortOrder.ASC}
    }
      .map(resp => resp.as[AMHDistributionCpyES].toSeq)
      .map(distributionCps =>  distributionCps.flatMap(distributionCpy => distributionCpy.toOverview))
  }

  def insert(distributions: Seq[AMHDistributionCpyES]) {
    withBulk(distributions)
  }

  private def withBulk(distributions: Seq[AMHDistributionCpyES]) {
    import ElasticJackson.Implicits._

    val bulkOps = new ListBuffer[BulkCompatibleDefinition]()
    for (distribution <- distributions) yield {
      bulkOps += index into "amhrouting/distributionCopies" source distribution id distribution.code
    }
    val res = client.execute(bulk(bulkOps)).await

    logger.debug(" distribution BULK DONE!! " + res)
  }

  def insert(distribution: AMHDistributionCpyES): Future[IndexResult] = {
    //import com.sksamuel.elastic4s.jackson.ElasticJackson.Implicits._
    import ElasticJackson.Implicits._

    import scala.concurrent.ExecutionContext.Implicits.global

    val res = client.execute {
      index into "amhrouting/distributionCopies" source distribution id distribution.code
    }

    res onComplete {
      case Success(s) => logger.debug(s" success $s")
      case Failure(t) => logger.error(s"An error has occured: $t")
    }
    res
  }

  def updateDistributionCpy(distribution: AMHDistributionCpyES): Future[UpdateResponse] = {
    import ElasticJackson.Implicits._

    import scala.concurrent.ExecutionContext.Implicits.global

    val res = client.execute {
      update id distribution.code in "amhrouting/distributionCopies" docAsUpsert distribution
    }

    res onComplete {
      case Success(s) => logger.debug(s" Update of amhrouting/distributionCopies/${distribution.code} succeed ")
      case Failure(t) => logger.error(s"An error has occurred while updating amhrouting/distributionCopies/$distribution.code: $t")
    }

    res
  }

  def unassignRuleByCode(code: String, ruleCode: String)(implicit ec: ExecutionContext): Future[UpdateResponse] = {
    import ElasticJackson.Implicits._

    client.execute {
      search in "amhrouting/distributionCopies" query termQuery("_id", code)
    }
      .map(resp => resp.as[AMHDistributionCpyES].toSeq)
      .flatMap(distributionCps => {
        val distributionOption = distributionCps.headOption
        distributionOption
          .map(distribution => distribution.copy(rules = distribution.rules.filter(rule => rule.code != ruleCode)))
          .map(distribution => updateDistributionCpy(distribution))
          .getOrElse(noDistributionFoundFuture(s"No distributionCopy found with code $code"))
      })
  }

  def noDistributionFoundFuture(logMsg: String)(implicit ec: ExecutionContext): Future[UpdateResponse] = {
    Future {
      logger.debug(logMsg)
      new UpdateResponse()
    }
  }

  def updateDistributionCps(ruleES: AMHRuleEntityES): Int = {
    import ElasticJackson.Implicits._

    val res = client.execute {
      search in "amhrouting/distributionCopies" size 500
    }

    val result: RichSearchResponse = Await.result(res, 15.seconds)

    val distributionCps: Seq[AMHDistributionCpyES] = result.as[AMHDistributionCpyES]

    val distributionFiltered = distributionCps.filter(distribution => distribution.rules.exists(_.code equals ruleES.code))

    val updatedDistributionCps: Seq[AMHDistributionCpyES] = distributionFiltered.map(distribution => {
      distribution.copy(rules = distribution.rules.map(rule => {
        if (rule.code equals ruleES.code) {
          rule.copy(expression = ruleES.expression.getOrElse(""))
        } else {
          rule
        }
      }))
    })

    updatedDistributionCps.foreach(distribution => {
      logger.debug("found Rules to update in distributionCpy " + distribution)
      updateDistributionCpy(distribution)
    })

    updatedDistributionCps.size
  }

}
