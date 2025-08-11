/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ide
package integration

import scala.collection.immutable.Set

import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FPCFAnalysis
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.PropertyStoreKey
import org.opalj.fpcf.SomeEOptionP
import org.opalj.ide.problem.IDEFact
import org.opalj.ide.problem.IDEValue
import org.opalj.ide.solver.IDEAnalysisProxy

/**
 * Base scheduler to schedule the proxy analysis that is used to access the IDE analysis results.
 *
 * @author Robin Körkemeier
 */
trait BaseIDEAnalysisProxyScheduler[Fact <: IDEFact, Value <: IDEValue, Statement, Callable <: Entity]
    extends FPCFAnalysisScheduler {
    val propertyMetaInformation: IDEPropertyMetaInformation[Fact, Value, Statement, Callable]

    override type InitializationData = IDEAnalysisProxy[Fact, Value, Statement, Callable]

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    override def requiredProjectInformation: ProjectInformationKeys = Seq(PropertyStoreKey)

    override def init(
        project:       SomeProject,
        propertyStore: PropertyStore
    ): IDEAnalysisProxy[Fact, Value, Statement, Callable] = {
        new IDEAnalysisProxy[Fact, Value, Statement, Callable](project, propertyMetaInformation)
    }

    override def uses: Set[PropertyBounds] =
        Set(PropertyBounds.ub(propertyMetaInformation.backingPropertyMetaInformation))

    override def beforeSchedule(project: SomeProject, propertyStore: PropertyStore): Unit = {
        /* Add initial result for target callables */
        propertyStore.handleResult(
            PartialResult(
                propertyMetaInformation,
                propertyMetaInformation.targetCallablesPropertyMetaInformation.key,
                { (_: SomeEOptionP) => None }
            )
        )
    }

    override def afterPhaseScheduling(propertyStore: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(
        project:       SomeProject,
        propertyStore: PropertyStore,
        analysis:      FPCFAnalysis
    ): Unit = {}
}
