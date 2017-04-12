package com.groupaxis.groupsuite.simulation.es.schema

import com.groupaxis.groupsuite.persistence.datastore.es.util.GPESHelper
import com.sksamuel.elastic4s.mappings.FieldType.{IntegerType, StringType}
import com.sksamuel.elastic4s.{CreateIndexDefinition, ElasticClient}
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse

import scala.concurrent.Future

object SimulationSchema {

  import com.sksamuel.elastic4s.ElasticDsl._

  val simulationIndexName = "messages"

  private val createAMHMessageIndex =
    create index simulationIndexName analysis(GPESHelper.GPAutocompleteAnalyzer, GPESHelper.GPIdAnalyzer) mappings(
      "amh" fields(
        "content" typed StringType index NotAnalyzed,
        "creationDate" typed StringType index NotAnalyzed,
        "group" typed StringType analyzer "id_analyzer",
        "groupCount" typed IntegerType index NotAnalyzed,
        "id" typed StringType index NotAnalyzed,
        "itemMap" typed StringType index NotAnalyzed,
        "name" typed StringType analyzer "autocomplete",
        "userId" typed StringType index NotAnalyzed
        ),
      "group" fields(
        "id" typed StringType index NotAnalyzed,
        "name" typed StringType analyzer "autocomplete",
        "itemMap" typed StringType index NotAnalyzed,
        "groupCount" typed IntegerType index NotAnalyzed,
        //"group" typed StringType analyzer "autocomplete"
        "group" typed StringType analyzer "id_analyzer"
        //        "content" typed StringType index NotAnalyzed,
        //        "creationDate" typed StringType index NotAnalyzed,
        //        "userId" typed StringType index NotAnalyzed
        )
      ) indexSetting("max_result_window", 500000) shards 3

  def createMessageIndex
  : (ElasticClient) => Future[CreateIndexResponse] =
    createIndex(createAMHMessageIndex)


  private def createIndex(indexDefinition: CreateIndexDefinition)
  : (ElasticClient) => Future[CreateIndexResponse] =
    (client: ElasticClient) => client.execute {
      indexDefinition
    }

}
