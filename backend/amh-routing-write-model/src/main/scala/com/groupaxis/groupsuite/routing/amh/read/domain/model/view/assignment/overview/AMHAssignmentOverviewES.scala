package com.groupaxis.groupsuite.routing.amh.read.domain.model.view.assignment.overview

/*
public active: boolean;
  public code: string;
  public backCode: string;
  public backDirection: string;
  public backName: string;
  public backSequence: number;
  public ruleCode: string;
  public ruleExpressions: string;
  public ruleSequence: number;
* */
case class AMHAssignmentOverviewES(active : Boolean, code : String, sequence : Long, backCode: Option[String] = None, backDirection: Option[String] = None, ruleCode : Option[String] = None, ruleSequence : Option[Long] = None, ruleExpression: Option[String] = None) {
  def toLine = s"$active;$sequence;$code;${backCode.getOrElse("")};${ruleSequence.getOrElse("")};${ruleCode.getOrElse("")};${ruleExpression.getOrElse("")}"
}

case class AMHBackendOverviewES(backCode: Option[String] = None, backDirection: Option[String]= None)

case class AMHRuleOverviewES(ruleCode : Option[String] = None, ruleSequence : Option[Long] = None, ruleExpression: Option[String] = None)


