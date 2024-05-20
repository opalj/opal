/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.value.ValueInformation

case class PDUWeb(
    defPCs: IntTrieSet,
    usePCs: IntTrieSet
) {
    def containsVarAt(pc: Int, pv: PV): Boolean = pv match {
        case PDVar(_, varUsePCs) => defPCs.contains(pc) && usePCs.intersect(varUsePCs).nonEmpty
        case PUVar(_, varDefPCs) => usePCs.contains(pc) && defPCs.intersect(varDefPCs).nonEmpty
    }

    def intersectsWith(other: PDUWeb): Boolean = {
        other.defPCs.intersect(defPCs).nonEmpty && other.usePCs.intersect(usePCs).nonEmpty
    }

    def intersect(other: PDUWeb): PDUWeb = PDUWeb(other.defPCs.intersect(defPCs), other.usePCs.intersect(usePCs))

    def size(): Int = defPCs.size + usePCs.size

    def toValueOriginForm(implicit pcToIndex: Array[Int]): DUWeb = DUWeb(
        valueOriginsOfPCs(defPCs, pcToIndex),
        valueOriginsOfPCs(usePCs, pcToIndex)
    )
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
