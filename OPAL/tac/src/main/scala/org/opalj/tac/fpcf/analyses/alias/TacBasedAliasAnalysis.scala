/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package alias

import org.opalj.br.Method
import org.opalj.br.fpcf.properties.alias.MayAlias
import org.opalj.br.fpcf.properties.alias.NoAlias
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.tac.fpcf.properties.TACAI

/**
 * A base trait for all alias analyses based on the TACAI.
 */
trait TacBasedAliasAnalysis extends AbstractAliasAnalysis {

    override protected[this] type AnalysisState <: TacBasedAliasAnalysisState

    override def doDetermineAlias(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): ProperPropertyComputationResult = {

        if (context.element1.isMethodBound) retrieveTAC(context.element1.method)
        if (context.element2.isMethodBound) retrieveTAC(context.element2.method)

        if (bothTacaisDefined) analyzeTAC()
        else interimResult(NoAlias, MayAlias)
    }

    /**
     * Computes the alias relation of the [[org.opalj.br.fpcf.properties.alias.AliasEntity]] using the TAC representation
     * of the corresponding methods.
     *
     * This method is called when the TACs of the methods of both elements are available. If an element is not method
     * bound, it is not considered.
     *
     * @return The result of the computation.
     */
    protected[this] def analyzeTAC()(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): ProperPropertyComputationResult

    /**
     * Retrieves the TACAI for the given method.
     */
    private[this] def retrieveTAC(
        m: Method
    )(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): Unit = {
        val tacai: EOptionP[Method, TACAI] = propertyStore(m, TACAI.key)

        state.addTacEPSToMethod(tacai.asEPS, m)

        if (tacai.isRefinable) {
            state.addDependency(tacai)
        }

        if (tacai.hasUBP && tacai.ub.tac.isDefined) {
            state.updateTACAI(m, tacai.ub.tac.get)
        }
    }

    /**
     * Continues the computation when a TACAI property is updated.
     */
    override protected[this] def continuation(
        someEPS: SomeEPS
    )(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): ProperPropertyComputationResult = {
        someEPS match {
            case UBP(ub: TACAI) =>
                state.removeDependency(someEPS)

                if (someEPS.isRefinable) state.addDependency(someEPS)
                if (ub.tac.isDefined) state.updateTACAI(state.getMethodForTacEPS(someEPS), ub.tac.get)

                if (bothTacaisDefined) analyzeTAC()
                else InterimResult(context.entity, NoAlias, MayAlias, state.getDependees, continuation)
            case _ =>
                throw new UnknownError(s"unhandled property (${someEPS.ub} for ${someEPS.e}")
        }
    }

    /**
     * @return `true` if both TACs are defined. If one of the elements is not method bound, it is not considered.
     */
    private[this] def bothTacaisDefined(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): Boolean = {
        (!context.element1.isMethodBound || state.tacai1.isDefined) &&
        (!context.element2.isMethodBound || state.tacai2.isDefined)
    }

}
