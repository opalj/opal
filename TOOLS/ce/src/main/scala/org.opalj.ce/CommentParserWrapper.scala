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
    def IterateConfigs(filepaths: mutable.Buffer[Path]): ListBuffer[ConfigObject]  = {
        val CommentedConfigs = new ListBuffer[ConfigObject]
        for (filepath <- filepaths) {
            CommentedConfigs += this.ParseComments(filepath)
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
        if(MergingConfigs.size > 0) {
            val conf = MergingConfigs(0)
            for (i <- MergingConfigs.indices) {
                if (i != 0) {
                    conf.merge(MergingConfigs(i))
                }
            }
            conf.comment.label = "reference.conf"
            conf.comment.brief = "Aggregated standard configuration of merged reference.conf files"
            CommentedConfigs.addOne(conf)
        }

        for(config <- CommentedConfigs){
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
    def ParseComments(filepath: Path): ConfigObject = {
        val CP = new CommentParser
        CP.parseComments(filepath)
    }

}
