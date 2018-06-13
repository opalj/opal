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
package br

import org.opalj.collection.immutable.UIDSet

/**
 * Represents the results of a type hierarchy related query.
 *
 * @author Michael Eichberg
 */
sealed abstract class TypeHierarchyInformation {

    def typeInformationType: String
    def classTypes: UIDSet[ObjectType]
    def interfaceTypes: UIDSet[ObjectType]

    def size: Int = classTypes.size + interfaceTypes.size

    def foreach[T](f: ObjectType ⇒ T): Unit = {
        classTypes.foreach(f)
        interfaceTypes.foreach(f)
    }

    def forall(f: ObjectType ⇒ Boolean): Boolean = {
        classTypes.forall(f) && interfaceTypes.forall(f)
    }

    def exists(f: ObjectType ⇒ Boolean): Boolean = {
        classTypes.exists(f) || interfaceTypes.exists(f)
    }

    def foldLeft[B](z: B)(op: (B, ObjectType) ⇒ B): B = {
        interfaceTypes.foldLeft(classTypes.foldLeft(z)(op))(op)
    }

    def contains(t: ObjectType): Boolean = {
        interfaceTypes.containsId(t.id) || classTypes.containsId(t.id)
    }

    /**
     * The set of all types. The set is computed on demand and NOT cached; in general,
     * the higher-order methods should be used!
     */
    def all: UIDSet[ObjectType]

    override def toString: String = {
        val classInfo = classTypes.map(_.toJava).mkString("classes={", ", ", "}")
        val interfaceInfo = interfaceTypes.map(_.toJava).mkString("interfaces={", ", ", "}")
        s"$typeInformationType($classInfo, $interfaceInfo)"
    }

}

/**
 * Represents a type's subtype information.
 *
 * @author Michael Eichberg
 */
sealed abstract class SubtypeInformation extends TypeHierarchyInformation {
    def typeInformationType: String = "SubtypeInformation"
}

object SubtypeInformation {

    final val None = new SubtypeInformation {
        def classTypes: UIDSet[ObjectType] = UIDSet.empty
        def interfaceTypes: UIDSet[ObjectType] = UIDSet.empty
        def all: UIDSet[ObjectType] = UIDSet.empty
    }

    def apply(
        theClassTypes:     UIDSet[ObjectType],
        theInterfaceTypes: UIDSet[ObjectType]
    ): SubtypeInformation = {
        if (theClassTypes.isEmpty) {
            if (theInterfaceTypes.isEmpty)
                None
            else
                new SubtypeInformation {
                    def classTypes: UIDSet[ObjectType] = UIDSet.empty
                    val interfaceTypes = theInterfaceTypes
                    def all: UIDSet[ObjectType] = interfaceTypes
                }
        } else if (theInterfaceTypes.isEmpty) {
            new SubtypeInformation {
                val classTypes = theClassTypes
                def interfaceTypes: UIDSet[ObjectType] = UIDSet.empty
                def all: UIDSet[ObjectType] = classTypes
            }
        } else {
            new SubtypeInformation {
                val classTypes = theClassTypes
                val interfaceTypes = theInterfaceTypes
                def all: UIDSet[ObjectType] = classTypes ++ interfaceTypes
            }
        }
    }
}

/**
 * Represents a type's supertype information.
 *
 * @author Michael Eichberg
 */
sealed abstract class SupertypeInformation extends TypeHierarchyInformation {
    def typeInformationType: String = "SupertypeInformation"
}

object SupertypeInformation {

    final val JustObject = new SupertypeInformation {
        final def classTypes: UIDSet[ObjectType] = ClassHierarchy.JustObject
        final def interfaceTypes: UIDSet[ObjectType] = UIDSet.empty
        final def all: UIDSet[ObjectType] = ClassHierarchy.JustObject
    }

    final val None = new SupertypeInformation {
        final def classTypes: UIDSet[ObjectType] = UIDSet.empty
        final def interfaceTypes: UIDSet[ObjectType] = UIDSet.empty
        final def all: UIDSet[ObjectType] = UIDSet.empty
    }

    def apply(
        theClassTypes:     UIDSet[ObjectType],
        theInterfaceTypes: UIDSet[ObjectType]
    ): SupertypeInformation = {
        if (theInterfaceTypes.isEmpty) {
            if (theClassTypes.isEmpty) {
                None
            } else if (theClassTypes.isSingletonSet && (theClassTypes.head eq ObjectType.Object)) {
                JustObject
            } else {
                new SupertypeInformation {
                    final val classTypes: UIDSet[ObjectType] = theClassTypes
                    final def interfaceTypes: UIDSet[ObjectType] = UIDSet.empty
                    final def all: UIDSet[ObjectType] = classTypes
                }
            }
        } else {
            if (theClassTypes.isEmpty) {
                new SupertypeInformation {
                    final def classTypes: UIDSet[ObjectType] = UIDSet.empty
                    final val interfaceTypes: UIDSet[ObjectType] = theInterfaceTypes
                    final def all: UIDSet[ObjectType] = theInterfaceTypes
                }
            } else if (theClassTypes.isSingletonSet && (theClassTypes.head eq ObjectType.Object)) {
                new SupertypeInformation {
                    final def classTypes: UIDSet[ObjectType] = ClassHierarchy.JustObject
                    final val interfaceTypes: UIDSet[ObjectType] = theInterfaceTypes
                    final def all: UIDSet[ObjectType] = theInterfaceTypes + ObjectType.Object
                }
            } else {
                new SupertypeInformation {
                    final val classTypes: UIDSet[ObjectType] = theClassTypes
                    final val interfaceTypes: UIDSet[ObjectType] = theInterfaceTypes
                    final def all: UIDSet[ObjectType] = classTypes ++ interfaceTypes
                }
            }
        }
    }
}
