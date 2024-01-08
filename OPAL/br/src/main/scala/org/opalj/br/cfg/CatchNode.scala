/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package cfg

/**
 * This node represents an exception handler.
 *
 * @note   `CatchNode`s are made explicit to handle/identify situations where the same
 *         exception handlers is responsible for handling multiple different exceptions.
 *         This situation generally arises in case of Java`s multi-catch expressions.
 *
 * @param  index The index of the underlying exception handler in the exception table.
 * @param  startPC The start pc of the try-block.
 * @param  endPC The pc of the first instruction after the try-block (exclusive!).
 * @param  handlerPC The first pc of the handler block.
 * @param  catchType The type of the handled exception.
 *
 * @author Erich Wittenbeck
 * @author Michael Eichberg
 */
final class CatchNode(
        val index:     Int, // primarily used to compute a unique id
        val startPC:   Int,
        val endPC:     Int,
        val handlerPC: Int,
        val catchType: Option[ObjectType]) extends CFGNode {

    /**
     * @param handler The Handler.
     * @param index The unique index in the exception handler table; this enables us to assign
     *              unique nodeids to catch nodes.
     */
    def this(handler: ExceptionHandler, index: Int) =
        this(index, handler.startPC, handler.endPC, handler.handlerPC, handler.catchType)

    override final def nodeId: Int =
        // OLD: the offset is required to ensure that catch node ids do not collide with basic
        // OLD: block ids (even if the index is zero!)
        // OLD: 0xFFFFFF + startPC + (index << 16)
        /*-1==normal exit, -2==abnormal exit*/ -3 - index

    def copy(
        index:     Int = this.index,
        startPC:   Int = this.startPC,
        endPC:     Int = this.endPC,
        handlerPC: Int = this.handlerPC,
        catchType: Option[ObjectType] = this.catchType): CatchNode =
        new CatchNode(index, startPC, endPC, handlerPC, catchType)

    override final def isBasicBlock: Boolean             = false
    override final def isExitNode: Boolean               = false
    override final def isAbnormalReturnExitNode: Boolean = false
    override final def isNormalReturnExitNode: Boolean   = false
    override final def isStartOfSubroutine: Boolean      = false

    override final def isCatchNode: Boolean   = true
    override final def asCatchNode: this.type = this

    //
    // FOR DEBUGGING/VISUALIZATION PURPOSES
    //

    override def toHRR: Option[String] = Some(
        s"try[$startPC,$endPC) => $handlerPC{${catchType.map(_.toJava).getOrElse("Any")}}"
    )

    override def visualProperties: Map[String, String] = Map(
        "shape"     -> "box",
        "labelloc"  -> "l",
        "fillcolor" -> "orange",
        "style"     -> "filled",
        "shape"     -> "rectangle"
    )

    override def toString: String =
        s"CatchNode([$startPC,$endPC)=>$handlerPC,${catchType.map(_.toJava).getOrElse("<none>")})"

}
