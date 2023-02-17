/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ifds

import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyMetaInformation

trait IFDSPropertyMetaInformation[S, IFDSFact <: AbstractIFDSFact] extends PropertyMetaInformation {
    /**
     * Creates an IFDSProperty containing the result of this analysis.
     *
     * @param result Maps each exit statement to the facts, which hold after the exit statement.
     * @return An IFDSProperty containing the `result`.
     */
    def create(result: Map[S, Set[IFDSFact]]): IFDSProperty[S, IFDSFact]
    def create(result: Map[S, Set[IFDSFact]], debugData: Map[S, Set[IFDSFact]]): IFDSProperty[S, IFDSFact]
}

abstract class IFDSProperty[S, IFDSFact <: AbstractIFDSFact]
    extends Property
    with IFDSPropertyMetaInformation[S, IFDSFact] {

    /**
     * Maps exit statements to the data flow facts which hold before them.
     */
    def flows: Map[S, Set[IFDSFact]]

    /**
     * Maps all statements to the data flow facts which hold before them if debug setting is enabled.
     */
    def debugData: Map[S, Set[IFDSFact]]

    override def equals(other: Any): Boolean = other match {
        case that: IFDSProperty[S @unchecked, IFDSFact @unchecked] =>
            // We cached the "hashCode" to make the following comparison more efficient;
            // note that all properties are eventually added to some set and therefore
            // the hashCode is required anyway!
            (this eq that) || (this.hashCode == that.hashCode && this.flows == that.flows)
        case _ =>
            false
    }

    override lazy val hashCode: Int = flows.hashCode()
}
