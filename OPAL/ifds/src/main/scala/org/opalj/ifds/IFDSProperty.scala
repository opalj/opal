/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ifds

import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyMetaInformation

trait IFDSPropertyMetaInformation[S, Fact <: AbstractIFDSFact] extends PropertyMetaInformation {
    /**
     * Creates an IFDSProperty containing the result of this analysis.
     *
     * @param result Maps each exit statement to the facts, which hold after the exit statement.
     * @return An IFDSProperty containing the `result`.
     */
    def create(result: Map[S, Set[Fact]]): IFDSProperty[S, Fact]
    def create(result: Map[S, Set[Fact]], debugData: Map[S, Set[Fact]]): IFDSProperty[S, Fact]
}

abstract class IFDSProperty[S, Fact <: AbstractIFDSFact]
    extends Property
    with IFDSPropertyMetaInformation[S, Fact] {

    /**
     * Maps exit statements to the data flow facts which hold before them.
     */
    def flows: Map[S, Set[Fact]]

    /**
     * Maps all statements to the data flow facts which hold before them if debug setting is enabled.
     */
    def debugData: Map[S, Set[Fact]]

    override def equals(other: Any): Boolean = other match {
        case that: IFDSProperty[S @unchecked, Fact @unchecked] =>
            // We cached the "hashCode" to make the following comparison more efficient;
            // note that all properties are eventually added to some set and therefore
            // the hashCode is required anyway!
            (this eq that) || (this.hashCode == that.hashCode && this.flows == that.flows)
        case _ =>
            false
    }

    override lazy val hashCode: Int = flows.hashCode()
}
