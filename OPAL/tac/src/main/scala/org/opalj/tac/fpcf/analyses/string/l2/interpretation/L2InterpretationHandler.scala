/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package l2
package interpretation

import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.ContextProviderKey
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.tac.fpcf.analyses.string.interpretation.InterpretationState
import org.opalj.tac.fpcf.analyses.string.l1.interpretation.L1InterpretationHandler
import org.opalj.tac.fpcf.properties.string.StringFlowFunctionProperty

/**
 * @inheritdoc
 *
 * Interprets statements similar to [[org.opalj.tac.fpcf.analyses.string.l1.interpretation.L1InterpretationHandler]] but
 * handles virtual function calls using the call graph.
 *
 * @note This level can be expanded to handle all function calls via the call graph, not just virtual ones.
 *
 * @author Maximilian Rüsch
 */
class L2InterpretationHandler(implicit override val project: SomeProject) extends L1InterpretationHandler {

    implicit val contextProvider: ContextProvider = p.get(ContextProviderKey)

    override protected def processStatement(implicit
        state: InterpretationState
    ): Stmt[V] => ProperPropertyComputationResult = {

        case stmt: FieldWriteAccessStmt[V] =>
            StringInterpreter.computeFinalResult(StringFlowFunctionProperty.identityForVariableAt(
                stmt.pc,
                stmt.value.asVar.toPersistentForm(using state.tac.stmts)
            ))

        case stmt @ AssignmentLikeStmt(_, expr: VirtualFunctionCall[V]) =>
            new L2VirtualFunctionCallInterpreter().interpretExpr(stmt.asAssignmentLike, expr)

        // IMPROVE add call-graph based interpreters for other call types than virtual function calls to L2

        case stmt => super.processStatement(using state)(stmt)
    }
}

object L2InterpretationHandler {

    def requiredProjectInformation: ProjectInformationKeys = Seq(ContextProviderKey)

    def uses: Set[PropertyBounds] = L1InterpretationHandler.uses ++ PropertyBounds.ubs(Callees)

    def apply(project: SomeProject): L2InterpretationHandler = new L2InterpretationHandler()(using project)
}
