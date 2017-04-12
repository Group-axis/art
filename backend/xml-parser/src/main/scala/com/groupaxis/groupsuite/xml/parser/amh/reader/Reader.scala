package com.groupaxis.groupsuite.xml.parser.amh.reader

import java.util.Calendar

import com.groupaxis.groupsuite.routing.write.domain.model.amh.assignment.{AMHAssignmentES, _}
import com.groupaxis.groupsuite.routing.write.domain.model.amh.backend.BackendDAO
import com.groupaxis.groupsuite.routing.write.domain.model.amh.distribution.copy._
import com.groupaxis.groupsuite.routing.write.domain.model.amh.feedback.distribution.copy._
import com.groupaxis.groupsuite.routing.write.domain.model.amh.rule.{AMHRuleDAO, AMHRuleEntity}
import com.groupaxis.groupsuite.xml.parser.amh.writer.es._
import com.groupaxis.groupsuite.xml.parser.amh.writer.jdbc.{JdbcAMHDistributionCpyBackendRepository, _}
import com.sksamuel.elastic4s.ElasticDsl.search
import com.sksamuel.elastic4s.RichSearchResponse
import com.sksamuel.elastic4s.analyzers.StandardAnalyzer
import com.sksamuel.elastic4s.jackson.ElasticJackson
import org.apache.logging.log4j.scala.Logging

import scala.xml.Elem
//import com.typesafe.slick.driver.oracle.OracleDriver
import org.elasticsearch.search.SearchHit

import scala.collection.mutable.ListBuffer
//import com.groupaxis.groupsuite.routing.write.domain.model.routing.point.RoutingKeyword
//import com.groupaxis.groupsuite.xml.parser.writer.es.RoutingRepository
import com.groupaxis.groupsuite.commons.persistence.jdbc.DatabaseService
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import org.elasticsearch.action.delete.DeleteResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._
import scala.util.{Failure, Success}
//import com.groupaxis.groupsuite.xml.parser.writer.jdbc.JdbcRoutingRepository

object test extends App with Logging {
  val ns = <foo><bar><baz/>Textooo</bar><bin/></foo>
  logger.debug("zazaza " + (ns \\ "foo" \ "bar").text)
  logger.debug("zazaza " + toOption((ns \\ "foo" \ "ba").text))
  def toOption(text: String) = {
    text match {
      case s: String if s.isEmpty => None
      case a: String                => Some(a)
    }
  }
}


object esHelper extends Logging {

  def client() = {
    import org.elasticsearch.common.settings.Settings
    val settings = Settings.settingsBuilder().put("cluster.name", "groupsuite").build()
    val machine = "127.0.0.1"
    val port = 9300
    val client = ElasticClient.transport(settings, ElasticsearchClientUri(s"elasticsearch://$machine:$port"))
    client
  }

  def remove(client: ElasticClient, indexName: String) {
    val res = client.execute { deleteIndex(indexName) }
    res.onComplete {
      case Success(t) => logger.debug(s" cool success delete $t")
      case Failure(t) => logger.debug(s" too bad deletion:/ $t")
    }
  }

  def create(client: ElasticClient, indexName: String) {
    val res = client.execute { createIndex("routing") }
    res.onComplete {
      case Success(t) => logger.debug(s" cool success created $t")
      case Failure(t) => logger.debug(s" too bad creation:/ $t")
    }
  }

  def init(indexName: String) {
    val ec = client()
    remove(ec, indexName)
    create(ec, indexName)
  }
}
object esMappingTest extends App {

  val esClient = esHelper.client()

  /*
  "code": { "type" : "completion",
            "analyzer" : "simple",
            "search_analyzer" : "simple",
            "payloads" : true
          },
          "expression": {
            "type": "string"
          }
  * */

//  esClient.execute {
//    create index "amhrouting2" mappings (
//      "rules" fields (
//        "code" typed CompletionType store true, //analyzer SimpleAnalyzer searchAnalyzer SimpleAnalyzer
//        "expression" typed StringType
//        )
//      )
//  }
}

object esRemoveTest extends App with Logging {
  val client = esHelper.client()

  deleteByIds(List("AVTjz2fxhEU0bZA_9_CS", "AVTjzsE6hEU0bZA_9_CR")).onComplete {
    case Success(s) => logger.debug(s" success $s")
    case Failure(t) => logger.debug(s"An error has occured: $t")
  }

  def deleteById(id: String): Future[DeleteResponse] = {
    client.execute {
      delete id id from "routing/points"
    }
  }

  def deleteByIds(ids: Seq[String]): Future[Seq[DeleteResponse]] = {
    var responses: List[Future[DeleteResponse]] = List();
    ids.foreach(id => { responses.+:(deleteById(id)) })
    Future.sequence(responses)
  }
}

object RemoveFromTable extends App with Logging {
  val dataBase = new DatabaseService(slick.driver.PostgresDriver, "jdbc:postgresql://localhost:5432/postgres", "postgres", "postgres")


  logger.debug(" -------------    BACKEND ------------")
  //  t.map(toAMHBackend).foreach(logger.debug)
  val backendDao : BackendDAO = BackendDAO(slick.driver.PostgresDriver)
  val jdbcBackendRepo = new JdbcAMHBackendRepository(backendDao, dataBase)
//  val resp = jdbcBackendRepo.createBackends(t.map(toAMHBackend))
  jdbcBackendRepo.findAllBackends.map( backends => backends.foreach(a=>logger.debug(s"$a")))
  jdbcBackendRepo.deleteAll()
  jdbcBackendRepo.findAllBackends.map( backends => backends.foreach(a=>logger.debug(s"$a")))
}

object TestMultiRead extends App with Logging  {
  import AMHParser._
  val t: scala.collection.mutable.HashMap[String,ListBuffer[Elem]] = unZipAll("C:/demo/BNP_20160513_STP.zip",List("AMHWizard.BackendConfiguration","Gateway.BackendChannelAssignmentSelectionTable"
  ,"Gateway.DistributionCopySelectionTable", "Gateway.FeedbackDistributionCopySelectionTable", "Gateway.RuleCriteria"))
  logger.debug(t)
}

object CleaningDB extends App with Logging {

  // val dataBase = new DatabaseService(OracleDriver, "jdbc:oracle:thin:@78.215.201.21:1521:FAFWDEV01", "GRPDBA", "GRPDBA")
    val dataBase = new DatabaseService(slick.driver.PostgresDriver, "jdbc:postgresql://localhost:5432/postgres", "postgres", "postgres")
//  val dataBase = new DatabaseService(slick.driver.PostgresDriver, "jdbc:postgresql://62.210.222.221:5432/rpl", "postgres", "2Ypseatu")

  val ruleDao: AMHRuleDAO = AMHRuleDAO
  val backendDao: BackendDAO = BackendDAO(slick.driver.PostgresDriver)
  val assignmentDao: AssignmentDAO = AssignmentDAO(slick.driver.PostgresDriver)
  val assignmentRuleDao: AssignmentRuleDAO = AssignmentRuleDAO(slick.driver.PostgresDriver)
  val feedDtnCpyDao: FeedbackDtnCpyDAO = FeedbackDtnCpyDAO(slick.driver.PostgresDriver)
  val feedDtnCpyRuleDao: FeedbackDtnCpyRuleDAO = FeedbackDtnCpyRuleDAO(slick.driver.PostgresDriver)
  val feedDtnCpyBackendDao: FeedbackDtnCpyBackDAO = FeedbackDtnCpyBackDAO(slick.driver.PostgresDriver)
  val distributionCpyDao: DistributionCpyDAO = DistributionCpyDAO(slick.driver.PostgresDriver)
  val distributionCpyRuleDao: DistributionCpyRuleDAO = DistributionCpyRuleDAO(slick.driver.PostgresDriver)
  val distributionCpyBackendDao: DistributionCpyBackendDAO = DistributionCpyBackendDAO(slick.driver.PostgresDriver)

  val jdbcRuleRepo = new JdbcAMHRuleRepository(ruleDao, dataBase)
  val jdbcBackendRepo = new JdbcAMHBackendRepository(backendDao, dataBase)
  val jdbcAssigmentRepo = new JdbcAMHAssignmentRepository(assignmentDao, dataBase)
  val jdbcAssigmentRuleRepo = new JdbcAMHAssignmentRuleRepository(assignmentRuleDao, dataBase)
  val jdbcFeedbackDtnCpyRepo = new JdbcAMHFeedbackDtnCpyRepository(feedDtnCpyDao, dataBase)
  val jdbcFeedbackDtnCpyRuleRepo = new JdbcAMHFeedbackDtnCpyRuleRepository(feedDtnCpyRuleDao, dataBase)
  val jdbcFeedbackDtnCpyBackendRepo = new JdbcAMHFeedbackDtnCpyBackendRepository(feedDtnCpyBackendDao, dataBase)
  val jdbcDistributionCpyRepo = new JdbcAMHDistributionCpyRepository(distributionCpyDao, dataBase)
  val jdbcDistributionCpyRuleRepo = new JdbcAMHDistributionCpyRuleRepository(distributionCpyRuleDao, dataBase)
  val jdbcDistributionCpyBackendRepo = new JdbcAMHDistributionCpyBackendRepository(distributionCpyBackendDao, dataBase)

  jdbcFeedbackDtnCpyRuleRepo.deleteAll()
  jdbcFeedbackDtnCpyBackendRepo.deleteAll()
  jdbcFeedbackDtnCpyRepo.deleteAll()
  jdbcDistributionCpyRuleRepo.deleteAll()
  jdbcDistributionCpyBackendRepo.deleteAll()
  jdbcDistributionCpyRepo.deleteAll()
  jdbcAssigmentRuleRepo.deleteAll()
  jdbcAssigmentRepo.deleteAll()
  jdbcBackendRepo.deleteAll()
  jdbcRuleRepo.deleteAll()
}

object Testing extends App with Logging {
  import AMHParser._
 // val dataBase = new DatabaseService(OracleDriver, "jdbc:oracle:thin:@78.215.201.21:1521:FAFWDEV01", "GRPDBA", "GRPDBA")
  val dataBase = new DatabaseService(slick.driver.PostgresDriver, "jdbc:postgresql://localhost:5432/postgres", "postgres", "postgres")
//  val dataBase = new DatabaseService(slick.driver.PostgresDriver, "jdbc:postgresql://62.210.222.221:5432/rpl", "postgres", "2Ypseatu")
  val start = Calendar.getInstance.getTime.getTime

  val ruleDao : AMHRuleDAO = AMHRuleDAO
  val backendDao : BackendDAO = BackendDAO(slick.driver.PostgresDriver)
  val assignmentDao : AssignmentDAO = AssignmentDAO(slick.driver.PostgresDriver)
  val assignmentRuleDao : AssignmentRuleDAO = AssignmentRuleDAO(slick.driver.PostgresDriver)
  val feedDtnCpyDao : FeedbackDtnCpyDAO = FeedbackDtnCpyDAO(slick.driver.PostgresDriver)
  val feedDtnCpyRuleDao : FeedbackDtnCpyRuleDAO = FeedbackDtnCpyRuleDAO(slick.driver.PostgresDriver)
  val feedDtnCpyBackendDao : FeedbackDtnCpyBackDAO = FeedbackDtnCpyBackDAO(slick.driver.PostgresDriver)
  val distributionCpyDao : DistributionCpyDAO = DistributionCpyDAO(slick.driver.PostgresDriver)
  val distributionCpyRuleDao : DistributionCpyRuleDAO = DistributionCpyRuleDAO(slick.driver.PostgresDriver)
  val distributionCpyBackendDao : DistributionCpyBackendDAO = DistributionCpyBackendDAO(slick.driver.PostgresDriver)

  val jdbcRuleRepo = new JdbcAMHRuleRepository(ruleDao, dataBase)
  val jdbcBackendRepo = new JdbcAMHBackendRepository(backendDao, dataBase)
  val jdbcAssigmentRepo = new JdbcAMHAssignmentRepository(assignmentDao, dataBase)
  val jdbcAssigmentRuleRepo = new JdbcAMHAssignmentRuleRepository(assignmentRuleDao, dataBase)
  val jdbcFeedbackDtnCpyRepo = new JdbcAMHFeedbackDtnCpyRepository(feedDtnCpyDao, dataBase)
  val jdbcFeedbackDtnCpyRuleRepo = new JdbcAMHFeedbackDtnCpyRuleRepository(feedDtnCpyRuleDao, dataBase)
  val jdbcFeedbackDtnCpyBackendRepo = new JdbcAMHFeedbackDtnCpyBackendRepository(feedDtnCpyBackendDao, dataBase)
  val jdbcDistributionCpyRepo = new JdbcAMHDistributionCpyRepository(distributionCpyDao, dataBase)
  val jdbcDistributionCpyRuleRepo = new JdbcAMHDistributionCpyRuleRepository(distributionCpyRuleDao, dataBase)
  val jdbcDistributionCpyBackendRepo = new JdbcAMHDistributionCpyBackendRepository(distributionCpyBackendDao, dataBase)

  val client = esHelper.client()

  //  val t: Seq[Elem] = unZip("C:/dev/DFS/AMH/BNP_20160513_STP.zip","AMHWizard.BackendConfiguration")
//  val t: Seq[Elem] = unZip("C:/dev/DFS/AMH/RoutageDemo.zip","AMHWizard.BackendConfiguration")
  val t: Seq[Elem] = unZip("C:/amhImports/_analysis.zip","AMHWizard.BackendConfiguration")
  logger.debug(" -------------   " + t.size + " BACKEND ------------")
//  t.map(toAMHBackend).foreach(logger.debug)

  jdbcFeedbackDtnCpyRuleRepo.deleteAll()
  jdbcFeedbackDtnCpyBackendRepo.deleteAll()
  jdbcFeedbackDtnCpyRepo.deleteAll()
  jdbcDistributionCpyRuleRepo.deleteAll()
  jdbcDistributionCpyBackendRepo.deleteAll()
  jdbcDistributionCpyRepo.deleteAll()
  jdbcAssigmentRuleRepo.deleteAll()
  jdbcAssigmentRepo.deleteAll()
  jdbcBackendRepo.deleteAll()
  jdbcRuleRepo.deleteAll()


  val env = "UNKNOWN"
  val version = "DEFAULT"
  val userId = "system"

  val d: Seq[Elem] = unZip("C:/amhImports/_analysis.zip","Gateway.RuleCriteria")

  logger.debug(" -------------   " + d.size + " RULES ------------")
  //d.map(toAMHRule).foreach(logger.debug)
  val toRule = toAMHRule(env,version, userId)_
  val ruleResp = jdbcRuleRepo.createRules(d.map(toRule))

  ruleResp.fold(
    ex => {
      logger.debug("Operation failed with " + ex)
    },
    v => {
      logger.debug("Operation produced value: " + v)
      val esRuleRepo : ESAMHRuleRepository = new  ESAMHRuleRepository(client)
      import com.groupaxis.groupsuite.xml.parser.amh.writer.es._
      initializeIndex("amhrouting", client)
      esRuleRepo.insert(v.rules.map(t => t.toES(Some(false))))
    }
  )
  val f: Future[String] = Future {
    Thread.sleep(1000)
    "future value"
  }
  logger.debug("waiting 1 seconds...................................")
  Await.result(f, 5.seconds)

  val toBackend = toAMHBackend(env,version)_
  val resp = jdbcBackendRepo.createBackends(t.map(toBackend))
  resp.fold(
    ex => {
      logger.debug("ERROR - Operation failed with " + ex)
    },
    v => {
      logger.debug("Operation produced value: " + v)
      val esBackendRepo : ESAMHBackendRepository = new  ESAMHBackendRepository(client)
      esBackendRepo.insert(v.rules.map(t => t.toES))
    }
  )

//  val d: Seq[Elem] = unZip("C:/dev/DFS/AMH/BNP_20160513_STP.zip","Gateway.RuleCriteria")


  // delete from "BCKENDCHASSNSELTABLES";  delete from "GATEWAY_RULECRITERIAS";  delete from "BACKENDCONFIGURATIONS"; delete from "BCKENDCHASSGNRULECRITERIAS";
//  val a: Seq[Elem] = unZip("C:/dev/DFS/AMH/BNP_20160513_STP.zip","Gateway.BackendChannelAssignmentSelectionTable")
  val a: Seq[Elem] = unZip("C:/amhImports/_analysis.zip","Gateway.BackendChannelAssignmentSelectionTable")


  logger.debug(" -------------   " + a.size + " ASSIGNMENTS ------------")
  //  a.map(toAMHAssignment).foreach(logger.debug)

  def getRuleExpression(rules : Seq[AMHRuleEntity])(code : String) : String = {
    logger.debug("looking for code "+ code + " in " +rules)
    val filtered = rules.filter(_.code.equalsIgnoreCase(code))
    if (filtered.nonEmpty) {
      logger.debug("found !! "+ filtered.head.expression.getOrElse("") + " |")
      filtered.head.expression.getOrElse("")
    }else {
      logger.debug("NOT FOUND :<")
      ""
      }
  }

  val AMHRules = ruleResp.right.get.rules
  logger.debug(" RULES :")
  logger.debug(AMHRules)
  val pp = getRuleExpression(AMHRules)_
  val toAssignment = toAMHAssignment(env,version, "system")_
  val tuples : Seq[(AMHAssignmentEntity, Seq[AMHAssignmentRuleEntity])] = a.map(toAssignment)
  val assignResp = jdbcAssigmentRepo.createAssignments(tuples.map(_._1))
  tuples.map(_._2).filter(_.nonEmpty).map(_.size).foreach(a => logger.debug(s"$a"))
  val tot : Seq[AMHAssignmentRuleEntity] = tuples.filter(_._2.nonEmpty).flatMap(_._2)

  val assignRuleResp = jdbcAssigmentRuleRepo.createAssignmentRules(tot)

  assignRuleResp.fold(
    ex => {
      logger.debug("Operation failed with " + ex)
    },
      v => {
        logger.debug("Operation produced value: " + v)
        val esAssignmentRepo : ESAMHAssignmentRepository = new  ESAMHAssignmentRepository(client)
        val esRuleRepo : ESAMHRuleRepository = new  ESAMHRuleRepository(client)
        esAssignmentRepo.insert(tuples.map(t => { t._1.toES(t._2.map(r => r.toES(esRuleRepo.findExpressionByCode(r.ruleCode))))}))
      }
  )

  /*
  val file = xml.XML.load("C:/dev/DFS/AMH/BNP_20160513_STP/_analysis/AMHWizard.BackendConfiguration/export_AEPP6-DISTRIBUTION.xml")
  logger.debug("file read " + toAMHBackend(file))
  logger.debug("file read ES " + toAMHBackend(file).toES)

  val file1 = xml.XML.load("C:/dev/DFS/AMH/BNP_20160513_STP/_analysis/Gateway.RuleCriteria/export_BA-BNABFRPP-FUNDS-HRAR8.xml")
  logger.debug("-------------------- RULES ---------")
  logger.debug("file read " + toAMHRule(file1))
  logger.debug("file read  ES " + toAMHRule(file1).toES)

  val file2 = xml.XML.load("C:/dev/DFS/AMH/BNP_20160513_STP/_analysis/Gateway.BackendChannelAssignmentSelectionTable/export_AEPP6.xml")
  logger.debug("-------------------- ASSIGMENTS  ---------")
  logger.debug("file read " + toAMHAssignment(file2))
  val code = (file2 \ "Code").text
  val rules = toAMHAssignmentRules(code, file2 \\ "BackendChannelAssignmentRuleBackendChannelAssignmentRuleCriteria" \ "BackendChannelAssignmentRuleCriteria")
  val rulesES = rules.map(_.toES(""))
  logger.debug("read  rules " + rules)
  logger.debug("file ES " + toAMHAssignment(file2).toES(rulesES))
*/

}

object TestingNew extends App with Logging {
  import AMHParser._
  val dataBase = new DatabaseService(slick.driver.PostgresDriver, "jdbc:postgresql://localhost:5432/postgres", "postgres", "postgres")
  val start = Calendar.getInstance.getTime.getTime

  val ruleDao : AMHRuleDAO = AMHRuleDAO
  val feedDtnCpyDao : FeedbackDtnCpyDAO = FeedbackDtnCpyDAO(slick.driver.PostgresDriver)
  val feedDtnCpyRuleDao : FeedbackDtnCpyRuleDAO = FeedbackDtnCpyRuleDAO(slick.driver.PostgresDriver)
  val feedDtnCpyBackendDao : FeedbackDtnCpyBackDAO = FeedbackDtnCpyBackDAO(slick.driver.PostgresDriver)
  val distributionCpyDao : DistributionCpyDAO = DistributionCpyDAO(slick.driver.PostgresDriver)
  val distributionCpyRuleDao : DistributionCpyRuleDAO = DistributionCpyRuleDAO(slick.driver.PostgresDriver)
  val distributionCpyBackendDao : DistributionCpyBackendDAO = DistributionCpyBackendDAO(slick.driver.PostgresDriver)

  val jdbcRuleRepo = new JdbcAMHRuleRepository(ruleDao, dataBase)
  val jdbcFeedbackDtnCpyRepo = new JdbcAMHFeedbackDtnCpyRepository(feedDtnCpyDao, dataBase)
  val jdbcFeedbackDtnCpyRuleRepo = new JdbcAMHFeedbackDtnCpyRuleRepository(feedDtnCpyRuleDao, dataBase)
  val jdbcFeedbackDtnCpyBackendRepo = new JdbcAMHFeedbackDtnCpyBackendRepository(feedDtnCpyBackendDao, dataBase)
  val jdbcDistributionCpyRepo = new JdbcAMHDistributionCpyRepository(distributionCpyDao, dataBase)
  val jdbcDistributionCpyRuleRepo = new JdbcAMHDistributionCpyRuleRepository(distributionCpyRuleDao, dataBase)
  val jdbcDistributionCpyBackendRepo = new JdbcAMHDistributionCpyBackendRepository(distributionCpyBackendDao, dataBase)

  val client = esHelper.client()

  jdbcFeedbackDtnCpyRuleRepo.deleteAll()
  jdbcFeedbackDtnCpyBackendRepo.deleteAll()
  jdbcFeedbackDtnCpyRepo.deleteAll()
  jdbcDistributionCpyRuleRepo.deleteAll()
  jdbcDistributionCpyBackendRepo.deleteAll()
  jdbcDistributionCpyRepo.deleteAll()
  val env = "UNKNOWN"
  val version = "DEFAULT"

  def getRuleExpression(rules : Seq[AMHRuleEntity])(code : String) : String = {
    logger.debug("looking for code "+ code + " in " +rules)
    val filtered = rules.filter(_.code.equalsIgnoreCase(code))
    if (filtered.nonEmpty) {
      logger.debug("found !! "+ filtered.head.expression.getOrElse("") + " |")
      filtered.head.expression.getOrElse("")
    }else {
      logger.debug("NOT FOUND :<")
      ""
    }
  }

  val amhRules = jdbcRuleRepo.findAllNonDeletedRules.get
  //  a.map(toAMHAssignment).foreach(logger.debug)

  val pp = getRuleExpression(amhRules)_
  val a: Seq[Elem] = unZip("C:/demo/BNP_20160513_STP.zip","Gateway.FeedbackDistributionCopySelectionTable")


  logger.debug(" -------------   " + a.size + " feedback------------")
  val ff = toAMHFeedbackDtnCpy(env, version, "System")_
  val tuples : Seq[(AMHFeedbackDistributionCpyEntity, Seq[AMHFeedbackDistributionCpyRuleEntity], Seq[AMHFeedbackDistributionCpyBackendEntity])] = a.map(ff)
  logger.debug(" FeedbackDtnCopies :")
  tuples.foreach(d => logger.debug("==> "+d._1))
  val feedbackResp = jdbcFeedbackDtnCpyRepo.createFeedbackDtnCps(tuples.map(_._1))
  logger.debug(" FeedbackDtnCopies AFTER : " + distributionResp)
  tuples.map(_._2).filter(_.nonEmpty).map(_.size).foreach(a => logger.debug(s"$a"))
  val rules : Seq[AMHFeedbackDistributionCpyRuleEntity] = tuples.filter(_._2.nonEmpty).flatMap(_._2)
  val backends : Seq[AMHFeedbackDistributionCpyBackendEntity] = tuples.filter(_._3.nonEmpty).flatMap(_._3)
  val feedbackRuleResp = jdbcFeedbackDtnCpyRuleRepo.createFeedbackDtnCpyRules(rules)
  val feedbackBackendResp = jdbcFeedbackDtnCpyBackendRepo.createFeedbackDtnCpyBackends(backends)

  feedbackRuleResp.fold(
  ex => {
    logger.debug("Operation failed with " + ex)
  },
  v => {
    logger.debug("Operation produced value: " + v)
    val esFeedbackDtnCpyRepo : ESAMHFeedbackDtnCpyRepository = new  ESAMHFeedbackDtnCpyRepository(client)
    esFeedbackDtnCpyRepo.insert(tuples.map(t => { t._1.toES(t._3.map(b => b.toES), t._2.map(r => r.toES(pp(r.ruleCode))))}))
  }
  )
  val b: Seq[Elem] = unZip("C:/demo/BNP_20160513_STP.zip","Gateway.DistributionCopySelectionTable")
  logger.debug(" -------------   " + b.size + " distribution ------------")
  val gg = toAMHDistributionCpy(env, version, "testing_system")_
  val distTuples : Seq[(AMHDistributionCpyEntity, Seq[AMHDistributionCpyRuleEntity], Seq[AMHDistributionCpyBackendEntity])] = b.map(gg)
  logger.debug(" FeedbackDtnCopies :")
  distTuples.foreach(d => logger.debug("==> "+d._1))
  val distributionResp = jdbcDistributionCpyRepo.createDistributionCps(distTuples.map(_._1))
  logger.debug(" FeedbackDtnCopies AFTER : " + distributionResp)
  distTuples.map(_._2).filter(_.nonEmpty).map(_.size).foreach(a => logger.debug(s"$a"))
  val distRules : Seq[AMHDistributionCpyRuleEntity] = distTuples.filter(_._2.nonEmpty).flatMap(_._2)
  val distBackends : Seq[AMHDistributionCpyBackendEntity] = distTuples.filter(_._3.nonEmpty).flatMap(_._3)
  val distributionRuleResp = jdbcDistributionCpyRuleRepo.createDistributionCpyRules(distRules)
  val distributionBackendResp = jdbcDistributionCpyBackendRepo.createDistributionCpyBackends(distBackends)

  distributionRuleResp.fold(
    ex => {
      logger.debug("Operation failed with " + ex)
    },
    v => {
      logger.debug("Operation produced value: " + v)
      val esFeedbackDtnCpyRepo : ESAMHDistributionCpyRepository = new  ESAMHDistributionCpyRepository(client)
      esFeedbackDtnCpyRepo.insert(distTuples .map(t => { t._1.toES(t._3.map(_.toES), t._2.map(r => r.toES(pp(r.ruleCode))))}))
    }
  )

}

object Init extends App with Logging {
  esHelper.init("amhrouting")
}

object UpdateAssignmentRules extends App with Logging {

  val client = esHelper.client()

  def updateAssignmentsByRuleCode(ruleCode: String, ruleExpression : String): Int = {
    import ElasticJackson.Implicits._

    val res = client.execute {
      search in "amhrouting/assignments" size 50
    }

    logger.debug(" Avant onComplete")
    val r : RichSearchResponse = Await.result(res, 10.seconds)

    val assignments :Seq[AMHAssignmentES]= r.as[AMHAssignmentES]

    val assignmentFiltered = assignments.filter( assign => {
      val ruleCodeFound = assign.rules.filter(rule => {
                    val isEq = rule.code equals ruleCode
                    if(isEq) {
                      rule.copy(expression=ruleExpression)

                    }
                    isEq
                  })
      ruleCodeFound.nonEmpty
    })

    val updated = assignmentFiltered.map(assign => {
        assign.copy(rules = assign.rules.map(rule => {
            if(rule.code equals ruleCode) {
               rule.copy(expression=ruleExpression)
            } else {
              rule
            }
        }))
    })

    assignments.foreach(uno => {
      logger.debug("filteredRules " + uno)
    })
    logger.debug(" ------------  FILTERED ---------")
    assignmentFiltered.foreach(uno => {
      logger.debug("filteredRules " + uno)
    })
    logger.debug(" ------------  updated ---------")
    updated.foreach(uno => {
      logger.debug("filteredRules " + uno)
    })
    45
  }
  val updateAssignmentByRule = updateAssignmentsByRuleCode("GROUFRPP-SECURITIES-MX-semt", "NEW expression value")
  logger.debug(s" updateAssignmentByRule $updateAssignmentByRule")

}

object Finder extends App with Logging {

  val client = esHelper.client()

  def findRuleIdByCode(ruleCode: String): String = {
    var id : String = ""
    //    import ElasticJackson.Implicits._

    val res = client.execute {
      search in "amhrouting/rules" sourceInclude("code", "expression")
    }

    logger.debug(" Avant onComplete")
    val r : RichSearchResponse = Await.result(res, 10 seconds)

        val found : Option[SearchHit]= r.getHits.getHits.find(searchHit => {
          searchHit.getSource().get("code") equals (ruleCode)
        }
        )

        logger.debug(s" success $found")
        id = found.map(hit => hit.getId).getOrElse("NOT_FOUND")

    logger.debug(" Apres onComplete")
    id
  }

  logger.debug(findRuleIdByCode("GROUFRPP-PRT01-940"))

}

object Search extends App with Logging {
  val client = esHelper.client()
  val start = Calendar.getInstance.getTime.getTime
  val res = client.execute {
    search in "amhrouting/rules"  sourceInclude ("code","expression") query { matchQuery("code", "BNP_Autocancel_Distribution_TOBEX") analyzer StandardAnalyzer operator("AND")}
  }
  //search in "routing" / "points" postFilter regexQuery("pointName",".*Sib.*") //{ wildcardQuery("pointName","Sib*")  }
//  val res = client.execute {
//    search in "routing" / "points" rawQuery {
//      """{  "query":  {
//              "filtered": {
//                "query": {
//                    "match_all": {}
//                 },
//                 "filter": {
//                   "prefix": {
//                      "routingPointName": "Sib"
//                   }
//                 }
//                 }}}"""
//    } scroll "3m"
//  }
  //  val res = client.execute { search in "routing" / "points" from 0 size 50 scroll "25m"}  
  //    val res = client.execute {
  //    search scroll "cXVlcnlUaGVuRmV0Y2g7NTs1MzI6NU9Gdm1JaEVSVDJrTFl4RzBZSWpBUTs1MzE6a2Y1TUQ1amlTNVMzbE9qSmptWVhmUTs1MzI6a2Y1TUQ1amlTNVMzbE9qSmptWVhmUTs1MzM6NU9Gdm1JaEVSVDJrTFl4RzBZSWpBUTs1MzQ6NU9Gdm1JaEVSVDJrTFl4RzBZSWpBUTswOw==" keepAlive "25m"
  //  }

  //  res.onComplete { 
  //    case Success(x) => logger.debug ("sucess!! $x")
  //    case Failure(e) => logger.debug (s":O/ $e")
  //    case _ => logger.debug("naranjas")
  //    }
  //  
  //   res.onFailure{
  //     case e => logger.debug(e)
  //   }

  val response = Await.result(res, 30 seconds)
  logger.debug(response.original)
  logger.debug("---------------------")
  logger.debug(response.hits.length + " " + response.hits) //.head.sourceAsMap.get("expression")
  //val vv: Array[PointES] = response.as
//  logger.debug(" RESPONSE points : " + vv.length)
//  vv.foreach(logger.debug)
  logger.debug(s"stop on " + (Calendar.getInstance.getTime.getTime - start))
}

//object Reader extends App {
//  val start = Calendar.getInstance.getTime.getTime
////  val file = xml.XML.load("file:///c:/dev/DFS/AMH/saarouting_20160211T060001.xml")
//  val file = xml.XML.load("C:/dev/DFS/AMH/BNP_20160513_STP/_analysis/AMHWizard.BackendConfiguration/export_AEPP6-DISTRIBUTION.xml")
//  val esRouting = new RoutingRepository("127.0.0.1", 9300)
//
//  val dataBase = new DatabaseService("jdbc:oracle:thin:@78.215.201.21:1521:FAFWDEV01", "GRPDBA", "GRPDBA")
//  val jdbcRepo = new JdbcRoutingRepository(dataBase)
////  val points = getRoutingPoints(file \\ "RoutingRuleData" \ "RoutingPointRules")
////  val denormalizedPoints = points.map { point => point.toES }
////  val denormalizedPoints = points.flatMap ( point => point.rules).map( rule => rule.condition.criteria.getOrElse("")).map(value => (value.length(), value))
////  denormalizedPoints.foreach(logger.debug)
////      <Code>AEMM6</Code>
////      <Direction
//      //"BackendChannel" \ "primarykey"
//  getRoutingKeywords(file \\ "" ).foreach(logger.debug)
////  logger.debug("TOTAL OF POINTS READ: " + points.length)
//  logger.debug("Time before ES indexation:" + (Calendar.getInstance.getTime.getTime - start))
//  //esRouting.insert(denormalizedPoints)
//  logger.debug("AFTER INSERTING IN ES" + (Calendar.getInstance.getTime.getTime - start))
////  val resp = jdbcRepo.createPoints( points)
////  logger.debug(resp.fold(
////  ex => "Operation failed with " + ex,
////  v => "Operation produced value: " + v
////))
//  logger.debug("AFTER INSERTING IN DB" + (Calendar.getInstance.getTime.getTime - start))
//  logger.debug("Final time: " + (Calendar.getInstance.getTime.getTime - start))
//  /**
//   * Required from ES ****
//   * List of routing points
//   * List of functions
//   * List of Action. How to create the list ???
//   * List of Action duo combo. How to create the list ???
//   * List of instanceTargetQueue. How to create the list ???
//   * List of Units ??
//   * List of Priority ???
//   *
//   *
//   */
//  /*************************** Routing Keywords ********************/
//  /* LINE 3016
//   <RoutingKeyword xmlns="urn:swift:saa:xsd:routingKeyword" xmlns:ns2="urn:swift:saa:xsd:syntaxVersion">
//    <Identifier>
//        <Name>Field11S</Name>
//    </Identifier>
//    <Description>Field_11S Original message type, Date, Session, ISN</Description>
//    <Type>RUKY_STRING</Type>
//</RoutingKeyword>
//   */
//
//
//
//  def getRoutingKeywords(nodeKeywords: NodeSeq): Seq[RoutingKeyword] =
//    nodeKeywords.map(node => RoutingKeyword((node \ "Identifier" \ "Name").text, (node \ "Description").text, (node \ "Type").text))
//
//  /************************* ROUTING POINTS BEGIN ******************/
////  def getRoutingPoints(nodePoints: NodeSeq) = nodePoints.map(routing => toRoutingPoint(routing))
//
////  def toRoutingPoint(node: Node): Point2 = Point2((node \ "RoutingPointName").text, ((node \ "Full").text).toBoolean, toRules(node \ "Rule", (node \ "RoutingPointName").text, toOption((node \ "Full").text)))
//
//  //  case class RuleAction(
//  //  actionOn: Option[String] = None,
//  //  instanceAction: Option[String] = None,
//  //  instanceInterventionType: Option[String] = None,
//  //  instanceInterventionTypeText: Option[String] = None,
//  //  instanceRoutingCode: Option[String] = None,
//  //  instanceTargetQueue: Option[String] = None,
//  //  instanceUnit: Option[String] = None,
//  //  instancePriority: Option[String] = None,
//  //  newInstanceAction: Option[String] = None,
//  //  newInstanceRoutingCode: Option[String] = None,
//  //  newInstanceInterventionType: Option[String] = None,
//  //  newInstanceInterventionTypeText: Option[String] = None,
//  //  newInstanceTargetQueue: Option[String] = None,
//  //  newInstanceType: Option[String] = None,
//  //  newInstanceUnit: Option[String] = None,
//  //  newInstancePriority: Option[String] = None)
//  //
//  //case class RuleCondition(
//  //  conditionOn: Option[String] = None,
//  //  criteria: Option[String] = None,
//  //  functionList: Option[String] = None)
//  //
//
////  def toRule(node: Node, pointName: String, full: Option[String]): RuleEntity =
////    RuleEntity(
////      (node \ "SequenceNumber").text.toLong,
////      pointName,
////      full,
////      toOption((node \ "Description" \ "RuleDescription").text),
////      toOption((node \ "Description" \ "SchemaMap").text),
////      RuleAction(
////        toOption((node \ "Action" \ "ActionOn").text),
////        toOption((node \ "Action" \ "SourceInstanceRule" \ "InstanceAction").text),
////        toOption((node \ "Action" \ "SourceInstanceRule" \ "InstanceInterventionType").text),
////        toOption((node \ "Action" \ "SourceInstanceRule" \ "InstanceInterventionTypeText").text),
////        toOption((node \ "Action" \ "SourceInstanceRule" \ "InstanceRoutingCode").text),
////        toOption((node \ "Action" \ "SourceInstanceRule" \ "InstanceTargetQueue" \ "Name").text),
////        toOption((node \ "Action" \ "SourceInstanceRule" \ "InstanceUnit").text),
////        toOption((node \ "Action" \ "SourceInstanceRule" \ "InstancePriority").text),
////        toOption((node \ "Action" \ "NewInstanceRule" \ "InstanceAction").text),
////        toOption((node \ "Action" \ "NewInstanceRule" \ "InstanceRoutingCode").text),
////        toOption((node \ "Action" \ "NewInstanceRule" \ "InstanceInterventionType").text),
////        toOption((node \ "Action" \ "NewInstanceRule" \ "InstanceInterventionTypeText").text),
////        toOption((node \ "Action" \ "NewInstanceRule" \ "InstanceTargetQueue" \ "Name").text),
////        toOption((node \ "Action" \ "NewInstanceType").text),
////        toOption((node \ "Action" \ "NewInstanceRule" \ "InstanceUnit").text),
////        toOption((node \ "Action" \ "NewInstanceRule" \ "InstancePriority").text)),
////      RuleCondition(
////        toOption((node \ "Condition" \ "ConditionOn").text),
////        toOption((node \ "Condition" \ "Criteria").text),
////        toOption((node \ "Condition" \ "FunctionResult").text) //create a SEQ from here!!!!
////        ))
////
////  def toRules(rules: NodeSeq, pointName: String, full: Option[String]): Seq[RuleEntity] = rules.map(rule => toRule(rule, pointName, full))
//  /**********************   miselanous ***************************/
//
//  /*
//   sequence: Long, routingPointName: String,
//   actionOn: Option[String] = None, conditionOn: Option[String] = None, criteria: Option[String] = None, full: Option[String] = Some("true"),
//   functionList: Option[String] = None,
//   instanceAction: Option[String] = None,
//    instanceInterventionType: Option[String] = None,
//    instanceRoutingCode: Option[String] = None,
//    instanceTargetQueue: Option[String] = None,
//    newInstanceAction: Option[String] = None,
//    newInstanceRoutingCode: Option[String] = None,
//    newInstanceInterventionType: Option[String] = None,
//    newInstanceTargetQueue: Option[String] = None,
//    newInstanceType: Option[String] = None,
//    ruleDescription: Option[String] = None, schemaMap: Option[String] = None
//
//   sequence: Long, routingPointName: String,
//   actionOn: Option[String], conditionOn: Option[String], criteria: Option[String] , full: Option[String] ,
//   functionList: Option[String] , instanceAction: Option[String] , instanceInterventionType: Option[String] , instanceRoutingCode: Option[String],
//   instanceTargetQueue: Option[String] , newInstanceAction: Option[String], newInstanceRoutingCode: Option[String], newInstanceInterventionType: Option[String],
//   newInstanceTargetQueue: Option[String], newInstanceType: Option[String], ruleDescription: Option[String], schemaMap: Option[String]
//
//   <RoutingPointName>Sib_MT_ALL_E</RoutingPointName>
//    <Full>true</Full>
//    <Rule>
//        <SequenceNumber>1</SequenceNumber>
//        <Description>
//            <ns2:RuleDescription>XXXIFRS_20120917_20150615_M</ns2:RuleDescription>
//            <ns2:SchemaMap></ns2:SchemaMap>
//        </Description>
//        <Condition>
//            <ns2:ConditionOn>MESSAGE</ns2:ConditionOn>
//            <ns2:Criteria>(Sender = 'PARBFRPP') and (SibesBranchCodeE = 'XXX') and (SibesFlux like '&amp;PS%') and (SibesService like '0243%,0246%,4678%,9881%')</ns2:Criteria>
//        </Condition>
//        EITHER
//        <Condition>
//            <ns2:ConditionOn>FUNCTION</ns2:ConditionOn>
//            <ns2:FunctionResult>Success</ns2:FunctionResult>
//        </Condition>
//        <Action>
//            <ns2:ActionOn>SOURCE</ns2:ActionOn>
//            <ns2:SourceInstanceRule>
//                <ns2:InstanceAction>ACTION_TYPE_COMPLETE</ns2:InstanceAction>
//                <ns2:InstanceInterventionType>INTV_NO_INTV</ns2:InstanceInterventionType>
//                <ns2:InstanceRoutingCode>IFR-S</ns2:InstanceRoutingCode>
//            </ns2:SourceInstanceRule>
//        </Action>
//        <Action>
//            <ns2:ActionOn>NEW_INSTANCE</ns2:ActionOn>
//            <ns2:NewInstanceType>INST_TYPE_COPY</ns2:NewInstanceType>
//            <ns2:NewInstanceRule>
//                <ns2:InstanceAction>ACTION_TYPE_ROUTING_POINT</ns2:InstanceAction>
//                <ns2:InstanceTargetQueue>
//                    <ns2:Name>SARMTR</ns2:Name>
//                </ns2:InstanceTargetQueue>
//                <ns2:InstanceInterventionType>INTV_NO_INTV</ns2:InstanceInterventionType>
//                <ns2:InstanceRoutingCode>LDTS2</ns2:InstanceRoutingCode>
//            </ns2:NewInstanceRule>
//        </Action>
//    </Rule>
//
//   * */
//  def toOption(text: String) : Option = {
//    text match {
//      case value: String if value.isEmpty() => None
//      case nonEmptyValue: String            => Some(nonEmptyValue)
//    }
//  }
//}