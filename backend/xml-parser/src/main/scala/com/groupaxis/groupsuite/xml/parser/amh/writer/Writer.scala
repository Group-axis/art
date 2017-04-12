package com.groupaxis.groupsuite.xml.parser.amh.writer

import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar

import scala.xml.{Elem, Node, NodeSeq}
import scala.xml.Atom
import com.groupaxis.groupsuite.routing.write.domain.model.routing.rule.RuleEntity
import com.groupaxis.groupsuite.xml.parser.writer.es.RoutingRepository
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.ElasticDsl._

import scala.concurrent._
import scala.concurrent.duration._
import ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import org.elasticsearch.action.delete.DeleteResponse
import com.sksamuel.elastic4s.SearchType
import org.apache.logging.log4j.scala.Logging
//import com.groupaxis.groupsuite.routing.write.domain.model.routing.point.Point2
import com.groupaxis.groupsuite.routing.write.domain.model.routing.point.PointES
import com.groupaxis.groupsuite.routing.write.domain.model.routing.rule.RuleCondition
import com.groupaxis.groupsuite.routing.write.domain.model.routing.rule.RuleAction
import com.groupaxis.groupsuite.commons.persistence.jdbc.DatabaseService
import com.groupaxis.groupsuite.routing.write.domain.model.amh.assignment.{AMHAssignmentEntity, AMHAssignmentRuleEntity, AssignmentDAO, AssignmentRuleDAO}
import com.groupaxis.groupsuite.routing.write.domain.model.amh.backend.{AMHBackendEntity, BackendDAO}
import com.groupaxis.groupsuite.routing.write.domain.model.amh.distribution.copy._
import com.groupaxis.groupsuite.routing.write.domain.model.amh.feedback.distribution.copy._
import com.groupaxis.groupsuite.routing.write.domain.model.amh.rule.{AMHRuleDAO, AMHRuleEntity}
import com.groupaxis.groupsuite.xml.parser.amh.writer.jdbc._
import com.groupaxis.groupsuite.xml.parser.writer.jdbc.JdbcRoutingRepository

import scala.collection.immutable.HashMap
import scala.concurrent.java8.FuturesConvertersImpl.P
//import scala.slick.backend
import scala.xml.XML
import scala.xml.TopScope
import scala.xml.Null
import scala.xml.Text

object dateFormat extends App with Logging {

  val today = Calendar.getInstance().getTime
  val minuteFormat = new SimpleDateFormat("yyyyMMddHHmmss")
  val currentMinute = minuteFormat.format(today)  // 29
  logger.debug("today : "+new SimpleDateFormat("yyyyMMddHHmmss").format(today))

}


object Writer extends App with Logging {
//  val dataBase = new DatabaseService(slick.driver.PostgresDriver, "jdbc:oracle:thin:@78.215.201.21:1521:FAFWDEV01", "GRPDBA", "GRPDBA")
  val dataBase = new DatabaseService(slick.driver.PostgresDriver, "jdbc:postgresql://localhost:5432/postgres", "postgres", "postgres")

  logger.debug(" -------------   " + " BACKEND ------------")
  //  t.map(toAMHBackend).foreach(logger.debug)
  val backendDao : BackendDAO = BackendDAO(slick.driver.PostgresDriver)
  val jdbcBackendRepo = new JdbcAMHBackendRepository(backendDao, dataBase)
  val allBackends = jdbcBackendRepo.findAllBackends
  allBackends.foreach( backends => {
    backends.foreach{
      backend => {
        XML.save("c:/dev/DFS/AMH/tmp/AMHWizard.BackendConfiguration/export_"+backend.code.toUpperCase+".xml", Util.toXmlBackend(backend), "UTF-8", xmlDecl=true)
      }
    }
  })

  /******************    rules *****************/
  val ruleDao : AMHRuleDAO = AMHRuleDAO
  val jdbcRuleRepo = new JdbcAMHRuleRepository(ruleDao, dataBase)
  val allRules = jdbcRuleRepo.findAllNonDeletedRules
  allRules.foreach( rules => {
    rules.foreach{
      rule => {
        XML.save("c:/dev/DFS/AMH/tmp/Gateway.RuleCriteria/export_"+rule.code.toUpperCase+".xml", Util.toXmlRule(rule), "UTF-8", xmlDecl=true)
      }
    }
  })

  /******************    back assignment  *****************/
  val assignmentDao : AssignmentDAO = AssignmentDAO(slick.driver.PostgresDriver)
  val jdbcAssignmentRepo = new JdbcAMHAssignmentRepository(assignmentDao, dataBase)
  val assignmentRuleDao : AssignmentRuleDAO = AssignmentRuleDAO(slick.driver.PostgresDriver)
  val jdbcAssignmentRuleRepo = new JdbcAMHAssignmentRuleRepository(assignmentRuleDao, dataBase)

  val allAssignments = jdbcAssignmentRepo.findAllAssignments
  val allAssignmentRules = jdbcAssignmentRuleRepo.findAllAssignmentRules
  val ruleMap : Map[String, Seq[AMHAssignmentRuleEntity]] = allAssignmentRules.map(rules => rules.groupBy(_.code)).getOrElse(new HashMap())

  allAssignments.foreach( assignments => {
    assignments.foreach{
      assignment => {
        XML.save("c:/dev/DFS/AMH/tmp/Gateway.BackendChannelAssignmentSelectionTable/export_"+assignment.code.toUpperCase+".xml", Util.toAssignmentXml(assignment, ruleMap.get(assignment.code.toUpperCase)), "UTF-8", true)
      }
    }
  })

  /******************    FeedbackDistributionCopy  *****************/
  val feedbackDtnCpyDao : FeedbackDtnCpyDAO = FeedbackDtnCpyDAO(slick.driver.PostgresDriver)
  val jdbcFeedbackDtnCpyRepo = new JdbcAMHFeedbackDtnCpyRepository(feedbackDtnCpyDao, dataBase)
  val feedbackDtnCpyRuleDao : FeedbackDtnCpyRuleDAO = FeedbackDtnCpyRuleDAO(slick.driver.PostgresDriver)
  val jdbcFeedbackDtnCpyRuleRepo = new JdbcAMHFeedbackDtnCpyRuleRepository(feedbackDtnCpyRuleDao, dataBase)
  val feedbackDtnCpyBackendDao : FeedbackDtnCpyBackDAO = FeedbackDtnCpyBackDAO(slick.driver.PostgresDriver)
  val jdbcFeedbackDtnCpyBackendRepo = new JdbcAMHFeedbackDtnCpyBackendRepository(feedbackDtnCpyBackendDao, dataBase)

  val allFeedbackDtnCps = jdbcFeedbackDtnCpyRepo.findAllFeedbackDtnCps
  val allFeedbackDtnCpyRules = jdbcFeedbackDtnCpyRuleRepo.findAllFeedbackDtnCpyRules
  val allFeedbackDtnCpyBackends = jdbcFeedbackDtnCpyBackendRepo.findAllFeedbackDtnCpyBackends

  val feedbackRuleMap : Map[String, Seq[AMHFeedbackDistributionCpyRuleEntity]] = allFeedbackDtnCpyRules.map(rules => rules.groupBy(_.code.toUpperCase())).getOrElse(new HashMap())
  val feedbackBackendMap : Map[String, Seq[AMHFeedbackDistributionCpyBackendEntity]] = allFeedbackDtnCpyBackends.map(backends => backends.groupBy(_.code.toUpperCase())).getOrElse(new HashMap())

  allFeedbackDtnCps.foreach( feedbackDtnCpys => {
    feedbackDtnCpys.foreach{
      feedbackDtnCpy => {
        XML.save("c:/dev/DFS/AMH/tmp/Gateway.FeedbackDistributionCopySelectionTable/export_"+feedbackDtnCpy.code.toUpperCase+".xml", Util.toFeedbackDtnCpyXml(feedbackDtnCpy, feedbackBackendMap.get(feedbackDtnCpy.code.toUpperCase), feedbackRuleMap.get(feedbackDtnCpy.code.toUpperCase)), "UTF-8", true)
      }
    }
  })


  /******************    DistributionCopy  *****************/
  val distributionCpyDao : DistributionCpyDAO = DistributionCpyDAO(slick.driver.PostgresDriver)
  val jdbcDistributionCpyRepo = new JdbcAMHDistributionCpyRepository(distributionCpyDao, dataBase)
  val distributionCpyRuleDao : DistributionCpyRuleDAO = DistributionCpyRuleDAO(slick.driver.PostgresDriver)
  val jdbcDistributionCpyRuleRepo = new JdbcAMHDistributionCpyRuleRepository(distributionCpyRuleDao, dataBase)
  val distributionCpyBackendDao : DistributionCpyBackendDAO = DistributionCpyBackendDAO(slick.driver.PostgresDriver)
  val jdbcDistributionCpyBackendRepo = new JdbcAMHDistributionCpyBackendRepository(distributionCpyBackendDao, dataBase)

  val allDistributionCps = jdbcDistributionCpyRepo.findAllDistributionCps
  val allDistributionCpyRules = jdbcDistributionCpyRuleRepo.findAllDistributionCpyRules
  val allDistributionCpyBackends = jdbcDistributionCpyBackendRepo.findAllDistributionCpyBackends

  val distributionRuleMap : Map[String, Seq[AMHDistributionCpyRuleEntity]] = allDistributionCpyRules.map(rules => rules.groupBy(_.code.toUpperCase())).getOrElse(new HashMap())
  val distributionBackendMap : Map[String, Seq[AMHDistributionCpyBackendEntity]] = allDistributionCpyBackends.map(backends => backends.groupBy(_.code.toUpperCase())).getOrElse(new HashMap())

  allDistributionCps.foreach( distributionCpys => {
    distributionCpys.foreach{
      distributionCpy => {
        XML.save("c:/dev/DFS/AMH/tmp/Gateway.DistributionCopySelectionTable/export_"+distributionCpy.code.toUpperCase+".xml", Util.toDistributionCpyXml(distributionCpy, distributionBackendMap.get(distributionCpy.code.toUpperCase), distributionRuleMap.get(distributionCpy.code.toUpperCase)), "UTF-8", true)
      }
    }
  })

  /******************    ZIP creation *****************/
  val path = "c:/dev/DFS/AMH/tmp"
  val outputFilename = "c:/dev/DFS/AMH/tmp/output.zip"
  val file = new File(path)
  val filePaths = Zipper.createFileList(file, outputFilename)
  Zipper.createZip(filePaths, outputFilename, path)

//  val jdbcRepo = new JdbcRoutingRepository(dataBase)
//  val allRules = jdbcRepo.findRulesByRoutingPoint("Sib_ABNAGB22")
//  val point = allRules.map( all => Point2(all.head.routingPointName, all.head.full.map(v => v.toBoolean).getOrElse(false), all))
//  XML.save("c:/dev/DFS/AMH/saarouting_20160211T060001_point.xml", Util.toXmlPoint(point), "UTF-8", true)
}
 object Util extends Logging {

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

  def toXmlBackend(backend: AMHBackendEntity) : Node = {
      <BackendConfiguration xmlns="AMHWizard" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="AMHWizard AMHWizard.xsd">
      <BackendChannel>
        <primarykey model="Gateway" entity="BackendChannel">
          <Code>{backend.pkCode.getOrElse("")}</Code>
          <Direction>{backend.pkDirection.getOrElse("")}</Direction>
        </primarykey>
      </BackendChannel>
      <Code>{backend.code}</Code>
    { backend.dataOwner match {
    case Some(data) =>
      <DataOwner>{data}</DataOwner>
    case None =>
        <DataOwner/>
    }}
    { backend.description match {
    case Some(data) =>
        <Description>{data}</Description>
    case None =>
        <Description/>
  }}
    { backend.lockCode match {
    case Some(data) =>
      <LockCode>{data}</LockCode>
    case None =>
        <LockCode/>
   }}
    { backend.name match {
    case Some(data) =>
      <Name>{data}</Name>
    case None =>
        <Name/>
    }}
  </BackendConfiguration>
  }

   def toXmlRule(rule: AMHRuleEntity) : Node = {
     <RuleCriteria xmlns="Gateway" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="Gateway Gateway.xsd">
      <Code>{rule.code}</Code>
       { rule.dataOwner match {
       case Some(data) =>
          <DataOwner>{data}</DataOwner>
       case None =>
            <DataOwner/>
     }}
       { rule.expression match {
       case Some(data) =>
          <Expression>{data}</Expression>
       case None =>
            <Expression/>
     }}
       { rule.lockCode match {
       case Some(data) =>
          <LockCode>{data}</LockCode>
       case None =>
            <LockCode/>
     }}
       { rule.ruleType match {
       case Some(data) =>
          <Type>{data}</Type>
       case None =>
            <Type/>
     }}
     </RuleCriteria>
   }
   def toAssignmentXml(assignment : AMHAssignmentEntity, rules : Option[Seq[AMHAssignmentRuleEntity]]) : Node = {
     logger.debug("TREATING "+ assignment.code)
     val xmlRules : Seq[Node] = rules.getOrElse(List())map(Util.toXml)
     logger.debug("RULES  "+ xmlRules)
     <BackendChannelAssignmentSelectionTable xmlns="Gateway" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="Gateway Gateway.xsd">
       { assignment.active match {
       case Some(data) =>
         <Active>{data}</Active>
       case None =>
           <Active/>
     }}
       <AssignedBackendChannel>
         <primarykey model="Gateway" entity="BackendChannel">
           <Code>{assignment.backCode}</Code>
           <Direction>{assignment.backDirection}</Direction>
         </primarykey>
       </AssignedBackendChannel>
       <Code>{assignment.code}</Code>
    { assignment.dataOwner match {
    case Some(data) =>
     <DataOwner>{data}</DataOwner>
    case None =>
       <DataOwner/>
    }}
   { assignment.description match {
   case Some(data) =>
     <Description>{data}</Description>
   case None =>
       <Description/>
  }}
   { assignment.lockCode match {
   case Some(data) =>
     <LockCode>{data}</LockCode>
   case None =>
       <LockCode/>
 }}
       <Sequence>{assignment.sequence}</Sequence>
       <BackendChannelAssignmentRuleBackendChannelAssignmentRuleCriteria>
         {xmlRules}
       </BackendChannelAssignmentRuleBackendChannelAssignmentRuleCriteria>
     </BackendChannelAssignmentSelectionTable>
   }

   def toXml(assignmentRule : AMHAssignmentRuleEntity) : Node = {
     <BackendChannelAssignmentRuleCriteria>
       <Criteria>
         <primarykey model="Gateway" entity="RuleCriteria">
          <Code>BA-PARBFRPP-NBB-AEPP6</Code>
         </primarykey>
       </Criteria>
       { assignmentRule.dataOwner match {
       case Some(data) =>
         <DataOwner>{data}</DataOwner>
       case None =>
           <DataOwner/>
     }}
       { assignmentRule.lockCode match {
       case Some(data) =>
         <LockCode>{data}</LockCode>
       case None =>
           <LockCode/>
     }}
       <SequenceNumber>{assignmentRule.sequence}</SequenceNumber>
     </BackendChannelAssignmentRuleCriteria>
   }

   def toFeedbackDtnCpyXml(feedbackDtnCpy : AMHFeedbackDistributionCpyEntity, backends : Option[Seq[AMHFeedbackDistributionCpyBackendEntity]], rules : Option[Seq[AMHFeedbackDistributionCpyRuleEntity]]) : Node = {
     logger.debug("feedback code "+ feedbackDtnCpy.code)
     val xmlRules : Seq[Node] = rules.getOrElse(List()) map Util.toXml
     val xmlBackends : Seq[Node] = backends.getOrElse(List()) map Util.toXml
     logger.debug("RULES  "+ xmlRules)
     <FeedbackDistributionCopySelectionTable xmlns="Gateway" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="Gateway Gateway.xsd">
       { feedbackDtnCpy.active match {
       case Some(data) =>
         <Active>{data}</Active>
       case None =>
           <Active/>
     }}
       <Code>{feedbackDtnCpy.code}</Code>
       { feedbackDtnCpy.dataOwner match {
       case Some(data) =>
         <DataOwner>{data}</DataOwner>
       case None =>
           <DataOwner/>
     }}
       { feedbackDtnCpy.description match {
       case Some(data) =>
         <Description>{data}</Description>
       case None =>
           <Description/>
     }}
       { feedbackDtnCpy.lockCode match {
       case Some(data) =>
         <LockCode>{data}</LockCode>
       case None =>
           <LockCode/>
       }}
       { feedbackDtnCpy.name match {
       case Some(data) =>
         <Name>{data}</Name>
       case None =>
           <Name/>
       }}
       { feedbackDtnCpy.copies match {
       case Some(data) =>
         <NumberOfCopies>{data}</NumberOfCopies>
       case None =>
           <NumberOfCopies/>
       }}
       { feedbackDtnCpy.layoutTemplate match {
       case Some(data) =>
         <PrintLayoutTemplate>{data}</PrintLayoutTemplate>
       case None =>
           <PrintLayoutTemplate/>
       }}
       { feedbackDtnCpy.selectionGroup match {
       case Some(data) =>
         <SelectionGroup>{data}</SelectionGroup>
       case None =>
           <SelectionGroup/>
       }}
     <Sequence>{feedbackDtnCpy.sequence}</Sequence>
     <FeedbackDistributionCopyRuleFeedbackDistributionCopyRuleCriteria>
       {xmlRules}
     </FeedbackDistributionCopyRuleFeedbackDistributionCopyRuleCriteria>
     <FeedbackDistributionCopyMultiFeedbackDestination>
       {xmlBackends}
     </FeedbackDistributionCopyMultiFeedbackDestination>
     </FeedbackDistributionCopySelectionTable>
   }

   def toXml(feedbackDtnCpyRule : AMHFeedbackDistributionCpyRuleEntity) : Node = {
     <FeedbackDistributionCopyRuleCriteria>
       <Criteria>
         <primarykey model="Gateway" entity="RuleCriteria">
           <Code>{feedbackDtnCpyRule.code}</Code>
         </primarykey>
       </Criteria>
       { feedbackDtnCpyRule.dataOwner match {
       case Some(data) =>
         <DataOwner>{data}</DataOwner>
       case None =>
           <DataOwner/>
       }}
       { feedbackDtnCpyRule.lockCode match {
       case Some(data) =>
         <LockCode>{data}</LockCode>
       case None =>
           <LockCode/>
       }}
       <SequenceNumber>{feedbackDtnCpyRule.sequence}</SequenceNumber>
     </FeedbackDistributionCopyRuleCriteria>
   }

   def toXml(feedbackDtnCpyBackend : AMHFeedbackDistributionCpyBackendEntity) : Node = {
       <MultiFeedbackDestination>
         <BackendChannel>
           <primarykey model="Gateway" entity="BackendChannel">
             <Code>{feedbackDtnCpyBackend.backCode}</Code>
             <Direction>{feedbackDtnCpyBackend.backDirection}</Direction>
           </primarykey>
         </BackendChannel>
         { feedbackDtnCpyBackend.dataOwner match {
         case Some(data) =>
           <DataOwner>{data}</DataOwner>
         case None =>
             <DataOwner/>
       }}
         { feedbackDtnCpyBackend.lockCode match {
         case Some(data) =>
           <LockCode>{data}</LockCode>
         case None =>
             <LockCode/>
       }}
       </MultiFeedbackDestination>
   }

   /************************   Distribution Selection Table  ******/
   def toDistributionCpyXml(distributionCpy : AMHDistributionCpyEntity, backends : Option[Seq[AMHDistributionCpyBackendEntity]], rules : Option[Seq[AMHDistributionCpyRuleEntity]]) : Node = {
     logger.debug("distribution code "+ distributionCpy.code)
     val xmlRules : Seq[Node] = rules.getOrElse(List()) map Util.toXml
     val xmlBackends : Seq[Node] = backends.getOrElse(List()) map Util.toXml
     logger.debug("RULES  "+ xmlRules)
     <DistributionCopySelectionTable xmlns="Gateway" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="Gateway Gateway.xsd">
       { distributionCpy.active match {
       case Some(data) =>
         <Active>{data}</Active>
       case None =>
           <Active/>
     }}
       <Code>{distributionCpy.code}</Code>
       { distributionCpy.dataOwner match {
       case Some(data) =>
         <DataOwner>{data}</DataOwner>
       case None =>
           <DataOwner/>
     }}
       { distributionCpy.description match {
       case Some(data) =>
         <Description>{data}</Description>
       case None =>
           <Description/>
     }}
       { distributionCpy.lockCode match {
       case Some(data) =>
         <LockCode>{data}</LockCode>
       case None =>
           <LockCode/>
     }}
       { distributionCpy.name match {
       case Some(data) =>
         <Name>{data}</Name>
       case None =>
           <Name/>
     }}
       { distributionCpy.copies match {
       case Some(data) =>
         <NumberOfCopies>{data}</NumberOfCopies>
       case None =>
           <NumberOfCopies/>
     }}
       { distributionCpy.layoutTemplate match {
       case Some(data) =>
         <PrintLayoutTemplate>{data}</PrintLayoutTemplate>
       case None =>
           <PrintLayoutTemplate/>
     }}
       { distributionCpy.selectionGroup match {
       case Some(data) =>
         <SelectionGroup>{data}</SelectionGroup>
       case None =>
           <SelectionGroup/>
     }}
       <Sequence>{distributionCpy.sequence}</Sequence>
       <DistributionCopyRuleDistributionCopyRuleCriteria>
         {xmlRules}
       </DistributionCopyRuleDistributionCopyRuleCriteria>
       <DistributionCopyMultiCopyDestination>
         {xmlBackends}
       </DistributionCopyMultiCopyDestination>
     </DistributionCopySelectionTable>
   }

   def toXml(distributionCpyRule : AMHDistributionCpyRuleEntity) : Node = {
     <DistributionCopyRuleCriteria>
       <Criteria>
         <primarykey model="Gateway" entity="RuleCriteria">
           <Code>{distributionCpyRule.code}</Code>
         </primarykey>
       </Criteria>
       { distributionCpyRule.dataOwner match {
       case Some(data) =>
         <DataOwner>{data}</DataOwner>
       case None =>
           <DataOwner/>
     }}
       { distributionCpyRule.lockCode match {
       case Some(data) =>
         <LockCode>{data}</LockCode>
       case None =>
           <LockCode/>
     }}
       <SequenceNumber>{distributionCpyRule.sequence}</SequenceNumber>
     </DistributionCopyRuleCriteria>
   }

   def toXml(distributionCpyBackend : AMHDistributionCpyBackendEntity) : Node = {
     <MultiCopyDestination>
       <BackendChannel>
         <primarykey model="Gateway" entity="BackendChannel">
           <Code>{distributionCpyBackend.backCode}</Code>
           <Direction>{distributionCpyBackend.backDirection}</Direction>
         </primarykey>
       </BackendChannel>
       { distributionCpyBackend.dataOwner match {
       case Some(data) =>
         <DataOwner>{data}</DataOwner>
       case None =>
           <DataOwner/>
     }}
       { distributionCpyBackend.lockCode match {
       case Some(data) =>
         <LockCode>{data}</LockCode>
       case None =>
           <LockCode/>
     }}
     </MultiCopyDestination>
   }
   /************************  RULE  ******************************/
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
