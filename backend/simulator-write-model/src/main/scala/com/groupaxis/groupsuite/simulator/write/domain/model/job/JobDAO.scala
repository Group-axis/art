package com.groupaxis.groupsuite.simulator.write.domain.model.job

import java.sql.Timestamp
import com.groupaxis.groupsuite.persistence.driver.DBDriver
import org.joda.time.DateTime

trait JobDAO extends DBDriver {

  // Import the query language features from the driver
  import driver.api._

  implicit def mapDate = MappedColumnType.base[Option[DateTime], Timestamp](
    d => new Timestamp(d.map(_.getMillis).getOrElse(new DateTime().getMillis)),
    time => Some(new DateTime(time.getTime))
  )

  protected class Jobs(tag: Tag) extends Table[JobEntity](tag, "sml_jobs") {

    def id = column[Int]("id", O.AutoInc, O.PrimaryKey)

    def user = column[Option[String]]("id_user")

    def creationDate = column[Option[DateTime]]("date_creation")

    def startDate = column[Option[DateTime]]("date_start")

    def endDate = column[Option[DateTime]]("date_end")

    def status = column[Option[Int]]("id_status")

    def numOfMessages = column[Option[Int]]("no_of_messages")

    def fileName = column[Option[String]]("file_name")

    def comment = column[Option[String]]("comment")

    def params = column[Option[String]]("params")

    def output = column[Option[String]]("file_content")

    def jobLauncher = column[Int]("job_launcher_system")

    private type JobEntityTupleType = (Int, Option[String], Option[DateTime], Option[DateTime], Option[DateTime],
      Option[Int], Option[Int], Option[String], Option[String], Option[String], Option[String], Int)

    private val jobShapedValue = (id,
      user, creationDate, startDate, endDate,
      status, numOfMessages, fileName, comment, params, output, jobLauncher
      ).shaped[JobEntityTupleType]

    private val toJobRow: (JobEntityTupleType => JobEntity) = { jobTuple => {
      JobEntity(jobTuple._1, jobTuple._2, jobTuple._3, jobTuple._4, jobTuple._5, jobTuple._6, jobTuple._7, jobTuple._8, jobTuple._9, jobTuple._10, jobTuple._11, jobTuple._12)
    }
    }

    private val toJobTuple: (JobEntity => Option[JobEntityTupleType]) = { jobRow =>
      Some((jobRow.id, jobRow.user, jobRow.creationDate, jobRow.startDate, jobRow.endDate, jobRow.status, jobRow.numOfMessages, jobRow.fileName, jobRow.comment, jobRow.params, jobRow.output, jobRow.jobLauncher))
    }

    def * = jobShapedValue <> (toJobRow, toJobTuple)

  }

  val jobs: TableQuery[Jobs] = TableQuery[Jobs]

}

object JobDAO extends JobDAO {
  def apply = JobDAO
}