package com.groupaxis.groupsuite.xxxxx

import akka.actor.ActorSystem
import com.groupaxis.groupsuite.persistence.datastore.jdbc.Database
import com.groupaxis.groupsuite.xxxxx.interfaces.http.HttpSimulatorService
import com.typesafe.config.{Config, ConfigFactory}


object XxxxxApp {
//  private val jvmArg = """-D(\S+)=(\S+)""".r
  private val xxxxxClusterName = "groupsuite-xxxxx"


  def main(args: Array[String]): Unit = {
    //for (jvmArg(name, value) <- args) System.setProperty(name, value)

    // load worker.conf
    val conf : Config = ConfigFactory.parseString("akka.remote.netty.tcp.port=0").
      withFallback(ConfigFactory.load("worker"))
    val database = new Database(slick.driver.PostgresDriver, conf.getString("database.url"), conf.getString("database.user"), conf.getString("database.password"))

    if (args.isEmpty) {
      startHttpService(database, 0)
    }
  }

    def startHttpService(database: Database, port: Int): Unit = {
      val conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
        withFallback(ConfigFactory.load())
      val system = ActorSystem(simulatorClusterName, conf)


      system.actorOf(HttpXxxxxService.props(conf, database), HttpXxxxxService.Name)

    }

}












