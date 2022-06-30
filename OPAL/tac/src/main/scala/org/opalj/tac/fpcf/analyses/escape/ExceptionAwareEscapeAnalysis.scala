/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package escape

import org.opalj.tac.common.DefinitionSiteLike
import org.opalj.br.DefinedMethod
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.EscapeViaAbnormalReturn

/**
 * Handling for exceptions, that are allocated within the current method.
 * Only if the throw stmt leads to an abnormal return in the cfg, the escape state is decreased
 * down to [[org.opalj.br.fpcf.properties.EscapeViaAbnormalReturn]]. Otherwise (the exception
 * is caught) the escape state remains the same.
 *
 * @author Florian Kuebler
 */
trait ExceptionAwareEscapeAnalysis extends AbstractEscapeAnalysis {

    override protected[this] def handleThrow(
        aThrow: Throw[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (state.usesDefSite(aThrow.exception)) {
            val tacai = state.tacai.get
            val index = tacai.stmts indexWhere {
                _ == aThrow
            }
            val successors = tacai.cfg.bb(index).successors

            var isCaught = false
            var abnormalReturned = false
            for (pc <- successors) {
                if (pc.isCatchNode) {
                    val exceptionType = context.entity match {
                        case (_: Context, defSite: DefinitionSiteLike) =>
                            val Assignment(_, left, _) = tacai.stmts.find(_.pc == defSite.pc).get
                            classHierarchy.joinReferenceTypesUntilSingleUpperBound(
                                left.value.asReferenceValue.upperTypeBound
                            )
                        case (_: Context, VirtualFormalParameter(dm: DefinedMethod, -1)) =>
                            dm.definedMethod.classFile.thisType
                        case (_: Context, VirtualFormalParameter(callee, origin)) =>
                            // we would not end in this case if the parameter is not an object
                            callee.descriptor.parameterTypes(-2 - origin).asObjectType
                    }
                    pc.asCatchNode.catchType match {
                        case Some(catchType) =>
                            if (classHierarchy.isSubtypeOf(exceptionType, catchType))
                                isCaught = true
                        case None =>
                    }
                } else if (pc.isAbnormalReturnExitNode) {
                    abnormalReturned = true
                }
            }
            if (abnormalReturned && !isCaught) {
                state.meetMostRestrictive(EscapeViaAbnormalReturn)
            }
        }
    }
}
