package com.groupaxis.groupsuite.routing.write.domain.model.routing.keyword


import com.groupaxis.groupsuite.persistence.driver.DBDriver

trait KeywordDAO extends DBDriver {

  // Import the query language features from the driver
  import driver.api._

  protected class Keywords(tag: Tag) extends Table[KeywordEntity](tag, "sbs_routingkeyword") {
    def name = column[String]("name", O.PrimaryKey)
    def keywordType = column[String]("type", O.PrimaryKey)
    def env = column[String]("ENV", O.PrimaryKey)
    def version = column[String]("VERSION", O.PrimaryKey)
    def description = column[Option[String]]("description")

    private type KeywordEntityTupleType = (String, String, String, String, Option[String])

    private val keywordShapedValue = (
      name,
      keywordType,
      env,
      version,
      description
      ).shaped[KeywordEntityTupleType]

    private val toKeywordRow: (KeywordEntityTupleType => KeywordEntity) = keywordTuple =>  KeywordEntity(keywordTuple._1, keywordTuple._2, keywordTuple._5)

    private val toKeywordTuple: (KeywordEntity => Option[KeywordEntityTupleType]) = { keywordRow =>
      Some((keywordRow.name,keywordRow.keywordType, "UNKNOWN", "UNKNOWN", keywordRow.description))
    }

    def * = keywordShapedValue <> (toKeywordRow, toKeywordTuple)

  }

  val keywords = TableQuery[Keywords]
}

object KeywordDAO extends KeywordDAO {
  def apply = KeywordDAO
}


