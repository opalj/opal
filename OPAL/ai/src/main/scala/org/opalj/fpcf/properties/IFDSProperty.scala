/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties

import org.opalj.ai.Domain
import org.opalj.ai.domain.RecordDefUse
import org.opalj.fpcf.analyses.ifds.Statement
import org.opalj.tac.DUVar

trait IFDSPropertyMetaInformation[DataFlowFact] extends PropertyMetaInformation {
    def noFlowInformation: IFDSProperty[DataFlowFact]

}

abstract class IFDSProperty[DataFlowFact] extends Property
    with IFDSPropertyMetaInformation[DataFlowFact] {

    /** The type of the TAC domain. */
    type V = DUVar[(Domain with RecordDefUse)#DomainValue]

    def flows: Map[Statement, Set[DataFlowFact]]

    override def equals(that: Any): Boolean = that match {
        case other: IFDSProperty[DataFlowFact] if flows == other.flows ⇒ true
        case _ ⇒ false
    }

    override def hashCode(): Int = flows.hashCode()
}