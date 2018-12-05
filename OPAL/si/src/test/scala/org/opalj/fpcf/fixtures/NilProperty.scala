/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf
package fixtures

/**
 * Basically just a `Property` object.
 *
 * @note Only intended to be used as a test fixture.
 */
object NilProperty extends Property {
    override type Self = this.type
    final override val key = PropertyKey.create[Object, NilProperty.type]("NIL")
}
