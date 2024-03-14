/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package interpretation

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

import org.opalj.ai.FormalParametersOriginOffset
import org.opalj.ai.ImmediateVMExceptionsOriginOffset
import org.opalj.br.DefinedMethod
import org.opalj.br.ObjectType
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.string_definition.StringConstancyType
import org.opalj.br.fpcf.properties.string_definition.StringTreeDynamicString
import org.opalj.fpcf.ProperPropertyComputationResult

/**
 * Processes expressions that are relevant in order to determine which value(s) the string value at a given def site
 * might have.
 *
 * [[InterpretationHandler]]s of any level may use [[StringInterpreter]]s from their level or any level below.
 * [[StringInterpreter]]s defined in the [[interpretation]] package may be used by any level.
 *
 * @author Maximilian Rüsch
 */
abstract class InterpretationHandler {

    def analyze(entity: DefSiteEntity): ProperPropertyComputationResult = {
        val pc = entity.pc
        implicit val defSiteState: DefSiteState = DefSiteState(pc, entity.dm, entity.tac)
        if (pc <= FormalParametersOriginOffset) {
            if (pc == -1 || pc <= ImmediateVMExceptionsOriginOffset) {
                return StringInterpreter.computeFinalResult(pc, StringConstancyInformation.lb)
            } else {
                return StringInterpreter.computeFinalResult(pc, StringConstancyInformation.getElementForParameterPC(pc))
            }
        }

        processNewDefSitePC(pc)
    }

    protected def processNewDefSitePC(pc: Int)(implicit state: DefSiteState): ProperPropertyComputationResult
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

    def getStringConstancyInformationForReplace: StringConstancyInformation =
        StringConstancyInformation(StringConstancyType.REPLACE, StringTreeDynamicString)

    def getEntityForDefSite(defSite: Int)(implicit state: DefSiteState): DefSiteEntity =
        getEntityForPC(pcOfDefSite(defSite)(state.tac.stmts))

    def getEntityForDefSite(defSite: Int, dm: DefinedMethod, tac: TAC): DefSiteEntity =
        getEntityForPC(pcOfDefSite(defSite)(tac.stmts), dm, tac)

    def getEntityForPC(pc: Int)(implicit state: DefSiteState): DefSiteEntity =
        getEntityForPC(pc, state.dm, state.tac)

    def getEntity(state: DefSiteState): DefSiteEntity =
        getEntityForPC(state.pc, state.dm, state.tac)

    def getEntityForPC(pc: Int, dm: DefinedMethod, tac: TAC): DefSiteEntity =
        DefSiteEntity(pc, dm, tac)
}
