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

sealed abstract class TypeHierarchyInformation {

    def typeInformationType: String
    def classTypes: UIDSet[ObjectType]
    def interfaceTypes: UIDSet[ObjectType]

    def size: Int = classTypes.size + interfaceTypes.size

    def foreach[T](f: ObjectType ⇒ T): Unit = {
        classTypes.foreach(f)
        interfaceTypes.foreach(f)
    }

    def all: UIDSet[ObjectType] = classTypes ++ interfaceTypes

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

    final val empty = new SubtypeInformation {
        def classTypes: UIDSet[ObjectType] = UIDSet.empty
        def interfaceTypes: UIDSet[ObjectType] = UIDSet.empty
    }

    def apply(
        theClassTypes:     UIDSet[ObjectType],
        theInterfaceTypes: UIDSet[ObjectType]
    ): SubtypeInformation = {
        if (theClassTypes.isEmpty) {
            if (theInterfaceTypes.isEmpty)
                empty
            else
                new SubtypeInformation {
                    def classTypes: UIDSet[ObjectType] = UIDSet.empty
                    val interfaceTypes = theInterfaceTypes
                }
        } else if (theInterfaceTypes.isEmpty) {
            new SubtypeInformation {
                val classTypes = theClassTypes
                def interfaceTypes: UIDSet[ObjectType] = UIDSet.empty
            }
        } else {
            new SubtypeInformation {
                val classTypes = theClassTypes
                val interfaceTypes = theInterfaceTypes
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

    def none = new SupertypeInformationForClasses(UIDSet.empty, UIDSet.empty)

    def apply(
        classTypes:     UIDSet[ObjectType],
        interfaceTypes: UIDSet[ObjectType]
    ): SupertypeInformation = {
        new SupertypeInformationForClasses(classTypes, interfaceTypes)
    }
}

private[br] final class SupertypeInformationForClasses(
    val classTypes:     UIDSet[ObjectType],
    val interfaceTypes: UIDSet[ObjectType]
) extends SupertypeInformation

private[br] object NoSpecificSupertypeInformationForInterfaces extends SupertypeInformation {
    def classTypes: UIDSet[ObjectType] = ClassHierarchy.JustObject
    def interfaceTypes: UIDSet[ObjectType] = UIDSet.empty
}

private[br] final class SupertypeInformationForInterfaces private (
        val interfaceTypes: UIDSet[ObjectType]
) extends SupertypeInformation {
    def classTypes: UIDSet[ObjectType] = ClassHierarchy.JustObject
}

object SupertypeInformationForInterfaces {
    def apply(interfaceTypes: UIDSet[ObjectType]): SupertypeInformation = {
        if (interfaceTypes.isEmpty)
            NoSpecificSupertypeInformationForInterfaces
        else
            new SupertypeInformationForInterfaces(interfaceTypes)
    }
}
