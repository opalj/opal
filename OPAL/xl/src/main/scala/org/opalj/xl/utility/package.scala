/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files

package object utility {
    def asFiles(fileName: String, suffix: String, code: List[String]): List[File] = {
        code.zipWithIndex.map(ci => { asFile(fileName + ci._2.toString, suffix, ci._1) })
    }

    def asFile(fileName: String, suffix: String, code: String): File = {
        val tmpFile: File = Files.createTempFile(fileName, suffix).toFile
        val fos = new FileOutputStream(tmpFile)
        fos.write(code.getBytes())
        fos.close()
        tmpFile
    }
}
