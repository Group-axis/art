package com.groupaxis.groupsuite.user.es.schema

import com.groupaxis.groupsuite.persistence.datastore.es.util.GPESHelper
import com.sksamuel.elastic4s.analyzers._
import com.sksamuel.elastic4s.mappings.FieldType.{BooleanType, LongType, StringType}
import com.sksamuel.elastic4s.{CreateIndexDefinition, ElasticClient}
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse

import scala.concurrent.Future

object UserSchema {

  import com.sksamuel.elastic4s.ElasticDsl._

  val userIndexName = "authentication"

 private val createUserIndexDefinition =
   create index userIndexName analysis (GPESHelper.GPAutocompleteAnalyzer, GPESHelper.GPIdAnalyzer) mappings(
      "routingusers" fields(
        "username" typed StringType analyzer "autocomplete",
        "oldPassword" typed StringType index NotAnalyzed,
        "newPassword" typed StringType index NotAnalyzed,
        "firstName" typed StringType analyzer "autocomplete",
        "lastName" typed StringType analyzer "autocomplete",
        "active" typed BooleanType index NotAnalyzed,
        "profiles" typed StringType analyzer "autocomplete",
        "permissions" typed StringType index NotAnalyzed),
      "routingprofiles" fields(
        "active" typed BooleanType index NotAnalyzed,
        "id" typed LongType index NotAnalyzed,
        "name" typed StringType analyzer KeywordAnalyzer,
        "permissions" typed StringType index NotAnalyzed
        )
      ) shards 3

      def createUserIndex
  : (ElasticClient) => Future[CreateIndexResponse] =
    createIndex(createUserIndexDefinition)


  private def createIndex(indexDefinition: CreateIndexDefinition)
  : (ElasticClient) => Future[CreateIndexResponse] =
  (client: ElasticClient) => client.execute {
    indexDefinition
  }

}

