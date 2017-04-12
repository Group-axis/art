package com.groupaxis.groupsuite.audit.write.domain.model.difference

import java.sql.Timestamp

import org.joda.time.DateTime
import slick.driver.JdbcProfile


class DifferenceDAO(val driver: JdbcProfile) {

  import slick.driver.PostgresDriver.api._

  implicit def mapDate = MappedColumnType.base[DateTime, Timestamp](
    d => new Timestamp(d.getMillis),
    time => new DateTime(time.getTime)
  )

  class Differences(tagg: Tag) extends Table[DifferenceEntity](tagg, "aud_diff") {

    def id = column[Int]("id", O.AutoInc, O.PrimaryKey)

    def userId = column[String]("user_id")

    def creationDate = column[DateTime]("creation_date")

    def entityType = column[String]("entity_type")

    def entityId = column[String]("entity_id")

    private type DifferenceEntityTupleType = (Int, String, DateTime, String, String)

    private val differenceShapedValue = (id, userId, creationDate, entityType, entityId).shaped[DifferenceEntityTupleType]

    private val toMessageDifference: (DifferenceEntityTupleType => DifferenceEntity) = { differenceTuple => {
      DifferenceEntity(differenceTuple._1, differenceTuple._2, differenceTuple._3, differenceTuple._4, differenceTuple._5)
    }
    }

    private val toDifferenceTuple: (DifferenceEntity => Option[DifferenceEntityTupleType]) = { diffRow =>
      Some((diffRow.id, diffRow.user, diffRow.date, diffRow.entityType, diffRow.entityId))
    }

    def * = differenceShapedValue <> (toMessageDifference, toDifferenceTuple)

  }

  class Changes(tagg: Tag) extends Table[ChangeEntity](tagg, "aud_change") {

    def id = column[Int]("id", O.AutoInc, O.PrimaryKey)

    def diffId = column[Int]("diff_id")

    def changeType = column[String]("change_type")

    def propertyName = column[String]("property_name")

    private type ChangeEntityTupleType = (Int, Int, String, String)

    private val changeShapedValue = (id, diffId, changeType, propertyName).shaped[ChangeEntityTupleType]

    private val toMessageChange: (ChangeEntityTupleType => ChangeEntity) = { changeTuple => {
      ChangeEntity(changeTuple._1, changeTuple._2, changeTuple._3, changeTuple._4)
    }
    }

    private val toChangeTuple: (ChangeEntity => Option[ChangeEntityTupleType]) = { changeRow =>
      Some((changeRow.id, changeRow.diffId, changeRow.changeType, changeRow.propertyName))
    }

    def * = changeShapedValue <> (toMessageChange, toChangeTuple)

  }

  class ContainerChanges(tagg: Tag) extends Table[ContainerChangeEntity](tagg, "aud_container_change") {

    def id = column[Int]("id", O.AutoInc, O.PrimaryKey)

    def changeId = column[Int]("change_id")

    def value = column[String]("change_value")

    def addOrRemove = column[String]("add_or_remove")

    private type ContainerChangeEntityTupleType = (Int, Int, String, String)

    private val containerChangeShapedValue = (id, changeId, value, addOrRemove).shaped[ContainerChangeEntityTupleType]

    private val toContainerChange: (ContainerChangeEntityTupleType => ContainerChangeEntity) = { containerChangeTuple => {
      ContainerChangeEntity(containerChangeTuple._1, containerChangeTuple._2, containerChangeTuple._3, containerChangeTuple._4)
    }
    }

    private val toContainerChangeTuple: (ContainerChangeEntity => Option[ContainerChangeEntityTupleType]) = { containerChangeRow =>
      Some((containerChangeRow.id, containerChangeRow.changeId, containerChangeRow.value, containerChangeRow.addOrRemove))
    }

    def * = containerChangeShapedValue <> (toContainerChange, toContainerChangeTuple)

  }

  class ValueChanges(tagg: Tag) extends Table[ValueChangeEntity](tagg, "aud_value_change") {

    def id = column[Int]("id", O.AutoInc, O.PrimaryKey)

    def changeId = column[Int]("change_id")

    def oldValue = column[String]("old_value")

    def newValue = column[String]("new_value")

    private type ValueChangeEntityTupleType = (Int, Int, String, String)

    private val valueChangeShapedValue = (id, changeId, oldValue, newValue).shaped[ValueChangeEntityTupleType]

    private val toValueChange: (ValueChangeEntityTupleType => ValueChangeEntity) = { valueChangeTuple => {
      ValueChangeEntity(valueChangeTuple._1, valueChangeTuple._2, valueChangeTuple._3, valueChangeTuple._4)
    }
    }

    private val toValueChangeTuple: (ValueChangeEntity => Option[ValueChangeEntityTupleType]) = { valueChangeRow =>
      Some((valueChangeRow.id, valueChangeRow.changeId, valueChangeRow.oldValue, valueChangeRow.newValue))
    }

    def * = valueChangeShapedValue <> (toValueChange, toValueChangeTuple)

  }

  val differences: TableQuery[Differences] = TableQuery[Differences]
  val changes: TableQuery[Changes] = TableQuery[Changes]
  val containerChanges: TableQuery[ContainerChanges] = TableQuery[ContainerChanges]
  val valueChanges: TableQuery[ValueChanges] = TableQuery[ValueChanges]

}