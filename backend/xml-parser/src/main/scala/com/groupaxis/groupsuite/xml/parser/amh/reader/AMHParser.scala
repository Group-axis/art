package com.groupaxis.groupsuite.xml.parser.amh.reader

import java.util.zip.{ZipEntry, ZipFile}
import java.io._

import scala.collection.JavaConversions._
import com.groupaxis.groupsuite.routing.write.domain.model.amh.assignment.{AMHAssignmentEntity, AMHAssignmentRuleEntity}
import com.groupaxis.groupsuite.routing.write.domain.model.amh.backend.AMHBackendEntity
import com.groupaxis.groupsuite.routing.write.domain.model.amh.distribution.copy.{AMHDistributionCpyBackendEntity, AMHDistributionCpyEntity, AMHDistributionCpyRuleEntity}
import com.groupaxis.groupsuite.routing.write.domain.model.amh.feedback.distribution.copy.{AMHFeedbackDistributionCpyBackendEntity, AMHFeedbackDistributionCpyEntity, AMHFeedbackDistributionCpyRuleEntity}
import com.groupaxis.groupsuite.routing.write.domain.model.amh.rule.AMHRuleEntity
import org.apache.logging.log4j.scala.Logging
import org.joda.time.DateTime

import scala.collection.mutable.ListBuffer
import scala.xml.{Elem, Node, NodeSeq}

object AMHParser extends Logging {

  val BUFSIZE = 4096
  val buffer = new Array[Byte](BUFSIZE)

  def unZipAll(source: String, foldersPath: Seq[String]) : scala.collection.mutable.HashMap[String, ListBuffer[Elem]] = {
    val zipFile = new ZipFile(source)
    val folderMap : scala.collection.mutable.HashMap[String, ListBuffer[Elem]] = initFolderMap(foldersPath)
    //    val configFiles = new ListBuffer[Elem]()
    unzipAllFiles(zipFile.entries.toList, getZipEntryInputStream(zipFile), folderMap, foldersPath)
    folderMap
  }

  def unzipAllFiles(entryList: List[ZipEntry], inputGetter: (ZipEntry) => InputStream, folderMap: scala.collection.mutable.HashMap[String, ListBuffer[Elem]], foldersPath: Seq[String]): Boolean = {

    entryList match {
      case entry :: entries =>

        if (entry.isDirectory)
        //          new File(targetFolder, entry.getName).mkdirs
          logger.debug("directory " + entry.getName + " found")
        else {
          val elemList = existEntryPath(entry, folderMap)
          elemList.map( list => {
            logger.debug(" adding... "+entry.getName )
            list += xml.XML.load(inputGetter(entry))
          })
        }
        unzipAllFiles(entries, inputGetter, folderMap, foldersPath)
      case _ =>
        true
    }
  }

  private def existEntryPath(entry : ZipEntry, folderMap : scala.collection.mutable.HashMap[String, ListBuffer[Elem]]) : Option[ListBuffer[Elem]] = {
    val name = entry.getName
    val limit = name.indexOf("/")
    logger.debug(" entryName " + entry.getName.substring(0,limit) + " limit " + limit )
    folderMap.get(entry.getName.substring(0,limit))
  }

  private def initFolderMap(foldersPath: Seq[String]): scala.collection.mutable.HashMap[String, ListBuffer[Elem]] = {
    val folderMap : scala.collection.mutable.HashMap[String, ListBuffer[Elem]] = new scala.collection.mutable.HashMap[String, ListBuffer[Elem]]
    foldersPath.foreach(folderPath => folderMap += (folderPath -> new ListBuffer[Elem]()))
    folderMap
  }

  def unZip(source: String, folderPath: String) : Seq[Elem] = {
    val zipFile = new ZipFile(source)
    val configFiles = new ListBuffer[Elem]()
    unzipAllFile(zipFile.entries.toList, getZipEntryInputStream(zipFile), configFiles, folderPath)
    configFiles
  }

  def getZipEntryInputStream(zipFile: ZipFile)(entry: ZipEntry) = zipFile.getInputStream(entry)

  def unzipAllFile(entryList: List[ZipEntry], inputGetter: (ZipEntry) => InputStream, configFiles: ListBuffer[Elem], folderPath: String): Boolean = {

    entryList match {
      case entry :: entries =>

        if (entry.isDirectory)
//          new File(targetFolder, entry.getName).mkdirs
            logger.debug("directory " + entry.getName + " found")
        else if (entry.getName.startsWith(folderPath)){
          logger.debug(" adding... "+entry.getName )
          configFiles += xml.XML.load(inputGetter(entry))
        }
        unzipAllFile(entries, inputGetter, configFiles, folderPath)
      case _ =>
        true
    }
  }

  def toAMHBackend(env: String, version : String)(root : Elem) : AMHBackendEntity =  AMHBackendEntity(toOption((root \ "BackendChannel" \ "primarykey" \ "Code").text),
    toOption((root \ "BackendChannel" \ "primarykey" \ "Direction").text),
    (root \ "Code").text,
    toOption((root \ "DataOwner").text),
    toOption((root \ "Description").text),
    toOption((root \ "LockCode").text),
    toOption((root \ "Name").text),
    env,
    version)

  def toAMHRule(environment : String, version : String, userId : String)(root : Elem) : AMHRuleEntity =  AMHRuleEntity(
    (root \ "Code").text,
    environment,
    version,
    toOption((root \ "DataOwner").text),
    toOption((root \ "Expression").text),
    toOption((root \ "LockCode").text),
    toOption((root \ "Type").text),
    "N", None, Some(userId))

  def toAMHAssignment(environment : String, version : String, username : String)(root : Elem) : (AMHAssignmentEntity, Seq[AMHAssignmentRuleEntity]) =  {
    val code = (root \ "Code").text
    val rules = toAMHAssignmentRules(environment, version, code, username, root \ "BackendChannelAssignmentRuleBackendChannelAssignmentRuleCriteria" \ "BackendChannelAssignmentRuleCriteria")
    val assignment =

    AMHAssignmentEntity (
      (root \ "Code").text,
      toOption((root \ "Sequence").text).map(value => if (value.length == 0) 0L else value.toLong).getOrElse(0),
      (root \ "AssignedBackendChannel" \ "primarykey" \ "Code").text,
      (root \ "AssignedBackendChannel" \ "primarykey" \ "Direction").text,
      toOption((root \ "Active").text),
      toOption((root \ "DataOwner").text),
      toOption((root \ "LockCode").text),
      toOption((root \ "Description").text),
      environment, version,"Noname.xml", List(), Some(username))
    (assignment, rules)

  }

  def toAMHAssignmentRules(environment : String, version : String, code: String, username : String,  nodes: NodeSeq) = nodes.map(node => toAMHAssignmentRule(environment, version, code, username, node))

  def toAMHAssignmentRule(environment : String, version : String, code:String, username : String, ruleBckAssign : Node) : AMHAssignmentRuleEntity =
    AMHAssignmentRuleEntity(code,
      toOption((ruleBckAssign \ "SequenceNumber").text).map(value => if (value.length == 0) 0L else value.toLong).getOrElse(0),
      (ruleBckAssign \ "Criteria" \ "primarykey" \ "Code").text,
      toOption((ruleBckAssign \ "DataOwner").text),
      toOption((ruleBckAssign \ "LockCode").text),
      environment,
      version, Some(username)
    )

  /****************  feedback distribution copy ******************************************/
  def toAMHFeedbackDtnCpy(environment : String, version : String, username : String)(root : Elem) : (AMHFeedbackDistributionCpyEntity, Seq[AMHFeedbackDistributionCpyRuleEntity], Seq[AMHFeedbackDistributionCpyBackendEntity]) =  {
    val code = (root \ "Code").text
    val rules = toAMHFeedbackDtnCpyRules(code, environment, version, username, root \ "FeedbackDistributionCopyRuleFeedbackDistributionCopyRuleCriteria" \ "FeedbackDistributionCopyRuleCriteria")
    val backends = toAMHFeedbackDtnCpyBackends(code, environment, version, username, root \ "FeedbackDistributionCopyMultiFeedbackDestination" \ "MultiFeedbackDestination")
    val feedbackDtnCpy =
      AMHFeedbackDistributionCpyEntity (
        (root \ "Code").text,
        toOption((root \ "Sequence").text).map(value => if (value.length == 0) 0L else value.toLong).getOrElse(0),
        environment,
        version,
        "Noname.xml",
        toOption((root \ "Active").text),
        toOption((root \ "DataOwner").text),
        toOption((root \ "LockCode").text),
        toOption((root \ "Description").text),
        toOption((root \ "SelectionGroup").text),
        toOption((root \ "PrintLayoutTemplate").text),
        toOption((root \ "NumberOfCopies").text).map(value => if (value.length == 0) 0L else value.toLong),
        toOption((root \ "Name").text),
        List(),
        List(),
        Some(username))
    (feedbackDtnCpy, rules, backends)
  }

  def toAMHFeedbackDtnCpyRules(code: String, environment: String, version : String, username : String, nodes: NodeSeq) = nodes.map(node => toAMHFeedbackDtnCpyRule(code, environment, version, username ,node))

  def toAMHFeedbackDtnCpyRule(code:String, environment: String, version : String, username : String, ruleBckAssign : Node) : AMHFeedbackDistributionCpyRuleEntity =
    AMHFeedbackDistributionCpyRuleEntity(code,
      toOption((ruleBckAssign \ "SequenceNumber").text).map(value => if (value.length == 0) 0L else value.toLong).getOrElse(0),
      (ruleBckAssign \ "Criteria" \ "primarykey" \ "Code").text,
      toOption((ruleBckAssign \ "DataOwner").text),
      toOption((ruleBckAssign \ "LockCode").text),
      environment,
      version,
      Some(username)
    )

  def toAMHFeedbackDtnCpyBackends(code: String, environment: String, version : String, username : String, nodes: NodeSeq) = nodes.map(node => toAMHFeedbackDtnCpyBackend(code, environment, version, username, node))

  def toAMHFeedbackDtnCpyBackend(code:String, environment: String, version : String, username: String, feedbackCpyBackend : Node) : AMHFeedbackDistributionCpyBackendEntity =
    AMHFeedbackDistributionCpyBackendEntity(code,
      (feedbackCpyBackend \ "BackendChannel" \ "primarykey" \ "Code").text,
      (feedbackCpyBackend \ "BackendChannel" \ "primarykey" \ "Direction").text,
      toOption((feedbackCpyBackend \ "DataOwner").text),
      toOption((feedbackCpyBackend \ "LockCode").text),
      environment,
      version,
      Some(username)
    )

  /*********************  distribution copy *********************************************/
  def toAMHDistributionCpy(environment : String, version : String, username : String)(root : Elem) : (AMHDistributionCpyEntity, Seq[AMHDistributionCpyRuleEntity], Seq[AMHDistributionCpyBackendEntity]) =  {
    val code = (root \ "Code").text
    val rules = toAMHDistributionCpyRules(code, environment, version, username, root \ "DistributionCopyRuleDistributionCopyRuleCriteria" \ "DistributionCopyRuleCriteria")
    val backends = toAMHDistributionCpyBackends(code, environment, version, username, root \ "DistributionCopyMultiCopyDestination" \ "MultiCopyDestination")
    val feedbackDtnCpy =
      AMHDistributionCpyEntity (
        (root \ "Code").text,
        toOption((root \ "Sequence").text).map(value => if (value.length == 0) 0L else value.toLong).getOrElse(0),
        environment,
        version,
        "Noname.xml",
        toOption((root \ "Active").text),
        toOption((root \ "DataOwner").text),
        toOption((root \ "LockCode").text),
        toOption((root \ "Description").text),
        toOption((root \ "SelectionGroup").text),
        toOption((root \ "PrintLayoutTemplate").text),
        toOption((root \ "NumberOfCopies").text).map(value => if (value.length == 0) 0L else value.toLong),
        toOption((root \ "Name").text), List(), List(), Some(username))
    (feedbackDtnCpy, rules, backends)
  }

  def toAMHDistributionCpyRules(code: String, environment: String, version : String, username : String, nodes: NodeSeq) = nodes.map(node => toAMHDistributionCpyRule(code, environment, version, username,node))

  def toAMHDistributionCpyRule(code:String, environment: String, version : String, username : String, ruleBckAssign : Node) : AMHDistributionCpyRuleEntity =
    AMHDistributionCpyRuleEntity(code,
      toOption((ruleBckAssign \ "SequenceNumber").text).map(value => if (value.length == 0) 0L else value.toLong).getOrElse(0),
      (ruleBckAssign \ "Criteria" \ "primarykey" \ "Code").text,
      toOption((ruleBckAssign \ "DataOwner").text),
      toOption((ruleBckAssign \ "LockCode").text),
      environment,
      version,
      Some(username)
    )

  def toAMHDistributionCpyBackends(code: String, environment: String, version : String, username : String, nodes: NodeSeq) = nodes.map(node => toAMHDistributionCpyBackend(code, environment, version, username, node))

  def toAMHDistributionCpyBackend(code:String, environment: String, version : String, username : String, feedbackCpyBackend : Node) : AMHDistributionCpyBackendEntity =
    AMHDistributionCpyBackendEntity(code,
      (feedbackCpyBackend \ "BackendChannel" \ "primarykey" \ "Code").text,
      (feedbackCpyBackend \ "BackendChannel" \ "primarykey" \ "Direction").text,
      toOption((feedbackCpyBackend \ "DataOwner").text),
      toOption((feedbackCpyBackend \ "LockCode").text),
      environment,
      version,
      Some(username)
    )

  def toOption(text: String) : Option[String] = {
    text match {
      case value: String if value.isEmpty => None
      case nonEmptyValue: String            => Some(nonEmptyValue)
    }
  }
}
