package com.groupaxis.groupsuite.datastore.jdbc

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import slick.driver.JdbcDriver


  class Database(val driver: JdbcDriver, jdbcUrl: String, dbUser: String, dbPassword: String) {
  private val hikariConfig = new HikariConfig()
  hikariConfig.setJdbcUrl(jdbcUrl)
  hikariConfig.setUsername(dbUser)
  hikariConfig.setPassword(dbPassword)
  hikariConfig.setInitializationFailFast(false)
  hikariConfig.setMaximumPoolSize(5)

  private val dataSource = new HikariDataSource(hikariConfig)

  import driver.api._
  val db = Database.forDataSource(dataSource)
  db.createSession()
}
