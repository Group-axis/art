package com.groupaxis.groupsuite.amh.routing.infrastructor.jdbc

import akka.actor.{Actor, Props}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.groupaxis.groupsuite.amh.routing.infrastructor.es.ESRuleWriteRepository
import com.groupaxis.groupsuite.amh.routing.interfaces.http.client.HttpAuditRoutingClient
import com.groupaxis.groupsuite.commons.persistence.jdbc.DatabaseService
import com.groupaxis.groupsuite.routing.amh.util.RuleUtil
import com.groupaxis.groupsuite.routing.write.domain.audit.messages.AMHRoutingAuditMessages.{CreateFeedbackAssignment, UpdateFeedbackAssignment}
import com.groupaxis.groupsuite.routing.write.domain.model.amh.feedback.distribution.copy.AMHFeedbackDistributionCpyMessages._
import com.groupaxis.groupsuite.routing.write.domain.model.amh.feedback.distribution.copy.{AMHFeedbackDistributionCpyBackendEntity, AMHFeedbackDistributionCpyEntity, AMHFeedbackDistributionCpyRuleEntity}
import com.groupaxis.groupsuite.routing.write.domain.model.amh.rule.AMHRuleMessages.{AMHRulesFound, FindAllAMHRules, SetRulesAsAssigned, SetRulesAsUnassigned}
import com.groupaxis.groupsuite.routing.write.domain.model.amh.rule.{AMHRule, AMHRuleDAO, AMHRuleEntity}
import com.groupaxis.groupsuite.xml.parser.amh.writer.es.ESAMHFeedbackDtnCpyRepository
import com.groupaxis.groupsuite.xml.parser.amh.writer.jdbc._
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import com.typesafe.config.Config
import org.apache.logging.log4j.scala.Logging
import org.joda.time.DateTime

import scala.concurrent.{Await, Future}
import scala.util.Success

object JdbcAMHFeedbackDistributionCopyWriteRepository {

  final val Name = "jdbc-amh-feedback-copy-write-repository"

  def props(feedbackCopyRepo: JdbcAMHFeedbackDtnCpyRepository, feedbackCopyRuleRepo: JdbcAMHFeedbackDtnCpyRuleRepository,
            feedbackCopyBackendRepo: JdbcAMHFeedbackDtnCpyBackendRepository, databaseService: DatabaseService, restClient : HttpAuditRoutingClient): Props = Props(classOf[JdbcAMHFeedbackDistributionCopyWriteRepository], feedbackCopyRepo, feedbackCopyRuleRepo, feedbackCopyBackendRepo, databaseService, restClient)
}

class JdbcAMHFeedbackDistributionCopyWriteRepository(val feedbackCopyRepo: JdbcAMHFeedbackDtnCpyRepository, val feedbackCopyRuleRepo: JdbcAMHFeedbackDtnCpyRuleRepository, feedbackCopyBackendRepo: JdbcAMHFeedbackDtnCpyBackendRepository, val databaseService: DatabaseService, val restClient : HttpAuditRoutingClient) extends Actor with Logging {
  import scala.concurrent.duration._

  //TODO: Move this to another actor to make the denormalization asynchronous
  val config : Config = context.system.settings.config
  import org.elasticsearch.common.settings.Settings
  val settings = Settings.settingsBuilder().put("cluster.name", config.getString("elastic.cluster.name")).build()
  val client : ElasticClient = ElasticClient.transport(settings, ElasticsearchClientUri(s"elasticsearch://"+config.getString("elastic.url")))
  val esFeedbackRepo: ESAMHFeedbackDtnCpyRepository = new ESAMHFeedbackDtnCpyRepository(client)

  val esRuleWrite = context.actorOf(ESRuleWriteRepository.props(client), ESRuleWriteRepository.Name)

  //TODO:Remove AMHRule creation and move it to the same actor than esAssignmentRepo
  val ruleDao : AMHRuleDAO = AMHRuleDAO
  val ruleRepo = context.actorOf(JdbcAMHRuleWriteRepository.props(ruleDao, databaseService, restClient),JdbcAMHRuleWriteRepository.Name+"_temp")

  implicit val ec = context.dispatcher

  def getRuleExpression(rules : Seq[AMHRuleEntity])(code : String) : String = {
    logger.debug("looking for code F "+ code + " in " +rules)
    val foundExpression : Seq[Option[String]] = rules.filter(_.code.equalsIgnoreCase(code)).map(_.expression)
    if (foundExpression.nonEmpty) {
      return foundExpression.head.getOrElse("")
    }
    ""
  }

  def ruleFinder: String => String = {
    implicit val timeout: Timeout = 50.seconds
    logger.info("ruleFinder calling FindAllAMHRules ")
    val rulesFound = Await.result((ruleRepo ?  FindAllAMHRules()).mapTo[Either[String,AMHRulesFound]], 50.seconds)
    val rules = rulesFound match {
      case Left(ex) => logger.error(s"An error while looking for rules: $ex"); Seq()
      case Right(amhRulesFound) => amhRulesFound.rules.getOrElse(Seq())
    }
    getRuleExpression(rules)
  }

  implicit val system = context.system
  implicit val materializer = ActorMaterializer()

  def receive: Receive = {
    case CreateAMHFeedbackDistributionCpy(code, feedbackEntityUpdate, userId, datetime) =>
      logger.info(s" receiving create($feedbackEntityUpdate) on JdbcAMHFeedbackWriteRepository")

      val username = userId.getOrElse("no_user_defined")
      val dateTime = datetime.getOrElse(DateTime.now)

      val feedbackToCreate = feedbackEntityUpdate.merge(code)
      logger.debug("feedbackToCreate " + feedbackToCreate)

      val result = feedbackCopyRepo.createFeedbackCopy(feedbackToCreate)
      logger.info(s"feedback $result created")

      result.fold(
        errorMsg => {
          logger.info("Feedback creation failed with " + errorMsg)
          sender() ! result
        },
        feedbackCreated => {
          logger.info(s"Feedback created!")

          logger.info(s"Now updating audit with new: ${feedbackCreated.response}")
          val auditCreateFeedback = CreateFeedbackAssignment(username, dateTime, feedbackCreated.response)
          restClient.sendFeedbackCreation(auditCreateFeedback)
            .onComplete {
              case Success(resp) =>
                resp.fold(
                  error => logger.error(s"While auditing feedback assignment create: $error"),
                  done => logger.info(s" Audit feedback assignment create success")
                )
              case error => logger.error(s"While auditing feedback assignment create: $error")
            }

          val rulesCreatedResponse = feedbackCopyRuleRepo.createFeedbackDtnCpyRules(feedbackCreated.response.rules)
          rulesCreatedResponse.fold(
            errorMsg => {
              logger.debug(s"Rule Feedback creation failed with $errorMsg")
              logger.info("Rule Feedback creation failed with " + errorMsg)
              sender() ! rulesCreatedResponse
            },
            rulesCreated => {
              val backendsCreatedResponse = feedbackCopyBackendRepo.createFeedbackDtnCpyBackends(feedbackCreated.response.backends)
              backendsCreatedResponse.fold(
                errorMsg => {
                  logger.debug(s"Backend Feedback creation failed with $errorMsg")
                  logger.info("Backend Feedback creation failed with " + errorMsg)
                  sender() ! backendsCreatedResponse
                },
                backendsCreated => {
                  try {
                    val finder = ruleFinder
                    val feedbackRules = rulesCreated.response.map({
                      rule: AMHFeedbackDistributionCpyRuleEntity => {
                        logger.debug("finding ruleCode " + rule.ruleCode)
                        rule.toES(finder(rule.ruleCode))
                      }
                    })
                    val feedbackBackends = feedbackCreated.response.backends.map({
                      backend: AMHFeedbackDistributionCpyBackendEntity => {
                        logger.debug("finding backendCode " + backend.backCode)
                        backend.toES
                      }
                    })

                    esFeedbackRepo.insert(feedbackCreated.response.toES(feedbackBackends, feedbackRules))
                    logger.info("feedback was created into ES ")
                    esRuleWrite ! SetRulesAsAssigned(rulesCreated.response)
                  } catch {
                    case e: Exception =>
                      logger.error("feedback was not saved into ES : " + e.getMessage)
                  }

                  sender() ! backendsCreatedResponse
                }
              )
            }
          )
        }
      )
    case UpdateAMHFeedbackDistributionCpy(code, feedbackEntityUpdate, userId, datetime) =>
      logger.info(s" receiving create($feedbackEntityUpdate) on JdbcAMHFeedbackWriteRepository")

      val username = userId.getOrElse("no_user_defined")
      val dateTime = datetime.getOrElse(DateTime.now)
      val oldFeedback = feedbackCopyRepo.getFeedbackCopyByCode(code).fold(err => None, resp => resp.feedbackDistCpy)
      val orginalfeedbackCopyRules = feedbackCopyRuleRepo.getFeedbackRulesByDistributionCode(code)
      val orginalfeedbackCopyBackends = feedbackCopyBackendRepo.findAllFeedbackBackendsByCode(code)

      val result = feedbackCopyRepo.updateFeedbackCopy(code, feedbackEntityUpdate, orginalfeedbackCopyRules, orginalfeedbackCopyBackends)
      logger.info(s"amh feedback $result updated")
      result.fold(
        errorMsg => {
          logger.error("Feedback update failed with " + errorMsg)
          sender() ! result
        },
        feedbackUpdated => {
          logger.info("feedback updated!")

          logger.info(s"now updating audit with old: $oldFeedback new: ${feedbackUpdated.response}")
          val notFoundFeedback = AMHFeedbackDistributionCpyEntity(code, -1, "","","Y", Some("N"), None,None,Some("description"), None,None,None,None, orginalfeedbackCopyBackends, orginalfeedbackCopyRules)
          val updatedOldFeedback = oldFeedback.map(_.copy(backends=orginalfeedbackCopyBackends,  rules=orginalfeedbackCopyRules)).getOrElse(notFoundFeedback)
          val auditUpdateFeedback = UpdateFeedbackAssignment(username, dateTime, updatedOldFeedback,feedbackUpdated.response)
          restClient.sendFeedbackUpdate(auditUpdateFeedback)
            .onComplete {
              case Success(resp) =>
                resp.fold(
                  error => logger.error(s"While auditing feedback update: $error"),
                  done => logger.info(s" Audit feedback update success")
                )
              case error => logger.error(s"While auditing feedback update: $error")
            }


          feedbackCopyRuleRepo.deleteFeedbackRulesByDistributionCode(code)
          val rulesCreatedResponse = feedbackCopyRuleRepo.createFeedbackDtnCpyRules(feedbackUpdated.response.rules)
          rulesCreatedResponse.fold(
            errorMsg => {
              logger.info("Feedback update failed with " + errorMsg)
              sender() ! rulesCreatedResponse
            },
            rulesUpdated => {
              logger.info("rules feedback updated!")
              feedbackCopyBackendRepo.deleteFeedbackDistributionBackendsByDistributionCode(code)
              val backendsUpdatedResponse = feedbackCopyBackendRepo.createFeedbackDtnCpyBackends(feedbackUpdated.response.backends)
              backendsUpdatedResponse.fold(
                errorMsg => {
                  logger.info(s"Backend Feedback update failed with $errorMsg")
                  sender() ! backendsUpdatedResponse
                },
                backendsCreated => {
                  try {
                    val finder = ruleFinder
                    val feedbackRules = feedbackUpdated.response.rules.map(
                      { rule: AMHFeedbackDistributionCpyRuleEntity => rule.toES(finder(rule.ruleCode)) })

                    val feedbackBackends = feedbackUpdated.response.backends.map(
                      { backend: AMHFeedbackDistributionCpyBackendEntity => {
                        logger.debug("finding backendCode " + backend.backCode)
                        backend.toES
                      }
                      })

//                    esFeedbackRepo.updateFeedbackDtnCpy(feedbackUpdated.response.toES(feedbackBackends, feedbackRules))
//                    logger.info("feedback was created into ES ")

                    for {
                      updated <- esFeedbackRepo.updateFeedbackDtnCpy(feedbackUpdated.response.toES(feedbackBackends, feedbackRules))
                      (removed: Option[Seq[AMHRule]], added: Option[Seq[AMHRule]]) <- Future {
                        RuleUtil.getRemovedAddedPair(orginalfeedbackCopyRules, rulesUpdated.response)
                      }
                    } yield {
                      logger.info("feedback was updated in ES ")
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
                      logger.error("feedback was not saved into ES : " + e.getMessage)
                  }
                  sender() ! result
                })
            })
        })

    case FindAMHFeedbackDistributionCpyByCode(code) =>
      logger.debug(s" receiving FindAMHFeedbackDistributionCpyByCode($code) on JdbcAMHFeedbackCpyWriteRepository")
      val result = feedbackCopyRepo.getFeedbackCopyByCode(code)
      logger.debug(s"retrieving amh feedback $result")
      sender() ! result

    case FindAllAMHFeedbackDistributionCpy() =>
      logger.debug(s" receiving FindAllAMHFeedbackCpy on JdbcAMHFeedbackCpyWriteRepository")
      val result = feedbackCopyRepo.findAllFeedbackDtnCps
      logger.debug(s"retrieving amh Feedback $result")
      sender() ! AMHFeedbackDistributionCpsFound(result)
  }

}