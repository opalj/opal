/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.ifds

abstract class Statement[C, Node] {
    def node: Node
    def callable: C
}
