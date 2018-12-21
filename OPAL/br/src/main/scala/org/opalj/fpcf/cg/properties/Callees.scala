/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package cg
package properties

import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.collection.IntIterator
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.value.ValueInformation
import scala.collection.immutable.IntMap

sealed trait CalleesPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = Callees
}

/**
 * Encapsulates all possible callees of a method, as computed by a set of cooperating call graph
 * analyses.
 */
sealed trait Callees extends Property with CalleesPropertyMetaInformation {

    /**
     * PCs of call sites that at least one of the analyses could not resolve completely.
     */
    def incompleteCallSites(implicit propertyStore: PropertyStore): IntIterator

    /**
     * Returns whether at least on analysis could not resolve the call site at `pc` completely.
     */
    def isIncompleteCallSite(pc: Int)(implicit propertyStore: PropertyStore): Boolean

    /**
     * Potential callees of the call site at `pc`. The callees may not match the invocation
     * instruction at the pc and a remapping of parameters using [[indirectCallParameters]] may be
     * necessary.
     */
    def callees(
        pc: Int
    )(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): Iterator[DeclaredMethod]

    /**
     * Potential callees of the call site at `pc`. The callees will match the invocation
     * instruction at the pc.
     */
    def directCallees(
        pc: Int
    )(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): Iterator[DeclaredMethod]

    /**
     * Potential callees of the call site at `pc`. The callees will not match the invocation
     * instruction at the pc and a remapping of parameters using [[indirectCallParameters]] may be
     * necessary.
     */
    def indirectCallees(
        pc: Int
    )(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): Iterator[DeclaredMethod]

    /**
     * Number of potential callees at the call site at `pc`.
     */
    def numCallees(pc: Int)(implicit propertyStore: PropertyStore): Int

    /**
     * PCs of all call sites in the method.
     */
    def callSitePCs(implicit propertyStore: PropertyStore): Iterator[Int] // TODO Use IntIterator once we have an IntMap

    /**
     * Map of pc to potential callees of the call site at that pc. The callees may not match the
     * invocation instruction at the pc and a remapping of parameters using
     * [[indirectCallParameters]] may be necessary.
     */
    def callSites()(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): Map[Int, Iterator[DeclaredMethod]]

    /**
     * Map of pc to potential direct callees of the call site at that pc. The callees will match the
     * invocation instruction at the pc.
     */
    def directCallSites()(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): Map[Int, Iterator[DeclaredMethod]]

    /**
     * Map of pc to potential indirect callees of the call site at that pc. The callees will not
     * match the invocation instruction at the pc and remapping of parameters using
     * [[indirectCallParameters]] may be necessary.
     */
    def indirectCallSites()(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): Map[Int, Iterator[DeclaredMethod]]

    /**
     * Returns for a given call site pc and indirect target method the sequence of parameter
     * sources. If a parameter source can not be determined, the Option will be empty, otherwise it
     * will contain all PCs and the negative indices of parameters that may define the value of the
     * corresponding actual parameter.
     * The parameter at index 0 always corresponds to the *this* local and is `null` for static
     * methods.
     */
    def indirectCallParameters(
        pc:     Int,
        callee: DeclaredMethod
    )(implicit propertyStore: PropertyStore): Seq[Option[(ValueInformation, IntTrieSet)]]

    final def key: PropertyKey[Callees] = Callees.key
}

/**
 * Callees class used for final results where the callees are already aggregated.
 */
sealed class FinalCallees(
        private[this] val directCalleesIds:        IntMap[IntTrieSet],
        private[this] val indirectCalleesIds:      IntMap[IntTrieSet],
        private[this] val _incompleteCallSites:    IntTrieSet,
        private[this] val _indirectCallParameters: IntMap[Map[DeclaredMethod, Seq[Option[(ValueInformation, IntTrieSet)]]]]
) extends Callees {

    override def incompleteCallSites(implicit propertyStore: PropertyStore): IntIterator = {
        _incompleteCallSites.iterator
    }

    override def isIncompleteCallSite(pc: Int)(implicit propertyStore: PropertyStore): Boolean = {
        _incompleteCallSites.contains(pc)
    }

    override def callees(
        pc: Int
    )(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): Iterator[DeclaredMethod] = {
        directCallees(pc) ++ indirectCallees(pc)
    }

    override def directCallees(
        pc: Int
    )(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): Iterator[DeclaredMethod] = {
        directCalleesIds.getOrElse(pc, IntTrieSet.empty).iterator.map[DeclaredMethod](declaredMethods.apply)
    }

    override def indirectCallees(
        pc: Int
    )(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): Iterator[DeclaredMethod] = {
        indirectCalleesIds.getOrElse(pc, IntTrieSet.empty).iterator.map[DeclaredMethod](declaredMethods.apply)
    }

    override def numCallees(pc: Int)(implicit propertyStore: PropertyStore): Int = {
        directCalleesIds(pc).size + indirectCalleesIds.get(pc).map(_.size).getOrElse(0)
    }

    override def callSitePCs(implicit propertyStore: PropertyStore): Iterator[Int] = {
        directCalleesIds.keysIterator ++ indirectCalleesIds.keysIterator
    }

    override def callSites()(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): IntMap[Iterator[DeclaredMethod]] = {
        var res = IntMap(directCallSites().toStream: _*)

        for ((pc, indirect) ← indirectCallSites()) {
            res = res.updateWith(pc, indirect, (direct, indirect) ⇒ direct ++ indirect)
        }

        res
    }

    override def directCallSites()(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): Map[Int, Iterator[DeclaredMethod]] = {
        directCalleesIds.mapValues { calleeIds ⇒
            calleeIds.iterator.map[DeclaredMethod](declaredMethods.apply)
        }
    }

    override def indirectCallSites()(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): Map[Int, Iterator[DeclaredMethod]] = {
        indirectCalleesIds.mapValues { calleeIds ⇒
            calleeIds.iterator.map[DeclaredMethod](declaredMethods.apply)
        }
    }

    override def indirectCallParameters(
        pc:     Int,
        method: DeclaredMethod
    )(
        implicit
        propertyStore: PropertyStore
    ): Seq[Option[(ValueInformation, IntTrieSet)]] = {
        _indirectCallParameters(pc)(method)
    }
}

object NoCallees extends Callees {

    override def incompleteCallSites(implicit propertyStore: PropertyStore): IntIterator =
        IntIterator.empty

    override def isIncompleteCallSite(pc: Int)(implicit propertyStore: PropertyStore): Boolean =
        false

    override def callees(pc: Int)(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): Iterator[DeclaredMethod] = Iterator.empty

    override def directCallees(pc: Int)(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): Iterator[DeclaredMethod] = Iterator.empty

    override def indirectCallees(pc: Int)(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): Iterator[DeclaredMethod] = Iterator.empty

    override def numCallees(pc: Int)(implicit propertyStore: PropertyStore): Int = 0

    override def callSitePCs(implicit propertyStore: PropertyStore): Iterator[Int] = Iterator.empty

    override def callSites()(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): IntMap[Iterator[DeclaredMethod]] = IntMap.empty

    override def directCallSites()(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): IntMap[Iterator[DeclaredMethod]] = IntMap.empty

    override def indirectCallSites()(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): IntMap[Iterator[DeclaredMethod]] = IntMap.empty

    override def indirectCallParameters(
        pc:     Int,
        method: DeclaredMethod
    )(
        implicit
        propertyStore: PropertyStore
    ): Seq[Option[(ValueInformation, IntTrieSet)]] = Seq.empty

}

object NoCalleesDueToNotReachableMethod extends Callees {

    override def incompleteCallSites(implicit propertyStore: PropertyStore): IntIterator =
        IntIterator.empty

    override def isIncompleteCallSite(pc: Int)(implicit propertyStore: PropertyStore): Boolean =
        false

    override def callees(pc: Int)(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): Iterator[DeclaredMethod] = Iterator.empty

    override def directCallees(pc: Int)(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): Iterator[DeclaredMethod] = Iterator.empty

    override def indirectCallees(pc: Int)(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): Iterator[DeclaredMethod] = Iterator.empty

    override def numCallees(pc: Int)(implicit propertyStore: PropertyStore): Int = 0

    override def callSitePCs(implicit propertyStore: PropertyStore): Iterator[Int] = Iterator.empty

    override def callSites()(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): IntMap[Iterator[DeclaredMethod]] = IntMap.empty

    override def directCallSites()(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): IntMap[Iterator[DeclaredMethod]] = IntMap.empty

    override def indirectCallSites()(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): IntMap[Iterator[DeclaredMethod]] = IntMap.empty

    override def indirectCallParameters(
        pc:     Int,
        method: DeclaredMethod
    )(
        implicit
        propertyStore: PropertyStore
    ): Seq[Option[(ValueInformation, IntTrieSet)]] = Seq.empty

}

object Callees extends CalleesPropertyMetaInformation {

    final val key: PropertyKey[Callees] = {
        val name = "opalj.Callees"
        PropertyKey.create(
            name,
            (_: PropertyStore, reason: FallbackReason, _: Entity) ⇒ reason match {
                case PropertyIsNotDerivedByPreviouslyExecutedAnalysis ⇒ NoCalleesDueToNotReachableMethod
                case _                                                ⇒
                    throw new IllegalStateException(s"No analysis is scheduled for property: $name")
            }
        )
    }
}
