package com.groupaxis.groupsuite.routing.infrastructor.es

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import org.apache.logging.log4j.scala.Logging

// elasticsearch stuff

// scala concurrency stuff
import com.sksamuel.elastic4s.ElasticsearchClientUri

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.util.{Failure, Success}

object Hello extends Logging {
  def main(args: Array[String]): Unit = {

    import org.elasticsearch.common.settings.Settings
    val settings = Settings.settingsBuilder().put("cluster.name", "groupsuite").build()
    val client = ElasticClient.transport(settings, ElasticsearchClientUri("elasticsearch://127.0.0.1:9300"))

    // scala uses the java driver which listens on port 9300
//    val client = ElasticClient.transport(ElasticsearchClientUri("127.0.0.1", 9300))

    val res = client execute { search in "index_name/type_name" query "your_query" }

    res onComplete {
      case Success(s) => logger.debug(s)
      case Failure(t) => logger.debug("An error has occured: " + t)
    }

    logger.debug("Request sent")

    //adjust this if needed
    Thread.sleep(1000)
  }
}