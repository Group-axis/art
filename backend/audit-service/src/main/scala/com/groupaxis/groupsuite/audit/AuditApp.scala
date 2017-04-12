package com.groupaxis.groupsuite.audit

import akka.actor.ActorSystem
import com.groupaxis.groupsuite.audit.interfaces.http.HttpAuditService
//import com.groupaxis.groupsuite.audit.interfaces.http.HttpAuditService
import com.groupaxis.groupsuite.persistence.datastore.jdbc.Database
import com.typesafe.config.{Config, ConfigFactory}


object AuditApp {
//  private val jvmArg = """-D(\S+)=(\S+)""".r
  private val auditClusterName = "groupsuite-audit"


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
//      val conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
//        withFallback(ConfigFactory.load())
      val conf = ConfigFactory.load()
      val system = ActorSystem(auditClusterName, conf)

      system.actorOf(HttpAuditService.props(conf, database), HttpAuditService.Name)

    }

}












