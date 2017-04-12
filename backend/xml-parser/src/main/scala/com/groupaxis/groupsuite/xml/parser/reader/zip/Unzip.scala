package com.groupaxis.groupsuite.xml.parser.reader.zip

import java.io.{File, FileInputStream, FileOutputStream, IOException}
import java.util.zip.{ZipEntry, ZipInputStream}

import org.apache.logging.log4j.scala.Logging

/**
  * Created by anquegi on 04/06/15.
  */
object Unzip extends App with Logging {

  val INPUT_ZIP_FILE: String = "C:\\dev\\AMH_formation\\AMH_reference_flows.zip"
  val OUTPUT_FOLDER: String = "C:\\dev\\AMH_formation\\toto"

  def unZipIt(zipFile: String, outputFolder: String): Unit = {

    val buffer = new Array[Byte](1024)

    try {

      //output directory
      val folder = new File(OUTPUT_FOLDER)
      if (!folder.exists()) {
        folder.mkdir()
      }

      //zip file content
      val zis: ZipInputStream = new ZipInputStream(new FileInputStream(zipFile))
      //get the zipped file list entry
      var ze: ZipEntry = zis.getNextEntry

      while (ze != null) {

        val fileName = ze.getName
        val newFile = new File(outputFolder + File.separator + fileName)

        logger.debug("file unzip : " + newFile.getAbsoluteFile)

        //create folders
        new File(newFile.getParent).mkdirs()

        val fos = new FileOutputStream(newFile)

        var len: Int = zis.read(buffer)

        while (len > 0) {

          fos.write(buffer, 0, len)
          len = zis.read(buffer)
        }

        fos.close()
        ze = zis.getNextEntry
      }

      zis.closeEntry()
      zis.close()

    } catch {
      case e: IOException => logger.error("exception caught: " + e.getMessage)
    }

  }

  Unzip.unZipIt(INPUT_ZIP_FILE, OUTPUT_FOLDER)

}
