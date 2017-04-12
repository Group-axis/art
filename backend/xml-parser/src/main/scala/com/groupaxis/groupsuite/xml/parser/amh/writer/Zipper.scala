package com.groupaxis.groupsuite.xml.parser.amh.writer

import java.io.{File, FileInputStream, FileOutputStream, IOException}
import java.util.zip.{ZipEntry, ZipOutputStream}

import org.apache.logging.log4j.scala.Logging

import scala.io.{BufferedSource, Codec}

object Zipper extends Logging {

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
                parentPath: String) : Either[String, String]= {
    try {
      val fileOutputStream = new FileOutputStream(outputFilename)
      val zipOutputStream = new ZipOutputStream(fileOutputStream)
logger.debug("***  params filePath "+filePaths + " output "+ outputFilename + " parentPAth "+ parentPath)
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

      Right("Done")
    } catch {
      case e: IOException => {
        Left(e.getMessage)
      }
    }
  }
}
