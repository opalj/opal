/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package cg
package properties

import scala.collection.immutable.IntMap

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.value.ValueInformation
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.DeclaredMethods

/**
 * Encapsulates – for a given [[org.opalj.br.DeclaredMethod]] and for each call site (represented by
 * the PC) – the set of methods that are possible call targets according to a partial call graph
 * analysis.
 *
 * @author Florian Kübler
 * @author Dominik Helm
 */
trait CalleesLike extends OrderedProperty with CalleesLikePropertyMetaInformation {

    def size: Int

    /**
     * PCs of callsites that could not be completely resolved by the analysis.
     */
    def incompleteCallSites: IntTrieSet

    /**
     * IDs of the potential callees at this call site. None if the (indirect) analysis doesn't apply
     * to this call site.
     */
    def callees(pc: Int): Option[IntTrieSet]

    /**
     * PCs of all call sites resolved by the analysis.
     */
    def callSitePCs: Iterator[Int]

    /**
     * Map from PC to set of IDs of all potential callees derived by the analysis for that PC.
     */
    def callSites(implicit declaredMethods: DeclaredMethods): IntMap[IntTrieSet]

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {
        if (other.size < size)
            throw new IllegalArgumentException(s"$e: illegal refinement of $other to $this");
    }
}

/**
 * Base class for storing callees of a reachable method.
 */
abstract class AbstractCalleesLike extends CalleesLike {

    protected[this] val calleesIds: IntMap[IntTrieSet]

    override val incompleteCallSites: IntTrieSet

    override def callees(pc: Int): Option[IntTrieSet] = calleesIds.get(pc)

    override def callSitePCs: Iterator[Int] = calleesIds.keysIterator

    override def callSites(implicit declaredMethods: DeclaredMethods): IntMap[IntTrieSet] = {
        calleesIds
    }

    // TODO Determine if used "regularly" - if so, rewrite using foreach to avoid (un)boxing operations.
    override val size: Int = calleesIds.valuesIterator.foldLeft(0)(_ + _.size)

}

/**
 * Trait to mixin for objects that represent empty callees because the method is not reachable.
 */
trait CalleesLikeNotReachable extends CalleesLike {

    final override def size: Int = 0

    final override def incompleteCallSites: IntTrieSet = IntTrieSet.empty

    final override def callees(pc: Int): Option[IntTrieSet] = None

    final override def callSitePCs: Iterator[Int] = Iterator.empty

    override def callSites(implicit declaredMethods: DeclaredMethods): IntMap[IntTrieSet] = {
        IntMap.empty
    }
}

trait IndirectCallees extends CalleesLike {
    val parameters: IntMap[Map[DeclaredMethod, Seq[Option[(ValueInformation, IntTrieSet)]]]]
}

trait CalleesLikePropertyMetaInformation extends PropertyMetaInformation {

    override type Self <: CalleesLike

    /**
     * True if the callees represented by this property are not directly invoked by a call site, but
     * indirectly. This applies only to such indirect callees, that are called sequentially, but not
     * to e.g. Thread.run being called by Thread.start asynchronously.
     */
    val isIndirect: Boolean
}

trait DirectCalleesPropertyMetaInformation extends CalleesLikePropertyMetaInformation {
    final override val isIndirect = false
}

trait IndirectCalleesPropertyMetaInformation extends CalleesLikePropertyMetaInformation {
    final override val isIndirect = true
}
