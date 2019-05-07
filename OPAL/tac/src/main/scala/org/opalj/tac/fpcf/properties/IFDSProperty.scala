/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package properties

import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.value.KnownTypedValue
import org.opalj.tac.fpcf.analyses.Statement

trait IFDSPropertyMetaInformation[DataFlowFact] extends PropertyMetaInformation

abstract class IFDSProperty[DataFlowFact]
    extends Property
    with IFDSPropertyMetaInformation[DataFlowFact] {

    /** The type of the TAC domain. */
    type V = DUVar[KnownTypedValue]

    /**
     * Maps exits statements to the data flow facts, which hold after them.
     */
    def flows: Map[Statement, Set[DataFlowFact]]

    override def equals(that: Any): Boolean = that match {
        case other: IFDSProperty[DataFlowFact] ⇒ flows == other.flows
        case _                                 ⇒ false
    }

    override def hashCode(): Int = flows.hashCode()
}
