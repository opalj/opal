/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

import java.net.URL

import org.opalj.ai.domain.l2
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.analyses.LazyClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyL0FieldImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyTypeImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.EagerLxClassImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.LazyL0ReferenceImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.LazyL2FieldMutabilityAnalysis
import org.opalj.tac.fpcf.analyses.LazyLxTypeImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis

/**
 * @author Tobias Peter Roth
 */
class ClassImmutabilityTests extends PropertiesTest {

  override def init(p: Project[URL]): Unit = {
    p.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) { _ =>
      Set[Class[_ <: AnyRef]](classOf[l2.DefaultPerformInvocationsDomainWithCFGAndDefUse[URL]])
    }
    p.get(RTACallGraphKey)
  }

  /**
   * describe("no analysis is scheduled") {
   * val as = executeAnalyses(Set.empty)
   * as.propertyStore.shutdown()
   * validateProperties(as, fieldsWithAnnotations(as.project), Set("ClassImmutability_new"))
   * }*
   */
  describe("the org.opalj.fpcf.analyses.LxClassImmutabilityAnalysis is executed") {
    println(1)
    val as = executeAnalyses(
      Set(
        LazyTypeImmutabilityAnalysis,
        LazyUnsoundPrematurelyReadFieldsAnalysis,
        LazyL2PurityAnalysis,
        LazyL2FieldMutabilityAnalysis,
        LazyClassImmutabilityAnalysis,
        LazyL0ReferenceImmutabilityAnalysis,
        LazyL0FieldImmutabilityAnalysis,
        LazyLxTypeImmutabilityAnalysis_new,
        EagerLxClassImmutabilityAnalysis_new
      )
    )
    as.propertyStore.shutdown()
    validateProperties(
      as,
      classFilesWithAnnotations(as.project).map(tp => (tp._1.thisType, tp._2, tp._3)),
      Set("ClassImmutability_new")
    )
  }
  /**
   * describe("the org.opalj.fpcf.analyses.L1FieldMutabilityAnalysis is executed") {
   * val as = executeAnalyses(
   * Set(
   * EagerL1FieldMutabilityAnalysis,
   * LazyUnsoundPrematurelyReadFieldsAnalysis,
   * LazyInterProceduralEscapeAnalysis
   * )
   * )
   * as.propertyStore.shutdown()
   * validateProperties(as, fieldsWithAnnotations(as.project), Set("FieldMutability"))
   * }*
   */
  /**
 * describe("the org.opalj.fpcf.analyses.L2FieldMutabilityAnalysis is executed") {
 * val as = executeAnalyses(
 * Set(
 * EagerL2FieldMutabilityAnalysis,
 * LazyUnsoundPrematurelyReadFieldsAnalysis,
 * LazyL2PurityAnalysis,
 * LazyInterProceduralEscapeAnalysis
 * )
 * )
 * as.propertyStore.shutdown()
 * validateProperties(as, fieldsWithAnnotations(as.project), Set("FieldMutability"))
 * } *
 */
}
