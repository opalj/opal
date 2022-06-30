/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package properties
package cg

import scala.collection.immutable.IntMap

import org.opalj.collection.IntIterator
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyIsNotDerivedByPreviouslyExecutedAnalysis
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.value.ValueInformation
import org.opalj.br.Opcode
import org.opalj.br.PCs
import org.opalj.br.fpcf.properties.Context
import org.opalj.tac.fpcf.analyses.cg.TypeProvider

sealed trait CalleesPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = Callees
}

/**
 * Encapsulates all possible callees of a method, as computed by a set of cooperating call graph
 * analyses.
 */
sealed trait Callees extends Property with CalleesPropertyMetaInformation {

    /**
     * Is there a call to method `target` at `pc`?
     */
    def containsCall(callerContext: Context, pc: Int, calleeContext: Context): Boolean

    def containsDirectCall(callerContext: Context, pc: Int, calleeContext: Context): Boolean

    def containsIndirectCall(callerContext: Context, pc: Int, calleeContext: Context): Boolean

    /**
     * PCs of call sites that at least one of the analyses could not resolve completely.
     */
    def incompleteCallSites(
        callerContext: Context
    )(implicit propertyStore: PropertyStore): IntIterator

    /**
     * Returns whether at least on analysis could not resolve the call site at `pc` completely.
     */
    def isIncompleteCallSite(
        callerContext: Context, pc: Int
    )(implicit propertyStore: PropertyStore): Boolean

    /**
     * States whether there is at least one call site that could not be resolved.
     */
    def hasIncompleteCallSites(callerContext: Context): Boolean

    /**
     * Potential callees of the call site at `pc`. The callees may not match the invocation
     * instruction at the pc and a remapping of parameters using [[indirectCallParameters]] may be
     * necessary.
     */
    def callees(
        callerContext: Context,
        pc:            Int
    )(
        implicit
        propertyStore: PropertyStore,
        typeProvider:  TypeProvider
    ): Iterator[Context]

    /**
     * Potential callees of the call site at `pc`. The callees will match the invocation
     * instruction at the pc.
     */
    def directCallees(
        callerContext: Context,
        pc:            Int
    )(
        implicit
        propertyStore: PropertyStore,
        typeProvider:  TypeProvider
    ): Iterator[Context]

    /**
     * Potential callees of the call site at `pc`. The callees will not match the invocation
     * instruction at the pc and a remapping of parameters using [[indirectCallParameters]] may be
     * necessary.
     */
    def indirectCallees(
        callerContext: Context,
        pc:            Int
    )(
        implicit
        propertyStore: PropertyStore,
        typeProvider:  TypeProvider
    ): Iterator[Context]

    /**
     * Number of potential callees at the call site at `pc`.
     */
    def numCallees(pc: Int)(implicit propertyStore: PropertyStore): Int

    /**
     * The available contexts of the calling methods.
     */
    def callerContexts(implicit typeProvider: TypeProvider): Iterator[Context]

    /**
     * PCs of all call sites in the method.
     */
    // TODO Use IntIterator once we have our own IntMap
    def callSitePCs(callerContext: Context)(implicit propertyStore: PropertyStore): Iterator[Int]

    /**
     * Map of pc to potential callees of the call site at that pc. The callees may not match the
     * invocation instruction at the pc and a remapping of parameters using
     * [[indirectCallParameters]] may be necessary.
     */
    def callSites(callerContext: Context)(
        implicit
        propertyStore: PropertyStore,
        typeProvider:  TypeProvider
    ): Map[Int, Iterator[Context]]

    /**
     * Map of pc to potential direct callees of the call site at that pc. The callees will match the
     * invocation instruction at the pc.
     */
    def directCallSites(callerContext: Context)(
        implicit
        propertyStore: PropertyStore,
        typeProvider:  TypeProvider
    ): Map[Int, Iterator[Context]]

    /**
     * Map of pc to potential indirect callees of the call site at that pc. The callees will not
     * match the invocation instruction at the pc and remapping of parameters using
     * [[indirectCallParameters]] may be necessary.
     */
    def indirectCallSites(callerContext: Context)(
        implicit
        propertyStore: PropertyStore,
        typeProvider:  TypeProvider
    ): Map[Int, Iterator[Context]]

    /**
     * Returns for a given call site pc and indirect target method the receiver information.
     * If the receiver can not be determined, the `scala.Option` will be empty, otherwise it will
     * contain all [[br.PCs]] and the the negative indices of parameters that may define the value of
     * the receiver.
     * The parameter at index 0 always corresponds to the *this* local and is `null` for static
     * methods.
     */
    def indirectCallReceiver(
        callerContext: Context, pc: Int, calleeContext: Context
    ): Option[(ValueInformation, br.PCs)]

    /**
     * Returns for a given call site pc and indirect target method the sequence of parameter
     * sources. If a parameter source can not be determined, the `scala.Option` will be empty,
     * otherwise it will contain all PCs and the negative indices of parameters that may define the
     * value of the corresponding actual parameter.
     * The parameter at index 0 always corresponds to the *this* local and is `null` for static
     * methods.
     */
    def indirectCallParameters(
        callerContext: Context,
        pc:            Int,
        calleeContext: Context
    )(implicit propertyStore: PropertyStore): Seq[Option[(ValueInformation, IntTrieSet)]]

    /**
     * Creates a copy of the current callees object, including the additional callee information
     * specified in the parameters.
     */
    def updateWithCallees(
        callerContext:          Context,
        directCallees:          IntMap[IntTrieSet],
        indirectCallees:        IntMap[IntTrieSet],
        incompleteCallSites:    br.PCs,
        indirectCallReceivers:  IntMap[IntMap[Option[(ValueInformation, br.PCs)]]],
        indirectCallParameters: IntMap[IntMap[Seq[Option[(ValueInformation, br.PCs)]]]]
    ): Callees

    final def key: PropertyKey[Callees] = Callees.key
}

/**
 * Callees class used for final results where the callees are already aggregated.
 */
sealed class ConcreteCallees(
        private[this] val directCalleesIds:        IntMap[IntMap[IntTrieSet]], // Caller Context => PC => Callees
        private[this] val indirectCalleesIds:      IntMap[IntMap[IntTrieSet]], // Caller Context => PC => Callees
        private[this] val _incompleteCallSites:    IntMap[br.PCs], // Caller Context => PCs
        private[this] val _indirectCallReceivers:  IntMap[IntMap[IntMap[Option[(ValueInformation, br.PCs)]]]], // Caller Context => PC => Callee => Receiver
        private[this] val _indirectCallParameters: IntMap[IntMap[IntMap[Seq[Option[(ValueInformation, br.PCs)]]]]] // Caller Context => PC => Callee => Parameters
) extends Callees {

    override def incompleteCallSites(callerContext: Context)(implicit propertyStore: PropertyStore): IntIterator = {
        _incompleteCallSites.getOrElse(callerContext.id, IntTrieSet.empty).iterator
    }

    override def isIncompleteCallSite(callerContext: Context, pc: Int)(implicit propertyStore: PropertyStore): Boolean = {
        val cId = callerContext.id
        _incompleteCallSites.contains(cId) && _incompleteCallSites(cId).contains(pc)
    }

    override def hasIncompleteCallSites(callerContext: Context): Boolean = {
        val cId = callerContext.id
        _incompleteCallSites.contains(cId) && _incompleteCallSites(cId).nonEmpty
    }

    override def callees(
        callerContext: Context,
        pc:            Int
    )(
        implicit
        propertyStore: PropertyStore,
        typeProvider:  TypeProvider
    ): Iterator[Context] = {
        directCallees(callerContext, pc) ++ indirectCallees(callerContext, pc)
    }

    override def directCallees(
        callerContext: Context,
        pc:            Int
    )(
        implicit
        propertyStore: PropertyStore,
        typeProvider:  TypeProvider
    ): Iterator[Context] = {
        val contexts = directCalleesIds.get(callerContext.id)
        val directCallees = contexts.flatMap(_.get(pc)).getOrElse(IntTrieSet.empty)
        directCallees.iterator.map[Context](typeProvider.contextFromId)
    }

    override def indirectCallees(
        callerContext: Context,
        pc:            Int
    )(
        implicit
        propertyStore: PropertyStore,
        typeProvider:  TypeProvider
    ): Iterator[Context] = {
        val contexts = indirectCalleesIds.get(callerContext.id)
        val indirectCallees = contexts.flatMap(_.get(pc)).getOrElse(IntTrieSet.empty)
        indirectCallees.iterator.map[Context](typeProvider.contextFromId)
    }

    override def numCallees(pc: Int)(implicit propertyStore: PropertyStore): Int = {
        directCalleesIds.valuesIterator.map { _(pc).size }.sum +
            indirectCalleesIds.valuesIterator.map { _.get(pc).map(_.size).getOrElse(0) }.sum
    }

    override def callerContexts(implicit typeProvider: TypeProvider): Iterator[Context] = {
        directCalleesIds.keysIterator.map(typeProvider.contextFromId)
    }

    override def callSitePCs(callerContext: Context)(implicit propertyStore: PropertyStore): Iterator[Int] = {
        directCalleesIds(callerContext.id).keysIterator ++
            indirectCalleesIds(callerContext.id).keysIterator
    }

    override def callSites(callerContext: Context)(
        implicit
        propertyStore: PropertyStore,
        typeProvider:  TypeProvider
    ): IntMap[Iterator[Context]] = {
        var res = IntMap(directCallSites(callerContext).to(LazyList): _*)

        for ((pc, indirect) <- indirectCallSites(callerContext)) {
            res = res.updateWith(pc, indirect, (direct, indirect) => direct ++ indirect)
        }

        res
    }

    override def directCallSites(callerContext: Context)(
        implicit
        propertyStore: PropertyStore,
        typeProvider:  TypeProvider
    ): Map[Int, Iterator[Context]] = {
        if (directCalleesIds.contains(callerContext.id))
            directCalleesIds(callerContext.id).view.mapValues { calleeIds =>
                calleeIds.iterator.map[Context](typeProvider.contextFromId)
            }.toMap
        else Map.empty
    }

    override def indirectCallSites(callerContext: Context)(
        implicit
        propertyStore: PropertyStore,
        typeProvider:  TypeProvider
    ): Map[Int, Iterator[Context]] = {
        if (indirectCalleesIds.contains(callerContext.id))
            indirectCalleesIds(callerContext.id).view.mapValues { calleeIds =>
                calleeIds.iterator.map[Context](typeProvider.contextFromId)
            }.toMap
        else Map.empty
    }

    override def indirectCallReceiver(
        callerContext: Context, pc: Opcode, calleeContext: Context
    ): Option[(ValueInformation, br.PCs)] = {
        _indirectCallReceivers(callerContext.id)(pc)(calleeContext.id)
    }

    override def indirectCallParameters(
        callerContext: Context,
        pc:            Int,
        calleeContext: Context
    )(
        implicit
        propertyStore: PropertyStore
    ): Seq[Option[(ValueInformation, IntTrieSet)]] = {
        _indirectCallParameters(callerContext.id)(pc)(calleeContext.id)
    }

    override def updateWithCallees(
        callerContext:          Context,
        directCallees:          IntMap[IntTrieSet],
        indirectCallees:        IntMap[IntTrieSet],
        incompleteCallSites:    br.PCs,
        indirectCallReceivers:  IntMap[IntMap[Option[(ValueInformation, br.PCs)]]],
        indirectCallParameters: IntMap[IntMap[Seq[Option[(ValueInformation, br.PCs)]]]]
    ): Callees = {
        val cId = callerContext.id
        directCalleesIds.updateWith(cId, directCallees, (o, n) => o.unionWith(n, (_, l, r) => l ++ r))
        new ConcreteCallees(
            directCalleesIds.updateWith(
                cId, directCallees, (o, n) => o.unionWith(n, (_, l, r) => l ++ r)
            ),
            indirectCalleesIds.updateWith(
                cId, indirectCallees, (o, n) => o.unionWith(n, (_, l, r) => l ++ r)
            ),
            _incompleteCallSites.updateWith(cId, incompleteCallSites, (o, n) => o ++ n),
            _indirectCallReceivers.updateWith(
                cId,
                indirectCallReceivers,
                (o, n) =>
                    o.unionWith(
                        n,
                        (_, l, r) => {
                            r.unionWith(
                                l,
                                (_, vl, vr) =>
                                    if (vl == vr) vl
                                    else throw new UnknownError("Incompatible receivers for indirect call")
                            )
                        }
                    )
            ),
            _indirectCallParameters.updateWith(
                cId,
                indirectCallParameters,
                (o, n) =>
                    o.unionWith(
                        n,
                        (_, l, r) => {
                            r.unionWith(
                                l,
                                (_, vl, vr) =>
                                    if (vl == vr) vl
                                    else throw new UnknownError("Incompatible receivers for indirect call")
                            )
                        }
                    )
            )
        )
    }

    override def containsCall(callerContext: Context, pc: Int, calleeContext: Context): Boolean = {
        containsDirectCall(callerContext, pc, calleeContext) ||
            containsIndirectCall(callerContext, pc, calleeContext)
    }

    override def containsDirectCall(
        callerContext: Context, pc: Int, calleeContext: Context
    ): Boolean = {
        val cId = callerContext.id
        directCalleesIds.contains(cId) && directCalleesIds(cId).contains(pc) &&
            directCalleesIds(cId)(pc).contains(calleeContext.id)
    }

    override def containsIndirectCall(
        callerContext: Context, pc: Int, calleeContext: Context
    ): Boolean = {
        val cId = callerContext.id
        indirectCalleesIds.contains(cId) && indirectCalleesIds(cId).contains(pc) &&
            indirectCalleesIds(cId)(pc).contains(calleeContext.id)
    }
}

object ConcreteCallees {

    def apply(
        callerContext:          Context,
        directCallees:          IntMap[PCs],
        indirectCallees:        IntMap[PCs],
        incompleteCallSites:    PCs,
        indirectCallReceivers:  IntMap[IntMap[Option[(ValueInformation, PCs)]]],
        indirectCallParameters: IntMap[IntMap[Seq[Option[(ValueInformation, PCs)]]]]
    ): ConcreteCallees = {
        val cId = callerContext.id
        new ConcreteCallees(
            IntMap(cId -> directCallees),
            IntMap(cId -> indirectCallees),
            IntMap(cId -> incompleteCallSites),
            IntMap(cId -> indirectCallReceivers),
            IntMap(cId -> indirectCallParameters)
        )
    }

}

object NoCallees extends Callees {

    override def incompleteCallSites(
        callerContext: Context
    )(implicit propertyStore: PropertyStore): IntIterator = IntIterator.empty

    override def isIncompleteCallSite(
        callerContext: Context, pc: Int
    )(implicit propertyStore: PropertyStore): Boolean = false

    override def hasIncompleteCallSites(callerContext: Context): Boolean = false

    override def callees(callerContext: Context, pc: Int)(
        implicit
        propertyStore: PropertyStore,
        typeProvider:  TypeProvider
    ): Iterator[Context] = Iterator.empty

    override def directCallees(callerContext: Context, pc: Int)(
        implicit
        propertyStore: PropertyStore,
        typeProvider:  TypeProvider
    ): Iterator[Context] = Iterator.empty

    override def indirectCallees(callerContext: Context, pc: Int)(
        implicit
        propertyStore: PropertyStore,
        typeProvider:  TypeProvider
    ): Iterator[Context] = Iterator.empty

    override def numCallees(pc: Int)(implicit propertyStore: PropertyStore): Int = 0

    override def callerContexts(implicit typeProvider: TypeProvider): Iterator[Context] =
        Iterator.empty

    override def callSitePCs(
        callerContext: Context
    )(implicit propertyStore: PropertyStore): IntIterator = IntIterator.empty

    override def callSites(callerContext: Context)(
        implicit
        propertyStore: PropertyStore,
        typeProvider:  TypeProvider
    ): IntMap[Iterator[Context]] = IntMap.empty

    override def directCallSites(callerContext: Context)(
        implicit
        propertyStore: PropertyStore,
        typeProvider:  TypeProvider
    ): IntMap[Iterator[Context]] = IntMap.empty

    override def indirectCallSites(callerContext: Context)(
        implicit
        propertyStore: PropertyStore,
        typeProvider:  TypeProvider
    ): IntMap[Iterator[Context]] = IntMap.empty

    override def indirectCallReceiver(
        callerContext: Context, pc: Opcode, calleeContext: Context
    ): Option[(ValueInformation, br.PCs)] = None

    override def indirectCallParameters(
        callerContext: Context,
        pc:            Int,
        calleeContext: Context
    )(
        implicit
        propertyStore: PropertyStore
    ): Seq[Option[(ValueInformation, IntTrieSet)]] = Seq.empty

    override def updateWithCallees(
        callerContext:          Context,
        directCallees:          IntMap[IntTrieSet],
        indirectCallees:        IntMap[IntTrieSet],
        incompleteCallSites:    br.PCs,
        indirectCallReceivers:  IntMap[IntMap[Option[(ValueInformation, br.PCs)]]],
        indirectCallParameters: IntMap[IntMap[Seq[Option[(ValueInformation, br.PCs)]]]]
    ): ConcreteCallees = {
        ConcreteCallees(
            callerContext,
            directCallees,
            indirectCallees,
            incompleteCallSites,
            indirectCallReceivers,
            indirectCallParameters
        )
    }

    override def containsCall(
        callerContext: Context, pc: Int, calleeContext: Context
    ): Boolean = false

    override def containsDirectCall(
        callerContext: Context, pc: Int, calleeContext: Context
    ): Boolean = false

    override def containsIndirectCall(
        callerContext: Context, pc: Int, calleeContext: Context
    ): Boolean = false
}

object NoCalleesDueToNotReachableMethod extends Callees {

    override def incompleteCallSites(
        callerContext: Context
    )(implicit propertyStore: PropertyStore): IntIterator = IntIterator.empty

    override def isIncompleteCallSite(
        callerContext: Context, pc: Int
    )(implicit propertyStore: PropertyStore): Boolean = false

    override def hasIncompleteCallSites(callerContext: Context): Boolean = false

    override def callees(callerContext: Context, pc: Int)(
        implicit
        propertyStore: PropertyStore,
        typeProvider:  TypeProvider
    ): Iterator[Context] = Iterator.empty

    override def directCallees(callerContext: Context, pc: Int)(
        implicit
        propertyStore: PropertyStore,
        typeProvider:  TypeProvider
    ): Iterator[Context] = Iterator.empty

    override def indirectCallees(callerContext: Context, pc: Int)(
        implicit
        propertyStore: PropertyStore,
        typeProvider:  TypeProvider
    ): Iterator[Context] = Iterator.empty

    override def numCallees(pc: Int)(implicit propertyStore: PropertyStore): Int = 0

    override def callerContexts(implicit typeProvider: TypeProvider): Iterator[Context] =
        Iterator.empty

    override def callSitePCs(
        callerContext: Context
    )(implicit propertyStore: PropertyStore): IntIterator = IntIterator.empty

    override def callSites(callerContext: Context)(
        implicit
        propertyStore: PropertyStore,
        typeProvider:  TypeProvider
    ): IntMap[Iterator[Context]] = IntMap.empty

    override def directCallSites(callerContext: Context)(
        implicit
        propertyStore: PropertyStore,
        typeProvider:  TypeProvider
    ): IntMap[Iterator[Context]] = IntMap.empty

    override def indirectCallSites(callerContext: Context)(
        implicit
        propertyStore: PropertyStore,
        typeProvider:  TypeProvider
    ): IntMap[Iterator[Context]] = IntMap.empty

    override def indirectCallReceiver(
        callerContext: Context, pc: Opcode, calleeContext: Context
    ): Option[(ValueInformation, br.PCs)] = None

    override def indirectCallParameters(
        callerContext: Context,
        pc:            Int,
        calleeContext: Context
    )(
        implicit
        propertyStore: PropertyStore
    ): Seq[Option[(ValueInformation, IntTrieSet)]] = Seq.empty

    override def updateWithCallees(
        callerContext:          Context,
        directCallees:          IntMap[IntTrieSet],
        indirectCallees:        IntMap[IntTrieSet],
        incompleteCallSites:    br.PCs,
        indirectCallReceivers:  IntMap[IntMap[Option[(ValueInformation, br.PCs)]]],
        indirectCallParameters: IntMap[IntMap[Seq[Option[(ValueInformation, br.PCs)]]]]
    ): Callees = throw new IllegalStateException("Unreachable methods can't be updated!")

    override def containsCall(
        callerContext: Context, pc: Int, calleeContext: Context
    ): Boolean = false

    override def containsDirectCall(
        callerContext: Context, pc: Int, calleeContext: Context
    ): Boolean = false

    override def containsIndirectCall(
        callerContext: Context, pc: Int, calleeContext: Context
    ): Boolean = false
}

object Callees extends CalleesPropertyMetaInformation {

    final val key: PropertyKey[Callees] = {
        val name = "opalj.Callees"
        PropertyKey.create(
            name,
            (_: PropertyStore, reason: FallbackReason, _: Entity) => reason match {
                case PropertyIsNotDerivedByPreviouslyExecutedAnalysis =>
                    NoCalleesDueToNotReachableMethod
                case _ =>
                    throw new IllegalStateException(s"analysis required for property: $name")
            }
        )
    }
}
