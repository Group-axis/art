package com.groupaxis.groupsuite.xml.parser.amh.model

import akka.http.scaladsl.model.Multipart
import org.joda.time.DateTime

object Messages {

  case class ImportAMHXmlFile(filePath : String, username: String, importException : String)
  case class XmlFileImported(response : String)

  case class ExportAMHXmlFile(filePath : String, fileName : String, userId: String, time : DateTime, env : String = "UNKNOWN", version: String = "DEFAULT")
  case class ExportAMHBackupXmlFile(fileData: Multipart.FormData, fileName : String)
  case class ExportXmlFile(filePath : String, fileName : String, env : String = "UNKNOWN", version: String = "DEFAULT")
  case class XmlFileExported(response : String, username : String = "")

}
