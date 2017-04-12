package com.groupaxis.groupsuite.routing.write.domain.model.routing.keyword

case class KeywordEntity(name: String, keywordType: String, description: Option[String]) {

  def toES = KeywordES(name, keywordType, description)
}

case class KeywordUpdate(keywordType: String, description: Option[String] = None) {

  def merge(keyword: KeywordEntity): KeywordEntity = KeywordEntity(keyword.name, keywordType, description.orElse(keyword.description))

  def merge(name: String, keywordType: String): KeywordEntity =
    KeywordEntity(name, keywordType, description)
}

case class KeywordES(name: String, keywordType: String, description: Option[String])