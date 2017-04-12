package com.groupaxis.groupsuite.xml.parser.routing.infrastructure.util

import com.groupaxis.groupsuite.routing.write.domain.model.routing.exit.point.ExitPoint
import com.groupaxis.groupsuite.routing.write.domain.model.routing.message.partner.MessagePartner
import com.groupaxis.groupsuite.routing.write.domain.model.routing.point.Point
import com.groupaxis.groupsuite.routing.write.domain.model.routing.rule.{RuleAction, RuleCondition, RuleEntity}
import com.groupaxis.groupsuite.routing.write.domain.model.routing.schema.Schema

import scala.xml.{Atom, Elem, Node, NodeSeq}

object XmlHelper {

  def getMessagePartners(messagePartners : NodeSeq) :Seq[MessagePartner] = messagePartners.map(toMessagePartner)

  def toString(nodeSeq : NodeSeq) : String = nodeSeq.text
  def toOptStr(nodeSeq : NodeSeq) : Option[String] = toOption(nodeSeq.text)
  def toOptLong(nodeSeq : NodeSeq) : Option[Long] = try toOption(nodeSeq.text).map(_.toLong) catch {case e:Exception => None }
  def toOptBool(nodeSeq : NodeSeq) : Option[Boolean] = try toOption(nodeSeq.text).map(_.toBoolean) catch {case e:Exception => None }


  def toMessagePartner(node : Node) : MessagePartner = MessagePartner(
    toString(node \ "Identifier" \ "Name"),
    toOptStr(node \ "description"),
    toOptStr(node \ "ConnectionMethod"),
    toOptStr(node \ "AuthenticationRequired"),
    toOptStr(node \ "AllowedDirection"),
    toOptStr(node \ "EmissionDetails" \ "AlwaysTransferMacPac"),
    toOptStr(node \ "EmissionDetails" \ "TransferPkiSignature"),
    toOptStr(node \ "EmissionDetails" \ "IncrementSequenceAccrossSession"),
    toOptStr(node \ "EmissionDetails" \ "AccessEmissionDetails" \ "AssignedExitPoints" \ "Name"),
    toOptStr(node \ "EmissionDetails" \ "AccessEmissionDetails" \  "RoutingCodeTransmitted"),
    toOptStr(node \ "EmissionDetails" \ "AccessEmissionDetails" \  "MessageEmissionFormat"),
    toOptStr(node \ "EmissionDetails" \ "AccessEmissionDetails" \  "NotificationIncludesOriginalMessage"),
    toOptStr(node \ "EmissionDetails" \ "AccessEmissionDetails" \  "OriginalMessageFormat"),
    toOptStr(node \ "EmissionDetails" \ "AccessEmissionDetails" \  "TransferUUMID"),
    toOptStr(node \ "EmissionDetails" \ "Language"),
    toOptStr(node \ "Profile" \ "Name")
  )

  def getSchemas(schemas : NodeSeq) :Seq[Schema] = schemas.map(toSchema)

  def toSchema(node : Node) : Schema = Schema(
    toString(node \ "Identifier" \ "Name"),
    toOptStr(node \ "Description")
  )

  def getExitPoints(exitPoints : NodeSeq) :Seq[ExitPoint] = exitPoints.map(toExitPoint)

  def toExitPoint(node : Node) : ExitPoint = ExitPoint(
    toString(node \ "Identifier" \ "Name"),
    toString(node \ "QueueType"),
    toOptLong(node \ "QueueThreshold"),
    toOptStr(node \ "MessagePartner" \ "Name"),
    toOptBool(node \ "RoutingInfo" \ "RulesVisible"),
    toOptBool(node \ "RoutingInfo" \ "RulesModifiable")
  )

  def getRoutingPoints(nodePoints: NodeSeq) : Seq[Point] = nodePoints.map(routing => toRoutingPoint(routing))

  def toRoutingPoint(node: Node): Point = Point((node \ "RoutingPointName").text, (node \ "Full").text.toBoolean, toRules(node \ "Rule", (node \ "RoutingPointName").text, toOption((node \ "Full").text)))

  def toRules(rules: NodeSeq, pointName: String, full: Option[String]): Seq[RuleEntity] = rules.map(rule => toRule(rule, pointName, full))

  def toRule(node: Node, pointName: String, full: Option[String]): RuleEntity =
    RuleEntity(
      (node \ "SequenceNumber").text.toLong,
      pointName,
      full,
      toOption((node \ "Description" \ "RuleDescription").text),
      toOption((node \ "Description" \ "SchemaMap").text),
      action = RuleAction(
        toOption((node \ "Action" \ "ActionOn").text),
        toOption((node \ "Action" \ "SourceInstanceRule" \ "InstanceAction").text),
        toOption((node \ "Action" \ "SourceInstanceRule" \ "InstanceInterventionType").text),
        toOption((node \ "Action" \ "SourceInstanceRule" \ "InstanceInterventionTypeText").text),
        toOption((node \ "Action" \ "SourceInstanceRule" \ "InstanceRoutingCode").text),
        toOption((node \ "Action" \ "SourceInstanceRule" \ "InstanceTargetQueue" \ "Name").text),
        toOption((node \ "Action" \ "SourceInstanceRule" \ "InstanceUnit").text),
        toOption((node \ "Action" \ "SourceInstanceRule" \ "InstancePriority").text),
        toOption((node \ "Action" \ "NewInstanceRule" \ "InstanceAction").text),
        toOption((node \ "Action" \ "NewInstanceRule" \ "InstanceRoutingCode").text),
        toOption((node \ "Action" \ "NewInstanceRule" \ "InstanceInterventionType").text),
        toOption((node \ "Action" \ "NewInstanceRule" \ "InstanceInterventionTypeText").text),
        toOption((node \ "Action" \ "NewInstanceRule" \ "InstanceTargetQueue" \ "Name").text),
        toOption((node \ "Action" \ "NewInstanceType").text),
        toOption((node \ "Action" \ "NewInstanceRule" \ "InstanceUnit").text),
        toOption((node \ "Action" \ "NewInstanceRule" \ "InstancePriority").text)),
      condition = RuleCondition(
        toOption((node \ "Condition" \ "ConditionOn").text),
        toOption(removeCharacters((node \ "Condition" \ "Criteria").text,Seq("\n","\r"))),
        toOption(toFunctions(node \ "Condition" \ "FunctionResult"))
      ))


  def toFunctions(functions: NodeSeq) : String = {
    if (functions.isEmpty) "" else
       functions.map(node => node.text).reduceLeft[String]((r, c) => c+","+r)

  }

  def toOption(text: String) = {
    text match {
      case value: String if value.isEmpty => None
      case nonEmptyValue: String          => Some(nonEmptyValue)
    }
  }

  def removeCharacters(text: String, chars : Seq[String]) : String = {
    chars.foldLeft(text)((acc, value) => acc.replaceAll(value, ""))
  }

  /****************************  WRITE METHODS ****************************/


  implicit def optionElem(e: Elem) = new {
    def ? : NodeSeq = {
      require(e.child.length == 1)
      e.child.head match {
        case atom: Atom[Option[_]] => atom.data match {
          case None => NodeSeq.Empty
          case Some(x) => e.copy(child = x match {
            case n: NodeSeq => n
            case x          => new Atom(x)
          })
        }
        case _ => e
      }
    }
  }

  def toXmlPoints(points: Iterable[Option[Point]]) : Node = {
    val rules : Iterable[Node]= points.map(toXmlPoint)
    <RoutingRuleData xmlns="urn:swift:saa:xsd:impex:routing">
      {rules}
    </RoutingRuleData>
  }

  def toXmlPoint(point: Option[Point]) : Node = {
    point.map( p => {
      val xmlRules : Seq[Node] = p.rules.map( XmlHelper.toXmlRule)
          <RoutingPointRules xmlns="urn:swift:saa:xsd:impex:routing" xmlns:ns2="urn:swift:saa:xsd:routingpoint" xmlns:ns3="urn:swift:saa:xsd:unit" xmlns:ns4="urn:swift:saa:xsd:messagePartner">
          <RoutingPointName>{ p.pointName }</RoutingPointName>
          <Full>{ p.full }</Full>
          {xmlRules}
        </RoutingPointRules>
    }) match {
      case Some(node) => node
    }
  }

  //TODO: make functionResult a list
  //TODO: Add Unit and Priority
  def toXmlRule(rule: RuleEntity): Node = {
    <Rule>
      <SequenceNumber>{ rule.sequence }</SequenceNumber>
      <Description>
        <ns2:RuleDescription>{ rule.ruleDescription.getOrElse("") }</ns2:RuleDescription>
        <ns2:SchemaMap>{ rule.schemaMap.getOrElse("") }</ns2:SchemaMap>
      </Description>
      <Condition>
        <ns2:ConditionOn>{ rule.condition.conditionOn.getOrElse("") }</ns2:ConditionOn>
        { <ns2:Criteria>{ rule.condition.criteria }</ns2:Criteria>? }
        { <ns2:FunctionResult>{ rule.condition.functions }</ns2:FunctionResult>? }
      </Condition>

      {
      rule.action.actionOn match {
        case Some("SOURCE") =>
          <Action>
            <ns2:ActionOn>{ rule.action.actionOn.getOrElse("") }</ns2:ActionOn>
            <ns2:SourceInstanceRule>
              { <ns2:InstanceAction>{ rule.action.instanceAction }</ns2:InstanceAction>? }
              {target(rule.action.instanceTargetQueue)}
              {<ns2:InstanceInterventionType>{rule.action.instanceInterventionType}</ns2:InstanceInterventionType>?}
              {<ns2:InstanceInterventionTypeText>{rule.action.instanceInterventionTypeText}</ns2:InstanceInterventionTypeText>?}
              {<ns2:InstanceRoutingCode>{rule.action.instanceRoutingCode}</ns2:InstanceRoutingCode>?}
            </ns2:SourceInstanceRule>
          </Action>
        case Some("NEW_INSTANCE") =>
          <Action>
            <ns2:ActionOn>{ rule.action.actionOn.getOrElse("") }</ns2:ActionOn>
            {<ns2:NewInstanceType>{rule.action.newInstanceType}</ns2:NewInstanceType>?}
            <ns2:NewInstanceRule>
              { <ns2:InstanceAction>{ rule.action.newInstanceAction }</ns2:InstanceAction>? }
              {target(rule.action.newInstanceTargetQueue)}
              {<ns2:InstanceInterventionType>{rule.action.newInstanceInterventionType}</ns2:InstanceInterventionType>?}
              {<ns2:InstanceInterventionTypeText>{rule.action.newInstanceInterventionTypeText}</ns2:InstanceInterventionTypeText>?}
              {<ns2:InstanceRoutingCode>{rule.action.newInstanceRoutingCode}</ns2:InstanceRoutingCode>?}
            </ns2:NewInstanceRule>
          </Action>
        case Some("SOURCE_AND_NEW_INSTANCE") =>
          <Action>
            <ns2:ActionOn>{ rule.action.actionOn.getOrElse("") }</ns2:ActionOn>
            <ns2:SourceInstanceRule>
              { <ns2:InstanceAction>{ rule.action.instanceAction }</ns2:InstanceAction>? }
              {target(rule.action.instanceTargetQueue)}
              {<ns2:InstanceInterventionType>{rule.action.instanceInterventionType}</ns2:InstanceInterventionType>?}
              {<ns2:InstanceInterventionTypeText>{rule.action.instanceInterventionTypeText}</ns2:InstanceInterventionTypeText>?}
              {<ns2:InstanceRoutingCode>{rule.action.instanceRoutingCode}</ns2:InstanceRoutingCode>?}
            </ns2:SourceInstanceRule>
            {<ns2:NewInstanceType>{rule.action.newInstanceType}</ns2:NewInstanceType>?}
            <ns2:NewInstanceRule>
              { <ns2:InstanceAction>{ rule.action.newInstanceAction }</ns2:InstanceAction>? }
              {target(rule.action.newInstanceTargetQueue)}
              {<ns2:InstanceInterventionType>{rule.action.newInstanceInterventionType}</ns2:InstanceInterventionType>?}
              {<ns2:InstanceInterventionTypeText>{rule.action.newInstanceInterventionTypeText}</ns2:InstanceInterventionTypeText>?}
              {<ns2:InstanceRoutingCode>{rule.action.newInstanceRoutingCode}</ns2:InstanceRoutingCode>?}
            </ns2:NewInstanceRule>
          </Action>
        case _=>
      }
      }

    </Rule>
  }
  def toInstanceAction(action: Option[String], interventionType: Option[String], interventionTypeText: Option[String], routingCode: Option[String],
                       targetQueue: Option[String], unit: Option[String], priority: Option[String]) : NodeSeq = {

    { <ns2:InstanceAction>{ action }</ns2:InstanceAction>? }
    {target(targetQueue)}
    {<ns2:InstanceInterventionType>{interventionType}</ns2:InstanceInterventionType>?}
    {<ns2:InstanceInterventionTypeText>{interventionTypeText}</ns2:InstanceInterventionTypeText>?}
    {<ns2:InstanceRoutingCode>{routingCode}</ns2:InstanceRoutingCode>?}

  }

  def target(t: Option[String]): NodeSeq = {
    {
      t match {
        case Some(v) => <ns2:InstanceTargetQueue>
          <ns2:Name>{ v }</ns2:Name>
        </ns2:InstanceTargetQueue>
        case _ => NodeSeq.Empty
      }
    }
  }



}
