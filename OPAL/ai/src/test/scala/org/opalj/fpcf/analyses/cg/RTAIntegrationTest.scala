/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses
package cg

import org.junit.runner.RunWith
import org.opalj.bi.TestResources.locateTestResources
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.br.reader.Java8Framework.ClassFiles
import org.opalj.fpcf.properties.Callees
import org.opalj.fpcf.properties.CallersProperty
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner
import play.api.libs.json.JsSuccess
import play.api.libs.json.Json
import play.api.libs.json.Reads

case class CallSites(callSites: Set[CallSite])

case class CallSite(declaredTarget: Method, line: Int, method: Method, targets: Set[Method])

case class Method(name: String, declaringClass: String, returnType: String, parameterTypes: List[String])

@RunWith(classOf[JUnitRunner])
class RTAIntegrationTest extends FlatSpec with Matchers {

    behavior of "the rta call graph analysis on columbus"

    val columbusProject =
        Project(
            ClassFiles(locateTestResources("/classfiles/Flashcards 0.4 - target 1.6.jar", "bi")),
            //ClassFiles(locateTestResources("/classfiles/Columbus 2008_10_16 - target 1.5.jar", "bi")),
            Traversable.empty,
            libraryClassFilesAreInterfacesOnly = true
        )

    /*columbusProject.getOrCreateProjectInformationKeyInitializationData(
        PropertyStoreKey,
        (context: List[PropertyStoreContext[AnyRef]]) ⇒ {
            val ps = PKEParallelTasksPropertyStore.create(
                new RecordAllPropertyStoreTracer,
                context.iterator.map(_.asTuple).toMap
            )(columbusProject.logContext)
            PropertyStore.updateDebug(true)
            ps
        }
    )*/
    val propertyStore: PropertyStore = columbusProject.get(PropertyStoreKey)
    //PropertyStore.updateDebug(true)

    val manager: FPCFAnalysesManager = columbusProject.get(FPCFAnalysesManagerKey)
    /*val propertyStore = */ manager.runAll(
        EagerRTACallGraphAnalysisScheduler,
        EagerLoadedClassesAnalysis,
        EagerFinalizerAnalysisScheduler
    )
    implicit val declaredMethods: DeclaredMethods = columbusProject.get(DeclaredMethodsKey)

    it should "have matching callers and callees" in {
        checkBidirectionCallerCallee(propertyStore)
    }

    it should "consists of calls that are also present in Soots CHA" in {
        //val callSites = retrieveCallSites("/columbus1_5_SOOT_CHA.json")
        val callSites = retrieveCallSites("/flashchards_SOOT_CHA.json")

        for {
            m ← columbusProject.allMethodsWithBody
            dm = declaredMethods(m)
            computedCallees = propertyStore(dm, Callees.key).asFinal.p
            (pc, computedTargets) ← computedCallees.callees
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

    it should "contain all calls from WALA 1-CFA" in {
        //val callSites = retrieveCallSites("/columbus1_5_WALA_1_CFA.json").callSites
        val callSites = retrieveCallSites("/flashchards_SOOT_SPARK.json").callSites
        for {
            m ← columbusProject.allMethodsWithBody
            dm = declaredMethods(m)
            methodRepresentation = convertMethod(dm)
            FinalEP(_, computedCallees) = propertyStore(dm, Callees.key).asFinal
            CallSite(declaredTgt, line, _, tgts) ← callSites.filter(_.method == methodRepresentation)
            computedCallSites = computedCallees.callees.filter {
                case (pc, computedTgt) ⇒
                    m.body.get.lineNumber(pc).get == line &&
                        computedTgt.nonEmpty && computedTgt.head.name == declaredTgt.name // todo also use retType + paramTypes
            }.toList
            tgt ← tgts
        } {
            val containsCall = computedCallSites.exists(cs ⇒ cs._2.exists(computedTgt ⇒ convertMethod(computedTgt) == tgt))
            if (!containsCall) {
                println()
            }
            assert(
                containsCall,
                s"cg does not contain call from \n\t$dm \nto \n\t$tgt \nat line $line in: \n\t $computedCallSites"
            )
        }

    }

    def checkBidirectionCallerCallee(
        propertyStore: PropertyStore
    )(implicit declaredMethods: DeclaredMethods): Unit = {
        for {
            FinalEP(dm: DeclaredMethod, callees) ← propertyStore.entities(Callees.key).map(_.asFinal)
            (pc, tgts) ← callees.callees
            callee ← tgts
        } {
            val FinalEP(_, callersProperty) = propertyStore(callee, CallersProperty.key).asFinal
            assert(callersProperty.callers.toSet.contains(dm → pc))
        }

        for {
            FinalEP(dm: DeclaredMethod, callers) ← propertyStore.entities(CallersProperty.key).map(_.asFinal)
            (caller, pc) ← callers.callers
        } {
            val FinalEP(_, calleesProperty) = propertyStore(caller, Callees.key).asFinal
            calleesProperty.callees(pc).contains(dm)
        }
    }

    def convertMethod(dm: DeclaredMethod): Method = {
        assert(dm.hasSingleDefinedMethod)
        val method = dm.definedMethod
        assert(dm.declaringClassType eq method.classFile.thisType)

        val name = method.name
        val declaringClass = method.classFile.thisType.toJVMTypeName
        val returnType = method.returnType.toJVMTypeName
        val parameterTypes = method.parameterTypes.map(_.toJVMTypeName).toList

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
