/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.alias

import org.opalj.br.ReferenceType

trait TypeBasedAliasAnalysis extends SetBasedAliasAnalysis {

    override protected[this] type AliasElementType = ReferenceType
    override protected[this] type AliasSet = TypeBasedAliasSet
    override protected[this] type AnalysisState <: TypeBasedAliasAnalysisState

}

/**
 * Encapsulates the current state of an alias analysis that uses an [[TypeBasedAliasSet]] to store the
 * allocations sites to which each of the alias elements can point to.
 */
class TypeBasedAliasAnalysisState extends SetBasedAliasAnalysisState[ReferenceType, TypeBasedAliasSet] {

    override protected[this] def createAliasSet(): TypeBasedAliasSet = new TypeBasedAliasSet
}
