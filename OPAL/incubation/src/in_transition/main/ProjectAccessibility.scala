/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
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
