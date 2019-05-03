/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj

package tac

/**
 * Just a very simple class to demonstrate the very busy expressions analysis.
 *
 * @author Michael Eichberg
 */
class VeryBusyExpressionsDemo {

    // Inspired by Example 2.8 in Principles of Program Analysis; Nielsen, Nielsen, and Hankin; 2005
    // The expressions "a-b" and "b-a" are examples of very busy expressions that could be hoisted.
    def n_n_h_ex_2_8(u: Int, v: Int): Int = {
        var x = 0
        var y = 0
        val z = u + v
        // When we reach this point, the expressions a-b and b-a should no longer be "very busy"
        // because the variables are no longer defined!
        val a = u + 1
        val b = v + 1
        if (a > b) {
            x = b - a
            y = a - b
        } else {
            x = a - b
            y = b - a
        }
        z + x + y // necessary to quiet the compiler
    }

}
