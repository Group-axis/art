package com.groupaxis.groupsuite.persistence.datastore.jdbc

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import slick.driver.JdbcDriver


  class Database(val driver: JdbcDriver, jdbcUrl: String, dbUser: String, dbPassword: String) {
  private val hikariConfig = new HikariConfig()
  hikariConfig.setJdbcUrl(jdbcUrl)
  hikariConfig.setUsername(dbUser)
  hikariConfig.setPassword(dbPassword)
  hikariConfig.setInitializationFailFast(false)
  hikariConfig.setMaximumPoolSize(10)

  private val dataSource = new HikariDataSource(hikariConfig)

  import driver.api._
  val db = Database.forDataSource(dataSource)
//    val db =   Database.forURL("jdbc:h2:mem:test", driver = "org.h2.Driver")
  db.createSession()
}
