/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package analyses
package javaAnalysis
package adaptor

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
import org.opalj.xl.axa.common.AdaptorLattice
import org.opalj.xl.axa.common.CrossLanguageCall
import org.opalj.xl.axa.common.Language
import org.opalj.xl.axa.common.NoCrossLanguageCall

import scala.collection.immutable
import scala.collection.mutable

class ScriptEngineAdaptor(val project: SomeProject) extends FPCFAnalysis {

  def analyzeMethod(method: Method): ProperPropertyComputationResult = {

    val tacaiKey: Method => AITACode[TACMethodParameter, ValueInformation] = p.get(ComputeTACAIKey)
    val tacai = tacaiKey(method)
    val stmts = tacai.stmts
    val assignments:mutable.Map[String,Set[DefinitionSite]] = mutable.Map.empty
    def getString(id: Int): String =
      stmts(id).asAssignment.expr.asStringConst.value

    var sourceCode = ""
    var language = Language.Unknown
    stmts.foreach(stmt=>
    stmt match {

      case Assignment(_, _,
      VirtualFunctionCall(_, ObjectType("javax/script/ScriptEngineManager"), _,
      "getEngineByName", _, _, params)) =>
        val lowerLanguageString = getString(params.head.asVar.definedBy.head).toLowerCase
        if (lowerLanguageString=="javascript" || lowerLanguageString=="nashorn")
          language = Language.JavaScript

      case ExprStmt(_, VirtualFunctionCall(_, ObjectType("javax/script/ScriptEngine"), _, "eval", _, _, params)) =>
        sourceCode += getString(params.head.asVar.definedBy.head) + "\n"

      case VirtualMethodCall(_, ObjectType("javax/script/ScriptEngine"),_,"put",_, _, params) =>
        val defSites:Set[DefinitionSite] =
          params.tail.head.asVar.definedBy.map(id=> {
            val pc = stmts(id).asAssignment.pc
            DefinitionSite(method, pc)})
      assignments += getString(params.head.asVar.definedBy.head) -> defSites

      case _ =>
    })

    if(sourceCode!="" && language != Language.Unknown)
      Result(method, CrossLanguageCall(language, sourceCode , assignments))
    else
      Result(method, NoCrossLanguageCall)
  }

  def lazilyAdaptJavaJavaScript(entity: Entity): ProperPropertyComputationResult = {
    entity match {
      case method: Method => analyzeMethod(method)
      case _              => throw new IllegalArgumentException("can only process functions")
    }
  }

}

trait JavaJavaScriptAdaptorScheduler extends FPCFAnalysisScheduler {
  def derivedProperty: PropertyBounds = PropertyBounds.ub(AdaptorLattice)

  override def requiredProjectInformation: ProjectInformationKeys = Seq.empty

  override def uses: immutable.Set[PropertyBounds] =
    immutable.Set(
      PropertyBounds.ub(AdaptorLattice)
    )
}
object EagerJavaJavaScriptAdaptor
    extends JavaJavaScriptAdaptorScheduler
    with BasicFPCFEagerAnalysisScheduler {
  override def derivesEagerly: immutable.Set[PropertyBounds] = immutable.Set(derivedProperty)

  override def derivesCollaboratively: immutable.Set[PropertyBounds] = immutable.Set.empty

  override def start(
      project: SomeProject,
      propertyStore: PropertyStore,
      initData: InitializationData
  ): FPCFAnalysis = {
    val analysis = new ScriptEngineAdaptor(project)
    propertyStore.scheduleEagerComputationsForEntities(project.allMethods)(analysis.analyzeMethod)
    analysis
  }
}


object LazyJavaJavaScriptAdaptor extends JavaJavaScriptAdaptorScheduler with BasicFPCFLazyAnalysisScheduler {
  override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

  override def register(
      project: SomeProject,
      propertyStore: PropertyStore,
      initData: InitializationData
  ): FPCFAnalysis = {
    val analysis = new ScriptEngineAdaptor(project)
    propertyStore.registerLazyPropertyComputation(
      AdaptorLattice.key,
      analysis.lazilyAdaptJavaJavaScript
    )
    analysis
  }
}