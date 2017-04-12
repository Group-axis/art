package com.groupaxis.groupsuite.amh.routing.infrastructor.file

import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar

import akka.actor.SupervisorStrategy.{Restart, Resume, Stop}
import akka.actor.{Actor, OneForOneStrategy, Props}
import akka.http.javadsl.model.Multipart.BodyPart
import akka.http.scaladsl.model.Multipart
import akka.stream.scaladsl.FileIO
import akka.stream.{ActorMaterializer, Supervision}
import com.groupaxis.groupsuite.amh.routing.infrastructor.es.ESRuleWriteRepository
import com.groupaxis.groupsuite.amh.routing.interfaces.http.client.HttpAuditRoutingClient
import com.groupaxis.groupsuite.commons.persistence.jdbc.DatabaseService
import com.groupaxis.groupsuite.routing.write.domain.audit.messages.AMHRoutingAuditMessages.{CreateExport, CreateImport}
import com.groupaxis.groupsuite.routing.write.domain.model.amh.assignment.{AMHAssignmentEntity, AMHAssignmentRuleEntity}
import com.groupaxis.groupsuite.routing.write.domain.model.amh.rule.AMHRuleMessages._
import com.groupaxis.groupsuite.routing.write.domain.model.amh.rule.{AMHRule, AMHRuleEntity}
import com.groupaxis.groupsuite.xml.parser.amh.model.Messages.{XmlFileImported, _}
import com.groupaxis.groupsuite.xml.parser.amh.reader.AMHParser
import com.groupaxis.groupsuite.xml.parser.amh.writer.Zipper
import com.groupaxis.groupsuite.xml.parser.amh.writer.es.{ESAMHAssignmentRepository, ESAMHBackendRepository, ESAMHRuleRepository}
import com.groupaxis.groupsuite.xml.parser.amh.writer.jdbc._
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import com.typesafe.config.Config
import org.apache.logging.log4j.scala.Logging
import org.elasticsearch.common.settings.Settings
import org.joda.time.DateTime

import scala.collection.immutable.HashMap
import scala.collection.mutable.ListBuffer
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}
import scala.xml.{Elem, XML}

object forExa extends App with Logging {
  val nEL = Some(List(1, 2, 2, 3))
  val eL = Some(List())

  val r = for {
    a <- nEL
    if a.nonEmpty
    b <- {
      logger.debug("in b execution!! ")
      a.foreach { n => logger.debug(s" using $n") }
      Some(List(5, 5, 5))
    }
  } yield b

  logger.debug(s"r=$r")
}

object FileAMHXmlWriteRepository {

  final val Name = "file-amh-xml-write-repository"

  def props(jdbcRuleRepo: JdbcAMHRuleRepository, jdbcBackendRepo: JdbcAMHBackendRepository, jdbcAssignmentRepo: JdbcAMHAssignmentRepository, jdbcAssignmentRuleRepo: JdbcAMHAssignmentRuleRepository,
            distributionCpyRepo: FileDistributionCpyWriteRepository, feedbackCpyRepo: FileFeedbackDtnCpyWriteRepository, databaseService: DatabaseService, restClient: HttpAuditRoutingClient):
  Props = Props(classOf[FileAMHXmlWriteRepository], jdbcRuleRepo, jdbcBackendRepo, jdbcAssignmentRepo, jdbcAssignmentRuleRepo, distributionCpyRepo, feedbackCpyRepo, databaseService, restClient)
}

class FileAMHXmlWriteRepository(val jdbcRuleRepo: JdbcAMHRuleRepository, val jdbcBackendRepo: JdbcAMHBackendRepository, val jdbcAssignmentRepo: JdbcAMHAssignmentRepository, val jdbcAssignmentRuleRepo: JdbcAMHAssignmentRuleRepository,
                                val distributionCpyRepo: FileDistributionCpyWriteRepository, val feedbackCpyRepo: FileFeedbackDtnCpyWriteRepository, val databaseService: DatabaseService, val restClient: HttpAuditRoutingClient) extends Actor with Logging {
  //  import databaseService._
  //  import databaseService.driver.api._
  import AMHParser._

  import scala.concurrent.duration._

  implicit val ec = context.dispatcher
  implicit val materializer = ActorMaterializer()

  val config: Config = context.system.settings.config
  val start = Calendar.getInstance.getTime.getTime

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1.minute) {
      case _: ArithmeticException => Resume
      case _: NullPointerException => Restart
      case _: IllegalArgumentException => Stop
      case e: Exception =>
        logger.warn("[FileAMHXmlWriteRepository] Exception has been received, so restarting the actor " + e.getMessage)
        e.printStackTrace()
        Restart
    }

  val decider: Supervision.Decider = (ex) => Supervision.Resume

  val settings = Settings.settingsBuilder().put("cluster.name", config.getString("elastic.cluster.name")).build()
  val client: ElasticClient = ElasticClient.transport(settings, ElasticsearchClientUri(s"elasticsearch://" + config.getString("elastic.url")))

  val esRuleWriteRepo = context.actorOf(ESRuleWriteRepository.props(client), ESRuleWriteRepository.Name)

  //val backupService = new BackupServiceImpl(ec, database)
  private def importAssignments(env: Option[String], version: Option[String], fromFile: String, username : String, rules: AMHRulesCreated, client: ElasticClient): Try[Seq[AMHAssignmentRuleEntity]] = {
    val elements: Seq[Elem] = unZip(fromFile, "Gateway.BackendChannelAssignmentSelectionTable")

    logger.debug(s" -------------  ${elements.size} ASSIGNMENTS from file $fromFile ------------")
    if (elements.isEmpty) {
      return Success(Seq())
    }

    val toAssignment = toAMHAssignment(env.getOrElse("UNKNOWN"), "DEFAULT", username) _
    val tupled: Seq[(AMHAssignmentEntity, Seq[AMHAssignmentRuleEntity])] = elements.map(toAssignment)

    val assignResp = this.jdbcAssignmentRepo.createAssignments(tupled.map(_._1))
    Try(assignResp.fold(
      ex => {
        logger.error(s"Assignment insert failed with $ex")
        throw new Exception(s"Assignment Insert failed with $ex")
      },
      assignmentCreated => {
        logger.info("Assignments successfully created")
        val assignedRules: Seq[AMHAssignmentRuleEntity] = tupled.filter(_._2.nonEmpty).flatMap(_._2)
        val assignRuleResp = this.jdbcAssignmentRuleRepo.createAssignmentRules(assignedRules)

        assignRuleResp.fold(
          ex => {
            logger.error(s"Rules assignment insert failed with $ex")
            throw new Exception(s"Rules assignment insert failed with $ex")
          },

          v => {
            logger.info("Insert assignment rules success")
            val esAssignmentRepo: ESAMHAssignmentRepository = new ESAMHAssignmentRepository(client)
            val esRuleRepo: ESAMHRuleRepository = new ESAMHRuleRepository(client)
            esAssignmentRepo.insert(tupled.map(t => {
              t._1.toES(t._2.map(r => r.toES(esRuleRepo.findExpressionByCode(r.ruleCode))))
            }))
            //TODO: send message to update rule assigned actor
            assignedRules
          }
        )
      }))
  }

  def getRuleExpression(rules: Seq[AMHRuleEntity])(code: String): String = {
    logger.debug("Looking for code " + code + " in " + rules)
    val filtered = rules.filter(_.code.equalsIgnoreCase(code))
    if (filtered.nonEmpty) {
//      logger.debug("found !! " + filtered.head.expression.getOrElse("") + " |")
      filtered.head.expression.getOrElse("")
    } else {
      logger.warn("NOT FOUND :<")
      ""
    }
  }

  private def importRules(env: Option[String], version: Option[String], fromFile: String, username: String, client: ElasticClient): Try[AMHRulesCreated] = {
    val d: Seq[Elem] = unZip(fromFile, "Gateway.RuleCriteria")

    logger.debug(s" -------------  ${d.size} RULES ------------")
    val toRule = toAMHRule(env.getOrElse("UNKNOWN"), version.getOrElse("DEFAULT"), username) _
    val ruleResp = this.jdbcRuleRepo.createRules(d.map(toRule))

    Try(ruleResp.fold(
      ex => {
        logger.error(s"Import rules failed : $ex")
        throw new Exception(s"Import rules failed : $ex" )
      },
      v => {
        if (v.rules.nonEmpty) {
          logger.debug("Import rules Success")
          val esRuleRepo: ESAMHRuleRepository = new ESAMHRuleRepository(client)
          esRuleRepo.insert(v.rules.map(t => t.toES(Some(false))))
        } else {
          logger.debug("No rules to import!")
        }
        v
      }
    ))
  }

  private def initializeDB = {
    val deleteDistribution = Future {
      distributionCpyRepo.initialize()
    }
    val deleteFeedback = Future {
      feedbackCpyRepo.initialize()
    }
    val deleteAssigRules = Future {
      jdbcAssignmentRuleRepo.deleteAll()
    }

    val deleteAssociations = for {
      a <- deleteDistribution
      b <- deleteFeedback
      c <- deleteAssigRules
      d <- Future {
        jdbcAssignmentRepo.deleteAll()
      }
    } yield d

    Await.result(deleteAssociations, 20.seconds)
    jdbcBackendRepo.deleteAll()
    jdbcRuleRepo.deleteAll()
  }

  //TODO: handle rollback from slick, refactor to one single transaction
  private def rollback(client: ElasticClient) = {
    logger.debug("Rollback in progress ...")
    initializeDB
    val esBackendRepo: ESAMHBackendRepository = new ESAMHBackendRepository(client)
    esBackendRepo.initializeIndex("amhrouting")
  }

  private def importBackends(env: Option[String], version: Option[String], fromFile: String, username: String, client: ElasticClient): Try[XmlFileImported] = {
    val t: Seq[Elem] = unZip(fromFile, "AMHWizard.BackendConfiguration")
    logger.debug(s" -------------   ${t.size} BACKEND ------------")

    initializeDB

    val toBackend = toAMHBackend(env.getOrElse("UNKNOWN"), version.getOrElse("DEFAULT")) _
    val resp = jdbcBackendRepo.createBackends(t.map(toBackend))
    Try(resp.fold(
      ex => {
        logger.error(s"Backend import failed: $ex" )
        throw new Exception(s"Backend import failed: $ex")
      },
      v => {
        logger.debug(s"Backend created produced value: $v")
        val esBackendRepo: ESAMHBackendRepository = new ESAMHBackendRepository(client)
        esBackendRepo.initializeIndex("amhrouting")
        logger.debug("Index \"amhrouting\" created, the backends will be inserted into ES.")
        esBackendRepo.insert(v.rules.map(t => t.toES))
        XmlFileImported("Success")
      }
    ))
  }

  //////////////////////////   exports methods /////////////////////////////////////////////////////
  def createDirectory(pathStr: String): Unit = {
    import scalax.file.Path
    val path: Path = Path.fromString(pathStr)
    path.createDirectory(failIfExists = false)
  }

  def exportBackends(filePath: String): Either[String, XmlFileExported] = {
    //c:/dev/DFS/AMH/tmp
    val tmpBackendDirectory = filePath + "/AMHWizard.BackendConfiguration"

    val processedList = for {
      _ <- Some(createDirectory(tmpBackendDirectory))
      backs <- jdbcBackendRepo.findAllBackends
      if backs.nonEmpty
      exportedList <- {
        backs.foreach {
          backend => {
            XML.save(tmpBackendDirectory + "/export_" + backend.code.toUpperCase + ".xml", XmlHelper.toXmlBackend(backend), "UTF-8", true)
          }
        }
        Some(backs)
      }
    } yield exportedList

    if (processedList.isEmpty) logger.warn("No backends to export")

    Right(XmlFileExported("Success"))
  }

  def exportRules(filePath: String): Either[String, XmlFileExported] = {
    val tmpRuleDirectory = filePath + "/Gateway.RuleCriteria"
    createDirectory(tmpRuleDirectory)
    val allRules: Option[Seq[AMHRuleEntity]] = jdbcRuleRepo.findAllNonDeletedRules

    if (allRules.isEmpty) {
      logger.warn("There is no rules to export")
      return Right(XmlFileExported("Success"))
    }

    allRules.foreach(rules => {
      rules.foreach {
        rule => {
          XML.save(tmpRuleDirectory + "/export_" + rule.code.toUpperCase + ".xml", XmlHelper.toXmlRule(rule), "UTF-8", true)
        }
      }
    })
    Right(XmlFileExported("Success"))
  }

  def exportDeletedRules(filePath: String): Either[String, XmlFileExported] = {

    val tmpRuleDirectory = filePath + "/Gateway.RuleCriteria"
    createDirectory(tmpRuleDirectory)
    val allRules: Option[Seq[AMHRuleEntity]] = jdbcRuleRepo.findAllDeletedRules

    if (allRules.isEmpty) {
      logger.warn("There is no deleted rules to export")
      return Right(XmlFileExported("Success"))
    }

    allRules.foreach(rules => {
      rules.foreach {
        rule => {
          def uuid = java.util.UUID.randomUUID.toString
          XML.save(tmpRuleDirectory + "/export_deleted_" + rule.originalCode.getOrElse(uuid).toUpperCase + ".xml", XmlHelper.toXmlDeletedRule(rule), "UTF-8", xmlDecl = true)
        }
      }
    })
    Right(XmlFileExported("Success"))
  }

  def exportAssignments(filePath: String): Either[String, XmlFileExported] = {
    val tmpAssignmentsDirectory = filePath + "/Gateway.BackendChannelAssignmentSelectionTable"
    createDirectory(tmpAssignmentsDirectory)

    val allAssignments = jdbcAssignmentRepo.findAllAssignments
    val allAssignmentRules = jdbcAssignmentRuleRepo.findAllAssignmentRules
    val ruleMap: Map[String, Seq[AMHAssignmentRuleEntity]] = allAssignmentRules.map(rules => rules.groupBy(_.code)).getOrElse(new HashMap())

    if (allAssignments.isEmpty) {
      logger.warn("There is no assignments to export")
      return Right(XmlFileExported("Success"))
    }

    allAssignments.foreach(assignments => {
      assignments.foreach {
        assignment => {
          XML.save(tmpAssignmentsDirectory + "/export_" + assignment.code.toUpperCase + ".xml", XmlHelper.toAssignmentXml(assignment, ruleMap.get(assignment.code.toUpperCase)), "UTF-8", true)
        }
      }
    })

    Right(XmlFileExported("Success"))
  }

  def zipExportedFiles(filePath: String, outputFileFolder: String, outputFileName: String): Either[String, XmlFileExported] = {
    //    val path = "c:/dev/DFS/AMH/tmp"
    //    val outputFilename = "c:/dev/DFS/AMH/tmp/output.zip"
    val file = new File(filePath)
    logger.debug("creating file list from " + filePath)
    val filePaths = Zipper.createFileList(file, outputFileFolder + outputFileName)
    logger.debug("creating zip file " + outputFileFolder + outputFileName)
    val resp = Zipper.createZip(filePaths, outputFileFolder + outputFileName, filePath)
    resp.fold(
      errorMsg => {
        logger.error("an error has occurred while zipping " + errorMsg)
        Left(errorMsg)
      },
      msgOk => {
        logger.debug("zipping done")
        Right(XmlFileExported(outputFileName))
      }
    )
  }

  private def currentDate: String = {
    val today = Calendar.getInstance().getTime
    val minuteFormat = new SimpleDateFormat("yyyyMMddHHmmss")
    minuteFormat.format(today)
  }

  private def createTempFile(formData: Multipart.FormData): Future[Map[String, String]] = {
    formData.parts.mapAsync[(String, String)](1) {

      case file: BodyPart if file.name == "file" =>
        //TODO: use import folder instead of temp folder ?? to do it pass 3th parameter to createTempFile("","","/toto/xxx/importFolder")
        val tempFile: File = File.createTempFile("import_", ".zip")
        file.entity.dataBytes.runWith(FileIO.toFile(tempFile)).map(_ => file.name -> tempFile.getAbsolutePath)

      case data: BodyPart => data.toStrict(2.seconds).map(strict => data.name -> strict.entity.data.utf8String)

    }.runFold(Map.empty[String, String])((map, tuple) => map + tuple)
  }


  def executeExports(fs: Seq[String => Either[String, XmlFileExported]], filePath: String): Either[String, XmlFileExported] = {

    def looping(functions: Seq[String => Either[String, XmlFileExported]]): Either[String, XmlFileExported] = functions match {
      case Nil => Left("empty execution List!!")
      case head :: tail =>
        val tmpResp = head(filePath)
        if (tmpResp.isLeft)
          tmpResp
        else if (tail.isEmpty)
          tmpResp
        else
          looping(tail)
    }

    val resp = looping(fs)
    resp

  }

  def executeDoExports(exportsResp: Either[String, XmlFileExported], fs: Seq[(Option[String], Option[String], String) => Either[String, XmlFileExported]], filePath: String): Either[String, XmlFileExported] = {

    def looping(functions: Seq[(Option[String], Option[String], String) => Either[String, XmlFileExported]]): Either[String, XmlFileExported] = functions match {
      case Nil => Left("empty doExecution List!!")
      case head :: tail =>
        val tmpResp = head(None, None, filePath)
        if (tmpResp.isLeft)
          tmpResp
        else if (tail.isEmpty)
          tmpResp
        else
          looping(tail)
    }

    exportsResp.fold(
      ex => return Left(ex),
      success => looping(fs)
    )

  }

  def filePaths(fileName: String): (String, String, String) = {
    val workDirectory = config.getString("amh.export.dir")
    val tmpFile = fileName + "_" + currentDate + ".zip"
    val tmpFilePath = workDirectory + "/" + currentDate
    (workDirectory, tmpFile, tmpFilePath)
  }

  def importFile(filePath : Option[String],username : String, dateTime:DateTime, version : Option[String] = None,env : Option[String] = None) : Try[Seq[AMHRule]] = {
    filePath.map(filePath => {
      logger.info(s" receiving import msg with ($filePath) on FileAMHXmlWriteRepository")
      val unzippedFile: scala.collection.mutable.HashMap[String, ListBuffer[Elem]] = unZipAll(filePath, List("AMHWizard.BackendConfiguration", "Gateway.BackendChannelAssignmentSelectionTable",
           "Gateway.DistributionCopySelectionTable", "Gateway.FeedbackDistributionCopySelectionTable", "Gateway.RuleCriteria"))

      for {
        backendResp <- importBackends(env, version, filePath, username, client)
        ruleResp <- importRules(env, version, filePath, username, client)
        waitForRules <- Try(Await.result(Future { Thread.sleep(2000) }, 5.seconds))
        importedAssignmentRules <- importAssignments(env, version, filePath, username, ruleResp, client)
        importedDistRules <- distributionCpyRepo.doImport(env, version, ruleResp, username, unzippedFile.get("Gateway.DistributionCopySelectionTable"), client)
        importedFeedbackRules <- feedbackCpyRepo.doImport(env, version, ruleResp, username, unzippedFile.get("Gateway.FeedbackDistributionCopySelectionTable"), client)
      } yield {
        (importedDistRules ++ importedAssignmentRules ++ importedFeedbackRules).distinct
      }
    }).getOrElse(Failure(new Exception("Something weird happen")))

//      val backendResponse: Try[XmlFileImported] = importBackends(env, version, filePath, client)
//
//      backendResponse.fold(
//        ex =>  Left(ex) ,
//        success => {
//          val ruleResp = importRules(env, version, filePath, client)
//          logger.info("waiting 2 seconds  for ES to be ready")
//          Await.result(Future { Thread.sleep(2000) }, 5.seconds)
//          ruleResp.fold(
//            ex => Left(ex),
//            rulesCreated => {
//              importAssignments(env, version, filePath, rulesCreated, client)
//              .fold(
//                ex => Left(ex) ,
//                importedAssignmentRules => {
//                  distributionCpyRepo.doImport(env, version, rulesCreated, unzippedFile.get("Gateway.DistributionCopySelectionTable"), client)
//                  .fold(
//                    ex => {
//                      Left(ex)
//                    },
//                    importedDistRules => {
//                      feedbackCpyRepo.doImport(env, version, rulesCreated, unzippedFile.get("Gateway.FeedbackDistributionCopySelectionTable"), client)
//                      .fold(
//                        ex => {
//                          Left(ex)
//                        },
//                        importedFeedbackRules => {
//                          val r = (importedDistRules ++ importedAssignmentRules ++ importedFeedbackRules).distinct
//                          Right(r)
//                        })
//                    })
//                })
//            })
//        })
//    }).getOrElse(Left("Something weird happen"))
  }

  //////////////////////////   receive         /////////////////////////////////////////////////////
  implicit val system = context.system

  def receive: Receive = {
    case fileData: Multipart.FormData =>
      logger.info(" es cluster name " + config.getString("elastic.cluster.name"))
      logger.info(" es URL " + config.getString("elastic.url"))

      val formData: Map[String, String] = Await.result(createTempFile(fileData), 5.seconds)
      val filePath = formData.get("file")
      val env = formData.get("env")
      val version: Option[String] = formData.get("ver")
      val userId: Option[String] = formData.get("userId")
      val time: Option[String] = formData.get("time")

      val dateTime = time.map(time => {
        try {
          new DateTime(time.toLong)
        } catch {
          case e: Exception => logger.error(s" while receiving time for importing ${e.getMessage} "); DateTime.now()
        }
      }).getOrElse(DateTime.now())
      val username = userId.getOrElse("no_user_defined")

      logger.info(s" temp file ($filePath) created in env ($env) with version ($version)")

      importFile(filePath, username, dateTime, version, env) match {
        case Success(ruleCodes) =>
          sender() ! Right(XmlFileImported("Success"))
          esRuleWriteRepo ! SetRulesAsAssigned(ruleCodes)
          val auditCreateImport = CreateImport(username, dateTime, filePath.getOrElse(""), "OK")
          restClient.sendImportCreation(auditCreateImport)
          logger.info(s" temp file ($filePath) import finished ")
        case Failure(ex) =>
          logger.error(s"Error while importing : ${ex.getMessage}")
          rollback(client)
          sender() ! Left(ex.getMessage)
          val auditCreateImport = CreateImport(username, dateTime, filePath.getOrElse(""), ex.getMessage)
          restClient.sendImportCreation(auditCreateImport)
      }
//      .fold(
//        ex => {
//          logger.error(s"Error while importing : $ex")
//          rollback(client)
//          sender() ! Left(ex)
//          val auditCreateImport = CreateImport(username, dateTime, filePath.getOrElse(""), ex)
//          restClient.sendImportCreation(auditCreateImport)
//        },
//        ruleCodes => {
//          sender() ! Right(XmlFileImported("Success"))
//          esRuleWriteRepo ! SetRulesAsAssigned(ruleCodes)
//          val auditCreateImport = CreateImport(username, dateTime, filePath.getOrElse(""), "OK")
//          restClient.sendImportCreation(auditCreateImport)
//          logger.info(s" temp file ($filePath) import finished ")
//        })
    case ImportAMHXmlFile(fileName, username, importException) =>
      val filePath = config.getString("amh.export.dir") + "/" + fileName
      importFile(Some(filePath), username, DateTime.now) match {
        case Success(ruleCodes) =>
          sender() ! Left(importException)
          esRuleWriteRepo ! SetRulesAsAssigned(ruleCodes)
          logger.info(s" BACKUP: temp file ($filePath) finished ")
        case Failure(ex) =>
          logger.error(s"Error while importing backup: ${ex.getMessage}")
          sender() ! Left(ex.getMessage)
      }
//        .fold(
//          ex => {
//            logger.error(s"Error while importing backup: $ex")
//            sender() ! Left(ex)
//          },
//          ruleCodes => {
//            sender() ! Left(importException)
//            esRuleWriteRepo ! SetRulesAsAssigned(ruleCodes)
//            logger.info(s" BACKUP: temp file ($filePath) finished ")
//          })
    case ExportAMHXmlFile(filePath, fileName, username, dateTime, env, version) =>
      val (workDirectory, tmpFile, tmpFilePath) = filePaths(fileName)
      logger.info(s" receiving ExportAMHXmlFile msg with ($tmpFilePath) on FileAMHXmlWriteRepository")
      val assignExportsResp: Either[String, XmlFileExported] = executeExports(List(exportRules, exportDeletedRules, exportAssignments), tmpFilePath)
      val distributionExportsResp: Either[String, XmlFileExported] = executeDoExports(assignExportsResp, List(distributionCpyRepo.doExport, feedbackCpyRepo.doExport), tmpFilePath)
      distributionExportsResp.fold(
        ex => {
          sender() ! distributionExportsResp
          val auditCreateExport = CreateExport(username, dateTime, tmpFilePath, ex)
          restClient.sendExportCreation(auditCreateExport)
        },
        success => {
          val resp = zipExportedFiles(tmpFilePath, workDirectory + "/", tmpFile)
          sender() ! resp
          val auditCreateExport = CreateExport(username, dateTime, tmpFilePath, "OK")
          restClient.sendExportCreation(auditCreateExport)
        })
        .onComplete {
          case Success(resp) =>
            resp.fold(
              error => logger.error(s"While auditing export creation: $error"),
              done => logger.info(s" Audit export creation success")
            )
          case error => logger.error(s"While auditing export creation: $error")
        }
    case ExportAMHBackupXmlFile(fileData, fileName) =>
//      val formData: Map[String, String] = Await.result(createTempFile(fileData), 5.seconds)
//      val username = formData.getOrElse("userId", "backup")
      val workDirectory = config.getString("amh.export.dir")
      val tmpFile = fileName
      val tmpFilePath = workDirectory + "/" + currentDate

//      val (workDirectory, tmpFile, tmpFilePath) = filePaths(fileName)

      logger.info(s" receiving ExportAMHBackupXmlFile msg with ($tmpFilePath) on FileAMHXmlWriteRepository")

      val assignExportsResp: Either[String, XmlFileExported] = executeExports(List(exportBackends, exportRules, exportAssignments), tmpFilePath)
      val distributionExportsResp: Either[String, XmlFileExported] = executeDoExports(assignExportsResp, List(distributionCpyRepo.doExport, feedbackCpyRepo.doExport), tmpFilePath)

      distributionExportsResp.fold(
        ex => {
          logger.error(s"While backup export creation: $ex")
          sender() ! distributionExportsResp
        },
        success => {
          logger.info(s"Backup export about to zip...")
          sender() ! zipExportedFiles(tmpFilePath, workDirectory + "/", tmpFile)
        })
    case unknown => logger.error(s"No match for message $unknown")
  }

}