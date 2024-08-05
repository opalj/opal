/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br

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
}

class PDVar[+Value <: ValueInformation /*org.opalj.ai.ValuesDomain#DomainValue*/ ] private (
    val value:  Value,
    val usePCs: PCs
) extends PDUVar[Value] {

    def defPCs: Nothing = throw new UnsupportedOperationException

    override def equals(other: Any): Boolean = {
        other match {
            case that: PDVar[_] => this.usePCs == that.usePCs
            case _              => false
        }
    }

    override def toString: String = {
        s"PDVar(usePCs=${usePCs.mkString("{", ",", "}")},value=$value)"
    }
}

object PDVar {

    def apply[Value <: ValueInformation](value: Value, useSites: IntTrieSet): PDVar[Value] = new PDVar(value, useSites)

    def unapply[Value <: ValueInformation](d: PDVar[Value]): Some[(Value, IntTrieSet)] = Some((d.value, d.usePCs))
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
        s"PUVar(defPCs=${defPCs.mkString("{", ",", "}")},value=$value)"
    }

}

object PUVar {

    def apply[Value <: ValueInformation](value: Value, defPCs: IntTrieSet): PUVar[Value] = new PUVar(value, defPCs)

    def unapply[Value <: ValueInformation](u: PUVar[Value]): Some[(Value, IntTrieSet)] = Some((u.value, u.defPCs))
}
