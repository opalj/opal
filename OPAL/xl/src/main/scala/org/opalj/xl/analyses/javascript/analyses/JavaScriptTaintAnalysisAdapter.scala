/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package analyses
package javascript
package analyses
/*
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.properties.JavaFunction
import org.opalj.br.fpcf.properties.TIPFunction
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.Entity
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.LBP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.xl.adapter.FinalAnalysisResult
import org.opalj.xl.adapter.InterimAnalysisResults
import org.opalj.xl.analyses.java.analysis.JavaTaintLattice
import org.opalj.xl.bridge.UniFUntainted
import org.opalj.xl.bridge.UniversalFunctionTaintLattice
import org.opalj.xl.bridge.variable.UniVTainted
import org.opalj.xl.bridge.variable.UniVUntainted

class TIPTaintAnalysisAdapter(val project: SomeProject) extends FPCFAnalysis {


  def analyzeTIPFunction(function: TIPFunction): ProperPropertyComputationResult = {

    var dependencies: Set[EOptionP[Entity, Property]] = Set.empty




    def c(updatedValue: SomeEPS): ProperPropertyComputationResult = {
      updatedValue match {
        case UBP(UniVTainted | UniVTainted) => Result(function, TIPTainted)
        case LBP(UniVUntainted | UniFUntainted) => Result(function, TIPUntainted)
        case eps => dependencies+=eps;  InterimResult(updatedValue.e, TIPUntainted, TIPTainted, dependencies, c)
      }
    }
val result = propertyStore(JavaFunction("h"), JavaTaintLattice.key)

    }
  JavaScriptNativeAnalysis.analyze(function, propertyStore) match {
      case InterimAnalysisResults(_) =>
        InterimResult(function, TIPUntainted, TIPTainted, Set(result), c)
      case FinalAnalysisResult(result) =>
        if (result)
          Result(function, TIPTainted)
        else
          Result(function, TIPUntainted)
    }.analyze(function, propertyStore) match {
      case InterimAnalysisResults(_) =>
        InterimResult(function, TIPUntainted, TIPTainted, Set(result), c)
      case FinalAnalysisResult(result) =>
        if(result)
          Result(function, TIPTainted)
        else
          Result(function, TIPUntainted)
    }
  }


  def lazilyAnalyzeTIPFunction(function: Entity): ProperPropertyComputationResult = {
    function match {
      case function: TIPFunction => analyzeTIPFunction(function)
      case f            => throw new IllegalArgumentException(s"$f is no TIP function")
    }
  }
}

trait L1TaintAnalysisScheduler extends FPCFAnalysisScheduler {
  def derivedProperty: PropertyBounds = PropertyBounds.ub(TIPTaintLattice)

  override def requiredProjectInformation: ProjectInformationKeys = Seq.empty

  override def uses: Set[PropertyBounds] =
    Set(PropertyBounds.ub(TIPTaintLattice), PropertyBounds.ub(UniversalFunctionTaintLattice))
}

object LazyTIPTaintAnalysis extends L1TaintAnalysisScheduler with BasicFPCFLazyAnalysisScheduler {
  override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

  override def register(project: SomeProject, propertyStore: PropertyStore, initData: InitializationData): FPCFAnalysis = {
    val analysis = new TIPTaintAnalysisAdapter(project)
    propertyStore.registerLazyPropertyComputation(TIPTaintLattice.key, analysis.lazilyAnalyzeTIPFunction)
    analysis
  }
}

*/ 