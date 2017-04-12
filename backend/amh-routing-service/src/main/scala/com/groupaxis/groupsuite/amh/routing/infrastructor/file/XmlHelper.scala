package com.groupaxis.groupsuite.amh.routing.infrastructor.file

import com.groupaxis.groupsuite.routing.write.domain.model.amh.assignment.{AMHAssignmentEntity, AMHAssignmentRuleEntity}
import com.groupaxis.groupsuite.routing.write.domain.model.amh.backend.AMHBackendEntity
import com.groupaxis.groupsuite.routing.write.domain.model.amh.distribution.copy.{AMHDistributionCpyBackendEntity, AMHDistributionCpyEntity, AMHDistributionCpyRuleEntity}
import com.groupaxis.groupsuite.routing.write.domain.model.amh.feedback.distribution.copy.{AMHFeedbackDistributionCpyBackendEntity, AMHFeedbackDistributionCpyEntity, AMHFeedbackDistributionCpyRuleEntity}
import com.groupaxis.groupsuite.routing.write.domain.model.amh.rule.AMHRuleEntity
import org.apache.logging.log4j.scala.Logging

import scala.xml.{Atom, Elem, Node, NodeSeq}

object XmlHelper extends Logging {

    implicit def optionElem(e: Elem) : Any = new {
      def ? : NodeSeq = {
        require(e.child.length == 1)
        e.child.head match {
          case atom: Atom[Option[_]] => atom.data match {
            case None => NodeSeq.Empty
            case Some(x) => e.copy(child = x match {
              case n: NodeSeq => n
              case z          => new Atom(z)
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

  def toXmlDeletedRule(rule: AMHRuleEntity) : Node = {
  <RuleCriteria xmlns="Gateway" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="Gateway Gateway.xsd" importType="delete">
    <Code>{rule.originalCode.getOrElse("undefined")}</Code>
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
      val xmlRules : Seq[Node] = rules.getOrElse(List())map this.toXml
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
            <Code>{assignmentRule.ruleCode}</Code>
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

    /**************************** Feedback Distribution Copy ************************************************/
    def toFeedbackDtnCpyXml(feedbackDtnCpy : AMHFeedbackDistributionCpyEntity, backends : Option[Seq[AMHFeedbackDistributionCpyBackendEntity]], rules : Option[Seq[AMHFeedbackDistributionCpyRuleEntity]]) : Node = {
      logger.debug("feedback code "+ feedbackDtnCpy.code)
      val xmlRules : Seq[Node] = rules.getOrElse(List()) map XmlHelper.toXml
      val xmlBackends : Seq[Node] = backends.getOrElse(List()) map XmlHelper.toXml
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
          <Code>{feedbackDtnCpyRule.ruleCode}</Code>
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

  /*********************  distribution copy *********************************************/
  def toDistributionCpyXml(distributionCpy : AMHDistributionCpyEntity, backends : Option[Seq[AMHDistributionCpyBackendEntity]], rules : Option[Seq[AMHDistributionCpyRuleEntity]]) : Node = {
    logger.debug("distribution code "+ distributionCpy.code)
    val xmlRules : Seq[Node] = rules.getOrElse(List()) map XmlHelper.toXml
    val xmlBackends : Seq[Node] = backends.getOrElse(List()) map XmlHelper.toXml
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
          <Code>{distributionCpyRule.ruleCode}</Code>
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
}
