package com.groupaxis.groupsuite.routing.write

import com.groupaxis.groupsuite.routing.write.domain.model.routing.exit.point.{ExitPoint, ExitPointES}
import com.groupaxis.groupsuite.routing.write.domain.model.routing.message.partner.{MessagePartner, MessagePartnerES}
import com.groupaxis.groupsuite.routing.write.domain.model.routing.point.{Point, PointES}
import com.groupaxis.groupsuite.routing.write.domain.model.routing.schema.{Schema, SchemaES}

package object domain {
  /**********************************************/
  /*  TYPES                                     */
  /**********************************************/
  sealed trait DomainRequest
  sealed trait DomainResponse


  case class ImportSAARouting(points : Seq[Point], exitPoints : Seq [ExitPoint], messagePartners : Seq[MessagePartner], schemas: Seq[Schema])
  case class ImportSAARoutingES(points : Seq[PointES], exitPoints : Seq [ExitPointES], messagePartners : Seq[MessagePartnerES], schemas : Seq[SchemaES])

  case class SAARoutingImported(points : Seq[Point], exitPoints : Seq [ExitPoint], messagePartners : Seq[MessagePartner], schemas : Seq[Schema]) {
    def toImportSAARoutingES = ImportSAARoutingES(points.map(_.toES), exitPoints.map(_.toES), messagePartners.map(_.toES), schemas.map(_.toES))
  }
  case class SAARoutingESImported(points : Seq[PointES], exitPoints : Seq [ExitPointES], messagePartners : Seq[MessagePartnerES], schemas : Seq[SchemaES])

  /**********************************************/
  /*  MARSHALLERS                               */
  /**********************************************/



}