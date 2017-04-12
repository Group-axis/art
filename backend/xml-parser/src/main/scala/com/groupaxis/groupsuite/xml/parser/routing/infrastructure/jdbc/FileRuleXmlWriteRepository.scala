package com.groupaxis.groupsuite.xml.parser.routing.infrastructure.jdbc

import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar

import akka.actor.{Actor, Props}
import akka.http.javadsl.model.Multipart.BodyPart
import akka.http.scaladsl.model.Multipart
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.FileIO
import akka.util.Timeout
import com.groupaxis.groupsuite.routing.infrastructor.es.ESRuleWriteRepository
import com.groupaxis.groupsuite.routing.infrastructor.jdbc.JdbcRuleWriteRepository
import com.groupaxis.groupsuite.routing.write.domain.model.routing.exit.point.ExitPoint
import com.groupaxis.groupsuite.routing.write.domain.model.routing.message.partner.MessagePartner
import com.groupaxis.groupsuite.routing.write.domain.model.routing.point.Point
import com.groupaxis.groupsuite.routing.write.domain.model.routing.rule.RuleEntity
import com.groupaxis.groupsuite.routing.write.domain.model.routing.schema.Schema
import com.groupaxis.groupsuite.routing.write.domain.{ImportSAARouting, SAARoutingESImported, SAARoutingImported}
import com.groupaxis.groupsuite.xml.parser.amh.model.Messages.{XmlFileImported, _}
import com.groupaxis.groupsuite.xml.parser.routing.infrastructure.util.XmlHelper
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import com.typesafe.config.Config
import org.apache.logging.log4j.scala.Logging
import org.elasticsearch.common.settings.Settings

import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}
import scala.xml.{Elem, XML}
import scalaz.Kleisli.{apply => _}

object FileRuleXmlWriteRepository {

  final val Name = "file-routing-xml-write-repository"

  def props(jdbcRuleRepo: JdbcRuleWriteRepository):
  Props = Props(classOf[FileRuleXmlWriteRepository], jdbcRuleRepo)
}

class FileRuleXmlWriteRepository(val ruleRepo: JdbcRuleWriteRepository) extends Actor with Logging {

  import scala.concurrent.duration._

  implicit val ec = context.dispatcher
  implicit val materializer = ActorMaterializer()
  val config: Config = context.system.settings.config
  val start = Calendar.getInstance.getTime.getTime
  //TODO: Move this to a ES connection pool
  logger.info(" es cluster name " + config.getString("elastic.cluster.name"))
  logger.info(" es URL " + config.getString("elastic.url"))
  val settings = Settings.settingsBuilder().put("cluster.name", config.getString("elastic.cluster.name")).build()
  val client: ElasticClient = ElasticClient.transport(settings, ElasticsearchClientUri(s"elasticsearch://" + config.getString("elastic.url")))
  implicit val esCallTimeout: Timeout = Timeout(3.minutes)

  val esRuleWriteRepository = context.actorOf(ESRuleWriteRepository.props(client), ESRuleWriteRepository.Name)

  def importSAARoutingES(dbImportResponse : Either[String, SAARoutingImported]) : Future[Either[String, SAARoutingESImported]]=
      dbImportResponse match {
        case Left(error) => Future { Left(error) }
        case Right(imported) =>
          (esRuleWriteRepository ? imported.toImportSAARoutingES).mapTo[Either[String, SAARoutingESImported]]
      }


  def importSAARouting(root: Elem): Future[Either[String, SAARoutingImported]] = {
         val points: Seq[Point] = XmlHelper.getRoutingPoints(root \\ "RoutingRuleData" \ "RoutingPointRules")
         val messagePartners: Seq[MessagePartner] = XmlHelper.getMessagePartners(root \\ "MessagePartnerData" \ "MessagePartner")
         val exitPoints: Seq[ExitPoint] = XmlHelper.getExitPoints(root \\ "ExitPointData" \ "ExitPoint")
          val schemas: Seq[Schema] = XmlHelper.getSchemas(root \\ "RoutingSchemaData" \ "RoutingSchema")
         logger.debug("about to process xml elements.")
         ruleRepo.doImport(ImportSAARouting(points, exitPoints, messagePartners, schemas))
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
    createDirectory(tmpBackendDirectory)

    Right(XmlFileExported("Success"))
  }

  def exportRules(filePath: String): Either[String, XmlFileExported] = {
    val resp = ruleRepo.getAllRules
    resp.fold(
      ex => {
        logger.warn("Error while exporting rules " + ex)
        Left("Error while exporting rules " + ex)
      },
      allRules => {
        val pointMap: Map[String, Seq[RuleEntity]] = allRules.rules.groupBy(rule => rule.routingPointName)
        val points: Iterable[Option[Point]] =
          pointMap.map(kv => {
            val ruleOption = kv._2.headOption
            if (ruleOption.isDefined) {
              val rule = ruleOption.get
              Some(Point(rule.routingPointName, rule.full.exists(v => v.toBoolean), kv._2))
            } else {
              None
            }
          })

        XML.save(filePath, XmlHelper.toXmlPoints(points), "UTF-8", xmlDecl = true)
        Right(XmlFileExported("Success"))
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
        val tempFile: File = File.createTempFile("import_", ".xml")
        file.entity.dataBytes.runWith(FileIO.toFile(tempFile)).map(_ => file.name -> tempFile.getAbsolutePath)

      case data: BodyPart => data.toStrict(20.seconds).map(strict => data.name -> strict.entity.data.utf8String)

    }.runFold(Map.empty[String, String])((map, tuple) => map + tuple)
  }

  //////////////////////////   receive         /////////////////////////////////////////////////////
  def receive: Receive = {
    case fileData: Multipart.FormData =>

      logger.info(s" receiving import msg on FileRuleXmlWriteRepository")
      val formData: Map[String, String] = Await.result(createTempFile(fileData), 50.seconds)
      val filePath = formData.get("file")
      logger.info(s" temp file $filePath created ")
      val originalSender = sender()
      filePath match {
        case Some(path) =>
          val tryRoot: Try[Elem]= Try(xml.XML.load("file:///" + filePath.get))

          val importSaaResponse = tryRoot match {
            case Success(root) =>
              logger.debug(s" Multipart.FormData parser O.K.")
              logger.debug(s" temp file $filePath processed ")
              for {
                  dbResponse <- Try(importSAARouting(root)) match {case Success(f)=>f; case Failure(e)=>Future.failed(e)}
                  esResponse <- Try(importSAARoutingES(dbResponse)) match {case Success(f)=>f; case Failure(e)=>Future.failed(e)}
              } yield esResponse match {
                case Left(e) => Left(e)
                case Right(imported) => Right(XmlFileImported("Success"))
              }
            case Failure(e)=>
              logger.error(s" Multipart.FormData parser error ${e.getMessage}")
              Future { Left(e.getMessage) }
          }

          importSaaResponse.onComplete {
            case Success(either) =>
              logger.debug(s"Multipart.FormData success $either")
              originalSender ! either

            case Failure(error) =>
              logger.error(s"ImportSaaResponse with error ${error.getMessage}")
              logger.error(" Trace: \n "+error.getStackTrace.foldLeft[String]("")((acc,value) => acc+value.toString+"\n"))
              originalSender ! Left(error.getMessage)
          }
        case None =>
          logger.error(s"$filePath Not processed !!")
          originalSender ! Left(s" An error has occurred while processing $filePath file")
      }


    case ExportXmlFile(filePath, fileName, env, version) =>
      val workDirectory = config.getString("sibes.export.dir")
      val tmpFile = fileName + "_" + currentDate + ".xml"
      val tmpFilePath = workDirectory + "/" + tmpFile
      logger.info(s" receiving export msg with ($tmpFilePath) on FileRuleXmlWriteRepository")
      val ruleResp: Either[String, XmlFileExported] = exportRules(tmpFilePath)
      ruleResp.fold(
        ex => sender() ! ruleResp,
        file => sender() ! Right(XmlFileExported(tmpFile))
      )
      logger.info(s" Exported ($tmpFilePath / $tmpFile) done. ")

    case msg => logger.error(s"message type not processed by FileRuleXmlWriteRepository $msg")
  }

}