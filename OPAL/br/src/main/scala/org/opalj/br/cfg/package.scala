/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br

package object cfg {
    private def enumerate(node: CFGNode): Int = node match {
        case _: BasicBlock => 1
        case _: CatchNode  => 2
        case _: ExitNode   => 3
    }
    implicit val cfgNodeOrdering: Ordering[CFGNode] = (x: CFGNode, y: CFGNode) => {
        (x, y) match {
            case (b1: BasicBlock, b2: BasicBlock) => b1.startPC compare b2.startPC
            case (b1: CatchNode, b2: CatchNode)   => (b1.handlerPC, b1.catchType) compare (b2.handlerPC, b2.catchType)
            case (b1: ExitNode, b2: ExitNode)     => b1.normalReturn compare b2.normalReturn
            case (_, _)                           => enumerate(x) compare enumerate(y)
        }
    }
}
