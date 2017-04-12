package com.groupaxis.groupsuite.routing.write.domain.model.routing.exit.point

case class ExitPoint(name: String, queueType: String, queueThreshold: Option[Long]
                     , messagePartner: Option[String], rulesVisible: Option[Boolean]
                     , rulesModifiable: Option[Boolean]) {

  def toES = ExitPointES(name, queueType, queueThreshold, messagePartner, rulesVisible, rulesModifiable)
}

case class ExitPointUpdate(queueThreshold: Option[Long] = None
                           , messagePartner: Option[String] = None, rulesVisible: Option[Boolean] = None
                           , rulesModifiable: Option[Boolean] = None) {

  def merge(exitPoint: ExitPoint): ExitPoint = ExitPoint(exitPoint.name, exitPoint.queueType, queueThreshold.orElse(exitPoint.queueThreshold)
      , messagePartner.orElse(exitPoint.messagePartner), rulesVisible.orElse(exitPoint.rulesVisible)
      , rulesModifiable.orElse(exitPoint.rulesModifiable))

  def merge(name: String, queueType: String): ExitPoint =
    ExitPoint(name, queueType, queueThreshold, messagePartner, rulesVisible, rulesModifiable)
}

case class ExitPointES(name: String, queueType: String, queueThreshold: Option[Long] = None,
                       messagePartner: Option[String] = None, rulesVisible: Option[Boolean] = None
                       , rulesModifiable: Option[Boolean] = None)