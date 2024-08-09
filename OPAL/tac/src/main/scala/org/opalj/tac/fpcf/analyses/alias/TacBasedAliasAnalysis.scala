/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package alias

import org.opalj.ai.domain.l1.DefaultDomainWithCFGAndDefUse
import org.opalj.ai.fpcf.analyses.L0BaseAIResultAnalysis
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.Method
import org.opalj.br.fpcf.properties.alias.MayAlias
import org.opalj.br.fpcf.properties.alias.NoAlias
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.graphs.{DominatorTree, PostDominatorTree}
import org.opalj.log.LogContext
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

/**
 * Encapsulates the state of a TAC-based alias analysis.
 *
 * It additionally contains the TACAI for the first and second alias source element if they are method bound and
 * provides a way to calculate and cache the dominator and postDominator tree of the methods.
 */
trait TacBasedAliasAnalysisState extends AliasAnalysisState {

    private[this] var _tacai1: Option[TACode[TACMethodParameter, V]] = None
    private[this] var _tacai2: Option[TACode[TACMethodParameter, V]] = None

    private[this] var _dominatorTree1: Option[DominatorTree] = None
    private[this] var _dominatorTree2: Option[DominatorTree] = None

    private[this] var _postDominatorTree1: Option[PostDominatorTree] = None
    private[this] var _postDominatorTree2: Option[PostDominatorTree] = None

    private[this] var _tacEPSToMethod: Map[SomeEPS, Method] = Map()

    /**
     * Updates the TACAI for the given method.
     */
    private[alias] def updateTACAI(
        m:     Method,
        tacai: TACode[TACMethodParameter, V]
    )(implicit context: AliasAnalysisContext): Unit = {

        var anyMatch: Boolean = false

        if (context.element1.isMethodBound && m.equals(context.element1.method)) {
            _tacai1 = Some(tacai)
            anyMatch = true
        }

        if (context.element2.isMethodBound && m.equals(context.element2.method)) {
            _tacai2 = Some(tacai)
            anyMatch = true
        }

        if (!anyMatch) throw new IllegalArgumentException("Method not found")
    }

    /**
     * Returns the TACAI for the first alias source element.
     *
     * If the element is not method bound, `None` is returned.
     *
     * @return The TACAI for the first alias source element.
     */
    def tacai1: Option[TACode[TACMethodParameter, V]] = _tacai1

    /**
     * Returns the TACAI for the second alias source element.
     *
     * If the element is not method bound, `None` is returned.
     *
     * @return The TACAI for the second alias source element.
     */
    def tacai2: Option[TACode[TACMethodParameter, V]] = _tacai2

    /**
     * Associates the given TAC EPS with the given method.
     */
    def addTacEPSToMethod(eps: SomeEPS, m: Method): Unit = {
        _tacEPSToMethod += (eps -> m)
    }

    /**
     * Returns the method that is represented by the given TAC EPS.
     */
    def getMethodForTacEPS(eps: SomeEPS): Method = {
        _tacEPSToMethod(eps)
    }

    /**
     * Returns the [[DominatorTree]] for the given method. It caches its results for future invocations to avoid
     * multiple calculations of the same dominator tree.
     */
    def dominatorTree(m: Method)(implicit context: AliasAnalysisContext): DominatorTree = {

        if (m.equals(context.element1.method)) {
            _dominatorTree1.getOrElse({
                val dominatorTree = _tacai1.get.cfg.dominatorTree
                _dominatorTree1 = Some(dominatorTree)
                dominatorTree
            })
        } else {
          _dominatorTree2.getOrElse({
              val dominatorTree = _tacai2.get.cfg.dominatorTree
              _dominatorTree2 = Some(dominatorTree)
              dominatorTree
          })
        }
    }

    /**
     * Returns the [[PostDominatorTree]] for the given method. It caches its results for future invocations to avoid
     * multiple calculations of the same post dominator tree.
     */
    def postDominatorTree(m: Method)(implicit context: AliasAnalysisContext, logContext: LogContext): PostDominatorTree = {

        if (m.equals(context.element1.method)) {
            _postDominatorTree1.getOrElse({
                val aiResult =
                  L0BaseAIResultAnalysis.performAI(m)(context.project.get(AIDomainFactoryKey), logContext)
                val postDomTree = aiResult.domain.asInstanceOf[DefaultDomainWithCFGAndDefUse[_]].postDominatorTree
                _postDominatorTree1 = Some(postDomTree)
                postDomTree
            })
        } else {
            _postDominatorTree2.getOrElse({
                val aiResult =
                  L0BaseAIResultAnalysis.performAI(m)(context.project.get(AIDomainFactoryKey), logContext)
                val postDomTree = aiResult.domain.asInstanceOf[DefaultDomainWithCFGAndDefUse[_]].postDominatorTree
                _postDominatorTree2 = Some(postDomTree)
                postDomTree
            })
        }

    }

}
