/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

import scala.collection.immutable.IntMap
import scala.collection.mutable

/**
 * An mutable set storing EOptionP values.
 *
 * @author Michael Eichberg
 */
sealed trait EOptionPSet[E <: Entity, P <: Property] extends Traversable[EOptionP[E, P]] {

    override def foreach[U](f: EOptionP[E, P] ⇒ U): Unit
    override def isEmpty: Boolean
    override def hasDefiniteSize: Boolean

    override def filter(p : EOptionP[E,P] ⇒ Boolean) : EOptionPSet[E,P]

    /**
     * Gets the last queried value or queries the property store and stores the value. The value
     * is stored to ensure that a client gets a consistent view of the same EPK is queried
     * multiple times during an analysis.
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

    def remove(e: Entity): Unit

    def remove(pk: SomePropertyKey): Unit

    def remove(eOptionP: SomeEOptionP): Unit

    def update(eps: SomeEPS): Unit

    def updateAll(implicit ps : PropertyStore) : Unit

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
    override def isEmpty: Boolean = data.isEmpty
    override def hasDefiniteSize: Boolean = true
    override def size: Int = { var size = 0; data.valuesIterator.foreach(size += _.size); size }

    def getOrQueryAndUpdate[NewE <: E, NewP <: P](
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
                        eEOptionPs += (eOptionP.e → eOptionP)
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
            case None ⇒ throw new IllegalStateException(s"no old entry found for $eps")
            case Some(eEOptionPs) ⇒
                data = data.updated(pkId, eEOptionPs.updated(eps.e, eps.asInstanceOf[EPS[E, P]]))
        }
    }

    override def updateAll(implicit ps : PropertyStore) : Unit = {
        data.valuesIterator.foreach{ eEOptionPs ⇒
            eEOptionPs
                .mapValues(eOptionP ⇒
                if(eOptionP.isEPK)
                    ps(eOptionP.asEPK)
                else
                    ps(eOptionP.toEPK))
                .filter(_._2.isRefinable)
            }
        data = data.filter(_._2.nonEmpty)
    }

}

object EOptionPSet {

    def empty[E <: Entity, P <: Property]: EOptionPSet[E, P] = new MultiEOptionPSet(IntMap.empty)

    // IMPROVE Provide specialized implementations for sets consisting only of e/p pairs belonging to the same PK
    def apply[E <: Entity, P <: Property](eOptionP: EOptionP[E, P]): EOptionPSet[E, P] = {
        new MultiEOptionPSet(IntMap(eOptionP.pk.id → mutable.Map(eOptionP.e → eOptionP)))
    }
}
