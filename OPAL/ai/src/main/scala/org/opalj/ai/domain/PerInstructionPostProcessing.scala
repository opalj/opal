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
package ai
package domain

import org.opalj.collection.immutable.{Chain ⇒ List}
import org.opalj.collection.immutable.{Naught ⇒ Nil}
import org.opalj.collection.mutable.IntArrayStack

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
trait PerInstructionPostProcessing extends CoreDomainFunctionality {

    type DomainValueUpdater = (DomainValue) ⇒ DomainValue

    private[this] var onExceptionalControlFlow: List[DomainValueUpdater] = Nil

    private[this] var onRegularControlFlow: List[DomainValueUpdater] = Nil

    abstract override def flow(
        currentPC:                        Int,
        currentOperands:                  Operands,
        currentLocals:                    Locals,
        successorPC:                      Int,
        isSuccessorScheduled:             Answer,
        isExceptionalControlFlow:         Boolean,
        abruptSubroutineTerminationCount: Int,
        wasJoinPerformed:                 Boolean,
        worklist:                         List[Int /*PC*/ ],
        operandsArray:                    OperandsArray,
        localsArray:                      LocalsArray,
        tracer:                           Option[AITracer]
    ): List[Int /*PC*/ ] = {

        def doUpdate(updaters: List[DomainValueUpdater]): Unit = {
            val oldOperands = operandsArray(successorPC)
            val newOperands =
                oldOperands mapConserve { op ⇒
                    updaters.tail.foldLeft(updaters.head(op)) { (updatedOp, updater) ⇒
                        updater(updatedOp)
                    }
                }
            if (newOperands ne oldOperands)
                operandsArray(successorPC) = newOperands

            val locals: Locals = localsArray(successorPC)
            localsArray(successorPC) = locals.mapConserve { l ⇒
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
            currentPC, currentOperands, currentLocals,
            successorPC,
            isSuccessorScheduled,
            isExceptionalControlFlow, abruptSubroutineTerminationCount,
            wasJoinPerformed,
            worklist,
            operandsArray, localsArray,
            tracer
        )
    }

    def registerOnRegularControlFlowUpdater(f: DomainValue ⇒ DomainValue): Unit = {
        onRegularControlFlow :&:= f
    }

    def registerOnExceptionalControlFlowUpdater(f: DomainValue ⇒ DomainValue): Unit = {
        onExceptionalControlFlow :&:= f
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
        pc:            Int,
        worklist:      List[Int /*PC*/ ],
        evaluatedPCs:  IntArrayStack,
        operandsArray: OperandsArray,
        localsArray:   LocalsArray,
        tracer:        Option[AITracer]
    ): Unit = {
        val l = Nil
        onExceptionalControlFlow = l
        onRegularControlFlow = l

        super.evaluationCompleted(
            pc, worklist, evaluatedPCs,
            operandsArray, localsArray,
            tracer
        )
    }
}
