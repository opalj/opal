/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ide.ifds.problem

import org.opalj.ide.problem.EdgeFunction

/**
 * Edge function evaluating all source values to the bottom value
 */
object AllBottomEdgeFunction extends org.opalj.ide.problem.AllBottomEdgeFunction[IFDSValue](Bottom) {
    override def composeWith(secondEdgeFunction: EdgeFunction[IFDSValue]): EdgeFunction[IFDSValue] = {
        this
    }

    override def toString: String = "AllBottomEdgeFunction()"
}
