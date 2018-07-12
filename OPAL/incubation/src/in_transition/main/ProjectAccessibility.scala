/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties

import org.opalj.br.ClassFile
import org.opalj.br.Field
import org.opalj.br.Method

/**
 * This is a common trait for all ProjectAccessibility properties which can be emitted to the
 * PropertyStore. It describes the scope where the given entity can be accessed.
 */
// IMPROVE Transform ProjectAccessibility into standard project information.
sealed trait ProjectAccessibility extends Property {

    final type Self = ProjectAccessibility

    final def isRefinable = false

    final def key = ProjectAccessibility.Key
}

object ProjectAccessibility {

    final val fallback: (PropertyStore, Entity) ⇒ ProjectAccessibility = (ps, e) ⇒ {
        // IMPROVE Query the project's Method/Type extensibility/the defining module to compute the scope in which the entity is accessible.
        e match {
            case _: ClassFile ⇒ Global
            case m: Method    ⇒ if (m.isPrivate) ClassLocal else Global
            case f: Field     ⇒ if (f.isPrivate) ClassLocal else Global
        }
    }

    final val Key = {
        PropertyKey.create[ProjectAccessibility](
            "ProjectAccessibility",
            fallbackProperty = fallback,
            (ps: PropertyStore, epks: Iterable[SomeEPK]) ⇒ { throw new UnsupportedOperationException(); }
        )
    }
}

/**
 * Entities with `Global` project accessibility can be accessed by every entity within the project.
 */
case object Global extends ProjectAccessibility

/**
 * Entities with `PackageLocal` project accessibility can be accessed by every entity within
 * the package where the entity is defined.
 */
case object PackageLocal extends ProjectAccessibility

/**
 * Entities with `ClassLocal` project accessibility can be accessed by every entity within
 * the class where the entity is defined.
 */
case object ClassLocal extends ProjectAccessibility
