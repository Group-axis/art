package com.groupaxis.groupsuite.routing.amh.es.schema

import com.groupaxis.groupsuite.persistence.datastore.es.util.GPESHelper
import com.sksamuel.elastic4s.analyzers._
import com.sksamuel.elastic4s.mappings.FieldType.{BooleanType, LongType, StringType}
import com.sksamuel.elastic4s.{CreateIndexDefinition, ElasticClient}
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse

import scala.concurrent.Future

object AHMRoutingSchema {

  import com.sksamuel.elastic4s.ElasticDsl._

  val amhRoutingIndexName = "amhrouting"

  val amhReferenceIndexName = "amhreference"

  private val amhRoutingIndexDefinition =
    create index amhRoutingIndexName analysis (GPESHelper.GPAutocompleteAnalyzer, GPESHelper.GPIdAnalyzer) mappings(
      "rules" fields(
        "code" typed StringType analyzer "autocomplete",
        "expression" typed StringType analyzer "autocomplete",
        "version" typed StringType analyzer KeywordAnalyzer,
        "environment" typed StringType analyzer KeywordAnalyzer,
        "assigned" typed BooleanType index NotAnalyzed,
        "valid" typed BooleanType index NotAnalyzed,
        "validMessage" typed StringType index NotAnalyzed),
      "backends" fields(
        "code" typed StringType analyzer "autocomplete",
        "pkDirection" typed StringType analyzer "autocomplete",
        "pkCode" typed StringType analyzer "autocomplete",
        "name" typed StringType analyzer KeywordAnalyzer,
        "description" typed StringType analyzer KeywordAnalyzer,
        "version" typed StringType analyzer KeywordAnalyzer,
        "environment" typed StringType analyzer KeywordAnalyzer),
      "distributionCopies" fields(
        "code" typed StringType analyzer "autocomplete",
        "name" typed StringType analyzer KeywordAnalyzer,
        "description" typed StringType analyzer KeywordAnalyzer,
        "sequence" typed LongType index NotAnalyzed,
        "copies" typed LongType index NotAnalyzed,
        "active" typed BooleanType index NotAnalyzed,
        "dataOwner" typed StringType analyzer KeywordAnalyzer,
        "lockCode" typed StringType analyzer KeywordAnalyzer,
        "version" typed StringType analyzer KeywordAnalyzer,
        "environment" typed StringType analyzer KeywordAnalyzer,
        "rules" nested(
          "sequence" typed LongType index NotAnalyzed,
          "code" typed StringType analyzer "autocomplete",
          "expression" typed StringType analyzer "autocomplete",
          "dataOwner" typed StringType analyzer KeywordAnalyzer,
          "lockCode" typed StringType analyzer KeywordAnalyzer
          ),
        "backends" nested(
          "code" typed StringType analyzer "autocomplete",
          "direction" typed StringType analyzer "autocomplete",
          "dataOwner" typed StringType analyzer KeywordAnalyzer,
          "lockCode" typed StringType analyzer KeywordAnalyzer
          )
        ),
      "feedbackDtnCopies" fields(
        "code" typed StringType analyzer "autocomplete",
        "name" typed StringType analyzer KeywordAnalyzer,
        "description" typed StringType analyzer KeywordAnalyzer,
        "sequence" typed LongType index NotAnalyzed,
        "copies" typed LongType index NotAnalyzed,
        "active" typed BooleanType index NotAnalyzed,
        "dataOwner" typed StringType analyzer KeywordAnalyzer,
        "lockCode" typed StringType analyzer KeywordAnalyzer,
        "version" typed StringType analyzer KeywordAnalyzer,
        "environment" typed StringType analyzer KeywordAnalyzer,
        "rules" nested(
          "sequence" typed LongType index NotAnalyzed,
          "code" typed StringType analyzer "autocomplete",
          "expression" typed StringType analyzer "autocomplete",
          "dataOwner" typed StringType analyzer KeywordAnalyzer,
          "lockCode" typed StringType analyzer KeywordAnalyzer
          ),
        "backends" nested(
          "code" typed StringType analyzer "autocomplete",
          "direction" typed StringType analyzer "autocomplete",
          "dataOwner" typed StringType analyzer KeywordAnalyzer,
          "lockCode" typed StringType analyzer KeywordAnalyzer
          )
        ),
      "assignments" fields(
        "code" typed StringType analyzer "autocomplete",
        "sequence" typed LongType index NotAnalyzed,
        "backendPrimaryKey" inner(
          "code" typed StringType analyzer "autocomplete",
          "direction" typed StringType analyzer "autocomplete"
          ),
        "active" typed BooleanType index NotAnalyzed,
        "version" typed StringType analyzer KeywordAnalyzer,
        "environment" typed StringType analyzer KeywordAnalyzer,
        "rules" nested(
          "sequence" typed LongType index NotAnalyzed,
          "code" typed StringType analyzer "autocomplete",
          "expression" typed StringType analyzer "autocomplete",
          "dataOwner" typed StringType analyzer KeywordAnalyzer,
          "lockCode" typed StringType analyzer KeywordAnalyzer
          )
        )
      ) indexSetting("max_result_window", 500000) shards 3

  private val amhReferenceIndexDefinition = create index amhReferenceIndexName analysis (GPESHelper.GPAutocompleteAnalyzer, GPESHelper.GPIdAnalyzer) mappings (
    "criteria" fields(
      "code" typed StringType index NotAnalyzed,
      "searchCode" typed StringType index NotAnalyzed,
      "description" typed StringType index NotAnalyzed,
      "children" nested(
        "code" typed StringType index NotAnalyzed,
        "searchCode" typed StringType index NotAnalyzed,
        "description" typed StringType index NotAnalyzed
        )
      )
    ) indexSetting("max_result_window", 500000) shards 3

  def createAMHRoutingIndex
  : (ElasticClient) => Future[CreateIndexResponse] =
    createIndex(amhRoutingIndexDefinition)

  def createAMHReferenceIndex
  : (ElasticClient) => Future[CreateIndexResponse] =
    createIndex(amhReferenceIndexDefinition)


  private def createIndex(indexDefinition: CreateIndexDefinition)
  : (ElasticClient) => Future[CreateIndexResponse] =
  (client: ElasticClient) => client.execute {
    indexDefinition
  }
}