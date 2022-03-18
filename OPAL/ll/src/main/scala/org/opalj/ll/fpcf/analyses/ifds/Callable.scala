/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds
import org.opalj.ll.llvm.Function
import org.opalj.ifds.Callable

case class LLVMFunction(val function: Function) extends Callable {
    override def name(): String = function.name()
    override def signature(): String = function.name() // TODO: add signature
}

