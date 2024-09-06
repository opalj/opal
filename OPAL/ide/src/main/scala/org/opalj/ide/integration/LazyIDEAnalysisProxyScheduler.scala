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
import org.opalj.ide.solver.IDEAnalysisProxy

/**
 * A scheduler to (lazily) schedule the proxy analysis that is used to access the IDE analysis results
 */
class LazyIDEAnalysisProxyScheduler[Fact <: IDEFact, Value <: IDEValue, Statement, Callable <: Entity](
    val propertyMetaInformation: IDEPropertyMetaInformation[Fact, Value]
) extends BaseIDEAnalysisProxyScheduler[Fact, Value, Statement, Callable] with FPCFLazyAnalysisScheduler {
    def this(ideAnalysisScheduler: IDEAnalysisScheduler[Fact, Value, Statement, Callable]) = {
        this(ideAnalysisScheduler.propertyMetaInformation)
    }

    override def derivesLazily: Some[PropertyBounds] = Some(PropertyBounds.ub(propertyMetaInformation))

    override def register(
        project:       SomeProject,
        propertyStore: PropertyStore,
        analysis:      IDEAnalysisProxy[Fact, Value, Statement, Callable]
    ): FPCFAnalysis = {
        propertyStore.registerLazyPropertyComputation(propertyMetaInformation.key, analysis.proxyAnalysis)
        analysis
    }
}
