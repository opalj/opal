/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses

import java.net.URL

import org.opalj.tac.fpcf.analyses.EagerLxClassImmutabilityAnalysis_new
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.analyses.LazyClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyL0FieldImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.br.fpcf.properties.ClassImmutability_new
import org.opalj.br.fpcf.properties.DeepImmutableClass
import org.opalj.br.fpcf.properties.MutableClass
import org.opalj.br.fpcf.properties.ShallowImmutableClass
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.LazyL0ReferenceImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.LazyL2FieldMutabilityAnalysis
import org.opalj.tac.fpcf.analyses.LazyLxTypeImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis;

/**
 * Runs the EagerLxClassImmutabilityAnalysis_new as well as analysis needed for improving the result
 *
 * @author Tobias Peter Roth
 */
object ClassImmutabilityAnalysisDemo extends ProjectAnalysisApplication {

  override def title: String = "run EagerLxClassImmutabilityAnalysis_new"

  override def description: String = "run EagerLxClassImmutabilityAnalysis_new"

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

    analysesManager.project.get(RTACallGraphKey)

    val (propertyStore, _) = analysesManager.runAll(
      //LazyTypeImmutabilityAnalysis,
      LazyUnsoundPrematurelyReadFieldsAnalysis,
      LazyL2PurityAnalysis,
      LazyL2FieldMutabilityAnalysis,
      LazyClassImmutabilityAnalysis,
      LazyL0ReferenceImmutabilityAnalysis,
      LazyL0FieldImmutabilityAnalysis,
      LazyLxTypeImmutabilityAnalysis_new,
      EagerLxClassImmutabilityAnalysis_new
    )

    "Mutable Class: " + propertyStore
      .finalEntities(MutableClass)
      .toList
      .toString() + "\n" +
      "Dependent Immutable Class: " + propertyStore
      .entities(ClassImmutability_new.key)
      .toList
      .toString() + "\n" +
      "Shallow Immutable Class: " + propertyStore
      .finalEntities(ShallowImmutableClass)
      .toList
      .toString() + "\n" +
      "Deep Immutable Class: " + propertyStore
      .finalEntities(DeepImmutableClass)
      .toList
      .toString() + "\n"
  }
}
