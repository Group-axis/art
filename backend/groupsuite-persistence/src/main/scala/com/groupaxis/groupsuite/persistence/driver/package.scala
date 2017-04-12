package com.groupaxis.groupsuite.persistence

package object driver {
  trait DBDriver {
    val driver = slick.driver.PostgresDriver
  }
}