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
    final val PropertyKeyName = "opalj.SystemProperties"

    final val key: PropertyKey[SystemProperties] = {
        PropertyKey.create(
            PropertyKeyName,
            (_: PropertyStore, reason: FallbackReason, _: Entity) ⇒ reason match {
                case PropertyIsNotDerivedByPreviouslyExecutedAnalysis ⇒
                    new SystemProperties(Map.empty)
                case _ ⇒
                    throw new IllegalStateException(s"No analysis is scheduled for property: $PropertyKeyName")
            }
        )
    }
}
