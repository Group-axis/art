package com.groupaxis.groupsuite.amh.routing.infrastructor.file

import com.groupaxis.groupsuite.routing.write.domain.model.amh.distribution.copy.{AMHDistributionCpyBackendEntity, AMHDistributionCpyEntity, AMHDistributionCpyRuleEntity}
import com.groupaxis.groupsuite.routing.write.domain.model.amh.rule.AMHRuleEntity
import com.groupaxis.groupsuite.xml.parser.amh.model.Messages.XmlFileExported
import com.groupaxis.groupsuite.xml.parser.amh.reader.AMHParser
import com.groupaxis.groupsuite.xml.parser.amh.writer.es.{ESAMHDistributionCpyRepository, ESAMHRuleRepository}
import com.groupaxis.groupsuite.xml.parser.amh.writer.jdbc.{JdbcAMHDistributionCpyBackendRepository, _}
import com.sksamuel.elastic4s.ElasticClient
import org.apache.logging.log4j.scala.Logging

import scala.collection.immutable.HashMap
import scala.concurrent.Future
import scala.util.{Success, Try}
import scala.xml.{Elem, XML}

class FileDistributionCpyWriteRepository(jdbcDistributionCpyRepo : JdbcAMHDistributionCpyRepository, jdbcDistributionBackendRepo : JdbcAMHDistributionCpyBackendRepository, jdbcDistributionRuleRepo : JdbcAMHDistributionCpyRuleRepository) extends Logging {

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

  def doImport(env : Option[String], version : Option[String], amhRules : AMHRulesCreated, username : String, elements : Option[Seq[Elem]], client : ElasticClient) : Try[Seq[AMHDistributionCpyRuleEntity]] = {
    if (elements.isEmpty || elements.get.isEmpty) {
      return Success(Seq())
    }
    val distributionElements : Seq[Elem] = elements.get
    logger.debug(s" -------------   ${distributionElements.size} distributionCopies------------")
    val ff = toAMHDistributionCpy(env.getOrElse("UNKNOWN"), version.getOrElse("DEFAULT"), username)_
    val tupled : Seq[(AMHDistributionCpyEntity, Seq[AMHDistributionCpyRuleEntity], Seq[AMHDistributionCpyBackendEntity])] = distributionElements.map(ff)
    logger.debug(" DistributionCopies : ")
    tupled.foreach(d => logger.debug("==> "+d._1))
    val distributionResp = jdbcDistributionCpyRepo.createDistributionCps(tupled.map(_._1))
    logger.debug(" DistributionCopies AFTER : " + distributionResp)
    tupled.map(_._2).filter(_.nonEmpty).map(_.size).foreach(a => logger.debug(s"$a"))
    val rules : Seq[AMHDistributionCpyRuleEntity] = tupled.filter(_._2.nonEmpty).flatMap(_._2)
    val backends : Seq[AMHDistributionCpyBackendEntity] = tupled.filter(_._3.nonEmpty).flatMap(_._3)
    val distributionRuleResp = jdbcDistributionRuleRepo.createDistributionCpyRules(rules)
    val distributionBackendResp = jdbcDistributionBackendRepo.createDistributionCpyBackends(backends)

    Try(distributionRuleResp.fold(
      ex => {
        logger.error(s"Importing Distribution-Copy-Rule failed with $ex")
        throw new Exception(s"Importing Distribution-Copy-Rule failed with $ex")
      },
      rulesCreated => {
        distributionBackendResp.fold(
          backEx => {
            logger.debug(s"Importing Distribution-Copy-Back failed with $backEx" )
            throw new Exception(s"Importing Distribution-Copy-Back failed with $backEx")
          },
          backendCreated => {
            if (backendCreated.response.nonEmpty) {
              logger.debug(s"Importing Distribution-Copy-Back success")

              val esDistributionCpyRepo: ESAMHDistributionCpyRepository = new ESAMHDistributionCpyRepository(client)
              val esRuleRepo: ESAMHRuleRepository = new ESAMHRuleRepository(client)
              esDistributionCpyRepo.insert(tupled.map(t => {
                t._1.toES(t._3.map(b => b.toES), t._2.map(r => r.toES(esRuleRepo.findExpressionByCode(r.ruleCode))))
              }))
            } else {
              logger.debug("No Distribution copy to import!")
            }
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
    val tmpDistributionDirectory = filePath + "/Gateway.DistributionCopySelectionTable"
    createDirectory(tmpDistributionDirectory)

    val allDistributionCps = jdbcDistributionCpyRepo.findAllDistributionsByEnvAndVersion(env.getOrElse("UNKNOWN"), version.getOrElse("DEFAULT"))
    val allDistributionCpyRules = jdbcDistributionRuleRepo.findAllDistributionRulesByEnvAndVersion(env.getOrElse("UNKNOWN"), version.getOrElse("DEFAULT"))
  val allDistributionCpyBackends = jdbcDistributionBackendRepo.findAllDistributionBackendsByEnvAndVersion(env.getOrElse("UNKNOWN"), version.getOrElse("DEFAULT"))

    val distributionRuleMap : Map[String, Seq[AMHDistributionCpyRuleEntity]] = allDistributionCpyRules.map(rules => rules.groupBy(_.code.toUpperCase())).getOrElse(new HashMap())
    val distributionBackendMap : Map[String, Seq[AMHDistributionCpyBackendEntity]] = allDistributionCpyBackends.map(backends => backends.groupBy(_.code.toUpperCase())).getOrElse(new HashMap())

    if ( allDistributionCps.isEmpty ) {
      logger.warn("There is no Distribution Copies to export")
      return Right(XmlFileExported("Success"))
    }

    allDistributionCps.foreach( DistributionCpys => {
      DistributionCpys.foreach{
        distributionCpy => {
          XML.save(tmpDistributionDirectory + "/export_"+distributionCpy.code.toUpperCase+".xml", XmlHelper.toDistributionCpyXml(distributionCpy, distributionBackendMap.get(distributionCpy.code.toUpperCase), distributionRuleMap.get(distributionCpy.code.toUpperCase)), "UTF-8", xmlDecl=true)
        }
      }
    })
    Right(XmlFileExported("Success"))
  }

  def initialize() = {
    jdbcDistributionBackendRepo.deleteAll()
    jdbcDistributionRuleRepo.deleteAll()
    jdbcDistributionCpyRepo.deleteAll()
  }

  def unAssignRule(ruleCode : String): Future[Int] = {
    jdbcDistributionRuleRepo.deleteDistributionRulesByRuleCode(ruleCode)
  }

}
