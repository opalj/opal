/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll

import org.opalj.ll.llvm.Module
import org.opalj.ll.llvm.Reader
import org.opalj.ll.llvm.value

/**
 * This is a collection of LLVM Modules that are to be analyzed together as a single analysis project
 *
 * @author Marc Clement
 */
class LLVMProject(val modules: Iterable[Module]) {

    private lazy val namedFunctions = modules.flatMap(module => module.functions).map(f => (f.name, f)).toMap

    def functions: Iterable[value.Function] =
        namedFunctions.values

    def function(name: String): Option[value.Function] =
        namedFunctions.get(name)
}

object LLVMProject {
    def apply(modules_paths: Iterable[String]): LLVMProject = {
        val modules = modules_paths.map(path => Reader.readIR(path).get)
        val project = new LLVMProject(modules)
        project
    }
}
