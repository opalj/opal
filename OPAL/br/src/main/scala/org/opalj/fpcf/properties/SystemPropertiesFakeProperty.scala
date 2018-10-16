/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties

import org.opalj.fpcf.Entity
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation

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
    extends Property with OrderedProperty with SystemPropertiesFakePropertyMetaInformation {

    override def key: PropertyKey[SystemPropertiesFakeProperty] = SystemPropertiesFakeProperty.key
}

object SystemPropertiesFakePropertyFinal extends SystemPropertiesFakeProperty {

    override def checkIsEqualOrBetterThan(
        e: Entity, other: SystemPropertiesFakeProperty
    ): Unit = {}
}
object SystemPropertiesFakePropertyNonFinal extends SystemPropertiesFakeProperty {

    override def checkIsEqualOrBetterThan(
        e: Entity, other: SystemPropertiesFakeProperty
    ): Unit = {
        if (other eq SystemPropertiesFakePropertyFinal) {
            throw new IllegalArgumentException(s"$e: illegal refinement of property $other to $this")
        }
    }
}

object SystemPropertiesFakeProperty extends SystemPropertiesFakePropertyMetaInformation {
    final val key: PropertyKey[SystemPropertiesFakeProperty] = {
        PropertyKey.forSimpleProperty(
            "SystemPropertiesFakeProperty", SystemPropertiesFakePropertyFinal
        )
    }
}
