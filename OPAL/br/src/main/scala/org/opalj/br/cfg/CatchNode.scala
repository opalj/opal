/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
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

import org.opalj.br.ExceptionHandler
import org.opalj.br.PC
import org.opalj.br.ObjectType

/**
 * This node represents an exception handler.
 *
 * @author Erich Wittenbeck
 * @author Michael Eichberg
 */
class CatchNode(
        val startPC:   PC,
        val endPC:     PC,
        val handlerPC: PC,
        val catchType: Option[ObjectType]
) extends CFGNode {

    def this(handler: ExceptionHandler) {
        this(handler.startPC, handler.endPC, handler.handlerPC, handler.catchType)
    }

    final def isBasicBlock: Boolean = false
    final def isCatchNode: Boolean = true
    final override def asCatchNode: this.type = this
    final def isExitNode: Boolean = false

    final def isStartOfSubroutine: Boolean = false

    //
    // FOR DEBUGGING/VISUALIZATION PURPOSES
    //

    override def nodeId: Long =
        startPC.toLong |
            (endPC.toLong << 16) |
            (handlerPC.toLong << 32) |
            // ObjectTypes have positive ids; Any can hence be associated with -1
            (catchType.map(_.hashCode()).getOrElse(-1).toLong << 48)

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

    override def toString: String =
        s"CatchNode([$startPC,$endPC)⇒$handlerPC,"+
            s"${catchType.map(_.toJava).getOrElse("<none>")})"

}
