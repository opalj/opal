/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac

import org.opalj.ai.PCs
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.value.ValueInformation

/**
 * A persistent (i.e. TAC independent) representation of a [[DUVar]].
 */
abstract class PDUVar[+Value <: ValueInformation] {

    /**
     * The information about the variable that were derived by the underlying data-flow analysis.
     */
    def value: Value

    /**
     * The PCs of the instructions which use this variable.
     *
     * '''Defined, if and only if this is an assignment statement.'''
     */
    def usePCs: PCs

    /**
     * The PCs of the instructions which initialize this variable/
     * the origin of the value identifies the expression which initialized this variable.
     *
     * '''Defined, if and only if this is a variable usage.'''
     *
     * For more information see [[DUVar.definedBy]]
     */
    def defPCs: PCs

    def toValueOriginForm(implicit pcToIndex: Array[Int]): DUVar[Value]
}

class PUVar[+Value <: ValueInformation /*org.opalj.ai.ValuesDomain#DomainValue*/ ] private (
    val value:  Value,
    val defPCs: PCs
) extends PDUVar[Value] {

    def usePCs: Nothing = throw new UnsupportedOperationException

    override def equals(other: Any): Boolean = {
        other match {
            case that: PUVar[_] => this.defPCs == that.defPCs
            case _              => false
        }
    }

    override def toString: String = {
        s"PUVar(defSites=${defPCs.mkString("{", ",", "}")},value=$value)"
    }

    override def toValueOriginForm(
        implicit pcToIndex: Array[Int]
    ): UVar[Value] = UVar(value, valueOriginsOfPCs(defPCs, pcToIndex))
}

object PUVar {

    def apply(d: org.opalj.ai.ValuesDomain)(
        value:  d.DomainValue,
        defPCs: PCs
    ): PUVar[d.DomainValue] = {
        new PUVar[d.DomainValue](value, defPCs)
    }

    def apply[Value <: ValueInformation](value: Value, defSites: IntTrieSet): PUVar[Value] = new PUVar(value, defSites)

    def unapply[Value <: ValueInformation](u: UVar[Value]): Some[(Value, IntTrieSet)] = Some((u.value, u.defSites))
}
