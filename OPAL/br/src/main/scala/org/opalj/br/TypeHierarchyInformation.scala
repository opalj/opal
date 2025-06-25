/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import org.opalj.collection.ForeachRefIterator
import org.opalj.collection.immutable.UIDSet

/**
 * Represents the results of a type hierarchy related query.
 *
 * @author Michael Eichberg
 */
sealed abstract class TypeHierarchyInformation {

    def typeInformationType: String
    def classTypes: UIDSet[ClassType]
    def interfaceTypes: UIDSet[ClassType]
    /**
     * The set of all types; cached if useful.
     */
    def allTypes: UIDSet[ClassType]

    final def size: Int = allTypes.size

    def foreach[T](f: ClassType => T): Unit = allTypes.foreach(f)

    def iterator: Iterator[ClassType]

    def foreachIterator: ForeachRefIterator[ClassType] = allTypes.foreachIterator

    def forall(f: ClassType => Boolean): Boolean = allTypes.forall(f)

    def exists(f: ClassType => Boolean): Boolean = allTypes.exists(f)

    def foldLeft[B](z: B)(op: (B, ClassType) => B): B = allTypes.foldLeft(z)(op)

    /**
     * Tests if the given type belongs to the super/subtype of `this` type; this
     * test is not reflexive. I.e., if this information was computed for the type
     * X and contains is called with X, `false` will be returned!
     */
    def contains(t: ClassType): Boolean

    /**
     * Checks if the classTypeId is contained in the underlying set; no special cases
     * related to `java.lang.Object` are supported!
     */
    private[br] def containsId(classTypeId: Int): Boolean

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

/**
 * Factory to create the subtype information data structure.
 */
object SubtypeInformation {

    final val None: SubtypeInformation = new SubtypeInformation {
        override final def classTypes: UIDSet[ClassType] = UIDSet.empty
        override final def interfaceTypes: UIDSet[ClassType] = UIDSet.empty
        override final def allTypes: UIDSet[ClassType] = UIDSet.empty

        override final def contains(t: ClassType): Boolean = false

        override private[br] final def containsId(classTypeId: Int): Boolean = false

        override final def iterator: Iterator[ClassType] = Iterator.empty
    }

    def forObject(
        theClassTypes:     UIDSet[ClassType],
        theInterfaceTypes: UIDSet[ClassType],
        initialAllTypes:   UIDSet[ClassType]
    ): SubtypeInformation = {
        val theAllTypes = initialAllTypes ++ theClassTypes ++ theInterfaceTypes
        new SubtypeInformation {
            override final val classTypes: UIDSet[ClassType] = theClassTypes
            override final val interfaceTypes: UIDSet[ClassType] = theInterfaceTypes
            override final val allTypes: UIDSet[ClassType] = theAllTypes
            override final def iterator: Iterator[ClassType] = allTypes.iterator
            override final def contains(t: ClassType): Boolean = t ne ClassType.Object
            override private[br] final def containsId(classTypeId: Int): Boolean = {
                classTypeId != ClassType.ObjectId
            }
        }
    }

    def forSubtypesOfObject(
        isKnownType:       Array[Boolean],
        isInterfaceType:   Array[Boolean],
        theClassTypes:     UIDSet[ClassType],
        theInterfaceTypes: UIDSet[ClassType],
        initialAllTypes:   UIDSet[ClassType] // just used to increase "sharing" possibilities
    ): SubtypeInformation = {
        if (theClassTypes.isEmpty) {
            if (theInterfaceTypes.isEmpty)
                None
            else
                new SubtypeInformation { // ... if all subtypes are interfaces!
                    override final def classTypes: UIDSet[ClassType] = UIDSet.empty
                    override final val interfaceTypes: UIDSet[ClassType] = theInterfaceTypes
                    override final def allTypes: UIDSet[ClassType] = interfaceTypes
                    override final def iterator: Iterator[ClassType] = interfaceTypes.iterator
                    override final def contains(t: ClassType): Boolean = {
                        val tid = t.id
                        tid < isInterfaceType.length && isInterfaceType(tid) &&
                            interfaceTypes.containsId(tid)
                    }
                    override private[br] final def containsId(classTypeId: Int): Boolean = {
                        interfaceTypes.containsId(classTypeId)
                    }
                }
        } else if (theInterfaceTypes.isEmpty) {
            new SubtypeInformation {
                override final val classTypes: UIDSet[ClassType] = theClassTypes
                override final def interfaceTypes: UIDSet[ClassType] = UIDSet.empty
                override final def allTypes: UIDSet[ClassType] = classTypes
                override final def iterator: Iterator[ClassType] = classTypes.iterator
                override final def contains(t: ClassType): Boolean = {
                    val tid = t.id
                    // the first three checks are just guard checks..
                    tid != ClassType.ObjectId &&
                        tid < isKnownType.length && isKnownType(tid) && !isInterfaceType(tid) &&
                        classTypes.containsId(tid)
                }
                override private[br] final def containsId(classTypeId: Int): Boolean = {
                    classTypes.containsId(classTypeId)
                }
            }
        } else {
            val theAllTypes = initialAllTypes ++ theClassTypes ++ theInterfaceTypes
            new SubtypeInformation {
                override final val classTypes: UIDSet[ClassType] = theClassTypes
                override final val interfaceTypes: UIDSet[ClassType] = theInterfaceTypes
                // We precompute the information to ensure that tests that will fail will
                // only take half as many steps... (see containsID)
                override final val allTypes: UIDSet[ClassType] = theAllTypes
                override final def iterator: Iterator[ClassType] = allTypes.iterator
                override final def contains(t: ClassType): Boolean = {
                    val tid = t.id
                    // the first two checks are just guard checks...
                    tid != ClassType.ObjectId &&
                        tid < isKnownType.length && isKnownType(tid) &&
                        allTypes.containsId(tid)
                }
                override private[br] final def containsId(classTypeId: Int): Boolean = {
                    allTypes.containsId(classTypeId)
                }
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

    final val JustObject: SupertypeInformation = new SupertypeInformation {
        override final def classTypes: UIDSet[ClassType] = ClassHierarchy.JustObject
        override final def interfaceTypes: UIDSet[ClassType] = UIDSet.empty
        override final def allTypes: UIDSet[ClassType] = ClassHierarchy.JustObject
        override final def iterator: Iterator[ClassType] = Iterator(ClassType.Object)
        override final def contains(t: ClassType): Boolean = t eq ClassType.Object
        override private[br] final def containsId(classTypeId: Int): Boolean = {
            ClassType.ObjectId == classTypeId
        }
    }

    // Required in case of incomplete type hierarchies:
    final val Unknown: SupertypeInformation = new SupertypeInformation {
        override final def classTypes: UIDSet[ClassType] = UIDSet.empty
        override final def interfaceTypes: UIDSet[ClassType] = UIDSet.empty
        override final def allTypes: UIDSet[ClassType] = UIDSet.empty
        override final def iterator: Iterator[ClassType] = Iterator.empty

        override final def contains(t: ClassType): Boolean = t eq ClassType.Object

        override private[br] final def containsId(classTypeId: Int): Boolean = false
    }

    final val ForObject: SupertypeInformation = new SupertypeInformation {
        override final val classTypes: UIDSet[ClassType] = UIDSet.empty
        override final def interfaceTypes: UIDSet[ClassType] = UIDSet.empty
        override final def allTypes: UIDSet[ClassType] = UIDSet.empty
        override final def iterator: Iterator[ClassType] = Iterator.empty

        override final def contains(t: ClassType): Boolean = false

        override private[br] final def containsId(classTypeId: Int): Boolean = false
    }

    def forSubtypesOfObject(
        isKnownType:       Array[Boolean],
        isInterfaceType:   Array[Boolean],
        theClassTypes:     UIDSet[ClassType],
        theInterfaceTypes: UIDSet[ClassType],
        initialAllTypes:   UIDSet[ClassType] // just used to increase "sharing" possibilities
    ): SupertypeInformation = {
        if (theInterfaceTypes.isEmpty) {
            if (theClassTypes.isEmpty) {
                Unknown
            } else if (theClassTypes.isSingletonSet && (theClassTypes.head eq ClassType.Object)) {
                JustObject
            } else {
                new SupertypeInformation {
                    override final val classTypes: UIDSet[ClassType] = theClassTypes
                    override final def interfaceTypes: UIDSet[ClassType] = UIDSet.empty
                    override final def allTypes: UIDSet[ClassType] = classTypes
                    override final def iterator: Iterator[ClassType] = classTypes.iterator
                    override final def contains(t: ClassType): Boolean = {
                        val tid = t.id
                        tid == ClassType.ObjectId || (
                            tid < isKnownType.length && isKnownType(tid) &&
                            !isInterfaceType(tid) &&
                            classTypes.containsId(t.id)
                        )
                    }
                    override private[br] final def containsId(classTypeId: Int): Boolean = {
                        classTypes.containsId(classTypeId)
                    }
                }
            }
        } else {
            if (theClassTypes.isEmpty) {
                // we have an interface type with an incomplete type hierarchy
                new SupertypeInformation {
                    override final def classTypes: UIDSet[ClassType] = UIDSet.empty
                    override final val interfaceTypes: UIDSet[ClassType] = theInterfaceTypes
                    override final def allTypes: UIDSet[ClassType] = interfaceTypes
                    override final def iterator: Iterator[ClassType] = interfaceTypes.iterator
                    override final def contains(t: ClassType): Boolean = {
                        val tid = t.id
                        tid == ClassType.ObjectId || (
                            tid < isKnownType.length && isKnownType(tid) &&
                            isInterfaceType(tid) &&
                            interfaceTypes.containsId(t.id)
                        )
                    }
                    override private[br] final def containsId(classTypeId: Int): Boolean = {
                        interfaceTypes.containsId(classTypeId)
                    }
                }
            } else if (theClassTypes.isSingletonSet && (theClassTypes.head eq ClassType.Object)) {
                new SupertypeInformation {
                    override final def classTypes: UIDSet[ClassType] = ClassHierarchy.JustObject
                    override final val interfaceTypes: UIDSet[ClassType] = theInterfaceTypes
                    override final val allTypes: UIDSet[ClassType] = {
                        initialAllTypes + ClassType.Object ++ theInterfaceTypes
                    }
                    override final def iterator: Iterator[ClassType] = allTypes.iterator
                    override final def contains(t: ClassType): Boolean = {
                        val tid = t.id
                        tid == ClassType.ObjectId || (
                            tid < isKnownType.length && isKnownType(tid) &&
                            isInterfaceType(tid) &&
                            interfaceTypes.containsId(t.id)
                        )
                    }
                    override private[br] final def containsId(classTypeId: Int): Boolean = {
                        interfaceTypes.containsId(classTypeId)
                    }
                }
            } else {
                new SupertypeInformation {
                    override final val classTypes: UIDSet[ClassType] = theClassTypes
                    override final val interfaceTypes: UIDSet[ClassType] = theInterfaceTypes
                    override final val allTypes: UIDSet[ClassType] = {
                        initialAllTypes ++ classTypes ++ interfaceTypes
                    }
                    override final def iterator: Iterator[ClassType] = allTypes.iterator
                    override final def contains(t: ClassType): Boolean = {
                        val tid = t.id
                        tid == ClassType.ObjectId || (
                            tid < isKnownType.length && isKnownType(tid) &&
                            allTypes.containsId(t.id)
                        )
                    }
                    override private[br] final def containsId(classTypeId: Int): Boolean = {
                        allTypes.containsId(classTypeId)
                    }
                }
            }
        }
    }
}
