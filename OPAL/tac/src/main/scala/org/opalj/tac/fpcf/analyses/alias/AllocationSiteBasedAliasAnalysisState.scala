/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.alias

import org.opalj.br.PC
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.alias.AliasSourceElement

/**
 * Encapsulates the current state of an alias analysis that uses an [[AllocationSiteBasedAliasSet]] to store the
 * allocations sites to which each of the alias elements can point to.
 */
class AllocationSiteBasedAliasAnalysisState
    extends SetBasedAliasAnalysisState[AllocationSite, AllocationSiteBasedAliasSet] {

    override protected[this] def createAliasSet(): AllocationSiteBasedAliasSet = new AllocationSiteBasedAliasSet

    def addPointsTo(ase: AliasSourceElement, context: Context, pc: PC)(
        implicit aliasContext: AliasAnalysisContext
    ): Unit = {
        addPointsTo(ase, (context, pc))
    }
}
