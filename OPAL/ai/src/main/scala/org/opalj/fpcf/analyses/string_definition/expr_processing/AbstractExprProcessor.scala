/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.expr_processing

import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.fpcf.properties.StringConstancyProperty
import org.opalj.tac.Expr
import org.opalj.tac.Stmt

/**
 * AbstractExprProcessor defines the abstract / general strategy to process expressions in the
 * context of string definition analyses. Different sub-classes process different kinds of
 * expressions. The idea is to transform expressions into [[StringConstancyProperty]] objects. For
 * example, the expression of a constant assignment might be processed.
 *
 * @author Patrick Mell
 */
abstract class AbstractExprProcessor() {

    /**
     * Implementations process an expression which is supposed to yield (not necessarily fixed) a
     * string value.
     *
     * @param stmts The statements that surround the expression to process, such as a method.
     *              Concrete processors might use these to retrieve further information.
     * @param expr  The expression to process. Make sure that the expression, which is passed, meets
     *              the requirements of that implementation.
     * @return Determines the [[org.opalj.fpcf.properties.StringConstancyLevel]] as well as possible
     *         string values that the expression might produce. If `expr` does not meet the
     *         requirements of a an implementation, `None` will be returned.
     *         For further details, see [[StringConstancyProperty]].
     * @see StringConstancyProperty
     */
    def process(stmts: Array[Stmt[V]], expr: Expr[V]): Option[StringConstancyProperty]

}
