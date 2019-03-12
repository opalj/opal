/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.interpretation

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.Property
import org.opalj.value.ValueInformation
import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel
import org.opalj.br.fpcf.properties.string_definition.StringConstancyType
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.tac.Assignment
import org.opalj.tac.Expr
import org.opalj.tac.New
import org.opalj.tac.Stmt
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.fpcf.analyses.string_analysis.V
import org.opalj.tac.DUVar
import org.opalj.tac.GetField
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.TACStmts
import org.opalj.tac.fpcf.analyses.string_analysis.InterproceduralStringAnalysis

abstract class InterpretationHandler(tac: TACode[TACMethodParameter, DUVar[ValueInformation]]) {

    protected val stmts: Array[Stmt[DUVar[ValueInformation]]] = tac.stmts
    protected val cfg: CFG[Stmt[DUVar[ValueInformation]], TACStmts[DUVar[ValueInformation]]] =
        tac.cfg

    /**
     * A list of definition sites that have already been processed. Store it as a map for constant
     * loop-ups (the value is not relevant and thus set to [[Unit]]).
     */
    protected val processedDefSites: mutable.Map[Int, Unit] = mutable.Map()

    /**
     * Processes a given definition site. That is, this function determines the interpretation of
     * the specified instruction.
     *
     * @param defSite The definition site to process. Make sure that (1) the value is >= 0, (2) it
     *                actually exists, and (3) can be processed by one of the subclasses of
     *                [[AbstractStringInterpreter]] (in case (3) is violated, an
     *                [[IllegalArgumentException]] will be thrown.
     * @param params For a (precise) interpretation, (method / function) parameter values might be
     *               necessary. They can be leveraged using this value. The implementing classes
     *               should make sure that (1) they handle the case when no parameters are given
     *               and (2)they have a proper mapping from the definition sites within used methods
     *               to the indices in `params` (as the definition sites of parameters are < 0).
     * @return Returns the result of the interpretation. Note that depending on the concrete
     *         interpreter either a final or an intermediate result can be returned!
     *         In case the rules listed above or the ones of the different concrete interpreters are
     *         not met, the neutral [[org.opalj.br.fpcf.properties.StringConstancyProperty]] element
     *         will be encapsulated in the result (see
     *         [[org.opalj.br.fpcf.properties.StringConstancyProperty.isTheNeutralElement]]).
     *         The entity of the result will be the given `defSite`.
     */
    def processDefSite(
        defSite: Int,
        params:  List[Seq[StringConstancyInformation]] = List()
    ): EOptionP[Entity, Property]

    /**
     * [[InterpretationHandler]]s keeps an internal state for correct and faster processing. As
     * long as a single object within a CFG is analyzed, there is no need to reset the state.
     * However, when analyzing a second object (even the same object) it is necessary to call
     * `reset` to reset the internal state. Otherwise, incorrect results will be produced.
     * (Alternatively, another instance of an implementation of [[InterpretationHandler]] could be
     * instantiated.)
     */
    def reset(): Unit = {
        processedDefSites.clear()
    }

}

object InterpretationHandler {

    /**
     * Checks whether an expression contains a call to [[StringBuilder#toString]] or
     * [[StringBuffer#toString]].
     *
     * @param expr The expression that is to be checked.
     * @return Returns true if `expr` is a call to `toString` of [[StringBuilder]] or
     *         [[StringBuffer]].
     */
    def isStringBuilderBufferToStringCall(expr: Expr[V]): Boolean =
        expr match {
            case VirtualFunctionCall(_, clazz, _, name, _, _, _) ⇒
                val className = clazz.mostPreciseObjectType.fqn
                (className == "java/lang/StringBuilder" || className == "java/lang/StringBuffer") &&
                    name == "toString"
            case _ ⇒ false
        }

    /**
     * Checks whether the given expression is a string constant / string literal.
     *
     * @param expr The expression to check.
     * @return Returns `true` if the given expression  is a string constant / literal and `false`
     *         otherwise.
     */
    def isStringConstExpression(expr: Expr[V]): Boolean = if (expr.isStringConst) {
        true
    } else {
        if (expr.isVar) {
            val value = expr.asVar.value
            value.isReferenceValue && value.asReferenceValue.upperTypeBound.exists {
                _.toJava == "java.lang.String"
            }
        } else {
            false
        }
    }

    /**
     * Returns `true` if the given expressions is a primitive number type
     */
    def isPrimitiveNumberTypeExpression(expr: Expr[V]): Boolean =
        expr.asVar.value.isPrimitiveValue &&
            InterproceduralStringAnalysis.isSupportedPrimitiveNumberType(
                expr.asVar.value.asPrimitiveValue.primitiveType.toJava
            )

    /**
     * Checks whether an expression contains a call to [[StringBuilder#append]] or
     * [[StringBuffer#append]].
     *
     * @param expr The expression that is to be checked.
     * @return Returns true if `expr` is a call to `append` of [[StringBuilder]] or
     *         [[StringBuffer]].
     */
    def isStringBuilderBufferAppendCall(expr: Expr[V]): Boolean = {
        expr match {
            case VirtualFunctionCall(_, clazz, _, name, _, _, _) ⇒
                val className = clazz.toJavaClass.getName
                (className == "java.lang.StringBuilder" || className == "java.lang.StringBuffer") &&
                    name == "append"
            case _ ⇒ false
        }
    }

    /**
     * Helper function for [[findDefSiteOfInit]].
     */
    private def findDefSiteOfInitAcc(
        toString: VirtualFunctionCall[V], stmts: Array[Stmt[V]]
    ): List[Int] = {
        // TODO: Check that we deal with an instance of AbstractStringBuilder
        if (toString.name != "toString") {
            return List()
        }

        val defSites = ListBuffer[Int]()
        val stack = mutable.Stack[Int](toString.receiver.asVar.definedBy.filter(_ >= 0).toArray: _*)
        val seenElements: mutable.Map[Int, Unit] = mutable.Map()
        while (stack.nonEmpty) {
            val next = stack.pop()
            stmts(next) match {
                case a: Assignment[V] ⇒
                    a.expr match {
                        case _: New ⇒
                            defSites.append(next)
                        case vfc: VirtualFunctionCall[V] ⇒
                            val recDefSites = vfc.receiver.asVar.definedBy.filter(_ >= 0).toArray
                            // recDefSites.isEmpty => Definition site is a parameter => Use the
                            // current function call as a def site
                            if (recDefSites.nonEmpty) {
                                stack.pushAll(recDefSites.filter(!seenElements.contains(_)))
                            } else {
                                defSites.append(next)
                            }
                        case _: GetField[V] ⇒
                            defSites.append(next)
                        case _ ⇒ // E.g., NullExpr
                    }
                case _ ⇒
            }
            seenElements(next) = Unit
        }

        defSites.sorted.toList
    }

    /**
     * Determines the definition sites of the initializations of the base object of `duvar`. This
     * function assumes that the definition sites refer to `toString` calls.
     *
     * @param duvar The `DUVar` to get the initializations of the base object for.
     * @param stmts The search context for finding the relevant information.
     * @return Returns the definition sites of the base object.
     */
    def findDefSiteOfInit(duvar: V, stmts: Array[Stmt[V]]): List[Int] = {
        val defSites = ListBuffer[Int]()
        duvar.definedBy.foreach { ds ⇒
            defSites.appendAll(stmts(ds).asAssignment.expr match {
                case vfc: VirtualFunctionCall[V] ⇒ findDefSiteOfInitAcc(vfc, stmts)
                // The following case is, e.g., for {NonVirtual, Static}FunctionCalls
                case _                           ⇒ List(ds)
            })
        }
        // If no init sites could be determined, use the definition sites of the UVar
        if (defSites.isEmpty) {
            defSites.appendAll(duvar.definedBy.toArray)
        }

        defSites.distinct.sorted.toList
    }

    /**
     * Determines the [[New]] expressions that belongs to a given `duvar`.
     *
     * @param duvar The [[org.opalj.tac.DUVar]] to get the [[New]]s for.
     * @param stmts The context to search in, e.g., the surrounding method.
     * @return Returns all found [[New]] expressions.
     */
    def findNewOfVar(duvar: V, stmts: Array[Stmt[V]]): List[New] = {
        val news = ListBuffer[New]()

        // HINT: It might be that the search has to be extended to further cases
        duvar.definedBy.filter(_ >= 0).foreach { ds ⇒
            stmts(ds) match {
                // E.g., a call to `toString` or `append`
                case Assignment(_, _, vfc: VirtualFunctionCall[V]) ⇒
                    vfc.receiver.asVar.definedBy.filter(_ >= 0).foreach { innerDs ⇒
                        stmts(innerDs) match {
                            case Assignment(_, _, expr: New) ⇒
                                news.append(expr)
                            case Assignment(_, _, expr: VirtualFunctionCall[V]) ⇒
                                val exprReceiverVar = expr.receiver.asVar
                                // The "if" is to avoid endless recursion
                                if (duvar.definedBy != exprReceiverVar.definedBy) {
                                    news.appendAll(findNewOfVar(exprReceiverVar, stmts))
                                }
                            case _ ⇒
                        }
                    }
                case Assignment(_, _, newExpr: New) ⇒
                    news.append(newExpr)
                case _ ⇒
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
     * @return Returns a [[StringConstancyProperty]] element that describes the result of a
     *         `replace` operation. That is, the returned element currently consists of the value
     *         [[StringConstancyLevel.DYNAMIC]], [[StringConstancyType.REPLACE]], and
     *         [[StringConstancyInformation.UnknownWordSymbol]].
     */
    def getStringConstancyPropertyForReplace: StringConstancyProperty =
        StringConstancyProperty(StringConstancyInformation(
            StringConstancyLevel.DYNAMIC,
            StringConstancyType.REPLACE,
            StringConstancyInformation.UnknownWordSymbol
        ))

}
