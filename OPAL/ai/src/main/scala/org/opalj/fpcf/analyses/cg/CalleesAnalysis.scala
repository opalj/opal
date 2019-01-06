/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses
package cg

import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.cg.properties.Callees
import org.opalj.fpcf.cg.properties.CalleesLikeNotReachable
import org.opalj.fpcf.cg.properties.CalleesLike
import org.opalj.fpcf.cg.properties.CalleesLikePropertyMetaInformation
import org.opalj.fpcf.cg.properties.NoCalleesDueToNotReachableMethod
import org.opalj.fpcf.cg.properties.ConcreteCallees
import org.opalj.fpcf.cg.properties.IndirectCallees
import scala.collection.immutable.IntMap

import org.opalj.value.ValueInformation

// todo discuss whether we want the callees property computed collaboratively or stick with the
// subproperties
/**
 *
 * @author Florian Kuebler
 * @author Dominik Helm
 */
final class CalleesAnalysis private[analyses] (
        final val project:           SomeProject,
        directCalleesPropertyKeys:   Set[PropertyKey[CalleesLike]],
        indirectCalleesPropertyKeys: Set[PropertyKey[CalleesLike]]
) extends FPCFAnalysis {

    implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    def doAnalysis(dm: DeclaredMethod): ProperPropertyComputationResult = {
        var dependees: Set[EOptionP[DeclaredMethod, CalleesLike]] = Set.empty
        var isReachable = false
        var directKeys = directCalleesPropertyKeys
        var indirectKeys = indirectCalleesPropertyKeys

        val handlePropertyKey = (pk: PropertyKey[CalleesLike]) ⇒ {
            val r = handleEOptP(propertyStore(dm, pk), dependees, directKeys, indirectKeys)
            isReachable |= r._1
            dependees = r._2
            directKeys = r._3
            indirectKeys = r._4
        }: Unit
        directCalleesPropertyKeys.foreach(handlePropertyKey)
        indirectCalleesPropertyKeys.foreach(handlePropertyKey)

        returnResult(dm, dependees, directKeys, indirectKeys, isReachable)

    }

    @inline def handleEOptP(
        eOptionP:     EOptionP[DeclaredMethod, CalleesLike],
        dependees:    Set[EOptionP[DeclaredMethod, CalleesLike]],
        directKeys:   Set[PropertyKey[CalleesLike]],
        indirectKeys: Set[PropertyKey[CalleesLike]]
    ): (Boolean, Set[EOptionP[DeclaredMethod, CalleesLike]], Set[PropertyKey[CalleesLike]], Set[PropertyKey[CalleesLike]]) = {
        eOptionP match {
            case ep @ FinalP(p: CalleesLikeNotReachable) ⇒
                if (p.isIndirect)
                    (false, updateDependee(ep, dependees), directKeys, indirectKeys - p.key)
                else
                    (false, updateDependee(ep, dependees), directKeys - p.key, indirectKeys)

            case InterimUBP(_: CalleesLikeNotReachable) ⇒
                throw new IllegalArgumentException("non reachable methods must have final property")

            case ep: EPS[DeclaredMethod, CalleesLike] ⇒
                (true, updateDependee(ep, dependees), directKeys, indirectKeys)

            case epk: EPK[DeclaredMethod, CalleesLike] ⇒
                (true, dependees + epk, directKeys, indirectKeys)
        }
    }

    def continuation(
        declaredMethod: DeclaredMethod,
        directKeys:     Set[PropertyKey[CalleesLike]],
        indirectKeys:   Set[PropertyKey[CalleesLike]],
        dependees:      Set[EOptionP[DeclaredMethod, CalleesLike]]
    )(
        eOptionP: SomeEPS
    ): ProperPropertyComputationResult = {
        val (_, newDependees, newDirectKeys, newIndirectKeys) =
            handleEOptP(
                eOptionP.asInstanceOf[EPS[DeclaredMethod, CalleesLike]],
                dependees,
                directKeys,
                indirectKeys
            )

        returnResult(
            declaredMethod,
            newDependees,
            newDirectKeys,
            newIndirectKeys,
            isReachable = true
        )
    }

    @inline def returnResult(
        declaredMethod: DeclaredMethod,
        dependees:      Set[EOptionP[DeclaredMethod, CalleesLike]],
        directKeys:     Set[PropertyKey[CalleesLike]],
        indirectKeys:   Set[PropertyKey[CalleesLike]],
        isReachable:    Boolean
    ): ProperPropertyComputationResult = {
        if (!isReachable) {
            assert(dependees.isEmpty)
            return Result(declaredMethod, NoCalleesDueToNotReachableMethod);
        }

        var directCalleeIds: IntMap[IntTrieSet] = IntMap.empty
        var indirectCalleeIds: IntMap[IntTrieSet] = IntMap.empty
        var incompleteCallSites: IntTrieSet = IntTrieSet.empty
        var indirectCallParameters: IntMap[Map[DeclaredMethod, Seq[Option[(ValueInformation, IntTrieSet)]]]] =
            IntMap.empty

        for (key ← directKeys.toIterator ++ indirectKeys.toIterator) {
            val p = propertyStore(declaredMethod, key)
            if (p.hasUBP) {
                val callees = p.ub
                if (callees.isIndirect) {
                    indirectCalleeIds =
                        indirectCalleeIds.unionWith(callees.callSites, (_, l, r) ⇒ l ++ r)
                    indirectCallParameters = indirectCallParameters.unionWith(
                        callees.asInstanceOf[IndirectCallees].parameters,
                        (_, l, r) ⇒
                            throw new UnknownError("Indirect callee derived by two analyses")
                    )
                } else {
                    directCalleeIds =
                        directCalleeIds.unionWith(callees.callSites, (_, l, r) ⇒ l ++ r)
                }
                incompleteCallSites ++!= callees.incompleteCallSites
            }
        }

        val ub = new ConcreteCallees(
            directCalleeIds,
            indirectCalleeIds,
            incompleteCallSites,
            indirectCallParameters
        )

        if (dependees.isEmpty) {
            Result(declaredMethod, ub)
        } else {
            InterimResult.forUB(
                declaredMethod,
                ub,
                dependees,
                continuation(declaredMethod, directKeys, indirectKeys, dependees)
            )
        }
    }

    @inline protected[this] def updateDependee(
        eOptionP:  EOptionP[DeclaredMethod, CalleesLike],
        dependees: Set[EOptionP[DeclaredMethod, CalleesLike]]
    ): Set[EOptionP[DeclaredMethod, CalleesLike]] = {
        val filtered = dependees.filter { d ⇒ d.e != eOptionP.e || d.pk != eOptionP.pk }
        if (eOptionP.isRefinable) filtered + eOptionP
        else filtered
    }

}

case class LazyCalleesAnalysis(
        calleesProperties: Set[CalleesLikePropertyMetaInformation]
) extends BasicFPCFLazyAnalysisScheduler {

    override def uses: Set[PropertyBounds] = calleesProperties.map(PropertyBounds.ub)

    override def derivesLazily: Some[PropertyBounds] = Some(PropertyBounds.ub(Callees))

    override def register(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val (indirectCalleesProperties, directCalleesProperties) =
            calleesProperties.partition(_.isIndirect)
        val analysis = new CalleesAnalysis(
            p,
            directCalleesProperties.map(_.key),
            indirectCalleesProperties.map(_.key)
        )
        ps.registerLazyPropertyComputation(Callees.key, analysis.doAnalysis)
        analysis
    }
}
