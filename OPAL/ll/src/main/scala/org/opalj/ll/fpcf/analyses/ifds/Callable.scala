/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds
import org.opalj.ifds.Callable
import org.opalj.ll.llvm.value.Function

case class LLVMFunction(val function: Function) extends Callable {
    override def name: String = function.name
    override def signature: String = function.name // TODO: add signature
}

