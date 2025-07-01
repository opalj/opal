/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.fpcf.properties.Context

/**
 * @author Maximilian Rüsch
 */
package object string {

    type TAC = TACode[TACMethodParameter, V]

    /**
     * Internal entity for the string analysis that represents a local variable in a context-free setting. Influences
     * from method parameters are thus NOT yet resolved in the corresponding property.
     *
     * @param pc Program counter the variable's value is requested for
     * @param pv Persistent representation of the variable in question
     * @param m The method in which the variable and program counter reside
     */
    private[string] case class VariableDefinition(pc: Int, pv: PV, m: Method)

    /**
     * External entity for the string analysis that represents a local variable in a context-sensitive setting.
     * Influences from method parameters ARE resolved in the corresponding property.
     *
     * @param pc Program counter the variable's value is requested for
     * @param pv Persistent representation of the variable in question
     * @param context The context the variable and program counter are to be interpreted in
     */
    case class VariableContext(pc: Int, pv: PV, context: Context) {
        def m: Method = context.method.definedMethod
    }

    /**
     * Internal entity for the string analysis that represents a method parameter in a context-sensitive setting to
     * determine the parameter value in that context.
     *
     * @param index Index of the parameter in question (zero-based index into the params array)
     * @param context The context the parameter is to be interpreted in
     */
    private[string] case class MethodParameterContext(index: Int, context: Context) {
        def m: Method = context.method.definedMethod
    }

    /**
     * Internal entity for the string flow analysis that represents a specific point in a method for which a flow
     * function is requested.
     */
    case class MethodPC(pc: Int, dm: DefinedMethod)
}
