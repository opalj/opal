/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses

import java.net.URL

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.analyses.EagerClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.EagerL0FieldImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.EagerL0PurityAnalysis
import org.opalj.br.fpcf.analyses.EagerTypeImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.EagerUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.br.fpcf.properties.DeepImmutableField
import org.opalj.br.fpcf.properties.DependentImmutableField
import org.opalj.br.fpcf.properties.MutableField
import org.opalj.br.fpcf.properties.ShallowImmutableField
import org.opalj.tac.fpcf.analyses.EagerL0ReferenceImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.EagerL1FieldMutabilityAnalysis
import org.opalj.tac.fpcf.analyses.TACAITransformer
import org.opalj.tac.fpcf.analyses.escape.EagerSimpleEscapeAnalysis

/**
 * Runs the EagerL0FieldImmutabilityAnalysis including analysis needed for improving the result.
 *
 * @author Tobias Peter Roth
 */
object FieldImmutabilityAnalysisDemo extends ProjectAnalysisApplication {

  override def title: String = "runs the EagerL0FieldImmutabilityAnalysis"

  override def description: String =
    "runs the EagerL0FieldImmutabilityAnalysis"

  override def doAnalyze(
      project: Project[URL],
      parameters: Seq[String],
      isInterrupted: () => Boolean
  ): BasicReport = {
    val result = analyze(project)
    BasicReport(result)
  }

  def analyze(project: Project[URL]): String = {
    val analysesManager = project.get(FPCFAnalysesManagerKey)

    val (propertyStore, _) = analysesManager.runAll(
      EagerTypeImmutabilityAnalysis,
      EagerUnsoundPrematurelyReadFieldsAnalysis,
      EagerClassImmutabilityAnalysis,
      EagerL0ReferenceImmutabilityAnalysis,
      EagerL0PurityAnalysis,
      EagerL1FieldMutabilityAnalysis,
      EagerSimpleEscapeAnalysis,
      TACAITransformer,
      EagerL0FieldImmutabilityAnalysis
    );

    "Mutable Fields: " + propertyStore
      .finalEntities(MutableField)
      .toList
      .toString() + "\n" +
      "Shallow Immutable Fields: " + propertyStore
      .finalEntities(ShallowImmutableField)
      .toList
      .toString() + "\n" +
      "Dependet Immutable Fields:" + propertyStore
      .finalEntities(DependentImmutableField)
      .toList
      .toString() + "\n" +
      "Deep Immutable Fields: " + propertyStore
      .finalEntities(DeepImmutableField)
      .toList
      .toString()
  }
}
