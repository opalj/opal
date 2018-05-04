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

import scala.collection.Set

import org.opalj.collection.immutable.UIDSet
import org.opalj.collection.immutable.UIDSet1
import org.opalj.br.ObjectType

/**
 * Records the results of the evaluation of the `current` method such that the results
 * can directly be adapted to the calling context and can be used by the caller to continue
 * the abstract interpretation of the calling method.
 *
 * '''The returned value will only be a given parameter, if the given parameter is not mutated.'''
 * For example, if an array is passed to a method where the content is reified, the
 * returned value will only be mapped back to the parameter value if the array is not changed at all.
 * In other words, the returned value, which may get the pc of the method caller, may refer to
 * any parameter given to the method. Only, if the returned value is a parameter, we know that
 * this specific parameter was not mutated at all.
 *
 * @author Michael Eichberg
 */
trait RecordMethodCallResults
    extends MethodCallResults
    with RecordReturnedValues
    with RecordThrownExceptions {
    this: Domain with TheClassHierarchy ⇒

    type ThrownException <: Set[this.ExceptionValue]

    type ReturnedValue <: DomainValue

    private[this] var hasReturnedNormally: Boolean = false

    abstract override def returnVoid(pc: Int): Computation[Nothing, ExceptionValue] = {
        hasReturnedNormally = true
        super.returnVoid(pc)
    }

    def returnedNormally: Boolean = hasReturnedNormally || allReturnedValues.nonEmpty

    def returnedValue(target: TargetDomain, callerPC: Int): Option[target.DomainValue] = {
        if (allReturnedValues.isEmpty)
            None
        else {
            Some(summarize(callerPC, allReturnedValues.values).adapt(target, callerPC))
        }
    }

    def returnedValueRemapped(
        callerDomain: TargetDomain,
        callerPC:     Int
    )(
        originalOperands: callerDomain.Operands,
        passedParameters: Locals
    ): Option[callerDomain.DomainValue] = {

        if (allReturnedValues.isEmpty)
            None
        else {
            // IMPROVE If some of the returned values are, e.g., MultipleReferenceValues
            // or if we have multiple return sites
            // where some refer to parameters and some to local variables, then we should map back
            // the information regarding the parameters and summarize only w.r.t. the
            // local variables.
            val summarizedValue = summarize(callerPC, allReturnedValues.values)

            val nthParameter = passedParameters.nthValue { _ eq summarizedValue }
            if (nthParameter == -1)
                Some(summarizedValue.adapt(callerDomain, callerPC))
            else {
                // map back to operand...
                val mappedBackValue = originalOperands.reverse(nthParameter)
                Some(mappedBackValue)
            }
        }
    }

    // IMPROVE Remap returned exceptions
    def thrownExceptions(target: TargetDomain, callerPC: Int): target.ExceptionValues = {

        val allThrownExceptions = this.allThrownExceptions //: Map[PC, ThrownException]
        if (allThrownExceptions.isEmpty) {
            Iterable.empty
        } else {
            var exceptionValuesPerType: Map[ObjectType, Set[ExceptionValue]] = Map.empty

            def handleExceptionValue(exceptionValue: ExceptionValue): Unit = {
                exceptionValue.upperTypeBound match {
                    case EmptyUpperTypeBound ⇒
                        println("[info] [RecordMethodCallResults.thrownExceptions] Type of exception is unknown.")
                        exceptionValuesPerType = exceptionValuesPerType.updated(
                            ObjectType.Throwable,
                            exceptionValuesPerType.getOrElse(
                                ObjectType.Throwable, Set.empty
                            ) + exceptionValue
                        )
                    case UIDSet1(exceptionType: ObjectType) ⇒
                        exceptionValuesPerType = exceptionValuesPerType.updated(
                            exceptionType,
                            exceptionValuesPerType.getOrElse(
                                exceptionType, Set.empty
                            ) + exceptionValue
                        )
                    case utb ⇒
                        val exceptionType =
                            classHierarchy.joinObjectTypesUntilSingleUpperBound(
                                utb.asInstanceOf[UIDSet[ObjectType]]
                            )
                        exceptionValuesPerType = exceptionValuesPerType.updated(
                            exceptionType,
                            exceptionValuesPerType.getOrElse(
                                exceptionType, Set.empty
                            ) + exceptionValue
                        )
                }
            }

            for {
                exceptionValuesPerInstruction ← allThrownExceptions.values
                exceptionValues ← exceptionValuesPerInstruction
                exceptionValue ← exceptionValues.allValues
            } {
                handleExceptionValue(exceptionValue)
            }

            exceptionValuesPerType.values.map { exceptionValuesPerType ⇒
                summarize(callerPC, exceptionValuesPerType)
            }.map { exceptionValuePerType ⇒
                exceptionValuePerType.adapt(target, callerPC).asInstanceOf[target.ExceptionValue]
            }
        }
    }
}
