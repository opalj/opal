/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties

import org.opalj.fpcf.analyses.ifds.Statement
import org.opalj.tac.DUVar
import org.opalj.value.KnownTypedValue

trait IFDSPropertyMetaInformation[DataFlowFact] extends PropertyMetaInformation {
    def noFlowInformation: IFDSProperty[DataFlowFact]

}

abstract class IFDSProperty[DataFlowFact] extends Property
    with IFDSPropertyMetaInformation[DataFlowFact] {

    /** The type of the TAC domain. */
    type V = DUVar[KnownTypedValue]

    def flows: Map[Statement, Set[DataFlowFact]]

    override def equals(that: Any): Boolean = that match {
        case other: IFDSProperty[DataFlowFact] ⇒
            if (this eq noFlowInformation) (other eq noFlowInformation)
            else if (other eq noFlowInformation) false
            else flows == other.flows
        case _ ⇒ false
    }

    def noFlowInformation: IFDSProperty[DataFlowFact]

    override def hashCode(): Int = flows.hashCode()
}