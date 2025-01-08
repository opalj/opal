/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ide.integration

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFEagerAnalysisScheduler
import org.opalj.fpcf.Entity
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.ide.problem.IDEFact
import org.opalj.ide.problem.IDEValue
import org.opalj.ide.solver.IDEAnalysisProxy

/**
 * A scheduler to (eagerly) schedule the proxy analysis that is used to access the IDE analysis results
 * @param methodProvider for which methods the results should be computed eagerly
 */
class EagerIDEAnalysisProxyScheduler[Fact <: IDEFact, Value <: IDEValue, Statement, Callable <: Entity](
    val propertyMetaInformation: IDEPropertyMetaInformation[Statement, Fact, Value],
    methodProvider:              SomeProject => Iterable[Method] = { project => project.allMethodsWithBody }
) extends BaseIDEAnalysisProxyScheduler[Fact, Value, Statement, Callable] with FPCFEagerAnalysisScheduler {
    def this(ideAnalysisScheduler: IDEAnalysisScheduler[Fact, Value, Statement, Callable, ?]) = {
        this(ideAnalysisScheduler.propertyMetaInformation)
    }

    def this(
        ideAnalysisScheduler: IDEAnalysisScheduler[Fact, Value, Statement, Callable, ?],
        methodProvider:       SomeProject => Iterable[Method]
    ) = {
        this(ideAnalysisScheduler.propertyMetaInformation, methodProvider)
    }

    override def derivesEagerly: Set[PropertyBounds] = Set(PropertyBounds.ub(propertyMetaInformation))

    override def start(
        project:       SomeProject,
        propertyStore: PropertyStore,
        analysis:      IDEAnalysisProxy[Fact, Value, Statement, Callable]
    ): FPCFAnalysis = {
        propertyStore.scheduleEagerComputationsForEntities(methodProvider(project))(analysis.proxyAnalysis)
        analysis
    }
}
