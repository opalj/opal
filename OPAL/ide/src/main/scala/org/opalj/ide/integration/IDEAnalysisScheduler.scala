/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ide
package integration

import scala.collection.immutable

import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.fpcf.Entity
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.ide.problem.IDEFact
import org.opalj.ide.problem.IDEProblem
import org.opalj.ide.problem.IDEValue
import org.opalj.ide.solver.ICFG
import org.opalj.ide.solver.IDEAnalysis

/**
 * A base scheduler for IDE analyses adding common default behavior.
 *
 * @author Robin Körkemeier
 */
abstract class IDEAnalysisScheduler[
    Fact <: IDEFact,
    Value <: IDEValue,
    Statement,
    Callable <: Entity,
    _ICFG <: ICFG[Statement, Callable]
] extends FPCFLazyAnalysisScheduler {
    override final type InitializationData = IDEAnalysis[Fact, Value, Statement, Callable]

    def propertyMetaInformation: IDEPropertyMetaInformation[Fact, Value, Statement, Callable]

    def createProblem(project: SomeProject, icfg: _ICFG): IDEProblem[Fact, Value, Statement, Callable]

    def createICFG(project: SomeProject): _ICFG

    override final def derivesLazily: Some[PropertyBounds] =
        Some(PropertyBounds.ub(propertyMetaInformation.backingPropertyMetaInformation))

    override def requiredProjectInformation: ProjectInformationKeys =
        Seq(PropertyStoreKey)

    override def uses: immutable.Set[PropertyBounds] =
        immutable.Set.empty

    override def beforeSchedule(project: SomeProject, propertyStore: PropertyStore): Unit = {}

    override final def init(
        project:       SomeProject,
        propertyStore: PropertyStore
    ): IDEAnalysis[Fact, Value, Statement, Callable] = {
        val icfg = createICFG(project)
        val problem = createProblem(project, icfg)
        new IDEAnalysis(project, problem, icfg, propertyMetaInformation)
    }

    override final def register(
        project:       SomeProject,
        propertyStore: PropertyStore,
        analysis:      IDEAnalysis[Fact, Value, Statement, Callable]
    ): FPCFAnalysis = {
        propertyStore.registerLazyPropertyComputation(
            propertyMetaInformation.backingPropertyMetaInformation.key,
            analysis.performAnalysis
        )
        analysis
    }

    override def afterPhaseScheduling(propertyStore: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(
        project:       SomeProject,
        propertyStore: PropertyStore,
        analysis:      FPCFAnalysis
    ): Unit = {}
}
