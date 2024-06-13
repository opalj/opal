/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.alias

import org.opalj.br.ReferenceType

trait TypeBasedAliasAnalysis extends SetBasedAliasAnalysis {

    override protected[this] type AliasElementType = ReferenceType
    override protected[this] type AliasSet = TypeBasedAliasSet
    override protected[this] type AnalysisState <: TypeBasedAliasAnalysisState

}
