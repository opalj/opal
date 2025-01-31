/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ide.integration

import scala.collection.immutable

import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.fpcf.Entity
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.ide.problem.IDEFact
import org.opalj.ide.problem.IDEValue
import org.opalj.ide.solver.IDEAnalysisProxy

/**
 * Base scheduler to schedule the proxy analysis that is used to access the IDE analysis results
 */
trait BaseIDEAnalysisProxyScheduler[Fact <: IDEFact, Value <: IDEValue, Statement, Callable <: Entity]
    extends FPCFAnalysisScheduler {
    val propertyMetaInformation: IDEPropertyMetaInformation[Fact, Value, Statement, Callable]

    override type InitializationData = IDEAnalysisProxy[Fact, Value, Statement, Callable]

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    override def requiredProjectInformation: ProjectInformationKeys = Seq(PropertyStoreKey)

    override def init(project: SomeProject, ps: PropertyStore): IDEAnalysisProxy[Fact, Value, Statement, Callable] = {
        new IDEAnalysisProxy[Fact, Value, Statement, Callable](project, propertyMetaInformation)
    }

    override def uses: Set[PropertyBounds] =
        immutable.Set(PropertyBounds.ub(propertyMetaInformation.backingPropertyMetaInformation))

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(p: SomeProject, ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}
}
