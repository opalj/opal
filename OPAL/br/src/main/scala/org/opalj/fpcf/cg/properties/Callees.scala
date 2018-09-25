/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package cg
package properties

import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.collection.IntIterator
import org.opalj.collection.immutable.IntTrieSet

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
     * Potential callees of the call site at `pc`.
     *
     * @param onlyIndirectCallees If true, only indirect target callees will be returned, e.g. the
     *                            final targets of a reflective call, not the reflection method
     *                            itself
     */
    def callees(
        pc:                  Int,
        onlyIndirectCallees: Boolean
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
    def callSites(implicit propertyStore: PropertyStore): Iterator[Int] // TODO Use IntIterator once we have an IntMap

    /**
     * Map of pc to potential callees of the call site at that pc.
     *
     * @param onlyIndirectCallees If true, only indirect target callees will be returned, e.g. the
     *                            final targets of a reflective call, not the reflection method
     *                            itself
     */
    def callSites(
        onlyIndirectCallees: Boolean
    )(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): IntMap[Iterator[DeclaredMethod]]

    final def key: PropertyKey[Callees] = Callees.key
}

/**
 * Callees class used for intermediate results. Works as a proxy, aggregating results of
 * underlying analyses on demand.
 */
sealed class IntermediateCallees(
        private[this] val declaredMethod: DeclaredMethod,
        val directKeys:                   Traversable[PropertyKey[CalleesLike]],
        val indirectKeys:                 Traversable[PropertyKey[CalleesLike]]
) extends Callees {

    override def incompleteCallSites(implicit propertyStore: PropertyStore): IntIterator = {
        (directKeys.toIterator ++ indirectKeys.toIterator).foldLeft(IntIterator.empty) { (it, key) ⇒
            propertyStore(declaredMethod, key) match {
                case ESimplePS(_, ub, _) ⇒ it ++ ub.incompleteCallSites.iterator
                case _                   ⇒ IntIterator.empty
            }
        }
    }

    override def isIncompleteCallSite(pc: Int)(implicit propertyStore: PropertyStore): Boolean = {
        (directKeys.toIterator ++ indirectKeys.toIterator).exists { key ⇒
            propertyStore(declaredMethod, key) match {
                case ESimplePS(_, ub, _) ⇒ ub.incompleteCallSites.contains(pc)
                case _                   ⇒ false
            }
        }
    }

    override def callees(
        pc:                  Int,
        onlyIndirectCallees: Boolean
    )(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): Iterator[DeclaredMethod] = {
        var res: Iterator[DeclaredMethod] = Iterator.empty

        var hasIndirectCallees = false
        indirectKeys foreach { key ⇒
            propertyStore(declaredMethod, key) match {
                case ESimplePS(_, ub, _) ⇒
                    for (callees ← ub.callees(pc)) {
                        res ++= callees.iterator.map[DeclaredMethod](declaredMethods.apply)
                        hasIndirectCallees = true
                    }
                case _ ⇒ hasIndirectCallees = true
            }
        }
        if (!onlyIndirectCallees || !hasIndirectCallees) {
            directKeys foreach { key ⇒
                propertyStore(declaredMethod, key) match {
                    case ESimplePS(_, ub, _) ⇒
                        for (callees ← ub.callees(pc)) {
                            res ++= callees.iterator.map[DeclaredMethod](declaredMethods.apply)
                        }
                    case _ ⇒
                }
            }
        }

        res
    }

    override def numCallees(pc: Int)(implicit propertyStore: PropertyStore): Int = {
        (directKeys.toIterator ++ indirectKeys.toIterator).foldLeft(0) {
            (sum, key) ⇒
                propertyStore(declaredMethod, key) match {
                    case ESimplePS(_, ub, _) ⇒ sum + ub.callees(pc).map(_.size).getOrElse(0)
                    case _                   ⇒ 0
                }
        }
    }

    override def callSites(implicit propertyStore: PropertyStore): Iterator[Int] = {
        (directKeys.toIterator ++ indirectKeys.toIterator).foldLeft(Iterator.empty: Iterator[Int]) {
            (it, key) ⇒
                propertyStore(declaredMethod, key) match {
                    case ESimplePS(_, ub, _) ⇒ it ++ ub.callSitePCs
                    case _                   ⇒ Iterator.empty
                }
        }
    }

    override def callSites(
        onlyEventualCallees: Boolean
    )(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): IntMap[Iterator[DeclaredMethod]] = {
        var res: IntMap[Iterator[DeclaredMethod]] = IntMap.empty
        var indirectPCs: IntTrieSet = IntTrieSet.empty

        indirectKeys foreach { key ⇒
            propertyStore(declaredMethod, key) match {
                case ESimplePS(_, ub, _) ⇒
                    val callSites = ub.callSites
                    indirectPCs ++= callSites.keys
                    for ((k, v) ← callSites)
                        res = res.updated(
                            k,
                            res.getOrElse(k, Iterator.empty) ++
                                v.iterator.map[DeclaredMethod](declaredMethods.apply)
                        )
                case _ ⇒
            }
        }

        directKeys foreach { key ⇒
            propertyStore(declaredMethod, key) match {
                case ESimplePS(_, ub, _) ⇒
                    for ((k, v) ← ub.callSites)
                        res = res.updated(
                            k,
                            res.getOrElse(k, Iterator.empty) ++
                                v.iterator.map[DeclaredMethod](declaredMethods.apply)
                        )
                case _ ⇒
            }
        }

        res
    }
}

/**
 * Callees class used for final results where the callees are already aggregated.
 */
sealed class FinalCallees(
        val directCalleeIds:      IntMap[IntTrieSet],
        val indirectCalleesIds:   IntMap[IntTrieSet],
        val _incompleteCallSites: IntTrieSet
) extends Callees {

    override def incompleteCallSites(implicit propertyStore: PropertyStore): IntIterator = {
        _incompleteCallSites.iterator
    }

    override def isIncompleteCallSite(pc: Int)(implicit propertyStore: PropertyStore): Boolean = {
        _incompleteCallSites.contains(pc)
    }

    override def callees(
        pc:                  Int,
        onlyIndirectCallees: Boolean
    )(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): Iterator[DeclaredMethod] = {
        val indirectCallees =
            indirectCalleesIds.getOrElse(pc, IntTrieSet.empty).iterator.map[DeclaredMethod](declaredMethods.apply)

        if (onlyIndirectCallees) {
            indirectCallees
        } else {
            val directCallees =
                directCalleeIds.getOrElse(pc, IntTrieSet.empty).iterator.map[DeclaredMethod](declaredMethods.apply)
            indirectCallees ++ directCallees
        }
    }

    override def numCallees(pc: Int)(implicit propertyStore: PropertyStore): Int = {
        directCalleeIds(pc).size + indirectCalleesIds.get(pc).map(_.size).getOrElse(0)
    }

    override def callSites(implicit propertyStore: PropertyStore): Iterator[Int] = {
        directCalleeIds.keysIterator ++ indirectCalleesIds.keysIterator
    }

    override def callSites(
        onlyIndirectCallees: Boolean
    )(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): IntMap[Iterator[DeclaredMethod]] = {
        var res: IntMap[Iterator[DeclaredMethod]] = IntMap.empty

        for ((pc, calleeIds) ← indirectCalleesIds) {
            res += pc → calleeIds.iterator.map[DeclaredMethod](declaredMethods.apply)
        }

        if (!onlyIndirectCallees) {
            for ((pc, calleeIds) ← directCalleeIds) {
                val directCallees = calleeIds.map[DeclaredMethod](declaredMethods.apply)
                val indirectCallees = res.getOrElse(pc, Iterator.empty)
                res = res.updated(pc, indirectCallees ++ directCallees)
            }
        }

        res
    }
}

object NoCallees extends Callees {

    override def incompleteCallSites(implicit propertyStore: PropertyStore): IntIterator =
        IntIterator.empty

    override def isIncompleteCallSite(pc: Int)(implicit propertyStore: PropertyStore): Boolean =
        false

    override def callees(
        pc:               Int,
        finalCalleesOnly: Boolean
    )(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): Iterator[DeclaredMethod] = Iterator.empty

    override def numCallees(pc: Int)(implicit propertyStore: PropertyStore): Int = 0

    override def callSites(implicit propertyStore: PropertyStore): Iterator[Int] = Iterator.empty

    override def callSites(
        finalCalleesOnly: Boolean
    )(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): IntMap[Iterator[DeclaredMethod]] = IntMap.empty

}

object NoCalleesDueToNotReachableMethod extends Callees {

    override def incompleteCallSites(implicit propertyStore: PropertyStore): IntIterator =
        IntIterator.empty

    override def isIncompleteCallSite(pc: Int)(implicit propertyStore: PropertyStore): Boolean =
        false

    override def callees(
        pc:               Int,
        finalCalleesOnly: Boolean
    )(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): Iterator[DeclaredMethod] = Iterator.empty

    override def numCallees(pc: Int)(implicit propertyStore: PropertyStore): Int = 0

    override def callSites(implicit propertyStore: PropertyStore): Iterator[Int] = Iterator.empty

    override def callSites(
        finalCalleesOnly: Boolean
    )(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): IntMap[Iterator[DeclaredMethod]] = IntMap.empty

}

object Callees extends CalleesPropertyMetaInformation {

    final val key: PropertyKey[Callees] = {
        PropertyKey.create(
            "Callees",
            (_: PropertyStore, r: FallbackReason, _: DeclaredMethod) ⇒ {
                r match {
                    case PropertyIsNotComputedByAnyAnalysis ⇒
                        throw new IllegalStateException("No call graph analysis was scheduled")
                    case PropertyIsNotDerivedByPreviouslyExecutedAnalysis ⇒
                        NoCalleesDueToNotReachableMethod
                }
            },
            (_: PropertyStore, eps: EPS[DeclaredMethod, Callees]) ⇒ eps.ub,
            (_: PropertyStore, _: DeclaredMethod) ⇒ None
        )
    }
}
