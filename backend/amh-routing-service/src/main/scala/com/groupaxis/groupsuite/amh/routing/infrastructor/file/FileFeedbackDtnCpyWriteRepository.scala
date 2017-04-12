package com.groupaxis.groupsuite.amh.routing.infrastructor.file

import com.groupaxis.groupsuite.routing.write.domain.model.amh.feedback.distribution.copy.{AMHFeedbackDistributionCpyBackendEntity, AMHFeedbackDistributionCpyEntity, AMHFeedbackDistributionCpyRuleEntity}
import com.groupaxis.groupsuite.routing.write.domain.model.amh.rule.AMHRuleEntity
import com.groupaxis.groupsuite.xml.parser.amh.model.Messages.XmlFileExported
import com.groupaxis.groupsuite.xml.parser.amh.reader.AMHParser
import com.groupaxis.groupsuite.xml.parser.amh.writer.es.{ESAMHFeedbackDtnCpyRepository, ESAMHRuleRepository}
import com.groupaxis.groupsuite.xml.parser.amh.writer.jdbc._
import com.sksamuel.elastic4s.ElasticClient
import org.apache.logging.log4j.scala.Logging

import scala.collection.immutable.HashMap
import scala.concurrent.Future
import scala.util.{Success, Try}
import scala.xml.{Elem, XML}

class FileFeedbackDtnCpyWriteRepository(jdbcFeedbackDtnCpyRepo : JdbcAMHFeedbackDtnCpyRepository, jdbcFeedbackBackendRepo : JdbcAMHFeedbackDtnCpyBackendRepository, jdbcFeedbackRuleRepo : JdbcAMHFeedbackDtnCpyRuleRepository) extends Logging {

  import AMHParser._
//  import scala.concurrent.duration._

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

  def doImport(env : Option[String], version: Option[String], amhRules : AMHRulesCreated, username: String, elements : Option[Seq[Elem]], client : ElasticClient) : Try[Seq[AMHFeedbackDistributionCpyRuleEntity]] = {
    if (elements.isEmpty || elements.get.isEmpty) {
      return Success(Seq())
    }
    val feedbackElements : Seq[Elem] = elements.get
    //val pp = getRuleExpression(amhRules.rules)_
    logger.debug(" -------------   " + feedbackElements.size + " feedback------------")
    val ff = toAMHFeedbackDtnCpy(env.getOrElse("UNKNOWN"), version.getOrElse("DEFAULT"), username)_
    val tuples : Seq[(AMHFeedbackDistributionCpyEntity, Seq[AMHFeedbackDistributionCpyRuleEntity], Seq[AMHFeedbackDistributionCpyBackendEntity])] = feedbackElements.map(ff)
    logger.debug(" FeedbackDtnCopies :")
    tuples.foreach(d => logger.debug("==> "+d._1))
    val feedbackResp = jdbcFeedbackDtnCpyRepo.createFeedbackDtnCps(tuples.map(_._1))
    logger.debug(" FeedbackDtnCopies AFTER : " + feedbackResp)
    tuples.map(_._2).filter(_.nonEmpty).map(_.size).foreach(a =>logger.debug(s"$a"))
    val rules : Seq[AMHFeedbackDistributionCpyRuleEntity] = tuples.filter(_._2.nonEmpty).flatMap(_._2)
    val backends : Seq[AMHFeedbackDistributionCpyBackendEntity] = tuples.filter(_._3.nonEmpty).flatMap(_._3)
    val feedbackRuleResp = jdbcFeedbackRuleRepo.createFeedbackDtnCpyRules(rules)
    val feedbackBackendResp = jdbcFeedbackBackendRepo.createFeedbackDtnCpyBackends(backends)

    Try(feedbackRuleResp.fold(
      ex => {
        logger.debug("Operation failed with " + ex)
        throw new Exception(s"Operation failed : $ex")
      },
      rulesCreated => {
        feedbackBackendResp.fold(
          backEx => {
            logger.debug("Operation failed with " + backEx)
            throw new Exception(s"Operation failed : $backEx")
          },
          backendCreated => {
            logger.debug("Operation produced value: " + backendCreated)
            val esFeedbackDtnCpyRepo: ESAMHFeedbackDtnCpyRepository = new ESAMHFeedbackDtnCpyRepository(client)
            val esRuleRepo : ESAMHRuleRepository = new  ESAMHRuleRepository(client)
            esFeedbackDtnCpyRepo.insert(tuples.map(t => {
              t._1.toES(t._3.map(b => b.toES), t._2.map(r => r.toES(esRuleRepo.findExpressionByCode(r.ruleCode))))
            }))
            rules
          }
        )
      }
    ))
  }

  def createDirectory(pathStr : String): Unit = {
    import scalax.file.Path
    val path : Path = Path.fromString(pathStr)
    path.createDirectory(failIfExists = false)
  }

  def doExport(env : Option[String], version : Option[String], filePath : String): Either[String, XmlFileExported] = {
    val tmpFeedbackDirectory = filePath + "/Gateway.FeedbackDistributionCopySelectionTable"
    createDirectory(tmpFeedbackDirectory)

    val allFeedbackDtnCps = jdbcFeedbackDtnCpyRepo.findAllFeedbackCpsByEnvAndVersion(env.getOrElse("UNKNOWN"), version.getOrElse("DEFAULT"))
    val allFeedbackDtnCpyRules = jdbcFeedbackRuleRepo.findAllFeedbackRulesByEnvAndVersion(env.getOrElse("UNKNOWN"), version.getOrElse("DEFAULT"))
    val allFeedbackDtnCpyBackends = jdbcFeedbackBackendRepo.findAllFeedbackBackendsByEnvAndVersion(env.getOrElse("UNKNOWN"), version.getOrElse("DEFAULT"))

    val feedbackRuleMap : Map[String, Seq[AMHFeedbackDistributionCpyRuleEntity]] = allFeedbackDtnCpyRules.map(rules => rules.groupBy(_.code.toUpperCase())).getOrElse(new HashMap())
    val feedbackBackendMap : Map[String, Seq[AMHFeedbackDistributionCpyBackendEntity]] = allFeedbackDtnCpyBackends.map(backends => backends.groupBy(_.code.toUpperCase())).getOrElse(new HashMap())

    if ( allFeedbackDtnCps.isEmpty ) {
      logger.debug("There is no Feedback Distribution Copies to export")
      return Right(XmlFileExported("Success"))
    }

    allFeedbackDtnCps.foreach( feedbackDtnCpys => {
      feedbackDtnCpys.foreach{
        feedbackDtnCpy => {
          XML.save(tmpFeedbackDirectory + "/export_"+feedbackDtnCpy.code.toUpperCase+".xml", XmlHelper.toFeedbackDtnCpyXml(feedbackDtnCpy, feedbackBackendMap.get(feedbackDtnCpy.code.toUpperCase), feedbackRuleMap.get(feedbackDtnCpy.code.toUpperCase)), "UTF-8", xmlDecl=true)
        }
      }
    })
    Right(XmlFileExported("Success"))
  }

  def initialize() = {
    jdbcFeedbackRuleRepo.deleteAll()
    jdbcFeedbackBackendRepo.deleteAll()
    jdbcFeedbackDtnCpyRepo.deleteAll()
  }


  def unAssignRule(ruleCode : String): Future[Int] = {
     jdbcFeedbackRuleRepo.deleteFeedbackRulesByRuleCode(ruleCode)
  }

}
