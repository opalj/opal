/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package interpretation

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

import org.opalj.ai.ImmediateVMExceptionsOriginOffset
import org.opalj.br.ObjectType
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel
import org.opalj.br.fpcf.properties.string_definition.StringConstancyType

/**
 * Processes expressions that are relevant in order to determine which value(s) the string value at a given def site
 * might have.
 *
 * [[InterpretationHandler]]s of any level may use [[StringInterpreter]]s from their level or any level below.
 * [[SingleStepStringInterpreter]]s defined in the [[interpretation]] package may be used by any level.
 *
 * @author Maximilian Rüsch
 */
abstract class InterpretationHandler[State <: ComputationState] {

    /**
     * Processes a given definition site. That is, this function determines the interpretation of
     * the specified instruction.
     *
     * @param defSite The definition site to process. Make sure that (1) the value is >= 0, (2) it actually exists, and
     *                (3) can be processed by one of the subclasses of [[StringInterpreter]] (in case (3) is violated,
     *                an [[IllegalArgumentException]] will be thrown).
     * @return Returns the result of the interpretation. Note that depending on the concrete
     *         interpreter either a final or an intermediate result can be returned!
     *         In case the rules listed above or the ones of the different concrete interpreters are
     *         not met, the neutral [[org.opalj.br.fpcf.properties.StringConstancyProperty]] element
     *         will be encapsulated in the result (see
     *         [[org.opalj.br.fpcf.properties.StringConstancyProperty.isTheNeutralElement]]).
     *         The entity of the result will be the given `defSite`.
     */
    def processDefSite(defSite: Int)(implicit state: State): IPResult = {
        val defSitePC = pcOfDefSite(defSite)(state.tac.stmts)

        if (state.fpe2ipr.contains(defSitePC) && state.fpe2ipr(defSitePC).isFinal) {
            return state.fpe2ipr(defSitePC)
        }

        if (defSite < 0) {
            val params = state.params.toList.map(_.toList)
            if (params.isEmpty || defSite == -1 || defSite <= ImmediateVMExceptionsOriginOffset) {
                state.fpe2ipr(defSitePC) = FinalIPResult.lb(state.dm, defSitePC)
                return FinalIPResult.lb(state.dm, defSitePC)
            } else {
                val sci = getParam(params, defSite)
                state.fpe2ipr(defSitePC) = FinalIPResult(sci, state.dm, defSitePC)
                return FinalIPResult(sci, state.dm, defSitePC)
            }
        }

        if (state.fpe2iprDependees.contains(defSitePC)) {
            val oldDependees = state.fpe2iprDependees(defSitePC)
            val updatedDependees = oldDependees._1.map {
                case ripr: RefinableIPResult => processDefSite(valueOriginOfPC(ripr.pc, state.tac.pcToIndex).get)
                case ipr                     => ipr
            }
            if (updatedDependees == oldDependees._1) {
                state.fpe2ipr(defSitePC)
            } else {
                state.fpe2iprDependees(defSitePC) = (updatedDependees, oldDependees._2)
                var newResult = state.fpe2ipr(defSitePC)
                for {
                    ipr <- updatedDependees
                    if !oldDependees._1.contains(ipr)
                } {
                    newResult = oldDependees._2(ipr)
                }
                newResult
            }
        } else {
            val result = processNewDefSite(defSite)
            state.fpe2ipr(defSitePC) = result
            result
        }
    }

    protected def processNewDefSite(defSite: Int)(implicit state: State): IPResult

    /**
     * This function takes parameters and a definition site and extracts the desired parameter from
     * the given list of parameters. Note that `defSite` is required to be <= -2.
     */
    protected def getParam(params: Seq[Seq[StringConstancyInformation]], defSite: Int): StringConstancyInformation = {
        val paramPos = Math.abs(defSite + 2)
        if (params.exists(_.length <= paramPos)) {
            // IMPROVE cant we just map each list of params with a nonexistent pos to lb and still reduce?
            StringConstancyInformation.lb
        } else {
            StringConstancyInformation.reduceMultiple(params.map(_(paramPos)).distinct)
        }
    }
}

object InterpretationHandler {

    /**
     * Checks whether an expression contains a call to [[StringBuilder#toString]] or [[StringBuffer#toString]].
     */
    def isStringBuilderBufferToStringCall(expr: Expr[V]): Boolean =
        expr match {
            case VirtualFunctionCall(_, clazz, _, "toString", _, _, _) =>
                clazz.mostPreciseObjectType == ObjectType.StringBuilder ||
                    clazz.mostPreciseObjectType == ObjectType.StringBuffer
            case _ => false
        }

    /**
     * Checks whether the given expression is a string constant / string literal.
     */
    def isStringConstExpression(expr: Expr[V]): Boolean = if (expr.isStringConst) {
        true
    } else {
        if (expr.isVar) {
            val value = expr.asVar.value
            value.isReferenceValue && value.asReferenceValue.upperTypeBound.exists { _ == ObjectType.String }
        } else {
            false
        }
    }

    /**
     * Returns `true` if the given expressions is a primitive number type
     */
    def isPrimitiveNumberTypeExpression(expr: Expr[V]): Boolean =
        expr.asVar.value.isPrimitiveValue &&
            StringAnalysis.isSupportedPrimitiveNumberType(
                expr.asVar.value.asPrimitiveValue.primitiveType.toJava
            )

    /**
     * Checks whether an expression contains a call to [[StringBuilder#append]] or [[StringBuffer#append]].
     */
    def isStringBuilderBufferAppendCall(expr: Expr[V]): Boolean = {
        expr match {
            case VirtualFunctionCall(_, clazz, _, "append", _, _, _) =>
                clazz.mostPreciseObjectType == ObjectType.StringBuilder ||
                    clazz.mostPreciseObjectType == ObjectType.StringBuffer
            case _ => false
        }
    }

    /**
     * Helper function for [[findDefSiteOfInit]].
     */
    private def findDefSiteOfInitAcc(
        toString: VirtualFunctionCall[V],
        stmts:    Array[Stmt[V]]
    ): List[Int] = {
        // TODO: Check that we deal with an instance of AbstractStringBuilder
        if (toString.name != "toString") {
            return List.empty
        }

        val defSites = ListBuffer[Int]()
        val stack = mutable.Stack[Int](toString.receiver.asVar.definedBy.filter(_ >= 0).toList: _*)
        val seenElements: mutable.Map[Int, Unit] = mutable.Map()
        while (stack.nonEmpty) {
            val next = stack.pop()
            stmts(next) match {
                case a: Assignment[V] =>
                    a.expr match {
                        case _: New =>
                            defSites.append(next)
                        case vfc: VirtualFunctionCall[V] =>
                            val recDefSites = vfc.receiver.asVar.definedBy.filter(_ >= 0).toArray
                            // recDefSites.isEmpty => Definition site is a parameter => Use the
                            // current function call as a def site
                            if (recDefSites.nonEmpty) {
                                stack.pushAll(recDefSites.filter(!seenElements.contains(_)))
                            } else {
                                defSites.append(next)
                            }
                        case _: GetField[V] =>
                            defSites.append(next)
                        case _ => // E.g., NullExpr
                    }
                case _ =>
            }
            seenElements(next) = ()
        }

        defSites.sorted.toList
    }

    /**
     * Determines the definition sites of the initializations of the base object of `duvar`. This
     * function assumes that the definition sites refer to `toString` calls.
     *
     * @param value The [[V]] to get the initializations of the base object for.
     * @param stmts The search context for finding the relevant information.
     * @return Returns the definition sites of the base object.
     */
    def findDefSiteOfInit(value: V, stmts: Array[Stmt[V]]): List[Int] = {
        val defSites = ListBuffer[Int]()
        value.definedBy.foreach { ds =>
            defSites.appendAll(stmts(ds).asAssignment.expr match {
                case vfc: VirtualFunctionCall[V] => findDefSiteOfInitAcc(vfc, stmts)
                // The following case is, e.g., for {NonVirtual, Static}FunctionCalls
                case _ => List(ds)
            })
        }
        // If no init sites could be determined, use the definition sites of the UVar
        if (defSites.isEmpty) {
            defSites.appendAll(value.definedBy.toArray)
        }

        defSites.distinct.sorted.toList
    }

    /**
     * Determines the [[New]] expressions that belongs to a given `duvar`.
     *
     * @param value The [[V]] to get the [[New]]s for.
     * @param stmts The context to search in, e.g., the surrounding method.
     * @return Returns all found [[New]] expressions.
     */
    def findNewOfVar(value: V, stmts: Array[Stmt[V]]): List[New] = {
        val news = ListBuffer[New]()

        // HINT: It might be that the search has to be extended to further cases
        value.definedBy.filter(_ >= 0).foreach { ds =>
            stmts(ds) match {
                // E.g., a call to `toString` or `append`
                case Assignment(_, _, vfc: VirtualFunctionCall[V]) =>
                    vfc.receiver.asVar.definedBy.filter(_ >= 0).foreach { innerDs =>
                        stmts(innerDs) match {
                            case Assignment(_, _, expr: New) =>
                                news.append(expr)
                            case Assignment(_, _, expr: VirtualFunctionCall[V]) =>
                                val exprReceiverVar = expr.receiver.asVar
                                // The "if" is to avoid endless recursion
                                if (value.definedBy != exprReceiverVar.definedBy) {
                                    news.appendAll(findNewOfVar(exprReceiverVar, stmts))
                                }
                            case _ =>
                        }
                    }
                case Assignment(_, _, newExpr: New) =>
                    news.append(newExpr)
                case _ =>
            }
        }

        news.toList
    }

    /**
     * @return Returns a [[StringConstancyInformation]] element that describes an `int` value.
     *         That is, the returned element consists of the value [[StringConstancyLevel.DYNAMIC]],
     *         [[StringConstancyType.APPEND]], and [[StringConstancyInformation.IntValue]].
     */
    def getConstancyInfoForDynamicInt: StringConstancyInformation =
        StringConstancyInformation(
            StringConstancyLevel.DYNAMIC,
            StringConstancyType.APPEND,
            StringConstancyInformation.IntValue
        )

    /**
     * @return Returns a [[StringConstancyInformation]] element that describes a `float` value.
     *         That is, the returned element consists of the value [[StringConstancyLevel.DYNAMIC]],
     *         [[StringConstancyType.APPEND]], and [[StringConstancyInformation.IntValue]].
     */
    def getConstancyInfoForDynamicFloat: StringConstancyInformation =
        StringConstancyInformation(
            StringConstancyLevel.DYNAMIC,
            StringConstancyType.APPEND,
            StringConstancyInformation.FloatValue
        )

    /**
     * @return Returns a [[StringConstancyInformation]] element that describes the result of a
     *         `replace` operation. That is, the returned element currently consists of the value
     *         [[StringConstancyLevel.DYNAMIC]], [[StringConstancyType.REPLACE]], and
     *         [[StringConstancyInformation.UnknownWordSymbol]].
     */
    def getStringConstancyInformationForReplace: StringConstancyInformation =
        StringConstancyInformation(
            StringConstancyLevel.DYNAMIC,
            StringConstancyType.REPLACE,
            StringConstancyInformation.UnknownWordSymbol
        )
}
