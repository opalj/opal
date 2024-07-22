/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string

import org.opalj.ai.ImmediateVMExceptionsOriginOffset
import org.opalj.br.Method
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.string.StringTreeDynamicString
import org.opalj.br.fpcf.properties.string.StringTreeInvalidElement
import org.opalj.br.fpcf.properties.string.StringTreeParameter
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EUBP
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.tac.fpcf.analyses.string.flowanalysis.DataFlowAnalysis
import org.opalj.tac.fpcf.analyses.string.flowanalysis.FlowGraph
import org.opalj.tac.fpcf.analyses.string.flowanalysis.Statement
import org.opalj.tac.fpcf.analyses.string.flowanalysis.StructuralAnalysis
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.string.MethodStringFlow
import org.opalj.tac.fpcf.properties.string.StringFlowFunctionProperty
import org.opalj.tac.fpcf.properties.string.StringTreeEnvironment

/**
 * @author Maximilian Rüsch
 */
class MethodStringFlowAnalysis(override val project: SomeProject) extends FPCFAnalysis {

    val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    def analyze(method: Method): ProperPropertyComputationResult = {
        val state = ComputationState(method, declaredMethods(method), ps(method, TACAI.key))

        if (state.tacDependee.isRefinable) {
            InterimResult.forUB(
                state.entity,
                MethodStringFlow.ub,
                Set(state.tacDependee),
                continuation(state)
            )
        } else if (state.tacDependee.ub.tac.isEmpty) {
            // No TAC available, e.g., because the method has no body
            Result(state.entity, MethodStringFlow.lb)
        } else {
            determinePossibleStrings(state)
        }
    }

    /**
     * Takes the `data` an analysis was started with as well as a computation `state` and determines
     * the possible string values. This method returns either a final [[Result]] or an
     * [[InterimResult]] depending on whether other information needs to be computed first.
     */
    private def determinePossibleStrings(implicit state: ComputationState): ProperPropertyComputationResult = {
        implicit val tac: TAC = state.tac

        state.flowGraph = FlowGraph(tac.cfg)
        val (_, superFlowGraph, controlTree) =
            StructuralAnalysis.analyze(state.flowGraph, FlowGraph.entry)
        state.superFlowGraph = superFlowGraph
        state.controlTree = controlTree
        state.flowAnalysis = new DataFlowAnalysis(state.controlTree, state.superFlowGraph)

        state.flowGraph.nodes.toOuter.foreach {
            case Statement(pc) if pc >= 0 =>
                state.updateDependee(pc, propertyStore(MethodPC(pc, state.dm), StringFlowFunctionProperty.key))

            case _ =>
        }

        computeResults
    }

    private def continuation(state: ComputationState)(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case FinalP(_: TACAI) if eps.pk.equals(TACAI.key) =>
                state.tacDependee = eps.asInstanceOf[FinalEP[Method, TACAI]]
                determinePossibleStrings(state)

            case EUBP(e: MethodPC, _: StringFlowFunctionProperty) if eps.pk.equals(StringFlowFunctionProperty.key) =>
                state.updateDependee(e.pc, eps.asInstanceOf[EOptionP[MethodPC, StringFlowFunctionProperty]])
                computeResults(state)

            case _ =>
                throw new IllegalArgumentException(s"Unknown EPS given in continuation: $eps")
        }
    }

    private def computeResults(implicit state: ComputationState): ProperPropertyComputationResult = {
        if (state.hasDependees) {
            getInterimResult(state)
        } else {
            Result(state.entity, computeNewUpperBound(state))
        }
    }

    private def getInterimResult(state: ComputationState): InterimResult[MethodStringFlow] = {
        InterimResult.forUB(
            state.entity,
            computeNewUpperBound(state),
            state.dependees.toSet,
            continuation(state)
        )
    }

    private def computeNewUpperBound(state: ComputationState): MethodStringFlow = {
        val startEnv = StringTreeEnvironment(state.getWebs.map { web: PDUWeb =>
            val defPCs = web.defPCs.toList.sorted
            if (defPCs.head >= 0) {
                (web, StringTreeInvalidElement)
            } else {
                val pc = defPCs.head
                if (pc == -1 || pc <= ImmediateVMExceptionsOriginOffset) {
                    (web, StringTreeDynamicString)
                } else {
                    (web, StringTreeParameter.forParameterPC(pc))
                }
            }
        }.toMap)

        MethodStringFlow(state.flowAnalysis.compute(state.getFlowFunctionsByPC)(startEnv))
    }
}
