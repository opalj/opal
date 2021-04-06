/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import java.net.URL

import com.typesafe.config.ConfigFactory
import org.junit.runner.RunWith
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.PropertyStore
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.TestSupport.allBIProjects
import org.opalj.br.analyses.cg.ClassExtensibilityKey
import org.opalj.br.analyses.cg.ClosedPackagesKey
import org.opalj.br.analyses.cg.IsOverridableMethodKey
import org.opalj.br.analyses.cg.TypeExtensibilityKey
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.properties.cg.NoCallers
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.tac.cg.AllocationSiteBasedPointsToCallGraphKey
import org.opalj.tac.cg.CallGraph
import org.opalj.tac.cg.CHACallGraphKey
import org.opalj.tac.cg.RTACallGraphKey

@RunWith(classOf[JUnitRunner]) // TODO: We should use JCG for some basic tests
class CallGraphIntegrationTest extends AnyFlatSpec with Matchers {

    // These projects have millions of CG edges, ignore them to keep test times reasonable
    val ignoredProjects = List(
        "lecturedoc_2.10-0.0.0-one-jar.jar",
        "OPAL-MultiJar-SNAPSHOT-01-04-2018-dependencies",
        "scala-2.12.4"
    )

    allBIProjects(
        config = ConfigFactory.load("LibraryProject.conf")
    ) foreach { biProject ⇒
            val (name, projectFactory) = biProject
            if (!ignoredProjects.contains(name))
                checkProject(name, projectFactory)
        }

    def checkProject(projectName: String, projectFactory: () ⇒ Project[URL]): Unit = {

        behavior of s"the call graph analyses on $projectName"

        var project: Project[URL] = null
        var declaredMethods: DeclaredMethods = null
        var cha: CallGraph = null
        var chaPS: PropertyStore = null
        var rta: CallGraph = null
        var rtaPS: PropertyStore = null
        var pointsTo: CallGraph = null

        it should s"have matching callers and callees for CHA" in {
            project = projectFactory()
            declaredMethods = project.get(DeclaredMethodsKey)
            chaPS = project.get(PropertyStoreKey)
            cha = project.get(CHACallGraphKey)

            checkBidirectionCallerCallee(chaPS)(declaredMethods)
        }

        it should s"have matching callers and callees for RTA" in {
            val rtaProject = project.recreate {
                case DeclaredMethodsKey.uniqueId | IsOverridableMethodKey.uniqueId |
                    TypeExtensibilityKey.uniqueId | ClosedPackagesKey.uniqueId |
                    ClassExtensibilityKey.uniqueId ⇒ true
                case _ ⇒ false
            }
            rtaPS = rtaProject.get(PropertyStoreKey)
            rta = rtaProject.get(RTACallGraphKey)

            checkBidirectionCallerCallee(rtaPS)(declaredMethods)
        }
        it should s"have RTA more precise than CHA" in {
            checkMorePrecise(cha, rta, chaPS, declaredMethods)
        }

        it should s"have matching callers and callees for PointsTo" in {
            val pointsToProject = project.recreate {
                case DeclaredMethodsKey.uniqueId | IsOverridableMethodKey.uniqueId |
                    TypeExtensibilityKey.uniqueId | ClosedPackagesKey.uniqueId |
                    ClassExtensibilityKey.uniqueId ⇒ true
                case _ ⇒ false
            }
            val pointsToPS = pointsToProject.get(PropertyStoreKey)
            pointsTo = pointsToProject.get(AllocationSiteBasedPointsToCallGraphKey)

            checkBidirectionCallerCallee(pointsToPS)(declaredMethods)
        }
        // FIXME This is currently not the case, e.g. we don't have a non-pointsTo DoPrivileged analysis
        ignore should s"have PointsTo more precise than RTA" in {
            checkMorePrecise(rta, pointsTo, rtaPS, declaredMethods)
        }
    }

    def checkMorePrecise(
        lessPreciseCG:   CallGraph,
        morePreciseCG:   CallGraph,
        lessPreciseCGPS: PropertyStore,
        declaredMethods: DeclaredMethods
    ): Unit = {
        var unexpectedCalls: List[UnexpectedCallTarget] = Nil
        morePreciseCG.reachableMethods().foreach { method ⇒
            val callersLPCG = lessPreciseCG.callersPropertyOf(method)
            val callersMPCG = morePreciseCG.callersPropertyOf(method)
            if ((callersLPCG eq NoCallers) ||
                callersMPCG.hasVMLevelCallers && !callersLPCG.hasVMLevelCallers)
                unexpectedCalls ::= UnexpectedCallTarget(method, null, -1)
            val allCalleesMPCG = morePreciseCG.calleesOf(method)
            for {
                (pc, calleesMPCG) ← allCalleesMPCG
                calleesLPCG = lessPreciseCG.calleesPropertyOf(method)
                if !calleesLPCG.isIncompleteCallSite(pc)(lessPreciseCGPS)
                allCalleesLPCG = calleesLPCG.callees(pc)(lessPreciseCGPS, declaredMethods).toSet
                calleeMPCG ← calleesMPCG
                if !allCalleesLPCG(calleeMPCG)
            } {
                unexpectedCalls ::= UnexpectedCallTarget(method, calleeMPCG, pc)
            }
        }

        val hasUnexpectedCalls = unexpectedCalls.nonEmpty
        assert(!hasUnexpectedCalls, s"found unexpected calls:\n${unexpectedCalls.mkString("\n")}")
    }

    case class UnexpectedCallTarget(caller: DeclaredMethod, callee: DeclaredMethod, pc: Int)

    def checkBidirectionCallerCallee(
        propertyStore: PropertyStore
    )(implicit declaredMethods: DeclaredMethods): Unit = {
        implicit val ps: PropertyStore = propertyStore
        for {
            FinalEP(dm: DeclaredMethod, callees) ← propertyStore.entities(Callees.key).map(_.asFinal)
            (pc, tgts) ← callees.callSites()
            callee ← tgts
        } {
            val FinalP(callersProperty) = propertyStore(callee, Callers.key).asFinal
            assert(callersProperty.callers.map(caller ⇒ (caller._1, caller._2)).toSet.contains(dm → pc))
        }

        for {
            FinalEP(dm: DeclaredMethod, callers) ← propertyStore.entities(Callers.key).map(_.asFinal)
            (caller, pc, _) ← callers.callers
        } {
            val FinalP(calleesProperty) = propertyStore(caller, Callees.key).asFinal
            assert(calleesProperty.callees(pc).contains(dm))
        }
    }

}
