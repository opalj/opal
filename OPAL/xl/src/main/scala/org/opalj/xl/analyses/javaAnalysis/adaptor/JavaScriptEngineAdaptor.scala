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

import org.opalj.xl.analyses.javaAnalysis.adaptor.ScriptEngineAdaptor.jsEngineNames
import org.opalj.xl.analyses.javaAnalysis.adaptor.ScriptEngineAdaptor.jsExtensions
import org.opalj.xl.analyses.javaAnalysis.adaptor.ScriptEngineAdaptor.jsMimetypes

import org.opalj.tac.AssignmentLikeStmt

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
    stmts.foreach {

      case AssignmentLikeStmt(_, VirtualFunctionCall(_, ObjectType("javax/script/ScriptEngineManager"), _, "getEngineByName", _, _, params)) =>
        val name = getString(params.head.asVar.definedBy.head).toLowerCase
        if (jsEngineNames.contains(name))
          language = Language.JavaScript

      case AssignmentLikeStmt(_, VirtualFunctionCall(_, ObjectType("javax/script/ScriptEngineManager"), _, "getEngineByExtension", _, _, params)) =>
        val extension = getString(params.head.asVar.definedBy.head)
        if (jsExtensions.contains(extension))
          language = Language.JavaScript

      case AssignmentLikeStmt(_, VirtualFunctionCall(_, ObjectType("javax/script/ScriptEngineManager"), _, "getEngineByMimeType", _, _, params)) =>
        val mimetype = getString(params.head.asVar.definedBy.head)
        if (jsMimetypes.contains(mimetype))
          language = Language.JavaScript

      case AssignmentLikeStmt(_, VirtualFunctionCall(_, ObjectType("javax/script/ScriptEngine"), _, "eval", _, _, params)) =>
        sourceCode += getString(params.head.asVar.definedBy.head) + "\n"

      // TODO Handle Invocable.invokeFunction? It can be called on the ScriptEngine after an eval that defined top-level functions

      case VirtualMethodCall(_, ObjectType("javax/script/ScriptEngine"), _, "put", _, _, params) =>
        val defSites: Set[DefinitionSite] =
          params.tail.head.asVar.definedBy.map(id => {
            val pc = stmts(id).asAssignment.pc
            DefinitionSite(method, pc)
          })
        assignments += getString(params.head.asVar.definedBy.head) -> defSites

      case _ =>
    }

    if(sourceCode!="" && language != Language.Unknown)
      Result(method, CrossLanguageCall(language, sourceCode , assignments))
    else
      Result(method, NoCrossLanguageCall)
  }

  def lazilyAdaptJavaJavaScript(entity: Entity): ProperPropertyComputationResult = {
    entity match {
      case method: Method => analyzeMethod(method)
      case _              => throw new IllegalArgumentException("can only process methods")
    }
  }

}

object ScriptEngineAdaptor {
  val jsEngineNames = Set("nashorn", "rhino", "js", "javascript", "ecmascript")
  val jsExtensions = Set("js")
  val jsMimetypes = Set("application/javascript, application/ecmascript, text/javascript, text/ecmascript")
}

trait JavaJavaScriptAdaptorScheduler extends FPCFAnalysisScheduler {

  def derivedProperty: PropertyBounds = PropertyBounds.ub(AdaptorLattice)

  override def requiredProjectInformation: ProjectInformationKeys = Seq.empty

  override def uses: Set[PropertyBounds] = Set(PropertyBounds.ub(AdaptorLattice))

}
object EagerJavaJavaScriptAdaptor extends JavaJavaScriptAdaptorScheduler
    with BasicFPCFEagerAnalysisScheduler {

  override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

  override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

  override def start(
      project: SomeProject,
      propertyStore: PropertyStore,
      unused: Null
  ): FPCFAnalysis = {
    val analysis = new ScriptEngineAdaptor(project)
    propertyStore.scheduleEagerComputationsForEntities(project.allMethods)(analysis.analyzeMethod)
    analysis
  }
}

object LazyJavaJavaScriptAdaptor extends JavaJavaScriptAdaptorScheduler
    with BasicFPCFLazyAnalysisScheduler {

  override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

  override def register(
      project: SomeProject,
      propertyStore: PropertyStore,
      unused: Null
  ): FPCFAnalysis = {
    val analysis = new ScriptEngineAdaptor(project)
    propertyStore.registerLazyPropertyComputation(
      AdaptorLattice.key,
      analysis.lazilyAdaptJavaJavaScript
    )
    analysis
  }
}