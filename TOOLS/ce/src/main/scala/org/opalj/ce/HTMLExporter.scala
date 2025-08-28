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
 */
object HTMLExporter {
    /**
     * Exports the ConfigList into an HTML file.
     * The following parameters are all read from the Configuration Explorer config, however, the CE config was not handed over due to namespace conflicts with the internally used ConfigNode.
     * @param configList A list of parsed Configuration Files.
     * @param templatePath A path to the HTML Template that should be used.
     * @param config The config of the ConfigurationExplorer in order to read necessary values from it directly.
     * @param exportFile A path to the file that the Config shall be written to.
     */
    def exportHTML(configList: Iterable[ConfigNode], templatePath: Path, config: Config, exportFile: File): Unit = {
        val headline = config.getString("org.opalj.ce.html.headline")
        val content = config.getString("org.opalj.ce.html.content")
        val pageHTML = new StringBuilder()
        val sortAlphabetically = config.getBoolean("org.opalj.ce.html.sortAlphabetically")
        val maximumHeadlinePreviewLength = config.getInt("org.opalj.ce.html.maximumHeadlinePreviewLength")

        // Generate HTML
        var fileContent = ""
        val template = Using(Source.fromFile(templatePath.toFile)) { _.mkString }.getOrElse("")
        for (config <- configList) {
            if (!config.isEmpty) {
                config.toHTML(
                    "",
                    headline,
                    content,
                    pageHTML,
                    sortAlphabetically,
                    maximumHeadlinePreviewLength
                )
                pageHTML ++= "<hr>\n"
            }
        }
        fileContent = template.replace("$body", pageHTML.toString())

        // Write to file
        Using(new PrintWriter(exportFile)) { _.write(fileContent) }
    }

}
