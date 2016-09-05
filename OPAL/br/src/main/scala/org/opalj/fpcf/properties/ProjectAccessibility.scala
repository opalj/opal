/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
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
import org.opalj.br.analyses.SomeProject

/**
 * This is a common trait for all ProjectAccessibility properties which can be emitted to the
 * PropertyStore. It describes the scope where the given entity can be accessed.
 */
sealed trait ProjectAccessibility extends Property {

    final type Self = ProjectAccessibility

    final def isRefineable = false

    final def key = ProjectAccessibility.Key
}

object ProjectAccessibility {

    final val fallback: (PropertyStore, Entity) ⇒ ProjectAccessibility = (ps, e) ⇒ {
        import AnalysisModes._
        val analysisMode = ps.context[SomeProject].analysisMode

        e match {
            case m: Method ⇒
                if (m.isPrivate) ClassLocal
                else if (m.isPackagePrivate && !(analysisMode eq OPA)) PackageLocal
                else Global
            case cf: ClassFile ⇒
                if (cf.isPackageVisible && !(analysisMode eq OPA)) PackageLocal
                else Global
            case f: Field ⇒
                if (f.isPrivate) ClassLocal
                else if (f.isPackagePrivate && !(analysisMode eq OPA)) PackageLocal
                else Global
        }
    }

    final val cycleResolutionStrategy: PropertyKey.CycleResolutionStrategy = (
        ps: PropertyStore,
        epks: PropertyKey.SomeEPKs
    ) ⇒ {
        throw new Error("there should be no cycles")
    }

    final val Key = {
        PropertyKey.create[ProjectAccessibility](
            "ProjectAccessibility",
            fallbackProperty = fallback,
            cycleResolutionStrategy = cycleResolutionStrategy
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
 * Entities with `PackageLocal` project accessibility can be accessed by every entity within
 * the class where the entity is defined.
 */
case object ClassLocal extends ProjectAccessibility

