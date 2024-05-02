/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.alias

import org.opalj.br.ReferenceType

/**
 * Encapsulates the current state of an alias analysis that uses an [[AllocationSiteBasedAliasSet]] to store the
 * allocations sites to which each of the alias elements can point to.
 */
class TypeBasedAliasAnalysisState extends SetBasedAliasAnalysisState[ReferenceType, TypeBasedAliasSet] {

    override protected[this] def createAliasSet(): TypeBasedAliasSet = new TypeBasedAliasSet
}
