/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis

import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.SomeFinalEP
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler

/**
 * @author Maximilian Rüsch
 */
trait StringInterpreter {

    type T <: ASTNode[V]

    /**
     * @param instr   The instruction that is to be interpreted.
     * @param pc The pc that corresponds to the given instruction.
     * @return A [[ProperPropertyComputationResult]] for the given pc containing the interpretation of the given
     *         instruction.
     */
    def interpret(instr: T, pc: Int)(implicit state: ComputationState): ProperPropertyComputationResult

    def computeFinalResult(pc: Int, sci: StringConstancyInformation)(implicit state: ComputationState): Result =
        StringInterpreter.computeFinalResult(pc, sci)

    // IMPROVE remove this since awaiting all final is not really feasible
    // replace with intermediate lattice result approach
    protected final def awaitAllFinalContinuation(
        depender:    EPSDepender[T, ComputationState],
        finalResult: Seq[SomeFinalEP] => ProperPropertyComputationResult
    )(result: SomeEPS): ProperPropertyComputationResult = {
        if (result.isFinal) {
            val updatedDependees = depender.dependees.updated(
                depender.dependees.indexWhere(_.e.asInstanceOf[Entity] == result.e.asInstanceOf[Entity]),
                result
            )

            if (updatedDependees.forall(_.isFinal)) {
                finalResult(updatedDependees.asInstanceOf[Seq[SomeFinalEP]])
            } else {
                InterimResult.forLB(
                    InterpretationHandler.getEntityFromDefSitePC(depender.pc)(depender.state),
                    StringConstancyProperty.lb,
                    depender.dependees.toSet,
                    awaitAllFinalContinuation(depender.withDependees(updatedDependees), finalResult)
                )
            }
        } else {
            InterimResult.forLB(
                InterpretationHandler.getEntityFromDefSitePC(depender.pc)(depender.state),
                StringConstancyProperty.lb,
                depender.dependees.toSet,
                awaitAllFinalContinuation(depender, finalResult)
            )
        }
    }
}

object StringInterpreter {

    def computeFinalResult[State <: ComputationState](pc: Int, sci: StringConstancyInformation)(implicit
        state: State
    ): Result =
        Result(FinalEP(InterpretationHandler.getEntityFromDefSitePC(pc), StringConstancyProperty(sci)))
}

trait ParameterEvaluatingStringInterpreter extends StringInterpreter {

    val ps: PropertyStore

    protected def getParametersForPC(pc: Int)(implicit state: ComputationState): Seq[Expr[V]] = {
        state.tac.stmts(state.tac.pcToIndex(pc)) match {
            case ExprStmt(_, vfc: FunctionCall[V])     => vfc.params
            case Assignment(_, _, fc: FunctionCall[V]) => fc.params
            case _                                     => Seq.empty
        }
    }

    protected def evaluateParameters(params: Seq[Expr[V]])(implicit
        state: ComputationState
    ): Seq[Seq[EOptionP[DefSiteEntity, StringConstancyProperty]]] = {
        params.map { nextParam =>
            Seq.from(nextParam.asVar.definedBy.toArray.sorted.map { ds =>
                ps(InterpretationHandler.getEntityFromDefSite(ds), StringConstancyProperty.key)
            })
        }
    }
}
