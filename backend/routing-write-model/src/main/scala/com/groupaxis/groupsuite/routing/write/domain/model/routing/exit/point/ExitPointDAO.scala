package com.groupaxis.groupsuite.routing.write.domain.model.routing.exit.point

import com.groupaxis.groupsuite.persistence.driver.DBDriver

trait ExitPointDAO extends DBDriver {

  // Import the query language features from the driver
  import driver.api._

  protected class ExitPoints(tag: Tag) extends Table[ExitPoint](tag, "sbs_exitpoint") {
    def name = column[String]("identifiername", O.PrimaryKey)

    def queueType = column[String]("queuetype", O.PrimaryKey)

    def env = column[String]("env", O.PrimaryKey)

    def version = column[String]("version", O.PrimaryKey)

    def queueThreshold = column[Option[String]]("queuethreshold")

    def messagePartner = column[Option[String]]("messagepartner")

    def rulesVisible = column[Option[String]]("rulesvisible")

    def rulesModifiable = column[Option[String]]("rulesmodifiable")

    private type ExitPointTupleType = (String, String, String, String, Option[String], Option[String], Option[String], Option[String])

    private val exitPointShapedValue = (
      name,
      queueType,
      env,
      version,
      queueThreshold,
      messagePartner,
      rulesVisible,
      rulesModifiable
      ).shaped[ExitPointTupleType]

    def toOptionLong(value: Option[String]) = try value.map(_.toLong) catch {
      case e: Exception => None
    }

    def toOptionBoolean(value: Option[String]) = try value.map(_.toBoolean) catch {
      case e: Exception => None
    }

    private val toExitPointRow: (ExitPointTupleType => ExitPoint) = exitPointTuple =>
      ExitPoint(exitPointTuple._1, exitPointTuple._2, toOptionLong(exitPointTuple._5), exitPointTuple._6, toOptionBoolean(exitPointTuple._7), toOptionBoolean(exitPointTuple._8))

    private val toExitPointTuple: (ExitPoint => Option[ExitPointTupleType]) = { exitPointRow =>
      Some((exitPointRow.name, exitPointRow.queueType, "UNKNOWN", "UNKNOWN", exitPointRow.queueThreshold.map(_.toString), exitPointRow.messagePartner
        , exitPointRow.rulesVisible.map(_.toString), exitPointRow.rulesModifiable.map(_.toString)))
    }

    def * = exitPointShapedValue <> (toExitPointRow, toExitPointTuple)

  }

  val exitPoints = TableQuery[ExitPoints]
}

object ExitPointDAO extends ExitPointDAO {
  def apply = ExitPointDAO
}