package com.groupaxis.groupsuite.commons.persistence.jdbc

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import slick.driver.{JdbcDriver, JdbcProfile}
//import com.typesafe.slick.driver.oracle.OracleDriver
//import slick.driver.PostgresDriver

class DatabaseService(val driver: JdbcDriver, jdbcUrl: String, dbUser: String, dbPassword: String) {
  private val hikariConfig = new HikariConfig()
  hikariConfig.setJdbcUrl(jdbcUrl)
  hikariConfig.setUsername(dbUser)
  hikariConfig.setPassword(dbPassword)
  hikariConfig.setInitializationFailFast(false)

  private val dataSource = new HikariDataSource(hikariConfig)

//  val driver = OracleDriver
//  val driver = PostgresDriver
  import driver.api._
  val db = Database.forDataSource(dataSource)
  db.createSession()
}
