/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

/** Just a very simple class to demonstrate the available expressions analysis. */
class AvailableExpressionsDemo {

    def simple(a: Int, b: Int): Unit = {
        var c = a + b
        var t = c
        while (t < 1000) {
            c = a + b
            t = t + c
        }
    }

    // Inspired by Example 2.5 in Principles of Program Analysis; Nielsen, Nielsen, and Hankin; 2005
    def n_n_h_ex_2_5(initialA: Int, b: Int): Int = {
        var a = initialA // a has to be variable
        var x = a + b;
        val y = a * b;
        while (y > a + b) {
            a = a + 1
            x = a + b
        }
        a + x // necessary to "shut up" the compiler w.r.t. unused local vars
    }

}