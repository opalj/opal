/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll

import org.opalj.ll.llvm.{Function, Module, Reader}

class LLVMProject(val modules: Iterable[Module]) {
    def functions(): Iterable[Function] = {
        modules.flatMap(module ⇒ module.functions())
    }
}

object LLVMProject {
    def apply(modules_paths: Iterable[String]): LLVMProject = {
        val modules = modules_paths.map(path ⇒ Reader.readIR(path).get)
        val project = new LLVMProject(modules)
        project
    }
}
