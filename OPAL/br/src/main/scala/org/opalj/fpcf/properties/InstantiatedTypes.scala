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
import org.opalj.collection.immutable.UIDSet
import scala.collection.Set

/**
 * TODO
 * @author Florian Kuebler
 */
sealed trait InstantiatedTypesPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = InstantiatedTypes
}

class InstantiatedTypes( val orderedTypes: List[ObjectType], val types: UIDSet[ObjectType])
        extends Property with OrderedProperty with InstantiatedTypesPropertyMetaInformation {

    final def key: PropertyKey[InstantiatedTypes] = InstantiatedTypes.key

    override def toString: String = s"InstantiatedTypes(size=${types.size}:${types.mkString("\n\t")})"

    /**
     * Tests if this property is equal or better than the given one (better means that the
     * value is above the given value in the underlying lattice.)
     */
    override def checkIsEqualOrBetterThan(e: Entity, other: InstantiatedTypes): Unit = {
        if (!types.subsetOf(other.types))
            throw new IllegalArgumentException(s"$e: illegal refinement of property $other to $this")
    }
}

object InstantiatedTypes extends InstantiatedTypesPropertyMetaInformation {

    def apply(types: Set[ObjectType]): InstantiatedTypes = new InstantiatedTypes( /*types, */ UIDSet(types.toSeq: _*))

    // todo cache
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
