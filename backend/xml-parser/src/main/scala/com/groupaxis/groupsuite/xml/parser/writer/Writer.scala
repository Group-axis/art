package com.groupaxis.groupsuite.xml.parser.writer

import com.groupaxis.groupsuite.datastore.jdbc.Database
import com.groupaxis.groupsuite.routing.infrastructor.jdbc.JdbcRuleWriteRepository
import com.groupaxis.groupsuite.routing.write.domain.model.routing.rule.{RuleDAO, RuleEntity}
import org.apache.logging.log4j.scala.Logging

import scala.xml.{Atom, Elem, Node, NodeSeq}
//import com.groupaxis.groupsuite.xml.parser.writer.es.RoutingRepository
import com.groupaxis.groupsuite.routing.write.domain.model.routing.point.Point
import com.groupaxis.groupsuite.routing.write.domain.model.routing.rule.{RuleAction, RuleCondition}

import scala.concurrent.duration._
//import com.groupaxis.groupsuite.commons.persistence.jdbc.DatabaseService
//import com.groupaxis.groupsuite.xml.parser.writer.jdbc.JdbcRoutingRepository
import scala.xml.XML

object Writer extends App with Logging {
//  val dataBase = new Database(slick.driver.PostgresDriver, "jdbc:oracle:thin:@78.215.201.21:1521:FAFWDEV01", "GRPDBA", "GRPDBA")
  val dataBase = new Database(slick.driver.PostgresDriver, "jdbc:postgresql://localhost:5432/postgres", "postgres", "postgres")
//  val jdbcDao = new RuleDAO(slick.driver.PostgresDriver)
  val jdbcRepo = new JdbcRuleWriteRepository( dataBase, 15.seconds)
  val resp = jdbcRepo.getAllRules
  resp.fold(
    ex => logger.error("error "+ex),
    allRules => {
      val pointMap : Map[String, Seq[RuleEntity]] = allRules.rules.groupBy(rule => rule.routingPointName)
      val points : Iterable[Option[Point]] = pointMap.map( kv => {
        val ruleOption = kv._2.headOption
        if (ruleOption.isDefined) {
          val rule = ruleOption.get
          Some(Point(rule.routingPointName, rule.full.exists(v => v.toBoolean), kv._2))
        } else {
          None
        }
      } )


   val size = points.size
      logger.debug(s" total points $size ")
    val rr = Util.toXmlPoints(points)
      val nSize = rr.length
      logger.debug(s" total nodes $nSize ")
     XML.save("c:/dev/DFS/AMH/saarouting_20160211T060001_points.xml", rr, "UTF-8", xmlDecl = true)

      //      val headRule = allRules.rules.headOption
      //      headRule.foreach(rule => {
      //        val point = Some(Point(rule.routingPointName, rule.full.exists(v => v.toBoolean), allRules.rules))
//      })
    }
  )
}

object test extends App {
  //val pp = new PrettyPrinter(80, 2)
  val rule = RuleEntity(25, "testrp", Some("true"), None, None,"","", RuleAction(Some("SOURCE_AND_NEW_INSTANCE"), Some("TT"), Some("TT"), Some("TT"), Some("TT"), Some("TT"), Some("TT"), Some("TT"), Some("TT"), Some("TT"), Some("TT"), Some("TT"), Some("TT"), Some("TT"), Some("TT"), Some("TT")),
            RuleCondition(Some("QQ"),Some("DD"),Some("FF,RR,VV")))
  
   XML.save("c:/dev/DFS/AMH/saarouting_20160211T060001_ooo.xml", Util.toXmlRule(rule), "UTF-8", true)

  }

 object Util {
   /* *********** HELPERS *****************************/

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
        val xmlRules : Seq[Node] = p.rules.map( Util.toXmlRule)
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
