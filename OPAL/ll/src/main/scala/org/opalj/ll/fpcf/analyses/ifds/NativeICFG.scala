/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll
package fpcf
package analyses
package ifds

import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.ifds.ICFG
import org.opalj.ll.fpcf.analyses.cg.SimpleNativeCallGraphKey
import org.opalj.ll.llvm.value.Call
import org.opalj.ll.llvm.value.Function

/**
 * This represents the icfg of the native functions of a LLVM program
 *
 * @author Nicolas Gross
 */
abstract class NativeICFG(project: SomeProject) extends ICFG[NativeFunction, LLVMStatement] {
    implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    /**
     * Gets the set of all methods possibly called at some statement.
     *
     * @param statement The statement.
     * @return All callables possibly called at the statement or None, if the statement does not
     *         contain a call.
     */
    override def getCalleesIfCallStatement(
        statement: LLVMStatement
    ): Option[Set[_ <: NativeFunction]] = {
        statement.instruction match {
            case call: Call => Some(resolveCallee(call))
            case _          => None
        }
    }

    def resolveCallee(call: Call): Set[_ <: NativeFunction] = call.calledValue match {
        case function: Function               => Set(LLVMFunction(function))
        case _ if JNICallUtil.isJNICall(call) => JNICallUtil.resolve(call)
        case _                                => Set()
    }

    override def getCallers(callee: NativeFunction): Set[LLVMStatement] =
        project.get(SimpleNativeCallGraphKey)(callee).getOrElse(Set.empty).map(LLVMStatement)
}
