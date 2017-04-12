package com.groupaxis.groupsuite.synchronizator.file

import com.groupaxis.groupsuite.synchronizator.parser.GPParserHelper
import org.apache.logging.log4j.scala.Logging


object GPFileHelperTest {

}

/*"\{2:.(.{3})"
"\{2:.(.{3})"
"\{2:.{4}[0-9]*(.{8})"
"\{1:.{3}(.{8})"
"\{3:.*\{108:(.+?)\}"
"''"

('field21','',':21:(.*)');
('field50K','','(?s):50K:(.*?)\r\n');
('field57A','',':57A:(.*)');
('field57D','','(?s):57D:(.*?)\r\n:');
('field79','','(?s):79:(.*?)\r\n-');
('FINParameters/messageType','','\{2:.(.{3})');
('messageReference','',':20:(.*)');
('messageType/code','<Saa:MessageIdentifier>(.*)</Saa:MessageIdentifier>','\{2:.(.{3})');
('receiverAddress','<Saa:Receiver>.*<Saa:DN>(.*)</Saa:DN>.*</Saa:Receiver>','\{2:.{4}[0-9]*(.{8})');
('senderAddress','<Saa:Sender>.*<Saa:DN>(.*)</Saa:DN>.*</Saa:Sender>','\{1:.{3}(.{8})');
('swiftParameters/requestReference','<Saa:SenderReference>(.*)</Saa:SenderReference>','\{3:.*\{108:(.+?)\}');
('swiftParameters/service','<Saa:NetworkInfo>.*<Saa:Service>(.*)</Saa:Service>.*</Saa:NetworkInfo>','');


*/
object testing extends App with Logging {
  val mx_mappings = Map(
    "senderAddress" -> "<Saa:Sender>.*<Saa:DN>(.*)</Saa:DN>.*</Saa:Sender>",
    "receiverAddress" -> "<Saa:Receiver>.*<Saa:DN>(.*)</Saa:DN>.*</Saa:Receiver>",
    "swiftParameters/service" -> "<Saa:NetworkInfo>.*<Saa:Service>(.*)</Saa:Service>.*</Saa:NetworkInfo>",
    "messageType/code" -> "<Saa:MessageIdentifier>(.*)</Saa:MessageIdentifier>",
    "swiftParameters/requestReference" -> "<Saa:SenderReference>(.*)</Saa:SenderReference>",
    "messageReference" -> "''")

  val mt_mappings = Map(
    "field21" -> ":21:(.*)",
    "field50K" -> "(?s):50K:(.*?)\r\n",
    "field57A" -> ":57A:(.*)",
    "field57D" -> "(?s):57D:(.*?)\r\n:",
    "field79" -> "(?s):79:(.*?)\r\n-",
    "senderAddress" -> "\\{1:.{3}(.{8})",
    "receiverAddress" -> "\\{2:.{4}[0-9]*(.{8})",
    "swiftParameters/service" -> "",
    "messageType/code" -> "\\{2:.(.{3})",
    "swiftParameters/requestReference" -> "\\{3:.*\\{108:(.+?)\\}",
    "messageReference" -> ":20:(.*)")

  //  val p = Pattern.compile("<Saa:Receiver>.*<Saa:DN>(.*)</Saa:DN>.*</Saa:Receiver>", Pattern.MULTILINE | Pattern.UNIX_LINES | Pattern.DOTALL)
  //  p.matcher("")

  val mx_files = GPFileHelper.unZipAll("/dev/DFS/AMH/Simulator/AMHSimulator/samples/samples.zip")

  mx_files.foreach(key => {
    logger.debug(key._1)
    logger.debug(s" found =>  " + GPParserHelper
      .findMatches(1, Some(key._2.toString), mt_mappings))
  })

  logger.debug("----------------------------------")

  //  val fileName = "/dev/DFS/AMH/Simulator/AMHSimulator/samples/mt_msg1.txt"
    val fileName = "/dev/DFS/AMH/Simulator/messages/999-GROUPAY2.txt"
    val mt_file = scala.io.Source.fromFile(fileName).mkString
  logger.debug(fileName)
  logger.debug(s" found =>  " + GPParserHelper
      .findMatches2(1, Some(mt_file), mt_mappings))

//  val dir = new File("/dev/DFS/AMH/Simulator/messages")
//  dir.listFiles.filter(_.isFile).map(_.getCanonicalPath).foreach(fileName => {
//    val mt_file = scala.io.Source.fromFile(fileName).mkString
//    logger.debug(fileName)
//    logger.debug(s" found =>  " + GPParserHelper
//      .findMatches2(1, Some(mt_file), mt_mappings))
//  })

}

