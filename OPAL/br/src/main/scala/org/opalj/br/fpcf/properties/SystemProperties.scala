/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties

import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyIsNotDerivedByPreviouslyExecutedAnalysis
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Entity

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
    final val Name = "opalj.SystemProperties"

    final val key: PropertyKey[SystemProperties] = {
        PropertyKey.create(
            Name,
            (_: PropertyStore, reason: FallbackReason, _: Entity) => reason match {
                case PropertyIsNotDerivedByPreviouslyExecutedAnalysis =>
                    new SystemProperties(Map.empty)
                case _ =>
                    throw new IllegalStateException(s"analysis required for property: $Name")
            }
        )
    }
}
