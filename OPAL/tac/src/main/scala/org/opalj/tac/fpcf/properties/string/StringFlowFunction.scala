/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package properties
package string

import org.opalj.br.fpcf.properties.string.StringTreeDynamicString
import org.opalj.br.fpcf.properties.string.StringTreeInvalidElement
import org.opalj.br.fpcf.properties.string.StringTreeNeutralElement
import org.opalj.br.fpcf.properties.string.StringTreeNode
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation

/**
 * @author Maximilian Rüsch
 */
sealed trait StringFlowFunctionPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = StringFlowFunction
}

trait StringFlowFunction extends (StringTreeEnvironment => StringTreeEnvironment)
    with Property
    with StringFlowFunctionPropertyMetaInformation {

    final def key: PropertyKey[StringFlowFunction] = StringFlowFunction.key
}

object StringFlowFunction extends StringFlowFunctionPropertyMetaInformation {

    private final val propertyName = "opalj.StringFlowFunction"

    override val key: PropertyKey[StringFlowFunction] = PropertyKey.create(propertyName)

    def ub: StringFlowFunction = ConstantResultFlow.forAll(StringTreeNeutralElement)
    def ub(v:     PV): StringFlowFunction = ConstantResultFlow.forVariable(v, StringTreeNeutralElement)
    def lb(v:     PV): StringFlowFunction = ConstantResultFlow.forVariable(v, StringTreeDynamicString)
    def noFlow(v: PV): StringFlowFunction = ConstantResultFlow.forVariable(v, StringTreeInvalidElement)
}

object ConstantResultFlow {
    def forAll(result: StringTreeNode): StringFlowFunction =
        (env: StringTreeEnvironment) => env.updateAll(result)

    def forVariable(v: PV, result: StringTreeNode): StringFlowFunction =
        (env: StringTreeEnvironment) => env.update(v, result)
}

object IdentityFlow extends StringFlowFunction {

    override def apply(env: StringTreeEnvironment): StringTreeEnvironment = env
}
