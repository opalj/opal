/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds
import org.opalj.br.Method
import org.opalj.ifds.Callable
import org.opalj.ll.llvm.value.Function

abstract class NativeFunction extends Callable {
    def name: String
}

case class LLVMFunction(function: Function) extends NativeFunction {
    override def name: String = function.name
    override def signature: String = function.name // TODO: add signature
}

case class JNIMethod(method: Method) extends NativeFunction {
    override def name: String = method.name
    override def signature: String = method.toString
}
