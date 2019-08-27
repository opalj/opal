/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSetScala

/**
 * Applies the impact of preconfigured methods to the points-to analysis.
 *
 * TODO: example
 * TODO: refer to the config file
 *
 * @author Florian Kuebler
 */
object ConfiguredMethodsPointsToScalaAnalysisScheduler
    extends ConfiguredMethodsPointsToAnalysisScheduler {

    override val propertyKind: PropertyMetaInformation = AllocationSitePointsToSetScala
    override val createAnalysis: SomeProject ⇒ ConfiguredMethodsPointsToAnalysis =
        new ConfiguredMethodsPointsToAnalysis(_) with AllocationSiteBasedScalaAnalysis
}
