package com.groupaxis.groupsuite.xml.parser.reader

import java.util.Calendar

import akka.actor.ActorSystem
import akka.util.Timeout
import com.groupaxis.groupsuite.routing.write.domain.model.routing.rule.{RuleDAO, RuleEntity}
import com.groupaxis.groupsuite.routing.write.domain.model.routing.rule.RuleMessages._
import org.apache.logging.log4j.scala.Logging

import scala.xml.{Elem, NodeSeq}
import akka.pattern.ask
import com.groupaxis.groupsuite.datastore.jdbc.Database
import com.groupaxis.groupsuite.xml.parser.routing.infrastructure.util.XmlHelper
import com.groupaxis.groupsuite.xml.parser.writer.es.RoutingRepository
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import org.elasticsearch.action.delete.DeleteResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import com.groupaxis.groupsuite.routing.infrastructor.es.ESRuleWriteRepository
import com.groupaxis.groupsuite.routing.infrastructor.jdbc.JdbcRuleWriteRepository
import com.groupaxis.groupsuite.routing.write.domain.model.routing.keyword.KeywordEntity
import com.groupaxis.groupsuite.routing.write.domain.model.routing.point.{Point, PointES}
import com.groupaxis.groupsuite.routing.write.domain.model.routing.rule.{RuleAction, RuleCondition}

object test extends App with Logging {
  val ns = <foo><bar><baz/>Textooo</bar><bin/></foo>

  def toOption(text: String) = {
    text match {
      case s: String if s.isEmpty => None
      case a: String                => Some(a)
    }
  }
}

object esTest extends App {
  val esRouting = new RoutingRepository("127.0.0.1", 9300)
  val r : Point = Point("_MP_mod_text", full=true,
    Seq(
      RuleEntity(100,
        "_MP_mod_text",
        Some("true"),
        Some("SWIFT financial to Verificatio"),
        Some("CMWXY"),
        "",
        "",
        RuleAction(
          Some("SOURCE"),
          Some("ACTION_TYPE_ROUTING_POINT"),
          Some("INTV_NO_INTV"),
          None,
          None,
          Some("TAGET_ROUTING_POINT"),
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None),
        RuleCondition(
          Some("MESSAGE_AND_FUNCTION"),
          Some("(Format = 'Swift') and (Nature = Finance)"),
          Some("Success")) //create a SEQ from here!!!!
          )))

  esRouting.insert(r.toES)
}

object esHelper extends Logging {

  def client() = {
    import org.elasticsearch.common.settings.Settings
    val settings = Settings.settingsBuilder().put("cluster.name", "groupsuite").build()
    val client = ElasticClient.transport(settings, ElasticsearchClientUri("elasticsearch://127.0.0.1:9300"))
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
    val res = client.execute { createIndex(indexName) }
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

object esRemoveTest extends App with Logging {
  import org.elasticsearch.common.settings.Settings
  val settings = Settings.settingsBuilder().put("cluster.name", "groupsuite").build()
  val client = ElasticClient.transport(settings, ElasticsearchClientUri("elasticsearch://127.0.0.1:9300"))

  deleteByIds(List("AVTjz2fxhEU0bZA_9_CS", "AVTjzsE6hEU0bZA_9_CR")).onComplete {
    case Success(s) => logger.debug(s" success $s")
    case Failure(t) => logger.debug(s"An error has occurred: $t")
  }

  def deleteById(id: String): Future[DeleteResponse] = {
    client.execute {
      delete id id from "routing/points"
    }
  }

  def deleteByIds(ids: Seq[String]): Future[Seq[DeleteResponse]] = {
    val responses: List[Future[DeleteResponse]] = List()
    ids.foreach(id => { responses.+:(deleteById(id)) })
    Future.sequence(responses)
  }
}

object Init extends App with Logging {

  esHelper.init("amhrouting")
}

object Search extends App with Logging {
  val client = esHelper.client()
  val start = Calendar.getInstance.getTime.getTime

  val res = client.execute {
    search in "routing" -> "points" query {
      termQuery("_id", "Sib_MT_ALL")
    }}

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
  //    case Success(x) => logger.debug ("success!! $x")
  //    case Failure(e) => logger.debug (s":O/ $e")
  //    case _ => logger.debug("nothing")
  //    }
  //  
  //   res.onFailure{
  //     case e => logger.debug(e)
  //   }
  import com.sksamuel.elastic4s.jackson.ElasticJackson
  import ElasticJackson.Implicits._

  val response = Await.result(res, 30.seconds)
  logger.debug(response.original)
  val vv: Array[PointES] = response.as
  logger.debug(" RESPONSE points : " + vv.length)
  vv.foreach(a => logger.debug(s"$a"))
  logger.debug(s"stop on " + (Calendar.getInstance.getTime.getTime - start))
}



object Reader extends App with Logging {
  val system = ActorSystem("reader")
  implicit val timeout: Timeout = 15.seconds
  val start = Calendar.getInstance.getTime.getTime
  val file : Elem = xml.XML.load("file:///c:/dev/DFS/AMH/saarouting_20160211T060001.xml")

  val esRuleWriteRepository = system.actorOf(ESRuleWriteRepository.props(esHelper.client()), ESRuleWriteRepository.Name)

//  val esRouting = new RoutingRepository("127.0.0.1", 9300)

//  val dataBase = new DatabaseService(slick.driver.PostgresDriver, "jdbc:oracle:thin:@78.215.201.21:1521:FAFWDEV01", "GRPDBA", "GRPDBA")
  val database = new Database(slick.driver.PostgresDriver, "jdbc:postgresql://localhost:5432/postgres", "postgres", "postgres")
//  val ruleDao : RuleDAO = new RuleDAO(slick.driver.PostgresDriver)
  val ruleRepo : JdbcRuleWriteRepository = new JdbcRuleWriteRepository(database, 10.seconds)

//  val jdbcRepo = new JdbcRoutingRepository(dataBase)
  val environment = "UNKNOWN"; val version = "DEFAULT"
  val points : Seq[Point]= XmlHelper.getRoutingPoints(file \\ "RoutingRuleData" \ "RoutingPointRules")
  val filtered = points.flatMap(point => {
   if ( point.rules.exists(rule => rule.condition.functionList.nonEmpty) )
     point.rules
    else
     Seq()
  }
  )
  filtered.foreach(rule => logger.debug(rule.condition.functionList))
  val denormalizedPoints = points.map { point => point.toES }
//  val denormalizedPoints = points.flatMap ( point => point.rules).map( rule => rule.condition.criteria.getOrElse("")).map(value => (value.length(), value))

  denormalizedPoints.foreach(a => logger.debug(s"$a"))
  getRoutingKeywords("UNKNOWN","DEFAULT",file \\ "RoutingKeywordData" \ "RoutingKeyword").foreach(a=>logger.debug(s"$a"))
  logger.debug("TOTAL OF POINTS READ: " + points.length)
  logger.debug("Time before ES indexation:" + (Calendar.getInstance.getTime.getTime - start))
  //esRouting.insert(denormalizedPoints)
  logger.debug("AFTER INSERTING IN ES" + (Calendar.getInstance.getTime.getTime - start))
  val allRules = points.flatMap(p => p.rules)

  val resp = ruleRepo.createRules(allRules)
  resp.fold(
    ex => {
      logger.debug("Operation failed with " + ex)
      system.terminate()
    },
    v => {
      logger.debug("Operation produced value: " + v)
      val ff = (esRuleWriteRepository ? InsertPointsES(denormalizedPoints)).mapTo[Either[String, PointsESInserted]]
      val rES = Await.result(ff, 15.seconds)
      rES.fold(
        ee => logger.debug("ES Operation failed with " + ee),
        vv => logger.debug("ES Operation OK ")
      )
      system.terminate()
    }
    )

//  val resp = ruleRepo.createRules( points)

  logger.debug("AFTER INSERTING IN DB" + (Calendar.getInstance.getTime.getTime - start))
  logger.debug("Final time: " + (Calendar.getInstance.getTime.getTime - start))
  /**
   * Required from ES ****
   * List of routing points
   * List of functions
   * List of Action. How to create the list ???
   * List of Action duo combo. How to create the list ???
   * List of instanceTargetQueue. How to create the list ???
   * List of Units ??
   * List of Priority ???
   *
   *
   */
  /*************************** Routing Keywords ********************/
  /* LINE 3016
   <RoutingKeyword xmlns="urn:swift:saa:xsd:routingKeyword" xmlns:ns2="urn:swift:saa:xsd:syntaxVersion">
    <Identifier>
        <Name>Field11S</Name>
    </Identifier>
    <Description>Field_11S Original message type, Date, Session, ISN</Description>
    <Type>RUKY_STRING</Type>
</RoutingKeyword>
   */

  

  def getRoutingKeywords(environment: String, version : String, nodeKeywords: NodeSeq): Seq[KeywordEntity] =
    nodeKeywords.map(node => KeywordEntity((node \ "Identifier" \ "Name").text, (node \ "Type").text, XmlHelper.toOption((node \ "Description").text)))


}