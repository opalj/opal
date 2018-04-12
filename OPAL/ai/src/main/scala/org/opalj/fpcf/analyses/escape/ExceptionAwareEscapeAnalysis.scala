/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package fpcf
package analyses
package escape

import org.opalj.ai.DefinitionSite
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
                        case defSite: DefinitionSite ⇒
                            val Assignment(_, left, _) = context.code.find(_.pc == defSite.pc).get
                            classHierarchy.joinReferenceTypesUntilSingleUpperBound(
                                left.value.asDomainReferenceValue.upperTypeBound
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