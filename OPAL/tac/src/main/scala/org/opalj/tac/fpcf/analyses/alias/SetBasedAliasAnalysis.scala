/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.alias

import org.opalj.br.fpcf.properties.alias.AliasSourceElement
import org.opalj.br.fpcf.properties.alias.MayAlias
import org.opalj.br.fpcf.properties.alias.MustAlias
import org.opalj.br.fpcf.properties.alias.NoAlias
import org.opalj.fpcf.ProperPropertyComputationResult

trait SetBasedAliasAnalysis extends AbstractAliasAnalysis {

    protected[this] type AliasElementType
    protected[this] type AliasSet <: AliasSetLike[AliasElementType, AliasSet]

    override protected[this] type AnalysisState <: SetBasedAliasAnalysisState[AliasElementType, AliasSet]

    override protected[this] def createResult()(
        implicit
        state:   AnalysisState,
        context: AnalysisContext
    ): ProperPropertyComputationResult = {

        val pointsTo1 = state.pointsTo1
        val pointsTo2 = state.pointsTo2

        if (pointsTo1.pointsToAny || pointsTo2.pointsToAny) return result(MayAlias)

        val intersections = pointsTo1.findTwoIntersections(pointsTo2)

        intersections match {
            case (None, None) /* no intersection */ =>
                if (state.hasDependees) interimResult(NoAlias, MayAlias) else result(NoAlias)
            case (Some(intersection), None) /* exactly one intersection */ if checkMustAlias(intersection) =>
                if (state.hasDependees) interimResult(MustAlias, MayAlias) else result(MustAlias)
            case _ /*  at least two intersections */ =>
                result(MayAlias)
        }
    }

    /**
     * Checks if the current analysis state allows for a [[MustAlias]] relation between the two elements. It assumes
     * the the given element is the only intersection between the two points-to sets.
     *
     * This method always returns false and should be overriden if more precise must alias checks can be performed.
     *
     * @param intersectingElement The only between the two points-to sets.
     * @return `true` if the two elements can be a [[MustAlias]], `false` otherwise.
     */
    protected[this] def checkMustAlias(intersectingElement: AliasElementType)(implicit
        state:   AnalysisState,
        context: AnalysisContext
    ): Boolean = false
}

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