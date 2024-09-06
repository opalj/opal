/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ide.integration

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
import org.opalj.ide.problem.IDEValue
import org.opalj.ide.solver.IDEAnalysisProxy

/**
 * A scheduler to schedule the proxy analysis that is used to access the IDE analysis results
 */
class IDEAnalysisProxyScheduler[Fact <: IDEFact, Value <: IDEValue, Statement, Callable <: Entity](
    propertyMetaInformation: IDEPropertyMetaInformation[Fact, Value]
) extends FPCFLazyAnalysisScheduler {
    override final type InitializationData = IDEAnalysisProxy[Fact, Value, Statement, Callable]

    def this(ideAnalysisScheduler: IDEAnalysisScheduler[Fact, Value, Statement, Callable]) = {
        this(ideAnalysisScheduler.propertyMetaInformation)
    }

    override def derivesLazily: Some[PropertyBounds] = Some(PropertyBounds.ub(propertyMetaInformation))

    override def requiredProjectInformation: ProjectInformationKeys = Seq(PropertyStoreKey)

    override def init(project: SomeProject, ps: PropertyStore): IDEAnalysisProxy[Fact, Value, Statement, Callable] = {
        new IDEAnalysisProxy[Fact, Value, Statement, Callable](project, propertyMetaInformation)
    }

    override def uses: Set[PropertyBounds] =
        immutable.Set(PropertyBounds.ub(propertyMetaInformation.backingPropertyMetaInformation))

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def register(
        project:       SomeProject,
        propertyStore: PropertyStore,
        analysis:      IDEAnalysisProxy[Fact, Value, Statement, Callable]
    ): FPCFAnalysis = {
        propertyStore.registerLazyPropertyComputation(propertyMetaInformation.key, analysis.proxyAnalysis)
        analysis
    }

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(p: SomeProject, ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}
}
