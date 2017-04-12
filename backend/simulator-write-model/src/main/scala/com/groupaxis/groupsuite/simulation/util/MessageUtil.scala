package com.groupaxis.groupsuite.simulation.util

import com.groupaxis.groupsuite.simulator.write.domain.model.mapping.MappingEntity
import com.groupaxis.groupsuite.simulator.write.domain.model.mapping.MappingMessages.MappingsFound
import org.apache.logging.log4j.scala.Logging

object MessageUtil extends Logging {

  def getMappings(msgType: Int, mappings: Either[String, Seq[MappingEntity]]) : Map[String, String] = {

    //Mappings from DB
    mappings.fold(
      error => {
        logger.error(error)
        Map[String, String]()
      },
      mappingsFound => mappingsFound
.foldLeft(Map[String, String]()) {
          (m, me) => {

            val expReg = msgType match {
              case 1 => me.mxRegExp.getOrElse("no_value")
              case 2 => me.mtRegExp.getOrElse("no_value")
              case _ => "no_value"
            }

            m + (me.keyword -> expReg)}
        }
    )


  }

}
