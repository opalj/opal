/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ce

import java.io.File
import java.io.PrintWriter
import java.nio.file.Path
import scala.io.Source
import scala.util.Using

import com.typesafe.config.Config

/**
 * Exports the Config structure into an HTML file.
 * @param ConfigList Accepts a List of parsed Configuration Files.
 * @param templatePath Accepts a Path to the HTML Template that should be used.
 */
class HTMLExporter(ConfigList: Iterable[ConfigNode], templatePath: Path) {
    /**
     * Exports the ConfigList into an HTML file.
     * The following parameters are all read from the Configuration Explorer config, however, the CE config was not handed over due to namespace conflicts with the internally used ConfigNode.
     * @param config Accepts the config of the ConfigurationExplorer in order to read necessary values from it directly.
     * @param exportFile Accepts a Path to the file that the Config shall be written to.
     */
    def exportHTML(config: Config, exportFile: File): Unit = {
        val HTMLHeadline = config.getString("org.opalj.ce.html.headline")
        val HTMLContent = config.getString("org.opalj.ce.html.content")
        val HTMLStringBuilder = new StringBuilder()
        val sort_alphabetically = config.getBoolean("org.opalj.ce.html.sort_alphabetically")
        val maximumHeadlinePreviewLength = config.getInt("org.opalj.ce.html.maximum_headline_preview_length")

        // Generate HTML
        var fileContent = ""
        val template = Using(Source.fromFile(templatePath.toString)) { source =>
            source.getLines().mkString("\n")
        }.getOrElse("")
        for (config <- ConfigList) {
            if (!config.isEmpty) {
                config.toHTML(
                    "",
                    HTMLHeadline,
                    HTMLContent,
                    HTMLStringBuilder,
                    sort_alphabetically,
                    maximumHeadlinePreviewLength
                )
                HTMLStringBuilder ++= "<hr>\n"
            }
        }
        fileContent = template.replace("$body", HTMLStringBuilder.toString())

        // Write to file
        val printWriter = new PrintWriter(exportFile)
        printWriter.write(fileContent)
        printWriter.close()
    }

}
