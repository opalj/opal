/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.xl.bridge

import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPS
import org.opalj.xl.analyses.a0.A0ProjectKey
import org.opalj.xl.analyses.a0.A0TaintLattice
import org.opalj.xl.analyses.a0.A0Tainted
import org.opalj.xl.analyses.a1.A1ProjectKey
import org.opalj.xl.analyses.a1.A1TaintLattice
import org.opalj.xl.analyses.a1.A1Tainted
import org.opalj.xl.analyses.a2.A2ProjectKey
import org.opalj.xl.analyses.a2.A2TaintLattice
import org.opalj.xl.analyses.a2.A2Tainted
import org.opalj.xl.languages.L.ForeignFunctionCall
import org.opalj.xl.languages.L0
import org.opalj.xl.languages.L1
import org.opalj.xl.languages.L2

import scala.collection.immutable

class Translator(val project: SomeProject) extends FPCFAnalysis {

  val A0Project = project.get(A0ProjectKey)
  val A1Project = project.get(A1ProjectKey)
  val A2Project = project.get(A2ProjectKey)

  def translateFunction(foreignFunctionCall: ForeignFunctionCall): ProperPropertyComputationResult = {
    

    val foreignLanguage = foreignFunctionCall.language
    val functionName = foreignFunctionCall.name
    // val language = foreignFunctionCall.getClass.getSuperclass

    val (function, taintKey) =
      foreignLanguage match {
      case L0 => (A0Project.functions.find(f=>f.name==functionName).head, A0TaintLattice.key)
      case L1 => (A1Project.functions.find(f=>f.name==functionName).head, A1TaintLattice.key)
      case L2 => (A2Project.functions.find(f=>f.name==functionName).head, A2TaintLattice.key)
    }





    def continuation(updatedValue: SomeEPS): ProperPropertyComputationResult = {
      updatedValue match {
        case FinalP(A0Tainted | A1Tainted | A2Tainted) => Result(foreignFunctionCall, UniFTainted)
        case FinalP(_)                                 => Result(foreignFunctionCall, UniFUntainted)
        case eps => InterimResult(foreignFunctionCall, UniFUntainted, UniFTainted, immutable.Set(eps):Set[SomeEOptionP], continuation(_))
      }
    }

    val result = propertyStore(function, taintKey)

    result match {
      case FinalP(A0Tainted | A1Tainted | A2Tainted) => Result(foreignFunctionCall, UniFTainted)
      case FinalP(_)                                 => Result(foreignFunctionCall, UniFUntainted)
      case eps  => InterimResult(foreignFunctionCall, UniFUntainted, UniFTainted, immutable.Set(eps), continuation)
    }
  }

  def lazilyAnalyzeClassImmutability(entity: Entity): ProperPropertyComputationResult = {
    entity match {
      case foreignFunctionCall: ForeignFunctionCall => translateFunction(foreignFunctionCall)
      case _                    => throw new IllegalArgumentException("can only process ffcs")
    }
  }
}

/* SCHEDULERS */

trait FunctionTranslatorScheduler extends FPCFAnalysisScheduler {
  def derivedProperty: PropertyBounds = PropertyBounds.ub(UniversalFunctionTaintLattice)

  override def requiredProjectInformation: ProjectInformationKeys = Seq.empty

  override def uses: Set[PropertyBounds] = Set(PropertyBounds.ub(A0TaintLattice), PropertyBounds.ub(A1TaintLattice), PropertyBounds.ub(A2TaintLattice))
}
/*
object FunctionTranslatorAnalysis extends FunctionTranslatorScheduler with BasicFPCFEagerAnalysisScheduler {
  override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

  override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

  override def start(project: SomeProject, propertyStore: PropertyStore, initData: InitializationData): FPCFAnalysis = {
    val analysis = new Translator(project)
    propertyStore.scheduleEagerComputationsForEntities(project.functions)(analysis.translateFunction)
    analysis
  }
} */

object LazyFunctionTranslatorAnalysis extends FunctionTranslatorScheduler with BasicFPCFLazyAnalysisScheduler {
  override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

  override def register(project: SomeProject, propertyStore: PropertyStore, initData: InitializationData): FPCFAnalysis = {
    val analysis = new Translator(project)
    propertyStore.registerLazyPropertyComputation(UniversalFunctionTaintLattice.key, analysis.translateFunction)
    analysis
  }
}
