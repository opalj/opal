/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ide.solver

import scala.collection.immutable

import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.fpcf.Entity
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.ide.integration.IDEPropertyMetaInformation
import org.opalj.ide.problem.IDEFact
import org.opalj.ide.problem.IDEValue
import org.opalj.ide.util.Logging

/**
 * A proxy for IDE analyses that accepts analysis requests for callables as well as statement-callable combinations.
 * The [[IDEAnalysis]] solver runs on callables only and additionally produces results for each statement of that
 * callable. This proxy analysis reduces all analysis requests to the callable and then forward it to the actual IDE
 * solver.
 */
class IDEAnalysisProxy[Fact <: IDEFact, Value <: IDEValue, Statement, Callable <: Entity](
    val project:                 SomeProject,
    val propertyMetaInformation: IDEPropertyMetaInformation[Fact, Value, Statement, Callable]
) extends FPCFAnalysis with Logging.ByProjectConfig {
    /**
     * @param entity either only a callable or a pair of callable and statement that should be analyzed (if no statement
     *               is given, the result for all exit statements is calculated)
     */
    def proxyAnalysis(entity: Entity): ProperPropertyComputationResult = {
        logInfo(s"proxying request to ${PropertyKey.name(propertyMetaInformation.key)} for $entity")

        val (callable, stmtOption) = entity match {
            case (c: Entity, s: Entity) => (c.asInstanceOf[Callable], Some(s.asInstanceOf[Statement]))
            case c                      => (c.asInstanceOf[Callable], None)
        }

        createResult(callable, stmtOption)
    }

    private def createResult(callable: Callable, stmtOption: Option[Statement]): ProperPropertyComputationResult = {
        val backingEOptionP = propertyStore(
            callable,
            propertyMetaInformation.backingPropertyMetaInformation.key
        )

        val entity = stmtOption match {
            case Some(statement) => (callable, statement)
            case None            => callable
        }

        if (backingEOptionP.isEPK) {
            // In this case, the analysis has not been called yet
            InterimPartialResult(
                immutable.Set(backingEOptionP),
                onDependeeUpdateContinuation(callable, stmtOption)
            )
        } else if (backingEOptionP.isFinal) {
            Result(
                entity,
                propertyMetaInformation.createProperty(
                    stmtOption match {
                        case Some(statement) => backingEOptionP.ub.stmtResults.getOrElse(statement, immutable.Set.empty)
                        case None            => backingEOptionP.ub.callableResults
                    }
                )
            )
        } else if (backingEOptionP.hasUBP) {
            InterimResult.forUB(
                entity,
                propertyMetaInformation.createProperty(
                    stmtOption match {
                        case Some(statement) => backingEOptionP.ub.stmtResults.getOrElse(statement, immutable.Set.empty)
                        case None            => backingEOptionP.ub.callableResults
                    }
                ),
                immutable.Set(backingEOptionP),
                onDependeeUpdateContinuation(callable, stmtOption)
            )
        } else {
            throw new IllegalStateException(s"Expected a final or interim EPS but got $backingEOptionP!")
        }
    }

    private def onDependeeUpdateContinuation(
        callable:   Callable,
        stmtOption: Option[Statement]
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        createResult(callable, stmtOption)
    }
}
