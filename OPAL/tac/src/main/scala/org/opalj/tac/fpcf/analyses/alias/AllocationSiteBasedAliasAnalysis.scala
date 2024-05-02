/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.alias

trait AllocationSiteBasedAliasAnalysis extends SetBasedAliasAnalysis {

    override protected[this] type AliasSet = AllocationSiteBasedAliasSet
    override protected[this] type AnalysisState <: AllocationSiteBasedAliasAnalysisState

}
