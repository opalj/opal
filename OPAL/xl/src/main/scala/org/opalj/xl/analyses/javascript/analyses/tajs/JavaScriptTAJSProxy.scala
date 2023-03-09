/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package analyses
package javascript
package analyses

import org.opalj.br.Method
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.fpcf.Entity
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.xl.axa.bridge.javajavascript.JavaJavaScriptAdaptorLattice
import org.opalj.xl.trash.analysis.TIPTainted


class JavaScriptTAJSProxy(val project: SomeProject) extends FPCFAnalysis {


  def analyzeJavaScript(code: Method): ProperPropertyComputationResult = {

    //var dependencies: Set[EOptionP[Entity, Property]] = Set.empty


    val result =  propertyStore.entities(JavaJavaScriptAdaptorLattice.key)
print(result)

Result(code, TIPTainted)
    }

  /*
  def c(updatedValue: SomeEPS): ProperPropertyComputationResult = {
    updatedValue match {
      case UBP(UniVTainted | UniVTainted) => Result(function, TIPTainted)
      case LBP(UniVUntainted | UniFUntainted) => Result(function, TIPUntainted)
      case eps => dependencies+=eps;  //InterimResult(updatedValue.e, TIPUntainted, TIPTainted, dependencies, c)
    }
  }
val result = propertyStore(JavaFunction("h"), JavaTaintLattice.key) */
 /* analyze(function, propertyStore) match {
      case InterimAnalysisResults(_) =>
        InterimResult(function, TIPUntainted, TIPTainted, Set(result), c)
      case FinalAnalysisResult(result) =>
        if (result)
          Result(function, TIPTainted)
        else
          Result(function, TIPUntainted)
    }
  }*/


  def lazilyAnalyzeJavaScript(e: Entity): ProperPropertyComputationResult = {
    e match {
      case m: Method => analyzeJavaScript(m)
      case f            => throw new IllegalArgumentException(s"$f is no TIP function")
    }
  }
}

trait JavaScriptAnalysisScheduler extends FPCFAnalysisScheduler {
  def derivedProperty: PropertyBounds = PropertyBounds.ub(TIPTaintLattice)

  override def requiredProjectInformation: ProjectInformationKeys = Seq.empty

  override def uses: Set[PropertyBounds] =
    Set(PropertyBounds.ub(TIPTaintLattice)/*, PropertyBounds.ub(UniversalFunctionTaintLattice)*/)
}

object LazyJavaScriptAnalysis extends JavaScriptAnalysisScheduler with BasicFPCFLazyAnalysisScheduler {
  override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

  override def register(project: SomeProject, propertyStore: PropertyStore, initData: InitializationData): FPCFAnalysis = {
    val analysis = new JavaScriptTAJSProxy(project)
    propertyStore.registerLazyPropertyComputation(TIPTaintLattice.key, analysis.lazilyAnalyzeJavaScript)
    analysis
  }
}
/*
object EagerJavaScriptAnalysis
  extends JavaScriptAnalysisScheduler
    with BasicFPCFLazyAnalysisScheduler {
  override def derivesEagerly: Set[PropertyBounds] = immutable.Set(derivedProperty)

  override def derivesCollaboratively: immutable.Set[PropertyBounds] = immutable.Set.empty

  override def start(
                      project: SomeProject,
                      propertyStore: PropertyStore,
                      initData: InitializationData
                    ): FPCFAnalysis = {
    val analysis = new JavaScriptTAJSProxy(project)
    propertyStore.scheduleEagerComputationsForEntities(project.allMethods)(analysis.lazilyAnalyzeJavaScript)
    analysis
  }
}
*/