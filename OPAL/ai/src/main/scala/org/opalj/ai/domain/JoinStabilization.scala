/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import java.util.{HashMap => JHashMap}
//import scala.collection.mutable.Map
//import scala.collection.mutable.AnyRefMap

import org.opalj.collection.immutable.IdentityPair

/**
 * Ensures that the '''same `DomainValue`''' is used whenever we merge the same
 * pair of domain values. This ensures that the relation between the values remains the same.
 *
 * For example, given the following two stacks:
 *  - `AnIntegerValue[#1]` <- `AnIntegerValue[#1]` <- `IntRange(lb=0,ub=10)[#2]` <- ...
 *  - `AnIntegerValue[#3]` <- `AnIntegerValue[#3]` <- `IntRange(lb=0,ub=10)[#2]` <- ...
 *
 * The result will be (assuming that the result of joining `AnIntegerValue[#'''1''']` with
 *  `AnIntegerValue[#'''3''']` creates a new value, e.g., `AnIntegerValue[#'''4''']`):
 *  - `AnIntegerValue[#'''4''']` <- `AnIntegerValue[#'''4''']` <- `IntRange(lb=0,ub=10)[#2]` <- ...
 *
 * Without this trait each pair of values is joined again. In this case the result would be:
 *  - `AnIntegerValue[#'''4''']` <- `AnIntegerValue[#'''5''']` <- `IntRange(lb=0,ub=10)[#2]` <- ...
 *
 * Using join stabilization is necessary if constraints are propagated
 * or (makes sense) if the merge of domain values is expensive.
 *
 * @note Join stabilization is always done for all domain values once this trait is mixed in.
 *
 * @author Michael Eichberg
 */
trait JoinStabilization extends CoreDomainFunctionality {

    // THE FOLLOWING IS AN ALTERNATIVE IMPLEMENTATION
    // WHICH IS LESS EFFICIENT (BUT WAS EXPECTED TO BE MORE EFFICIENT)
    //
    //    import java.util.{ IdentityHashMap => IDMap }
    //
    //    private[this] val leftValues =
    //        new IDMap[DomainValue, IDMap[DomainValue, Update[DomainValue]]]()
    //
    //    abstract override protected[this] def joinValues(
    //        pc: PC,
    //        left: DomainValue, right: DomainValue): Update[DomainValue] = {
    //        val rightMap = leftValues.get(left)
    //        if (rightMap == null) {
    //            val rightMap = new IDMap[DomainValue, Update[DomainValue]]()
    //            val joinedValue = super.joinValues(pc, left, right)
    //            rightMap.put(right, joinedValue)
    //            leftValues.put(left, rightMap)
    //            joinedValue
    //        } else {
    //            val cachedValue = rightMap.get(right)
    //            if (cachedValue == null) {
    //                val joinedValue = super.joinValues(pc, left, right)
    //                rightMap.put(right, joinedValue)
    //                joinedValue
    //            } else {
    //                cachedValue
    //            }
    //        }
    //    }
    //
    //    abstract override protected[this] def afterBaseJoin(pc: PC): Unit = {
    //        super.afterBaseJoin(pc)
    //        leftValues.clear()
    //    }

    //    import java.util.HashMap
    //
    //    private[this] val joinedValues =
    //        new HashMap[IdentityPair[AnyRef, AnyRef], Update[DomainValue]]()
    //
    //     override protected[this] def joinValues(
    //        pc: PC,
    //        left: DomainValue, right: DomainValue): Update[DomainValue] = {
    //        val key = new IdentityPair(left, right)
    //        val value = joinedValues.get(key)
    //        if (value ne null) {
    //            value
    //        } else {
    //            val value = super.joinValues(pc, left, right)
    //            joinedValues.put(key, value)
    //            value
    //        }
    //    }
    //
    //     override protected[this] def afterBaseJoin(pc: PC): Unit = {
    //        super.afterBaseJoin(pc)
    //        joinedValues.clear()
    //    }

    /*
    protected[this] val joinedValues: Map[IdentityPair[AnyRef, AnyRef], Update[DomainValue]] = {
        AnyRefMap.empty[IdentityPair[AnyRef, AnyRef], Update[DomainValue]]
    }

    /** Classes overriding this method generally have to call it! */
    override protected[this] def joinValues(
        pc:   Int,
        left: DomainValue, right: DomainValue
    ): Update[DomainValue] = {
        val key = new IdentityPair(left, right)
        joinedValues.getOrElseUpdate(key, super.joinValues(pc, left, right))
    }

    /** Classes overriding this method generally have to call it! */
    override protected[this] def afterBaseJoin(pc: Int): Unit = {
        super.afterBaseJoin(pc)
        joinedValues.clear()
    }
    */

    protected[this] val joinedValues: JHashMap[IdentityPair[AnyRef, AnyRef], Update[DomainValue]] = {
        new JHashMap[IdentityPair[AnyRef, AnyRef], Update[DomainValue]]()
    }

    /** Classes overriding this method generally have to call it! */
    override protected[this] def joinValues(
        pc:   Int,
        left: DomainValue, right: DomainValue
    ): Update[DomainValue] = {
        val key = new IdentityPair(left, right)
        val joinedValue = joinedValues.get(key)
        if (joinedValue != null) {
            joinedValue
        } else {
            val newJoinedValue = super.joinValues(pc, left, right)
            joinedValues.put(key, newJoinedValue)
            newJoinedValue
        }
    }

    /** Classes overriding this method generally have to call it! */
    override protected[this] def afterBaseJoin(pc: Int): Unit = {
        super.afterBaseJoin(pc)
        joinedValues.clear()
    }

}
