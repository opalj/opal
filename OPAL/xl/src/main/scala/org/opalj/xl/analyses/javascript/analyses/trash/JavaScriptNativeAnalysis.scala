/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package analyses
package javascript
package analyses
/*
import org.opalj.br.fpcf.properties.ForeignFunction
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.PropertyStore
import org.opalj.xl.adapter.AnalysisInterface
import org.opalj.xl.adapter.AnalysisResults
import org.opalj.xl.adapter.FinalAnalysisResult
import org.opalj.xl.adapter.InterimAnalysisResults
import org.opalj.xl.bridge.UniFTainted
import org.opalj.xl.bridge.UniversalFunctionTaintLattice

object JavaScriptNativeAnalysis extends AnalysisInterface {
  override def analyze(
      entity: Entity, propertyStore: PropertyStore
  ): AnalysisResults ={
    val result = propertyStore(ForeignFunction("Java", "h"), UniversalFunctionTaintLattice.key)
    result match {
      case FinalP(UniFTainted) =>  FinalAnalysisResult(true)
      case eps => InterimAnalysisResults(Set(eps))
    }
  }

  override def resume(
      entity: Entity
  ): AnalysisResults = FinalAnalysisResult(true)
}


*/ 