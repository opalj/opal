/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties
package alias

import scala.runtime.ScalaRunTime

import org.opalj.br.fpcf.properties.Context

/**
 * Represents a pair of [[AliasSourceElement]]s and a [[Context]] for each the elements to which an alias relationship can be assigned.
 * It is used to query and store the associated alias property in the property store.
 * The order of the elements is irrelevant, as the alias property is symmetric.
 * To ensure this, the given elements might be swapped internally.
 *
 * @param e1 The first [[AliasSourceElement]] to which the alias relationship is assigned.
 * @param e2 The second [[AliasSourceElement]] to which the alias relationship is assigned.
 * @param c1 The [[Context]] for the first element in which the alias relationship is valid.
 * @param c2 The [[Context]] for the second element in which the alias relationship is valid.
 */
class AliasEntity(
    private val c1: Context,
    private val c2: Context,
    private val e1: AliasSourceElement,
    private val e2: AliasSourceElement
) {

    /**
     * A copy of two elements of this [[AliasEntity]].
     * It is used to ensure that the order of the elements is irrelevant.
     */
    private[this] val (_element1, _element2) = (e1, e2) match {
        case (AliasNull, e2)                           => (AliasNull, e2)
        case (e1, AliasNull)                           => (AliasNull, e1)
        case (e1, e2) if e1.hashCode() < e2.hashCode() => (e1, e2)
        case (e1, e2)                                  => (e2, e1)
    }

    /**
     * A copy of two contexts of this [[AliasEntity]]. It uses the same order as the elements.
     */
    private[this] val (_context1, _context2) = (c1, c2) match {
        case (c1, c2) if e1 == _element1 => (c1, c2)
        case (c1, c2)                    => (c2, c1)
    }

    /**
     * @return the first [[AliasSourceElement]] of this [[AliasEntity]].
     */
    def element1: AliasSourceElement = _element1

    /**
     * @return the second [[AliasSourceElement]] of this [[AliasEntity]].
     */
    def element2: AliasSourceElement = _element2

    /**
     * @return the [[Context]] for the first [[AliasSourceElement]] of this [[AliasEntity]].
     */
    def context1: Context = _context1

    /**
     * @return the [[Context]] for the second [[AliasSourceElement]] of this [[AliasEntity]].
     */
    def context2: Context = _context2

    /**
     * Checks if the two elements of this [[AliasEntity]] are bound to a method.
     * @return True, if both elements are bound to a method, false otherwise.
     *
     * @see [[AliasSourceElement.isMethodBound]]
     */
    def bothElementsMethodBound: Boolean = element1.isMethodBound && element2.isMethodBound

    /**
     * Checks if the two elements of this [[AliasEntity]] are bound to the same method.
     *
     * This method should only be called if both elements are bound to a method.
     *
     * @return True, if both elements are bound to the same method, false otherwise.
     *
     * @see [[bothElementsMethodBound]]
     * @see [[AliasSourceElement.isMethodBound]]
     */
    def elementsInSameMethod: Boolean = element1.method.eq(element2.method)

    // we can't use a case class because the order of the two attributes is irrelevant
    override def equals(other: Any): Boolean = other match {
        case that: AliasEntity =>
            that.isInstanceOf[AliasEntity] &&
                context1 == that.context1 &&
                context2 == that.context2 &&
                element1 == that.element1 &&
                element2 == that.element2
        case _ => false
    }

    override def hashCode(): Int = {
        ScalaRunTime._hashCode((context1, context2, element1, element2))
    }
}

object AliasEntity {

    /**
     * Creates an [[AliasEntity]] that represents the given pair of [[AliasSourceElement]]s and the given [[Context]].
     *
     * @param c1 The [[Context]] for the first element in which the alias relationship is valid.
     * @param c2 The [[Context]] for the second element in which the alias relationship is valid.
     * @param e1 The first [[AliasSourceElement]] to which the alias relationship is assigned.
     * @param e2 The second [[AliasSourceElement]] to which the alias relationship is assigned.
     * @return An [[AliasEntity]] that represents the given pair of [[AliasSourceElement]]s and the given [[Context]].
     */
    def apply(c1: Context, c2: Context, e1: AliasSourceElement, e2: AliasSourceElement): AliasEntity =
        new AliasEntity(c1, c2, e1, e2)

}
