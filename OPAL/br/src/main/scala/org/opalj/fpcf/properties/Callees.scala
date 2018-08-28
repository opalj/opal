/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties

import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.collection.immutable.IntTrieSet

import scala.collection.immutable.IntMap

/**
 * For a given [[DeclaredMethod]], and for each call site (represented by the PC), the set of methods
 * that are possible call targets.
 *
 * @author Florian Kuebler
 * @author Dominik Helm
 */
sealed trait CalleesPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = Callees
}

sealed trait Callees extends Property with CalleesPropertyMetaInformation {

    def callees(
        pc:               Int,
        finalCalleesOnly: Boolean
    )(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): Set[DeclaredMethod]

    def callsites(
        finalCalleesOnly: Boolean
    )(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): Map[Int, Set[DeclaredMethod]]

    override def toString: String = {
        //s"Callees(size=${this.size})"
        ""
    }

    final def key: PropertyKey[Callees] = Callees.key
}

sealed class CalleesImplementation(
        private[this] val declaredMethod:     DeclaredMethod,
        private[properties] val directKeys:   Set[PropertyKey[CalleesLike]],
        private[properties] val indirectKeys: Set[PropertyKey[CalleesLike]]
) extends Callees {

    override def callees(
        pc:               Int,
        finalCalleesOnly: Boolean
    )(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): Set[DeclaredMethod] = {
        var res: Set[DeclaredMethod] = Set.empty
        var hasIndirectCallees = false
        indirectKeys foreach { key ⇒
            propertyStore(declaredMethod, key) match {
                case FinalEP(_, _: CalleesLikeNotReachable | _: CalleesLikeLowerBound) ⇒
                case EPS(_, _, ub) ⇒
                    val calleesO = ub.callees(pc)
                    if (calleesO.isDefined) {
                        res ++= calleesO.get
                        hasIndirectCallees = true
                    }
                case _ ⇒ hasIndirectCallees = true
            }
        }
        if (!finalCalleesOnly || !hasIndirectCallees) {
            directKeys foreach { key ⇒
                propertyStore(declaredMethod, key) match {
                    case EPS(_, _, ub) ⇒
                        val calleesO = ub.callees(pc)
                        if (calleesO.isDefined)
                            res ++= calleesO.get
                    case _ ⇒
                }
            }
        }
        res
    }

    override def callsites(
        finalCalleesOnly: Boolean
    )(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): Map[Int, Set[DeclaredMethod]] = {
        var res: IntMap[Set[DeclaredMethod]] = IntMap.empty
        var indirectPCs: IntTrieSet = IntTrieSet.empty

        indirectKeys foreach { key ⇒
            propertyStore(declaredMethod, key) match {
                case EPS(_, _, ub) ⇒
                    val callsites = ub.callsites
                    indirectPCs ++= callsites.keys
                    for ((k, v) ← callsites)
                        res = res.updated(k, res.getOrElse(k, Set.empty) ++ v)
                case _ ⇒
            }
        }

        directKeys foreach { key ⇒
            propertyStore(declaredMethod, key) match {
                case EPS(_, _, ub) ⇒
                    for ((k, v) ← ub.callsites if !finalCalleesOnly || !indirectPCs.contains(k))
                        res = res.updated(k, res.getOrElse(k, Set.empty) ++ v)
                case _ ⇒
            }
        }

        res
    }
}

object LowerBoundCallees extends Callees {
    override def callees(
        pc:               Int,
        finalCalleesOnly: Boolean
    )(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): Set[DeclaredMethod] =
        throw new UnsupportedOperationException

    override def callsites(
        finalCalleesOnly: Boolean
    )(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): Map[Int, Set[DeclaredMethod]] =
        throw new UnsupportedOperationException
}

object NoCallees extends CalleesImplementation(null, Set.empty, Set.empty)

object NoCalleesDueToNotReachableMethod extends Callees {

    override def callees(
        pc:               Int,
        finalCalleesOnly: Boolean
    )(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): Set[DeclaredMethod] = Set.empty

    override def callsites(
        finalCalleesOnly: Boolean
    )(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): Map[Int, Set[DeclaredMethod]] = Map.empty

}

object Callees extends CalleesPropertyMetaInformation {

    final val key: PropertyKey[Callees] = {
        PropertyKey.create[DeclaredMethod, Callees](
            "Callees",
            (_: PropertyStore, r: FallbackReason, _: DeclaredMethod) ⇒ {
                r match {
                    case PropertyIsNotComputedByAnyAnalysis ⇒
                        LowerBoundCallees
                    case PropertyIsNotDerivedByPreviouslyExecutedAnalysis ⇒
                        NoCalleesDueToNotReachableMethod
                }
            },
            (_: PropertyStore, eps: EPS[DeclaredMethod, Callees]) ⇒ eps.ub,
            (_: PropertyStore, _: DeclaredMethod) ⇒ None
        )
    }
}
