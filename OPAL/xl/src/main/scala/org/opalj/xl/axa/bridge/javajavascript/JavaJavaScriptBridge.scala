/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.xl.axa.bridge.javajavascript

import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.BasicFPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.fpcf.Entity
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.tac.AITACode
import org.opalj.tac.Assignment
import org.opalj.tac.ComputeTACAIKey
import org.opalj.tac.ExprStmt
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.VirtualMethodCall
import org.opalj.tac.common.DefinitionSite
import org.opalj.value.ValueInformation
import org.opalj.xl.analyses.javaAnalysis.adaptor.CrossLanguageCall
import org.opalj.xl.analyses.javaAnalysis.adaptor.JavaJavaScriptAdaptorLattice
import org.opalj.xl.analyses.javaAnalysis.adaptor.NoCrossLanguageCall

import scala.collection.immutable
import scala.collection.mutable

class JavaJavaScriptBridge(val project: SomeProject) extends FPCFAnalysis {



  def analyzeMethod(method: Method): ProperPropertyComputationResult = {

    val tacaiKey: Method => AITACode[TACMethodParameter, ValueInformation] = p.get(ComputeTACAIKey)
    val tacai = tacaiKey(method)
    val stmts = tacai.stmts
    val assignments:mutable.Map[String,Set[DefinitionSite]] = mutable.Map.empty
    def getString(id: Int): String =
      stmts(id).asAssignment.expr.asStringConst.value

    var sourceCode = ""
    var language = ""
    stmts.foreach(stmt=>
    stmt match {
      case Assignment(pc, _, VirtualFunctionCall(pc2, ObjectType("javax/script/ScriptEngineManager"), isInterface, "getEngineByName", descriptor, receiver, params)) =>
        language = getString(params.head.asVar.definedBy.head)
      case ExprStmt(pc, VirtualFunctionCall(pc2, ObjectType("javax/script/ScriptEngine"), isInterface, "eval", descriptor, receiver, params)) =>
        sourceCode += getString(params.head.asVar.definedBy.head) + "\n"
      case VirtualMethodCall(_, ObjectType("javax/script/ScriptEngine"),_,"put",_, _, params) =>
        println(params)

        val defSites:Set[DefinitionSite] =
          params.tail.head.asVar.definedBy.map(id=> {
            val pc = stmts(id).asAssignment.pc
            DefinitionSite(method, pc)})
      assignments += getString(params.head.asVar.definedBy.head) -> defSites
      case _ =>
    }
    )
    if(sourceCode!="" && language!="")
      Result(method, CrossLanguageCall(language, sourceCode , assignments))
    else
      Result(method, NoCrossLanguageCall)

  }

  def lazilyAdaptJavaJavaScript(entity: Entity): ProperPropertyComputationResult = {
    entity match {
      case method: Method =>
        analyzeMethod(method)
      case _              => throw new IllegalArgumentException("can only process functions")
    }
  }

}

trait JavaJavaScriptBridgeScheduler extends FPCFAnalysisScheduler {
  def derivedProperty: PropertyBounds = PropertyBounds.ub(JavaJavaScriptAdaptorLattice)

  override def requiredProjectInformation: ProjectInformationKeys = Seq.empty

  override def uses: immutable.Set[PropertyBounds] =
    immutable.Set(
      PropertyBounds.ub(JavaJavaScriptAdaptorLattice)
    )
}
object EagerJavaJavaScriptBridge
    extends JavaJavaScriptBridgeScheduler
    with BasicFPCFEagerAnalysisScheduler {
  override def derivesEagerly: immutable.Set[PropertyBounds] = immutable.Set(derivedProperty)

  override def derivesCollaboratively: immutable.Set[PropertyBounds] = immutable.Set.empty

  override def start(
      project: SomeProject,
      propertyStore: PropertyStore,
      initData: InitializationData
  ): FPCFAnalysis = {
    val analysis = new JavaJavaScriptBridge(project)
    propertyStore.scheduleEagerComputationsForEntities(project.allMethods)(analysis.analyzeMethod)
    analysis
  }
}


object LazyJavaJavaScriptBridge extends JavaJavaScriptBridgeScheduler with BasicFPCFLazyAnalysisScheduler {
  override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

  override def register(
      project: SomeProject,
      propertyStore: PropertyStore,
      initData: InitializationData
  ): FPCFAnalysis = {
    val analysis = new JavaJavaScriptBridge(project)
    propertyStore.registerLazyPropertyComputation(
      JavaJavaScriptAdaptorLattice.key,
      analysis.lazilyAdaptJavaJavaScript
    )
    analysis
  }
}