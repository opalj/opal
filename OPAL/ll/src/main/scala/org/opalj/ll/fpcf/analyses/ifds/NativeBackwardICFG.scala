/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll
package fpcf
package analyses
package ifds

import org.opalj.br.analyses.SomeProject

/**
 * An ICFG for a native IFDS backwards analysis.
 *
 * @param project the project to which the ICFG belongs.
 *
 * @author Nicolas Gross
 */
class NativeBackwardICFG(project: SomeProject) extends NativeICFG(project) {
    /**
     * Determines the statements at which the analysis starts.
     *
     * @param callable The analyzed callable.
     * @return The statements at which the analysis starts.
     */
    override def startStatements(callable: NativeFunction): Set[LLVMStatement] = callable match {
        case LLVMFunction(function) =>
            if (function.basicBlockCount == 0) {
                throw new IllegalArgumentException(s"${callable} does not contain any basic blocks and likely should not be in scope of the analysis")
            }
            function.exitBlocks.map(bb => LLVMStatement(bb.lastInstruction)).toSet
    }

    /**
     * Determines the statement, that will be analyzed after some other `statement`.
     *
     * @param statement The source statement.
     * @return The successor statements
     */
    override def nextStatements(statement: LLVMStatement): Set[LLVMStatement] =
        statement.instruction.previous match {
            case Some(i) => Set(LLVMStatement(i))
            case None    => statement.basicBlock.predecessors.map(bb => LLVMStatement(bb.lastInstruction))
        }

    /**
     * Determines whether the statement is an exit statement.
     *
     * @param statement The source statement.
     * @return Whether the statement flow may exit its callable (function/method)
     */
    override def isExitStatement(statement: LLVMStatement): Boolean = statement.function match {
        case LLVMFunction(function) => function.entryBlock.firstInstruction.equals(statement.instruction)
    }
}
