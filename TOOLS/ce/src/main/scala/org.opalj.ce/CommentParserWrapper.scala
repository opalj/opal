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
    def IterateConfigs(filepaths: mutable.Buffer[Path]): ListBuffer[ConfigNode]  = {
        val CommentedConfigs = new ListBuffer[ConfigNode]
        for (filepath <- filepaths) {
            CommentedConfigs += this.ParseComments(filepath)
        }
        CommentedConfigs
    }

    /**
     * Handles the frame around parsing the configuration file
     * @param filepath accepts the full path to a valid HOCON config file
     * @return returns the parsed config as a ConfigNode
     */
    def ParseComments(filepath: Path) : ConfigNode = {
        val CP = new CommentParser
        CP.parseComments(filepath)
    }

}
