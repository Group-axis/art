package com.groupaxis.groupsuite.simulator.application.services

import com.groupaxis.groupsuite.simulator.infrastructor.jdbc.JdbcJobWriteRepository
import com.groupaxis.groupsuite.simulator.write.domain.model.job.{JobEntity, JobEntityUpdate}
import com.groupaxis.groupsuite.synchronizator.date.GPDateHelper
import com.groupaxis.groupsuite.synchronizator.file.GPFileHelper
import com.typesafe.config.Config
import org.apache.logging.log4j.scala.Logging

import scala.util.{Failure, Success, Try}

class BatchTask(val conf: Config, val jobRepo: JdbcJobWriteRepository) extends Runnable with Logging {

  val amhSimulatorJarPath = conf.getString("batch.simulator.jar.path")
  val javaHomePath = conf.getString("batch.java.home.path")
  val mailHost = conf.getString("batch.mail.host")
  val mailPort = conf.getInt("batch.mail.port")
  val mailLogin = conf.getString("batch.mail.login")
  val mailPassword = conf.getString("batch.mail.password")
  val mailFrom = conf.getString("batch.mail.from")

  private def sendMail(filePath : String, toAddress : String): Unit = {
    import com.groupaxis.groupsuite.synchronizator.courier._, com.groupaxis.groupsuite.synchronizator.courier.Defaults._
    val mailer = Mailer(mailHost, mailPort)
      .auth(true)
      .as(mailLogin, mailPassword)
      .startTtls(true)()
    mailer(Envelope.from(mailFrom.addr)
      .to(toAddress.addr)
      .subject("ART ROUTING SIMULATION")
      .content(Multipart()
        .attach(new java.io.File(filePath))
        .html("<html><body><h2>Hello</h2><br><br><h2>Please find attached the result of your last routing simulation on ART application<h2> <br><br><h2>Best Regards<h2> </body></html>")))
      .onComplete {
        case Success(_) => logger.info("delivered report")
        case Failure(ex) => logger.error(ex.getMessage)
      }

  }
  private def findJobByStatus(status: Seq[Int]): Either[String, Option[JobEntity]] = jobRepo.findJobByStatus(status)

  private def updateJob(jobId: Int, updatedJob: JobEntityUpdate) = jobRepo.updateJob(jobId, updatedJob)

  private def createUpdateJob(status: Int, comment: String, fromSystem: Int, output: Option[String] = None) =
    JobEntityUpdate(None, None, None, None,
      Some(status), None, None, Some(comment),
      None, output, fromSystem)

  private def extractEmail(params : Option[String]) : Option[String] = {
    params.map(m=>m.substring(m.indexOf("SEND_MAIL=")+"SEND_MAIL=".length))
  }
  private def executeSimulator(commandToExecute: String): Either[String, String] = {
    import scala.sys.process._
    logger.info(s"Running batch task: $commandToExecute")

    val exec: Try[String] = Try(Process(commandToExecute, Some(new java.io.File(amhSimulatorJarPath))).!!)

    exec match {
      case Success(resp) => logger.info(resp); Right(resp)
      case Failure(t) => logger.error(t.getMessage); Left(t.getMessage)
    }
  }

  override def run(): Unit = {

    findJobByStatus(Seq(1, 6)) match {
      case Left(error) =>
        logger.error(error)
      case Right(None) =>
        logger.info("No batch in [1] pending status found")
      case Right(Some(jobEntity)) =>
        updateJob(jobEntity.id, createUpdateJob(2, "In progress", jobEntity.jobLauncher))
        match {
          case Right(_) =>
            val fileName = s"job_${GPDateHelper.currentDate}.csv"
            val command = if (jobEntity.jobLauncher == 1) "-saajob" else "-job"
            val commandToExecute: String = s"$javaHomePath/bin/java -jar AMHSimulator.jar $command ${jobEntity.id} $fileName"
            executeSimulator(commandToExecute) match {
              case Right(_) =>
                updateJob(jobEntity.id, createUpdateJob(3, "O.K.", jobEntity.jobLauncher))
                match {
                  case Right(_) =>
                    val filePath = amhSimulatorJarPath + "/" + fileName
                    val fileContent = GPFileHelper.readFile(filePath)
                    updateJob(jobEntity.id, createUpdateJob(3, "O.K.", jobEntity.jobLauncher, fileContent))
                    match {
                      case Right(_) =>
                        logger.info(s"Batch for job ${jobEntity.id} was successfully finished.")
                        extractEmail(jobEntity.params)
                            .map(m=>{logger.info(s"email defined: $m");m})
                            .filter(_.length>0)
                          .foreach(email=>sendMail(filePath,email)
                        )

                      case Left(error) =>
                        logger.error(error)
                    }
                  case Left(error) =>
                    logger.error(error)
                }
              case Left(error) =>
                logger.error(error)
                updateJob(jobEntity.id, createUpdateJob(4, "An Error has occurred", jobEntity.jobLauncher))
            }
          case Left(error) =>
            logger.error(error)
        }
    }
  }

  //override
  def run2(): Unit = {
    val process = for {
      jobEntity <- findJobByStatus(Seq(1, 6)).right
    //      toInProgress <- if (jobEntity.isDefined) setStatus(jobEntity.get.id, 2, "In progress").right
    //      else Left("")

    } yield ()
  }
}

object BatchTask {
  def apply(conf: Config, jobRepo: JdbcJobWriteRepository) = new BatchTask(conf, jobRepo)
}
