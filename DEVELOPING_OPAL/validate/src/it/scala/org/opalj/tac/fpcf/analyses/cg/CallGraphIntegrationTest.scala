/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

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
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.TestSupport.allBIProjects
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.properties.cg.NoCallers
import org.opalj.tac.cg.CallGraph
import org.opalj.tac.cg.CHACallGraphKey
import org.opalj.tac.cg.RTACallGraphKey

@RunWith(classOf[JUnitRunner]) // TODO: We should use JCG for some basic tests
class CallGraphIntegrationTest extends AnyFlatSpec with Matchers {

    /*allBIProjects() foreach { biProject ⇒
        val (name, projectFactory) = biProject
        val project = projectFactory()

        checkProject(
            name,
            Project.recreate(project, ConfigFactory.load("LibraryProject.conf"))
        )
    }*/

    def checkProject(projectName: String, project: SomeProject): Unit = {

        implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

        val chaProject = project.recreate {
            case DeclaredMethodsKey.uniqueId ⇒ true
            case _                           ⇒ false
        }
        val chaPS = chaProject.get(PropertyStoreKey)

        val rtaProject = project.recreate {
            case DeclaredMethodsKey.uniqueId ⇒ true
            case _                           ⇒ false
        }
        val rtaPS = rtaProject.get(PropertyStoreKey)

        /*val pointsToProject = project.recreate {
            case DeclaredMethodsKey.uniqueId ⇒ true
            case _                           ⇒ false
        }
        val pointsToPS = pointsToProject.get(PropertyStoreKey)
*/
        val cha = chaProject.get(CHACallGraphKey)
        val rta = rtaProject.get(RTACallGraphKey)
        //val pointsTo = pointsToProject.get(PointsToCallGraphKey)

        behavior of s"the CHA call graph analysis on $projectName"
        it should s"have matching callers and callees" in {
            checkBidirectionCallerCallee(chaPS)
        }

        behavior of s"the RTA call graph analysis on $projectName"
        it should s"have matching callers and callees" in {
            checkBidirectionCallerCallee(rtaPS)
        }
        it should s"be more precise than the CHA" in {
            checkMorePrecise(cha, rta)
        }

        /*behavior of s"the points-to call graph analysis on $projectName"
        it should s"have matching callers and callees" in {
            checkBidirectionCallerCallee(pointsToPS)
        }
        it should s"be more precise than the CHA" in {
            checkMorePrecise(cha, pointsTo)
        }
        it should s"be more precise than the RTA" in {
            checkMorePrecise(rta, pointsTo)
        }*/

    }

    def checkMorePrecise(
        lessPreciseCG: CallGraph, morePreciseCG: CallGraph
    ): Unit = {
        var unexpectedCalls: List[UnexpectedCallTarget] = Nil
        morePreciseCG.reachableMethods().foreach { method ⇒
            if (lessPreciseCG.callersPropertyOf(method) eq NoCallers)
                unexpectedCalls ::= UnexpectedCallTarget(method, null, -1)
            val allCalleesMPCG = morePreciseCG.calleesOf(method)
            for {
                (pc, calleesMPCG) ← allCalleesMPCG
                calleesLPCG = lessPreciseCG.calleesOf(method, pc).toSet
                calleeMPCG ← calleesMPCG
                if !calleesLPCG.contains(calleeMPCG)
            } {
                unexpectedCalls ::= UnexpectedCallTarget(method, calleeMPCG, pc)
            }
        }

        // todo: better output
        assert(unexpectedCalls.isEmpty, s"found unexpected calls:\n${unexpectedCalls.mkString("\n")}")
    }

    case class UnexpectedCallTarget(caller: DeclaredMethod, callee: DeclaredMethod, pc: Int)

    def checkBidirectionCallerCallee(
        propertyStore: PropertyStore
    )(implicit declaredMethods: DeclaredMethods): Unit = {
        implicit val ps = propertyStore
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
