/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.alias

import org.opalj.br.fpcf.properties.alias.AliasSourceElement

/**
 * Encapsulates the current state of an alias analysis that uses an [[AliasSetLike]] to store the elements
 * that an [[AliasSourceElement]] can point to.
 *
 * It additionally stores and handles an [[AliasSetLike]] for each [[AliasSourceElement]] and provides methods for
 * interacting with it.
 */
trait SetBasedAliasAnalysisState[ElementType, AliasSet <: AliasSetLike[ElementType, AliasSet]]
    extends AliasAnalysisState {

    private[this] val _pointsTo1: AliasSet = createAliasSet()
    private[this] val _pointsTo2: AliasSet = createAliasSet()

    /**
     * @return The current [[AliasSetLike]] for the first [[AliasSourceElement]].
     */
    def pointsTo1: AliasSet = _pointsTo1

    /**
     * @return The current [[AliasSetLike]] for the second [[AliasSourceElement]].
     */
    def pointsTo2: AliasSet = _pointsTo2

    /**
     * adds the given element set to the [[AliasSetLike]] of the given [[AliasSourceElement]].
     */
    def addPointsTo(ase: AliasSourceElement, element: ElementType)(
        implicit aliasContext: AliasAnalysisContext
    ): Unit = {
        if (aliasContext.isElement1(ase)) {
            _pointsTo1.addPointsTo(element)
        } else {
            _pointsTo2.addPointsTo(element)
        }
    }

    /**
     * Marks that the given [[AliasSourceElement]] can point to any arbitrary element
     */
    def setPointsToAny(ase: AliasSourceElement)(
        implicit context: AliasAnalysisContext
    ): Unit = {
        if (context.isElement1(ase)) {
            _pointsTo1.setPointsToAny()
        } else {
            _pointsTo2.setPointsToAny()
        }
    }

    /**
     * Creates a new [[AliasSetLike]] of the used type
     *
     * @return
     */
    protected[this] def createAliasSet(): AliasSet

}
