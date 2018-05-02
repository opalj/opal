/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj.br.cfg

import org.opalj.br.ObjectType
import org.opalj.br.ExceptionHandler

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
class CatchNode(
        val index:     Int, // primarily used to compute a unique id
        val startPC:   Int,
        val endPC:     Int,
        val handlerPC: Int,
        val catchType: Option[ObjectType]
) extends CFGNode {

    def this(handler: ExceptionHandler, index: Int) {
        this(index, handler.startPC, handler.endPC, handler.handlerPC, handler.catchType)
    }

    final override def nodeId: Int = {
        // the offset is required to ensure that catch node ids do not collide with basic
        // block ids (even if the index is zero!)
        0xFFFFFF + startPC + (index << 16)
    }

    def copy(
        index:     Int                = this.index,
        startPC:   Int                = this.startPC,
        endPC:     Int                = this.endPC,
        handlerPC: Int                = this.handlerPC,
        catchType: Option[ObjectType] = this.catchType
    ): CatchNode = {
        new CatchNode(index, startPC, endPC, handlerPC, catchType)
    }

    final override def isBasicBlock: Boolean = false
    final override def isExitNode: Boolean = false
    final override def isAbnormalReturnExitNode: Boolean = false
    final override def isNormalReturnExitNode: Boolean = false
    final override def isStartOfSubroutine: Boolean = false

    final override def isCatchNode: Boolean = true
    final override def asCatchNode: this.type = this

    //
    // FOR DEBUGGING/VISUALIZATION PURPOSES
    //

    override def toHRR: Option[String] = Some(
        s"try[$startPC,$endPC) ⇒ $handlerPC{${catchType.map(_.toJava).getOrElse("Any")}}"
    )

    override def visualProperties: Map[String, String] = Map(
        "shape" → "box",
        "labelloc" → "l",
        "fillcolor" → "orange",
        "style" → "filled",
        "shape" → "rectangle"
    )

    override def toString: String = {
        s"CatchNode([$startPC,$endPC)⇒$handlerPC,${catchType.map(_.toJava).getOrElse("<none>")})"
    }

}
