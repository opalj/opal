/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ifds

abstract class Statement[C, Node] {
    def basicBlock: Node
    def callable: C
}
