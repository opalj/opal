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
package org.opalj
package ba

import scala.collection.mutable

private[ba] class ExceptionHandlerBuilder(
        var startPC:   br.PC                 = -1,
        var endPC:     br.PC                 = -1,
        var handlerPC: br.PC                 = -1,
        var catchType: Option[br.ObjectType] = None
)

/**
 * Incrementally builds the [[org.opalj.br.ExceptionHandlers]] from the added pseudo instructions
 * ([[ExceptionHandlerElement]]) representing an [[org.opalj.br.ExceptionHandler]].
 *
 * @author Malte Limmeroth
 */
class ExceptionHandlerGenerator {

    private[this] val map: mutable.Map[Symbol, ExceptionHandlerBuilder] = mutable.Map.empty

    private[this] def getExceptionHandlerBuilder(id: Symbol): ExceptionHandlerBuilder = {
        map.getOrElseUpdate(id, new ExceptionHandlerBuilder())
    }

    def add(element: ExceptionHandlerElement, pc: br.PC): Unit = {
        val handler = getExceptionHandlerBuilder(element.id)
        element match {
            case TRY(_)    ⇒ handler.startPC = pc
            case TRYEND(_) ⇒ handler.endPC = pc
            case CATCH(_, catchType) ⇒
                handler.handlerPC = pc
                handler.catchType = catchType
        }
    }

    private def sortByLastNumber(left: Symbol, right: Symbol) = {
        val pattern = "^(.*?)([0-9]*)$".r
        val pattern(lName, lId) = left.name
        val pattern(rName, rId) = right.name
        if (lId.isEmpty && rId.isEmpty) {
            lName < rName
        } else if (lId.isEmpty) {
            false
        } else if (rId.isEmpty) {
            true
        } else {
            Integer.parseInt(lId) < Integer.parseInt(rId)
        }
    }

    /**
     * Generates the final [[org.opalj.br.ExceptionHandlers]] from the added pseudo instructions.
     * Fails if any handler is incomplete. That is, only one or two of the three pseudo instructions
     * for a single id was added.
     */
    def result(): br.ExceptionHandlers = {
        map.toIndexedSeq.sortWith((left, right) ⇒ sortByLastNumber(left._1, right._1)).map { e ⇒
            val (id, ehBuilder) = e
            val errorMsg = s"invalid exception handler ($id): %s"
            require(ehBuilder.startPC >= 0, errorMsg.format(s"startPC = ${ehBuilder.startPC}"))
            require(ehBuilder.endPC >= 0, errorMsg.format(s"endPC = ${ehBuilder.endPC}"))
            require(ehBuilder.handlerPC >= 0, errorMsg.format(s"handlerPC = ${ehBuilder.handlerPC}"))
            require(ehBuilder.startPC < ehBuilder.endPC, errorMsg.format("empty sequence"))
            br.ExceptionHandler(
                ehBuilder.startPC, ehBuilder.endPC, ehBuilder.handlerPC,
                ehBuilder.catchType
            )
        }
    }
}
