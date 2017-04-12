package com.groupaxis.groupsuite.synchronizator.parser

import java.util.regex.{Matcher, Pattern}

import org.apache.logging.log4j.scala.Logging

object GPParserHelper extends Logging {


  def findMatches2(msgType: Int, source: Option[String], mappings: Map[String, String]): String = {

    source.map(message => {
      val removeSlashR = !message.contains("\r")
      mappings
        .map(entry => entry._1 -> Pattern.compile(if (removeSlashR) entry._2.replaceAll("\\\\r", "") else entry._2))
        .map(entry => {
          val key = entry._1.replaceAll("/", "_")
          val entryKey = if (msgType == 2) {
            if (message.length>15 && message.charAt(15) == '0')
              key.replace("swiftParameters_service", "swift.fin!p")
            else
              key.replace("swiftParameters_service", "swift.fin")
          } else
            key

          val m: Matcher = entry._2.matcher(message)
          if (m.find()) {
            var s: String = ""
            for (j <- 1 to m.groupCount()) {
              s += m.group(j)
            }

            if (s.nonEmpty) {
              entryKey -> s //.replace("\r", "\\r").replace("\n", "\\n")
            } else {
              entryKey -> "no_value"
            }
          } else {
            entryKey -> "no_value"
          }
        })
    }
    )
      //      mappings
      //        .map(mapping => {
      //        MY_PATTERN = Pattern.compile(mapping._2)
      //        m = MY_PATTERN.matcher(message)
      //        if (m.find()) {
      //          var s: String = ""
      //          for (j <- 1 to m.groupCount()) {
      //            s += m.group(j)
      //          }
      //          if (s.nonEmpty) {
      //            Map(mapping._1.replaceAll("/", "_") -> s.replace("\r", "\\r").replace("\n", "\\n"))
      //          } else {
      //            Map(mapping._1.replaceAll("/", "_") -> "no_value")
      //          }
      //        } else {
      //          Map(mapping._1.replaceAll("/", "_") -> "no_value")
      //        }
      //      })
      //    })
      .map(mapping => {
      val contentBasedMappings: Map[String, String] = msgType match {
        case 1 => // MX
          Map("networkProtocol" -> "Swift-InterAct", "direction" -> "DISTRIBUTION")
        case 2 => // MT
          val network = source.map(content => {
            if (content.indexOf("}{2:I") != -1)
              Map("networkProtocol" -> "Swift-FIN", "direction" -> "ROUTING")
            else
              Map("networkProtocol" -> "Swift-FIN", "direction" -> "DISTRIBUTION")
          }).getOrElse(Map())

          val docData = source.map(content => {
            Map("document_data" -> content/*.replace("\"", "\\\"").replace("\r","\\r").replace("\n", "\\n")*/)
          })
            .getOrElse(Map())

          network ++ docData
        case _ => Map()
      }

      if (contentBasedMappings.isEmpty) {
        logger.debug("No (fixed) mappings were added from the message content!!!")
      }

      mapping ++ contentBasedMappings
    })
      .map(map => toStringJson(map))
      .getOrElse("{}")

    //      if(msgType == 2) {
    //        if(message.charAt(15)=='0')
    //          this.messageProperties.replace("swiftParameters_service", "swift.fin!p");
    //        else
    //        this.messageProperties.replace("swiftParameters_service", "swift.fin");
    //      }
    //      message
    //    })

  }

  /*

  public Message(String message, Vector<Mapping> mappingPatterns) {

		// Mapping d'un message MX (en XML v2) ou FIN :
		boolean MT = false;
		if(message.indexOf("xml") != -1) {
			// MX
			this.messageProperties.put("networkProtocol","Swift-InterAct");
			this.messageProperties.put("direction", "DISTRIBUTION");
			MT=false;
		} else {
			//MT
			this.messageProperties.put("networkProtocol","Swift-FIN");
			if (message.indexOf("}{2:I")!=-1)
				this.messageProperties.put("direction", "ROUTING");
			else
				this.messageProperties.put("direction", "DISTRIBUTION");
			MT=true;
		}

		// Full message in document field :
		this.messageProperties.put("document_data",((message.replace("\"", "\\\"")).replace("\r","\\r")).replace("\n", "\\n"));

		// Parse mapping list and create relevant properties
		Pattern MY_PATTERN;
		Matcher m;
		for(int i=0;i<mappingPatterns.size();i++) {

			if(MT)
				MY_PATTERN = Pattern.compile(mappingPatterns.get(i).getMT_pattern());
			else
				MY_PATTERN = Pattern.compile(mappingPatterns.get(i).getMX_pattern());

			m = MY_PATTERN.matcher(message);
			if (m.find()) {
				String result = new String();
				for(int j=1;j<=m.groupCount();j++) {
					result = result + m.group(j);
				}
				if(result.length()>0)
					this.messageProperties.put(mappingPatterns.get(i).getAMH_keyword().replaceAll("/", "_"), (result.replace("\r","\\r")).replace("\n", "\\n"));
				else
					this.messageProperties.put(mappingPatterns.get(i).getAMH_keyword().replaceAll("/", "_"), "");
			} else
				this.messageProperties.put(mappingPatterns.get(i).getAMH_keyword().replaceAll("/", "_"), "");
		}

		if(MT) {
			if(message.charAt(15)=='0')
				this.messageProperties.replace("swiftParameters_service", "swift.fin!p");
			else
				this.messageProperties.replace("swiftParameters_service", "swift.fin");
		}

	}
  * */

  def findMatches(msgType: Int, source: Option[String], mappings: Map[String, String]): String = {
    source.map(source => {
      val filtered = source.replaceAll("\\r", "").replaceAll("\\n", "")
      //      val filtered = source
      mappings
        .map(entry => entry._1 -> entry._2.r("gp1"))
        .map(entry =>
          entry._1.replace("/", "_") ->
            entry._2
              .findFirstMatchIn(filtered)
              .map(
                reg => {
                  if (reg.groupCount >= 1)
                    reg.group("gp1")
                  else
                    "no_value"
                })
              .getOrElse("no_value"))
    }
    )
      .map(mapping => {
        val contentBasedMappings: Map[String, String] = msgType match {
          case 1 => // MX
            Map("networkProtocol" -> "Swift-InterAct", "direction" -> "DISTRIBUTION")
          case 2 => // MT
            source.map(content => {
              if (content.indexOf("}{2:I") != -1)
                Map("networkProtocol" -> "Swift-FIN", "direction" -> "ROUTING")
              else
                Map("networkProtocol" -> "Swift-FIN", "direction" -> "DISTRIBUTION")
            }).getOrElse(Map())
          case _ => Map()
        }

        if (contentBasedMappings.isEmpty) {
          logger.debug("No (fixed) mappings were added from the message content!!!")
        }

        mapping ++ contentBasedMappings
      })
      .map(map => toStringJson(map))
      .getOrElse("{}")
  }

  def toStringJson(map: Map[String, String]): String = {
    import io.circe.syntax._
    map.asJson.noSpaces
  }

  def toArrayResult(content: Option[String]): Option[Array[String]] = {
    val separator = "\n" //sys.props("line.separator")
    content.map(c => if(c.isEmpty) "''" else c)
      .map(c => c.substring(1, c.length - 1))
    .map(_.split(separator).drop(1)
    )
  }

}
