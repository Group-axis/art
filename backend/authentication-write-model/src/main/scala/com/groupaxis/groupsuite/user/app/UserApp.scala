package com.groupaxis.groupsuite.user.app

import com.groupaxis.groupsuite.persistence.datastore.jdbc.Database
import com.groupaxis.groupsuite.user.model.User
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.Await


object UserApp extends Logging {

  private val userClusterName = "groupsuite-users"

  def main(args: Array[String]): Unit = {

    val conf: Config = ConfigFactory.parseString("akka.remote.netty.tcp.port=0").
      withFallback(ConfigFactory.load("worker"))

    val database = new Database(slick.driver.PostgresDriver, conf.getString("database.url"), conf.getString("database.user"), conf.getString("database.password"))

    testUserService(database)

  }

  import com.groupaxis.groupsuite.user.service.interpreter.UserServiceInterpreter
  import scala.concurrent.duration._

  private def testUserService(database: Database) = {
    val userService = UserServiceInterpreter()
    val userCreated = userService.create("iilish","Irach","Ramos",active = false,None,None)
            .run(database)

    try {
      val user = Await.result(userCreated.run, 10.seconds)
      val userActivated = userService.activateUser(user.getOrElse(User("iilish","","",true,None, None)).login)
        .run(database)
      logger.debug(userActivated)
    }catch {
      case e:Exception => logger.debug(s"FROM OUTSIDE!! ${e.getMessage}")
    }
    logger.debug(s"userCreated => $userCreated")

  }
}
