/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package properties
package string

import org.opalj.br.fpcf.properties.string.StringTreeNode
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation

/**
 * An FPCF property that captures the string flow results of an entire method based on the final [[StringTreeEnvironment]]
 * after executing the [[org.opalj.tac.fpcf.analyses.string.flowanalysis.DataFlowAnalysis]].
 *
 * @see [[StringTreeEnvironment]]
 *
 * @author Maximilian Rüsch
 */
sealed trait MethodStringFlowPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = MethodStringFlow
}

case class MethodStringFlow(private val env: StringTreeEnvironment) extends Property
    with MethodStringFlowPropertyMetaInformation {

    final def key: PropertyKey[MethodStringFlow] = MethodStringFlow.key

    def apply(pc: Int, pv: PV): StringTreeNode = env.mergeAllMatching(pc, pv)
}

object MethodStringFlow extends MethodStringFlowPropertyMetaInformation {

    private final val propertyName = "opalj.MethodStringFlow"

    override val key: PropertyKey[MethodStringFlow] = PropertyKey.create(propertyName)

    def ub: MethodStringFlow = AllUBMethodStringFlow
    def lb: MethodStringFlow = AllLBMethodStringFlow
}

private object AllUBMethodStringFlow extends MethodStringFlow(StringTreeEnvironment(Map.empty)) {

    override def apply(pc: Int, pv: PV): StringTreeNode = StringTreeNode.ub
}

private object AllLBMethodStringFlow extends MethodStringFlow(StringTreeEnvironment(Map.empty)) {

    override def apply(pc: Int, pv: PV): StringTreeNode = StringTreeNode.lb
}
