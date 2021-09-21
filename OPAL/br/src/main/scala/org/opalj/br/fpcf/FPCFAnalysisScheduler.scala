/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf

import java.util.concurrent.atomic.AtomicInteger

import org.opalj.fpcf.ComputationSpecification
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject

/**
 * Specification of the properties of an analysis.
 *
 * @note   It is possible to use an analysis that directly uses the property store and
 *         an analysis that uses this factory infrastructure at the same time.
 *
 * @author Michael Reif
 * @author Michael Eichberg
 */
trait FPCFAnalysisScheduler extends ComputationSpecification[FPCFAnalysis] {

    /**
     * Returns all [[org.opalj.br.analyses.ProjectInformationKey]]s required by the analyses.
     *
     * This information is in particular required by keys which - when the key is computed - make use of
     * other keys which are not statically known at compile time. If a single key that is (transitively)
     * used is not correctly listed, a deadlock will _always_ occur.
     */
    def requiredProjectInformation: ProjectInformationKeys

    /**
     * The unique id of this factory.
     *
     * Every factory for a specific analysis is automatically associated with a unique id.
     */
    final val uniqueId: Int = FPCFAnalysisScheduler.nextId

    final override def init(ps: PropertyStore): InitializationData = {
        init(ps.context(classOf[SomeProject]), ps)
    }

    final override def uses(ps: PropertyStore): Set[PropertyBounds] = {
        uses ++ uses(ps.context(classOf[SomeProject]), ps)
    }

    final override def beforeSchedule(ps: PropertyStore): Unit = {
        beforeSchedule(ps.context(classOf[SomeProject]), ps)
    }

    final override def afterPhaseCompletion(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {
        afterPhaseCompletion(ps.context(classOf[SomeProject]), ps, analysis)
    }

    def init(p: SomeProject, ps: PropertyStore): InitializationData

    /** The uses that are configuration (project) dependent. */
    def uses(p: SomeProject, ps: PropertyStore): Set[PropertyBounds] = Set.empty

    /** The uses that are configuration independent. */
    def uses: Set[PropertyBounds]

    def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit

    def afterPhaseCompletion(p: SomeProject, ps: PropertyStore, analysis: FPCFAnalysis): Unit

}

/**
 * Companion object of [[org.opalj.br.fpcf.FPCFAnalysisScheduler]] that defines internal helper
 * functions and values.
 *
 * @author Michael Reif
 */
private[fpcf] object FPCFAnalysisScheduler {

    private[this] val idGenerator: AtomicInteger = new AtomicInteger(0)

    private[FPCFAnalysisScheduler] def nextId: Int = idGenerator.getAndIncrement()

}
