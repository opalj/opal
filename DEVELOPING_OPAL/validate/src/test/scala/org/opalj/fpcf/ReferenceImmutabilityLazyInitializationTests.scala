/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

import java.net.URL

import org.opalj.ai.domain.l2
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.analyses.LazyUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.EagerL0ReferenceImmutabilityLazyInitializationAnalysis
import org.opalj.tac.fpcf.analyses.LazyL0ReferenceImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.LazyL2FieldMutabilityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis

/**
 * @author Tobias Peter Roth
 */
class ReferenceImmutabilityLazyInitializationTests extends PropertiesTest {

  override def init(p: Project[URL]): Unit = {
    p.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) { _ =>
      Set[Class[_ <: AnyRef]](classOf[l2.DefaultPerformInvocationsDomainWithCFGAndDefUse[URL]])
    }
    p.get(RTACallGraphKey)
  }

  describe("no analysis is scheduled") {
    val as = executeAnalyses(Set.empty)
    as.propertyStore.shutdown()
    validateProperties(
      as,
      fieldsWithAnnotations(as.project),
      Set("ReferenceImmutabilityLazyInitialization")
    )
  }

  describe("the org.opalj.fpcf.analyses.ReferenceImmutabilityLazyInitialization is executed") {
    val as = executeAnalyses(
      Set(
        EagerL0ReferenceImmutabilityLazyInitializationAnalysis,
        LazyL0ReferenceImmutabilityAnalysis,
        LazyL2FieldMutabilityAnalysis,
        LazyUnsoundPrematurelyReadFieldsAnalysis,
        LazyL2PurityAnalysis,
        LazyInterProceduralEscapeAnalysis
      )
    )
    as.propertyStore.shutdown()
    validateProperties(
      as,
      fieldsWithAnnotations(as.project),
      Set("ReferenceImmutabilityLazyInitialization")
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
