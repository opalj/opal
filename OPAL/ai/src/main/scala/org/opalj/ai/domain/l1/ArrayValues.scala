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
package l1

import scala.collection.SortedSet

import org.opalj.util.{ Answer, Yes, No, Unknown }

import br._

/**
 * @author Michael Eichberg
 */
trait ArrayValues
        extends l1.ReferenceValues
        with Origin
        with PerInstructionPostProcessing {
    domain: Configuration with ConcreteIntegerValues with ClassHierarchy ⇒

    // We do not refine the type DomainArrayValue any further since we also want
    // to use the super level ArrayValue class.
    // DO NOT: type DomainArrayValue <: ArrayValue with DomainSingleOriginReferenceValue

    protected class ArrayValue(
        vo: ValueOrigin,
        theType: ArrayType,
        val values: Array[DomainValue])
            extends super.ArrayValue(vo, No, true, theType) {
        this: DomainArrayValue ⇒

        override def length: Some[Int] = Some(values.size)

        override def doLoad(
            loadPC: PC,
            index: DomainValue,
            potentialExceptions: ExceptionValues): ArrayLoadResult = {
            if (potentialExceptions.nonEmpty) {
                // - a "NullPointerException" is not possible
                // - if an ArrayIndexOutOfBoundsException may be thrown then we certainly
                //   do not have enough information about the index...
                return ComputedValueOrException(
                    TypedValue(loadPC, theUpperTypeBound.componentType),
                    potentialExceptions)
            }

            intValue[ArrayLoadResult](index) { index ⇒
                ComputedValue(values(index))
            } {
                // This handles the case that we know that the index is not precise 
                // but still known to be valid.
                super.doLoad(loadPC, index, potentialExceptions)
            }
        }

        override def doStore(
            storePC: PC,
            value: DomainValue,
            index: DomainValue,
            potentialExceptions: ExceptionValues): ArrayStoreResult = {
            // Here, a "NullPointerException" is not possible
            if (potentialExceptions.nonEmpty) {
                // In both of the following cases, we are no longer able to trace
                // the contents of the array.

                // - if an ArrayIndexOutOfBoundsException may be thrown then we certainly
                //   do not have enough information about the index...
                // - if an ArrayStoreException may be thrown, we are totally lost..
                // TODO [BUG] Mark array as dead
                return ThrowsException(potentialExceptions)
            }

            // If we reach this point none of the given exceptions is guaranteed to be thrown
            // However, we now have to provide the solution for the happy path
            intValue[ArrayStoreResult](index) { index ⇒
                // let's check if we need to do anything
                if (values(index) == value) {
                    // TODO [BUG] Mark array as dead                    
                    var newArrayValue: DomainValue = null // <= we create the new array value only on demand and at most once!
                    registerOnRegularControlFlowUpdater { someDomainValue ⇒
                        if (someDomainValue eq ArrayValue.this) {
                            if (newArrayValue == null) {
                                newArrayValue = ArrayValue(vo, theType, values.updated(index, value))
                            }
                            newArrayValue
                        } else {
                            someDomainValue
                        }
                    }
                }
                ComputationWithSideEffectOnly
            } {
                // This handles the case that the index is not precise, but still
                // known to be valid. In this case we have to resort to the 
                // abstract representation of the array.

                // TODO [BUG] Mark array as dead 
                ComputationWithSideEffectOrException(potentialExceptions)
            }
        }

        override def doJoinWithNonNullValueWithSameOrigin(
            joinPC: PC,
            other: DomainSingleOriginReferenceValue): Update[DomainSingleOriginReferenceValue] = {

            other match {
                case that: ArrayValue if this.values.size == that.values.size ⇒
                    var update: UpdateType = NoUpdateType
                    var isOther: Boolean = true
                    val allValues = this.values.view.zip(that.values)
                    val newValues =
                        (allValues map { (v) ⇒
                            val (v1, v2) = v
                            val joinResult = v1.join(joinPC, v2)
                            joinResult match {
                                case NoUpdate ⇒
                                    v1
                                case SomeUpdate(newValue) ⇒
                                    if (v2 ne newValue) {
                                        isOther = false
                                    }
                                    update = joinResult &: update
                                    newValue
                            }
                        }).toArray // <= forces the evaluation - WHICH IS REQUIRED
                    update match {
                        case NoUpdateType ⇒ NoUpdate
                        case _ ⇒
                            if (isOther) {
                                update(other)
                            } else
                                update(ArrayValue(vo, theType, newValues))
                    }

                case _ ⇒
                    val answer = super.doJoinWithNonNullValueWithSameOrigin(joinPC, other)
                    if (answer == NoUpdate) {
                        // => This array and the other array have a corresponding
                        //    abstract representation (w.r.t. the next abstraction level!)
                        //    but we still need to drop the concrete information
                        StructuralUpdate(ArrayValue(vo, No, true, theUpperTypeBound))
                    } else {
                        answer
                    }
            }
        }

        override def adapt(target: Domain, vo: ValueOrigin): target.DomainValue =
            target match {

                case thatDomain: l1.ArrayValues ⇒
                    val adaptedValues =
                        values.map(_.adapt(target, vo).asInstanceOf[thatDomain.DomainValue])
                    thatDomain.ArrayValue(
                        vo, theUpperTypeBound, adaptedValues).
                        asInstanceOf[target.DomainValue]

                case _ ⇒ super.adapt(target, vo)
            }

        override def equals(other: Any): Boolean = {
            other match {
                case that: ArrayValue ⇒ (
                    (that eq this) ||
                    (
                        (that canEqual this) &&
                        this.origin == that.origin &&
                        (this.upperTypeBound eq that.upperTypeBound) &&
                        this.values == that.values
                    )
                )
                case _ ⇒ false
            }
        }

        protected def canEqual(other: ArrayValue): Boolean = true

        override def hashCode: Int =
            ((vo) * 41 + values.hashCode) * 79 + upperTypeBound.hashCode

        override def toString() = {
            var description = theUpperTypeBound.toJava+"(origin="+vo+", values#"+values.size+"="
            description += values.mkString("(", ",", ")")
            description += ")"+"###"+System.identityHashCode(this)
            description
        }
    }

    /**
     * Returns `true` if the specified array should be reified and precisely tracked.
     * By default `true` is returned.
     *
     * '''This method is intended to be overwritten by subclasses to configure which
     * arrays are reified.''' Depending on the analysis task it is in general only
     * useful to only track selected arrays (e.g, arrays of certain types of values
     * or up to a specific length).
     *
     * By default only arrays up to a size of 16 (this value is more or less
     * arbitrary) are reified.
     */
    protected def reifyArray(pc: PC, count: Int, arrayType: ArrayType): Boolean = {
        count <= 16
    }

    override def NewArray(pc: PC, count: DomainValue, arrayType: ArrayType): DomainArrayValue = {
        intValueOption(count) foreach { count ⇒
            if (reifyArray(pc, count, arrayType)) {
                val defaultValue = arrayType.componentType match {
                    case BooleanType      ⇒ BooleanValue(pc, false)
                    case ByteType         ⇒ ByteValue(pc, 0)
                    case CharType         ⇒ CharValue(pc, 0)
                    case ShortType        ⇒ ShortValue(pc, 0)
                    case IntegerType      ⇒ IntegerValue(pc, 0)
                    case FloatType        ⇒ FloatValue(pc, 0.0f)
                    case LongType         ⇒ LongValue(pc, 0l)
                    case DoubleType       ⇒ DoubleValue(pc, 0.0d)
                    case _: ReferenceType ⇒ NullValue(pc)
                }
                return ArrayValue(pc, arrayType, Array.fill(count)(defaultValue))
            }
        }

        ArrayValue(pc, No, true, arrayType)
    }

    //
    // DECLARATION OF ADDITIONAL DOMAIN VALUE FACTORY METHODS
    //

    protected def ArrayValue( // for ArrayValue
        pc: PC,
        theUpperTypeBound: ArrayType,
        values: Array[DomainValue]): DomainArrayValue

}
