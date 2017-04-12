package com.groupaxis.groupsuite.amh.routing.infrastructor.jdbc

import akka.actor.SupervisorStrategy.{Restart, Resume, Stop}
import akka.actor.{Actor, OneForOneStrategy, Props}
import akka.pattern.ask
import akka.stream.{ActorMaterializer, Supervision}
import akka.util.Timeout
import com.groupaxis.groupsuite.amh.routing.infrastructor.es.ESRuleWriteRepository
import com.groupaxis.groupsuite.amh.routing.interfaces.http.client.HttpAuditRoutingClient
import com.groupaxis.groupsuite.commons.persistence.jdbc.DatabaseService
import com.groupaxis.groupsuite.routing.amh.util.RuleUtil
import com.groupaxis.groupsuite.routing.write.domain.audit.messages.AMHRoutingAuditMessages.{CreateDistributionAssignment, UpdateDistributionAssignment}
import com.groupaxis.groupsuite.routing.write.domain.model.amh.distribution.copy.AMHDistributionCpyMessages._
import com.groupaxis.groupsuite.routing.write.domain.model.amh.distribution.copy.{AMHDistributionCpyBackendEntity, AMHDistributionCpyEntity, AMHDistributionCpyRuleEntity}
import com.groupaxis.groupsuite.routing.write.domain.model.amh.rule.AMHRuleMessages.{AMHRulesFound, FindAllAMHRules, SetRulesAsAssigned, SetRulesAsUnassigned}
import com.groupaxis.groupsuite.routing.write.domain.model.amh.rule.{AMHRule, AMHRuleDAO, AMHRuleEntity}
import com.groupaxis.groupsuite.xml.parser.amh.writer.es.ESAMHDistributionCpyRepository
import com.groupaxis.groupsuite.xml.parser.amh.writer.jdbc.{JdbcAMHDistributionCpyBackendRepository, JdbcAMHDistributionCpyRepository, JdbcAMHDistributionCpyRuleRepository}
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import com.typesafe.config.Config
import org.apache.logging.log4j.scala.Logging
import org.joda.time.DateTime

import scala.concurrent.{Await, Future}
import scala.util.Success

object JdbcAMHDistributionCopyWriteRepository {

  final val Name = "jdbc-amh-distribution-copy-write-repository"

  def props(distributionCopyRepo: JdbcAMHDistributionCpyRepository, distributionCopyRuleRepo: JdbcAMHDistributionCpyRuleRepository,
            distributionCopyBackendRepo: JdbcAMHDistributionCpyBackendRepository, databaseService: DatabaseService, restClient : HttpAuditRoutingClient): Props = Props(classOf[JdbcAMHDistributionCopyWriteRepository], distributionCopyRepo, distributionCopyRuleRepo, distributionCopyBackendRepo, databaseService, restClient)
}

class JdbcAMHDistributionCopyWriteRepository(val distributionCopyRepo: JdbcAMHDistributionCpyRepository, val distributionCopyRuleRepo: JdbcAMHDistributionCpyRuleRepository, distributionCopyBackendRepo: JdbcAMHDistributionCpyBackendRepository, val databaseService: DatabaseService, val restClient : HttpAuditRoutingClient) extends Actor with Logging {

  import scala.concurrent.duration._

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1.minute) {
      case _: ArithmeticException => Resume
      case _: NullPointerException => Restart
      case _: IllegalArgumentException => Stop
      case e: Exception =>
        logger.warn("[HttpRoutingService] Exception has been received, so restarting the actor " + e.getMessage)
        e.printStackTrace()
        Restart
    }

  val decider: Supervision.Decider = (ex) => Supervision.Resume

  //TODO: Move this to another actor to make the denormalization asynchronous
  val config: Config = context.system.settings.config

  import org.elasticsearch.common.settings.Settings

  val settings = Settings.settingsBuilder().put("cluster.name", config.getString("elastic.cluster.name")).build()
  val client: ElasticClient = ElasticClient.transport(settings, ElasticsearchClientUri(s"elasticsearch://" + config.getString("elastic.url")))
  val esDistributionRepo: ESAMHDistributionCpyRepository = new ESAMHDistributionCpyRepository(client)

  val esRuleWrite = context.actorOf(ESRuleWriteRepository.props(client), ESRuleWriteRepository.Name)

  //TODO:Remove AMHRule creation and move it to the same actor than esAssignmentRepo
  val ruleDao: AMHRuleDAO = AMHRuleDAO
  val ruleRepo = context.actorOf(JdbcAMHRuleWriteRepository.props(ruleDao, databaseService, restClient), JdbcAMHRuleWriteRepository.Name + "_temp")

  implicit val ec = context.dispatcher

  def getRuleExpression(rules: Seq[AMHRuleEntity])(code: String): String = {
    logger.debug("looking for code F " + code + " in " + rules)
    val foundExpression: Seq[Option[String]] = rules.filter(_.code.equalsIgnoreCase(code)).map(_.expression)
    if (foundExpression.nonEmpty) {
      return foundExpression.head.getOrElse("")
    }
    ""
  }

  def ruleFinder: (String) => String = {
    implicit val timeout: Timeout = 15.seconds
    logger.debug("ruleFinder calling FindAllAMHRules ")
    logger.debug("ruleFinder calling FindAllAMHRules ")
    val rulesFound = Await.result((ruleRepo ? FindAllAMHRules()).mapTo[Either[String,AMHRulesFound]], 5.seconds)
    val rules = rulesFound match {
      case Left(ex) => logger.error(s"An error while looking for rules: $ex"); Seq()
      case Right(amhRulesFound) => amhRulesFound.rules.getOrElse(Seq())
    }
    getRuleExpression(rules)
  }

  implicit val system = context.system
  implicit val materializer = ActorMaterializer()

  def receive: Receive = {
    case CreateAMHDistributionCpy(code, distributionEntityUpdate, userId, datetime) =>
      logger.info(s" receiving create($distributionEntityUpdate) on JdbcAMHDistributionWriteRepository")
      logger.debug(s" receiving create($distributionEntityUpdate) on JdbcAMHDistributionWriteRepository")

      val distributionToCreate = distributionEntityUpdate.merge(code)
      logger.debug("distributionToCreate " + distributionToCreate)

      val result = distributionCopyRepo.createDistributionCopy(distributionToCreate)
      logger.info(s"disitrbution $result created")

      result.fold(
        errorMsg => {
          logger.info("Distribution creation failed with " + errorMsg)
          logger.debug("Distribution creation failed with " + errorMsg)
          sender() ! result
        },
        distributionCreated => {
          logger.info(s"distribution created!")

          logger.info(s"Now updating audit with new: ${distributionCreated.response}")
          val username = distributionCreated.response.creationUserId.getOrElse("no_user_defined")
          val dateTime = distributionCreated.response.creationDate

          val auditCreateDistribution = CreateDistributionAssignment(username, dateTime, distributionCreated.response)
          restClient.sendDistributionCreation(auditCreateDistribution)
            .onComplete {
              case Success(resp) =>
                resp.fold(
                  error => logger.error(s"While auditing distribution assignment create: $error"),
                  done => logger.info(s" Audit distribution assignment create success")
                )
              case error => logger.error(s"While auditing distribution assignment create: $error")
            }

          val rulesCreatedResponse = distributionCopyRuleRepo.createDistributionCpyRules(distributionCreated.response.rules)
          rulesCreatedResponse.fold(
            errorMsg => {
              logger.debug(s"Rule Distribution creation failed with $errorMsg")
              logger.info("Rule Distribution creation failed with " + errorMsg)
              sender() ! rulesCreatedResponse
            },
            rulesCreated => {
              val backendsCreatedResponse = distributionCopyBackendRepo.createDistributionCpyBackends(distributionCreated.response.backends)
              backendsCreatedResponse.fold(
                errorMsg => {
                  logger.debug(s"Backend Distribution creation failed with $errorMsg")
                  logger.info("Backend Distribution creation failed with " + errorMsg)
                  sender() ! backendsCreatedResponse
                },
                backendsCreated => {
                  try {
                    val finder = ruleFinder
                    val distributionRules = rulesCreated.response.map({
                      rule: AMHDistributionCpyRuleEntity => {
                        logger.debug("finding ruleCode " + rule.ruleCode)
                        rule.toES(finder(rule.ruleCode))
                      }
                    })
                    val distributionBackends = distributionCreated.response.backends.map({
                      backend: AMHDistributionCpyBackendEntity => {
                        logger.debug("finding backendCode " + backend.backCode)
                        backend.toES
                      }
                    })

                    esDistributionRepo.insert(distributionCreated.response.toES(distributionBackends, distributionRules))
                    logger.info("distribution was created into ES ")
                    esRuleWrite ! SetRulesAsAssigned(rulesCreated.response)

                  } catch {
                    case e: Exception =>
                      logger.error("distribution was not saved into ES : " + e.getMessage)
                  }

                  sender() ! backendsCreatedResponse
                }
              )
            }
          )
        }
      )
    case UpdateAMHDistributionCpy(code, distributionEntityUpdate, userId, datetime) =>
      logger.info(s" receiving create($distributionEntityUpdate) on JdbcAMHAssignmentWriteRepository")

      val oldDistribution = distributionCopyRepo.getDistributionCopyByCode(code).fold(err => None, resp => resp.distributionCpy)
      val orginalDistributionRules = distributionCopyRuleRepo.getDistributionRulesByCode(code)
      val originalDistributionBackends = distributionCopyBackendRepo.findAllDistributionBackendsByCode(code)
      val result = distributionCopyRepo.updateDistributionCopy(code, distributionEntityUpdate, orginalDistributionRules, originalDistributionBackends)
      logger.info(s"amh distribution $result updated")
      result.fold(
        errorMsg => {
          logger.info("Assignment creation failed with " + errorMsg)
          sender() ! result
        },
        distributionUpdated => {
          logger.info("distribution updated!")

          logger.info(s"now updating audit with old: $oldDistribution new: ${distributionUpdated.response}")
          val notFoundDistribution = AMHDistributionCpyEntity(code, -1, "","","Y", Some("N"), None,None,Some("description"), None,None,None,None)
          val username = distributionUpdated.response.modificationUserId.getOrElse("no_user_defined")
          val dateTime = distributionUpdated.response.modificationDate.getOrElse(DateTime.now)
          val auditUpdateDistribution = UpdateDistributionAssignment(username, dateTime, oldDistribution.getOrElse(notFoundDistribution),distributionUpdated.response)
          restClient.sendDistributionUpdate(auditUpdateDistribution)
            .onComplete {
              case Success(resp) =>
                resp.fold(
                  error => logger.error(s"While auditing distribution update: $error"),
                  done => logger.info(s" Audit distribution update success")
                )
              case error => logger.error(s"While auditing distribution update: $error")
            }


          distributionCopyRuleRepo.deleteDistributionRulesByDistributionCode(code)
          val rulesCreatedResponse = distributionCopyRuleRepo.createDistributionCpyRules(distributionUpdated.response.rules)
          rulesCreatedResponse.fold(
            errorMsg => {
              logger.debug(s"Assignment creation failed with $errorMsg")
              logger.info("Assignment creation failed with " + errorMsg)
              sender() ! rulesCreatedResponse
            },
            rulesUpdated => {
              logger.info("rules distribution updated!")
              logger.debug(s"rules distribution updated!")
              distributionCopyBackendRepo.deleteDistributionBackendsByDistributionCode(code)
              val backendsUpdatedResponse = distributionCopyBackendRepo.createDistributionCpyBackends(distributionUpdated.response.backends)
              backendsUpdatedResponse.fold(
                errorMsg => {
                  logger.debug(s"Backend Distribution creation failed with $errorMsg")
                  logger.info("Backend Distribution creation failed with " + errorMsg)
                  sender() ! backendsUpdatedResponse
                },
                backendsCreated => {
                  try {
                    val finder = ruleFinder
                    val distributionRules = distributionUpdated.response.rules.map(
                      { rule: AMHDistributionCpyRuleEntity => rule.toES(finder(rule.ruleCode)) })

                    val distributionBackends = distributionUpdated.response.backends.map(
                      { backend: AMHDistributionCpyBackendEntity => {
                        logger.debug("finding backendCode " + backend.backCode)
                        backend.toES
                      }
                      })

                    for {
                      updated <- esDistributionRepo.updateDistributionCpy(distributionUpdated.response.toES(distributionBackends, distributionRules))
                      (removed: Option[Seq[AMHRule]], added: Option[Seq[AMHRule]]) <- Future {
                        RuleUtil.getRemovedAddedPair(orginalDistributionRules, rulesUpdated.response)
                      }
                    } yield {
                      logger.info("distribution was created into ES ")
                      logger.info(s"Sending update rule unassign $removed")
                      removed
                        .filter(rules => rules.nonEmpty)
                        .foreach(rules => esRuleWrite ! SetRulesAsUnassigned(rules))
                      logger.info(s"Sending update rule assign $added")
                      added
                        .filter(rules => rules.nonEmpty)
                        .foreach(rules => esRuleWrite ! SetRulesAsAssigned(rules))
                    }

                  } catch {
                    case e: Exception =>
                      logger.error("distribution was not saved into ES : " + e.getMessage)
                  }
                  sender() ! result
                })
            })
        })

    case FindAMHDistributionCpyByCode(code) =>
      logger.debug(s" receiving FindAMHDistributionCpyByCode($code) on JdbcAMHDistributionCpyWriteRepository")
      val result = distributionCopyRepo.getDistributionCopyByCode(code)
      logger.debug(s"retrieving amh distribution $result")
      sender() ! result

    case FindAllAMHDistributionCpy() =>
      logger.debug(s" receiving FindAllAMHDistributionCpy on JdbcAMHDistributionCpyWriteRepository")
      val result = distributionCopyRepo.findAllDistributionCps
      logger.debug(s"retrieving amh distribution $result")
      sender() ! AMHDistributionCpsFound(result)
  }

}