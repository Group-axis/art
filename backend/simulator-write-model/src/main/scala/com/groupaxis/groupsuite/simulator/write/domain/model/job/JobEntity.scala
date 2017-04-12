package com.groupaxis.groupsuite.simulator.write.domain.model.job

import org.joda.time.DateTime

/** ******  JOB *************/
case class JobEntity(id: Int, user: Option[String],
                     creationDate: Option[DateTime], startDate: Option[DateTime],
                     endDate: Option[DateTime], status: Option[Int], numOfMessages: Option[Int],
                     fileName: Option[String], comment: Option[String],
                     params: Option[String], output: Option[String], jobLauncher : Int = 2, outputAsArray : Option[Array[String]] = None) {

}

case class JobEntityUpdate(user : Option[String],
                           creationDate : Option[DateTime],
                           startDate : Option[DateTime],
                           endDate : Option[DateTime],
                           status : Option[Int],
                           numOfMessages : Option[Int],
                           fileName : Option[String],
                           comment : Option[String],
                           params : Option[String],
                           output : Option[String],
                           jobLauncher : Int = 2) {

  def merge(job: JobEntity): JobEntity = {
    JobEntity(job.id, user.orElse(job.user), creationDate.orElse(job.creationDate)
      , startDate.orElse(job.startDate), endDate.orElse(job.endDate)
      , status.orElse(job.status), numOfMessages.orElse(job.numOfMessages)
      , fileName.orElse(job.fileName), comment.orElse(job.comment)
      , params.orElse(job.params), output.orElse(job.output), jobLauncher)
  }

  def merge(id: Int): JobEntity = {
    JobEntity(id, user, creationDate
      , startDate, endDate
      , status, numOfMessages
      , fileName, comment
      , params, output, jobLauncher)
  }

  def toJobEntity = merge(-1)
}

case class Hit(fileName : String, selectionSequence : Long, selectionCode : String,
               ruleSequence : Long, ruleName : String, ruleExpression : String,
               backendSequences : String, backendNames : String, messageReference : String, selectionType : String) {

  def toLine = s"$fileName;$messageReference;$selectionType;$selectionSequence;$selectionCode;$backendNames;$ruleSequence;$ruleName;$ruleExpression"

}
