/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses
package escape

import org.opalj.ai.common.DefinitionSiteLike
import org.opalj.br.DefinedMethod
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.fpcf.properties.EscapeViaAbnormalReturn
import org.opalj.tac.Assignment
import org.opalj.tac.Throw

/**
 * Handling for exceptions, that are allocated within the current method.
 * Only if the throw stmt leads to an abnormal return in the cfg, the escape state is decreased
 * down to [[org.opalj.fpcf.properties.EscapeViaAbnormalReturn]]. Otherwise (the exception is caught)
 * the escape state remains the same.
 *
 * @author Florian Kuebler
 */
trait ExceptionAwareEscapeAnalysis extends AbstractEscapeAnalysis {

    override type AnalysisContext <: AbstractEscapeAnalysisContext with CFGContainer

    override protected[this] def handleThrow(
        aThrow: Throw[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (context.usesDefSite(aThrow.exception)) {
            val index = context.code indexWhere {
                _ == aThrow
            }
            val successors = context.cfg.bb(index).successors

            var isCaught = false
            var abnormalReturned = false
            for (pc ← successors) {
                if (pc.isCatchNode) {
                    val exceptionType = context.entity match {
                        case defSite: DefinitionSiteLike ⇒
                            val Assignment(_, left, _) = context.code.find(_.pc == defSite.pc).get
                            classHierarchy.joinReferenceTypesUntilSingleUpperBound(
                                left.value.asReferenceValue.upperTypeBound
                            )
                        case VirtualFormalParameter(DefinedMethod(_, callee), -1) ⇒
                            callee.classFile.thisType
                        case VirtualFormalParameter(callee, origin) ⇒
                            // we would not end in this case if the parameter is not an object
                            callee.descriptor.parameterTypes(-2 - origin).asObjectType
                    }
                    pc.asCatchNode.catchType match {
                        case Some(catchType) ⇒
                            if (classHierarchy.isSubtypeOf(exceptionType, catchType).isYes)
                                isCaught = true
                        case None ⇒
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
