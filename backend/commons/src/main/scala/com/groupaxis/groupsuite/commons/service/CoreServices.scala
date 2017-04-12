package com.groupaxis.groupsuite.commons.service

import scala.concurrent.ExecutionContext

import com.typesafe.config.ConfigFactory

import akka.actor.ActorSystem
import akka.event.Logging
import akka.event.LoggingAdapter
import akka.stream.ActorMaterializer
import akka.stream.ActorMaterializerSettings
import akka.stream.Materializer
import akka.stream.Supervision

trait CoreServices extends GenericServices {
	override implicit val logger: LoggingAdapter = Logging(system, this.getClass)
	override val config = ConfigFactory.load()
  override implicit val system = ActorSystem("groupaxis-groupsuite")

  
  val decider: Supervision.Decider = {
    case _ => Supervision.Resume
  }
  override  implicit val mat: Materializer = ActorMaterializer(
    ActorMaterializerSettings(system).withSupervisionStrategy(decider)
  )
  
  override implicit val ec: ExecutionContext = system.dispatcher
  
  var version = "Version 1.0"
  
  // see: http://patorjk.com/software/taag/#p=display&h=1&v=2&f=Old%20Banner&t=GroupaXis Inc.
  val banner =
   s"""
   |  
   |  #####                                     #     #            ###                   
   | #     # #####   ####  #    # #####    ##    #   #  #  ####     #  #    #  ####      
   | #       #    # #    # #    # #    #  #  #    # #   # #         #  ##   # #    #     
   | #  #### #    # #    # #    # #    # #    #    #    #  ####     #  # #  # #          
   | #     # #####  #    # #    # #####  ######   # #   #      #    #  #  # # #      ### 
   | #     # #   #  #    # #    # #      #    #  #   #  # #    #    #  #   ## #    # ### 
   |  #####  #    #  ####   ####  #      #    # #     # #  ####    ### #    #  ####  ### 
   |  
   |  $version  
   """.stripMargin
  
  sys.addShutdownHook {
    system.terminate()
  }
  
 
   
}
