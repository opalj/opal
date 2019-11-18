/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses

import java.net.URL

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.analyses.EagerL0FieldImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyTypeImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.br.fpcf.properties.DeepImmutableField
import org.opalj.br.fpcf.properties.DependentImmutableField
import org.opalj.br.fpcf.properties.MutableField
import org.opalj.br.fpcf.properties.ShallowImmutableField
import org.opalj.tac.fpcf.analyses.LazyL0ReferenceImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.LazyL2FieldMutabilityAnalysis
import org.opalj.tac.fpcf.analyses.LazyLxTypeImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis

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
      /**
       * LazyTypeImmutabilityAnalysis,
       * LazyUnsoundPrematurelyReadFieldsAnalysis,
       * LazyClassImmutabilityAnalysis,
       * LazyL0ReferenceImmutabilityAnalysis,
       * LazyL0PurityAnalysis,
       * LazyL1FieldMutabilityAnalysis,
       * LazySimpleEscapeAnalysis,
       * TACAITransformer,
       * LazyLxTypeImmutabilityAnalysis_new,
       * EagerL0FieldImmutabilityAnalysis*
       */
      LazyTypeImmutabilityAnalysis,
      LazyUnsoundPrematurelyReadFieldsAnalysis,
      LazyL2PurityAnalysis,
      LazyL2FieldMutabilityAnalysis,
      LazyClassImmutabilityAnalysis,
      LazyL0ReferenceImmutabilityAnalysis,
      EagerL0FieldImmutabilityAnalysis,
      LazyLxTypeImmutabilityAnalysis_new
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
