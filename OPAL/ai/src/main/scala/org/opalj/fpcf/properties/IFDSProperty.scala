/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties

import org.opalj.fpcf.analyses.Statement
import org.opalj.value.KnownTypedValue
import org.opalj.tac.DUVar

trait IFDSPropertyMetaInformation[DataFlowFact] extends PropertyMetaInformation

abstract class IFDSProperty[DataFlowFact] extends Property
        with IFDSPropertyMetaInformation[DataFlowFact] {

    /** The type of the TAC domain. */
    type V = DUVar[KnownTypedValue]

    def flows: Map[Statement, Set[DataFlowFact]]

    override def equals(that: Any): Boolean = that match {
        case other: IFDSProperty[DataFlowFact] ⇒ flows == other.flows
        case _                                 ⇒ false
    }

    override def hashCode(): Int = flows.hashCode()
}