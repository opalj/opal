/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties

sealed trait SystemPropertiesFakePropertyMetaInformation extends PropertyMetaInformation {
    final type Self = SystemPropertiesFakeProperty
}

/**
 * A fake property used internally to allow dependencies to the tac property in the system
 * properties analysis.
 *
 * @author Florian Kuebler
 */
sealed abstract class SystemPropertiesFakeProperty
    extends Property with SystemPropertiesFakePropertyMetaInformation {

    override def key: PropertyKey[SystemPropertiesFakeProperty] = SystemPropertiesFakeProperty.key
}

object SystemPropertiesFakePropertyFinal extends SystemPropertiesFakeProperty

object SystemPropertiesFakePropertyNonFinal extends SystemPropertiesFakeProperty

object SystemPropertiesFakeProperty extends SystemPropertiesFakePropertyMetaInformation {
    final val key: PropertyKey[SystemPropertiesFakeProperty] = {
        PropertyKey.forSimpleProperty(
            "SystemPropertiesFakeProperty", SystemPropertiesFakePropertyFinal
        )
    }
}
