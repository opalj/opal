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
     * Maps exit statements to the data flow facts, which hold after them.
     */
    def flows: Map[Statement, Set[DataFlowFact]]

    override def equals(other: Any): Boolean = other match {
        case that: IFDSProperty[DataFlowFact @unchecked] =>
            // We cached the "hashCode" to make the following comparison more efficient;
            // note that all properties are eventually added to some set and therefore
            // the hashCode is required anyway!
            (this eq that) || (this.hashCode == that.hashCode && this.flows == that.flows)
        case _ =>
            false
    }

    override lazy val hashCode: Int = flows.hashCode()
}
