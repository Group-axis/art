package com.groupaxis.groupsuite.routing.es.schema

import com.groupaxis.groupsuite.persistence.datastore.es.util.GPESHelper
import com.sksamuel.elastic4s.analyzers._
import com.sksamuel.elastic4s.mappings.FieldType.{BooleanType, LongType, StringType}
import com.sksamuel.elastic4s.{CreateIndexDefinition, ElasticClient}
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse

import scala.concurrent.Future

object SAASchema {

  import com.sksamuel.elastic4s.ElasticDsl._

  val saaIndexName = "routing"

  private val GPAnalyzer = CustomAnalyzerDefinition(
    "autocomplete",
    WhitespaceTokenizer,
    LowercaseTokenFilter,
    NGramTokenFilter("autocomplete_filter", 1, 50)
  )


  private val createSAAIndexDefinition =
    create index saaIndexName analysis (GPESHelper.GPAutocompleteAnalyzer, GPESHelper.GPIdAnalyzer) mappings(
      "points" fields(
        "pointName" typed StringType analyzer "autocomplete",
        "full" typed BooleanType index NotAnalyzed,
        "rules" nested(
          "description" typed StringType analyzer "autocomplete",
          "routingPoint" typed StringType index NotAnalyzed,
          "sequence" typed LongType index NotAnalyzed,
          "schemas" typed StringType analyzer "autocomplete",
          "action" nested(
            "actionOn" nested(
              "id" typed LongType index NotAnalyzed,
              "code" typed StringType index NotAnalyzed,
               "description" typed StringType analyzer "autocomplete"),
            "source" nested(
              "action" typed StringType index NotAnalyzed,
              "actionOption" typed StringType index NotAnalyzed,
              "intervention" typed StringType index NotAnalyzed,
              "interventionText" typed StringType index NotAnalyzed,
              "priority" typed StringType index NotAnalyzed,
              "routingCode" typed StringType index NotAnalyzed,
              "unit" typed StringType index NotAnalyzed),
            "newInstance" nested(
              "action" typed StringType index NotAnalyzed,
              "actionOption" typed StringType index NotAnalyzed,
              "instanceType" typed StringType index NotAnalyzed,
              "instanceTypeOption" typed StringType index NotAnalyzed,
              "intervention" typed StringType index NotAnalyzed,
              "interventionText" typed StringType index NotAnalyzed,
              "priority" typed StringType index NotAnalyzed,
              "routingCode" typed StringType index NotAnalyzed,
              "unit" typed StringType index NotAnalyzed)
            ), //action
          "condition" nested(
            "conditionOn" nested(
              "description" typed StringType analyzer "autocomplete",
              "id" typed LongType index NotAnalyzed),
            "functions" nested(
              "description" typed StringType analyzer "autocomplete",
              "id" typed LongType index NotAnalyzed),
            "message" typed StringType analyzer "autocomplete"
            ) //condition
          ) //rules
        ), //points
      "messagePartners" fields(
        "name" typed StringType analyzer "autocomplete",
        "description" typed StringType index NotAnalyzed
        ),
      "exitPoints" fields(
        "name" typed StringType analyzer "autocomplete",
        "queueType" typed StringType index NotAnalyzed,
        "queueThreshold" typed LongType index NotAnalyzed,
        "messagePartner" typed StringType index NotAnalyzed,
        "rulesVisible" typed BooleanType index NotAnalyzed,
        "rulesModifiable" typed BooleanType index NotAnalyzed
        ),
      "schemas" fields(
        "name" typed StringType analyzer "autocomplete",
        "description" typed StringType index NotAnalyzed
        )
      ) indexSetting("max_result_window", 500000) shards 3

  def createSAAIndex
  : (ElasticClient) => Future[CreateIndexResponse] =
    createIndex(createSAAIndexDefinition)


  private def createIndex(indexDefinition: CreateIndexDefinition)
  : (ElasticClient) => Future[CreateIndexResponse] =
    (client: ElasticClient) => client.execute {
      indexDefinition
    }
}
