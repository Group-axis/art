package com.groupaxis.groupsuite.routing

import akka.actor.{Actor, ActorIdentity, ActorPath, ActorRef, ActorSystem, AddressFromURIString, Identify, PoisonPill, Props, RootActorPath, SupervisorStrategy, Terminated}
import akka.cluster.client.{ClusterClient, ClusterClientSettings}
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings}
import akka.japi.Util.immutableSeq
import akka.pattern.ask
import akka.persistence.journal.leveldb.{SharedLeveldbJournal, SharedLeveldbStore}
import akka.util.Timeout
import com.groupaxis.groupsuite.commons.protocol.Master
import com.groupaxis.groupsuite.routing.application.services.RuleService
import com.groupaxis.groupsuite.routing.write.domain.model.routing.rule.{Source => RuleSource}
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.duration._

object Supervisor {
  final val Name = "routing-main"

  def props(config: Config): Props = Props(classOf[Supervisor], config)
}

class Supervisor(config: Config) extends Actor with Logging {
  override val supervisorStrategy = SupervisorStrategy.stoppingStrategy

  private val ruleService = context.watch(createRuleService())
//  context.watch(createHttpService(ruleService))

  logger.info("Up and running")

  override def receive = {
    case Terminated(actor) => onTerminated(actor)
  }

  protected def createRuleService(): ActorRef = {
    context.actorOf(RuleService.props(10.seconds), RuleService.Name)
  }

//  protected def createHttpService(ruleService: ActorRef): ActorRef = {
//    //    import settings.httpService._
//    val selfTimeout = 10.seconds
//    context.actorOf(HttpRoutingService.props(config, ruleService), HttpRoutingService.Name)
//  }

  protected def onTerminated(actor: ActorRef): Unit = {
    logger.error("Terminating the system because $actor terminated!")
    context.system.terminate()
  }
}

//object RoutingApp extends App with CoreServices {
object RoutingAppWithCluster { //extends CoreServices
  private val jvmArg = """-D(\S+)=(\S+)""".r
  private val routingClusterName = "groupsuite-routing-cluster"
  private val routingWorkerName = "groupsuite-routing-worker"

  def main(args: Array[String]): Unit = {
    //for (jvmArg(name, value) <- args) System.setProperty(name, value)

    //val routingSupervisor = system.actorOf(Supervisor.props(config), Supervisor.Name)
    //    logger.info(banner)
    if (args.isEmpty) {
      startRuleMaster(2551, "backend")
      Thread.sleep(5000)
      startRuleMaster(2552, "backend")
      Thread.sleep(5000)
      startEndPoint(0)
      startWorker(0)

    } else {
      val port = args(0).toInt
      if (2000 <= port && port <= 2999)
        startRuleMaster(port, "backend")
      else if (3000 <= port && port <= 3999)
        startEndPoint(port)
      else
        startWorker(port)
    }
  }

  def workTimeout = 10.seconds

  def startRuleMaster(port: Int, role: String): Unit = {
    val conf = ConfigFactory.parseString(s"akka.cluster.roles=[$role]").
      withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port)).
      withFallback(ConfigFactory.load())
    val system = ActorSystem(routingClusterName, conf)
    val journalName = "store"
    startupSharedJournal(system, startStore = port == 2551, path =
      ActorPath.fromString(s"akka.tcp://$routingClusterName@127.0.0.1:2551/user/$journalName"), journalName)

    system.actorOf(
      ClusterSingletonManager.props(
        Master.props(workTimeout),
        PoisonPill,
        ClusterSingletonManagerSettings(system).withRole(role)),
      "master")

  }

  def startEndPoint(port: Int): Unit = {
    val conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
      withFallback(ConfigFactory.load())
    val system = ActorSystem(routingClusterName, conf)

    val service = system.actorOf(RuleService.props(10.seconds), RuleService.Name)
    val selfTimeout = 10.seconds
//    system.actorOf(HttpRoutingService.props(conf, service), HttpRoutingService.Name)
  }

  def startService(port: Int, serviceProps: Props, serviceName: String): ActorRef = {
    val conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
      withFallback(ConfigFactory.load())
    val system = ActorSystem(routingClusterName, conf)
    system.actorOf(serviceProps, serviceName)
  }

  def startWorker(port: Int): Unit = {
    // load worker.conf
    val conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
      withFallback(ConfigFactory.load("worker"))
    val system = ActorSystem(routingWorkerName, conf)
    val initialContacts = immutableSeq(conf.getStringList("contact-points")).map {
      case AddressFromURIString(addr) => RootActorPath(addr) / "system" / "receptionist"
    }.toSet

    val clusterClient = system.actorOf(
      ClusterClient.props(
        ClusterClientSettings(system)
          .withInitialContacts(initialContacts)),
      "clusterClient")

//    val database = new DatabaseService(conf.getString("database.url"), conf.getString("database.user"), conf.getString("database.password"))
//    system.actorOf(Worker.props(clusterClient, JdbcRuleWriteRepository.props(database)), "worker")
  }

  def startupSharedJournal(system: ActorSystem, startStore: Boolean, path: ActorPath, journalName: String): Unit = {
    // Start the shared journal one one node (don't crash this SPOF)
    // This will not be needed with a distributed journal
    if (startStore)
      system.actorOf(Props[SharedLeveldbStore], journalName)
    // register the shared journal
    import system.dispatcher
    implicit val timeout = Timeout(15.seconds)
    val f = (system.actorSelection(path) ? Identify(None))
    f.onSuccess {
      case ActorIdentity(_, Some(ref)) => SharedLeveldbJournal.setStore(ref, system)
      case _ =>
        system.terminate()
    }
    f.onFailure {
      case _ =>
        system.terminate()
    }
  }
}












