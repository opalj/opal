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

    final type Self = StringFlowFunctionProperty
}

case class StringFlowFunctionProperty private (
    webs: Set[PDUWeb],
    flow: StringFlowFunction
) extends Property
    with StringFlowFunctionPropertyMetaInformation {

    final def key: PropertyKey[StringFlowFunctionProperty] = StringFlowFunctionProperty.key
}

object StringFlowFunctionProperty extends StringFlowFunctionPropertyMetaInformation {

    private final val propertyName = "opalj.StringFlowFunction"

    override val key: PropertyKey[StringFlowFunctionProperty] = PropertyKey.create(propertyName)

    def apply(webs: Set[PDUWeb], flow: StringFlowFunction): StringFlowFunctionProperty = {
        new StringFlowFunctionProperty(
            webs.foldLeft(Seq.empty[PDUWeb]) { (reducedWebs, web) =>
                val index = reducedWebs.indexWhere(_.intersectsWith(web))
                if (index == -1)
                    reducedWebs :+ web
                else
                    reducedWebs.updated(index, reducedWebs(index).intersect(web))
            }.toSet,
            flow
        )
    }

    def apply(web: PDUWeb, flow: StringFlowFunction): StringFlowFunctionProperty =
        new StringFlowFunctionProperty(Set(web), flow)

    def apply(pc: Int, pv: PV, flow: StringFlowFunction): StringFlowFunctionProperty =
        StringFlowFunctionProperty(PDUWeb(pc, pv), flow)

    // TODO should this be the real bottom element?
    def ub: StringFlowFunctionProperty = constForAll(StringTreeNeutralElement)

    def identity: StringFlowFunctionProperty =
        StringFlowFunctionProperty(Set.empty[PDUWeb], IdentityFlow)

    def ub(pc: Int, v: PV): StringFlowFunctionProperty =
        constForVariableAt(pc, v, StringTreeNeutralElement)

    def lb(pc: Int, v: PV): StringFlowFunctionProperty =
        constForVariableAt(pc, v, StringTreeDynamicString)

    def noFlow(pc: Int, v: PV): StringFlowFunctionProperty =
        constForVariableAt(pc, v, StringTreeInvalidElement)

    def constForVariableAt(pc: Int, v: PV, result: StringTreeNode): StringFlowFunctionProperty =
        StringFlowFunctionProperty(pc, v, (env: StringTreeEnvironment) => env.update(pc, v, result))

    def constForAll(result: StringTreeNode): StringFlowFunctionProperty =
        StringFlowFunctionProperty(Set.empty[PDUWeb], (env: StringTreeEnvironment) => env.updateAll(result))
}

trait StringFlowFunction extends (StringTreeEnvironment => StringTreeEnvironment)

object IdentityFlow extends StringFlowFunction {

    override def apply(env: StringTreeEnvironment): StringTreeEnvironment = env
}
