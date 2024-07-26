/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ide.integration

import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.fpcf.Entity
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.ide.problem.IDEFact
import org.opalj.ide.problem.IDEValue
import org.opalj.ide.solver.IDEAnalysis

/**
 * A base scheduler for IDE analyses adding common default behavior
 */
abstract class IDEAnalysisScheduler[Fact <: IDEFact, Value <: IDEValue, Statement, Callable <: Entity]
    extends FPCFLazyAnalysisScheduler {
    override final type InitializationData = IDEAnalysis[Fact, Value, Statement, Callable]

    def property: IDEPropertyMetaInformation[Fact, Value]

    override def derivesLazily: Some[PropertyBounds] = Some(PropertyBounds.ub(property))

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def register(
        project:       SomeProject,
        propertyStore: PropertyStore,
        analysis:      IDEAnalysis[Fact, Value, Statement, Callable]
    ): FPCFAnalysis = {
        propertyStore.registerLazyPropertyComputation(property.key, analysis.performAnalysis)
        analysis
    }

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(p: SomeProject, ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}
}
