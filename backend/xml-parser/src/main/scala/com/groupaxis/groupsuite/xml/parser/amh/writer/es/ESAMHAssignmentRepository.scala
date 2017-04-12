package com.groupaxis.groupsuite.xml.parser.amh.writer.es

import com.groupaxis.groupsuite.routing.amh.read.domain.model.view.assignment.overview.AMHAssignmentOverviewES
import com.groupaxis.groupsuite.routing.write.domain.model.amh.assignment.AMHAssignmentES
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

class ESAMHAssignmentRepository(client : ElasticClient) extends Logging {
  final val UpdateNotFound = new UpdateResponse("", "not_found", "not_found", -1, false)

  def findAllAsOverview(implicit ec: ExecutionContext): Future[Seq[AMHAssignmentOverviewES]] = {
    import ElasticJackson.Implicits._

    client.execute {
      search in "amhrouting/assignments" from 0 size 20000 sort {field sort "sequence" order SortOrder.ASC}
    }
    .map(resp => resp.as[AMHAssignmentES].toSeq)
    .map(assignments =>  assignments.flatMap(assignment => assignment.toOverview))
  }

  def unassignRuleByCode(code: String, ruleCode: String)(implicit ec: ExecutionContext): Future[UpdateResponse] = {
    import ElasticJackson.Implicits._

    client.execute {
      search in "amhrouting/assignments" query termQuery("_id", code)
    }
      .map(resp => resp.as[AMHAssignmentES].toSeq)
      .flatMap(distributionCps => {
        val distributionOption = distributionCps.headOption
        distributionOption
          .map(distribution => distribution.copy(rules = distribution.rules.filter(rule => rule.code != ruleCode)))
          .map(distribution => updateAssignment(distribution))
          .getOrElse(noDistributionFoundFuture(s"No assignment found with code $code"))
      })
  }

  def noDistributionFoundFuture(logMsg: String)(implicit ec: ExecutionContext): Future[UpdateResponse] = {
    Future {
      logger.debug(logMsg)
      UpdateNotFound
    }
  }



  def insert(assignments: Seq[AMHAssignmentES]) {
    withBulk(assignments)
  }

  private def withBulk(assignments: Seq[AMHAssignmentES]) {

    import ElasticJackson.Implicits._
    val bulkOps = new ListBuffer[BulkCompatibleDefinition]()
    for (assignment <- assignments) yield { bulkOps += index into "amhrouting/assignments" source assignment id assignment.code }
    val res = client.execute(bulk(bulkOps)).await
    logger.debug("assignments BULK DONE!! " + res)
  }

  def insert(assignment: AMHAssignmentES): Future[IndexResult] = {
    //import com.sksamuel.elastic4s.jackson.ElasticJackson.Implicits._
    import ElasticJackson.Implicits._
    import scala.concurrent.ExecutionContext.Implicits.global

    val res = client.execute {
      index into "amhrouting/assignments" source assignment id assignment.code
    }

    res onComplete {
      case Success(s) => logger.debug(s" success $s")
      case Failure(t) => logger.debug(s"An error has occured: $t")
    }
    res
  }

  def updateAssignment(assignment: AMHAssignmentES): Future[UpdateResponse] = {
    import ElasticJackson.Implicits._

    import scala.concurrent.ExecutionContext.Implicits.global

    val res = client.execute {
      update id assignment.code in "amhrouting/assignments" docAsUpsert assignment
    }

    res onComplete {
      case Success(s) => logger.debug(s" Update of amhrouting/assignments/${assignment.code} succeed")
      case Failure(t) => logger.debug(s"An error has occurred while updating amhrouting/assignments/${assignment.code} : $t")
    }

    res
  }

    def updateAssignments(ruleES: AMHRuleEntityES): Int = {
      import ElasticJackson.Implicits._

      val res = client.execute {
        search in "amhrouting/assignments" size 500
      }

      val result : RichSearchResponse = Await.result(res, 15.seconds)

      val assignments :Seq[AMHAssignmentES]= result.as[AMHAssignmentES]

      val assignmentFiltered = assignments.filter( assign => assign.rules.exists(_.code equals ruleES.code))

      val updatedAssignments : Seq[AMHAssignmentES]= assignmentFiltered.map(assign => {
        assign.copy(rules = assign.rules.map(rule => {
          if(rule.code equals ruleES.code) {
            rule.copy(expression=ruleES.expression.getOrElse(""))
          } else {
            rule
          }
        }))
      })

      updatedAssignments.foreach(assign => {
        logger.debug("found Rules to update in assignment " + assign)
        updateAssignment(assign)
      })

      updatedAssignments.size
    }

}
