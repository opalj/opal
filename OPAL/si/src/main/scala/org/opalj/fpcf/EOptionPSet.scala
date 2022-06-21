/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import scala.collection.immutable.IntMap
import scala.collection.mutable

/**
 * An semi-mutable set storing EPKs and interim properties.
 *
 * The set is semi-mutable in the sense that the concrete property values associated with
 * a specific entity can be updated, but as soon as dependencies to other E/PKs are added or removed
 * a new set is created; this behavior is required by the property store since the list of
 * dependees w.r.t. the entity/property kind must not change
 *
 * The set never contains FinalEPs and can (should) therefore directly be used as the dependency
 * set passed to [[InterimResult]]s.
 *
 * @author Michael Eichberg
 */
sealed trait EOptionPSet[E <: Entity, P <: Property] extends Iterable[EOptionP[E, P]] {

    // The number of times a dependency was added or removed. That is, this number always
    // increases and is used by the property store to detect if an EOptionPSet was actually
    // updated w.r.t. the set of epk dependencies. However, if a dependency is updated and the
    // new property is final and dependency is therefor deleted, we still do not consider that
    // as an explicit epk dependency update!
    private[fpcf] def epkUpdatesCount: Int

    override def foreach[U](f: EOptionP[E, P] => U): Unit
    override def isEmpty: Boolean
    final override def hasDefiniteSize: Boolean = true

    /** Filters the respective EOptionP values and returns a new EOptionPSet. */
    override def filter(p: EOptionP[E, P] => Boolean): EOptionPSet[E, P] = {
        // an implementation is required to shut up the compiler...
        throw new UnknownError("this method must be overridden by subclasses")
    }

    /**
     * Returns the last queried value or queries the property store and stores the value unless
     * the value is final.
     *
     * The value is stored to ensure that a client gets a consistent view of the same EPK if it is
     * queried multiple times during an analysis.
     *
     * If the queried eOptionP is final then it is not added to the list of dependees.
     */
    def getOrQueryAndUpdate[NewE <: E, NewP <: P](
        e:  NewE,
        pk: PropertyKey[NewP]
    )(
        implicit
        ps: PropertyStore
    ): EOptionP[NewE, NewP]

    /*
    /** Removes all EOptionP values with the given entity from this set. */
    def remove(e: Entity): Unit

    /** Removes all EOptionP values with the given `PropertyKey` from this set. */
    def remove(pk: SomePropertyKey): Unit

    /** Removes the given EOptionP value from this set. */
    def remove(eOptionP: SomeEOptionP): Unit

    def clear(): Unit
*/

    /**
     * Updates this set's EOptionP that has the same entity and PropertyKey with the given one.
     * '''Here, update means that the value is replaced, unless the new value is final. In
     * that case the value is removed because it is no longer required as a dependency!'''
     */
    def update(eps: SomeEPS): Unit

    /**
     * Updates all dependent values. Similar to the update method final values will be removed.
     */
    def updateAll()(implicit ps: PropertyStore): Unit

    /** Creates new successor instance which can be manipulated independently from this instance. */
    override def clone(): EOptionPSet[E, P] = {
        // an implementation is required to shut up the compiler...
        throw new UnknownError("this method must be overridden by subclasses")
    }

}

private[fpcf] class MultiEOptionPSet[E <: Entity, P <: Property](
        private var data:                  Map[Int, mutable.Map[Entity, EOptionP[E, P]]] = IntMap.empty,
        private[fpcf] var epkUpdatesCount: Int                                           = 0
) extends EOptionPSet[E, P] {

    override def foreach[U](f: EOptionP[E, P] => U): Unit = {
        data.valuesIterator.foreach(_.valuesIterator.foreach(f))
    }
    override def forall(p: EOptionP[E, P] => Boolean) = {
        data.valuesIterator.forall(_.valuesIterator.forall(p))
    }
    override def exists(p: EOptionP[E, P] => Boolean) = {
        data.valuesIterator.exists(_.valuesIterator.exists(p))
    }
    override def isEmpty: Boolean = data.isEmpty // <= we always minimize the store
    override def size: Int = { var size = 0; data.valuesIterator.foreach(size += _.size); size }

    override def filter(p: EOptionP[E, P] => Boolean): EOptionPSet[E, P] = {
        var newData: Map[Int, mutable.Map[Entity, EOptionP[E, P]]] = IntMap.empty
        var filteredSomeValue = false
        data.foreach { entry =>
            val (pkId, eEOptionPs) = entry
            val newEEOptionPs =
                eEOptionPs.filter { entry =>
                    val (e, eOptionP) = entry
                    if (p(eOptionP)) {
                        true
                    } else {
                        filteredSomeValue = true
                        false
                    }
                }
            if (newEEOptionPs.nonEmpty) {
                newData += ((pkId, newEEOptionPs))
            }
        }

        if (filteredSomeValue) {
            new MultiEOptionPSet(newData, epkUpdatesCount + 1)
        } else {
            new MultiEOptionPSet(data, epkUpdatesCount)
        }
    }

    override def getOrQueryAndUpdate[NewE <: E, NewP <: P](
        e:  NewE,
        pk: PropertyKey[NewP]
    )(
        implicit
        ps: PropertyStore
    ): EOptionP[NewE, NewP] = {
        val pkId = pk.id
        data.get(pkId) match {
            case Some(eEOptionPs) =>
                eEOptionPs.get(e) match {
                    case Some(eOptionP) => eOptionP.asInstanceOf[EOptionP[NewE, NewP]]
                    case _ =>
                        val eOptionP: EOptionP[NewE, NewP] = ps(e, pk)
                        if (eOptionP.isRefinable) {
                            eEOptionPs += (eOptionP.e -> eOptionP)
                            epkUpdatesCount += 1
                        }
                        eOptionP
                }
            case _ =>
                val eOptionP: EOptionP[NewE, NewP] = ps(e, pk)
                if (eOptionP.isRefinable) {
                    data = data + (pkId -> mutable.Map(eOptionP.e -> eOptionP))
                    epkUpdatesCount += 1
                }
                eOptionP

        }
    }

    /*
    def remove(e: Entity): Unit = {
        data = data.mapValues(_ -= e).filter(_._2.nonEmpty)
    }

    def remove(pk: SomePropertyKey): Unit = {
        data = data - pk.id
    }

    def remove(eOptionP: SomeEOptionP): Unit = {
        val pkId = eOptionP.pk.id
        data.get(pkId) match {
            case None => /*nothing to do*/
            case Some(eEOptionPs) =>
                eEOptionPs.remove(eOptionP)
                data = data.updated(pkId, eEOptionPs - eOptionP.e)
        }
    }

    def clear(): Unit = {
        data = IntMap.empty
    }
    */

    override def update(eps: SomeEPS): Unit = {
        val pkId = eps.pk.id
        data.get(pkId) match {
            case Some(eEOptionPs) =>
                if (eps.isFinal) {
                    if (eEOptionPs.remove(eps.e).isEmpty) {
                        throw new IllegalStateException(s"no old entry found for $eps")
                    }
                    if (eEOptionPs.isEmpty) {
                        data = data - pkId
                    }
                } else {
                    eEOptionPs.update(eps.e, eps.asInstanceOf[EPS[E, P]])
                }
            case None => throw new IllegalStateException(s"no old entry found for $eps")
        }
    }

    override def updateAll()(implicit ps: PropertyStore): Unit = {
        data.valuesIterator.foreach { eEOptionPs =>
            eEOptionPs
                .mapValuesInPlace((_, eOptionP) =>
                    if (eOptionP.isEPK)
                        ps(eOptionP.asEPK)
                    else
                        ps(eOptionP.toEPK))
                .filterInPlace((_, eOptionP) =>
                    eOptionP.isRefinable)
        }
        data = data.filter(_._2.nonEmpty)
    }

    override def clone(): EOptionPSet[E, P] = new MultiEOptionPSet(data, epkUpdatesCount)

    override def toString(): String = {
        var s = "MultiEOptionPSet("
        foreach(e => s += s"\n\t$e")
        s+"\n)"
    }

    override def iterator: Iterator[EOptionP[E, P]] = data.valuesIterator.flatMap(vals => vals.valuesIterator)
}

object EOptionPSet {

    def empty[E <: Entity, P <: Property]: EOptionPSet[E, P] = new MultiEOptionPSet(IntMap.empty)

    // IMPROVE Provide specialized implementations for sets consisting only of e/p pairs belonging to the same PK
    def apply[E <: Entity, P <: Property](eOptionP: EOptionP[E, P]): EOptionPSet[E, P] = {
        if (eOptionP.isRefinable) {
            new MultiEOptionPSet(IntMap(eOptionP.pk.id -> mutable.Map(eOptionP.e -> eOptionP)))
        } else {
            empty
        }
    }
}
