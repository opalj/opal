/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSetScala

class AllocationSiteBasedPointsToScalaAnalysis private[analyses] (
        final val project: SomeProject
) extends AbstractPointsToAnalysis with AllocationSiteBasedScalaAnalysis

object AllocationSiteBasedPointsToScalaAnalysisScheduler extends AbstractPointsToAnalysisScheduler {

    override val propertyKind: PropertyMetaInformation = AllocationSitePointsToSetScala
    override val createAnalysis: SomeProject ⇒ AllocationSiteBasedPointsToScalaAnalysis =
        new AllocationSiteBasedPointsToScalaAnalysis(_)
}
