/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import scala.collection.immutable.IntMap
import scala.collection.mutable

/**
 * An mutable set storing EPKs and interim values; the set never contains FinalEPs and can
 * (should) therefore directly be used as the dependency set passed to [[InterimResult]]s.
 *
 * @author Michael Eichberg
 */
sealed trait EOptionPSet[E <: Entity, P <: Property] extends Traversable[EOptionP[E, P]] {

    override def foreach[U](f: EOptionP[E, P] ⇒ U): Unit
    override def isEmpty: Boolean
    final override def hasDefiniteSize: Boolean = true

    /** Filters the respective EOptionP values and returns a new EOptionPSet. */
    override def filter(p: EOptionP[E, P] ⇒ Boolean): EOptionPSet[E, P] = {
        // implementation is required to shut up the compiler...
        throw new UnknownError("this method must be overridden by subclasses")
    }

    /**
     * Gets the last queried value or queries the property store and stores the value unless
     * the value is final.
     *
     * The value is stored to ensure that a client gets a consistent view of the same EPK is
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

    /** Removes all EOptionP values with the given entity from this set. */
    def remove(e: Entity): Unit

    /** Removes all EOptionP values with the given `PropertyKey` from this set. */
    def remove(pk: SomePropertyKey): Unit

    /** Removes the given EOptionP value from this set. */
    def remove(eOptionP: SomeEOptionP): Unit

    /**
     * Updates this set's EOptionP that has the same entity and PropertyKey with the given one.
     * '''Here, update means that the value is replace, unless the new value is final. In
     * that case the value is removed because it is no longer required as a dependency!'''
     */
    def update(eps: SomeEPS): Unit

    /**
     * Updates all dependent values. Similar to the update method final values will be removed.
     */
    def updateAll(implicit ps: PropertyStore): Unit

}

private[fpcf] class MultiEOptionPSet[E <: Entity, P <: Property](
        private var data: Map[Int, mutable.Map[Entity, EOptionP[E, P]]] = IntMap.empty
) extends EOptionPSet[E, P] {

    override def foreach[U](f: EOptionP[E, P] ⇒ U): Unit = {
        data.valuesIterator.foreach(_.valuesIterator.foreach(f))
    }
    override def forall(p: EOptionP[E, P] ⇒ Boolean) = {
        data.valuesIterator.forall(_.valuesIterator.forall(p))
    }
    override def exists(p: EOptionP[E, P] ⇒ Boolean) = {
        data.valuesIterator.exists(_.valuesIterator.exists(p))
    }
    override def isEmpty: Boolean = data.isEmpty // <= we always minimize the store
    override def size: Int = { var size = 0; data.valuesIterator.foreach(size += _.size); size }

    override def filter(p: EOptionP[E, P] ⇒ Boolean): EOptionPSet[E, P] = {
        val newData =
            data
                .iterator
                .map(e ⇒ (e._1 /*PKid*/ , e._2.filter(e ⇒ p(e._2))))
                .filter(_._2.nonEmpty)
                .toMap
        new MultiEOptionPSet(newData)
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
            case Some(eEOptionPs) ⇒
                eEOptionPs.get(e) match {
                    case Some(eOptionP) ⇒ eOptionP.asInstanceOf[EOptionP[NewE, NewP]]
                    case _ ⇒
                        val eOptionP: EOptionP[NewE, NewP] = ps(e, pk)
                        if (eOptionP.isRefinable) {
                            eEOptionPs += (eOptionP.e → eOptionP)
                        }
                        eOptionP
                }
            case _ ⇒
                val eOptionP: EOptionP[NewE, NewP] = ps(e, pk)
                if (eOptionP.isRefinable) {
                    data = data + (pkId → mutable.Map(eOptionP.e → eOptionP))
                }
                eOptionP

        }
    }

    def remove(e: Entity): Unit = {
        data = data.mapValues(_ -= e).filter(_._2.nonEmpty)
    }

    def remove(pk: SomePropertyKey): Unit = {
        data = data - pk.id
    }

    def remove(eOptionP: SomeEOptionP): Unit = {
        val pkId = eOptionP.pk.id
        data.get(pkId) match {
            case None ⇒ /*nothing to do*/
            case Some(eEOptionPs) ⇒
                eEOptionPs.remove(eOptionP)
                data = data.updated(pkId, eEOptionPs - eOptionP.e)
        }
    }

    override def update(eps: SomeEPS): Unit = {
        val pkId = eps.pk.id
        data.get(pkId) match {
            case Some(eEOptionPs) ⇒
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
            case None ⇒ throw new IllegalStateException(s"no old entry found for $eps")
        }
    }

    override def updateAll(implicit ps: PropertyStore): Unit = {
        data.valuesIterator.foreach { eEOptionPs ⇒
            eEOptionPs
                .transform((_, eOptionP) ⇒
                    if (eOptionP.isEPK)
                        ps(eOptionP.asEPK)
                    else
                        ps(eOptionP.toEPK))
                .retain((_, eOptionP) ⇒
                    eOptionP.isRefinable)
        }
        data = data.filter(_._2.nonEmpty)
    }

    override def toString(): String = {
        var s = "MultiEOptionPSet("
        foreach(e ⇒ s += s"\n\t$e")
        s+"\n)"
    }
}

object EOptionPSet {

    def empty[E <: Entity, P <: Property]: EOptionPSet[E, P] = new MultiEOptionPSet(IntMap.empty)

    // IMPROVE Provide specialized implementations for sets consisting only of e/p pairs belonging to the same PK
    def apply[E <: Entity, P <: Property](eOptionP: EOptionP[E, P]): EOptionPSet[E, P] = {
        if (eOptionP.isRefinable) {
            new MultiEOptionPSet(IntMap(eOptionP.pk.id → mutable.Map(eOptionP.e → eOptionP)))
        } else {
            empty
        }
    }
}
