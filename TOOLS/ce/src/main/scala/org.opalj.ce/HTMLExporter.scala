package org.opalj
package ce

import java.io.File
import java.io.PrintWriter
import java.nio.file.Path
import scala.collection.mutable.ListBuffer
import scala.io.Source
import scala.util.Using

/**
 * Exports the Config structure into an HTML file
 * @param ConfigList Accepts a List of parsed Configuration Files
 * @param templatePath Accepts a Path to the HTML Template that should be used
 */
class HTMLExporter(ConfigList: ListBuffer[ConfigNode], templatePath: Path) {
    /**
     * Exports the ConfigList into an HTML file
     * The following parameters are all read from the Configuration Explorer config, however, the CE config was not handed over due to namespace conflicts with the internally used ConfigNode
     * @param exportFile Accepts a Path to the file that the Config shall be written to
     * @param HTMLHeadline Accepts the Headline HTML structure that gets passed on to the ConfigNodes
     * @param HTMLContent Accepts the Content HTML structure that gets passed on to the ConfigNodes
     */
    def exportHTML(exportFile: File, HTMLHeadline: String, HTMLContent: String, sort_alphabetically: Boolean): Unit = {

        // Generate HTML
        var fileContent = ""
        val template = Using(Source.fromFile(templatePath.toString)) { source =>
            source.getLines().mkString("\n")
        }.getOrElse("")
        var body = ""
        for (config <- ConfigList) {
            if (!config.isEmpty) {
                body += config.toHTML("", HTMLHeadline, HTMLContent, sort_alphabetically)
                body += "<hr>"
            }
        }
        fileContent = template.replace("$body", body)

        // Write to file
        val printWriter = new PrintWriter(exportFile)
        printWriter.write(fileContent)
        printWriter.close()
    }

}
