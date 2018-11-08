/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.expr_processing

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.fpcf.properties.StringConstancyProperty
import org.opalj.tac.ArrayLoad
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.SimpleTACAIKey
import org.opalj.tac.StringConst
import org.opalj.tac.VirtualFunctionCall

/**
 * `ExprHandler` is responsible for processing expressions that are relevant in order to determine
 * which value(s) a string read operation might have. These expressions usually come from the
 * definitions sites of the variable of interest.
 *
 * @param p The project associated with the analysis.
 * @param m The [[Method]] in which the read statement of the string variable of interest occurred.
 * @author Patrick Mell
 */
class ExprHandler(p: SomeProject, m: Method) {

    private val tacProvider = p.get(SimpleTACAIKey)
    private val methodStmts = tacProvider(m).stmts

    /**
     * Processes a given definition site. That is, this function determines the
     * [[StringConstancyProperty]] of a string definition.
     *
     * @param defSite The definition site to process. Make sure that (1) the value is >= 0, (2) it
     *                actually exists, and (3) contains an Assignment whose expression is of a type
     *                that is supported by a sub-class of [[AbstractExprProcessor]].
     * @return Returns an instance of [[StringConstancyProperty]] that describes the definition
     *         at the specified site. In case the rules listed above or the ones of the different
     *         processors are not met `None` will be returned.
     */
    def processDefSite(defSite: Int): Option[StringConstancyProperty] = {
        if (defSite < 0) {
            return None
        }

        val expr = methodStmts(defSite).asAssignment.expr
        val exprProcessor: AbstractExprProcessor = expr match {
            case _: ArrayLoad[V]              ⇒ new ArrayLoadProcessor(this)
            case _: VirtualFunctionCall[V]    ⇒ new VirtualFunctionCallProcessor()
            case _: NonVirtualFunctionCall[V] ⇒ new NonVirtualFunctionCallProcessor()
            case _: StringConst               ⇒ new StringConstProcessor()
            case _ ⇒ throw new IllegalArgumentException(
                s"cannot process expression $expr"
            )
        }
        exprProcessor.process(methodStmts, expr)
    }

    /**
     * This function serves as a wrapper function for [[ExprHandler.processDefSite]] in the
     * sense that it processes multiple definition sites. Thus, it may throw an exception as well if
     * an expression referenced by a definition site cannot be processed. The same rules as for
     * [[ExprHandler.processDefSite]] apply.
     *
     * @param defSites The definition sites to process.
     * @return Returns an array of [[StringConstancyProperty]] elements. In contrast to
     *         [[ExprHandler.processDefSite]] this function returns only those values that are not
     *         equals `None`. Furthermore, note that this function returns the values unmodified,
     *         e.g., no call to [[StringConstancyProperty#reduce]] whatsoever is executed that could
     *         change the array.
     */
    def processDefinitionSites(defSites: IntTrieSet): Array[StringConstancyProperty] =
        defSites.filter(_ >= 0).map(processDefSite _).filter(_.isDefined).map(_.get).toArray

}

object ExprHandler {

    /**
     * @see [[ExprHandler]]
     */
    def apply(p: SomeProject, m: Method): ExprHandler = new ExprHandler(p, m)

}
