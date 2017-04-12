package com.groupaxis.groupsuite.routing.amh.util

import com.groupaxis.groupsuite.routing.write.domain.model.amh.distribution.copy.AMHDistributionCpyRuleEntity
import com.groupaxis.groupsuite.routing.write.domain.model.amh.rule.AMHRule
import org.apache.logging.log4j.scala.Logging

object RuleUtil {

  def getRemovedAddedPair(original : Seq[AMHRule], updated : Seq[AMHRule]) : (Option[Seq[AMHRule]], Option[Seq[AMHRule]]) = {

   val removed = original.filterNot(updated.toSet)
   val added = updated.filterNot(original.toSet)
    (if (removed.nonEmpty) Some(removed) else None,
     if (added.nonEmpty) Some(added) else None)
  }

}

object test extends App with Logging {
val o = Seq(AMHDistributionCpyRuleEntity("1", 1, "toto", None, None, "", "", None),
    AMHDistributionCpyRuleEntity("2", 1, "tota", None, None, "", "", None),
    AMHDistributionCpyRuleEntity("3", 1, "tote", None, None, "", "", None))

val u = Seq(AMHDistributionCpyRuleEntity("1", 1, "toto", None, None, "", ""),
    AMHDistributionCpyRuleEntity("2", 1, "tota", None, None, "", ""),
    AMHDistributionCpyRuleEntity("3", 1, "tote", None, None, "", ""),
  AMHDistributionCpyRuleEntity("4", 1, "totu", None, None, "", ""))


  val (r, a) = RuleUtil.getRemovedAddedPair(o,u)

  logger.debug(s" $r  <=> $a")
}
