/* BSD 2-Clause License - see OPAL/LICENSE for details. */
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
