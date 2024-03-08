/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l1
package interpretation

import scala.collection.mutable.ListBuffer

import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.fpcf.analyses.string_analysis.StringInterpreter

/**
 * @author Maximilian Rüsch
 */
trait L1StringInterpreter[State <: L1ComputationState] extends StringInterpreter[State] {

    /**
     * This function returns all methods for a given `pc` among a set of `declaredMethods`. The
     * second return value indicates whether at least one method has an unknown body (if `true`,
     * then there is such a method).
     */
    protected def getMethodsForPC(pc: Int)(
        implicit
        state:           State,
        ps:              PropertyStore,
        contextProvider: ContextProvider
    ): (List[Method], Boolean) = {
        var hasMethodWithUnknownBody = false
        val methods = ListBuffer[Method]()

        state.callees.callees(state.methodContext, pc).map(_.method).foreach {
            case definedMethod: DefinedMethod => methods.append(definedMethod.definedMethod)
            case _                            => hasMethodWithUnknownBody = true
        }

        (methods.sortBy(_.classFile.fqn).toList, hasMethodWithUnknownBody)
    }
}
