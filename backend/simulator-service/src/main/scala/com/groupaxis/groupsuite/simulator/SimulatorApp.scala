package com.groupaxis.groupsuite.simulator

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import com.groupaxis.groupsuite.persistence.datastore.jdbc.Database
import com.groupaxis.groupsuite.simulator.application.services.BatchTask
import com.groupaxis.groupsuite.simulator.infrastructor.jdbc.JdbcJobWriteRepository
import com.groupaxis.groupsuite.simulator.interfaces.http.HttpSimulatorService
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration.Duration




object SimulatorApp {
//  private val jvmArg = """-D(\S+)=(\S+)""".r
  private val simulatorClusterName = "groupsuite-simulator"


  def main(args: Array[String]): Unit = {
    //for (jvmArg(name, value) <- args) System.setProperty(name, value)

    // load worker.conf
    val conf : Config = ConfigFactory.parseString("akka.remote.netty.tcp.port=0").
      withFallback(ConfigFactory.load("worker"))
    val database = new Database(slick.driver.PostgresDriver, conf.getString("database.url"), conf.getString("database.user"), conf.getString("database.password"))

    if (args.isEmpty) {
      startApp(database, 0)
    }
  }

    def startApp(database: Database, port: Int): Unit = {
      val conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
        withFallback(ConfigFactory.load())
      val system = ActorSystem(simulatorClusterName, conf)

      system.actorOf(HttpSimulatorService.props(conf, database), HttpSimulatorService.Name)

      import scala.concurrent.duration._
      scheduleJobProcessor(system, BatchTask(conf, new JdbcJobWriteRepository(database, 30.minutes)))
    }


  def scheduleJobProcessor(system : ActorSystem, task : Runnable): Unit = {
    val scheduler = system.scheduler
    implicit val executor = system.dispatcher
    scheduler.schedule(
      initialDelay = Duration(5, TimeUnit.SECONDS),
      interval = Duration(30, TimeUnit.SECONDS),
      runnable = task
    )
  }

//  def workTimeout = 10.seconds
//
//  def startRuleMaster(port: Int, role: String): Unit = {
//    val conf = ConfigFactory.parseString(s"akka.cluster.roles=[$role]").
//      withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port)).
//      withFallback(ConfigFactory.load())
//    val system = ActorSystem(routingClusterName, conf)
//    val journalName = "store"
//    startupSharedJournal(system, startStore = (port == 2551), path =
//      ActorPath.fromString(s"akka.tcp://$routingClusterName@127.0.0.1:2551/user/$journalName"), journalName)
//
//    system.actorOf(
//      ClusterSingletonManager.props(
//        Master.props(workTimeout),
//        PoisonPill,
//        ClusterSingletonManagerSettings(system).withRole(role)),
//      "master")
//
//  }
//
//  def startEndPoint(port: Int): Unit = {
//    val conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
//      withFallback(ConfigFactory.load())
//    val system = ActorSystem(routingClusterName, conf)
//
//    val service = system.actorOf(RuleService.props(10.seconds), RuleService.Name)
//    val selfTimeout = 10.seconds
//    system.actorOf(HttpRoutingService.props(conf, service), HttpRoutingService.Name)
//  }
//
//  def startService(port: Int, serviceProps: Props, serviceName: String): ActorRef = {
//    val conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
//      withFallback(ConfigFactory.load())
//    val system = ActorSystem(routingClusterName, conf)
//    system.actorOf(serviceProps, serviceName)
//  }
//
//  def startWorker(port: Int): Unit = {
//    // load worker.conf
//    val conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
//      withFallback(ConfigFactory.load("worker"))
//    val system = ActorSystem(routingWorkerName, conf)
//    val initialContacts = immutableSeq(conf.getStringList("contact-points")).map {
//      case AddressFromURIString(addr) => RootActorPath(addr) / "system" / "receptionist"
//    }.toSet
//
//    val clusterClient = system.actorOf(
//      ClusterClient.props(
//        ClusterClientSettings(system)
//          .withInitialContacts(initialContacts)),
//      "clusterClient")
//
//    val database = new DatabaseService(slick.driver.PostgresDriver,conf.getString("database.url"), conf.getString("database.user"), conf.getString("database.password"))
//    system.actorOf(Worker.props(clusterClient, JdbcRuleWriteRepository.props(database)), "worker")
//  }
//
//  def startupSharedJournal(system: ActorSystem, startStore: Boolean, path: ActorPath, journalName: String): Unit = {
//    // Start the shared journal one one node (don't crash this SPOF)
//    // This will not be needed with a distributed journal
//    if (startStore)
//      system.actorOf(Props[SharedLeveldbStore], journalName)
//    // register the shared journal
//    import system.dispatcher
//    implicit val timeout = Timeout(15.seconds)
//    val f = (system.actorSelection(path) ? Identify(None))
//    f.onSuccess {
//      case ActorIdentity(_, Some(ref)) => SharedLeveldbJournal.setStore(ref, system)
//      case _ =>
//        system.log.error("Shared journal not started at {}", path)
//        system.terminate()
//    }
//    f.onFailure {
//      case _ =>
//        system.log.error("Lookup of shared journal at {} timed out", path)
//        system.terminate()
//    }
//  }
}












