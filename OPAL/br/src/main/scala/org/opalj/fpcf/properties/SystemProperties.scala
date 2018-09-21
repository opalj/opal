/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties

/**
 * TODO Documentation
  *
  * @author Florian Kuebler
 */
sealed trait SystemPropertiesPropertyMetaInformation extends PropertyMetaInformation {
    type Self = SystemProperties
}

class SystemProperties(val properties: Map[String, Set[String]])
        extends Property with SystemPropertiesPropertyMetaInformation {
    final def key: PropertyKey[SystemProperties] = SystemProperties.key
}

object SystemProperties extends SystemPropertiesPropertyMetaInformation {
    final val PropertyKeyName = "SystemProperties"

    final val key: PropertyKey[SystemProperties] = {
        PropertyKey.create(
            PropertyKeyName,
            null//: FallbackPropertyComputation[SomeProject, SystemProperties],
            //(_: PropertyStore, eps: EPS[SomeProject, SystemProperties]) ⇒ eps.ub,
            //(_: PropertyStore, _: Entity) ⇒ None
        )
    }
}
