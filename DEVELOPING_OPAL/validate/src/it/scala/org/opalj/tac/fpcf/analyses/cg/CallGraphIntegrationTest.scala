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
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.tac.fpcf.properties.cg.Callees
import org.opalj.tac.fpcf.properties.cg.Callers
import org.opalj.br.TestSupport.allBIProjects
import org.opalj.br.analyses.cg.ClassExtensibilityKey
import org.opalj.br.analyses.cg.ClosedPackagesKey
import org.opalj.br.analyses.cg.IsOverridableMethodKey
import org.opalj.br.analyses.cg.TypeExtensibilityKey
import org.opalj.br.analyses.Project
import org.opalj.tac.fpcf.properties.cg.NoCallers
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.tac.cg.CallGraph
import org.opalj.tac.cg.CHACallGraphKey
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.cg.TypeBasedPointsToCallGraphKey
import org.opalj.tac.cg.TypeProviderKey

@RunWith(classOf[JUnitRunner]) // TODO: We should use JCG for some basic tests
class CallGraphIntegrationTest extends AnyFlatSpec with Matchers {

    // These projects have millions of CG edges, ignore them to keep test times reasonable
    val ignoredProjects = List(
        "lecturedoc_2.10-0.0.0-one-jar.jar",
        "OPAL-MultiJar-SNAPSHOT-01-04-2018-dependencies",
        "scala-2.12.4"
    )

    allBIProjects(
        config = ConfigFactory.load("CommandLineProject.conf")
    ) foreach { biProject =>
            val (name, projectFactory) = biProject
            if (!ignoredProjects.contains(name))
                checkProject(name, projectFactory)
        }

    def checkProject(projectName: String, projectFactory: () => Project[URL]): Unit = {

        behavior of s"the call graph analyses on $projectName"

        var project: Project[URL] = null
        var cha: CallGraph = null
        var chaPS: PropertyStore = null
        var rtaProject: Project[URL] = null
        var rta: CallGraph = null
        var rtaPS: PropertyStore = null
        var pointsToProject: Project[URL] = null
        var pointsTo: CallGraph = null

        it should s"have matching callers and callees for CHA" in {
            project = projectFactory()
            chaPS = project.get(PropertyStoreKey)
            cha = project.get(CHACallGraphKey)

            checkBidirectionCallerCallee(chaPS)(project.get(TypeProviderKey))
        }

        it should s"have matching callers and callees for RTA" in {
            rtaProject = project.recreate {
                case DeclaredMethodsKey.uniqueId | IsOverridableMethodKey.uniqueId |
                    TypeExtensibilityKey.uniqueId | ClosedPackagesKey.uniqueId |
                    ClassExtensibilityKey.uniqueId => true
                case _ => false
            }
            rtaPS = rtaProject.get(PropertyStoreKey)
            rta = rtaProject.get(RTACallGraphKey)

            checkBidirectionCallerCallee(rtaPS)(rtaProject.get(TypeProviderKey))
        }

        it should s"have RTA more precise than CHA" in {
            val lessPreciseTypeProvider = project.get(TypeProviderKey)
            val morePreciseTypeProvider = rtaProject.get(TypeProviderKey)
            checkMorePrecise(cha, rta, chaPS, morePreciseTypeProvider, lessPreciseTypeProvider)
        }

        project = null
        cha = null
        chaPS = null

        it should s"have matching callers and callees for PointsTo" in {
            pointsToProject = rtaProject.recreate {
                case DeclaredMethodsKey.uniqueId | IsOverridableMethodKey.uniqueId |
                    TypeExtensibilityKey.uniqueId | ClosedPackagesKey.uniqueId |
                    ClassExtensibilityKey.uniqueId => true
                case _ => false
            }
            val pointsToPS = pointsToProject.get(PropertyStoreKey)
            pointsTo = pointsToProject.get(TypeBasedPointsToCallGraphKey)

            checkBidirectionCallerCallee(pointsToPS)(pointsToProject.get(TypeProviderKey))
        }

        it should s"have PointsTo more precise than RTA" in {
            val lessPreciseTypeProvider = rtaProject.get(TypeProviderKey)
            val morePreciseTypeProvider = pointsToProject.get(TypeProviderKey)
            checkMorePrecise(rta, pointsTo, rtaPS, morePreciseTypeProvider, lessPreciseTypeProvider)
        }
    }

    def checkMorePrecise(
        lessPreciseCG:           CallGraph,
        morePreciseCG:           CallGraph,
        lessPreciseCGPS:         PropertyStore,
        morePreciseTypeProvider: TypeProvider,
        lessPreciseTypeProvider: TypeProvider
    ): Unit = {
        var unexpectedCalls: List[UnexpectedCallTarget] = Nil
        morePreciseCG.reachableMethods().foreach { context =>
            val method = context.method
            val callersLPCG = lessPreciseCG.callersPropertyOf(method)
            val callersMPCG = morePreciseCG.callersPropertyOf(method)
            if ((callersLPCG eq NoCallers) &&
                !callersMPCG.callers(method)(morePreciseTypeProvider).iterator.forall {
                    callSite =>
                        val callees = lessPreciseCG.calleesPropertyOf(callSite._1)
                        !callSite._3 &&
                            callees.isIncompleteCallSite(context, callSite._2)(lessPreciseCGPS)
                }) {
                unexpectedCalls ::= UnexpectedCallTarget(null, method, -1)
            } else if (callersMPCG.hasVMLevelCallers && !callersLPCG.hasVMLevelCallers) {
                unexpectedCalls ::= UnexpectedCallTarget(null, method, -1)
            }
            val allCalleesMPCG = morePreciseCG.calleesOf(method).toSet
            for {
                (pc, calleesMPCG) <- allCalleesMPCG
                calleesLPCG = lessPreciseCG.calleesPropertyOf(method)
                if !calleesLPCG.isIncompleteCallSite(context, pc)(lessPreciseCGPS)
                allCalleesLPCG = calleesLPCG.callees(context, pc)(lessPreciseCGPS, lessPreciseTypeProvider).toSet
                calleeMPCG <- calleesMPCG
                if !allCalleesLPCG(calleeMPCG)
            } {
                unexpectedCalls ::= UnexpectedCallTarget(method, calleeMPCG.method, pc)
            }
        }

        val hasUnexpectedCalls = unexpectedCalls.nonEmpty
        assert(!hasUnexpectedCalls, s"found unexpected calls:\n${unexpectedCalls.mkString("\n")}")
    }

    case class UnexpectedCallTarget(caller: DeclaredMethod, callee: DeclaredMethod, pc: Int)

    def checkBidirectionCallerCallee(
        propertyStore: PropertyStore
    )(implicit typeProvider: TypeProvider): Unit = {
        implicit val ps: PropertyStore = propertyStore
        for {
            FinalEP(dm: DeclaredMethod, callees) <- propertyStore.entities(Callees.key).map(_.asFinal)
            context <- callees.callerContexts
            (pc, tgts) <- callees.callSites(context)
            callee <- tgts
        } {
            val FinalP(callersProperty) = propertyStore(callee.method, Callers.key).asFinal
            assert(callersProperty.callers(dm).iterator.map(caller => (caller._1, caller._2)).iterator.to(Set).contains(dm -> pc))
        }

        for {
            FinalEP(dm: DeclaredMethod, callers) <- propertyStore.entities(Callers.key).map(_.asFinal)
            (callee, caller, pc, _) <- callers.callContexts(dm).iterator
            if caller.hasContext
        } {
            val FinalP(calleesProperty) = propertyStore(caller.method, Callees.key).asFinal
            assert(calleesProperty.callees(caller, pc).contains(callee))
        }
    }

}
