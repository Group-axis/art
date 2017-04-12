package com.groupaxis.groupsuite.amh.routing

import akka.actor.{Actor, ActorIdentity, ActorPath, ActorRef, ActorSystem, AddressFromURIString, Identify, PoisonPill, Props, RootActorPath, SupervisorStrategy, Terminated}
import akka.cluster.client.{ClusterClient, ClusterClientSettings}
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings}
import akka.japi.Util.immutableSeq
import akka.pattern.ask
import akka.persistence.journal.leveldb.{SharedLeveldbJournal, SharedLeveldbStore}
import akka.util.Timeout
import com.groupaxis.groupsuite.amh.routing.application.services.RuleService
import com.groupaxis.groupsuite.amh.routing.interfaces.http.HttpRoutingService
import com.groupaxis.groupsuite.commons.persistence.jdbc.DatabaseService
import com.groupaxis.groupsuite.commons.protocol.Master
import com.groupaxis.groupsuite.persistence.datastore.jdbc.Database
import com.groupaxis.groupsuite.routing.write.domain.model.amh.assignment.{AssignmentDAO, AssignmentRuleDAO}
import com.groupaxis.groupsuite.routing.write.domain.model.amh.rule.AMHRuleDAO
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.duration._

object Supervisor {
  final val Name = "amh-routing-main"

  def props(config: Config): Props = Props(classOf[Supervisor], config)
}

class Supervisor(config: Config) extends Actor with Logging {
  override val supervisorStrategy = SupervisorStrategy.stoppingStrategy

  private val amhRuleService = context.watch(createAmhRuleService())
//  context.watch(createHttpService(amhRuleService))

  logger.info("Up and running")

  override def receive = {
    case Terminated(actor) => onTerminated(actor)
  }

  protected def createAmhRuleService(): ActorRef = {
    context.actorOf(RuleService.props(20.seconds), RuleService.Name)
  }

//  protected def createHttpService(ruleService: ActorRef): ActorRef = {
//    //    import settings.httpService._
//    val selfTimeout = 23.seconds
////    context.actorOf(HttpRoutingService.props(config, ruleService, ruleService, ruleService), HttpRoutingService.Name)
//  }

  protected def onTerminated(actor: ActorRef): Unit = {
    logger.error(s"Terminating the system because $actor terminated!")
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

    // load worker.conf
    val conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=0").
      withFallback(ConfigFactory.load("worker"))
    val dbService = new DatabaseService(slick.driver.PostgresDriver, conf.getString("database.url"), conf.getString("database.user"), conf.getString("database.password"))
    val database = new Database(slick.driver.PostgresDriver, conf.getString("database.url"), conf.getString("database.user"), conf.getString("database.password"))
    if (args.isEmpty) {
//      startRuleMaster(2551, "backend")
      Thread.sleep(5000)
//      startRuleMaster(2552, "backend")
      Thread.sleep(5000)
      startEndPoint(dbService, database, 0)
//      startWorker(database, 0)

    } else {
      val port = args(0).toInt
      if (2000 <= port && port <= 2999)
        startRuleMaster(port, "backend")
      else if (3000 <= port && port <= 3999)
        startEndPoint(dbService, database, port)
      else
        startWorker(dbService, port)
    }
  }

  def workTimeout = 21.seconds

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

  def startEndPoint(toto: DatabaseService, database : Database, port: Int): Unit = {
//    val conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
//      withFallback(ConfigFactory.load())
//    val system = ActorSystem(routingClusterName, conf)
//
//    val ruleService = system.actorOf(RuleService.props(10.seconds), RuleService.Name)
//    val assignmentService = system.actorOf(AssignmentService.props(10.seconds), AssignmentService.Name)
//
//    val ruleDao : AMHRuleDAO = new AMHRuleDAO(slick.driver.PostgresDriver)
//    val backendDao : BackendDAO = new BackendDAO(slick.driver.PostgresDriver)
//    val assignmentDao : AssignmentDAO = new AssignmentDAO(slick.driver.PostgresDriver)
//    val assignmentRuleDao : AssignmentRuleDAO = new AssignmentRuleDAO(slick.driver.PostgresDriver)
//    val ruleRepo = new com.groupaxis.groupsuite.xml.parser.amh.writer.jdbc.JdbcAMHRuleRepository(ruleDao, database)
//    val backendRepo : JdbcAMHBackendRepository = new JdbcAMHBackendRepository(backendDao, database)
//    val assignmentRepo : JdbcAMHAssignmentRepository = new JdbcAMHAssignmentRepository(assignmentDao, database)
//    val assignmentRuleRepo : JdbcAMHAssignmentRuleRepository = new JdbcAMHAssignmentRuleRepository(assignmentRuleDao, database)
//    val impExpService = system.actorOf(FileAMHXmlWriteRepository.props(ruleRepo, backendRepo, assignmentRepo, assignmentRuleRepo, database), FileAMHXmlWriteRepository.Name)
//
//    system.actorOf(HttpRoutingService.props(conf, ruleService, assignmentService, impExpService), HttpRoutingService.Name)

  }

  def startHttpService(dbService: DatabaseService, database : Database, port: Int): Unit = {
    val conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
      withFallback(ConfigFactory.load())
    val system = ActorSystem(routingClusterName, conf)

    system.actorOf(HttpRoutingService.props(conf, dbService), HttpRoutingService.Name)

  }

//  def startService(port: Int, serviceProps: Props, serviceName: String): ActorRef = {
//    val conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
//      withFallback(ConfigFactory.load())
//    val system = ActorSystem(routingClusterName, conf)
//    system.actorOf(serviceProps, serviceName)
//  }

  def startWorker(database : DatabaseService, port: Int): Unit = {
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

    val clusterAssignmentClient = system.actorOf(
      ClusterClient.props(
        ClusterClientSettings(system)
          .withInitialContacts(initialContacts)),
      "clusterAssignmentClient")


    val ruleDao : AMHRuleDAO = AMHRuleDAO
    val assignmentDao : AssignmentDAO = AssignmentDAO(slick.driver.PostgresDriver)
    val assignmentRuleDao : AssignmentRuleDAO = AssignmentRuleDAO(slick.driver.PostgresDriver)
//    system.actorOf(Worker.props(clusterClient, JdbcAMHRuleWriteRepository.props(ruleDao, database)), "worker")
//    system.actorOf(Worker.props(clusterAssignmentClient, JdbcAMHAssignmentWriteRepository.props(assignmentDao, assignmentRuleDao, database)), "assignmentWorker")
  }

  def startupSharedJournal(system: ActorSystem, startStore: Boolean, path: ActorPath, journalName: String): Unit = {
    // Start the shared journal one one node (don't crash this SPOF)
    // This will not be needed with a distributed journal
    if (startStore)
      system.actorOf(Props[SharedLeveldbStore], journalName)
    // register the shared journal
    import system.dispatcher
    implicit val timeout = Timeout(15.seconds)
    val f = system.actorSelection(path) ? Identify(None)
    f.onSuccess {
      case ActorIdentity(_, Some(ref)) => SharedLeveldbJournal.setStore(ref, system)
      case _ =>
        system.log.error("Shared journal not started at {}", path)
        system.terminate()
    }
    f.onFailure {
      case _ =>
        system.log.error("Lookup of shared journal at {} timed out", path)
        system.terminate()
    }
  }
}












