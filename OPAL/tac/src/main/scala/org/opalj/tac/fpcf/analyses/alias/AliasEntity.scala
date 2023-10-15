/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package alias

import org.opalj.br.fpcf.properties.Context

import scala.runtime.ScalaRunTime

/**
 * Represents a pair of [[AliasSourceElement]]s and a [[Context]] to which an alias relationship can be assigned.
 * It is used to query and store the associated alias property in the property store.
 * The order of the elements is irrelevant, as the alias property is symmetric.
 * To ensure this, the given elements might be swapped internally.
 *
 * @param context The [[Context]] in which the alias relationship is valid.
 * @param e1 The first [[AliasSourceElement]] to which the alias relationship is assigned.
 * @param e2 The second [[AliasSourceElement]] to which the alias relationship is assigned.
 */
class AliasEntity(val context: Context, private val e1: AliasSourceElement, private val e2: AliasSourceElement) {

    /**
     * A copy of two elements of this [[AliasEntity]].
     * It is used to ensure that the order of the elements is irrelevant.
     */
    private val (_element1, _element2) = (e1, e2) match {
        case (e1: AliasReturnValue, e2) if !e2.isInstanceOf[AliasReturnValue] => (e1, e2)
        case (e1, e2: AliasReturnValue) if !e1.isInstanceOf[AliasReturnValue] => (e2, e1)
        case (e1: AliasNull, e2) => (e1, e2)
        case (e1, e2: AliasNull) => (e2, e1)
        case (e1, e2) if e1.hashCode() < e2.hashCode() => (e1, e2)
        case (e1, e2) => (e2, e1)
    }

    /**
     * Returns the first [[AliasSourceElement]] of this [[AliasEntity]].
     * @return The first [[AliasSourceElement]] of this [[AliasEntity]].
     */
    def element1: AliasSourceElement = _element1

    /**
     * Returns the second [[AliasSourceElement]] of this [[AliasEntity]].
     * @return The second [[AliasSourceElement]] of this [[AliasEntity]].
     */
    def element2: AliasSourceElement = _element2

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

    //we can't use a case class because the order of the order of the two elements
    override def equals(other: Any): Boolean = other match {
        case that: AliasEntity =>
            that.isInstanceOf[AliasEntity] &&
                context == that.context &&
                _element1 == that._element1 &&
                _element2 == that._element2
        case _ => false
    }

    override def hashCode(): Int = {
        ScalaRunTime._hashCode((context, _element1, _element2))
    }
}

object AliasEntity {

    /**
     * Creates an [[AliasEntity]] that represents the given pair of [[AliasSourceElement]]s and the given [[Context]].
     *
     * @param context The [[Context]] in which the alias relationship is valid.
     * @param e1 The first [[AliasSourceElement]] to which the alias relationship is assigned.
     * @param e2 The second [[AliasSourceElement]] to which the alias relationship is assigned.
     * @return An [[AliasEntity]] that represents the given pair of [[AliasSourceElement]]s and the given [[Context]].
     */
    def apply(context: Context, e1: AliasSourceElement, e2: AliasSourceElement): AliasEntity = new AliasEntity(context, e1, e2)

}
