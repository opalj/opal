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
trait StringInterpreter[State <: ComputationState] {

    type T <: ASTNode[V]

    /**
     * @param instr   The instruction that is to be interpreted.
     * @param defSite The definition site that corresponds to the given instruction.
     * @return A [[ProperPropertyComputationResult]] for the given def site containing the interpretation of the given
     *         instruction.
     */
    def interpret(instr: T, defSite: Int)(implicit state: State): ProperPropertyComputationResult

    def computeFinalResult(finalEP: FinalEP[DefSiteEntity, StringConstancyProperty]): Result =
        StringInterpreter.computeFinalResult(finalEP)

    def computeFinalResult(defSite: Int, sci: StringConstancyInformation)(implicit state: State): Result =
        StringInterpreter.computeFinalResult(defSite, sci)

    // IMPROVE remove this since awaiting all final is not really feasible
    // replace with intermediate lattice result approach
    protected final def awaitAllFinalContinuation(
        depender:    EPSDepender[T, State],
        finalResult: Iterable[SomeFinalEP] => ProperPropertyComputationResult
    )(result: SomeEPS): ProperPropertyComputationResult = {
        if (result.isFinal) {
            val updatedDependees = depender.dependees.updated(
                depender.dependees.indexWhere(_.e.asInstanceOf[Entity] == result.e.asInstanceOf[Entity]),
                result
            )

            if (updatedDependees.forall(_.isFinal)) {
                finalResult(updatedDependees.asInstanceOf[Iterable[SomeFinalEP]])
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

    def computeFinalResult(finalEP: FinalEP[DefSiteEntity, StringConstancyProperty]): Result = Result(finalEP)

    def computeFinalResult[State <: ComputationState](defSite: Int, sci: StringConstancyInformation)(implicit
        state: State
    ): Result =
        computeFinalResult(FinalEP(InterpretationHandler.getEntityFromDefSite(defSite), StringConstancyProperty(sci)))
}

trait ParameterEvaluatingStringInterpreter[State <: ComputationState] extends StringInterpreter[State] {

    val ps: PropertyStore

    protected def getParametersForPC(pc: Int)(implicit state: State): Seq[Expr[V]] = {
        state.tac.stmts(state.tac.pcToIndex(pc)) match {
            case ExprStmt(_, vfc: FunctionCall[V])     => vfc.params
            case Assignment(_, _, fc: FunctionCall[V]) => fc.params
            case _                                     => Seq.empty
        }
    }

    protected def evaluateParameters(params: Seq[Expr[V]])(implicit
        state: State
    ): Seq[Seq[EOptionP[DefSiteEntity, StringConstancyProperty]]] = {
        params.map { nextParam =>
            Seq.from(nextParam.asVar.definedBy.toArray.sorted.map { ds =>
                ps(InterpretationHandler.getEntityFromDefSite(ds), StringConstancyProperty.key)
            })
        }
    }
}
