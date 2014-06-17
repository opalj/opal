/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package l0

import language.implicitConversions

import org.opalj.collection.mutable.UShortSet
import br.ObjectType
import org.opalj.collection.immutable.{ UIDSet0, UIDSet1, UIDSet }

/**
 * Records the results of the evaluation of the `current` method such that the results
 * can directly be adapted to the calling context and used by the caller to continue
 * the abstract interpretation of the calling method.
 *
 * @author Michael Eichberg
 */
trait RecordMethodCallResults
        extends MethodCallResults
        with RecordLastReturnedValues
        with RecordAllThrownExceptions {
    this: TypeLevelReferenceValues with ClassHierarchy ⇒

    private[this] var hasReturnedNormally: Boolean = false

    abstract override def returnVoid(pc: PC): Unit = {
        hasReturnedNormally = true
        super.returnVoid(pc)
    }

    def returnedNormally: Boolean = hasReturnedNormally || allReturnedValues.nonEmpty

    def returnedValue(target: Domain, callerPC: PC): Option[target.DomainValue] = {
        if (allReturnedValues.isEmpty)
            None
        else {
            Some(summarize(callerPC, allReturnedValues.values).adapt(target, callerPC))
        }
    }

    def thrownExceptions(target: Domain, callerPC: PC): target.ExceptionValues = {
        val allThrownExceptions = this.allThrownExceptions //: Map[PC, ThrownException] 
        if (allThrownExceptions.isEmpty) {
            Iterable.empty
        } else {
            var exceptionValuesPerType: Map[ObjectType, Set[ExceptionValue]] = Map.empty

            def handleIsAReferenceValue(
                exceptionValue: ExceptionValue,
                exceptionValueProperties: IsAReferenceValue) {
                exceptionValueProperties.upperTypeBound match {
                    case UIDSet0 ⇒
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
                    case utb ⇒ {
                        val exceptionType =
                            classHierarchy.joinObjectTypesUntilSingleUpperBound(
                                utb.asInstanceOf[UIDSet[ObjectType]],
                                true)
                        exceptionValuesPerType = exceptionValuesPerType.updated(
                            exceptionType,
                            exceptionValuesPerType.getOrElse(
                                exceptionType, Set.empty
                            ) + exceptionValue
                        )
                    }
                }
            }

            for {
                exceptionValuesPerInstruction ← allThrownExceptions.values
                exceptionValue ← exceptionValuesPerInstruction
            } {
                typeOfValue(exceptionValue) match {
                    case IsReferenceValue(exceptionValues) ⇒
                        exceptionValues.foreach { anExceptionValue ⇒
                            handleIsAReferenceValue(
                                // TODO [Safety] We should make it possible that a value converts itself to a domain value
                                anExceptionValue.asInstanceOf[DomainValue],
                                anExceptionValue)
                        }

                    case exceptionValueProperties: IsAReferenceValue ⇒
                        handleIsAReferenceValue(exceptionValue, exceptionValueProperties)

                    // case _ => ... should never occur
                }
            }

            exceptionValuesPerType.values.map { exceptionValuesPerType ⇒
                summarize(callerPC, exceptionValuesPerType)
            }.map { exceptionValuePerType ⇒
                exceptionValuePerType.adapt(target, callerPC)
            }
        }
    }
}



