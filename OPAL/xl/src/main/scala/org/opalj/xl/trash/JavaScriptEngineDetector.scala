/* BSD 2-Clause License - see OPAL/LICENSE for details. */
/*package org.opalj
package xl
package javaanalyses
package detector
package scriptengine

import org.opalj.br.FieldType
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.tac.Assignment
import org.opalj.tac.Stmt
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.VirtualMethodCall
import org.opalj.tac.fpcf.properties.TACAI
import JavaScriptEngineDetector.jsEngineNames
import JavaScriptEngineDetector.jsExtensions
import JavaScriptEngineDetector.jsMimetypes

import org.opalj.tac.fpcf.analyses.cg.reflection.VarargsUtil
import org.opalj.xl.utility.Language.Language
import org.opalj.xl.utility.JavaScriptFunctionCall
import org.opalj.xl.utility.Language
import org.opalj.xl.utility.VarNames
import org.opalj.xl.coordinator.Coordinator.V
import org.opalj.xl.detector
import org.opalj.xl.detector.NoCrossLanguageCall
import scala.collection.mutable

import org.opalj.xl.detector.CrossLanguageInteraction

import org.opalj.fpcf.EPS
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.fpcf.properties.SystemProperties
import org.opalj.br.fpcf.BasicFPCFTriggeredAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.br.fpcf.properties.pointsto.TypeBasedPointsToSet
import org.opalj.tac.Call
import org.opalj.tac.VirtualFunctionCallStatement
import org.opalj.tac.cg.TypeIteratorKey
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.analyses.cg.ReachableMethodAnalysis
import org.opalj.tac.fpcf.analyses.cg.xta.TypeSetEntitySelector
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedAnalysis
import org.opalj.tac.fpcf.analyses.pointsto.PointsToAnalysisBase
import org.opalj.tac.fpcf.analyses.pointsto.TypeBasedAnalysis
import org.opalj.tac.fpcf.properties.cg.Callers
import org.opalj.tac.fpcf.properties.cg.InstantiatedTypes



/**
 * Scans all reachable methods for Java ScriptEngine JavaScript access.
 * @param project
 */
class JavaScriptEngineDetector(final val project: SomeProject) extends ReachableMethodAnalysis with PointsToAnalysisBase {

    val definitionSites = project.get(DefinitionSitesKey)
    val virtualFormalParameters = project.get(VirtualFormalParametersKey)

    val scriptEngine = ObjectType("javax/script/ScriptEngine")
    val engineManager = ObjectType("javax/script/ScriptEngineManager")
    val graalContext = ObjectType("org/graalvm/polyglot/Context")

    class JavaScriptEngineDetectorState(var context: ContextType,
                                        val puts: mutable.Map[String, Tuple3[FieldType, Set[AnyRef], Option[Double]]] = mutable.Map.empty,
                                        val gets: mutable.Map[V, String] = mutable.Map.empty,
                                        var sourceCode: String = "",
                                        val foreignFunctionCalls: mutable.ListBuffer[JavaScriptFunctionCall] = mutable.ListBuffer.empty[JavaScriptFunctionCall],
                                        var language: Language = Language.Unknown
                                       )


    override def processMethod(context: ContextType, tacaiEP: EPS[Method, TACAI]): ProperPropertyComputationResult = {
        //implicit val state: State = new PointsToAnalysisState[ElementType, PointsToSet, ContextType](context, tacaiEP)

        def getString(id: Int, stmts: Array[Stmt[V]]): String = {
            if (id >= 0) {
                val assignment = stmts(id).asAssignment
                if (assignment.expr.isStringConst)
                    return assignment.expr.asStringConst.value
            }
            ""
        }

        /*        def c(eps: SomeEPS)(implicit detectorState: JavaScriptEngineDetectorState): ProperPropertyComputationResult = {
        /*state.tacDependees = state.tacDependees.filter(_.e != eps.e)
        eps match     case UBP(tacai: TheTACAI) =>
                val stmts = tacai.tac.get.stmts
                scanForEngine(stmts)
            case ep =>
                state.tacDependees += ep
        } */
        createResult
        } */


        def scanForEngine(stmts: Array[Stmt[V]])(implicit detectorState: JavaScriptEngineDetectorState): Unit = {
            stmts.foreach { stmt =>
                stmt match {

                    case VirtualFunctionCallStatement(c@Call(this.engineManager, _, "getEngineByName", _)) =>
                        // TODO These "definedBy.head" are incorrect, there can be several definition sites that need to be ckecked
                        val name = getString(c.params.head.asVar.definedBy.head, stmts).toLowerCase
                        if (jsEngineNames.contains(name.toLowerCase))
                            detectorState.language = Language.JavaScript

                    case VirtualFunctionCallStatement(c@Call(this.engineManager, _, "getEngineByExtension", _)) =>
                        val extension = getString(c.params.head.asVar.definedBy.head, stmts)
                        if (jsExtensions.contains(extension.toLowerCase))
                            detectorState.language = Language.JavaScript

                    case VirtualFunctionCallStatement(c@Call(this.engineManager, _, "getEngineByMimeType", _)) =>
                        val mimetype = getString(c.params.head.asVar.definedBy.head, stmts)
                        if (jsMimetypes.contains(mimetype.toLowerCase))
                            detectorState.language = Language.JavaScript

                    // TODO Why don't we map back the return value of the eval/invokeFunction invocations?
                    case VirtualFunctionCallStatement(c@Call(this.scriptEngine, _, "eval", _)) =>
                        // TODO Support eval variants that take a Reader?
                        //state.sourceCode += scala.io.Source.fromURL(url).mkString
                        detectorState.sourceCode += getString(c.params.head.asVar.definedBy.head, stmts).filterNot(c => c.isWhitespace && c == '\n')
                    /*bingings...val x = stmts(params(1).asVar.definedBy.head).asAssignment.targetVar.usedBy.map(stmts(_))

                    x.foreach {
                        case ExprStmt(pc, VirtualFunctionCall(_, declaringClass, isInterface, "put", descriptor, receiver, params)) =>
                            println(params)
                        case _ =>
                    }

                    println(x)*/

                    case VirtualFunctionCallStatement(c@Call(this.graalContext, _, "eval", _)) =>
                        if (c.params.size == 1) {
                            // TODO Support graalvm Source class
                        } else {
                            val language = getString(c.params.head.asVar.definedBy.head, stmts)
                            if (language == "js") {
                                detectorState.language = Language.JavaScript
                                detectorState.sourceCode += getString(c.params(1).asVar.definedBy.head, stmts).filterNot(c => c.isWhitespace && c == '\n')
                            }
                        }

                    // TODO Graalvm supports a similar concept, with an executable Value as the result of either Context.eval or Context.parse
                    case Assignment(
                    _, lhs, VirtualFunctionCall(_, tpe, _, "invokeFunction", _, _, params)) =>
                        // TODO This implementation seems wrong, invokeFunction must be called on an Invocable and we have to trace it back to a ScriptEngine.eval
                        val varargsParams = VarargsUtil.getParamsFromVararg(params(1), stmts)
                        detectorState.foreignFunctionCalls +=
                            JavaScriptFunctionCall(
                                getString(params.head.asVar.definedBy.head, stmts),
                                varargsParams.get.toList.map(p => {
                                    val v: Option[Double] = if (stmts(p.asVar.definedBy.head).asAssignment.expr.isStaticFunctionCall) {
                                        if (stmts(p.asVar.definedBy.head).asAssignment.expr.asStaticFunctionCall.params(0).isVar) {
                                            val defSite = stmts(p.asVar.definedBy.head).asAssignment.expr.asStaticFunctionCall.params(0).asVar.definedBy.head
                                            val e = stmts(defSite).asAssignment.expr
                                            if (e.isIntConst)
                                                Some(e.asIntConst.value.doubleValue())
                                            else if (e.isDoubleConst)
                                                Some(e.asDoubleConst.value)
                                            else if (e.isFloatConst)
                                                Some(e.asFloatConst.value.doubleValue())
                                            else None
                                        }
                                        else null
                                    } else null
                                    // pointsto.allocationSiteToLong(context, )
                                    /*  currentPointsToOfDefSites(null, p.asVar.definedBy, _ => true).foreach { pts =>
                                          println(pts.toEntity)
                                          pts.forNewestNElements(pts.numElements) { as => print(as) }
                                      } */


                                    (VarNames.genVName() -> (ObjectType.Object.asFieldType, p.asVar.definedBy.toList.map(_.asInstanceOf[AnyRef]).toSet, v)) //TODO !!! p.asVar.value.asReferenceValue
                                }).toMap,
                                lhs.asVar
                            )

                    // TODO Support Graalvm getBindings/getPolyglotBindings -> getMember?
                    case Assignment(_, lhs, VirtualFunctionCall(_, this.scriptEngine, _, "get", _, _, params)) =>
                        val jsValueName = getString(params.head.asVar.definedBy.head, stmts)
                        detectorState.gets += lhs -> jsValueName

                    // TODO Beside put, should we support Binding and ScriptContext as ways to provide values to the script?
                    // TODO Support Graalvm getBindings/getPolyglotBindings -> putMember?
                    case VirtualMethodCall(_, this.scriptEngine, _, "put", _, _, params) =>
                        val defSites: Set[AnyRef] =
                            params(1).asVar.definedBy.map(id => {
                                if (id < 0) {
                                    virtualFormalParameters(context.method)(-id - 1)
                                } else {
                                    val pc = stmts(id).asAssignment.pc
                                    definitionSites(context.method.definedMethod, pc)
                                }
                            })

                        val value = params(1).asVar.value
                        val v: Option[Double] = if (params(1).asVar.definedBy.forall(_ > -1) && stmts(params(1).asVar.definedBy.head).asAssignment.expr.isStaticFunctionCall) {
                            if (stmts(params(1).asVar.definedBy.head).asAssignment.expr.asStaticFunctionCall.params(0).isVar) {
                                val defSite = stmts(params(1).asVar.definedBy.head).asAssignment.expr.asStaticFunctionCall.params(0).asVar.definedBy.head
                                val e = stmts(defSite).asAssignment.expr
                                if (e.isIntConst)
                                    Some(e.asIntConst.value.doubleValue())
                                else if (e.isDoubleConst)
                                    Some(e.asDoubleConst.value)
                                else if (e.isFloatConst)
                                    Some(e.asFloatConst.value.doubleValue())
                                else None
                            }
                            else None
                        }
                        else null
                        val tpe =
                            if (value.isPrimitiveValue) {
                                value.asPrimitiveValue.primitiveType.asFieldType
                            } else
                                value.asReferenceValue.leastUpperType.get.asFieldType
                        //    currentPointsToOfDefSites(null, params(1).asVar.definedBy, _=> true).foreach { pts =>
                        //        pts.forNewestNElements(pts.numElements) { as => print(as)}}
                        //   currentPointsToOfDefSites(null, params.head.asVar.definedBy, _=>true )

                        detectorState.puts += getString(params.head.asVar.definedBy.head, stmts) -> ((tpe, defSites, v))

                    case _ =>
                }
            }
        }

        def createResult(implicit detectorState: JavaScriptEngineDetectorState): ProperPropertyComputationResult = {
            //if (detectorState) {
            if ((detectorState.sourceCode != "" || detectorState.gets.size > 0) && detectorState.sourceCode != "true" && !context.method.definedMethod.classFile.thisType.packageName.contains("ScriptUtils")) { //&& state.language == Language.JavaScript
                Result(context, detector.JavaScriptInteraction(detectorState.sourceCode, detectorState.foreignFunctionCalls.toList, detectorState.puts, detectorState.gets)) //TODO use context
            } else {
                Result(context, NoCrossLanguageCall)
            }
            /*} else {
                InterimResult(
                    context,
                    NoJavaScriptCall,
                    detector.JavaScriptFunctionCall(detectorState.sourceCode, detectorState.foreignFunctionCalls.toList, detectorState.puts, detectorState.gets),
                    List.empty[],
                    c
                )
            }*/
        }

        //start
        implicit
        val detectorState: JavaScriptEngineDetectorState = new JavaScriptEngineDetectorState(context)
        scanForEngine(tacaiEP.ub.tac.get.stmts)
        createResult
    }

}

object JavaScriptEngineDetector {
    val jsEngineNames = Set("nashorn", "rhino", "js", "javascript", "ecmascript", "graal.js")
    val jsExtensions = Set("js")
    val jsMimetypes = Set("application/javascript", "application/ecmascript", "text/javascript", "text/ecmascript")
}
/*
object JavaScriptEngineDetectorScheduler extends BasicFPCFTriggeredAnalysisScheduler {
    override def requiredProjectInformation: ProjectInformationKeys =
        Seq(DeclaredMethodsKey, TypeIteratorKey)

    override def uses: Set[PropertyBounds] =
        Set(PropertyBounds.ub(Callers), PropertyBounds.ub(TACAI))

    override def triggeredBy: PropertyKey[Callers] = Callers.key

    override def register(p: SomeProject, ps: PropertyStore, unused: Null): JavaScriptEngineDetector = {
        val analysis = new JavaScriptEngineDetector(p)
        ps.registerTriggeredComputation(triggeredBy, analysis.analyze)
        analysis
    }

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override
    def derivesCollaboratively: Set[PropertyBounds] = Set(
        PropertyBounds.ub(SystemProperties)
    )

    val propertyKind: PropertyMetaInformation = CrossLanguageInteraction
    //val createAnalysis: SomeProject => JavaScriptEngineDetector

    override type InitializationData = Null
    /*
    override def requiredProjectInformation: ProjectInformationKeys =
        Seq(DeclaredMethodsKey, VirtualFormalParametersKey, DefinitionSitesKey, TypeIteratorKey)

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(Callers, propertyKind)


    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = createAnalysis(p)
        ps.registerTriggeredComputation(Callers.key, analysis.analyze)
        analysis
    } */
}
*/

class JavaScriptEngineDetectorScheduler(
                                              selectSetEntity: TypeSetEntitySelector
                                          ) extends BasicFPCFTriggeredAnalysisScheduler {
    val createAnalysis: SomeProject => JavaScriptEngineDetector
    override def requiredProjectInformation: ProjectInformationKeys = Seq(TypeIteratorKey)

    override def register(project: SomeProject, propertyStore: PropertyStore, i: Null): FPCFAnalysis = {
        val analysis = createAnalysis()
        propertyStore.registerTriggeredComputation(Callers.key, analysis.analyze)
        analysis
    }

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(TACAI)
    override def derivesEagerly: Set[PropertyBounds] = Set.empty
    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(InstantiatedTypes)
    override def triggeredBy: PropertyKind = Callers.key
}



object TypeBasedJavaScriptEngineDetectorScheduler extends JavaScriptEngineDetectorScheduler {
    override val propertyKind: PropertyMetaInformation = TypeBasedPointsToSet
    override val createAnalysis: SomeProject => JavaScriptEngineDetector =
        new JavaScriptEngineDetector(_) with TypeBasedAnalysis
}

object AllocationSiteJavaScriptEngineDetectorScheduler extends JavaScriptEngineDetectorScheduler {
    override val propertyKind: PropertyMetaInformation = AllocationSitePointsToSet
    override val createAnalysis: SomeProject => JavaScriptEngineDetector =
        new JavaScriptEngineDetector(_) with AllocationSiteBasedAnalysis
}
*/