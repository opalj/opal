/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ide.solver

import scala.collection.immutable

import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EPK
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.ide.integration.IDEPropertyMetaInformation
import org.opalj.ide.problem.IDEFact
import org.opalj.ide.problem.IDEValue

/**
 * A proxy for IDE analyses that accepts analysis requests for callables as well as statement-callable combinations.
 * The [[IDEAnalysis]] solver runs on callables only and additionally produces results for each statement of that
 * callable. This proxy analysis reduces all analysis requests to the callable and then forward it to the actual IDE
 * solver.
 */
class IDEAnalysisProxy[Fact <: IDEFact, Value <: IDEValue, Statement, Callable <: Entity](
    val project:                 SomeProject,
    val propertyMetaInformation: IDEPropertyMetaInformation[Fact, Value]
) extends FPCFAnalysis {
    /**
     * @param entity either only a callable or a pair of callable and statement that should be analyzed (if no statement
     *               is given, the result for all exit statements is calculated)
     */
    def proxyAnalysis(entity: Entity): ProperPropertyComputationResult = {
        println(s"Proxying request to ${PropertyKey.name(propertyMetaInformation.key)} for $entity")

        val (callable, stmt) = entity match {
            case (c: Entity, s: Entity) => (c.asInstanceOf[Callable], Some(s.asInstanceOf[Statement]))
            case c                      => (c.asInstanceOf[Callable], None)
        }

        createCoarseResult(callable, stmt)
    }

    private def createCoarseResult(callable: Callable, stmt: Option[Statement]): ProperPropertyComputationResult = {
        val eOptionP = propertyStore(callable, propertyMetaInformation.backingPropertyMetaInformation.key)
        eOptionP match {
            case _: EPK[Callable, ?] =>
                // In this case, the analysis has not been called yet
                InterimPartialResult(
                    immutable.Set(eOptionP),
                    onDependeeUpdateContinuationCoarse(callable, stmt)
                )
            case _ =>
                // In this case, some kind of result is present (for the callable, as well as for each statement)
                createFineResult(callable, stmt)
        }
    }

    private def onDependeeUpdateContinuationCoarse(
        callable: Callable,
        stmt:     Option[Statement]
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        createCoarseResult(callable, stmt)
    }

    private def createFineResult(callable: Callable, stmt: Option[Statement]): ProperPropertyComputationResult = {
        val eOptionP = propertyStore(
            stmt match {
                case Some(statement) => (callable, statement)
                case None            => callable
            },
            propertyMetaInformation.backingPropertyMetaInformation.key
        )
        if (eOptionP.isEPK) {
            InterimPartialResult(
                immutable.Set(eOptionP),
                onDependeeUpdateContinuationFine(callable, stmt)
            )
        } else if (eOptionP.isFinal) {
            Result(eOptionP.e, propertyMetaInformation.createProperty(eOptionP.ub.results))
        } else if (eOptionP.hasUBP) {
            InterimResult.forUB(
                eOptionP.e,
                propertyMetaInformation.createProperty(eOptionP.ub.results),
                immutable.Set(eOptionP),
                onDependeeUpdateContinuationFine(callable, stmt)
            )
        } else {
            throw new IllegalStateException(s"Expected a final or interim EPS but got $eOptionP!")
        }
    }

    private def onDependeeUpdateContinuationFine(
        callable: Callable,
        stmt:     Option[Statement]
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        createFineResult(callable, stmt)
    }
}
