/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import org.opalj.br.PC

/**
 * Common superclass of all TAC Naive unit tests.
 *
 * @author Michael Eichberg
 */
private[tac] class TACNaiveTest extends TACTest {

    def Assignment(
        pc:        PC,
        targetVar: IdBasedVar,
        expr:      Expr[IdBasedVar]
    ): Assignment[IdBasedVar] = {
        new Assignment[IdBasedVar](pc, targetVar, expr)
    }

}
