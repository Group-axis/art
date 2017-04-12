package com.groupaxis.groupsuite.xml.parser.amh.writer.es

import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import org.apache.logging.log4j.scala.Logging
import org.elasticsearch.common.settings.Settings

import scala.concurrent.{Await, Future}


object ESAMHAssignmentRepoTest extends Logging {

   def esClient : ElasticClient = {
     val settings = Settings.settingsBuilder().put("cluster.name", "groupsuite").build()
     ElasticClient.transport(settings, ElasticsearchClientUri(s"elasticsearch://127.0.0.1:9300"))
   }

  def log(msg: String) {
    logger.debug(s"${Thread.currentThread.getName}: $msg")
  }

}
//
object testQuery extends App with Logging
{

  import ESAMHAssignmentRepoTest._

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._
  val client = esClient
  val esRuleRepo = new ESAMHRuleRepository(client)
  val esAssignRepo = new ESAMHAssignmentRepository(client)
  val esDtnCpyRepo = new ESAMHDistributionCpyRepository(client)
  val esFeedDtnCpyRepo = new ESAMHFeedbackDtnCpyRepository(client)

  def testAssignmentUpdate(ruleCode : String) = {
    //    val r : Future[Seq[UpdateResponse]] =
    esRuleRepo.findAssingmentsByRuleCode(ruleCode)
      .flatMap(assignsFound => {
        Future.sequence(
          assignsFound.map(assignFound =>{
            assignFound._type match {
              case "assignments" => esAssignRepo.unassignRuleByCode(assignFound.code, ruleCode)
              case "distributionCopies" => esDtnCpyRepo.unassignRuleByCode(assignFound.code, ruleCode)
              case "feedbackDtnCopies" => esFeedDtnCpyRepo.unassignRuleByCode(assignFound.code, ruleCode)
            }
          })
        )
      })
    log("assign executing....")
    Thread.sleep(5000)
    log(".......... assign Done")
  }

  def testDtnCopyUpdate(ruleCode : String) = {
//    val r : Future[Seq[UpdateResponse]] =
      esRuleRepo.findAssingmentsByRuleCode(ruleCode)
      .flatMap(assignsFound => {
        Future.sequence(
          assignsFound.map(assignFound =>
              esDtnCpyRepo.unassignRuleByCode(assignFound.code, ruleCode))
        )
      })
log("distribution executing....")
    Thread.sleep(2000)
log("..........distribution Done")
   }

  def testSearch(ruleCode: String) = {
    log("........... begin!")
    //  val assigns = assignRepo.findAssingmentsByRuleCode("BA-PARBITMM-T2S-AEMM6")
    val s = for {
      //regle_match_mt499
      assign <- esRuleRepo.findAssingmentsByRuleCode(ruleCode)
    //    assign <- assignRepo.findAssingmentsByRuleCode("BA-DelNotif-FDA05-to-FDA08")
    } yield {
      logger.debug(s"---------  yield $assign")
      assign
    }
    val res = Await.result(s, 60.seconds)
    log(s" =>> ${res.foreach(a => logger.debug(s"$a"))}")
    //  val assigns = assignRepo.findAssingmentsByRuleCode("BA-DelNotif-FDA05-to-FDA08")
  }

//  assigns onComplete {
//    case Success(v) => logger.debug(s"---------  done ${v.original}")
//    case Failure(m) => logger.debug(s" ------- error ${m.getMessage}")
//  }

//  val res = Await.result( assigns, 60.seconds)
//  logger.debug(s"........... finish! ${res.original}")

//  testDtnCopyUpdate("regle_match_mt499")
  testAssignmentUpdate("regle_match_mt499")
//  testSearch("regle_match_mt499")
}