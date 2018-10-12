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
        extends Property with OrderedProperty with LoadedClassesFakePropertyMetaInformation {

    override def key: PropertyKey[LoadedClassesFakeProperty] = LoadedClassesFakeProperty.key
}

object LoadedClassesFakePropertyFinal extends LoadedClassesFakeProperty {

    override def checkIsEqualOrBetterThan(
        e: Entity, other: LoadedClassesFakeProperty
    ): Unit = {}
}
object LoadedClassesFakePropertyNonFinal extends LoadedClassesFakeProperty {

    override def checkIsEqualOrBetterThan(
        e: Entity, other: LoadedClassesFakeProperty
    ): Unit = {
        if (other eq LoadedClassesFakePropertyFinal) {
            throw new IllegalArgumentException(s"$e: illegal refinement of property $other to $this")
        }
    }
}

object LoadedClassesFakeProperty extends LoadedClassesFakePropertyMetaInformation {
    final val key: PropertyKey[LoadedClassesFakeProperty] = {
        PropertyKey.forSimpleProperty(
            "LoadedClassesFakeProperty", LoadedClassesFakePropertyFinal
        )
    }
}
