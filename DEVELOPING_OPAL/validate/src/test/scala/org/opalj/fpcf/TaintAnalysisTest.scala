/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf
import java.net.URL

import org.opalj.ai.domain.l2
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.analyses.{DeclaredMethodsKey, Project}
import org.opalj.fpcf.properties.taint.TaintedFlow
import org.opalj.tac.fpcf.analyses.{LazyTACAIProvider, TaintAnalysis}

class TaintAnalysisTest extends PropertiesTest {

  override def init(p: Project[URL]): Unit = {
    p.updateProjectInformationKeyInitializationData(
      AIDomainFactoryKey,
      (_: Option[Set[Class[_ <: AnyRef]]]) ⇒
        Set[Class[_ <: AnyRef]](classOf[l2.DefaultPerformInvocationsDomainWithCFGAndDefUse[URL]])
    )
  }

  describe("") {
    val testContext = executeAnalyses(Set(LazyTACAIProvider, TaintAnalysis))
    val project = testContext.project
    val declaredMethods = project.get(DeclaredMethodsKey)
    val eas = methodsWithAnnotations(project).map(x ⇒ ((declaredMethods(x._1), null), x._2, x._3))
    testContext.propertyStore.shutdown()
    validateProperties(testContext, eas, Set(TaintedFlow.PROPERTY_VALIDATOR_KEY))
  }
}
