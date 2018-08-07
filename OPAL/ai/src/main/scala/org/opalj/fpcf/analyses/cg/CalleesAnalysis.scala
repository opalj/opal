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
import org.opalj.fpcf.properties.Callees
import org.opalj.fpcf.properties.CalleesImplementation
import org.opalj.fpcf.properties.CalleesLike
import org.opalj.fpcf.properties.CalleesLikePropertyMetaInformation
import org.opalj.fpcf.properties.LowerBoundCallees

import scala.collection.immutable.IntMap

// todo the callees property could be collaborative (compute the complete set of callees on demand)
class CalleesAnalysis private[analyses] (
        final val project:   SomeProject,
        calleesPropertyKeys: Set[PropertyKey[CalleesLike]]
) extends FPCFAnalysis {

    implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    def doAnalysis(dm: DeclaredMethod): PropertyComputationResult = {
        var dependees = Set.empty[EOptionP[DeclaredMethod, CalleesLike]]
        var allCallees: IntMap[IntTrieSet] = IntMap.empty[IntTrieSet]

        for (pk ← calleesPropertyKeys) {
            val r = handleEOptP(propertyStore(dm, pk), dependees, allCallees)
            dependees = r._1
            allCallees = r._2
        }

        returnResult(dm, dependees, allCallees)

    }

    @inline def handleEOptP(
        eOptionP:   EOptionP[DeclaredMethod, CalleesLike],
        dependees:  Set[EOptionP[DeclaredMethod, CalleesLike]],
        allCallees: IntMap[IntTrieSet]
    ): (Set[EOptionP[DeclaredMethod, CalleesLike]], IntMap[IntTrieSet]) = {
        var resDependees = dependees
        var resAllCallees = allCallees
        eOptionP match {
            case ep @ FinalEP(_, callees) ⇒
                resDependees = removeDependee(ep, dependees)
                resAllCallees = updateCallees(callees, resAllCallees)

            case ep @ EPS(_, _, callees) ⇒
                resDependees = updateDependee(ep, dependees)
                resAllCallees = updateCallees(callees, resAllCallees)
            case epk: EPK[DeclaredMethod, CalleesLike] ⇒
                resDependees = updateDependee(epk, dependees)
        }
        (resDependees, resAllCallees)
    }

    def continuation(
        declaredMethod: DeclaredMethod,
        dependees:      Set[EOptionP[DeclaredMethod, CalleesLike]],
        allCallees:     IntMap[IntTrieSet]
    )(eOptionP: SomeEPS): PropertyComputationResult = {
        val (newDependees, newAllCallees) = handleEOptP(
            eOptionP.asInstanceOf[EPS[DeclaredMethod, CalleesLike]], dependees, allCallees
        )
        returnResult(declaredMethod, newDependees, newAllCallees)
    }

    @inline def returnResult(
        declaredMethod: DeclaredMethod,
        dependees:      Set[EOptionP[DeclaredMethod, CalleesLike]],
        allCallees:     IntMap[IntTrieSet]
    ): PropertyComputationResult = {
        val callees = new CalleesImplementation(allCallees)
        if (dependees.isEmpty) {
            Result(declaredMethod, callees)
        } else {
            IntermediateResult(
                declaredMethod,
                LowerBoundCallees,
                callees,
                dependees,
                continuation(declaredMethod, dependees, allCallees)
            )
        }
    }

    @inline def removeDependee(
        eOptionP:  EOptionP[DeclaredMethod, CalleesLike],
        dependees: Set[EOptionP[DeclaredMethod, CalleesLike]]
    ): Set[EOptionP[DeclaredMethod, CalleesLike]] = {
        val resDependees = dependees.filter { dependee ⇒
            dependee.e != eOptionP.e && dependee.pk != eOptionP.pk
        }
        resDependees
    }

    @inline def updateDependee(
        eOptionP:  EOptionP[DeclaredMethod, CalleesLike],
        dependees: Set[EOptionP[DeclaredMethod, CalleesLike]]
    ): Set[EOptionP[DeclaredMethod, CalleesLike]] = {
        var resDependees = removeDependee(eOptionP, dependees)
        resDependees += eOptionP
        resDependees
    }

    // todo: if this method is called from the continuation, this is incredibly slow
    // IMPROVE: get IntTrieSet from CalleesLike
    @inline def updateCallees(callees: CalleesLike, allCallees: IntMap[IntTrieSet]): IntMap[IntTrieSet] = {
        var resAllCallees = allCallees
        for ((pc, tgts) ← callees.encodedCallees) {
            val old = resAllCallees.getOrElse(pc, IntTrieSet.empty)
            resAllCallees = resAllCallees.updated(pc, old ++ tgts)
        }

        resAllCallees
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
        val analysis = new CalleesAnalysis(project, calleesProperties.map(_.key))
        propertyStore.registerLazyPropertyComputation(Callees.key, analysis.doAnalysis)
        analysis
    }

    override def afterPhaseCompletion(p: SomeProject, ps: PropertyStore): Unit = {}
}

