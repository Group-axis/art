package com.groupaxis.groupsuite.routing.write.domain.model.amh.criteria

import com.groupaxis.groupsuite.persistence.driver.DBDriver
import com.groupaxis.groupsuite.persistence.util.TableAudit

trait AMHRuleDAO extends DBDriver {

  // Import the query language features from the driver
  import driver.api._

  protected abstract class AMHRuleCriteria(tag: Tag) extends Table[AMHRuleCriteriaEntity](tag, "AMH_GW_RU_CRIT") with TableAudit {
  }
}

