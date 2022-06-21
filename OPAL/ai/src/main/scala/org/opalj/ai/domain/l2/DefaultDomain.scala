/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l2

import org.opalj.br.Method
import org.opalj.br.analyses.Project

class DefaultDomain[Source](
        project:                            Project[Source],
        method:                             Method,
        val frequentEvaluationWarningLevel: Int,
        val maxCallChainLength:             Int
) extends SharedDefaultDomain[Source](project, method)
    with PerformInvocationsWithRecursionDetection
    with RecordCFG
    with TheMemoryLayout {
    callingDomain =>

    type CalledMethodDomain = ChildDefaultDomain[Source]

    def this(project: Project[Source], method: Method) =
        this(project, method, 256, 2)

    final val coordinatingDomain = new CoordinatingValuesDomain(project)

    def calledMethodAI = BaseAI

    def calledMethodDomain(method: Method) = {
        new ChildDefaultDomain(project, method, callingDomain, maxCallChainLength - 1)
    }

    def shouldInvocationBePerformed(method: Method): Boolean = {
        maxCallChainLength > 0 && !method.returnType.isVoidType
    }

    // (HERE)
    // It has to be lazy, because we need the "MemoryLayout" which is set by the
    // abstract interpreter before the first evaluation of an instruction (and,
    // hence, before the first usage of the CalledMethodsStore by the AI)
    lazy val calledMethodsStore: CalledMethodsStore { val domain: coordinatingDomain.type } = {
        val operands =
            localsArray(0).foldLeft(List.empty[DomainValue])((l, n) =>
                if (n ne null) n :: l else l)
        CalledMethodsStore(
            coordinatingDomain, callingDomain.frequentEvaluationWarningLevel
        )(
            method, mapOperands(operands, coordinatingDomain)
        )
    }

}

class ChildDefaultDomain[Source](
        project:                Project[Source],
        method:                 Method,
        val callerDomain:       PerformInvocationsWithRecursionDetection { type CalledMethodDomain = ChildDefaultDomain[Source] },
        val maxCallChainLength: Int
) extends SharedDefaultDomain[Source](project, method)
    with ChildPerformInvocationsWithRecursionDetection
    with DefaultRecordMethodCallResults { callingDomain =>

    type CalledMethodDomain = callerDomain.CalledMethodDomain

    final def calledMethodAI: AI[_ >: CalledMethodDomain] = callerDomain.calledMethodAI

    def shouldInvocationBePerformed(method: Method): Boolean = {
        maxCallChainLength > 0 && !method.returnType.isVoidType
    }

    def calledMethodDomain(method: Method) = {
        new ChildDefaultDomain(project, method, callingDomain, maxCallChainLength - 1)
    }

}
