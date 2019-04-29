/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj

package tac

/** Just a very simple class to demonstrate the very busy expressions analysis. */
class VeryBusyExpressionsDemo {
 
  // Inspired by Example 2.8 in Principles of Program Analysis; Nielsen, Nielsen, and Hankin; 2005
  // The expressions "a-b" and "b-a" are very busy and could be hoisted.
  def n_n_h_ex_2_8(a: Int, b: Int): Int = {
    var x = 0
    var y = 0
    if (a > b) {
      x = b - a
      y = a - b
    } else {
      x = a - b
      y = b - a
    }

    x + y // necessary to quiet the compiler
  }

}
