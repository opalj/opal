/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import org.opalj.br.analyses.Project
import org.opalj.br.analyses.BasicReport

/**
 * Prints the 3-address code for all methods of all classes found in the given jar/folder.
 *
 * @author Michael Eichberg
 */
object PrintTAC {

    def main(args: Array[String]): Unit = {
        val p = Project(new java.io.File(args(0)))
        val tacProvider = p.get(SimpleTACAIKey) // TAC = Three-address code...
        for {
            cf ← p.allProjectClassFiles
            m ← cf.methods
            if m.body.isDefined
        } {
            val tac = tacProvider(m)
            println(m.toJava(ToTxt(tac).mkString("\n", "\n", "\n"))+"\n\n")
        }

        BasicReport("Done.")
    }
}
