package com.groupaxis.groupsuite.xml.parser.amh.writer.es

import com.groupaxis.groupsuite.routing.amh.read.domain.model.view.assignment.overview.AMHAssignmentOverviewES
import com.groupaxis.groupsuite.routing.write.domain.model.amh.distribution.copy.AMHDistributionCpyES
import com.groupaxis.groupsuite.routing.write.domain.model.amh.feedback.distribution.copy.AMHFeedbackDistributionCpyES
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

class ESAMHFeedbackDtnCpyRepository(client : ElasticClient) extends Logging {

  def findAllAsOverview(implicit ec: ExecutionContext): Future[Seq[AMHAssignmentOverviewES]] = {
    import ElasticJackson.Implicits._

    client.execute {
      search in "amhrouting/feedbackDtnCopies" sort {field sort "sequence" order SortOrder.ASC }
    }
      .map(resp => resp.as[AMHFeedbackDistributionCpyES].toSeq)
      .map(feedbackDtnCopies =>  feedbackDtnCopies.flatMap(feedbackDtnCopy => feedbackDtnCopy.toOverview))
  }

  def insert(feedbacks: Seq[AMHFeedbackDistributionCpyES]) {
        withBulk(feedbacks)
  }

  private def withBulk(feedbacks: Seq[AMHFeedbackDistributionCpyES]) {
    import ElasticJackson.Implicits._

    val bulkOps = new ListBuffer[BulkCompatibleDefinition]()
    for (feedback <- feedbacks) yield { bulkOps += index into "amhrouting/feedbackDtnCopies" source feedback id feedback.code }
    val res = client.execute(bulk(bulkOps)).await
    logger.debug(" feedback BULK DONE!! " + res)
  }

  def insert(feedback: AMHFeedbackDistributionCpyES): Future[IndexResult] = {
    //import com.sksamuel.elastic4s.jackson.ElasticJackson.Implicits._
    import ElasticJackson.Implicits._

    import scala.concurrent.ExecutionContext.Implicits.global

    val res = client.execute {
      index into "amhrouting/feedbackDtnCopies" source feedback id feedback.code
    }

    res onComplete {
      case Success(s) => logger.debug(s" success $s")
      case Failure(t) => logger.error(s"An error has occured: $t")
    }
    res
  }

  def updateFeedbackDtnCpy(feedback: AMHFeedbackDistributionCpyES): Future[UpdateResponse] = {
    import ElasticJackson.Implicits._

    import scala.concurrent.ExecutionContext.Implicits.global

    val res = client.execute {
      update id feedback.code in "amhrouting/feedbackDtnCopies" docAsUpsert feedback
    }

    res onComplete {
      case Success(s) => logger.debug(s"Update of amhrouting/feedbackDtnCopies/${feedback.code} succeed ")
      case Failure(t) => logger.error(s"An error has occurred while updating amhrouting/feedbackDtnCopies/${feedback.code}: $t")
    }

    res
  }

  def updateFeedbackDtnCps(ruleES: AMHRuleEntityES): Int = {
      import ElasticJackson.Implicits._

      val res = client.execute {
        search in "amhrouting/feedbackDtnCopies" size 500
      }

      val result : RichSearchResponse = Await.result(res, 15.seconds)

      val feedbackDtnCps :Seq[AMHFeedbackDistributionCpyES]= result.as[AMHFeedbackDistributionCpyES]

      val feedbackFiltered = feedbackDtnCps.filter( feedback => feedback.rules.exists(_.code equals ruleES.code))

      val updatedFeedbackDtnCps : Seq[AMHFeedbackDistributionCpyES]= feedbackFiltered.map(feedback => {
        feedback.copy(rules = feedback.rules.map(rule => {
          if(rule.code equals ruleES.code) {
            rule.copy(expression=ruleES.expression.getOrElse(""))
          } else {
            rule
          }
        }))

      })

      updatedFeedbackDtnCps.foreach(feedback => {
        logger.debug("found Rules to update in feedbackDtnCpy " + feedback)
        updateFeedbackDtnCpy(feedback)
      })

      updatedFeedbackDtnCps.size
    }

  def unassignRuleByCode(code: String, ruleCode: String)(implicit ec: ExecutionContext): Future[UpdateResponse] = {
    import ElasticJackson.Implicits._

    client.execute {
      search in "amhrouting/feedbackDtnCopies" query termQuery("_id", code)
    }
      .map(resp => resp.as[AMHFeedbackDistributionCpyES].toSeq)
      .flatMap(distributionCps => {
        val distributionOption = distributionCps.headOption
        distributionOption
          .map(distribution => distribution.copy(rules = distribution.rules.filter(rule => rule.code != ruleCode)))
          .map(distribution => updateFeedbackDtnCpy(distribution))
          .getOrElse(noDistributionFoundFuture(s"No feedback Distribution Copy found with code $code"))
      })
  }

  def noDistributionFoundFuture(logMsg: String)(implicit ec: ExecutionContext): Future[UpdateResponse] = {
    Future {
      logger.debug(logMsg)
      new UpdateResponse()
    }
  }
}
