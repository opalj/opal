/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ce

import java.io.File
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Path
import scala.io.Source
import scala.util.Using

import com.typesafe.config.Config

import org.opalj.br.analyses.SomeProject

/**
 * Exports the Config structure into an HTML file.
 */
class HTMLExporter(config: Config) {
    val headline = config.getString("org.opalj.ce.html.headline")
    val brief = config.getString("org.opalj.ce.html.brief")
    val details = config.getString("org.opalj.ce.html.details")
    val content = config.getString("org.opalj.ce.html.content")
    val sortAlphabetically = config.getBoolean("org.opalj.ce.html.sortAlphabetically")
    val maximumHeadlinePreviewLength = config.getInt("org.opalj.ce.html.maximumHeadlinePreviewLength")

    /**
     * Exports the ConfigList into an HTML file.
     * The following parameters are all read from the Configuration Explorer config, however, the CE config was not handed over due to namespace conflicts with the internally used ConfigNode.
     * @param configList A list of parsed Configuration Files.
     * @param templatePath A path to the HTML Template that should be used.
     * @param exportFile A path to the file that the Config shall be written to.
     */
    def exportHTML(configList: Iterable[ConfigNode], templatePath: Path, exportFile: File)(implicit
        project: SomeProject
    ): Unit = {
        val pageHTML = new StringBuilder()

        // Generate HTML
        var fileContent = ""
        val template = Using(Source.fromFile(templatePath.toFile)) { _.mkString }.getOrElse("")
        for (config <- configList) {
            if (!config.isEmpty) {
                config.createHTML(this, "", pageHTML)
                pageHTML ++= "<hr>\n"
            }
        }
        fileContent =
            template.replaceAll("\\$version", ConfigurationExplorer.buildVersion).replace("$body", pageHTML.toString())

        // Write to file
        Files.createDirectories(exportFile.toPath.getParent)
        Using(new PrintWriter(exportFile)) { _.write(fileContent) }
    }

    def restrictLength(text: String): String = {
        val length = text.length
        text.substring(0, length.min(maximumHeadlinePreviewLength)) +
            (if (length > maximumHeadlinePreviewLength && text.charAt(maximumHeadlinePreviewLength - 1) != ' ') "..."
             else "")
    }
}
