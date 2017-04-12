import java.util.zip.{ZipEntry, ZipFile, ZipOutputStream}
import java.io._

import scala.collection.JavaConversions._
import scala.io.Source

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

class Zipper {
  def compress(zipFilepath: String, files: List[File]) {

    def readByte(bufferedReader: BufferedReader): Stream[Int] = {
      bufferedReader.read() #:: readByte(bufferedReader)
    }
    val zip = new ZipOutputStream(new FileOutputStream(zipFilepath))
    try {
      for (file <- files) {
        //add zip entry to output stream
        zip.putNextEntry(new ZipEntry(file.getName))

        val in = Source.fromFile(file.getCanonicalPath).bufferedReader()
        try {
          readByte(in).takeWhile(_ > -1).toList.foreach(zip.write(_))
        }
        finally {
          in.close()
        }

        zip.closeEntry()
      }
    }
    finally {
      zip.close()
    }
  }
}
