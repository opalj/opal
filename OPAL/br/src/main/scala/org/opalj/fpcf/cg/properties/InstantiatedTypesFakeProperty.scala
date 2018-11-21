/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package cg
package properties

sealed trait InstantiatedTypesFakePropertyMetaInformation extends PropertyMetaInformation {
    final type Self = InstantiatedTypesFakeProperty
}

/**
 * A fake property used internally to allow dependencies to the tac property in the loaded classes
 * analyses.
 *
 * @author Florian Kuebler
 */
sealed abstract class InstantiatedTypesFakeProperty
    extends Property with InstantiatedTypesFakePropertyMetaInformation {

    override def key: PropertyKey[InstantiatedTypesFakeProperty] = InstantiatedTypesFakeProperty.key
}

object InstantiatedTypesFakePropertyFinal extends InstantiatedTypesFakeProperty

object InstantiatedTypesFakePropertyNonFinal extends InstantiatedTypesFakeProperty

object InstantiatedTypesFakeProperty extends InstantiatedTypesFakePropertyMetaInformation {
    final val key: PropertyKey[InstantiatedTypesFakeProperty] = {
        PropertyKey.forSimpleProperty(
            "InstantiatedTypesFakeProperty", InstantiatedTypesFakePropertyFinal
        )
    }
}
