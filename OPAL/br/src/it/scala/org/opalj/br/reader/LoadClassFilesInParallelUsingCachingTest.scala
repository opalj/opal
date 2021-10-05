/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import org.opalj.bytecode.JRELibraryFolder
import org.opalj.bi.TestResources.allBITestJARs

class LoadClassFilesInParallelUsingCachingTest extends AnyFlatSpec with Matchers {

    behavior of "OPAL when reading class files using caching"

    val reader = new Java8FrameworkWithCaching(new BytecodeInstructionsCache)

    private[this] def validate(classFile: ClassFile): Unit = {
        classFile.thisType.fqn should not be null
    }

    for {
        file <- Iterator(JRELibraryFolder) ++ allBITestJARs()
        if file.isFile && file.canRead && file.getName.endsWith(".jar")
        path = file.getPath
    } {
        it should s"should be able to read all classes in $path" in {
            reader.ClassFiles(file) foreach { cs => val (cf, _) = cs; validate(cf) }
        }
    }
}
