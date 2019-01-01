/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.interpretation

import org.opalj.br.cfg.CFG
import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.fpcf.properties.StringConstancyProperty
import org.opalj.fpcf.string_definition.properties.StringConstancyInformation
import org.opalj.fpcf.string_definition.properties.StringConstancyLevel
import org.opalj.fpcf.string_definition.properties.StringConstancyType
import org.opalj.tac.ArrayLoad
import org.opalj.tac.Assignment
import org.opalj.tac.BinaryExpr
import org.opalj.tac.Expr
import org.opalj.tac.ExprStmt
import org.opalj.tac.GetField
import org.opalj.tac.New
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.Stmt
import org.opalj.tac.StringConst
import org.opalj.tac.TACStmts
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.VirtualMethodCall

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * `InterpretationHandler` is responsible for processing expressions that are relevant in order to
 * determine which value(s) a string read operation might have. These expressions usually come from
 * the definitions sites of the variable of interest.
 *
 * @param cfg The control flow graph that underlies the program / method in which the expressions of
 *            interest reside.
 * @author Patrick Mell
 */
class InterpretationHandler(cfg: CFG[Stmt[V], TACStmts[V]]) {
    private val stmts = cfg.code.instructions
    private val processedDefSites = ListBuffer[Int]()

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
    def processDefSite(defSite: Int): List[StringConstancyInformation] = {
        // Function parameters are not evaluated but regarded as unknown
        if (defSite < 0) {
            return List(StringConstancyProperty.lowerBound.stringConstancyInformation)
        } else if (processedDefSites.contains(defSite)) {
            return List()
        }
        processedDefSites.append(defSite)

        stmts(defSite) match {
            case Assignment(_, _, expr: StringConst) ⇒
                new StringConstInterpreter(cfg, this).interpret(expr)
            case Assignment(_, _, expr: ArrayLoad[V]) ⇒
                new ArrayLoadInterpreter(cfg, this).interpret(expr)
            case Assignment(_, _, expr: New) ⇒
                new NewInterpreter(cfg, this).interpret(expr)
            case Assignment(_, _, expr: VirtualFunctionCall[V]) ⇒
                new VirtualFunctionCallInterpreter(cfg, this).interpret(expr)
            case Assignment(_, _, expr: StaticFunctionCall[V]) ⇒
                new StaticFunctionCallInterpreter(cfg, this).interpret(expr)
            case Assignment(_, _, expr: BinaryExpr[V]) ⇒
                new BinaryExprInterpreter(cfg, this).interpret(expr)
            case Assignment(_, _, expr: NonVirtualFunctionCall[V]) ⇒
                new NonVirtualFunctionCallInterpreter(cfg, this).interpret(expr)
            case Assignment(_, _, expr: GetField[V]) ⇒
                new FieldInterpreter(cfg, this).interpret(expr)
            case ExprStmt(_, expr: VirtualFunctionCall[V]) ⇒
                new VirtualFunctionCallInterpreter(cfg, this).interpret(expr)
            case vmc: VirtualMethodCall[V] ⇒
                new VirtualMethodCallInterpreter(cfg, this).interpret(vmc)
            case nvmc: NonVirtualMethodCall[V] ⇒
                new NonVirtualMethodCallInterpreter(cfg, this).interpret(nvmc)
            case _ ⇒ List()

        }
    }

    /**
     * This function serves as a wrapper function for [[InterpretationHandler.processDefSite]] in
     * the sense that it processes multiple definition sites. Thus, it may throw an exception as
     * well if an expression referenced by a definition site cannot be processed. The same rules as
     * for [[InterpretationHandler.processDefSite]] apply.
     *
     * @param defSites The definition sites to process.
     * @return Returns a list of lists of [[StringConstancyInformation]]. Note that this function
     *         preserves the order of the given `defSites`, i.e., the first element in the result
     *         list corresponds to the first element in `defSites` and so on. If a site could not be
     *         processed, the list for that site will be the empty list.
     */
    def processDefSites(defSites: Array[Int]): List[List[StringConstancyInformation]] =
        defSites.length match {
            case 0 ⇒ List()
            case 1 ⇒ List(processDefSite(defSites.head))
            case _ ⇒ defSites.filter(_ >= 0).map(processDefSite).toList
        }

    /**
     * The [[InterpretationHandler]] keeps an internal state for correct and faster processing. As
     * long as a single object within a CFG is analyzed, there is no need to reset the state.
     * However, when analyzing a second object (even the same object) it is necessary to call
     * `reset` to reset the internal state. Otherwise, incorrect results will be produced.
     * (Alternatively, you could instantiate another [[InterpretationHandler]] instance.)
     */
    def reset(): Unit = {
        processedDefSites.clear()
    }

}

object InterpretationHandler {

    /**
     * @see [[InterpretationHandler]]
     */
    def apply(cfg: CFG[Stmt[V], TACStmts[V]]): InterpretationHandler =
        new InterpretationHandler(cfg)

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
     * Determines the definition site of the initialization of the base object that belongs to a
     * ''toString'' call.
     *
     * @param toString The ''toString'' call of the object for which to get the initialization def
     *                 site for. Make sure that the object is a subclass of
     *                 [[AbstractStringBuilder]].
     * @param stmts A list of statements which will be used to lookup which one the initialization
     *              is.
     * @return Returns the definition sites of the base object of the call.
     */
    def findDefSiteOfInit(toString: VirtualFunctionCall[V], stmts: Array[Stmt[V]]): List[Int] = {
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
     * Determines the [[New]] expressions that belongs to a given `duvar`.
     *
     * @param duvar The [[org.opalj.tac.DUVar]] to get the [[New]]s for.
     * @param stmts The context to search in, e.g., the surrounding method.
     * @return Returns all found [[New]] expressions.
     */
    def findNewOfVar(duvar: V, stmts: Array[Stmt[V]]): List[New] = {
        val news = ListBuffer[New]()

        // HINT: It might be that the search has to be extended to further cases
        duvar.definedBy.foreach { ds ⇒
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
    def getStringConstancyInformationForInt: StringConstancyInformation =
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
    def getStringConstancyInformationForFloat: StringConstancyInformation =
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
