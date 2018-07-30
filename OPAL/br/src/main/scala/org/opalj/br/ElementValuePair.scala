/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * An annotation's name-value pair.
 *
 * @author Michael Eichberg
 * @author Arne Lottmann
 */
case class ElementValuePair(name: String, value: ElementValue) {

    def toJava: String = name+"="+value.toJava
}

object ElementValuePair {

    def apply(evPair: (String, ElementValue)): ElementValuePair = {
        val (name, value) = evPair
        new ElementValuePair(name, value)
    }

}
