/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf
package fixtures

object NilProperty extends Property {
    override type Self = this.type
    final override val key = PropertyKey.create[Object, NilProperty.type]("NIL")
}
