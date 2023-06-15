/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package analyses
package javaanalyses
package detector
package embodiment

import org.opalj.br.FieldType
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.BasicFPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.Entity
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.tac.Assignment
import org.opalj.tac.AssignmentLikeStmt
import org.opalj.tac.Stmt
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.VirtualMethodCall
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.TheTACAI
import JavaScriptEngineDetector.jsEngineNames
import JavaScriptEngineDetector.jsExtensions
import JavaScriptEngineDetector.jsMimetypes
import org.opalj.tac.ExprStmt
import org.opalj.tac.fpcf.analyses.cg.reflection.VarargsUtil
import org.opalj.xl.common
import org.opalj.xl.common.Language.Language
import org.opalj.xl.common.ForeignFunctionCall
import org.opalj.xl.common.Language
import org.opalj.xl.common.VarNames
import org.opalj.xl.coordinator.Coordinator.V
import org.opalj.xl.detector
import org.opalj.xl.detector.JavaScriptInteraction
import org.opalj.xl.detector.NoJavaScriptCall

import scala.collection.mutable

/**
 * Detects embodied JavaScript code via the Java ScriptEngine
 *
 */
class JavaScriptEngineDetector private[analyses] ( final val project: SomeProject) extends FPCFAnalysis {

    val definitionSites = project.get(DefinitionSitesKey)
    val virtualFormalParameters = project.get(VirtualFormalParametersKey)
    val declaredMethods = project.get(DeclaredMethodsKey)

    case class JavaScriptEngineDetectorState(
            method:                  Method,
            assignments:             mutable.Map[String, Tuple2[FieldType, Set[AnyRef]]] = mutable.Map.empty,
            returnValues:            mutable.Map[V, String]                              = mutable.Map.empty,
            var sourceCode:          String                                              = "",
            var foreignFunctionCall: ForeignFunctionCall                                 = null,
            var functionParams:      List[V]                                             = List.empty,
            var language:            Language                                            = Language.Unknown,
            var tacDependees:        Set[EOptionP[Entity, Property]]                     = Set.empty
    )

    def analyzeMethod(method: Method): ProperPropertyComputationResult = {
        def getTACAI(method: Method)(implicit state: JavaScriptEngineDetectorState): Option[TACode[TACMethodParameter, V]] = {
            val tacEOptP = propertyStore(method, TACAI.key)
            if (tacEOptP.hasUBP)
                tacEOptP.ub.tac
            else {
                state.tacDependees += tacEOptP
                None
            }
        }

        def getString(id: Int, stmts: Array[Stmt[V]]): String =
            stmts(id).asAssignment.expr.asStringConst.value

        def c(eps: SomeEPS)(implicit state: JavaScriptEngineDetectorState): ProperPropertyComputationResult = {
            state.tacDependees = state.tacDependees.filter(_.e != eps.e)
            eps match {
                case UBP(tacai: TheTACAI) =>
                    val stmts = tacai.tac.get.stmts
                    scanForEngine(stmts)
                case ep =>
                    state.tacDependees += ep
            }
            createResults
        }

        def scanForEngine(stmts: Array[Stmt[V]])(implicit state: JavaScriptEngineDetectorState): Unit = {
            stmts.foreach { stmt =>
                stmt match {

                    case AssignmentLikeStmt(
                        _,
                        VirtualFunctionCall(
                            _,
                            ObjectType("javax/script/ScriptEngineManager"),
                            _,
                            "getEngineByName",
                            _,
                            _,
                            params
                            )
                        ) =>
                        val engineName = getString(params.head.asVar.definedBy.head, stmts).toLowerCase
                        if (jsEngineNames.contains(engineName)) state.language = Language.JavaScript

                    case AssignmentLikeStmt(_,
                        VirtualFunctionCall(_, ObjectType("javax/script/ScriptEngineManager"), _, "getEngineByExtension", _, _, params
                            )
                        ) =>
                        val extension = getString(params.head.asVar.definedBy.head, stmts)
                        if (jsExtensions.contains(extension))
                            state.language = Language.JavaScript

                    case AssignmentLikeStmt(
                        _,
                        VirtualFunctionCall(
                            _,
                            ObjectType("javax/script/ScriptEngineManager"),
                            _,
                            "getEngineByMimeType",
                            _,
                            _,
                            params
                            )
                        ) =>
                        val mimetype = getString(params.head.asVar.definedBy.head, stmts)
                        if (jsMimetypes.contains(mimetype)) state.language = Language.JavaScript

                    case Assignment(
                        _, lhs, VirtualFunctionCall(_, ObjectType("javax/script/ScriptEngine"), _, "get", _, _, params)) =>
                        val jsValueName = getString(params.head.asVar.definedBy.head, stmts)
                        state.returnValues += lhs -> jsValueName

                    case AssignmentLikeStmt(
                        _, VirtualFunctionCall(_, ObjectType("javax/script/ScriptEngine"), _, "eval", _, _, params)) =>

                        state.sourceCode += getString(params.head.asVar.definedBy.head, stmts)+"\n"

                        val x = stmts(params(1).asVar.definedBy.head).asAssignment.targetVar.usedBy.map(stmts(_))

                        x.foreach {
                            case ExprStmt(pc, VirtualFunctionCall(_, declaringClass, isInterface, "put", descriptor, receiver, params)) =>
                                println(params)
                            case _ =>
                        }

                        println(x)
                    case Assignment(
                        _, lhs, VirtualFunctionCall(_, tpe, _, "invokeFunction", _, _, params)) =>
                        // TODO Handle Invocable.invokeFunction? It can be called on the ScriptEngine after an eval that defined top-level functions

                        val varargsParams = VarargsUtil.getParamsFromVararg(params(1), stmts)
                        state.foreignFunctionCall =
                            common.ForeignFunctionCall(
                                getString(params.head.asVar.definedBy.head, stmts),
                                varargsParams.get.toList.map(p => { (VarNames.genVName(), p.asVar) }),
                                lhs.asVar
                            )

                    case VirtualMethodCall(_, ObjectType("javax/script/ScriptEngine"), _, "put", _, _, params) =>
                        val defSites: Set[AnyRef] =
                            params(1).asVar.definedBy.map(id => {
                                if (id < 0) {
                                    virtualFormalParameters(declaredMethods(method))(-id - 1)
                                } else {
                                    val pc = stmts(id).asAssignment.pc
                                    definitionSites(method, pc)
                                }
                            })

                        val value = params.tail.head.asVar.value
                        val tpe =
                            if (value.isPrimitiveValue)
                                value.asPrimitiveValue.primitiveType.asFieldType
                            else
                                value.asReferenceValue.leastUpperType.get.asFieldType
                        state.assignments += getString(params.head.asVar.definedBy.head, stmts) -> (tpe, defSites)

                    case _ =>
                }
            }
        }

        def createResults(implicit state: JavaScriptEngineDetectorState): ProperPropertyComputationResult = {
            if (state.tacDependees.isEmpty) {
                if (state.sourceCode != "" && state.language == Language.JavaScript) {
                    Result(method, detector.JavaScriptFunctionCall(state.sourceCode, state.foreignFunctionCall, state.assignments))
                } else {
                    Result(method, NoJavaScriptCall)
                }
            } else {
                InterimResult(
                    method,
                    NoJavaScriptCall,
                    detector.JavaScriptFunctionCall(state.sourceCode, state.foreignFunctionCall, state.assignments),
                    state.tacDependees,
                    c
                )
            }
        }

        //start
        implicit val state: JavaScriptEngineDetectorState = JavaScriptEngineDetectorState(method)
        val optionTACAI = getTACAI(method)
        if (optionTACAI.isEmpty && state.tacDependees.isEmpty) {
            Result(method, NoJavaScriptCall)
        } else if (optionTACAI.isDefined)
            scanForEngine(optionTACAI.get.stmts)
        createResults
    }

    def lazilyDetectJavaScriptEngine(entity: Entity): ProperPropertyComputationResult = {
        entity match {
            case method: Method => analyzeMethod(method)
            case _              => throw new IllegalArgumentException("can only process methods")
        }
    }
}

object JavaScriptEngineDetector {
    val jsEngineNames = Set("nashorn", "rhino", "js", "javascript", "ecmascript")
    val jsExtensions = Set("js")
    val jsMimetypes = Set("application/javascript", "application/ecmascript", "text/javascript", "text/ecmascript")
}

trait JavaScriptEngineDetectorScheduler extends FPCFAnalysisScheduler {

    def derivedProperty: PropertyBounds = PropertyBounds.ub(JavaScriptInteraction)

    override def requiredProjectInformation: ProjectInformationKeys = Seq.empty

    override def uses: Set[PropertyBounds] = Set(PropertyBounds.ub(JavaScriptInteraction))

}

object EagerJavaScriptEngineDetector extends JavaScriptEngineDetectorScheduler
    with BasicFPCFEagerAnalysisScheduler {

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    override def start(
        project:       SomeProject,
        propertyStore: PropertyStore,
        unused:        Null
    ): FPCFAnalysis = {
        val analysis = new JavaScriptEngineDetector(project)
        val methods = project.allProjectClassFiles.flatMap(_.methods)
        propertyStore.scheduleEagerComputationsForEntities(methods)(analysis.analyzeMethod)
        analysis
    }
}

object LazyJavaScriptEngineDetector extends JavaScriptEngineDetectorScheduler
    with BasicFPCFLazyAnalysisScheduler {

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def register(
        project:       SomeProject,
        propertyStore: PropertyStore,
        unused:        Null
    ): FPCFAnalysis = {
        val analysis = new JavaScriptEngineDetector(project)
        propertyStore.registerLazyPropertyComputation(JavaScriptInteraction.key, analysis.lazilyDetectJavaScriptEngine)
        analysis
    }
}
