package com.groupaxis.groupsuite.routing.write.domain.model.routing.message.partner

trait MessagePartnerRequest
trait MessagePartnerResponse

object MessagePartnerMessages {

  //commands
  case class CreateMessagePartner(id: String, messagePartner: MessagePartnerUpdate) extends MessagePartnerRequest
  case class UpdateMessagePartner(id: String, messagePartner: MessagePartnerUpdate) extends MessagePartnerRequest
  case class FindMessagePartnerById(id: String) extends MessagePartnerRequest

  //ES
  case class CreateMessagePartnersES(messagePartners: Seq[MessagePartnerES]) extends MessagePartnerRequest

  //events
  case class MessagePartnerFound(assignment: Option[MessagePartner]) extends MessagePartnerResponse
  case class MessagePartnerCreated(response : MessagePartner) extends MessagePartnerResponse
  case class MessagePartnerUpdated(response : MessagePartner) extends MessagePartnerResponse
  case class MessagePartnersFound(assignments: Option[Seq[MessagePartner]]) extends MessagePartnerResponse

  //ES
  case class MessagePartnersCreatedES(response : Seq[MessagePartnerES]) extends MessagePartnerResponse
}
