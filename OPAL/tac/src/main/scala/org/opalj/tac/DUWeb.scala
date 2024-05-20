/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.value.ValueInformation

case class DUWeb(
    defSites: IntTrieSet,
    useSites: IntTrieSet
) {
    def intersectsWith(other: DUWeb): Boolean = {
        other.defSites.intersect(defSites).nonEmpty && other.useSites.intersect(useSites).nonEmpty
    }

    def intersect(other: DUWeb): DUWeb = DUWeb(other.defSites.intersect(defSites), other.useSites.intersect(useSites))

    def toPersistentForm(implicit stmts: Array[Stmt[V]]): PDUWeb = PDUWeb(
        defSites.map(pcOfDefSite _),
        useSites.map(pcOfDefSite _)
    )
}

object DUWeb {
    def forDVar[Value <: ValueInformation](defSite: Int, dVar: DVar[Value]): DUWeb =
        DUWeb(IntTrieSet(defSite), dVar.useSites)

    def forUVar[Value <: ValueInformation](useSite: Int, uVar: UVar[Value]): DUWeb =
        DUWeb(uVar.defSites, IntTrieSet(useSite))
}
