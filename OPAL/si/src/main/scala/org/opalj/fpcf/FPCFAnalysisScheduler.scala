/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import java.util.concurrent.atomic.AtomicInteger

import org.opalj.si.Project
import org.opalj.si.ProjectInformationKeys

/**
 * Specification of the properties of an analysis.
 *
 * @note   It is possible to use an analysis that directly uses the property store and
 *         an analysis that uses this factory infrastructure at the same time.
 *
 * @author Michael Reif
 * @author Michael Eichberg
 */
trait FPCFAnalysisScheduler[P <: Project] extends ComputationSpecification[FPCFAnalysis] {

    /**
     * Returns all [[org.opalj.si.ProjectInformationKey]]s required by the analyses.
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

    override final def init(ps: PropertyStore): InitializationData = {
        init(ps.context(classOf[Project]).asInstanceOf[P], ps)
    }

    override final def uses(ps: PropertyStore): Set[PropertyBounds] = {
        uses ++ uses(ps.context(classOf[Project]).asInstanceOf[P], ps)
    }

    override final def beforeSchedule(ps: PropertyStore): Unit = {
        beforeSchedule(ps.context(classOf[Project]).asInstanceOf[P], ps)
    }

    override final def afterPhaseCompletion(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {
        afterPhaseCompletion(ps.context(classOf[Project]).asInstanceOf[P], ps, analysis)
    }

    def init(p: P, ps: PropertyStore): InitializationData

    /** The uses that are configuration (project) dependent. */
    def uses(p: P, ps: PropertyStore): Set[PropertyBounds] = Set.empty

    /** The uses that are configuration independent. */
    def uses: Set[PropertyBounds]

    def beforeSchedule(p: P, ps: PropertyStore): Unit

    def afterPhaseCompletion(p: P, ps: PropertyStore, analysis: FPCFAnalysis): Unit

}

/**
 * Companion object of [[org.opalj.br.fpcf.FPCFAnalysisScheduler]] that defines internal helper
 * functions and values.
 *
 * @author Michael Reif
 */
private[fpcf] object FPCFAnalysisScheduler {

    private val idGenerator: AtomicInteger = new AtomicInteger(0)

    private[FPCFAnalysisScheduler] def nextId: Int = idGenerator.getAndIncrement()

}
