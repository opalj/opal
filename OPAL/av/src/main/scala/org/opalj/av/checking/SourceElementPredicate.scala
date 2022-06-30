/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package av
package checking

import org.opalj.br.ConcreteSourceElement
import org.opalj.bi.AccessFlagsMatcher
import org.opalj.br.{Attributes => SourceElementAttributes}

/**
 * A predicate related to a specific source element.
 *
 * @author Michael Eichberg
 */
trait SourceElementPredicate[-S <: ConcreteSourceElement] extends (S => Boolean) { left =>

    def and[T <: ConcreteSourceElement, X <: S with T](
        right: SourceElementPredicate[T]
    ): SourceElementPredicate[X] = {

        new SourceElementPredicate[X] {
            def apply(s: X): Boolean = left(s) && right(s)
            def toDescription() = left.toDescription()+" and "+right.toDescription()
        }

    }

    final def having[T <: ConcreteSourceElement, X <: S with T](
        right: SourceElementPredicate[T]
    ): SourceElementPredicate[X] = and(right)

    /**
     * Returns a human readable representation of this predicate that is well suited
     * for presenting it in messages related to architectural deviations.
     *
     * It should not end with a white space and should not use multiple lines.
     * It should not be a complete sentence as this description may be composed with
     * other descriptions.
     */
    def toDescription(): String
}

case class AccessFlags(
        accessFlags: AccessFlagsMatcher
)
    extends SourceElementPredicate[ConcreteSourceElement] {

    def apply(sourceElement: ConcreteSourceElement): Boolean = {
        this.accessFlags.unapply(sourceElement.accessFlags)
    }

    def toDescription(): String = accessFlags.toString

}

/**
 * @author Marco Torsello
 * @author Michael Eichberg
 */
case class Attributes(
        attributes: SourceElementAttributes
)
    extends SourceElementPredicate[ConcreteSourceElement] {

    def apply(sourceElement: ConcreteSourceElement): Boolean = {
        //    sourceElement.attributes.size == this.attributes.size &&
        this.attributes.forall(a => sourceElement.attributes.exists(_ == a))
    }

    def toDescription(): String = {
        attributes.view.map(_.getClass.getSimpleName).mkString(" « ", ", ", " »")
    }
}
