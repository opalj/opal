/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds

import org.opalj.br.analyses.{DeclaredMethodsKey, SomeProject}
import org.opalj.ifds.ICFG
import org.opalj.ll.llvm.value.{Call, Function}

abstract class NativeICFG(project: SomeProject) extends ICFG[NativeFunction, LLVMStatement] {
    implicit val declaredMethods = project.get(DeclaredMethodsKey)

    /**
     * Gets the set of all methods possibly called at some statement.
     *
     * @param statement The statement.
     * @return All callables possibly called at the statement or None, if the statement does not
     *         contain a call.
     */
    override def getCalleesIfCallStatement(statement: LLVMStatement): Option[collection.Set[_ <: NativeFunction]] = {
        statement.instruction match {
            case call: Call => Some(resolveCallee(call))
            case _          => None
        }
    }

    private def resolveCallee(call: Call): Set[_ <: NativeFunction] =
        if (call.calledValue.isInstanceOf[Function])
            Set(LLVMFunction(call.calledValue.asInstanceOf[Function]))
        else if (JNICallUtil.isJNICall(call))
            JNICallUtil.resolve(call)
        else Set()

    override def getCallers(callee: NativeFunction): Seq[(NativeFunction, Int)] = {
        // TODO
        ???
    }

    override def getStatement(callable: NativeFunction, index: Int): LLVMStatement = {
        // TODO
        ???
    }
}
