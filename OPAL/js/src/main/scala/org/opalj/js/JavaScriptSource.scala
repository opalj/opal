/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.js

import java.io.{File, FileOutputStream}
import java.nio.file.Files

/**
 * Common trait for representing JavaScript sources embedded in Java source code.
 */
sealed trait JavaScriptSource {
    /**
     * Return the JavaScript source code as a string.
     * @return JavaScript source
     */
    def asString: String

    /**
     *
     * @param codeBefore Code to be added in front of the script.
     * @param codeAfter Code to be added at the end of the script.
     * @return file handler
     */
    def asFile(codeBefore: String, codeAfter: String): File
}

case class JavaScriptStringSource(source: String) extends JavaScriptSource {
    lazy val tmpFile: File = Files.createTempFile("opal", ".js").toFile

    override def asString: String = source

    override def asFile(codeBefore: String, codeAfter: String): File = {
        val code = codeBefore + source + codeAfter
        val fos = new FileOutputStream(tmpFile)
        fos.write(code.getBytes())
        fos.close()
        tmpFile
    }
}

// TODO: Implement a way to read source files from disk and uncomment lines in LocalJSSourceFinder
case class JavaScriptFileSource(path: String) extends JavaScriptSource {
    override def asString: String = throw new RuntimeException("Not implemented!")

    override def asFile(codeBefore: String, codeAfter: String): File = throw new RuntimeException("Not implemented!")
}
