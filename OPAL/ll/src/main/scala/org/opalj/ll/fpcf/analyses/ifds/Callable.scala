/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds
import org.opalj.br.Method
import org.opalj.ll.llvm.Function

abstract class Callable

case class LLVMFunction(val function: Function) extends Callable
case class JavaMethod(val method: Method) extends Callable
