/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.alias.pointsto

import org.opalj.br.ReferenceType
import org.opalj.tac.fpcf.analyses.alias.TypeBasedAliasAnalysisState
import org.opalj.tac.fpcf.analyses.alias.TypeBasedAliasSet

/**
 * The state class used by an [[TypePointsToBasedAliasAnalysis]].
 *
 * @see [[PointsToBasedAliasAnalysisState]]
 */
class TypePointsToBasedAliasAnalysisState extends TypeBasedAliasAnalysisState
    with PointsToBasedAliasAnalysisState[ReferenceType, TypeBasedAliasSet] {}
