package org.opalj.ce

import java.io.File
import java.io.PrintWriter
import java.nio.file.Path
import scala.collection.mutable.ListBuffer
import scala.io.Source

class HTMLExporter(ConfigList: ListBuffer[ConfigNode], templatePath: Path) {
    def exportHTML(exportFile : File, HTMLHeadline : String, HTMLContent : String): Unit = {

        // Generate HTML
        var fileContent = ""
        val template = Source.fromFile(templatePath.toString).getLines().mkString("\n")
        var body = ""
        for(config <- ConfigList)
        {
            if(config.isEmpty() == false) {
                body += config.toHTML("", HTMLHeadline, HTMLContent)
                body += "<hr>"
            }
        }
        fileContent = template.replace("$body",body)

        // Write to file
        val printWriter = new PrintWriter(exportFile)
        printWriter.write(fileContent)
        printWriter.close
    }

}
