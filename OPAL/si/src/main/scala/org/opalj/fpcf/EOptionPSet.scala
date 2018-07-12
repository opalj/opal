/* BSD 2-Clause License - see OPAL/LICENSE for details. */
/*
package org.opalj
package fpcf


/**
* An efficient implementation of a set of EOptionP values where the underlying E/PK pair
* is used as a key. This enables O(1) updates when an EPK/EPS is updated; i.e., an
* EPK/EPS uses the "same" key.
*
* '''This set is immutable!'''
*
* @tparam E
* @tparam P
*/
sealed trait EOptionPSet[+E <: Entity, +P <: Property] {

def isEmpty: Boolean

final def nonEmpty: Boolean = !isEmpty

/**
 * Adds the given value to this set. Replaces a previous value with the same
 * entity.
 *
 * @return '''This''' set if the value was already stored in the set otherwise this set
 *        or a new set - depending on what is more efficient.
 */
def put[NewE >: E, NewP >: P](eOptionP: EOptionP[NewE, NewP]): EOptionPSet[NewE, NewP]

/** Removes the '''entity with the property key(!)''' of the given eOptionP. */
def remove(eOptionP: SomeEOptionP): EOptionPSet[E, P]

/** Removes properties of the entity. */
def remove(e: Entity): EOptionPSet[E, P]

/** Removes all entities with the given `PropertyKey`. */
def remove(pk: SomePropertyKey): EOptionPSet[E, P]

def foreach[U](f: (EOptionP[E, P]) ⇒ U): Unit

def iterator: Iterator[EOptionP[E, P]]
}

private[fpcf] object EOptionPSet {

def empty[E <: Entity, P <: Property]: EOptionPSet[E, P] = E0OptionP0Set
}

object E0OptionP0Set extends EOptionPSet[Nothing, Nothing] {

def isEmpty: Boolean = true

def put[E <: Entity, P <: Property](eOptionP: EOptionP[E, P]): EOptionPSet[E, P] = {
    new E1OptionP1Set(eOptionP)
}

def remove(eOptionP: SomeEOptionP): this.type = this

def remove(e: Entity): this.type = this

def remove(pk: SomePropertyKey): this.type = this

def foreach[U](f: (EOptionP[Nothing, Nothing]) ⇒ U): Unit = {}

def iterator: Iterator[EOptionP[Nothing, Nothing]] = Iterator.empty
}



private[fpcf] final class E1OptionP1Set[E <: Entity, P <: Property](
    value: EOptionP[E, P]
) extends EOptionPSet[E, P] {

override def isEmpty: Boolean = false

def put[E <: Entity, P <: Property](eOptionP: EOptionP[E, P]): EOptionPSet[E, P] = {
    if ((value.e eq eOptionP.e) && value.pk == eOptionP.pk)
        new E1OptionP1Set(eOptionP)
    else {
        ???
    }
}


def remove(eOptionP: SomeEOptionP): EOptionPSet[E, P] = {
    if ((value.e eq eOptionP.e) && value.pk == eOptionP.pk) E0OptionP0Set else this
}

def remove(e: Entity): EOptionPSet[E, P] =  if (value.e eq e) E0OptionP0Set else this

def remove(pk: SomePropertyKey): EOptionPSet[E, P] = if (value.pk == pk) E0OptionP0Set else this

def foreach[U](f: (EOptionP[E, P]) ⇒ U): Unit = f(value)

def iterator: Iterator[EOptionP[E, P]] = Iterator.single(value)
}

private[fpcf] final class ENOptionP1Set[E <: Entity, P <: Property](
                                                                   value: EOptionP[E, P]
                                                               ) extends EOptionPSet[E, P] {

override def isEmpty: Boolean = false

def put[E <: Entity, P <: Property](eOptionP: EOptionP[E, P]): EOptionPSet[E, P] = {
    if ((value.e eq eOptionP.e) && value.pk == eOptionP.pk)
        new E1OptionP1Set(eOptionP)
    else {
        ???
    }
}


def remove(eOptionP: SomeEOptionP): EOptionPSet[E, P] = {
    if ((value.e eq eOptionP.e) && value.pk == eOptionP.pk) E0OptionP0Set else this
}

def remove(e: Entity): EOptionPSet[E, P] =  if (value.e eq e) E0OptionP0Set else this

def remove(pk: SomePropertyKey): EOptionPSet[E, P] = if (value.pk == pk) E0OptionP0Set else this

def foreach[U](f: (EOptionP[E, P]) ⇒ U): Unit = f(value)

def iterator: Iterator[EOptionP[E, P]] = Iterator.single(value)
}



class EOptionPSetN[E <: Entity, P <: Property]extends EOptionPSet[E,P](
                                         private Any
                                         ){

def put(eOptionP : EOptionP[E,P]) : Boolean = {

}

def foreach




}

trait Properties

case object Properties0 extends Properties
case class Properties1 extends Properties
*/

