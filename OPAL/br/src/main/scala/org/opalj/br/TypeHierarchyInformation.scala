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
    /**
     * The set of all types; cached if usefull.
     */
    def allTypes: UIDSet[ObjectType]

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

    def contains(t: ObjectType): Boolean

    /**
     * Checks if the objectTypeId is contained in the underlying set; no special cases
     * related to `java.lang.Object` are supported!
     */
    private[br] def containsId(objectTypeId : Int): Boolean

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
        final override private[br] def containsId(objectTypeId : Int): Boolean = false
    }

    def forObject(
        theClassTypes:     UIDSet[ObjectType],
        theInterfaceTypes: UIDSet[ObjectType],
        initialAllTypes:   UIDSet[ObjectType] // just used to increase "sharing" possibilities
    ): SubtypeInformation = {
        val theAllTypes = initialAllTypes ++ theClassTypes ++ theInterfaceTypes
        new SubtypeInformation {
            final override val classTypes: UIDSet[ObjectType] = theClassTypes
            final override val interfaceTypes: UIDSet[ObjectType] = theInterfaceTypes
            final override val allTypes: UIDSet[ObjectType] = theAllTypes
            final override def contains(t: ObjectType): Boolean = true
            final override private[br] def containsId(objectTypeId : Int): Boolean = true
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
                new SubtypeInformation {
                    final override def classTypes: UIDSet[ObjectType] = UIDSet.empty
                    final override val interfaceTypes: UIDSet[ObjectType] = theInterfaceTypes
                    final override def allTypes: UIDSet[ObjectType] = interfaceTypes
                    final override def contains(t: ObjectType): Boolean = {
                        val tid = t.id
                        // the first three checks are just guard checks..
                        tid != ObjectType.ObjectId &&
                            tid < isKnownType.length && isKnownType(tid) &&
                            isInterfaceType(tid) &&
                            interfaceTypes.containsId(tid)
                    }
                    final override private[br] def containsId(objectTypeId : Int): Boolean = {
                        interfaceTypes.containsId(objectTypeId)
                    }
                }
        } else if (theInterfaceTypes.isEmpty) {
            new SubtypeInformation {
                final override val classTypes: UIDSet[ObjectType] = theClassTypes
                final override def interfaceTypes: UIDSet[ObjectType] = UIDSet.empty
                final override def allTypes: UIDSet[ObjectType] = classTypes
                final override def contains(t: ObjectType): Boolean = {
                    val tid = t.id
                    // the first three checks are just guard checks..
                    tid != ObjectType.ObjectId &&
                        tid < isKnownType.length && isKnownType(tid) &&
                        !isInterfaceType(tid) &&
                        classTypes.containsId(tid)
                }
                final override private[br] def containsId(objectTypeId : Int): Boolean = {
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
                final override def contains(t: ObjectType): Boolean = {
                    val tid = t.id
                    // the first two checks are just guard checks...
                    tid != ObjectType.ObjectId &&
                        tid < isKnownType.length && isKnownType(tid) &&
                        allTypes.containsId(tid)
                }
                final override private[br] def containsId(objectTypeId : Int): Boolean = {
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
        final override def contains(t: ObjectType): Boolean = t eq ObjectType.Object
        final override private[br] def containsId(objectTypeId : Int): Boolean = {
            ObjectType.ObjectId == objectTypeId
        }
    }

    // Required in case of incomplete type hierarchies:
    final val Unknown: SupertypeInformation = new SupertypeInformation {
        final override def classTypes: UIDSet[ObjectType] = UIDSet.empty
        final override def interfaceTypes: UIDSet[ObjectType] = UIDSet.empty
        final override def allTypes: UIDSet[ObjectType] = UIDSet.empty
        final override def contains(t: ObjectType): Boolean = t eq ObjectType.Object
        final override private[br] def containsId(objectTypeId : Int): Boolean = false
    }

    final val ForObject: SupertypeInformation = new SupertypeInformation {
        final override val classTypes: UIDSet[ObjectType] = UIDSet.empty
        final override def interfaceTypes: UIDSet[ObjectType] = UIDSet.empty
        final override def allTypes: UIDSet[ObjectType] = UIDSet.empty
        final override def contains(t: ObjectType): Boolean = false
        final override private[br] def containsId(objectTypeId : Int): Boolean = false
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
                    final override def contains(t: ObjectType): Boolean = {
                        val tid = t.id
                        tid == ObjectType.ObjectId || (
                            tid < isKnownType.length && isKnownType(tid) &&
                            !isInterfaceType(tid) &&
                            classTypes.containsId(t.id)
                        )
                    }
                    final override private[br] def containsId(objectTypeId : Int): Boolean = {
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
                    final override def contains(t: ObjectType): Boolean = {
                        val tid = t.id
                        tid == ObjectType.ObjectId || (
                            tid < isKnownType.length && isKnownType(tid) &&
                            isInterfaceType(tid) &&
                            interfaceTypes.containsId(t.id)
                        )
                    }
                    final override private[br] def containsId(objectTypeId : Int): Boolean = {
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
                    final override def contains(t: ObjectType): Boolean = {
                        val tid = t.id
                        tid == ObjectType.ObjectId || (
                            tid < isKnownType.length && isKnownType(tid) &&
                            isInterfaceType(tid) &&
                            interfaceTypes.containsId(t.id)
                        )
                    }
                    final override private[br] def containsId(objectTypeId : Int): Boolean = {
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
                    final override def contains(t: ObjectType): Boolean = {
                        val tid = t.id
                        tid == ObjectType.ObjectId || (
                            tid < isKnownType.length && isKnownType(tid) &&
                            allTypes.containsId(t.id)
                        )
                    }
                    final override private[br] def containsId(objectTypeId : Int): Boolean = {
                        allTypes.containsId(objectTypeId)
                    }
                }
            }
        }
    }
}
