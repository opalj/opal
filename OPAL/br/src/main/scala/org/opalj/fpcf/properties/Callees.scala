/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties

import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.collection.immutable.IntTrieSet

import scala.collection.immutable.IntMap

/**
 * For a given [[DeclaredMethod]], and for each call site (represented by the PC), the set of methods
 * that are possible call targets.
 *
 * @author Florian Kuebler
 */
sealed trait CalleesPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = Callees
}

sealed trait Callees extends Property with OrderedProperty with CalleesPropertyMetaInformation {

    def size: Int

    def callees(pc: Int): Set[DeclaredMethod]

    def callees: TraversableOnce[(Int, Set[DeclaredMethod])]

    override def toString: String = {
        s"Callees(size=${this.size})"
    }

    final def key: PropertyKey[Callees] = Callees.key

    /**
     * Tests if this property is equal or better than the given one (better means that the
     * value is above the given value in the underlying lattice.)
     */
    override def checkIsEqualOrBetterThan(e: Entity, other: Callees): Unit = {
        if (other.size < size) //todo if (!pcMethodPairs.subsetOf(other.pcMethodPairs))
            throw new IllegalArgumentException(s"$e: illegal refinement of property $other to $this")
    }
}

final class CalleesImplementation(
        private[this] val calleesIds:      IntMap[IntTrieSet],
        private[this] val declaredMethods: DeclaredMethods
) extends Callees {

    override def callees(pc: Int): Set[DeclaredMethod] = {
        calleesIds(pc).mapToAny[DeclaredMethod](declaredMethods.apply)
    }

    override def callees: TraversableOnce[(Int, Set[DeclaredMethod])] = {
        calleesIds.iterator.map {
            case (pc, x) ⇒ {
                pc → x.mapToAny[DeclaredMethod](declaredMethods.apply)
            }
        }
    }

    override val size: Int = {
        calleesIds.iterator.map(_._2.size).sum
    }
}

class FallbackCallees(
        private[this] val project:         SomeProject,
        private[this] val method:          DeclaredMethod,
        private[this] val declaredMethods: DeclaredMethods
) extends Callees {

    override lazy val size: Int = {
        //callees.size * project.allMethods.size
        // todo this is for performance improvement only
        Int.MaxValue
    }

    override def callees(pc: Int): Set[DeclaredMethod] = {
        project.allMethods.map(declaredMethods.apply).toSet
    }

    override def callees: TraversableOnce[(UShort, Set[DeclaredMethod])] = ???
}

object Callees extends CalleesPropertyMetaInformation {

    final val key: PropertyKey[Callees] = {
        PropertyKey.create(
            "Callees",
            (ps: PropertyStore, r: FallbackReason, m: DeclaredMethod) ⇒ {
                val p = ps.context(classOf[SomeProject])
                val declaredMethods = p.get(DeclaredMethodsKey)
                r match {
                    case PropertyIsNotComputedByAnyAnalysis ⇒
                        Callees.fallback(m, p, declaredMethods)
                    case PropertyIsNotDerivedByPreviouslyExecutedAnalysis ⇒
                        //println(s"Fallback callee $m")
                        new CalleesImplementation(IntMap.empty, declaredMethods)
                }
            },
            (_: PropertyStore, eps: EPS[DeclaredMethod, Callees]) ⇒ eps.ub,
            (_: PropertyStore, _: DeclaredMethod) ⇒ None
        )
    }

    def fallback(m: DeclaredMethod, p: SomeProject, declaredMethods: DeclaredMethods): Callees = {
        new FallbackCallees(p, m, declaredMethods)
    }
}
