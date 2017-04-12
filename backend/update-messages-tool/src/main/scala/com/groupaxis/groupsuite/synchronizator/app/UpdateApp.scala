package com.groupaxis.groupsuite.synchronizator.app

import com.groupaxis.groupsuite.persistence.datastore.es.util.GPESHelper
import com.groupaxis.groupsuite.persistence.datastore.jdbc.Database
import com.groupaxis.groupsuite.routing.amh.es.schema.AHMRoutingSchema._
import com.groupaxis.groupsuite.routing.es.schema.SAASchema._
import com.groupaxis.groupsuite.simulation.es.schema.SimulationSchema._
import com.groupaxis.groupsuite.simulator.write.domain.model.mapping.MappingForSystem.AMH_SYSTEM
import com.groupaxis.groupsuite.synchronizator.domain.model.assignment.backend.AMHSynchronousBackendAssignments
import com.groupaxis.groupsuite.synchronizator.domain.model.assignment.distribution.AMHSynchronousDistributionAssignments
import com.groupaxis.groupsuite.synchronizator.domain.model.assignment.feedback.AMHSynchronousFeedbackAssignments
import com.groupaxis.groupsuite.synchronizator.domain.model.backends.AMHSynchronousBackends
import com.groupaxis.groupsuite.synchronizator.domain.model.criteria.AMHSynchronousRuleCriteria
import com.groupaxis.groupsuite.synchronizator.domain.model.exitpoint.SAASynchronousExitPoints
import com.groupaxis.groupsuite.synchronizator.domain.model.messagePartner.SAASynchronousMessagePartners
import com.groupaxis.groupsuite.synchronizator.domain.model.points.SAASynchronousRules
import com.groupaxis.groupsuite.synchronizator.domain.model.rules.AMHSynchronousRules
import com.groupaxis.groupsuite.synchronizator.domain.model.schemas.SAASynchronousSchemas
import com.groupaxis.groupsuite.synchronizator.domain.model.simulation.swift.message.AMHSynchronousSwiftMessages
import com.groupaxis.groupsuite.synchronizator.file.GPFileHelper._
import com.groupaxis.groupsuite.user.es.schema.UserSchema._
import com.sksamuel.elastic4s.{BulkResult, ElasticClient, ElasticsearchClientUri}
import org.apache.logging.log4j.scala.Logging
import org.elasticsearch.action.bulk.BulkResponse

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Properties

trait ESHelper extends Logging {

  import com.sksamuel.elastic4s.ElasticDsl._

  val client: ElasticClient = {
    import org.elasticsearch.common.settings.Settings
    val clusterName = Properties.propOrElse("ES_CLUSTER", "groupsuite")
    val settings = Settings.settingsBuilder().put("cluster.name", clusterName).build()
    val esURL = Properties.propOrElse("ES_URL", "127.0.0.1:9300")
    ElasticClient.transport(settings, ElasticsearchClientUri(s"elasticsearch://$esURL"))
  }

  def bulkIntoES(data: Option[Seq[String]]): Future[BulkResult] = {


    data match {
      case Some(dataSequence) if dataSequence.nonEmpty => client.execute(bulk(
        dataSequence.filter(!_.startsWith("#")).map(_.split("::"))
          .map(values => index into values(0) source values(2) id values(1))
      ))
      case Some(dataSequence) => Future.successful(BulkResult(new BulkResponse(Array(), 0L)))
      case None => Future.successful(BulkResult(new BulkResponse(Array(), 0L)))
    }
  }

}

object ESHelper extends ESHelper

trait DBHelper {
  var dbUrl = Properties.propOrElse("SQL_URL", "jdbc:postgresql://localhost:5432/postgres")
  var dbUser = Properties.propOrElse("SQL_USER", "postgres")
  var dbPassword = Properties.propOrElse("SQL_PASSWORD", "postgres")
  val database = new Database(slick.driver.PostgresDriver, dbUrl, dbUser, dbPassword)
  //    val database = new Database(slick.driver.H2Driver, dbUrl, dbUser, dbPassword)
}

object DBHelper extends DBHelper

trait PropertiesParameters {
  val baseGrammarDir: String = Properties.propOrElse("GRAMMAR_PATH_FOLDER", "C:\\groupsuite\\exported")
  val bulkFileToImport: String = Properties.propOrElse("USERS_FILE_PATH", "C:\\groupsuite\\exported\\users.txt")
  val criteriaFileToImport: String = Properties.propOrElse("CRITERIA_FILE_PATH", "C:\\groupsuite\\exported\\amh_keywords.json")

}

/** *
  * Self-type annotations emphasize mixin composition.
  * Inheritance can imply a subtype relationship.
  * Note: Here we are explicitly defining composition of behavior through mixins,
  * instead of using inheritance and mixins.
  * **/
trait MappingHelper {
  self: DependencyHelper =>
  implicit def ec: ExecutionContext


  def formatMapping(a: String, v: String) = if (a.startsWith("\"")) a + " / \"" + v + "\"" else "\"" + a + "\" / \"" + v + "\""

  private lazy val mappings = mappingRepository.allMappingsBySystem(AMH_SYSTEM)
    .map(mappings =>
      mappings.map(mapping => mapping.keyword)
        .reduce(formatMapping))

  def updateMapping(inputFilePath: String, outputFilePath: String) =
    for {
      grammarText <- Future {
        readFile(inputFilePath) match {
          case Some(file) => file
          case None => throw new Exception(s"Error while reading $inputFilePath")
        }
      }
      mappings <- mappings
      grammarFullText <- Future {
        Some(grammarText + reservedDefinition1 + mappings + reservedDefinition2)
      }
      createdGrammarFile <- Future {
        writeFile(outputFilePath, grammarFullText)
      }
    } yield grammarFullText


  val reservedDefinition1 = "\n ReservedWord \"a criteria ('direction' 'messageType/code', etc.)\" \n  = \"direction\" / \"networkProtocol\" / \"document/data\" / "
  val reservedDefinition2 = "  \n    { \n           return text(); \n      }"
}

object UpdateApp extends DependencyHelper with DBHelper with ESHelper with PropertiesParameters with MappingHelper with Logging {
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  import scala.concurrent.duration._

  val timeout: Duration = 10.minutes

  private def printUsage() = {
    println("Usage: ")
    println("java -jar update-messages.jar option ")
    println("Where option can be any of the following values: {INITIALIZE | ALL | MESSAGES | AUTHENTICATION | GRAMMAR | MESSAGES-GRAMMAR | ROUTING | AMHCRITERIA | SAA}")
  }


  def main(args: Array[String]): Unit = {

    if (args.length != 1) {
      printUsage()
      System.exit(0)
    }

    args(0).toUpperCase() match {
      case "INITIALIZE" =>
        initializeES()
      case "ALL" =>
        initializeES()
        updateAMHRouting()
        updateSAARouting()
        updateAMHUsers()(bulkFileToImport)
        updateAMHMessages()
        updateAMHGrammars()(baseGrammarDir)
        updateAMHCriteria(criteriaFileToImport)
      case "MESSAGES" => updateAMHMessages()
      case "AUTHENTICATION" => updateAMHUsers()(bulkFileToImport)
      case "AMHCRITERIA" => updateAMHCriteria(criteriaFileToImport)
      case "GRAMMAR" => updateAMHGrammars()(baseGrammarDir)
      case "MESSAGES-GRAMMAR" =>
        updateAMHMessages()
        updateAMHGrammars()(baseGrammarDir)
      case "ROUTING" => updateAMHRouting()
      case "SAA" => updateSAARouting()
      case whatever =>
        logger.debug(s"Action '${args(0)}' is not defined.")
        printUsage()
    }

  }

  private def updateAMHCriteria(fileToImport: String): Unit = {

    val action = for {
      create <- GPESHelper.initializeIndex(amhReferenceIndexName, createAMHReferenceIndex)(client)
      insert <- AMHSynchronousRuleCriteria.synchronize.apply(criteriaRepository(fileToImport), criteriaESRepository)
    } yield insert

    Await.result(action, timeout)
  }

  private def updateAMHUsers()(implicit bulkFileToImport: String): Unit = {
    val (source, lines) = readFileAsIterator(bulkFileToImport)

    val action = for {
      create <- GPESHelper.initializeIndex(userIndexName, createUserIndex)(client)
      _ <- Future {
        logger.debug(s"index $userIndexName initialized")
      }
      insert <- bulkIntoES(lines.map(_.toSeq))
      _ <- Future {
        logger.debug(s"bulk to $userIndexName done")
      }
      close <- Future {
        source.close()
      }
    } yield insert

    Await.result(action, timeout)
  }

  private def updateAMHGrammars()(implicit baseDirGrammarPath: String) = {
    val action = for {
      grammarText <- updateMapping(s"$baseDirGrammarPath/AMHRuleGrammar_.pegjs", s"$baseDirGrammarPath/AMHRuleGrammar.pegjs")
      _ <- Future {
        logger.debug(s"$baseDirGrammarPath/AMHRuleGrammar_.pegjs updated")
      }
      grammarEvaluationText <- updateMapping(s"$baseDirGrammarPath/AMHRuleGrammarEvaluation_.pegjs", s"$baseDirGrammarPath/AMHRuleGrammarEvaluation.pegjs")
      _ <- Future {
        logger.debug(s"$baseDirGrammarPath/AMHRuleGrammarEvaluation_.pegjs updated")
      }
    } yield (grammarText, grammarEvaluationText)

    Await.result(action, timeout)
  }

  private def initializeES() = {
    val action = for {
      routing <- GPESHelper.initializeIndex(amhRoutingIndexName, createAMHRoutingIndex)(client)
      auth <- GPESHelper.initializeIndex(userIndexName, createUserIndex)(client)
      messages <- GPESHelper.initializeIndex(simulationIndexName, createMessageIndex)(client)
      saa <- GPESHelper.initializeIndex(saaIndexName, createSAAIndex)(client)
      amhReference <- GPESHelper.initializeIndex(amhReferenceIndexName, createAMHReferenceIndex)(client)
    } yield ()

    Await.result(action, timeout)
  }

  private def updateAMHMessages() = {
    val action = for {
      init <- GPESHelper.initializeIndex(simulationIndexName, createMessageIndex)(client)
      mappings <- mappingRepository.allMappingsBySystem(AMH_SYSTEM)
      esMessages <- AMHSynchronousSwiftMessages.synchronize(mappings)
        .apply(messageRepository, messageESRepository)
    } yield esMessages

    Await.result(action, timeout)

  }

  private def updateSAARouting() = {
    Await.result(GPESHelper.initializeIndex(saaIndexName, createSAAIndex)(client), timeout)
    logger.debug("1. SAA index initialization done...")

    Await.result(SAASynchronousRules.synchronize
      .apply(saaRuleRepository, saaPointESRepository), timeout)
    logger.debug("2. rules done...")

    Await.result(SAASynchronousSchemas.synchronize
      .apply(saaSchemaRepository, saaSchemaESRepository), timeout)
    logger.debug("3. schemas done...")

    Await.result(SAASynchronousExitPoints.synchronize
      .apply(saaExitPointRepository, saaExitPointESRepository), timeout)
    logger.debug("4. exitPoints done...")

    Await.result(SAASynchronousMessagePartners.synchronize
      .apply(saaMessagePartnerRepository, saaMessagePartnerESRepository), timeout)
    logger.debug("5. messagePartners done...")

  }

  private def updateAMHRouting() = {
    Await.result(GPESHelper.initializeIndex(amhRoutingIndexName, createAMHRoutingIndex)(client), timeout)
    logger.debug("1. ES initialization done...")
    val ruleExpressionMap = ruleRepository.ruleExpressionMap

    logger.debug("2. backend assignments ...")
    Await.result(AMHSynchronousBackendAssignments.synchronize(ruleExpressionMap)
      .apply(backendAssignmentRepository, backendAssignmentESRepository), timeout)

    logger.debug("3. distribution copy assignments ...")
    Await.result(AMHSynchronousDistributionAssignments.synchronize(ruleExpressionMap)
      .apply(distributionAssignmentRepository, distributionAssignmentESRepository), timeout)

    logger.debug("4. feedback copy assignments ...")
    Await.result(AMHSynchronousFeedbackAssignments.synchronize(ruleExpressionMap)
      .apply(feedbackAssignmentRepository, feedbackAssignmentESRepository), timeout)

    logger.debug("5. backend ...")
    Await.result(AMHSynchronousBackends.synchronize.apply(backendRepository, backendESRepository), timeout)

    logger.debug("6. rules ...")
    Await.result(AMHSynchronousRules.synchronize.apply(ruleRepository, ruleESRepository), timeout)
    logger.debug("7. Done")
  }

}

import org.apache.logging.log4j.scala.Logging

object TestApp extends ESHelper with PropertiesParameters with Logging {
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  import scala.concurrent.duration._

  val timeout: Duration = 10.minutes

  def main(args: Array[String]): Unit = {
    //    val findAllMessagesWithName = QueryStringQueryDefinition("25_000_messages")
    //    val searchQ = search in "messages/group" query findAllMessagesWithName size 100000
    //    val fresp = client execute { searchQ }
    //    val fmr = fresp.map(msg => {
    //      msg.as[SwiftMessageES]
    //    })
    val res = Seq() //Await.result(fmr, timeout)

    logger.info(s" hits == ${res.length}")
    logger.debug(s" hits == ${res.length}")
    logger.error(s" hits == ${res.length}")
  }

}




