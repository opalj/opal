/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties

import org.opalj.fpcf.analyses.ifds.Statement
import org.opalj.tac.DUVar
import org.opalj.value.KnownTypedValue

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