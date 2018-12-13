/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import java.util.concurrent.atomic.AtomicInteger

import org.opalj.br.analyses.SomeProject

/**
 * Provides the generic infrastructure that is implemented by all factories for
 * FPCF analyses.
 * Analyses that are created using this factory will then be run using the [[PropertyStore]].
 * I.e., this trait is typically implemented by the singleton object that facilitates
 * the creation of analyses.
 *
 * @note   It is possible to use an analysis that directly uses the property store and
 *         an analysis that uses this factory infrastructure at the same time.
 *
 * @author Michael Reif
 * @author Michael Eichberg
 */
trait AbstractFPCFAnalysisScheduler extends ComputationSpecification {

    /**
     * The unique id of this factory.
     *
     * Every factory for a specific analysis is automatically associated with a unique id.
     */
    final val uniqueId: Int = AbstractFPCFAnalysisScheduler.nextId

    final override def init(ps: PropertyStore): InitializationData = {
        init(ps.context(classOf[SomeProject]), ps)
    }

    final override def beforeSchedule(ps: PropertyStore): Unit = {
        beforeSchedule(ps.context(classOf[SomeProject]), ps)
    }

    final override def afterPhaseCompletion(ps: PropertyStore): Unit = {
        afterPhaseCompletion(ps.context(classOf[SomeProject]), ps)
    }

    def init(p: SomeProject, ps: PropertyStore): InitializationData

    def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit

    def afterPhaseCompletion(p: SomeProject, ps: PropertyStore): Unit

}

/**
 * Companion object of [[AbstractFPCFAnalysisScheduler]] that defines internal helper functions and
 * values.
 *
 * @author Michael Reif
 */
private[fpcf] object AbstractFPCFAnalysisScheduler {

    private[this] val idGenerator: AtomicInteger = new AtomicInteger(0)

    private[AbstractFPCFAnalysisScheduler] def nextId: Int = idGenerator.getAndIncrement()

}
