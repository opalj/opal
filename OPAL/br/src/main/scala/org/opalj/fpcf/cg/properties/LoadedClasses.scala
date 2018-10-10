/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package cg
package properties

import org.opalj.br.ObjectType
import org.opalj.collection.immutable.UIDSet

sealed trait LoadedClassesMetaInformation extends PropertyMetaInformation {
    final type Self = LoadedClasses
}

/**
 * Represent the set of types (classes) that were loaded by the VM during execution of the
 * respective [[org.opalj.br.analyses.Project]] (which is the entity for this property).
 *
 * @author Florian Kuebler
 */
sealed class LoadedClasses(val classes: UIDSet[ObjectType])
        extends Property with OrderedProperty with LoadedClassesMetaInformation {

    override def checkIsEqualOrBetterThan(e: Entity, other: LoadedClasses): Unit = {
        if (other.classes != null && !classes.subsetOf(other.classes)) {
            throw new IllegalArgumentException(s"$e: illegal refinement of property $other to $this")
        }
    }

    override def key: PropertyKey[LoadedClasses] = LoadedClasses.key

    override def toString: String = s"LoadedClasses(${classes.size})"
}

object NoLoadedClasses extends LoadedClasses(classes = UIDSet.empty)

object LoadedClasses extends LoadedClassesMetaInformation {
    final val key: PropertyKey[LoadedClasses] = {
        PropertyKey.forSimpleProperty("LoadedClasses", NoLoadedClasses)
    }
}
