/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package axa
package bridge

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
import org.opalj.xl.axa.common.AdaptorLattice
import org.opalj.xl.axa.common.CrossLanguageCall
import org.opalj.xl.axa.common.Language
import org.opalj.xl.axa.common.NoCrossLanguageCall

class JavaScriptBridge(val project: SomeProject) extends FPCFAnalysis {

  def analyzeMethod(method: Method): ProperPropertyComputationResult = {

    val result = propertyStore(method, AdaptorLattice.key)

    val analysisResult: ProperPropertyComputationResult = result.ub match {

      case CrossLanguageCall(Language.JavaScript, code, assignments) =>
        //TODO Translation process
        Result(method, JavaScriptCall(code, assignments))

      case NoCrossLanguageCall => Result(method, NoJavaScriptCall)

      case x => throw new Exception(s" $x is not the right property")
    }
    analysisResult
  }
}

object TriggeredJavaScriptBridgeScheduler extends BasicFPCFTriggeredAnalysisScheduler {
  override def requiredProjectInformation: ProjectInformationKeys = Seq()

  override def uses: Set[PropertyBounds] = Set(PropertyBounds.ub(AdaptorLattice))

  override def triggeredBy: PropertyKey[AdaptorLattice] = AdaptorLattice.key

  override def register(
      p: SomeProject,
      ps: PropertyStore,
      unused: Null
  ): JavaScriptBridge = {
    val analysis = new JavaScriptBridge(p)
    ps.registerTriggeredComputation(triggeredBy, analysis.analyzeMethod)
    analysis
  }

  override def derivesEagerly: Set[PropertyBounds] = Set(PropertyBounds.ub(JavaScriptBridgeLattice))

  override def derivesCollaboratively: Set[PropertyBounds] = Set.empty
}
