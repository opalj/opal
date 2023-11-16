/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll

package object llvm {
    def intToBool(i: Int): Boolean = i match {
        case 0 => false
        case 1 => true
        case _ => throw new IllegalArgumentException(s"${i} is not a valid Boolean")
    }
}
