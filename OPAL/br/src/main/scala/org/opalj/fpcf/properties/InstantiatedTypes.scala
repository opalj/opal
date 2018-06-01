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

import org.opalj.br.ObjectType
import org.opalj.br.analyses.ProjectLike

import scala.collection.Set

/**
 * TODO
 * @author Florian Kuebler
 */
sealed trait InstantiatedTypesPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = InstantiatedTypes
}

class InstantiatedTypes(val types: Set[ObjectType]) extends Property with InstantiatedTypesPropertyMetaInformation {

    final def key = InstantiatedTypes.key
}

object InstantiatedTypes extends InstantiatedTypesPropertyMetaInformation {

    def apply(types: Set[ObjectType]): InstantiatedTypes = new InstantiatedTypes(types)

    def allTypes(p: ProjectLike): InstantiatedTypes = {
        val allTypes = p.classHierarchy.allSubtypes(ObjectType.Object, reflexive = true)
        InstantiatedTypes(allTypes)
    }

    final val key: PropertyKey[InstantiatedTypes] = {
        PropertyKey.create[ProjectLike, InstantiatedTypes](
            "InstantiatedTypes",
            (_: PropertyStore, project: ProjectLike) ⇒ {
                allTypes(project)
            },
            (_, eps: EPS[ProjectLike, InstantiatedTypes]) ⇒ eps.toUBEP
        )
    }
}
