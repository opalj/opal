/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll

import org.opalj.ll.llvm.Module
import org.opalj.ll.llvm.Reader
import org.opalj.ll.llvm.value

class LLVMProject(val modules: Iterable[Module]) {
    def functions: Iterable[value.Function] =
        modules.flatMap(module => module.functions)

    def function(name: String): Option[value.Function] =
        functions.find(_.name == name)
}

object LLVMProject {
    def apply(modules_paths: Iterable[String]): LLVMProject = {
        val modules = modules_paths.map(path => Reader.readIR(path).get)
        val project = new LLVMProject(modules)
        project
    }
}
