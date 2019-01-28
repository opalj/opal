/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.interpretation

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel
import org.opalj.br.fpcf.properties.string_definition.StringConstancyType
import org.opalj.tac.Assignment
import org.opalj.tac.Expr
import org.opalj.tac.New
import org.opalj.tac.Stmt
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.fpcf.analyses.string_analysis.V
import org.opalj.tac.TACStmts

abstract class InterpretationHandler(cfg: CFG[Stmt[V], TACStmts[V]]) {

    /**
     * The statements of the given [[cfg]].
     */
    protected val stmts: Array[Stmt[V]] = cfg.code.instructions
    /**
     * A list of definition sites that have already been processed.
     */
    protected val processedDefSites: ListBuffer[Int] = ListBuffer[Int]()

    /**
     * Processes a given definition site. That is, this function determines the interpretation of
     * the specified instruction.
     *
     * @param defSite The definition site to process. Make sure that (1) the value is >= 0, (2) it
     *                actually exists, and (3) can be processed by one of the subclasses of
     *                [[AbstractStringInterpreter]] (in case (3) is violated, an
     *                [[IllegalArgumentException]] will be thrown.
     * @return Returns a list of interpretations in the form of [[StringConstancyInformation]]. In
     *         case the rules listed above or the ones of the different processors are not met, an
     *         empty list will be returned.
     */
    def processDefSite(defSite: Int): List[StringConstancyInformation]

    /**
     * This function serves as a wrapper function for [[processDefSites]] in the sense that it
     * processes multiple definition sites. Thus, it may throw an exception as well if an expression
     * referenced by a definition site cannot be processed. The same rules as for [[processDefSite]]
     * apply.
     *
     * @param defSites The definition sites to process.
     *
     * @return Returns a list of lists of [[StringConstancyInformation]]. Note that this function
     *         preserves the order of the given `defSites`, i.e., the first element in the result
     *         list corresponds to the first element in `defSites` and so on. If a site could not be
     *         processed, the list for that site will be the empty list.
     */
    final def processDefSites(defSites: Array[Int]): List[List[StringConstancyInformation]] =
        defSites.length match {
            case 0 ⇒ List()
            case 1 ⇒ List(processDefSite(defSites.head))
            case _ ⇒ defSites.filter(_ >= 0).map(processDefSite).toList
        }

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
                val className = clazz.toJavaClass.getName
                (className == "java.lang.StringBuilder" || className == "java.lang.StringBuffer") &&
                    name == "toString"
            case _ ⇒ false
        }

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
        while (stack.nonEmpty) {
            val next = stack.pop()
            stmts(next) match {
                case a: Assignment[V] ⇒
                    a.expr match {
                        case _: New ⇒
                            defSites.append(next)
                        case vfc: VirtualFunctionCall[V] ⇒
                            stack.pushAll(vfc.receiver.asVar.definedBy.filter(_ >= 0).toArray)
                    }
                case _ ⇒
            }
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
            defSites.appendAll(
                findDefSiteOfInitAcc(stmts(ds).asAssignment.expr.asVirtualFunctionCall, stmts)
            )
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
                                news.appendAll(findNewOfVar(expr.receiver.asVar, stmts))
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
    def getConstancyInformationForDynamicInt: StringConstancyInformation =
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
    def getConstancyInformationForDynamicFloat: StringConstancyInformation =
        StringConstancyInformation(
            StringConstancyLevel.DYNAMIC,
            StringConstancyType.APPEND,
            StringConstancyInformation.FloatValue
        )

    /**
     * @return Returns a [[StringConstancyInformation]] element that describes a the result of a
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
