package com.groupaxis.groupsuite.persistence.datastore.es.util

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.analyzers.{CustomAnalyzerDefinition, LowercaseTokenFilter, NGramTokenFilter, WhitespaceTokenizer}
import org.apache.logging.log4j.scala.Logging
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}


object GPESHelper extends Logging {

  import com.sksamuel.elastic4s.ElasticDsl._

  private def deleteESIndex(indexName: String): (ElasticClient) => Future[DeleteIndexResponse] =
    (client) => client.execute {
      delete index indexName
    }

  def initializeIndex(indexName: String, createAction: (ElasticClient) => Future[CreateIndexResponse]): (ElasticClient) => Future[CreateIndexResponse] = {
    (client) => {
      val initializeIndex = for {
        delete <- Try(deleteESIndex(indexName)(client))
        create <- Try(createAction(client))
      } yield create

      initializeIndex match {
        case Success(f) =>
          logger.debug(s" $indexName index  not created yet O.K.")
          f
        case Failure(ex) =>
          logger.error(s" $indexName index initialization failed with :${ex.getMessage}")
          Future.failed(ex)
      }
    }
  }
  val GPAutocompleteAnalyzer = CustomAnalyzerDefinition(
    "autocomplete",
    WhitespaceTokenizer,
    LowercaseTokenFilter,
    NGramTokenFilter("autocomplete_filter", 1, 50)
  )

  val GPIdAnalyzer = CustomAnalyzerDefinition(
    "id_analyzer",
    WhitespaceTokenizer,
    LowercaseTokenFilter
  )
}
