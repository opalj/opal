/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.opalj.bytecode.JRELibraryFolder
import org.opalj.bi.TestResources.allBITestJARs

class LoadClassFilesInParallelTest extends AnyFlatSpec with Matchers {

    behavior of "OPAL when reading class files (in parallel)"

    private[this] def commonValidator(classFile: ClassFile): Unit = {
        classFile.thisType should not be null
    }

    private[this] def publicInterfaceValidator(classFile: ClassFile): Unit = {
        commonValidator(classFile)
        // the body of no method should be available
        classFile.methods.forall(m => m.body.isEmpty)
    }

    for {
        file <- Iterator(JRELibraryFolder) ++ allBITestJARs()
        if file.isFile && file.canRead && file.getName.endsWith(".jar")
        path = file.getPath
    } {
        it should s"it should be able to reify all class files in $path" in {
            Java8Framework.ClassFiles(file) foreach { e => val (cf, _) = e; commonValidator(cf) }
        }

        it should s"it should be able to reify only the signatures of all methods in $path" in {
            Java8LibraryFramework.ClassFiles(file) foreach { cs =>
                val (cf, _) = cs
                publicInterfaceValidator(cf)
            }
        }
    }
}
