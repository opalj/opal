/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.alias.pointsto

import org.opalj.tac.fpcf.analyses.alias.AllocationSite
import org.opalj.tac.fpcf.analyses.alias.AllocationSiteBasedAliasAnalysisState
import org.opalj.tac.fpcf.analyses.alias.AllocationSiteBasedAliasSet

/**
 * The state class used by an [[AllocationSitePointsToBasedAliasAnalysis]].
 *
 * @see [[PointsToBasedAliasAnalysisState]]
 */
class AllocationSitePointsToBasedAliasAnalysisState extends AllocationSiteBasedAliasAnalysisState
    with PointsToBasedAliasAnalysisState[AllocationSite, AllocationSiteBasedAliasSet] {}
