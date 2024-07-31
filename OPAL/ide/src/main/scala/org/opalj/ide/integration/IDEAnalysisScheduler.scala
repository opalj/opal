/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ide.integration

import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.fpcf.Entity
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyStore
import org.opalj.ide.problem.IDEFact
import org.opalj.ide.problem.IDEValue
import org.opalj.ide.solver.IDEAnalysis
import org.opalj.ide.solver.IDEAnalysisProxy

/**
 * A base scheduler for IDE analyses adding common default behavior
 */
abstract class IDEAnalysisScheduler[Fact <: IDEFact, Value <: IDEValue, Statement, Callable <: Entity]
    extends FPCFLazyAnalysisScheduler {
    override final type InitializationData = IDEAnalysis[Fact, Value, Statement, Callable]

    def property: IDEPropertyMetaInformation[Fact, Value]

    override def derivesLazily: Some[PropertyBounds] = Some(PropertyBounds.ub(property))

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    private lazy val backingPropertyKey: PropertyKey[IDEPropertyMetaInformation[Fact, Value]#Self] = {
        PropertyKey.create(s"${PropertyKey.name(property.key)}_Backing")
    }

    override def register(
        project:       SomeProject,
        propertyStore: PropertyStore,
        analysis:      IDEAnalysis[Fact, Value, Statement, Callable]
    ): FPCFAnalysis = {
        propertyStore.registerLazyPropertyComputation(
            property.key,
            new IDEAnalysisProxy[Fact, Value, Statement, Callable](project, property, backingPropertyKey).proxyAnalysis
        )

        propertyStore.registerLazyPropertyComputation(backingPropertyKey, analysis.performAnalysis)

        analysis
    }

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(p: SomeProject, ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}
}
