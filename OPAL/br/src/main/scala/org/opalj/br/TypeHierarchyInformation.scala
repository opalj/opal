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
    def classTypes: UIDSet[ObjectType]
    def interfaceTypes: UIDSet[ObjectType]
    /**
     * The set of all types; cached if useful.
     */
    def allTypes: UIDSet[ObjectType]

    final def size: Int = allTypes.size

    def foreach[T](f: ObjectType => T): Unit = allTypes.foreach(f)

    def iterator: Iterator[ObjectType]

    def foreachIterator: ForeachRefIterator[ObjectType] = allTypes.foreachIterator

    def forall(f: ObjectType => Boolean): Boolean = allTypes.forall(f)

    def exists(f: ObjectType => Boolean): Boolean = allTypes.exists(f)

    def foldLeft[B](z: B)(op: (B, ObjectType) => B): B = allTypes.foldLeft(z)(op)

    /**
     * Tests if the given type belongs to the super/subtype of `this` type; this
     * test is not reflexive. I.e., if this information was computed for the type
     * X and contains is called with X, `false` will be returned!
     */
    def contains(t: ObjectType): Boolean

    /**
     * Checks if the objectTypeId is contained in the underlying set; no special cases
     * related to `java.lang.Object` are supported!
     */
    private[br] def containsId(objectTypeId: Int): Boolean

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
        final override def classTypes: UIDSet[ObjectType] = UIDSet.empty
        final override def interfaceTypes: UIDSet[ObjectType] = UIDSet.empty
        final override def allTypes: UIDSet[ObjectType] = UIDSet.empty
        final override def contains(t: ObjectType): Boolean = false
        final override private[br] def containsId(objectTypeId: Int): Boolean = false
        final override def iterator: Iterator[ObjectType] = Iterator.empty
    }

    def forObject(
        theClassTypes:     UIDSet[ObjectType],
        theInterfaceTypes: UIDSet[ObjectType],
        initialAllTypes:   UIDSet[ObjectType]
    ): SubtypeInformation = {
        val theAllTypes = initialAllTypes ++ theClassTypes ++ theInterfaceTypes
        new SubtypeInformation {
            final override val classTypes: UIDSet[ObjectType] = theClassTypes
            final override val interfaceTypes: UIDSet[ObjectType] = theInterfaceTypes
            final override val allTypes: UIDSet[ObjectType] = theAllTypes
            final override def iterator: Iterator[ObjectType] = allTypes.iterator
            final override def contains(t: ObjectType): Boolean = t ne ObjectType.Object
            final override private[br] def containsId(objectTypeId: Int): Boolean = {
                objectTypeId != ObjectType.ObjectId
            }
        }
    }

    def forSubtypesOfObject(
        isKnownType:       Array[Boolean],
        isInterfaceType:   Array[Boolean],
        theClassTypes:     UIDSet[ObjectType],
        theInterfaceTypes: UIDSet[ObjectType],
        initialAllTypes:   UIDSet[ObjectType] // just used to increase "sharing" possibilities
    ): SubtypeInformation = {
        if (theClassTypes.isEmpty) {
            if (theInterfaceTypes.isEmpty)
                None
            else
                new SubtypeInformation { // ... if all subtypes are interfaces!
                    final override def classTypes: UIDSet[ObjectType] = UIDSet.empty
                    final override val interfaceTypes: UIDSet[ObjectType] = theInterfaceTypes
                    final override def allTypes: UIDSet[ObjectType] = interfaceTypes
                    final override def iterator: Iterator[ObjectType] = interfaceTypes.iterator
                    final override def contains(t: ObjectType): Boolean = {
                        val tid = t.id
                        tid < isInterfaceType.length && isInterfaceType(tid) &&
                            interfaceTypes.containsId(tid)
                    }
                    final override private[br] def containsId(objectTypeId: Int): Boolean = {
                        interfaceTypes.containsId(objectTypeId)
                    }
                }
        } else if (theInterfaceTypes.isEmpty) {
            new SubtypeInformation {
                final override val classTypes: UIDSet[ObjectType] = theClassTypes
                final override def interfaceTypes: UIDSet[ObjectType] = UIDSet.empty
                final override def allTypes: UIDSet[ObjectType] = classTypes
                final override def iterator: Iterator[ObjectType] = classTypes.iterator
                final override def contains(t: ObjectType): Boolean = {
                    val tid = t.id
                    // the first three checks are just guard checks..
                    tid != ObjectType.ObjectId &&
                        tid < isKnownType.length && isKnownType(tid) && !isInterfaceType(tid) &&
                        classTypes.containsId(tid)
                }
                final override private[br] def containsId(objectTypeId: Int): Boolean = {
                    classTypes.containsId(objectTypeId)
                }
            }
        } else {
            val theAllTypes = initialAllTypes ++ theClassTypes ++ theInterfaceTypes
            new SubtypeInformation {
                final override val classTypes: UIDSet[ObjectType] = theClassTypes
                final override val interfaceTypes: UIDSet[ObjectType] = theInterfaceTypes
                // We precompute the information to ensure that tests that will fail will
                // only take half as many steps... (see containsID)
                final override val allTypes: UIDSet[ObjectType] = theAllTypes
                final override def iterator: Iterator[ObjectType] = allTypes.iterator
                final override def contains(t: ObjectType): Boolean = {
                    val tid = t.id
                    // the first two checks are just guard checks...
                    tid != ObjectType.ObjectId &&
                        tid < isKnownType.length && isKnownType(tid) &&
                        allTypes.containsId(tid)
                }
                final override private[br] def containsId(objectTypeId: Int): Boolean = {
                    allTypes.containsId(objectTypeId)
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
        final override def classTypes: UIDSet[ObjectType] = ClassHierarchy.JustObject
        final override def interfaceTypes: UIDSet[ObjectType] = UIDSet.empty
        final override def allTypes: UIDSet[ObjectType] = ClassHierarchy.JustObject
        final override def iterator: Iterator[ObjectType] = Iterator(ObjectType.Object)
        final override def contains(t: ObjectType): Boolean = t eq ObjectType.Object
        final override private[br] def containsId(objectTypeId: Int): Boolean = {
            ObjectType.ObjectId == objectTypeId
        }
    }

    // Required in case of incomplete type hierarchies:
    final val Unknown: SupertypeInformation = new SupertypeInformation {
        final override def classTypes: UIDSet[ObjectType] = UIDSet.empty
        final override def interfaceTypes: UIDSet[ObjectType] = UIDSet.empty
        final override def allTypes: UIDSet[ObjectType] = UIDSet.empty
        final override def iterator: Iterator[ObjectType] = Iterator.empty
        final override def contains(t: ObjectType): Boolean = t eq ObjectType.Object
        final override private[br] def containsId(objectTypeId: Int): Boolean = false
    }

    final val ForObject: SupertypeInformation = new SupertypeInformation {
        final override val classTypes: UIDSet[ObjectType] = UIDSet.empty
        final override def interfaceTypes: UIDSet[ObjectType] = UIDSet.empty
        final override def allTypes: UIDSet[ObjectType] = UIDSet.empty
        final override def iterator: Iterator[ObjectType] = Iterator.empty
        final override def contains(t: ObjectType): Boolean = false
        final override private[br] def containsId(objectTypeId: Int): Boolean = false
    }

    def forSubtypesOfObject(
        isKnownType:       Array[Boolean],
        isInterfaceType:   Array[Boolean],
        theClassTypes:     UIDSet[ObjectType],
        theInterfaceTypes: UIDSet[ObjectType],
        initialAllTypes:   UIDSet[ObjectType] // just used to increase "sharing" possibilities
    ): SupertypeInformation = {
        if (theInterfaceTypes.isEmpty) {
            if (theClassTypes.isEmpty) {
                Unknown
            } else if (theClassTypes.isSingletonSet && (theClassTypes.head eq ObjectType.Object)) {
                JustObject
            } else {
                new SupertypeInformation {
                    final override val classTypes: UIDSet[ObjectType] = theClassTypes
                    final override def interfaceTypes: UIDSet[ObjectType] = UIDSet.empty
                    final override def allTypes: UIDSet[ObjectType] = classTypes
                    final override def iterator: Iterator[ObjectType] = classTypes.iterator
                    final override def contains(t: ObjectType): Boolean = {
                        val tid = t.id
                        tid == ObjectType.ObjectId || (
                            tid < isKnownType.length && isKnownType(tid) &&
                            !isInterfaceType(tid) &&
                            classTypes.containsId(t.id)
                        )
                    }
                    final override private[br] def containsId(objectTypeId: Int): Boolean = {
                        classTypes.containsId(objectTypeId)
                    }
                }
            }
        } else {
            if (theClassTypes.isEmpty) {
                // we have an interface type with an incomplete type hierarchy
                new SupertypeInformation {
                    final override def classTypes: UIDSet[ObjectType] = UIDSet.empty
                    final override val interfaceTypes: UIDSet[ObjectType] = theInterfaceTypes
                    final override def allTypes: UIDSet[ObjectType] = interfaceTypes
                    final override def iterator: Iterator[ObjectType] = interfaceTypes.iterator
                    final override def contains(t: ObjectType): Boolean = {
                        val tid = t.id
                        tid == ObjectType.ObjectId || (
                            tid < isKnownType.length && isKnownType(tid) &&
                            isInterfaceType(tid) &&
                            interfaceTypes.containsId(t.id)
                        )
                    }
                    final override private[br] def containsId(objectTypeId: Int): Boolean = {
                        interfaceTypes.containsId(objectTypeId)
                    }
                }
            } else if (theClassTypes.isSingletonSet && (theClassTypes.head eq ObjectType.Object)) {
                new SupertypeInformation {
                    final override def classTypes: UIDSet[ObjectType] = ClassHierarchy.JustObject
                    final override val interfaceTypes: UIDSet[ObjectType] = theInterfaceTypes
                    final override val allTypes: UIDSet[ObjectType] = {
                        initialAllTypes + ObjectType.Object ++ theInterfaceTypes
                    }
                    final override def iterator: Iterator[ObjectType] = allTypes.iterator
                    final override def contains(t: ObjectType): Boolean = {
                        val tid = t.id
                        tid == ObjectType.ObjectId || (
                            tid < isKnownType.length && isKnownType(tid) &&
                            isInterfaceType(tid) &&
                            interfaceTypes.containsId(t.id)
                        )
                    }
                    final override private[br] def containsId(objectTypeId: Int): Boolean = {
                        interfaceTypes.containsId(objectTypeId)
                    }
                }
            } else {
                new SupertypeInformation {
                    final override val classTypes: UIDSet[ObjectType] = theClassTypes
                    final override val interfaceTypes: UIDSet[ObjectType] = theInterfaceTypes
                    final override val allTypes: UIDSet[ObjectType] = {
                        initialAllTypes ++ classTypes ++ interfaceTypes
                    }
                    final override def iterator: Iterator[ObjectType] = allTypes.iterator
                    final override def contains(t: ObjectType): Boolean = {
                        val tid = t.id
                        tid == ObjectType.ObjectId || (
                            tid < isKnownType.length && isKnownType(tid) &&
                            allTypes.containsId(t.id)
                        )
                    }
                    final override private[br] def containsId(objectTypeId: Int): Boolean = {
                        allTypes.containsId(objectTypeId)
                    }
                }
            }
        }
    }
}
