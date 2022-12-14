/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.python

import java.io.{File, FileOutputStream}
import java.nio.file.Files

/**
 * Common trait for representing Python sources embedded in Java source code.
 */
sealed trait PythonSource {
    /**
     * Return the Python source code as a string.
     * @return Python source
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

case class PythonStringSource(source: String) extends PythonSource {
    lazy val tmpFile: File = Files.createTempFile("opal", ".py").toFile

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
case class PythonFileSource(path: String) extends PythonSource {
    override def asString: String = throw new RuntimeException("Not implemented!")

    override def asFile(codeBefore: String, codeAfter: String): File = throw new RuntimeException("Not implemented!")
}
