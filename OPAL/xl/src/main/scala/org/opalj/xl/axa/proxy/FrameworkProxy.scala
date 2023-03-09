/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package axa
package proxy

import org.opalj.br.Method
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFTriggeredAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.SystemProperties
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.xl.analyses.javaAnalysis.adaptor.CrossLanguageCall
import org.opalj.xl.analyses.javaAnalysis.adaptor.JavaJavaScriptAdaptorLattice
import org.opalj.xl.analyses.javascript.analyses.tajs.TAJSProxy

class FrameworkProxy(val project: SomeProject) extends FPCFAnalysis {

  def analyzeCrossLanguageCalls(method: Method): ProperPropertyComputationResult = {

    val result = propertyStore(method, JavaJavaScriptAdaptorLattice.key)

    result.ub match {
      case CrossLanguageCall("JavaScript", code, assignments) =>
        TAJSProxy.analyze(code)
      case _ =>
    }

    Result(method, FinalAnalysisResult)
  }
}

object TriggeredFrameworkProxyScheduler extends BasicFPCFTriggeredAnalysisScheduler {
  override def requiredProjectInformation: ProjectInformationKeys =
    Seq()

  override def uses: Set[PropertyBounds] = Set(
    PropertyBounds.ub(JavaJavaScriptAdaptorLattice),
  )
  override def triggeredBy: PropertyKey[JavaJavaScriptAdaptorLattice] = JavaJavaScriptAdaptorLattice.key

  override def register(
      p: SomeProject,
      ps: PropertyStore,
      unused: Null
  ) = {
    val analysis = new FrameworkProxy(p)
    ps.registerTriggeredComputation(triggeredBy, analysis.analyzeCrossLanguageCalls)
    analysis
  }

  override def derivesEagerly: Set[PropertyBounds] = Set.empty

  override def derivesCollaboratively: Set[PropertyBounds] = Set(
    PropertyBounds.ub(SystemProperties)
  )
}
