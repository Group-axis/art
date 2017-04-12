package com.groupaxis.groupsuite.amh.routing

import com.groupaxis.groupsuite.commons.persistence.jdbc.DatabaseService
import com.groupaxis.groupsuite.amh.routing.interfaces.http.HttpRoutingService
import com.typesafe.config.{Config, ConfigFactory}
import akka.actor.ActorSystem
import com.groupaxis.groupsuite.persistence.datastore.jdbc.Database

object RoutingApp {
  private val routingClusterName = "groupsuite-routing"

  def main(args: Array[String]): Unit = {

    // load worker.conf
    //ConfigFactory.parseFile(new File("my_path/hdfs.conf"))

    val conf : Config = ConfigFactory.parseString("akka.remote.netty.tcp.port=0").
      withFallback(ConfigFactory.load("worker"))
//    if (true) {
//      logger.debug(s"config ${conf.getConfig("database")}")
//      System.exit(0)
//    }
    val dbService = new DatabaseService(slick.driver.PostgresDriver, conf.getString("database.url"), conf.getString("database.user"), conf.getString("database.password"))
   // val database = new Database(slick.driver.PostgresDriver, conf.getString("database.url"), conf.getString("database.user"), conf.getString("database.password"))
    if (args.isEmpty) {
//      startRuleMaster(2551, "backend")
//      Thread.sleep(5000)
//      startRuleMaster(2552, "backend")
//      Thread.sleep(5000)
      startHttpService(dbService, 0)
//      startWorker(database, 0)

    }
//    else {
//      val port = args(0).toInt
//      if (2000 <= port && port <= 2999)
//        startRuleMaster(port, "backend")
//      else if (3000 <= port && port <= 3999)
//        startEndPoint(database, port)
//      else
//        startWorker(database, port)
//    }
  }

  def startHttpService(dbService: DatabaseService, port: Int): Unit = {
    val conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
      withFallback(ConfigFactory.load())
    val system = ActorSystem(routingClusterName, conf)


    system.actorOf(HttpRoutingService.props(conf, dbService), HttpRoutingService.Name)

  }


}












