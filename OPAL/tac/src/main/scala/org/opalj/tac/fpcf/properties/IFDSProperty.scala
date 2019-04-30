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

    def normalExitFacts: Map[Statement, Set[DataFlowFact]]
    def abnormalExitFacts: Map[Statement, Set[DataFlowFact]]

    override def equals(that: Any): Boolean = that match {
        case other: IFDSProperty[DataFlowFact] ⇒ normalExitFacts == other.normalExitFacts && abnormalExitFacts == other.abnormalExitFacts
        case _                                 ⇒ false
    }

    override def hashCode(): Int = 31 * normalExitFacts.hashCode() + abnormalExitFacts.hashCode()
}
