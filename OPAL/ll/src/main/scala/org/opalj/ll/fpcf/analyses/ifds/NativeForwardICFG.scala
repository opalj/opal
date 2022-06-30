/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds

import org.opalj.br.analyses.{DeclaredMethodsKey, SomeProject}
import org.opalj.ifds.ICFG
import org.opalj.ll.llvm.value.{Call, Function, Instruction, Ret, Terminator}

class NativeForwardICFG(project: SomeProject) extends ICFG[NativeFunction, LLVMStatement] {
    implicit val declaredMethods = project.get(DeclaredMethodsKey)

    /**
     * Determines the statements at which the analysis starts.
     *
     * @param callable The analyzed callable.
     * @return The statements at which the analysis starts.
     */
    override def startStatements(callable: NativeFunction): Set[LLVMStatement] = callable match {
        case LLVMFunction(function) => {
            if (function.basicBlockCount == 0)
                throw new IllegalArgumentException(s"${callable} does not contain any basic blocks and likely should not be in scope of the analysis")
            Set(LLVMStatement(function.entryBlock.firstInstruction))
        }
    }

    /**
     * Determines the statement, that will be analyzed after some other `statement`.
     *
     * @param statement The source statement.
     * @return The successor statements
     */
    override def nextStatements(statement: LLVMStatement): Set[LLVMStatement] = {
        if (!statement.instruction.isTerminator) return Set(LLVMStatement(statement.instruction.next.get))
        statement.instruction.asInstanceOf[Instruction with Terminator].successors.map(LLVMStatement(_)).toSet
    }

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

    override def isExitStatement(statement: LLVMStatement): Boolean = statement.instruction match {
        case Ret(_) => true
        case _      => false
    }

    private def resolveCallee(call: Call): Set[_ <: NativeFunction] =
        if (call.calledValue.isInstanceOf[Function])
            Set(LLVMFunction(call.calledValue.asInstanceOf[Function]))
        else if (JNICallUtil.isJNICall(call))
            JNICallUtil.resolve(call)
        else Set()
}
