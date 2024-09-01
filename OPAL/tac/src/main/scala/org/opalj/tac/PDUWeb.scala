/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.value.ValueInformation

case class PDUWeb(
    defPCs: IntTrieSet,
    usePCs: IntTrieSet
) {
    def identifiesSameVarAs(other: PDUWeb): Boolean = other.defPCs.intersect(defPCs).nonEmpty

    def combine(other: PDUWeb): PDUWeb = PDUWeb(other.defPCs ++ defPCs, other.usePCs ++ usePCs)

    def size: Int = defPCs.size + usePCs.size

    // Performance optimizations
    private lazy val _hashCode = scala.util.hashing.MurmurHash3.productHash(this)
    override def hashCode(): Int = _hashCode
    override def equals(obj: Any): Boolean = obj.hashCode() == _hashCode && super.equals(obj)
}

object PDUWeb {

    def apply(pc: Int, pv: PV): PDUWeb = pv match {
        case pdVar: PDVar[_] => forDVar(pc, pdVar)
        case puVar: PUVar[_] => forUVar(pc, puVar)
    }

    def forDVar[Value <: ValueInformation](defPC: Int, pdVar: PDVar[Value]): PDUWeb =
        PDUWeb(IntTrieSet(defPC), pdVar.usePCs)

    def forUVar[Value <: ValueInformation](usePC: Int, puVar: PUVar[Value]): PDUWeb =
        PDUWeb(puVar.defPCs, IntTrieSet(usePC))
}
