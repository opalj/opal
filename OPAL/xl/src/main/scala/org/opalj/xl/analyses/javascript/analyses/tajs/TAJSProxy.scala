/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package analyses
package javascript
package analyses
package tajs

import org.opalj.xl.axa.proxy.IFrameworkProxy

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files

object TAJSProxy extends IFrameworkProxy[State]{

  private val tajs = new TAJS();

  lazy val tmpFile: File = Files.createTempFile("JavaScript", ".js").toFile


   def asFile(code:String): File = {
    val fos = new FileOutputStream(tmpFile)
    fos.write(code.getBytes())
    fos.close()
    tmpFile
  }

  override def analyze(code: String): State = {
    val file = asFile(code)

    tajs.analyze(file.getPath.toString)
    new State()
  }

  override def resume(code:String): State = {
    tajs.resume(code)
    new State()
  }


}

class State{val s = ""}