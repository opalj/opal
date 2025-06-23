/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import org.opalj.br.PDVar
import org.opalj.br.PUVar
import org.opalj.collection.immutable.IntTrieSet

/**
 * Identifies a variable inside a given fixed method. A method's webs can be constructed through the maximal unions of
 * all intersecting DU-UD-chains of the method.
 *
 * @param defPCs The def PCs of the variable that is identified through this web.
 * @param usePCs The use PCs of the variable that is identified through this web.
 *
 * @author Maximilian Rüsch
 */
case class PDUWeb(
    defPCs: IntTrieSet,
    usePCs: IntTrieSet
) {
    def identifiesSameVarAs(other: PDUWeb): Boolean = other.defPCs.intersect(defPCs).nonEmpty

    def combine(other: PDUWeb): PDUWeb = PDUWeb(other.defPCs ++ defPCs, other.usePCs ++ usePCs)

    // Performance optimizations
    private lazy val _hashCode = scala.util.hashing.MurmurHash3.productHash(this)
    override def hashCode(): Int = _hashCode
    override def equals(obj: Any): Boolean = obj.hashCode() == _hashCode && super.equals(obj)
}

object PDUWeb {

    def apply(pc: Int, pv: PV): PDUWeb = pv match {
        case pdVar: PDVar[_] => PDUWeb(IntTrieSet(pc), pdVar.usePCs)
        case puVar: PUVar[_] => PDUWeb(puVar.defPCs, IntTrieSet(pc))
    }
}
