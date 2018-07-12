/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l2

import org.opalj.br.Method
import org.opalj.br.analyses.Project

/**
 * Performs a simple invocation of the immediately called methods.
 */
class DefaultPerformInvocationsDomain[Source](
        project: Project[Source],
        method:  Method
) extends SharedDefaultDomain[Source](project, method) with PerformInvocations {

    def shouldInvocationBePerformed(method: Method): Boolean = !method.returnType.isVoidType

    type CalledMethodDomain = SharedDefaultDomain[Source] with DefaultRecordMethodCallResults

    def calledMethodDomain(method: Method) = {
        new SharedDefaultDomain(project, method) with DefaultRecordMethodCallResults
    }

    def calledMethodAI = BaseAI

}

class DefaultPerformInvocationsDomainWithCFG[Source](
        project: Project[Source],
        method:  Method
) extends DefaultPerformInvocationsDomain[Source](project, method)
    with RecordCFG

class DefaultPerformInvocationsDomainWithCFGAndDefUse[Source](
        project: Project[Source],
        method:  Method
) extends DefaultPerformInvocationsDomainWithCFG[Source](project, method)
    with RefineDefUseUsingOrigins
