package org.opalj.ce

import java.io.{File, PrintWriter}
import java.nio.file.Path
import scala.collection.mutable.ListBuffer
import scala.io.Source

class HTMLExporter(ConfigList: ListBuffer[ConfigNode], templatePath: Path) {
    def exportHTML(exportFile : File, HTMLHeadline : String, HTMLContent : String): Unit = {
        var fileContent = ""
        val template = Source.fromFile(templatePath.toString()).getLines().mkString("\n")
        var body = ""
        for(config <- ConfigList)
        {
            body += config.toHTML("",HTMLHeadline,HTMLContent)
            body += "<hr>"
        }
        fileContent = template.replace("$body",body)

        val printWriter = new PrintWriter(exportFile)
        printWriter.write(fileContent)
        printWriter.close
    }

}
