package com.groupaxis.groupsuite.synchronizator.file

import java.io._
import java.util.zip.{ZipEntry, ZipFile}

import akka.http.javadsl.model.Multipart.BodyPart
import akka.http.scaladsl.model.Multipart
import akka.stream.Materializer
import akka.stream.scaladsl.FileIO
import org.apache.logging.log4j.scala.Logging

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.io.{Codec, Source}
import scala.util.{Failure, Success, Try}
import scala.xml.Elem

object GPFileHelper extends Logging {

  def createDirectory(pathStr: String): Unit = {
    import scalax.file.Path
    val path: Path = Path.fromString(pathStr)
    path.createDirectory(failIfExists = false)
  }

  def createTempFile(prefix: String, suffix: String, formData: Multipart.FormData)(implicit ec: ExecutionContext, d: Materializer): Future[Map[String, String]] = {
    formData.parts.mapAsync[(String, String)](1) {

      case file: BodyPart if file.name == "file" =>
        val tempFile: File = File.createTempFile(prefix, suffix)
        file.entity.dataBytes.runWith(FileIO.toFile(tempFile)).map(_ => file.name -> tempFile.getAbsolutePath)

      case data: BodyPart => data.toStrict(2.seconds).map(strict => data.name -> strict.entity.data.utf8String)

    }.runFold(Map.empty[String, String])((map, tuple) => map + tuple)
  }

  def unZipAll(source: String): scala.collection.mutable.HashMap[String, (Int, String)] = {
    val zipFile = new ZipFile(source)
    val folderMap: scala.collection.mutable.HashMap[String, (Int, String)] = new scala.collection.mutable.HashMap[String, (Int, String)]
    unzipAllFiles(zipFile.entries.toList, getZipEntryContent(zipFile), folderMap)
    logger.debug(s" unzip ${folderMap.size} file(s) ")
    folderMap
  }

  def unzipAllFiles(entryList: List[ZipEntry], inputGetter: (ZipEntry) => (Int, String), folderMap: scala.collection.mutable.HashMap[String, (Int, String)]): Boolean = {

    entryList match {
      case entry :: entries =>

        if (entry.isDirectory)
          logger.debug("directory " + entry.getName + " found")
        else {
      //    logger.debug(" adding... " + entry.getName)

          //          inputGetter(entry) match {
          //            case (isXml, content) if isXml =>
          //              folderMap += (entry.getName -> (isXml -> xml.XML.load(content)))
          //            case (isText, content) if !isText =>
          //              folderMap += (entry.getName -> (isText -> content))
          //          }
          folderMap += (entry.getName -> inputGetter(entry))
        }
        unzipAllFiles(entries, inputGetter, folderMap)
      case _ =>
        true
    }
  }

  def getZipEntryContent(zipFile: ZipFile)(entry: ZipEntry): (Int, String) = {
    val content = scala.io.Source.fromInputStream(zipFile.getInputStream(entry))(Codec.UTF8).mkString
    (findMsgType(Some(content)), content)
  }

  def findMsgType(content: Option[String]): Int = {
    content match {
      case Some(value) =>
        if (value.indexOf("<Saa:Message>") >= 0) 1 else 2
      case None =>
        3
    }
  }

  def unZipAll(source: String, foldersPath: Seq[String]): scala.collection.mutable.HashMap[String, ListBuffer[Elem]] = {
    val zipFile = new ZipFile(source)
    val folderMap: scala.collection.mutable.HashMap[String, ListBuffer[Elem]] = initFolderMap(foldersPath)
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
          elemList.map(list => {
            logger.debug(" adding... " + entry.getName)
            list += xml.XML.load(inputGetter(entry))
          })
        }
        unzipAllFiles(entries, inputGetter, folderMap, foldersPath)
      case _ =>
        true
    }
  }

  private def existEntryPath[T](entry: ZipEntry, folderMap: scala.collection.mutable.HashMap[String, ListBuffer[T]]): Option[ListBuffer[T]] = {
    val name = entry.getName
    val limit = name.indexOf("/")
    logger.debug(" entryName " + entry.getName.substring(0, limit) + " limit " + limit)
    folderMap.get(entry.getName.substring(0, limit))
  }

  private def initFolderMap[T](foldersPath: Seq[String]): scala.collection.mutable.HashMap[String, ListBuffer[T]] = {
    val folderMap: scala.collection.mutable.HashMap[String, ListBuffer[T]] = new scala.collection.mutable.HashMap[String, ListBuffer[T]]
    foldersPath.foreach(folderPath => folderMap += (folderPath -> new ListBuffer[T]()))
    folderMap
  }

  def getZipEntryInputStream(zipFile: ZipFile)(entry: ZipEntry) = zipFile.getInputStream(entry)

  def writeFile (canonicalFilename: String, text: Option[String]) : Option[String] = {
    try{
      val file: File = new File (canonicalFilename)
      val out: BufferedWriter = new BufferedWriter (new FileWriter (file) )
      out.write (text.getOrElse(""))
      out.close ()
      None
    } catch {
      case e: IOException => Some(e.getMessage)
    }
  }

  def readFile(canonicalFilename: String) : Option[String] = {
    val (source, lines) = readFileAsIterator(canonicalFilename)
    val returnValue = lines.map(_ mkString "\n")
    source.close()
    returnValue
  }

  def readFileAsIterator(canonicalFilename: String) : (Source, Option[Iterator[String]]) = {
    val source = Source.fromFile(canonicalFilename)(Codec.UTF8)
    val tryReadFile = Try[Iterator[String]](source.getLines())

    tryReadFile match {
      case Success(fileAsLines) => (source,Some(fileAsLines))
      case Failure(error) =>
        logger.debug(s"Error while reading file $canonicalFilename : ${error.getMessage}")
        (source, None)
    }
  }

}

