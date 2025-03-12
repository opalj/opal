/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ide
package integration

import org.opalj.fpcf.Entity
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation

/**
 * Class for property meta information for properties carrying the target callables.
 *
 * @author Robin Körkemeier
 */
final class IDETargetCallablesPropertyMetaInformation[Callable <: Entity](
    propertyMetaInformation: IDEPropertyMetaInformation[?, ?, ?, Callable]
) extends PropertyMetaInformation {
    override type Self = IDETargetCallablesProperty[Callable]

    /**
     * The used property key, based on [[propertyMetaInformation]]
     */
    private lazy val propertyKey: PropertyKey[IDETargetCallablesProperty[Callable]] = {
        PropertyKey.create(s"${PropertyKey.name(propertyMetaInformation.key)}_TargetCallables")
    }

    override def key: PropertyKey[IDETargetCallablesProperty[Callable]] = propertyKey
}
