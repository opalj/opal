/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package ai
package domain

/**
 * Provides the generic infrastructure to register a function that updates the operands
 * and locals associated with an instruction that will be evaluated "next".
 * For example, let's assume that we are currently processing instruction X and that
 * instruction Y is the successor instruction. In this case, the framework will first
 * determine the effect of function X on the stack/locals. After that, all registered
 * updaters will be called. All registered updaters will be discarded as soon the
 * evaluation of instruction X has completed.
 *
 * @author Michael Eichberg
 */
trait PerInstructionPostProcessing extends CoreDomain {

    type DomainValueUpdater = (DomainValue) ⇒ DomainValue

    private[this] var onExceptionalControlFlow: List[DomainValueUpdater] = Nil

    private[this] var onRegularControlFlow: List[DomainValueUpdater] = Nil

    abstract override def flow(
        currentPC: PC,
        successorPC: PC,
        isExceptionalControlFlow: Boolean,
        worklist: List[PC],
        operandsArray: OperandsArray,
        localsArray: LocalsArray,
        tracer: Option[AITracer]): List[PC] = {

        def doUpdate(updaters: List[DomainValueUpdater]): Unit = {
            operandsArray(successorPC) =
                operandsArray(successorPC) map { op ⇒
                    val updatedValue = updaters.head(op)
                    updaters.tail.foldLeft(updatedValue) { (updatedValue, updater) ⇒
                        updater(updatedValue)
                    }
                    updatedValue
                }

            val locals: Locals = localsArray(successorPC)
            locals.update { l ⇒
                if (l ne null)
                    updaters.tail.foldLeft(updaters.head.apply(l))((c, u) ⇒ u.apply(c))
                else
                    null
            }

        }

        if (isExceptionalControlFlow) {
            val updaters = onExceptionalControlFlow
            if (updaters.nonEmpty) {
                doUpdate(updaters)
            }
        } else {
            val updaters = onRegularControlFlow
            if (updaters.nonEmpty) {
                doUpdate(updaters)
            }
        }

        super.flow(
            currentPC, successorPC, isExceptionalControlFlow, worklist,
            operandsArray, localsArray, tracer)
    }

    def registerOnRegularControlFlowUpdater(f: DomainValue ⇒ DomainValue): Unit = {
        onRegularControlFlow = f :: onRegularControlFlow
    }

    def registerOnExceptionalControlFlowUpdater(f: DomainValue ⇒ DomainValue): Unit = {
        onExceptionalControlFlow = f :: onExceptionalControlFlow
    }

    /**
     * @see [[registerOnRegularControlFlowUpdater]]
     * @see [[registerOnExceptionalControlFlowUpdater]]
     */
    def registerOnControlFlowUpdater(f: DomainValue ⇒ DomainValue): Unit = {
        registerOnRegularControlFlowUpdater(f)
        registerOnExceptionalControlFlowUpdater(f)
    }

    override def evaluationCompleted(
        pc: PC,
        worklist: List[PC],
        evaluated: List[PC],
        operandsArray: OperandsArray,
        localsArray: LocalsArray,
        tracer: Option[AITracer]): Unit = {
        val l = Nil
        onExceptionalControlFlow = l
        onRegularControlFlow = l

        super.evaluationCompleted(
            pc, worklist, evaluated,
            operandsArray, localsArray,
            tracer)
    }
}
