/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.cfg

/**
 * Represents the artificial exit node of a control flow graph. The graph contains
 * an explicit exit node to make it trivial to navigate to all instructions that may
 * cause a(n ab)normal return from the method.
 *
 * @author Erich Wittenbeck
 * @author Michael Eichberg
 */
final class ExitNode( final val normalReturn: Boolean) extends CFGNode {

    override def nodeId: Int = {
        // OLD: if (normalReturn) Int.MinValue else Int.MinValue + 1
        /*-1==normal exit, -2==abnormal exit*/
        if (normalReturn) -1 else -2
    }

    override def isBasicBlock: Boolean = false
    override def isCatchNode: Boolean = false
    override def isExitNode: Boolean = true
    override def isAbnormalReturnExitNode: Boolean = !normalReturn
    override def isNormalReturnExitNode: Boolean = normalReturn

    override def isStartOfSubroutine: Boolean = false

    override def addSuccessor(successor: CFGNode): Unit = {
        throw new UnsupportedOperationException()
    }

    override private[cfg] def setSuccessors(successors: Set[CFGNode]): Unit = {
        throw new UnsupportedOperationException()
    }

    //
    // FOR DEBUGGING/VISUALIZATION PURPOSES
    //

    override def toString: String = s"ExitNode(normalReturn=$normalReturn)"

    override def toHRR: Option[String] = {
        Some(if (normalReturn) "Normal Return" else "Abnormal Return")
    }

    override def visualProperties: Map[String, String] = {
        if (normalReturn)
            Map("labelloc" -> "l", "fillcolor" -> "green", "style" -> "filled")
        else
            Map("labelloc" -> "l", "fillcolor" -> "red", "style" -> "filled", "shape" -> "octagon")
    }

}

object ExitNode {

    def unapply(en: ExitNode): Some[Boolean] = Some(en.normalReturn)
}
