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
import org.opalj.br._

/**
 * Enables the precise tracking of arrays up to a specified size.
 *
 * @note Other domains that track arrays in a different way are easily imaginable.
 *
 * @author Michael Eichberg
 */
trait ArrayValues extends l1.ReferenceValues with PerInstructionPostProcessing {
    domain: Configuration with ConcreteIntegerValues with ClassHierarchy ⇒

    // We do not refine the type DomainArrayValue any further since we also want
    // to use the super level ArrayValue class to represent arrays for which we have
    // no further knowledge.
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
                // but it is still known to be valid.
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
                //   do not have enough information about the index, hence we don't
                //   know which value may have changed.
                // - if an ArrayStoreException may be thrown, we are totally lost..

                // When an exception is thrown the array remains untouched,
                // however, if no exception is thrown, we are no longer able to
                // approximate the state of the array's values; some value was changed
                // somewhere...
                val abstractArrayValue = ArrayValue(vo, No, true, theUpperTypeBound)
                registerOnRegularControlFlowUpdater(domainValue ⇒
                    domainValue match {
                        case that: ArrayValue if that eq this ⇒ abstractArrayValue
                        case _                                ⇒ domainValue
                    }
                )
                return ComputationWithSideEffectOrException(potentialExceptions)
            }

            // If we reach this point none of the given exceptions is guaranteed to be thrown
            // However, we now have to provide the solution for the happy path
            intValue[ArrayStoreResult](index) { index ⇒
                // let's check if we need to do anything
                if (values(index) != value) {
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

        override def adapt(target: TargetDomain, vo: ValueOrigin): target.DomainValue =
            target match {

                case thatDomain: l1.ArrayValues ⇒
                    val adaptedValues =
                        values.map(_.adapt(target, vo).asInstanceOf[thatDomain.DomainValue])
                    thatDomain.ArrayValue(
                        vo, theUpperTypeBound, adaptedValues).
                        asInstanceOf[target.DomainValue]

                case thatDomain: l1.ReferenceValues ⇒
                    thatDomain.ArrayValue(vo, No, true, theUpperTypeBound).
                        asInstanceOf[target.DomainValue]

                case thatDomain: l0.TypeLevelReferenceValues ⇒
                    thatDomain.InitializedArrayValue(vo, List(values.size), theUpperTypeBound).
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

        override def hashCode: Int = vo * 79 + upperTypeBound.hashCode

        override def toString() = {
            values.mkString(
                theUpperTypeBound.toJava+"(origin="+vo+", size"+values.size+"=(",
                ",",
                "))"+";SystemID="+System.identityHashCode(this))
        }
    }

    /**
     * Returns `true` if the specified array should be reified and precisely tracked.
     *
     * '''This method is intended to be overwritten by subclasses to configure which
     * arrays will be reified.''' Depending on the analysis task it is in general only
     * useful to track selected arrays (e.g, arrays of certain types of values
     * or up to a specific length). For example, to facilitate the the resolution
     * of reflectively called methods, it might be interesting to track arrays
     * that contain string values.
     *
     * @note Tracking the content of arrays generally has a significant performance
     *      impact and should be limited to cases where it is absolutely necessary.
     *      "Just tracking the contents of arrays" to improve the overall precision
     *      is in most cases not helpful.
     *
     * By default only arrays up to a size of 16 (this value is more or less
     * arbitrary) are reified.
     */
    protected def reifyArray(pc: PC, count: Int, arrayType: ArrayType): Boolean = {
        count <= 16
    }

    override def NewArray(
        pc: PC,
        count: DomainValue,
        arrayType: ArrayType): DomainArrayValue = {
        val intValue = intValueOption(count)
        if (intValue.isDefined && reifyArray(pc, intValue.get, arrayType)) {
            val count = intValue.get
            if (count >= 1024)
                println("[warn] tracking arrays ("+arrayType.toJava+
                    ") with more than 1024 ("+count+
                    ") elements is not officially supported")
            var virtualPC = 65536 + pc * 1024

            val array: Array[DomainValue] = new Array[DomainValue](count)
            var i = 0; while (i < count) {
                // we initialize each element with a new instance and also
                // assign each value with a unique PC
                array(i) = DefaultValue(virtualPC + i, arrayType.componentType)
                i += 1
            }
            ArrayValue(pc, arrayType, array)
        } else {
            ArrayValue(pc, No, true, arrayType)
        }
    }

    //
    // DECLARATION OF ADDITIONAL FACTORY METHODS
    //

    protected def ArrayValue( // for ArrayValue
        pc: PC,
        theUpperTypeBound: ArrayType,
        values: Array[DomainValue]): DomainArrayValue

}
