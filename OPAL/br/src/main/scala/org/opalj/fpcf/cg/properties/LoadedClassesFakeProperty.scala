/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package cg
package properties

sealed trait LoadedClassesFakePropertyMetaInformation extends PropertyMetaInformation {
    final type Self = LoadedClassesFakeProperty
}

/**
 * A fake property used internally to allow dependencies to the tac property in the loaded classes
 * analyses.
 *
 * @author Florian Kuebler
 */
sealed abstract class LoadedClassesFakeProperty
    extends Property with LoadedClassesFakePropertyMetaInformation {

    override def key: PropertyKey[LoadedClassesFakeProperty] = LoadedClassesFakeProperty.key
}

object LoadedClassesFakePropertyFinal extends LoadedClassesFakeProperty

object LoadedClassesFakePropertyNonFinal extends LoadedClassesFakeProperty

object LoadedClassesFakeProperty extends LoadedClassesFakePropertyMetaInformation {
    final val key: PropertyKey[LoadedClassesFakeProperty] = {
        PropertyKey.forSimpleProperty(
            "LoadedClassesFakeProperty", LoadedClassesFakePropertyFinal
        )
    }
}
