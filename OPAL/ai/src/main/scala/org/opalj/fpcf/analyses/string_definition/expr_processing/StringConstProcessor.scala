/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.expr_processing
import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.fpcf.string_definition.properties.StringConstancyInformation
import org.opalj.fpcf.string_definition.properties.StringConstancyLevel.CONSTANT
import org.opalj.fpcf.string_definition.properties.StringTree
import org.opalj.fpcf.string_definition.properties.TreeValueElement
import org.opalj.tac.Assignment
import org.opalj.tac.Stmt
import org.opalj.tac.StringConst

/**
 * This implementation of [[AbstractExprProcessor]] processes [[org.opalj.tac.StringConst]]
 * expressions.
 *
 * @author Patrick Mell
 */
class StringConstProcessor() extends AbstractExprProcessor {

    /**
     * For this implementation, `stmts` is not required (thus, it is safe to pass an empty value).
     * The `expr` of `assignment` is required to be of type [[org.opalj.tac.StringConst]] (otherwise
     * `None` will be returned).
     *
     * @note The sub-tree, which is created by this implementation, does not have any children.
     *
     * @see [[AbstractExprProcessor.process]]
     */
    override def process(
        assignment: Assignment[V], stmts: Array[Stmt[V]], ignore: List[Int] = List[Int]()
    ): Option[StringTree] =
        assignment.expr match {
            case strConst: StringConst ⇒ Some(TreeValueElement(
                None,
                StringConstancyInformation(CONSTANT, strConst.value)
            ))
            case _ ⇒ None
        }

}
