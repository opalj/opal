/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package properties
package string

import org.opalj.br.fpcf.properties.string.StringTreeInvalidElement
import org.opalj.br.fpcf.properties.string.StringTreeNode
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation

/**
 * @author Maximilian Rüsch
 */
sealed trait StringFlowFunctionPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = StringFlowFunctionProperty
}

case class StringFlowFunctionProperty(
    webs: Set[PDUWeb],
    flow: StringFlowFunction
) extends Property
    with StringFlowFunctionPropertyMetaInformation {

    final def key: PropertyKey[StringFlowFunctionProperty] = StringFlowFunctionProperty.key
}

object StringFlowFunctionProperty extends StringFlowFunctionPropertyMetaInformation {

    private final val propertyName = "opalj.StringFlowFunction"

    override val key: PropertyKey[StringFlowFunctionProperty] = PropertyKey.create(propertyName)

    def apply(web: PDUWeb, flow: StringFlowFunction): StringFlowFunctionProperty =
        StringFlowFunctionProperty(Set(web), flow)

    def apply(pc: Int, pv: PV, flow: StringFlowFunction): StringFlowFunctionProperty =
        StringFlowFunctionProperty(PDUWeb(pc, pv), flow)

    def ub: StringFlowFunctionProperty = constForAll(StringTreeInvalidElement)

    def identity: StringFlowFunctionProperty =
        StringFlowFunctionProperty(Set.empty[PDUWeb], (env: StringTreeEnvironment) => env)

    // Helps to register notable variable usage / definition which does not modify the current state
    def identityForVariableAt(pc: Int, v: PV): StringFlowFunctionProperty =
        StringFlowFunctionProperty(pc, v, (env: StringTreeEnvironment) => env)

    def lb(pc: Int, v: PV): StringFlowFunctionProperty =
        constForVariableAt(pc, v, StringTreeNode.lb)

    def ub(pc: Int, v: PV): StringFlowFunctionProperty =
        constForVariableAt(pc, v, StringTreeInvalidElement)

    def constForVariableAt(pc: Int, v: PV, result: StringTreeNode): StringFlowFunctionProperty =
        StringFlowFunctionProperty(pc, v, (env: StringTreeEnvironment) => env.update(pc, v, result))

    def constForAll(result: StringTreeNode): StringFlowFunctionProperty =
        StringFlowFunctionProperty(Set.empty[PDUWeb], ConstForAllFlow(result))
}

trait StringFlowFunction extends (StringTreeEnvironment => StringTreeEnvironment)

case class ConstForAllFlow(result: StringTreeNode) extends StringFlowFunction {

    def apply(env: StringTreeEnvironment): StringTreeEnvironment = env.updateAll(result)
}
