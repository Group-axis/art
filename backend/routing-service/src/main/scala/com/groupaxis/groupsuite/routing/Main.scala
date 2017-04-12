package com.groupaxis.groupsuite.routing

//import java.util.Date


//import com.groupaxis.groupsuite.commons.service.GenericServices
//import com.groupaxis.groupsuite.routing.write.domain.model.routing.rule.Action
//import com.groupaxis.groupsuite.routing.write.domain.model.routing.rule.Condition
//import com.groupaxis.groupsuite.routing.write.domain.model.routing.rule.ConditionFunction
//import com.groupaxis.groupsuite.routing.write.domain.model.routing.rule.MessageAndFunctionType
//import com.groupaxis.groupsuite.routing.write.domain.model.routing.rule.MessageType
//import com.groupaxis.groupsuite.routing.write.domain.model.routing.rule.NewInstance
//import com.groupaxis.groupsuite.routing.write.domain.model.routing.rule.NewInstanceType
//import com.groupaxis.groupsuite.routing.write.domain.model.routing.rule.Rule
//import com.groupaxis.groupsuite.routing.write.domain.model.routing.rule.{Source => RuleSource}
//import com.groupaxis.groupsuite.routing.write.domain.model.routing.rule.SourceAndNewInstanceType
//import com.groupaxis.groupsuite.routing.write.domain.model.routing.schema.Schema
//import akka.actor.Actor
//import akka.actor.ActorLogging
//import akka.actor.Props
//import akka.http.scaladsl.server.Directives._
//import akka.actor.actorRef2Scala
//import com.groupaxis.groupsuite.routing.write.domain.model.routing.rule.{Source => RuleSource}

//import scala.concurrent.Await
//import scala.concurrent.duration._
//import akka.http.javadsl.server.HttpService
//import akka.actor.Terminated
//import akka.actor.ActorRef
//import akka.actor.SupervisorStrategy
//import com.groupaxis.groupsuite.routing.infrastructor.jdbc.JdbcRuleWriteRepository
//import akka.cluster.Cluster
//import com.groupaxis.groupsuite.commons.protocol.Master
//
//import scala.concurrent.duration._
//import com.groupaxis.groupsuite.commons.protocol.worker.Worker
//import com.groupaxis.groupsuite.routing.application.services.RuleService
import com.typesafe.config.Config
import akka.actor.ActorSystem
//import akka.actor.ActorPath
import com.typesafe.config.ConfigFactory
//import akka.util.Timeout
//import akka.actor.Identify
//import akka.actor.PoisonPill
//import akka.persistence.journal.leveldb.SharedLeveldbStore
//import akka.cluster.singleton.ClusterSingletonManagerSettings
//import akka.cluster.singleton.ClusterSingletonManager
//import akka.pattern.ask
//import akka.actor.ActorIdentity
//import akka.persistence.journal.leveldb.SharedLeveldbJournal
//import akka.actor.AddressFromURIString
//import akka.actor.RootActorPath
//import akka.japi.Util.immutableSeq
//import akka.cluster.client.ClusterClient
//import akka.cluster.client.ClusterClientSettings
//import com.groupaxis.groupsuite.amh.routing.interfaces.http.HttpRoutingService
//import com.groupaxis.groupsuite.routing.infrastructor.jdbc.JdbcRuleWriteRepository
//import com.groupaxis.groupsuite.commons.persistence.jdbc.DatabaseService
import com.groupaxis.groupsuite.datastore.jdbc.Database
import com.groupaxis.groupsuite.routing.interfaces.http.HttpRoutingService

//object Supervisor {
//  final val Name = "routing-main"
//
//  def props(config: Config): Props = Props(classOf[Supervisor], config)
//}

//class Supervisor(config: Config) extends Actor with ActorLogging {
//  override val supervisorStrategy = SupervisorStrategy.stoppingStrategy
//
//  private val ruleService = context.watch(createRuleService())
//  context.watch(createHttpService(ruleService))
//
//  log.info("Up and running")
//
//  override def receive = {
//    case Terminated(actor) => onTerminated(actor)
//  }
//
//  protected def createRuleService(): ActorRef = {
//    context.actorOf(RuleService.props(10.seconds), RuleService.Name)
//  }
//
//  protected def createHttpService(ruleService: ActorRef): ActorRef = {
//    //    import settings.httpService._
//    val selfTimeout = 10.seconds
//    context.actorOf(HttpRoutingService.props(config, ruleService), HttpRoutingService.Name)
//  }
//
//  protected def onTerminated(actor: ActorRef): Unit = {
//    log.error("Terminating the system because {} terminated!", actor)
//    context.system.terminate()
//  }
//}

//object RoutingApp extends App with CoreServices {
object RoutingApp { //extends CoreServices 
  private val jvmArg = """-D(\S+)=(\S+)""".r
  private val routingClusterName = "groupsuite-routing"


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
      val system = ActorSystem(routingClusterName, conf)


      system.actorOf(HttpRoutingService.props(conf, database), HttpRoutingService.Name)

//      val service = system.actorOf(RuleService.props(10.seconds), RuleService.Name)
//      val selfTimeout = 10.seconds
//      system.actorOf(HttpRoutingService.props(conf, service), HttpRoutingService.Name)

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












