/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ce

import java.nio.file.Path
import scala.collection.immutable
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

import com.typesafe.config.ConfigFactory

/**
 * The class CommentParserWrapper is the class that should be used for parsing commented config within the Configuration Explorer.
 */
class CommentParserWrapper {
    /**
     * Made to parse multiple Configuration Files in bulk.
     * Used in combination with the file Locator to locate and parse all config files of a project.
     * @param filepaths accepts a list of full paths to the HOCON config files that shall be parsed.
     * @return is a list of the parsed configuration files, paired with the path they originate from.
     */
    def iterateConfigs(filepaths: Iterable[Path], rootDirectory: Path): immutable.Seq[ConfigObject] = {
        val commentedConfigs = new ListBuffer[ConfigObject]
        for (filepath <- filepaths) {
            commentedConfigs += ParseComments(filepath, rootDirectory)
        }

        // Merge all config files named "reference.conf"
        val mergingConfigs = new ListBuffer[ConfigObject]
        for (i <- commentedConfigs.indices.reverse) {
            val config = commentedConfigs(i)
            if (config.comment.label.endsWith("reference.conf")) {
                mergingConfigs += config
                commentedConfigs.remove(i)
            }
        }
        if (mergingConfigs.nonEmpty) {
            val conf = ConfigObject(mutable.Map[String, ConfigNode](), new DocumentationComment)
            for (i <- mergingConfigs.indices) {
                conf.merge(mergingConfigs(i))
            }
            conf.comment.label = "reference.conf"
            conf.comment.brief = "Aggregated standard configuration of merged reference.conf files"
            commentedConfigs += conf
        }

        for (config <- commentedConfigs) {
            config.collapse()
        }

        commentedConfigs.toSeq
    }

    /**
     * Handles the frame around parsing the configuration file.
     * Also checks if the config files are in an allowed HOCON formats to prevent endless loops.
     * @param filepath accepts the full path to a valid HOCON config file.
     * @return returns the parsed config as a ConfigNode.
     */
    def ParseComments(filepath: Path, rootDirectory: Path): ConfigObject = {
        // This prevents the Parser from parsing a file without valid syntax
        ConfigFactory.load(filepath.toString)

        val cp = new CommentParser
        cp.parseFile(filepath, rootDirectory)
    }

}
