/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package analyses
package javaAnalysis
package detector

import org.opalj.br.FieldType
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
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
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.VirtualMethodCall
import org.opalj.tac.common.DefinitionSite
import org.opalj.xl.axa.common.Language

import scala.collection.mutable
import org.opalj.xl.analyses.javaAnalysis.detector.JavaScriptEngineDetector.jsEngineNames
import org.opalj.xl.analyses.javaAnalysis.detector.JavaScriptEngineDetector.jsExtensions
import org.opalj.xl.analyses.javaAnalysis.detector.JavaScriptEngineDetector.jsMimetypes
import org.opalj.tac.AssignmentLikeStmt
import org.opalj.tac.Stmt
import org.opalj.tac.TACode
import org.opalj.tac.fpcf.analyses.cg.V
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.TheTACAI
import org.opalj.xl.axa.common.Language.Language
import org.opalj.xl.axa.detector.DetectorLattice
import org.opalj.xl.axa.detector.CrossLanguageCall
import org.opalj.xl.axa.detector.NoCrossLanguageCall

class JavaScriptEngineDetector private[analyses] (
                                             final val project: SomeProject
                                           ) extends FPCFAnalysis {

  case class JavaScriptEngineDetectorState(
       method:Method,
       assignments: mutable.Map[String, Tuple2[FieldType, Set[DefinitionSite]]] = mutable.Map.empty,
       var sourceCode: String ="",
       var language: Language = Language.Unknown,
       var tacDependees: Set[EOptionP[Entity, Property]] = Set.empty,
  )

    def analyzeMethod(method: Method): ProperPropertyComputationResult = {

    def getTACAI(method: Method)(implicit state:JavaScriptEngineDetectorState) : Option[TACode[TACMethodParameter, V]] = {
      val tacEOptP = propertyStore(method, TACAI.key)
      if (tacEOptP.hasUBP)
        tacEOptP.ub.tac
      else {
        state.tacDependees += tacEOptP
        None
      }
    }

    def getString(id: Int, stmts:Array[Stmt[V]]): String =
      stmts(id).asAssignment.expr.asStringConst.value

      def c(eps: SomeEPS)(implicit state:JavaScriptEngineDetectorState): ProperPropertyComputationResult = {
        state.tacDependees -= eps
        eps match {
          case UBP(tacai:TheTACAI) => //PointsToSet
            val stmts = tacai.tac.get.stmts
            scanForEngine(stmts)
          case ep =>
            state.tacDependees += ep
        }
        createResults
      }

    def scanForEngine(stmts:Array[Stmt[V]])(implicit state:JavaScriptEngineDetectorState): Unit = {
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
          val name = getString(params.head.asVar.definedBy.head, stmts).toLowerCase
          if (jsEngineNames.contains(name))
            state.language = Language.JavaScript

        case AssignmentLikeStmt(
            _,
            VirtualFunctionCall(
              _,
              ObjectType("javax/script/ScriptEngineManager"),
              _,
              "getEngineByExtension",
              _,
              _,
              params
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
          if (jsMimetypes.contains(mimetype))
            state.language = Language.JavaScript

        case AssignmentLikeStmt(
            _,
            VirtualFunctionCall(_, ObjectType("javax/script/ScriptEngine"), _, "eval", _, _, params)
            ) =>
          state.sourceCode += getString(params.head.asVar.definedBy.head, stmts) + "\n"

        // TODO Handle Invocable.invokeFunction? It can be called on the ScriptEngine after an eval that defined top-level functions

        case VirtualMethodCall(
            _,
            ObjectType("javax/script/ScriptEngine"),
            _,
            "put",
            _,
            _,
            params
            ) =>
          val defSites: Set[DefinitionSite] =
            params.tail.head.asVar.definedBy.map(id => {
              val pc = stmts(id).asAssignment.pc
              DefinitionSite(method, pc)
            })

          val value = params.tail.head.asVar.value
          val tpe = if (value.isPrimitiveValue)
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
        if (state.sourceCode != "" && state.language != Language.Unknown) {
          println(s"$method Crosslanguage call ${state.language} ${state.sourceCode}")
          state.assignments.foreach(println(_))
          println("................................")
          Result(method, CrossLanguageCall(state.language, state.sourceCode, state.assignments))
        } else{
          println(s"$method NoCrossLanguageCall")
          Result(method, NoCrossLanguageCall)
        }
      } else
        InterimResult(
          method,
          NoCrossLanguageCall,
          CrossLanguageCall(state.language, state.sourceCode, state.assignments),
          state.tacDependees,
          c
        )
    }

      //start
      implicit val state:JavaScriptEngineDetectorState = JavaScriptEngineDetectorState(method)
      val optionTACAI = getTACAI(method)
      if (optionTACAI.isEmpty && state.tacDependees.isEmpty){
        println(s"$method NoCrossLanguageCall")
        Result(method, NoCrossLanguageCall)
    } else if(optionTACAI.isDefined)
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
    val jsMimetypes = Set("application/javascript, application/ecmascript, text/javascript, text/ecmascript")
}

trait JavaScriptEngineDetectorScheduler extends FPCFAnalysisScheduler {

    def derivedProperty: PropertyBounds = PropertyBounds.ub(DetectorLattice)

    override def requiredProjectInformation: ProjectInformationKeys = Seq.empty

    override def uses: Set[PropertyBounds] = Set(PropertyBounds.ub(DetectorLattice))

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
        println("analyze the following methods:")
        methods.foreach(println(_))
        println("---------------------------")
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
        propertyStore.registerLazyPropertyComputation(DetectorLattice.key,analysis.lazilyDetectJavaScriptEngine)
        analysis
    }
}