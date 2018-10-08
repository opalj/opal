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
import org.opalj.fpcf.cg.properties.IntermediateCallees
import org.opalj.fpcf.cg.properties.NoCalleesDueToNotReachableMethod
import org.opalj.fpcf.cg.properties.FinalCallees
import org.opalj.fpcf.cg.properties.IndirectCallees
import org.opalj.value.KnownTypedValue

import scala.collection.immutable.IntMap

// todo the callees property could be collaborative (compute the complete set of callees on demand)
/**
 *
 * @author Florian Kuebler
 * @author Dominik Helm
 */
class CalleesAnalysis private[analyses] (
        final val project:           SomeProject,
        directCalleesPropertyKeys:   Set[PropertyKey[CalleesLike]],
        indirectCalleesPropertyKeys: Set[PropertyKey[CalleesLike]]
) extends FPCFAnalysis {

    implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    def doAnalysis(dm: DeclaredMethod): PropertyComputationResult = {
        var dependees: Set[EOptionP[DeclaredMethod, CalleesLike]] = Set.empty
        var isReachable = false
        var directKeys = directCalleesPropertyKeys
        var indirectKeys = indirectCalleesPropertyKeys

        for (pk ← directCalleesPropertyKeys.iterator ++ indirectCalleesPropertyKeys.iterator) {
            val r = handleEOptP(propertyStore(dm, pk), dependees, directKeys, indirectKeys)
            isReachable |= r._1
            dependees = r._2
            directKeys = r._3
            indirectKeys = r._4
        }

        returnResult(dm, dependees, directKeys, indirectKeys, isReachable)

    }

    @inline def handleEOptP(
        eOptionP:     EOptionP[DeclaredMethod, CalleesLike],
        dependees:    Set[EOptionP[DeclaredMethod, CalleesLike]],
        directKeys:   Set[PropertyKey[CalleesLike]],
        indirectKeys: Set[PropertyKey[CalleesLike]]
    ): (Boolean, Set[EOptionP[DeclaredMethod, CalleesLike]], Set[PropertyKey[CalleesLike]], Set[PropertyKey[CalleesLike]]) = {
        eOptionP match {
            case ep @ FinalEP(_, p: CalleesLikeNotReachable) ⇒
                if (p.isIndirect)
                    (false, updateDependee(ep, dependees), directKeys, indirectKeys - p.key)
                else
                    (false, updateDependee(ep, dependees), directKeys - p.key, indirectKeys)

            case IntermediateESimpleP(_, _: CalleesLikeNotReachable) ⇒
                throw new IllegalArgumentException("non reachable methods must have final property")

            case ep: EPS[_, _] ⇒
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
    )(eOptionP: SomeEPS): PropertyComputationResult = {
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
    ): PropertyComputationResult = {
        if (!isReachable) {
            assert(dependees.isEmpty)
            return Result(declaredMethod, NoCalleesDueToNotReachableMethod);
        }

        if (dependees.isEmpty) {

            var directCalleeIds: IntMap[IntTrieSet] = IntMap.empty
            var indirectCalleeIds: IntMap[IntTrieSet] = IntMap.empty
            var incompleteCallSites: IntTrieSet = IntTrieSet.empty
            var indirectCallParameters: IntMap[Map[DeclaredMethod, Seq[Option[(KnownTypedValue, IntTrieSet)]]]] =
                IntMap.empty

            for (key ← directKeys.toIterator ++ indirectKeys.toIterator) {
                val p1 = propertyStore(
                    declaredMethod,
                    key
                )
                if (!p1.isInstanceOf[FinalEP[DeclaredMethod, CalleesLike]]) {
                    propertyStore(
                        declaredMethod,
                        key
                    )
                }
                val p = p1.asInstanceOf[FinalEP[DeclaredMethod, CalleesLike]].p
                if (p.isIndirect) {
                    indirectCalleeIds =
                        indirectCalleeIds.unionWith(p.callSites, (_, l, r) ⇒ l ++ r)
                    indirectCallParameters = indirectCallParameters.unionWith(
                        p.asInstanceOf[IndirectCallees].parameters,
                        (_, l, r) ⇒
                            throw new UnknownError("Indirect callee derived by two analyses")
                    )
                } else {
                    directCalleeIds = directCalleeIds.unionWith(p.callSites, (_, l, r) ⇒ l ++ r)
                }
                incompleteCallSites ++!= p.incompleteCallSites
            }

            Result(
                declaredMethod,
                new FinalCallees(
                    directCalleeIds,
                    indirectCalleeIds,
                    incompleteCallSites,
                    indirectCallParameters
                )
            )
        } else {
            SimplePIntermediateResult(
                declaredMethod,
                new IntermediateCallees(
                    declaredMethod,
                    directKeys,
                    indirectKeys.asInstanceOf[Set[PropertyKey[IndirectCallees]]]
                ),
                dependees,
                continuation(declaredMethod, directKeys, indirectKeys, dependees)
            )
        }
    }

    @inline def updateDependee(
        eOptionP:  EOptionP[DeclaredMethod, CalleesLike],
        dependees: Set[EOptionP[DeclaredMethod, CalleesLike]]
    ): Set[EOptionP[DeclaredMethod, CalleesLike]] = {
        val filtered = dependees.filter { d ⇒ d.e != eOptionP.e && d.pk != eOptionP.pk }
        if (eOptionP.isRefinable) filtered + eOptionP
        else filtered
    }

}

class LazyCalleesAnalysis(calleesProperties: Set[CalleesLikePropertyMetaInformation]) extends FPCFLazyAnalysisScheduler {

    override type InitializationData = Null

    override def uses: Set[PropertyKind] = calleesProperties.asInstanceOf[Set[PropertyKind]]

    override def derives: Set[PropertyKind] = Set(Callees)

    override def init(p: SomeProject, ps: PropertyStore): Null = { null }

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def startLazily(
        project: SomeProject, propertyStore: PropertyStore, unused: Null
    ): FPCFAnalysis = {
        val (indirectCalleesProperties, directCalleesProperties) =
            calleesProperties.partition(_.isIndirect)
        val analysis = new CalleesAnalysis(
            project,
            directCalleesProperties.map(_.key),
            indirectCalleesProperties.map(_.key)
        )
        propertyStore.registerLazyPropertyComputation(Callees.key, analysis.doAnalysis)
        analysis
    }

    override def afterPhaseCompletion(p: SomeProject, ps: PropertyStore): Unit = {}
}

