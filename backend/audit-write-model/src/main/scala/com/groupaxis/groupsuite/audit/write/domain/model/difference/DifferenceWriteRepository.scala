package com.groupaxis.groupsuite.audit.write.domain.model.difference

import scala.concurrent.Future

trait DifferenceWriteRepository {

  def getActionsByUser(username : String): Future[Seq[DifferenceEntity]]


}