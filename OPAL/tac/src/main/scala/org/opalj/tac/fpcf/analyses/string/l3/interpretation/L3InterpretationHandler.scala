/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package l3
package interpretation

import org.opalj.br.analyses.DeclaredFields
import org.opalj.br.analyses.DeclaredFieldsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.ContextProviderKey
import org.opalj.br.fpcf.properties.fieldaccess.FieldWriteAccessInformation
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.tac.fpcf.analyses.string.interpretation.InterpretationState
import org.opalj.tac.fpcf.analyses.string.l2.interpretation.L2InterpretationHandler
import org.opalj.tac.fpcf.properties.string.StringFlowFunctionProperty

/**
 * @inheritdoc
 *
 * Interprets statements similar to [[org.opalj.tac.fpcf.analyses.string.l2.interpretation.L2InterpretationHandler]] but
 * handles field read accesses as well.
 *
 * @author Maximilian Rüsch
 */
class L3InterpretationHandler(implicit override val project: SomeProject) extends L2InterpretationHandler {

    implicit val declaredFields: DeclaredFields = p.get(DeclaredFieldsKey)

    override protected def processStatement(implicit
        state: InterpretationState
    ): Stmt[V] => ProperPropertyComputationResult = {
        case stmt @ Assignment(_, _, expr: FieldRead[V]) =>
            new L3FieldReadInterpreter().interpretExpr(stmt, expr)
        // Field reads without result usage are irrelevant
        case ExprStmt(_, _: FieldRead[V]) =>
            StringInterpreter.computeFinalResult(StringFlowFunctionProperty.identity)

        case stmt => super.processStatement(using state)(stmt)
    }
}

object L3InterpretationHandler {

    def requiredProjectInformation: ProjectInformationKeys = Seq(DeclaredFieldsKey, ContextProviderKey)

    def uses: Set[PropertyBounds] = L2InterpretationHandler.uses ++ PropertyBounds.ubs(FieldWriteAccessInformation)

    def apply(project: SomeProject): L3InterpretationHandler = new L3InterpretationHandler()(using project)
}
