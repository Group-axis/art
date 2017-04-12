package com.groupaxis.groupsuite.audit.infrastructor.jdbc

import com.groupaxis.groupsuite.audit.write.domain.model.difference._
import com.groupaxis.groupsuite.persistence.datastore.jdbc.Database
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import org.apache.logging.log4j.scala.Logging

  object dbHelper {
    val database = new Database(slick.driver.PostgresDriver, "jdbc:postgresql://localhost:5432/postgres", "postgres", "postgres")
  }

  object esHelper {

    def client = {
      import org.elasticsearch.common.settings.Settings
      val settings = Settings.settingsBuilder().put("cluster.name", "groupsuite").build()
      val client = ElasticClient.transport(settings, ElasticsearchClientUri("elasticsearch://127.0.0.1:9300"))
      client
    }

  }

object EmptyDiffTest extends  App with Logging {

  val diffDao = new DifferenceDAO(slick.driver.PostgresDriver)
  val diffRepo = new JdbcDifferenceWriteRepository(diffDao, dbHelper.database)
  val now = org.joda.time.DateTime.now()
  val newDiff = DifferenceEntity(-1,"irach",now,"rule","rule-code-01")
  val res = diffRepo.createDifference(newDiff)
  logger.debug(res)
  Thread.sleep(2500)
}

object DiffWithAChangeTest extends  App with Logging {

  val diffDao = new DifferenceDAO(slick.driver.PostgresDriver)
  val diffRepo = new JdbcDifferenceWriteRepository(diffDao, dbHelper.database)
  val now = org.joda.time.DateTime.now()
  val changes = Some(Seq(ChangeEntity(-1,-1,"NEW","CODE")))
  val newDiff = DifferenceEntity(-1,"irachitos",now,"ruleses","rules-code-011", changes)
  val res = diffRepo.createDifference(newDiff)
  logger.debug(res)
  Thread.sleep(2500)
}

object DiffWithFullChangeTest extends  App with Logging {

  val diffDao = new DifferenceDAO(slick.driver.PostgresDriver)
  val diffRepo = new JdbcDifferenceWriteRepository(diffDao, dbHelper.database)
  val now = org.joda.time.DateTime.now()
  val valuechanges = Some(ValueChangeEntity(-1,-1,"","25-opsdd"))
  val containerchanges = Some(Seq(ContainerChangeEntity(-1,-1,"rr","A")))
  val changes = Some(Seq(ChangeEntity(-1,-1,"NEW","CODE", containerchanges, valuechanges)))
  val newDiff = DifferenceEntity(-1,"irachitos",now,"ruleses","rules-code-011", changes)
  val res = diffRepo.createDifference(newDiff)
  logger.debug(res)
  Thread.sleep(2500)
}