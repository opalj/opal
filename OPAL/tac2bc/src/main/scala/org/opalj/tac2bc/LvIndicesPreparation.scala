package org.opalj
package tac2bc

import scala.collection.mutable

import org.opalj.br.Method
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.tac.DUVar
import org.opalj.tac.Expr
import org.opalj.tac.Stmt
import org.opalj.tac.UVar
import org.opalj.value.ValueInformation

/**
 * Handles the initial preparation of local variable (LV) indicess
 * for translating three-address code (TAC) to bytecode. This involves collecting all
 * defined-use variables (DUVars) in the method, assigning LV indices to parameters,
 * and populating a map that assigns unique LV indices to each unique variable.
 *
 * Key responsibilities:
 * - Collect all DUVars from the method's statements.
 * - Assign LV indices to method parameters.
 * - Populate a map (`uVarToLVIndex`) with unique LV indices for each variable used in the method.
 */
object LvIndicesPreparation {

    /**
     * Prepares local variable (LV) indices for the given method by:
     * 1. Collecting all defined-use variables (DUVars) from the method's statements.
     * 2. Assigning LV indices to method parameters.
     * 3. Populating the `uVarToLVIndex` map with unique LV indices for each unique variable.
     *
     * @param method Method which the Array 'tacStmts' belongs to
     * @param tacStmts Array of tuples where each tuple contains a TAC statement and its index.
     */
    def prepareLvIndices(
        method:   Method,
        tacStmts: Array[(Stmt[DUVar[ValueInformation]], Int)]
    ): mutable.Map[IntTrieSet, Int] = {

        // container for all DUVars in the method
        val duVars = mutable.ListBuffer[DUVar[_]]()
        tacStmts.foreach {
            case (stmt, _) =>
                if (stmt.isAssignment)
                    collectDUVarFromExpr(stmt.asAssignment.targetVar, duVars)
                stmt.forallSubExpressions(subExpr => { collectDUVarFromExpr(subExpr, duVars); true })
        }
        val uVarToLVIndex = mutable.Map[IntTrieSet, Int]()
        val nextLVIndexAfterParameters = mapParametersAndPopulate(method, uVarToLVIndex)
        collectAllUVarsAndPopulateUVarToLVIndexMap(duVars, uVarToLVIndex, nextLVIndexAfterParameters)
        uVarToLVIndex
    }

    /**
     * Populates the `uVarToLVIndex` map with unique LV indices for each variable in the given DUVars list.
     *
     * @param duVars ListBuffer containing all DUVars of the method.
     * @param uVarToLVIndex Map to be filled with indices
     * @param initialLVIndex The initial index after having processed the parameters
     */
    private def collectAllUVarsAndPopulateUVarToLVIndexMap(
        duVars:         mutable.ListBuffer[DUVar[_]],
        uVarToLVIndex:  mutable.Map[IntTrieSet, Int],
        initialLVIndex: Int
    ): Unit = {
        var nextLVIndex = initialLVIndex
        duVars.toArray.foreach {
            case uVar: UVar[_] =>
                nextLVIndex = populateUVarToLVIndexMap(uVar, uVarToLVIndex, nextLVIndex)
            case _ => // we only need uVars
        }

    }

    /**
     * Iterates over the parameterTypes of the method and assigns defSites -2, -3, -4, ... to LVIndices starting at 0 or 1 (for static/non-static methods respectively)
     */
    private def mapParametersAndPopulate(
        method:        Method,
        uVarToLVIndex: mutable.Map[IntTrieSet, Int]
    ): Int = {
        var nextLVIndex = if (method.isStatic) 0 else 1
        if (!method.isStatic) {
            uVarToLVIndex.getOrElseUpdate(IntTrieSet(-1), 0)
        }

        method.descriptor.parameterTypes.zipWithIndex.foreach { case (tpe, index) =>
            // defSite -1 is reserved for 'this' so we always start at -2 and then go further down per parameter (-3, -4, etc.)
            uVarToLVIndex.getOrElseUpdate(IntTrieSet(-(index + 2)), nextLVIndex)
            nextLVIndex += tpe.computationalType.operandSize
        }

        nextLVIndex
    }

    /**
     * Populates the `uVarToLVIndex` map with unique LV indices for each unique UVar.
     *
     * @param uVar A variable used in the method.
     */
    private def populateUVarToLVIndexMap(
        uVar:           UVar[_],
        uVarToLVIndex:  mutable.Map[IntTrieSet, Int],
        initialLVIndex: Int
    ): Int = {
        var nextLVIndex = initialLVIndex
        val existingEntry = uVarToLVIndex.find { case (key, _) => key.intersect(uVar.defSites).nonEmpty }
        existingEntry match {
            case Some((existingDefSites, index)) =>
                val mergedDefSites = existingDefSites ++ uVar.defSites
                uVarToLVIndex -= existingDefSites
                uVarToLVIndex(mergedDefSites) = index

            case None =>
                uVarToLVIndex.getOrElseUpdate(uVar.defSites, nextLVIndex)
                nextLVIndex = incrementLVIndex(uVar, nextLVIndex)
        }
        nextLVIndex
    }

    /**
     * Increments the LV index appropriately based on the type of the UVar.
     *
     * @param uVar The UVar for which the LV index is to be incremented.
     * @param initialLVIndex current LV index
     * @return updated LV index based on the type of UVar
     */
    private def incrementLVIndex(uVar: UVar[_], initialLVIndex: Int): Int = {
        initialLVIndex + uVar.cTpe.operandSize
    }

    /**
     * Traverses an expression to collect all DUVars embedded within it.
     *
     * @param expr The expression to be traversed
     * @param duVars ListBuffer to be extended with all DUVars found in the expression.
     */
    private def collectDUVarFromExpr(expr: Expr[_], duVars: mutable.ListBuffer[DUVar[_]]): Unit = {
        if(expr.isVar) duVars += expr.asVar
        else expr.forallSubExpressions(subExpr => { collectDUVarFromExpr(subExpr, duVars); true})
    }
}
