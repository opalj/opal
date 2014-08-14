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

/**
 * Ensures that the '''same `DomainValue`''' is used whenever we merge the same
 * pair of domain values. This ensures that the relation between the values remains the same.
 *
 * For example, given the following two stacks:
 *  - `AnIntegerValue[#1]` <- `AnIntegerValue[#1]` <- `IntegerRange(lb=0,ub=10)[#2]` <- ...
 *  - `AnIntegerValue[#3]` <- `AnIntegerValue[#3]` <- `IntegerRange(lb=0,ub=10)[#2]` <- ...
 *
 * The result will be (assuming that the result of joining `AnIntegerValue[#'''1''']` with
 *  `AnIntegerValue[#'''3''']` creates a new value, e.g., `AnIntegerValue[#'''4''']`):
 *  - `AnIntegerValue[#'''4''']` <- `AnIntegerValue[#'''4''']` <- `IntegerRange(lb=0,ub=10)[#2]` <- ...
 *
 * Without this trait each pair of values is joined again. In this case the result
 * would be:
 *  - `AnIntegerValue[#'''4''']` <- `AnIntegerValue[#'''5''']` <- `IntegerRange(lb=0,ub=10)[#2]` <- ...
 *
 * Using join stabilization is necessary if constraints are propagated
 * or (makes sense) if the merge of domain values is expensive.
 *
 * ==Thread Safety==
 * This domain requires that the join method is never invoked concurrently.
 *
 * @note Join stabilization is always done for all domain values once this trait
 *      is mixed in.
 *
 * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
 */
trait JoinStabilization extends CoreDomainFunctionality {
    /*
    import java.util.{ IdentityHashMap ⇒ IDMap }

    private[this] val leftValues =
        new IDMap[DomainValue, IDMap[DomainValue, Update[DomainValue]]]()

    abstract override protected[this] def joinValues(
        pc: PC,
        left: DomainValue, right: DomainValue): Update[DomainValue] = {
        val rightMap = leftValues.get(left)
        if (rightMap == null) {
            val rightMap = new IDMap[DomainValue, Update[DomainValue]]()
            val joinedValue = super.joinValues(pc, left, right)
            rightMap.put(right, joinedValue)
            leftValues.put(left, rightMap)
            joinedValue
        } else {
            val cachedValue = rightMap.get(right)
            if (cachedValue == null) {
                val joinedValue = super.joinValues(pc, left, right)
                rightMap.put(right, joinedValue)
                joinedValue
            } else {
                cachedValue
            }
        }
    }

    abstract override protected[this] def afterBaseJoin(pc: PC): Unit = {
        super.afterBaseJoin(pc)
        leftValues.clear()
    }
	*/

    import scala.collection.mutable.Map

    private[this] val joinedValues =
        Map.empty[IdentityPair, Update[DomainValue]]

    abstract override protected[this] def joinValues(
        pc: PC,
        left: DomainValue, right: DomainValue): Update[DomainValue] = {

        joinedValues.getOrElseUpdate(
            new IdentityPair(left, right),
            super.joinValues(pc, left, right))
    }

    abstract override protected[this] def afterBaseJoin(pc: PC): Unit = {
        super.afterBaseJoin(pc)
        joinedValues.clear()
    }
}

/**
 * Encapsulates a pair of values. Compared to a standard pair (Tuple2), however,
 * comparison of two `IdentityPair` objects is done by doing a reference-based
 * comparison of the stored values.
 *
 * @param A A non-null value.
 * @param b A non-null value.
 * @Michael Eichberg
 */
final class IdentityPair(
        final val a: Object,
        final val b: Object) {

    override def equals(other: Any): Boolean = {
        other match {
            case that: IdentityPair ⇒ (this.a eq that.a) && (this.b eq that.b)
            case _                  ⇒ false
        }
    }

    override val hashCode: Int =
        System.identityHashCode(a) * 113 + System.identityHashCode(b)
}
