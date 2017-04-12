package com.groupaxis.groupsuite.xml.parser.amh.reader.zip

import java.util.zip.{ZipEntry, ZipFile, ZipOutputStream}
import java.io._

import org.apache.logging.log4j.scala.Logging

import scala.collection.JavaConversions._
import scala.io.{BufferedSource, Codec, Source}

class ZipArchive {

  val BUFSIZE = 4096
  val buffer = new Array[Byte](BUFSIZE)

  def unZip(source: String, targetFolder: String) = {
    val zipFile = new ZipFile(source)

    unzipAllFile(zipFile.entries.toList, getZipEntryInputStream(zipFile)_, new File(targetFolder))
  }

  def getZipEntryInputStream(zipFile: ZipFile)(entry: ZipEntry) = zipFile.getInputStream(entry)

  def unzipAllFile(entryList: List[ZipEntry], inputGetter: (ZipEntry) => InputStream, targetFolder: File): Boolean = {

    entryList match {
      case entry :: entries =>

        if (entry.isDirectory)
          new File(targetFolder, entry.getName).mkdirs
        else
          saveFile(inputGetter(entry), new FileOutputStream(new File(targetFolder, entry.getName)))

        unzipAllFile(entries, inputGetter, targetFolder)
      case _ =>
        true
    }

  }

  def saveFile(fis: InputStream, fos: OutputStream) = {
    writeToFile(bufferReader(fis)_, fos)
    fis.close
    fos.close
  }

  def bufferReader(fis: InputStream)(buffer: Array[Byte]) = (fis.read(buffer), buffer)

  def writeToFile(reader: (Array[Byte]) => Tuple2[Int, Array[Byte]], fos: OutputStream): Boolean = {
    val (length, data) = reader(buffer)
    if (length >= 0) {
      fos.write(data, 0, length)
      writeToFile(reader, fos)
    } else
      true
  }
}

object runn extends App {
  new ZipArchive().unZip("C:\\dev\\AMH_formation\\AMH_reference_flows.zip","C:\\dev\\AMH_formation\\tata")
}

import java.io.{BufferedReader, FileOutputStream, File}
import java.util.zip.{ZipEntry, ZipOutputStream}


//recureisve
//https://examples.javacodegeeks.com/core-java/util/zip/create-zip-file-from-directory-recursively-with-zipoutputstream/
//https://examples.javacodegeeks.com/core-java/util/zip/create-zip-file-from-directory-with-zipoutputstream/
//https://github.com/dhbikoff/Scala-Zip-Archive-Util/blob/master/ZipArchiveUtil.scala

class ZipperC extends Logging {

  def createFileList(file: File, outputFilename: String): List[String] = {
    file match {
      case file if file.isFile => {
        if (file.getName != outputFilename)
          List(file.getAbsoluteFile.toString)
        else
          List()
      }
      case file if file.isDirectory => {
        val fList = file.list
        // Add all files in current dir to list and recur on subdirs
        fList.foldLeft(List[String]())((pList: List[String], path: String) =>
          pList ++ createFileList(new File(file, path), outputFilename))
      }
      case _ => throw new IOException("Bad path. No file or directory found.")
    }
  }

  def addFileToZipEntry(filename: String, parentPath: String,
                        filePathsCount: Int): ZipEntry = {
    if (filePathsCount <= 1)
      new ZipEntry(new File(filename).getName)
    else {
      // use relative path to avoid adding absolute path directories
      val relative = new File(parentPath).toURI.
        relativize(new File(filename).toURI).getPath
      new ZipEntry(relative)
    }
  }

  def createZip(filePaths: List[String], outputFilename: String,
                parentPath: String) = {
    try {
      val fileOutputStream = new FileOutputStream(outputFilename)
      val zipOutputStream = new ZipOutputStream(fileOutputStream)

      filePaths.foreach((name: String) => {
        logger.debug("adding " + name)
        val zipEntry = addFileToZipEntry(name, parentPath, filePaths.size)
        zipOutputStream.putNextEntry(zipEntry)
        val inputSrc = new BufferedSource(
          new FileInputStream(name))(Codec.UTF8)
        inputSrc foreach { c: Char => zipOutputStream.write(c) }
        inputSrc.close
      })

      zipOutputStream.closeEntry
      zipOutputStream.close
      fileOutputStream.close

    } catch {
      case e: IOException => {
        e.printStackTrace
      }
    }
  }
}
