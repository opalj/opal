/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner
import play.api.libs.json.Json
import play.api.libs.json.JsSuccess
import play.api.libs.json.Reads

import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.PropertyStore
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.br.TestSupport.allBIProjects
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.cg.properties.Callees
import org.opalj.br.fpcf.cg.properties.Callers
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.tac.cg.RTACallGraphKey

case class CallSites(callSites: Set[CallSite])

case class CallSite(declaredTarget: Method, line: Int, method: Method, targets: Set[Method])

case class Method(name: String, declaringClass: String, returnType: String, parameterTypes: List[String])

@RunWith(classOf[JUnitRunner]) // TODO: We should use JCG for some basic tests
class RTAIntegrationTest extends FlatSpec with Matchers {

    behavior of "the rta call graph analysis on columbus"

    allBIProjects() foreach { biProject ⇒
        val (name, projectFactory) = biProject
        val project = projectFactory()

        checkProject(name, project)
    }

    def checkProject(projectName: String, project: SomeProject): Unit = {
        implicit val propertyStore: PropertyStore = project.get(PropertyStoreKey)
        implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

        project.get(RTACallGraphKey)

        it should s"have matching callers and callees in $projectName" in {
            checkBidirectionCallerCallee
        }
    }

    def checkBidirectionCallerCallee(
        implicit
        propertyStore:   PropertyStore,
        declaredMethods: DeclaredMethods
    ): Unit = {
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

    def convertMethod(dm: DeclaredMethod): Method = {
        if (dm.hasSingleDefinedMethod)
            assert(dm.declaringClassType eq dm.definedMethod.classFile.thisType)

        val name = dm.name
        val declaringClass = dm.declaringClassType.toJVMTypeName
        val returnType = dm.descriptor.returnType.toJVMTypeName
        val parameterTypes = dm.descriptor.parameterTypes.map(_.toJVMTypeName).toList

        Method(name, declaringClass, returnType, parameterTypes)
    }

    def convertInstr(instr: MethodInvocationInstruction): Method = {
        val name = instr.name
        val declaringClass = instr.declaringClass.toJVMTypeName
        val descriptor = instr.methodDescriptor
        val returnType = descriptor.returnType.toJVMTypeName
        val parameterTypes = descriptor.parameterTypes.map(_.toJVMTypeName).toList

        Method(name, declaringClass, returnType, parameterTypes)
    }

    def retrieveCallSites(jsonPath: String): CallSites = {
        val jsValue = Json.parse(getClass.getResourceAsStream(jsonPath))

        implicit val methodReads: Reads[Method] = Json.reads[Method]
        implicit val callSiteReads: Reads[CallSite] = Json.reads[CallSite]
        implicit val callSitesReads: Reads[CallSites] = Json.reads[CallSites]
        jsValue.validate[CallSite]
        jsValue.validate[Method]
        val jsResult = jsValue.validate[CallSites]
        jsResult match {
            case _: JsSuccess[CallSites] ⇒
                jsResult.get

            case _ ⇒
                throw new IllegalArgumentException("invalid json file")
        }

    }

}
