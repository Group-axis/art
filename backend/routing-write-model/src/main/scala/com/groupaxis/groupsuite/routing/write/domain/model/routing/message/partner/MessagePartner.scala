package com.groupaxis.groupsuite.routing.write.domain.model.routing.message.partner

case class MessagePartner(name: String, description: Option[String]
                          , connectionMethod: Option[String], authenticationRequired: Option[String]
                          , allowedDirection: Option[String], alwaysTransMacPac: Option[String]
                          , transPKISignature: Option[String], incSeqAcrossSession: Option[String]
                          , assignedExitPointName: Option[String], routingCodeTransmitted: Option[String]
                          , messageEmissionFormat: Option[String], notificationIncOriMsg: Option[String]
                          , originalMessageFormat: Option[String], transferUUMID: Option[String]
                          , language: Option[String], profileName: Option[String]) {

  def toES = MessagePartnerES(name, description)
}

case class MessagePartnerUpdate(description: Option[String] = None
                                , connectionMethod: Option[String] = None, authenticationRequired: Option[String] = None
                                , allowedDirection: Option[String] = None, alwaysTransMacPac: Option[String] = None
                                , transPKISignature: Option[String] = None, incSeqAcrossSession: Option[String] = None
                                , assignedExitPointName: Option[String] = None, routingCodeTransmitted: Option[String] = None
                                , messageEmissionFormat: Option[String] = None, notificationIncOriMsg: Option[String] = None
                                , originalMessageFormat: Option[String] = None, transferUUMID: Option[String] = None
                                , language: Option[String] = None, profileName: Option[String] = None) {

  def merge(messagePartner: MessagePartner): MessagePartner =
    MessagePartner(messagePartner.name, description.orElse(messagePartner.description)
      , connectionMethod.orElse(messagePartner.connectionMethod), authenticationRequired.orElse(messagePartner.authenticationRequired)
      , allowedDirection.orElse(messagePartner.allowedDirection), alwaysTransMacPac.orElse(messagePartner.alwaysTransMacPac)
      , transPKISignature.orElse(messagePartner.transPKISignature), incSeqAcrossSession.orElse(messagePartner.incSeqAcrossSession)
      , assignedExitPointName.orElse(messagePartner.assignedExitPointName), routingCodeTransmitted.orElse(messagePartner.routingCodeTransmitted)
      , messageEmissionFormat.orElse(messagePartner.messageEmissionFormat), notificationIncOriMsg.orElse(messagePartner.notificationIncOriMsg)
      , originalMessageFormat.orElse(messagePartner.originalMessageFormat), transferUUMID.orElse(messagePartner.transferUUMID)
      , language.orElse(messagePartner.language), profileName.orElse(messagePartner.profileName)
    )

  def merge(name: String): MessagePartner =
    MessagePartner(name, description, connectionMethod, authenticationRequired
      , allowedDirection, alwaysTransMacPac, transPKISignature, incSeqAcrossSession
      , assignedExitPointName, routingCodeTransmitted, messageEmissionFormat, notificationIncOriMsg
      , originalMessageFormat, transferUUMID, language, profileName)
}

case class MessagePartnerES(name: String, description: Option[String])