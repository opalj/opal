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

import org.opalj.collection.immutable.Chain
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.analyses.TriggeredSystemPropertiesAnalysis
import org.opalj.fpcf.ComputationSpecification
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FinalP
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.br.TestSupport.allBIProjects
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.cg.properties.Callees
import org.opalj.br.fpcf.cg.properties.CallersProperty
import org.opalj.br.fpcf.cg.properties.ReflectionRelatedCallees
import org.opalj.br.fpcf.cg.properties.SerializationRelatedCallees
import org.opalj.br.fpcf.cg.properties.StandardInvokeCallees
import org.opalj.br.fpcf.cg.properties.ThreadRelatedIncompleteCallSites
import org.opalj.br.fpcf.FPCFAnalysesManager
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.tac.fpcf.analyses.cg.reflection.TriggeredReflectionRelatedCallsAnalysis

case class CallSites(callSites: Set[CallSite])

case class CallSite(declaredTarget: Method, line: Int, method: Method, targets: Set[Method])

case class Method(name: String, declaringClass: String, returnType: String, parameterTypes: List[String])

@RunWith(classOf[JUnitRunner])
class RTAIntegrationTest extends FlatSpec with Matchers {

    behavior of "the rta call graph analysis on columbus"

    allBIProjects() foreach { biProject ⇒
        val (name, projectFactory) = biProject
        val project = projectFactory()

        checkProject(name, project)
    }

    def checkProject(projectName: String, project: SomeProject): Unit = {
        /*val project =
            Project(
                ClassFiles(locateTestResources("/classfiles/Flashcards 0.4 - target 1.6.jar", "bi")),
                //ClassFiles(locateTestResources("/classfiles/Columbus 2008_10_16 - target 1.5.jar", "bi")),
                Traversable.empty,
                libraryClassFilesAreInterfacesOnly = true
            )*/

        implicit val propertyStore: PropertyStore = project.get(PropertyStoreKey)
        implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

        val calleesAnalysis = LazyCalleesAnalysis(
            Set(
                StandardInvokeCallees,
                SerializationRelatedCallees,
                ReflectionRelatedCallees,
                ThreadRelatedIncompleteCallSites
            )
        )

        val manager: FPCFAnalysesManager = project.get(FPCFAnalysesManagerKey)
        manager.runAll(
            List(
                RTACallGraphAnalysisScheduler,
                TriggeredStaticInitializerAnalysis,
                TriggeredLoadedClassesAnalysis,
                TriggeredFinalizerAnalysisScheduler,
                TriggeredThreadRelatedCallsAnalysis,
                TriggeredSerializationRelatedCallsAnalysis,
                TriggeredReflectionRelatedCallsAnalysis,
                TriggeredSystemPropertiesAnalysis,
                TriggeredConfiguredNativeMethodsAnalysis,
                TriggeredInstantiatedTypesAnalysis,
                LazyL0TACAIAnalysis,
                calleesAnalysis
            ),
            { css: Chain[ComputationSpecification[FPCFAnalysis]] ⇒
                if (css.contains(calleesAnalysis)) {
                    declaredMethods.declaredMethods.foreach { dm ⇒
                        propertyStore.force(dm, Callees.key)
                    }
                }
            }
        )

        it should s"have matching callers and callees in $projectName" in {
            checkBidirectionCallerCallee
        }
    }

    // TODO: In the current version, we can not compare the CG to the one from SOOT
    /*it should "consists of calls that are also present in Soots CHA" in {
        //val callSites = retrieveCallSites("/columbus1_5_SOOT_CHA.json")
        val callSites = retrieveCallSites("/flashchards_SOOT_CHA_MODIFIED.json")

        for {
            m ← project.allMethodsWithBody
            dm = declaredMethods(m)
            computedCallees = propertyStore(dm, Callees.key).asFinal.p
            (pc, computedTargets) ← computedCallees.directCallSites()
        } {
            val body = m.body.get
            val instr = body.instructions(pc).asMethodInvocationInstruction
            val declaredMethod = convertInstr(instr)

            val line = body.lineNumber(pc).get

            val overApproximatedCallSites = callSites.callSites.filter {
                case CallSite(dt, l, caller, _) ⇒
                    l == line &&
                        caller == convertMethod(dm) &&
                        dt.name == declaredMethod.name &&
                        dt.parameterTypes == declaredMethod.parameterTypes &&
                        dt.returnType == declaredMethod.returnType
            }
            assert(overApproximatedCallSites.nonEmpty)

            val overApproximatedTgts = overApproximatedCallSites.flatMap(_.targets)
            computedTargets.foreach { computedTgt ⇒
                assert(overApproximatedTgts.contains(convertMethod(computedTgt)))
            }

        }
    }

    it should "contain all calls from Soots SPARK" in {
        val callSites = retrieveCallSites("/flashchards_SOOT_SPARK_MODIFIED.json").callSites
        for {
            m ← project.allMethodsWithBody
            dm = declaredMethods(m)
            methodRepresentation = convertMethod(dm)
            FinalP(computedCallees) = propertyStore(dm, Callees.key).asFinal
            CallSite(declaredTgt, line, _, tgts) ← callSites.filter(_.method == methodRepresentation)
            computedCallSites = computedCallees.directCallSites().filter {
                case (pc, computedTgt) ⇒
                    m.body.get.lineNumber(pc).get == line &&
                        computedTgt.nonEmpty && computedTgt.next.name == declaredTgt.name // todo
                // also use retType + paramTypes
            }.toList
            tgt ← tgts
        } {
            val containsCall = computedCallSites.exists(cs ⇒ cs._2.exists(computedTgt ⇒ convertMethod(computedTgt) == tgt))
            assert(
                containsCall,
                s"missed call $line: ${tgt.returnType} ${tgt.name} ${tgt.parameterTypes} \n in: \n\t${dm.declaringClassType} ${dm.definedMethod} \nto \n\t${tgt.declaringClass} \ncomputed calls: \n\t $computedCallSites"
            )
        }
    }*/

    def checkBidirectionCallerCallee(
        implicit
        propertyStore: PropertyStore,
        declaredMethods: DeclaredMethods
    ): Unit = {
        for {
            FinalEP(dm: DeclaredMethod, callees) ← propertyStore.entities(Callees.key).map(_.asFinal)
            (pc, tgts) ← callees.callSites()
            callee ← tgts
        } {
            val FinalP(callersProperty) = propertyStore(callee, CallersProperty.key).asFinal
            assert(callersProperty.callers.toSet.contains(dm → pc))
        }

        for {
            FinalEP(dm: DeclaredMethod, callers) ← propertyStore.entities(CallersProperty.key).map(_.asFinal)
            (caller, pc) ← callers.callers
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
