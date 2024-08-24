package org.opalj.ce

import java.nio.file.Path
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class CommentParserWrapper {
    def IterateConfigs(filepaths: mutable.Buffer[Path]): ListBuffer[ConfigNode] = {
        val CommentedConfigs = new ListBuffer[ConfigNode]
        for (filepath <- filepaths) {
            CommentedConfigs += this.ParseComments(filepath)
        }
        CommentedConfigs
    }

    def ParseComments(filepath: Path) : ConfigNode = {
        val CP = new CommentParser
        CP.parseComments(filepath)
    }

}
