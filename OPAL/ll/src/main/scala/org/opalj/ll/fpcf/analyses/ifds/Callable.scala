/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll
package fpcf
package analyses
package ifds

import org.opalj.br.Method
import org.opalj.ifds.Callable
import org.opalj.ll.llvm.value.Function

/**
 * This is a marker trait for Callables that represent a native function, on both the java and llvm side
 */
trait NativeFunction extends Callable

/**
 * Represents a Native Function on the LLVM side
 * @param function reference to the llvm function
 */
case class LLVMFunction(function: Function) extends NativeFunction {
    override def name: String = function.name
    override def signature: String = function.getSignature
}

/**
 * The method that represent the native function on the java side. This class extends the NativeFunction trait since it
 * is handled correspondingly in the ICFG
 * @param method the method object.
 */
case class JNIMethod(method: Method) extends NativeFunction {
    override def name: String = method.name
    override def signature: String = method.toString
}
