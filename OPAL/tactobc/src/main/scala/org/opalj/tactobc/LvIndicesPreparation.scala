package org.opalj.tactobc

import scala.collection.mutable

import org.opalj.br.Method
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.tac.ArrayLength
import org.opalj.tac.ArrayLoad
import org.opalj.tac.ArrayStore
import org.opalj.tac.Assignment
import org.opalj.tac.BinaryExpr
import org.opalj.tac.Checkcast
import org.opalj.tac.Compare
import org.opalj.tac.DUVar
import org.opalj.tac.Expr
import org.opalj.tac.ExprStmt
import org.opalj.tac.GetField
import org.opalj.tac.If
import org.opalj.tac.InstanceOf
import org.opalj.tac.InvokedynamicFunctionCall
import org.opalj.tac.NewArray
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.PrefixExpr
import org.opalj.tac.PrimitiveTypecastExpr
import org.opalj.tac.PutField
import org.opalj.tac.PutStatic
import org.opalj.tac.ReturnValue
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.StaticMethodCall
import org.opalj.tac.Stmt
import org.opalj.tac.Switch
import org.opalj.tac.Throw
import org.opalj.tac.UVar
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.VirtualMethodCall
import org.opalj.value.ValueInformation

/**
 * Handles the initial preparation of local variable (LV) indices
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
                stmt match {
                    case Assignment(_, targetVar, expr) =>
                        collectDUVarFromExpr(targetVar, duVars)
                        collectDUVarFromExpr(expr, duVars)
                    case If(_, left, _, right, _) =>
                        collectDUVarFromExpr(left, duVars)
                        collectDUVarFromExpr(right, duVars)
                    case VirtualMethodCall(_, _, _, _, _, receiver, params) =>
                        collectDUVarFromExpr(receiver, duVars)
                        for (param <- params) {
                            collectDUVarFromExpr(param, duVars)
                        }
                    case PutField(_, _, _, _, objRef, value) =>
                        collectDUVarFromExpr(objRef, duVars)
                        collectDUVarFromExpr(value, duVars)
                    case PutStatic(_, _, _, _, value) =>
                        collectDUVarFromExpr(value, duVars)
                    case StaticMethodCall(_, _, _, _, _, params) =>
                        for (param <- params) {
                            collectDUVarFromExpr(param, duVars)
                        }
                    case NonVirtualMethodCall(_, _, _, _, _, receiver, params) =>
                        collectDUVarFromExpr(receiver, duVars)
                        for (param <- params) {
                            collectDUVarFromExpr(param, duVars)
                        }
                    case ReturnValue(_, expr) =>
                        collectDUVarFromExpr(expr, duVars)
                    case ArrayStore(_, arrayRef, index, value) =>
                        collectDUVarFromExpr(arrayRef, duVars)
                        collectDUVarFromExpr(index, duVars)
                        collectDUVarFromExpr(value, duVars)
                    case ExprStmt(_, expr) =>
                        collectDUVarFromExpr(expr, duVars)
                    case Throw(_, exception) =>
                        collectDUVarFromExpr(exception, duVars)
                    case Switch(_, _, index, _) =>
                        collectDUVarFromExpr(index, duVars)
                    case Checkcast(_, value, _) =>
                        collectDUVarFromExpr(value, duVars)
                    case _ =>
                }
        }

        val uVarToLVIndex = mutable.Map[IntTrieSet, Int]()
        val nextLVIndexAfterParameters =
            mapParametersAndPopulate(method, uVarToLVIndex)
        collectAllUVarsAndPopulateUVarToLVIndexMap(duVars, uVarToLVIndex, nextLVIndexAfterParameters)
        uVarToLVIndex
    }

    /**
     * Populates the `uVarToLVIndex` map with unique LV indices for each variable in the given DUVars list.
     *
     * @param duVars ListBuffer containing all DUVars of the method.
     * @return A map where keys are def-sites of UVars and values are their corresponding LV indices.
     */
    private def collectAllUVarsAndPopulateUVarToLVIndexMap(
        duVars:         mutable.ListBuffer[DUVar[_]],
        uVarToLVIndex:  mutable.Map[IntTrieSet, Int],
        initialLVIndex: Int
    ): Unit = {
        var nextLVIndex = initialLVIndex
        duVars.toArray.foreach {
            case uVar: UVar[_] => nextLVIndex = populateUVarToLVIndexMap(uVar, uVarToLVIndex, nextLVIndex)
            case _             =>
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

        method.descriptor.parameterTypes.zipWithIndex.foreach { case (tpe, index) =>
            // for some reason defSite -1 is *always* reserved for 'this' so we always start at -2 and then go further down per parameter (-3, -4, etc.)
            uVarToLVIndex.getOrElseUpdate(IntTrieSet(-(index + 2)), nextLVIndex)
            nextLVIndex += (if (tpe.isDoubleType || tpe.isLongType) 2 else 1)
        }
        if (!method.isStatic) {
            uVarToLVIndex.getOrElseUpdate(IntTrieSet(-1), 0)
        }
        nextLVIndex
    }

    /**
     * Populates the `uVarToLVIndex` map with unique LV indices for each unique UVar.
     *
     * @param uVar A variable used in the method.
     */
    private def populateUVarToLVIndexMap(uVar: UVar[_], uVarToLVIndex: mutable.Map[IntTrieSet, Int], initialLVIndex: Int): Int = {
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
        expr match {
            case duVar: DUVar[_]           => duVars += duVar
            case binaryExpr: BinaryExpr[_] => collectDUVarFromBinaryExpr(binaryExpr, duVars)
            case virtualFunctionCallExpr: VirtualFunctionCall[_] =>
                collectDUVarFromVirtualMethodCall(virtualFunctionCallExpr, duVars)
            case staticFunctionCallExpr: StaticFunctionCall[_] =>
                collectDUVarFromStaticFunctionCall(staticFunctionCallExpr, duVars)
            case primitiveTypecaseExpr: PrimitiveTypecastExpr[_] =>
                collectDUVarFromPrimitiveTypeCastExpr(primitiveTypecaseExpr, duVars)
            case arrayLengthExpr: ArrayLength[_] => collectDUVarFromArrayLengthExpr(arrayLengthExpr, duVars)
            case arrayLoadExpr: ArrayLoad[_]     => collectDUVarFromArrayLoadExpr(arrayLoadExpr, duVars)
            case newArrayExpr: NewArray[_]       => collectDUVarFromNewArrayExpr(newArrayExpr, duVars)
            case invokedynamicFunctionCall: InvokedynamicFunctionCall[_] =>
                collectDUVarFromInvokedynamicFunctionCall(invokedynamicFunctionCall, duVars)
            case getField: GetField[_]     => collectDUVarFromGetField(getField, duVars)
            case compare: Compare[_]       => collectDUVarFromCompare(compare, duVars)
            case prefixExpr: PrefixExpr[_] => collectDUVarFromPrefixExpr(prefixExpr, duVars)
            case instanceOf: InstanceOf[_] => collectDUVarFromInstanceOf(instanceOf, duVars)
            case _                         =>
        }
    }

    private def collectDUVarFromInstanceOf(instanceOf: InstanceOf[_], duVars: mutable.ListBuffer[DUVar[_]]): Unit = {
        collectDUVarFromExpr(instanceOf.value, duVars)
    }

    private def collectDUVarFromPrefixExpr(prefixExpr: PrefixExpr[_], duVars: mutable.ListBuffer[DUVar[_]]): Unit = {
        collectDUVarFromExpr(prefixExpr.operand, duVars)
    }

    private def collectDUVarFromCompare(compare: Compare[_], duVars: mutable.ListBuffer[DUVar[_]]): Unit = {
        collectDUVarFromExpr(compare.left, duVars)
        collectDUVarFromExpr(compare.right, duVars)
    }

    private def collectDUVarFromGetField(getField: GetField[_], duVars: mutable.ListBuffer[DUVar[_]]): Unit = {
        collectDUVarFromExpr(getField.objRef, duVars)
    }

    private def collectDUVarFromInvokedynamicFunctionCall(
        invokedynamicFunctionCall: InvokedynamicFunctionCall[_],
        duVars:                    mutable.ListBuffer[DUVar[_]]
    ): Unit = {
        // Process each parameter and collect from each
        for (param <- invokedynamicFunctionCall.params) {
            collectDUVarFromExpr(param, duVars)
        }
    }

    private def collectDUVarFromNewArrayExpr(newArrayExpr: NewArray[_], duVars: mutable.ListBuffer[DUVar[_]]): Unit = {
        for (count <- newArrayExpr.counts) {
            collectDUVarFromExpr(count, duVars)
        }
        // tpe does not contain any expr
    }

    /**
     * Traverses a `ArrayLoad` expr to collect all DUVars embedded within it.
     *
     * @param arrayLoadExpr The `ArrayLength` expr to be traversed.
     * @param duVars ListBuffer to be extended with all DUVars found in the expression.
     */
    private def collectDUVarFromArrayLoadExpr(arrayLoadExpr: ArrayLoad[_], duVars: mutable.ListBuffer[DUVar[_]]): Unit = {
        collectDUVarFromExpr(arrayLoadExpr.index, duVars)
        collectDUVarFromExpr(arrayLoadExpr.arrayRef, duVars)
    }

    /**
     * Traverses a `ArrayLength` expr to collect all DUVars embedded within it.
     *
     * @param arrayLength The `ArrayLength` expr to be traversed.
     * @param duVars ListBuffer to be extended with all DUVars found in the expression.
     */
    private def collectDUVarFromArrayLengthExpr(arrayLength: ArrayLength[_], duVars: mutable.ListBuffer[DUVar[_]]): Unit = {
        collectDUVarFromExpr(arrayLength.arrayRef, duVars)
    }

    /**
     * Traverses a `PrimitiveTypeCastExpr` to collect all DUVars embedded within it.
     *
     * @param primitiveTypecaseExpr The `PrimitiveTypecastExpr` to be traversed.
     * @param duVars ListBuffer to be extended with all DUVars found in the expression.
     */
    private def collectDUVarFromPrimitiveTypeCastExpr(
        primitiveTypecaseExpr: PrimitiveTypecastExpr[_],
        duVars:                mutable.ListBuffer[DUVar[_]]
    ): Unit = {
        collectDUVarFromExpr(primitiveTypecaseExpr.operand, duVars)
    }

    /**
     * Traverses a `StaticFunctionCall` expression to collect all DUVars embedded within it.
     *
     * @param staticFunctionCallExpr The `StaticFunctionCall` expression to be traversed.
     * @param duVars ListBuffer to be extended with all DUVars found in the expression.
     */
    private def collectDUVarFromStaticFunctionCall(
        staticFunctionCallExpr: StaticFunctionCall[_],
        duVars:                 mutable.ListBuffer[DUVar[_]]
    ): Unit = {
        // Process each parameter and collect from each
        for (param <- staticFunctionCallExpr.params) {
            collectDUVarFromExpr(param, duVars)
        }
    }

    /**
     * Traverses a `BinaryExpr` to collect all DUVars embedded within it.
     *
     * @param binaryExpr The `BinaryExpr` to be traversed.
     * @param duVars ListBuffer to be extended with all DUVars found in the expression.
     */
    private def collectDUVarFromBinaryExpr(binaryExpr: BinaryExpr[_], duVars: mutable.ListBuffer[DUVar[_]]): Unit = {
        collectDUVarFromExpr(binaryExpr.left, duVars)
        collectDUVarFromExpr(binaryExpr.right, duVars)
    }

    /**
     * Traverses a `VirtualFunctionCall` expression to collect all DUVars embedded within it.
     *
     * @param virtualFunctionCallExpr The `VirtualFunctionCall` expression to be traversed.
     * @param duVars ListBuffer to be extended with all DUVars found in the expression.
     */
    private def collectDUVarFromVirtualMethodCall(
        virtualFunctionCallExpr: VirtualFunctionCall[_],
        duVars:                  mutable.ListBuffer[DUVar[_]]
    ): Unit = {
        collectDUVarFromExpr(virtualFunctionCallExpr.receiver, duVars)
        for (param <- virtualFunctionCallExpr.params) {
            collectDUVarFromExpr(param, duVars)
        }
    }
}
