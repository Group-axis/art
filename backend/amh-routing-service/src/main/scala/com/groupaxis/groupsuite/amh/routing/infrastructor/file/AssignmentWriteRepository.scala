package com.groupaxis.groupsuite.amh.routing.infrastructor.file

import com.groupaxis.groupsuite.xml.parser.amh.writer.jdbc.{JdbcAMHAssignmentRepository, JdbcAMHAssignmentRuleRepository}

import scala.concurrent.Future

class AssignmentWriteRepository(val jdbcAssignmentRepo: JdbcAMHAssignmentRepository, val jdbcAssignmentRuleRepo: JdbcAMHAssignmentRuleRepository) {

  def unAssignRule(ruleCode: String): Future[Int] = {
    jdbcAssignmentRuleRepo.deleteAssignmentRulesByRuleCode(ruleCode)
  }

}
