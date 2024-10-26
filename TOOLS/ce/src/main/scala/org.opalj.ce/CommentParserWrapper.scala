/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ce

import java.nio.file.Path
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * The class CommentParserWrapper is the class that should be used for parsing commented config within the Configuration Explorer
 */
class CommentParserWrapper {
    /**
     * Made to parse multiple Configuration Files in bulk
     * Used in combination with the file Locator to locate and parse all config files of a project
     * @param filepaths accepts a list of full paths to the HOCON config files that shall be parsed
     * @return is a list of the parsed configuration files, paired with the path they originate from
     */
    def iterateConfigs(filepaths: mutable.Buffer[Path], rootDirectory: Path): ListBuffer[ConfigObject] = {
        val CommentedConfigs = new ListBuffer[ConfigObject]
        for (filepath <- filepaths) {
            CommentedConfigs += ParseComments(filepath, rootDirectory)
        }

        // Merge all config files named "reference.conf"
        val MergingConfigs = new ListBuffer[ConfigObject]
        for (i <- CommentedConfigs.indices.reverse) {
            val config = CommentedConfigs(i)
            if (config.comment.label.endsWith("reference.conf")) {
                MergingConfigs.addOne(config)
                CommentedConfigs.remove(i)
            } else {
                CommentedConfigs(i).expand()
            }
        }
        if (MergingConfigs.nonEmpty) {
            val conf = ConfigObject(mutable.Map[String, ConfigNode](), new Comment)
            for (i <- MergingConfigs.indices) {
                conf.merge(MergingConfigs(i))
            }
            conf.comment.label = "reference.conf"
            conf.comment.brief = "Aggregated standard configuration of merged reference.conf files"
            CommentedConfigs.addOne(conf)
        }

        for (config <- CommentedConfigs) {
            config.collapse()
        }

        CommentedConfigs
    }

    /**
     * Handles the frame around parsing the configuration file
     *
     * @param filepath accepts the full path to a valid HOCON config file
     * @return returns the parsed config as a ConfigNode
     */
    def ParseComments(filepath: Path, rootDirectory: Path): ConfigObject = {
        // This prevents the Parser from parsing a file without valid syntax
        com.typesafe.config.ConfigFactory.load(filepath.toString)

        val CP = new CommentParser
        CP.parseFile(filepath, rootDirectory)
    }

}
