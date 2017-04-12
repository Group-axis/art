package com.groupaxis.groupsuite.audit.infrastructor.jdbc

import com.groupaxis.groupsuite.audit.write.domain.model.difference._
import com.groupaxis.groupsuite.persistence.datastore.jdbc.Database
import org.apache.logging.log4j.scala.Logging
import org.joda.time.DateTime

import scala.concurrent.Future
//import scala.concurrent.duration.FiniteDuration


object JdbcDifferenceWriteRepository {
}

class JdbcDifferenceWriteRepository(diffDao: DifferenceDAO, database: Database) extends DifferenceWriteRepository with Logging  {

  import database._
  import slick.driver.PostgresDriver.api._

  import scala.concurrent.ExecutionContext.Implicits.global

  def getActionsByUser(username: String): Future[Seq[DifferenceEntity]] = {
    Future {
      Seq(DifferenceEntity(1, "", DateTime.now(), "", ""))
    }
  }

  def getChanges(diff: DifferenceEntity): Future[Seq[ChangeEntity]] = {
    Future {
      Seq(ChangeEntity(1, 1, "", ""))
    }
  }

  /*
  def deleteRuleByCode(code : String) : Future[Int] = {

     val action  = for {
        rulesFound <- dao.amhRules.filter(rule => rule.code === code).result
        // test <- dao.amhRules += AMHRuleEntity("irach_testing")
        deletedCount <- {
          logger.debug(s"ruleFound $rulesFound nonEmpty ${rulesFound.nonEmpty} nonEmpty2 ${rulesFound.size > 0} ")
          if (rulesFound.nonEmpty) {
            logger.debug(s"executing the delete")
            dao.amhRules.filter(rule => rule.code === code).delete
          } else {
            logger.debug(s"NO executing the delete")
            DBIO.failed(new SQLException(s"No rules found with code $code"))
          }
        }
        inserted <- {
          logger.debug(s"executing the insert")
          val rule = rulesFound.head
          dao.amhRules +=
            rule.copy(code="_" + currentDate, originalCode=Some(rule.code), deleted = "Y")
        }
     } yield inserted


     db.run(action.transactionally)
       .recover{
         case ex : java.sql.SQLException =>
           logger.debug(s"An error has occurred while deleting a rule code $code : ${ex.getLocalizedMessage }")
           -1
       }
   }
  * */
  def createDifference(diff: DifferenceEntity): Future[Any] = {

    val diffActions1  = for {
      diffAction <- diffDao.differences returning diffDao.differences += diff
    } yield diffAction

    val diffActions  = for {
      diffAction <- diffDao.differences returning diffDao.differences += diff
      changeActions <- {
        diff.changes match {
          case Some(changes) =>
            val r = changes.map(ch => {
              for {
                changed <- diffDao.changes returning diffDao.changes += ch.copy(diffId = diffAction.id)
                containerChanged <- {
                  val ee = ch.containerChanges
                    .map(cChanges => cChanges.map(cChange => cChange.copy(changeId = changed.id)))
                    .map(withChangeId => diffDao.containerChanges returning diffDao.containerChanges ++= withChangeId)
                    .getOrElse(DBIO.successful(1))
                  logger.debug(s"in container $ee")
                  ee
                }
                valueChanged <- {
                  val rr = ch.valueChange
                    .map(vChange => vChange.copy(changeId = changed.id))
                    .map(withChangeId => diffDao.valueChanges returning diffDao.valueChanges += withChangeId)
                    .getOrElse(DBIO.successful(1))
                  logger.debug(s"in value $rr")
                  rr
                }

              } yield containerChanged
            })
            r.foreach(a => logger.debug(s"value $a"))
            DBIO.sequence(r)
          case None => DBIO.successful()
        }
      }
    } yield changeActions

    db.run(diffActions.transactionally)
      .recover {
        case ex: java.sql.SQLException =>
          logger.debug(s"An error has occurred while inserting diff : ${ex.getLocalizedMessage}")
          -1
      }
  }


}