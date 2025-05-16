/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac2bc

import scala.collection.mutable

import org.opalj.ba.CodeElement
import org.opalj.ba.LabelElement
import org.opalj.br.MethodDescriptor
import org.opalj.br.analyses.SomeProject
import org.opalj.br.instructions.RewriteLabel
import org.opalj.tac.AITACode
import org.opalj.tac.DUVar
import org.opalj.tac.Expr
import org.opalj.tac.Stmt
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.UVar
import org.opalj.tac.V
import org.opalj.value.ValueInformation

object TACtoBC {

    /**
     * Converts the TAC Stmts of a single method into bytecode instructions.
     *
     * This helper method processes one method's TAC representation at a time, converting it into a sequence
     * of bytecode instructions. It handles various types of TAC statements and expressions, translating them
     * into their equivalent bytecode form.
     *
     * @param method method to be translated
     * @param tac TAC representation of a method to be converted into bytecode.
     * @return A Sequence of bytecode instructions representing the method's functionality
     */
    def translateTACtoBC(
        methodDescriptor: MethodDescriptor,
        isStaticMethod:   Boolean,
        tac:              AITACode[TACMethodParameter, ValueInformation]
    )(implicit project: SomeProject): IndexedSeq[CodeElement[Nothing]] = {
        val tacStmts = tac.stmts.zipWithIndex
        // fill tacToLVIndexMap
        val tacToLVIndex = prepareLvIndices(methodDescriptor, isStaticMethod, tacStmts)
        translateStmtsToInstructions(tacStmts, tacToLVIndex)
    }

    /**
     * Prepares local variable (LV) indices for the given method by:
     * 1. Collecting all defined-use variables (DUVars) from the method's statements.
     * 2. Assigning LV indices to method parameters.
     * 3. Populating the `tacToLVIndex` map with unique LV indices for each unique variable.
     *
     * @param method Method which the Array 'tacStmts' belongs to
     * @param tacStmts Array of tuples where each tuple contains a TAC statement and its index.
     */
    private def prepareLvIndices(
        methodDescriptor: MethodDescriptor,
        isStaticMethod:   Boolean,
        tacStmts:         Array[(Stmt[DUVar[ValueInformation]], Int)]
    ): Map[Int, Int] = {
        val tacToLVIndex = mutable.Map[Int, Int]()
        var nextLVIndex = populateLVIndicesForParameters(methodDescriptor, isStaticMethod, tacToLVIndex)
        tacStmts.foreach {
            case (stmt, _) => stmt.forallSubExpressions { subExpr =>
                    nextLVIndex = populateLVIndicesForExpression(subExpr, tacToLVIndex, nextLVIndex)
                    true
                }
        }
        tacToLVIndex.toMap
    }

    /**
     * Populates the `tacToLVIndex` map with unique LV indices for each parameter from the given method descriptor.
     *
     * @param methodDescriptor Descriptor giving the parameter types to assign LV indices for
     * @param isStaticMethod True if the method is a static method without a receiver object (this)
     * @param tacToLVIndex Map to be filled with indices
     */
    private def populateLVIndicesForParameters(
        methodDescriptor: MethodDescriptor,
        isStaticMethod:   Boolean,
        tacToLVIndex:     mutable.Map[Int, Int]
    ) = {
        var nextLVIndex = 0

        if (!isStaticMethod) {
            tacToLVIndex(-1) = 0
            nextLVIndex = 1
        }

        methodDescriptor.parameterTypes.zipWithIndex.foreach { case (tpe, index) =>
            // defSite -1 is reserved for 'this' so we always start at -2 and then go further down per parameter (-3, -4, etc.)
            tacToLVIndex(-(index + 2)) = nextLVIndex
            nextLVIndex += tpe.computationalType.operandSize
        }

        nextLVIndex
    }

    /**
     * Populates the `tacToLVIndex` map with unique LV indices for each variable in the given expression.
     *
     * @param expr The expression to extract variables from
     * @param tacToLVIndex Map to be filled with indices
     * @param initialLVIndex The first index to populate
     */
    private def populateLVIndicesForExpression(
        expr:           Expr[V],
        tacToLVIndex:   mutable.Map[Int, Int],
        initialLVIndex: Int
    ): Int = {
        expr match {
            case uVar: UVar[ValueInformation] => populatetacToLVIndexMap(uVar, tacToLVIndex, initialLVIndex)
            case _ =>
                var nextLVIndex = initialLVIndex
                expr.forallSubExpressions { subExpr =>
                    nextLVIndex = populateLVIndicesForExpression(subExpr, tacToLVIndex, nextLVIndex)
                    true
                }
                nextLVIndex
        }
    }

    /**
     * Populates the `tacToLVIndex` map with unique LV indices for each unique UVar.
     *
     * @param uVar A variable used in the method.
     */
    private def populatetacToLVIndexMap(
        uVar:           UVar[ValueInformation],
        tacToLVIndex:   mutable.Map[Int, Int],
        initialLVIndex: Int
    ): Int = {
        val indexOption = uVar.definedBy.iterator
            .find(defSite => tacToLVIndex.contains(defSite))
            .map(defSite => tacToLVIndex(defSite))

        val (index, nextLVIndex) = indexOption match {
            case Some(index) =>
                (index, initialLVIndex)
            case None =>
                (initialLVIndex, initialLVIndex + uVar.cTpe.operandSize)
        }

        uVar.definedBy.foreach(defSite => tacToLVIndex(defSite) = index)

        nextLVIndex
    }

    /**
     * Translates TAC statements to bytecode instructions.
     *
     * This method iterates over the given TAC statements, processes each statement according to its type,
     * generates the corresponding bytecode instructions.
     *
     * @param tacStmts Array of tuples where each tuple contains a TAC statement and its index
     * @param tacToLVIndex Map that holds information about what variable belongs to which register
     */
    def translateStmtsToInstructions(
        tacStmts:     Array[(Stmt[DUVar[ValueInformation]], Int)],
        tacToLVIndex: Map[Int, Int]
    )(implicit project: SomeProject): IndexedSeq[CodeElement[Nothing]] = {

        // generate Label for each TAC-Stmt -> index in TAC-Array = corresponding label
        // e.g. labelMap(2) = RewriteLabel of TAC-Statement at index 2
        val labels = tacStmts.map(_ => RewriteLabel())

        // list of all CodeElements including bytecode instructions as well as pseudo instructions
        val code = mutable.ListBuffer[CodeElement[Nothing]]()

        tacStmts.foreach { case (stmt, tacIndex) =>
            // add label to the list
            code += LabelElement(labels(tacIndex))
            StmtProcessor.processStmt(stmt, tacToLVIndex, labels, code)
        }
        code.toIndexedSeq
    }
}
