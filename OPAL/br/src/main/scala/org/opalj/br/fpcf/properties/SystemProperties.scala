/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties

import org.opalj.br.fpcf.properties.string.StringTreeNode
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyIsNotDerivedByPreviouslyExecutedAnalysis
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore

/**
 * Holds the possible values that a [[java.util.Properties]] can take on, e.g. by analyzing the parameters given to
 * calls like [[java.util.Properties.setProperty]] that are found in reachable methods.
 * <p>
 * Currently, values are not distinguished by the keys they are set for since the key parameters may also take on any
 * value conforming to their string tree, which can be infinitely many (see [[StringTreeNode]]).
 * <p>
 * All existing analyses do not distinguish between the system-wide properties (set through [[System.setProperty]] or
 * similar) and all other [[java.util.Properties]] instances.
 *
 * @author Maximilian Rüsch
 */
sealed trait SystemPropertiesPropertyMetaInformation extends PropertyMetaInformation {

    type Self = SystemProperties
}

case class SystemProperties(values: Set[StringTreeNode])
    extends Property with SystemPropertiesPropertyMetaInformation {

    def mergeWith(other: SystemProperties): SystemProperties = {
        if (values == other.values) this
        else SystemProperties(values ++ other.values)
    }

    final def key: PropertyKey[SystemProperties] = SystemProperties.key
}

object SystemProperties extends SystemPropertiesPropertyMetaInformation {

    final val Name = "opalj.SystemProperties"

    final val key: PropertyKey[SystemProperties] = {
        PropertyKey.create(
            Name,
            (_: PropertyStore, reason: FallbackReason, _: Entity) =>
                reason match {
                    case PropertyIsNotDerivedByPreviouslyExecutedAnalysis =>
                        new SystemProperties(Set.empty)
                    case _ =>
                        throw new IllegalStateException(s"analysis required for property: $Name")
                }
        )
    }
}
