package com.groupaxis.groupsuite.routing.write.domain.model.routing.message.partner

import com.groupaxis.groupsuite.persistence.driver.DBDriver

trait MessagePartnerDAO extends DBDriver {

  // Import the query language features from the driver
  import driver.api._

  protected class MessagePartners(tag: Tag) extends Table[MessagePartner](tag, "sbs_messagepartner") {
    def name = column[String]("identifiername", O.PrimaryKey)

    def env = column[String]("env", O.PrimaryKey)

    def version = column[String]("version", O.PrimaryKey)

    def description = column[Option[String]]("description")

    def connectionMethod = column[Option[String]]("connectionmethod")

    def authenticationRequired = column[Option[String]]("authenticationrequired")

    def allowedDirection = column[Option[String]]("alloweddirection")

    def alwaysTransMacPac = column[Option[String]]("alwaystransfermacpac")

    def transPKISignature = column[Option[String]]("transferpkisignature")

    def incSeqAcrossSession = column[Option[String]]("incrementseqaccrosssession")

    def assignedExitPointName = column[Option[String]]("assignedexitpointname")

    def routingCodeTransmitted = column[Option[String]]("routingcodetransmitted")

    def messageEmissionFormat = column[Option[String]]("messageemissionformat")

    def notificationIncOriMsg = column[Option[String]]("notificationincludesorimsg")

    def originalMessageFormat = column[Option[String]]("originalmessageformat")

    def transferUUMID = column[Option[String]]("transferuumid")

    def language = column[Option[String]]("lang")

    def profileName = column[Option[String]]("profilename")

    private type MessagePartnerTupleType = (String, String, String, Option[String], Option[String], Option[String]
      , Option[String], Option[String], Option[String], Option[String], Option[String], Option[String]
      , Option[String], Option[String], Option[String], Option[String], Option[String], Option[String])

    private val messagePartnerShapedValue = (
      name,
      env,
      version,
      description,
      connectionMethod,
      authenticationRequired,
      allowedDirection,
      alwaysTransMacPac,
      transPKISignature,
      incSeqAcrossSession,
      assignedExitPointName,
      routingCodeTransmitted,
      messageEmissionFormat,
      notificationIncOriMsg,
      originalMessageFormat,
      transferUUMID,
      language,
      profileName
      ).shaped[MessagePartnerTupleType]

    private val toMessagePartnerRow: (MessagePartnerTupleType => MessagePartner) = messagePartnerTuple
    => MessagePartner(messagePartnerTuple._1, messagePartnerTuple._4, messagePartnerTuple._5,
        messagePartnerTuple._6, messagePartnerTuple._7, messagePartnerTuple._8, messagePartnerTuple._9, messagePartnerTuple._10,
        messagePartnerTuple._11, messagePartnerTuple._12, messagePartnerTuple._13, messagePartnerTuple._14, messagePartnerTuple._15,
        messagePartnerTuple._16, messagePartnerTuple._17, messagePartnerTuple._18)

    private val toMessagePartnerTuple: (MessagePartner => Option[MessagePartnerTupleType]) = { messagePartnerRow =>
      Some((messagePartnerRow.name, "UNKNOWN", "UNKNOWN", messagePartnerRow.description, messagePartnerRow.connectionMethod
        , messagePartnerRow.authenticationRequired, messagePartnerRow.allowedDirection, messagePartnerRow.alwaysTransMacPac
        , messagePartnerRow.transPKISignature, messagePartnerRow.incSeqAcrossSession, messagePartnerRow.assignedExitPointName
        , messagePartnerRow.routingCodeTransmitted, messagePartnerRow.messageEmissionFormat, messagePartnerRow.notificationIncOriMsg
        , messagePartnerRow.originalMessageFormat, messagePartnerRow.transferUUMID, messagePartnerRow.language, messagePartnerRow.profileName))
    }

    def * = messagePartnerShapedValue <> (toMessagePartnerRow, toMessagePartnerTuple)

  }

  val messagePartners = TableQuery[MessagePartners]
}

object MessagePartnerDAO extends MessagePartnerDAO {
  def apply = MessagePartnerDAO
}


