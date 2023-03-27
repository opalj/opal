/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package axa
package frameworkconnector

import org.opalj.br.Method
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFTriggeredAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.xl.analyses.javascriptAnalysis.TAJS
import org.opalj.xl.axa.bridge.JavaScriptBridgeLattice
import org.opalj.xl.axa.bridge.JavaScriptCall
import org.opalj.xl.axa.bridge.NoJavaScriptCall
import org.opalj.xl.axa.common.AnalysisResultsLattice
import org.opalj.xl.axa.common.FinalAnalysisResult
import org.opalj.xl.axa.common.NoAnalysisResult

class JavaScriptAnalysisConnector(val project: SomeProject) extends FPCFAnalysis {

  def analyzeMethod(method: Method): ProperPropertyComputationResult = {

    val result = propertyStore(method, JavaScriptBridgeLattice.key)

    val analysisResult: ProperPropertyComputationResult = result.ub match {

      case JavaScriptCall(code, assignments) =>
        Result(method, FinalAnalysisResult(TAJS.analyze(code, assignments)))

      case NoJavaScriptCall => Result(method, NoAnalysisResult)

      case x => throw new Exception(s" $x is not the right property")
    }

    analysisResult
  }
}

object TriggeredFrameworkProxyScheduler extends BasicFPCFTriggeredAnalysisScheduler {
  override def requiredProjectInformation: ProjectInformationKeys = Seq()

  override def uses: Set[PropertyBounds] = Set(PropertyBounds.ub(JavaScriptBridgeLattice))

  override def triggeredBy: PropertyKey[JavaScriptBridgeLattice] = JavaScriptBridgeLattice.key

  override def register(
      p: SomeProject,
      ps: PropertyStore,
      unused: Null
  ): JavaScriptAnalysisConnector = {
    val analysis = new JavaScriptAnalysisConnector(p)
    ps.registerTriggeredComputation(triggeredBy, analysis.analyzeMethod)
    analysis
  }

  override def derivesEagerly: Set[PropertyBounds] = Set.empty

  override def derivesCollaboratively: Set[PropertyBounds] = Set(PropertyBounds.ub(AnalysisResultsLattice))
}
