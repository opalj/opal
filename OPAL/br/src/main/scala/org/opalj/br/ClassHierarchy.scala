/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import scala.language.implicitConversions
import scala.annotation.tailrec

import java.io.InputStream
import java.util.concurrent.locks.ReentrantReadWriteLock

import scala.collection.mutable
import scala.jdk.CollectionConverters._
import scala.concurrent.Await.{result => await}
import scala.concurrent.Future
import scala.concurrent.duration.Duration.Inf
import scala.concurrent.ExecutionContext
import scala.io.BufferedSource

import it.unimi.dsi.fastutil.ints.Int2IntArrayMap
import org.opalj.control.foreachNonNullValue
import org.opalj.graphs.Node
import org.opalj.io.process
import org.opalj.io.processSource

import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger
import org.opalj.log.Warn
import org.opalj.concurrent.OPALUnboundedExecutionContext
import org.opalj.collection.ForeachRefIterator
import org.opalj.collection.EqualSets
import org.opalj.collection.IntIterator
import org.opalj.collection.StrictSubset
import org.opalj.collection.StrictSuperset
import org.opalj.collection.UncomparableSets
import org.opalj.collection.immutable.UIDSet
import org.opalj.collection.immutable.UIDSet1
import org.opalj.collection.CompleteCollection
import org.opalj.collection.IncompleteCollection
import org.opalj.collection.QualifiedCollection
import org.opalj.br.ObjectType.Object

/**
 * Represents '''a project's class hierarchy'''. The class hierarchy only contains
 * information about those classes that were explicitly added to it. Hence, the class hierarchy
 * may contain holes. However, the type `java.lang.Object` is always part of the class hierarchy.
 *
 * ==Thread safety==
 * This class is effectively immutable; concurrent access to the class hierarchy is supported.
 *
 * @note    Java 9 module definitions are completely ignored.
 *
 * @note    Unless explicitly documented, it is an error to pass an instance of `ObjectType`
 *          to any method if the `ObjectType` was not previously added. If in doubt, first
 *          check if the type is known (`isKnown`/`ifKnown`).
 *
 * @param   knownTypesMap A mapping between the id of an object type and the object type;
 *          implicitly encodes which types are known.
 *
 * @param   isInterfaceTypeMap `true` iff the type is an interface otherwise `false`;
 *          '''only defined for those types that are known'''.
 *
 * @param   isKnownToBeFinalMap `true` if the class is known to be `final`. I.e.,
 *          if the class is final `isFinal(ClassFile(objectType)) =>
 *          isFinal(classHierarchy(objectType))`.
 *
 * @param   superclassTypeMap Contains type information about a type's immediate superclass.
 *          This value is always defined (i.e., not null) unless the key identifies the
 *          object type `java.lang.Object` or when the respective class file was not
 *          analyzed and the respective type was only seen in the declaration of another class.
 *
 * @param   superinterfaceTypesMap Contains type information about a type's directly
 *          implemented interfaces; if any.
 *
 * @param   subclassTypesMap Contains type information about a type's subclasses; if any.
 *
 * @param   subinterfaceTypesMap Contains type information about a type's subinterfaces.
 *          They only ''class type'' that is allowed to have a non-empty set of subinterfaces
 *          is `java.lang.Object`.
 *
 * @param   rootTypes  The set of ''all types'' which have no supertypes or for which we have
 *          no further supertype information because of an incomplete project.
 *          If the class hierarchy is complete, then this set contains exactly one element and
 *          that element must identify `java.lang.Object`.
 *
 *          If we load an application and all the jars used to implement it or a library
 *          and all the libraries it depends on, then the class hierarchy '''should not'''
 *          contain multiple root types. However, the (complete) JDK already contains
 *          some references to Eclipse classes which are not part of the JDK.
 *
 * @param   leafTypes The set of all types which have no subtypes.
 *
 * @param   isSupertypeInformationCompleteMap A map which stores for each ''known type''
 *          if its supertype information is complete.
 *
 * @param   supertypeInformationMap Contains for each object type the set of class types and
 *          interface types it inherits from. This set is computed on a best-effort basis.
 *
 *          In some cases the supertype information may be incomplete, because the project
 *          as such is incomplete. Whether the type information is complete for a given type
 *          or not can be checked using `isSupertypeInformationComplete`.
 *
 * @author Michael Eichberg
 */
class ClassHierarchy private (
        // the case "java.lang.Object" is handled explicitly!
        private[this] val knownTypesMap:       Array[ObjectType],
        private[this] val isKnownTypeMap:      Array[Boolean],
        private[this] val isInterfaceTypeMap:  Array[Boolean],
        private[this] val isKnownToBeFinalMap: Array[Boolean],

        // The element is null for types for which we have no complete information
        // (unless it is java.lang.Object)!
        private[this] val superclassTypeMap:      Array[ObjectType],
        private[this] val superinterfaceTypesMap: Array[UIDSet[ObjectType]],

        // In the following all elements are non-null for each known type!
        private[this] val subclassTypesMap:     Array[UIDSet[ObjectType]],
        private[this] val subinterfaceTypesMap: Array[UIDSet[ObjectType]],

        // DERIVED INFORMATION
        val rootTypes:                                       UIDSet[ObjectType],
        val leafTypes:                                       UIDSet[ObjectType],
        private[this] val isSupertypeInformationCompleteMap: Array[Boolean],

        private[this] val supertypeInformationMap: Array[SupertypeInformation],
        private[this] val subtypeInformationMap:   Array[SubtypeInformation]
)(
        implicit
        val logContext: LogContext
) {

    def updatedLogContext(newLogContext: LogContext): ClassHierarchy = {
        new ClassHierarchy(
            knownTypesMap,
            isKnownTypeMap,
            isInterfaceTypeMap,
            isKnownToBeFinalMap,
            superclassTypeMap,
            superinterfaceTypesMap,
            subclassTypesMap,
            subinterfaceTypesMap,
            rootTypes,
            leafTypes,
            isSupertypeInformationCompleteMap,
            supertypeInformationMap,
            subtypeInformationMap
        )(
            newLogContext
        )
    }

    // TODO Use all subTypes/subclassTypes/subinterfaceTypes
    // TODO Use all supertypes/superclassTypes/superinterfaceTypes
    // TODO Precompute all subTypesCF/subclassTypesCF/subinterfaceTypesCF
    // TODO Precompute all supertypesCF/superclassTypesCF/superinterfaceTypesCF

    /**
     * The set of ''all class types'' (excluding interfaces) which have no super type or
     * for which the supertype information is incomplete; that is all (pseudo) root types.
     * If the class hierarchy is complete, then this set contains exactly one element and
     * that element must identify `java.lang.Object`.
     *
     * @note    The returned root class types are not necessarily a subset of `rootTypes`.
     *          A class which has an unknown super class, but implements a known interface
     *          is considered to belong to be a root class type  but is not a member of
     *          `rootTypes` because some supertype information exists!
     *
     * @note    If we load an application and all the jars used to implement it or a library
     *          and all the library it depends on, then the class hierarchy '''should not'''
     *          contain multiple root types. However, the (complete) JDK contains some references
     *          to Eclipse classes which are not part of the JDK.
     */
    def rootClassTypesIterator: Iterator[ObjectType] = {
        knownTypesMap.iterator filter { objectType =>
            (objectType ne null) && {
                val oid = objectType.id
                (superclassTypeMap(oid) eq null) && !isInterfaceTypeMap(oid)
            }
        }
    }

    def leafClassTypesIterator: Iterator[ObjectType] = {
        leafTypes.iterator filterNot { objectType => isInterfaceTypeMap(objectType.id) }
    }

    /**
     * Iterates over all interfaces which only inherit from `java.lang.Object` and adds the
     * types to the given `Growable` collection. I.e., iterates
     * over all interfaces which are at the top of the interface inheritance hierarchy.
     */
    def rootInterfaceTypes(collection: mutable.Growable[ObjectType]): collection.type = {
        superinterfaceTypesMap.iterator.zipWithIndex foreach { si =>
            val (superinterfaceTypes, id) = si
            if (superinterfaceTypes != null && superinterfaceTypes.isEmpty && isInterface(id)) {
                collection += knownTypesMap(id)
            }
        }
        collection
    }

    /**
     * A dump of the class hierarchy information in TSV format.
     * The following information will be dumped:
     *  - type
     *  - id
     *  - (is) interface
     *  - (is) final
     *  - (is) root type
     *  - (is) leaf type
     *  - (is) supertype information complete
     *  - super class
     *  - super interfaces
     *  - sub classes
     *  - sub interfaces
     */
    def asTSV: String = {

        implicit def objectTypeToString(ot: ObjectType): String = {
            if (ot ne null) ot.toJava else "N/A"
        }

        implicit def objectTypesToString(ots: UIDSet[ObjectType]): String = {
            if (ots ne null) ots.map(_.toJava).mkString("{", ",", "}") else "N/A"
        }

        case class TypeInfo(
                objectType:                     String,
                objectTypeId:                   Int,
                isInterface:                    Boolean,
                isFinal:                        Boolean,
                isRootType:                     Boolean,
                isLeafType:                     Boolean,
                isSupertypeInformationComplete: Boolean,
                superclassType:                 String,
                superinterfaceTypes:            String,
                subclassTypes:                  String,
                subinterfaceTypes:              String
        ) {
            override def toString: String = {
                s"$objectType \t$objectTypeId \t$isInterface \t$isFinal \t$isRootType \t$isLeafType \t"+
                    s"$isSupertypeInformationComplete \t$superclassType \t$superinterfaceTypes \t"+
                    s"$subclassTypes \t$subinterfaceTypes"
            }
        }

        val rootTypes = this.rootTypes.toSet
        val typeInfos =
            (0 until knownTypesMap.length) filter { i => knownTypesMap(i) ne null } map { i =>
                val t = knownTypesMap(i)
                TypeInfo(
                    t,
                    t.id,
                    isInterfaceTypeMap(i),
                    isKnownToBeFinalMap(i),
                    rootTypes.contains(t),
                    leafTypes.contains(t),
                    isSupertypeInformationCompleteMap(i),
                    superclassTypeMap(i),
                    superinterfaceTypesMap(i),
                    subclassTypesMap(i),
                    subinterfaceTypesMap(i)
                ).toString
            }

        val header =
            "type \tid \tinterface \tfinal"+
                " \troot type \tleaf type \tsupertype information complete"+
                " \tsuper class \tsuper interfaces \tsub classes \tsub interfaces\n"
        typeInfos.sorted.mkString(header, "\n", "\n")
    }

    //
    //
    // IMPLEMENTS THE MAPPING BETWEEN AN ObjectType AND IT'S ID
    //
    //

    private[this] var objectTypesMap: Array[ObjectType] = new Array(ObjectType.objectTypesCount)
    private[this] final val objectTypesMapRWLock = new ReentrantReadWriteLock()

    private[this] final def objectTypesCreationListener(objectType: ObjectType): Unit = {
        val id = objectType.id
        val writeLock = objectTypesMapRWLock.writeLock()
        writeLock.lock()
        try {
            val thisObjectTypesMap = objectTypesMap
            if (id >= thisObjectTypesMap.length) {
                val newLength = Math.max(ObjectType.objectTypesCount, id) + 100
                val newObjectTypesMap = new Array[ObjectType](newLength)
                Array.copy(thisObjectTypesMap, 0, newObjectTypesMap, 0, thisObjectTypesMap.length)
                newObjectTypesMap(id) = objectType
                objectTypesMap = newObjectTypesMap
            } else {
                thisObjectTypesMap(id) = objectType
            }
        } finally {
            writeLock.unlock()
        }
    }

    ObjectType.setObjectTypeCreationListener(objectTypesCreationListener)

    /**
     * Returns the `ObjectType` with the given Id. The id has to be the id of a valid
     * ObjectType.
     */
    final def getObjectType(objectTypeId: Int): ObjectType = {
        val readLock = objectTypesMapRWLock.readLock()
        readLock.lock()
        try {
            val ot = objectTypesMap(objectTypeId)
            if (ot eq null) {
                throw new IllegalArgumentException("ObjectType id invalid: "+objectTypeId)
            }
            ot
        } finally {
            readLock.unlock()
        }
    }

    //
    //
    // REGULAR METHODS
    //
    //

    /**
     * Returns the supertype information if the given type is known. If the given type is unknown
     * `None` is returned.
     */
    def supertypeInformation(objectType: ObjectType): Option[SupertypeInformation] = {
        val oid = objectType.id
        if (isKnown(oid)) {
            Some(supertypeInformationMap(oid))
        } else {
            None
        }
    }

    /**
     * Returns the subtype information if the given type is known. If the given type is unknown
     * `None` is returned.
     */
    def subtypeInformation(objectType: ObjectType): Option[SubtypeInformation] = {
        val oid = objectType.id
        if (isKnown(oid)) {
            Some(subtypeInformationMap(oid))
        } else {
            None
        }
    }

    /**
     * Returns `true` if the class hierarchy has some information about the given
     * type.
     *
     * @note    Consider using isKnown(objectTypeId : Int) if you need the object ids anyway.
     */
    @inline final def isKnown(objectType: ObjectType): Boolean = isKnown(objectType.id)

    @inline final def isKnown(objectTypeId: Int): Boolean = {
        val isKnownTypeMap = this.isKnownTypeMap
        objectTypeId < isKnownTypeMap.length && isKnownTypeMap(objectTypeId)
    }

    /**
     * Returns `true` if the type is unknown. This is `true` for all types that are
     * referred to in the body of a method, but which are not referred to in the
     * declarations of the class files that were analyzed.
     *
     * @note    Consider using isUnknown(objectTypeId : Int) if you need the object ids anyway.
     */
    @inline final def isUnknown(objectType: ObjectType): Boolean = isUnknown(objectType.id)

    @inline final def isUnknown(objectTypeId: Int): Boolean = {
        val isKnownTypeMap = this.isKnownTypeMap
        objectTypeId >= knownTypesMap.length || !isKnownTypeMap(objectTypeId)
    }

    /**
     * Tests if the given objectType is known and if so executes the given function.
     *
     * @example
     * {{{
     * ifKnown(ObjectType.Serializable){isDirectSupertypeInformationComplete}
     * }}}
     */
    @inline final def ifKnown[T](objectType: ObjectType)(f: ObjectType => T): Option[T] = {
        if (isKnown(objectType))
            Some(f(objectType))
        else
            None
    }

    /**
     * Calls the given function `f` for each type that is known to the class hierarchy.
     */
    def foreachKnownType[T](f: ObjectType => T): Unit = {
        foreachNonNullValue(knownTypesMap)((_ /*index*/ , t) => f(t))
    }

    /**
     * Returns `true` if the given type is `final`. I.e., the declaring class
     * was explicitly declared `final` and no subtypes exist.
     *
     * @note No explicit `isKnown` check is required.
     *
     * @return  `false` is returned if:
     *  - the object type is unknown,
     *  - the object type is known not to be final or
     *  - the information is incomplete
     */
    @inline def isKnownToBeFinal(objectType: ObjectType): Boolean = {
        isKnownToBeFinal(objectType.id)
    }

    @inline def isKnownToBeFinal(objectTypeId: Int): Boolean = {
        isKnown(objectTypeId) && isKnownToBeFinalMap(objectTypeId)
    }

    /**
     * Returns `true` if the given type is known and is `final`. I.e., the declaring class
     * was explicitly declared final or – if the type identifies an array type –
     * the component type is either known to be final or is a primitive/base type.
     *
     * @note No explicit `isKnown` check is required.
     *
     * @return `false` is returned if:
     *  - the object type/component type is unknown,
     *  - the object type/component type is known not to be final or
     *  - the information about the object type/component type is incomplete
     */
    @inline def isKnownToBeFinal(referenceType: ReferenceType): Boolean = {
        referenceType match {
            case objectType: ObjectType =>
                isKnownToBeFinal(objectType)
            case arrayType: ArrayType =>
                val elementType = arrayType.elementType
                elementType.isBaseType || isKnownToBeFinal(elementType.asObjectType)
        }
    }

    /**
     * Returns `true` if the given `objectType` is known and defines an interface type.
     *
     * @note No explicit `isKnown` check is required.
     *
     * @param objectType An `ObjectType`.
     */
    @inline def isInterface(objectType: ObjectType): Answer = {
        val oid = objectType.id
        if (isUnknown(oid))
            Unknown
        else
            Answer(isInterfaceTypeMap(oid))
    }

    /** Returns  `true` if and only if the given type is known to define an interface! */
    @inline private[this] def isInterface(objectTypeId: Int): Boolean = {
        isKnown(objectTypeId) && isInterfaceTypeMap(objectTypeId)
    }

    @inline private[br] def unsafeIsInterface(objectTypeId: Int): Boolean = {
        isInterfaceTypeMap(objectTypeId)
    }

    /**
     * Returns `true` if the type hierarchy information related to the given type's
     * supertypes is complete.
     *
     * @note No explicit `isKnown` check is required.
     */
    @inline def isDirectSuperclassTypeInformationComplete(objectType: ObjectType): Boolean = {
        (objectType eq Object) || {
            val oid = objectType.id
            isKnown(oid) && superclassTypeMap(oid) != null
        }
    }

    /**
     * Returns `true` if the type hierarchy has complete information about all supertypes
     * of the given type.
     *
     * @note No explicit `isKnown` check is required.
     */
    @inline final def isSupertypeInformationComplete(objectType: ObjectType): Boolean = {
        val oid = objectType.id
        isKnown(oid) && isSupertypeInformationCompleteMap(oid)
    }

    /**
     * Returns `Yes` if the class hierarchy contains subtypes of the given type and `No` if
     * it contains no subtypes. `Unknown` is returned if the given type is not known.
     *
     * Please note, that the answer will be `No` even though the (running) project contains
     * (in)direct subtypes of the given type, but  the class hierarchy is not
     * complete. I.e., not all class files (libraries) used by the project are analyzed.
     * A second case is that some class files are generated at runtime that inherit from
     * the given `ObjectType`.
     *
     * @note    No explicit `isKnown` check is required.
     * @param   objectType Some `ObjectType`.
     */
    def hasSubtypes(objectType: ObjectType): Answer = {
        val oid = objectType.id
        if (isUnknown(oid)) {
            Unknown
        } else {
            Answer(subclassTypesMap(oid).nonEmpty || subinterfaceTypesMap(oid).nonEmpty)
        }
    }

    /**
     * The set of all class- and interface-types that (directly or indirectly)
     * inherit from the given type.
     *
     * @note    No explicit `isKnown` check is required; if the type is unknown the
     *          returned set will be empty unless `reflexive` is true.
     * @note    If you don't need the set, it is more efficient to use `foreachSubtype`.
     * @note    If the type hierarchy is not complete, the answer may also be incomplete.
     *          E.g., if x inherits from y and y inherits from z, but y is not known to the
     *          class hierarchy then x will not be in the set of all (known) subtypes of z.
     *
     * @param   objectType An `ObjectType`.
     * @param   reflexive If `true` the given type is also included in the returned
     *          set.
     * @return  The set of all direct and indirect subtypes of the given type.
     */
    def allSubtypes(objectType: ObjectType, reflexive: Boolean): Set[ObjectType] = {
        val oid = objectType.id
        if (isUnknown(oid))
            return if (reflexive) UIDSet1(objectType) else UIDSet.empty

        if (reflexive)
            subtypeInformationMap(oid).allTypes + objectType
        else
            subtypeInformationMap(oid).allTypes
    }

    def allSubtypesIterator(objectType: ObjectType, reflexive: Boolean): Iterator[ObjectType] = {
        val oid = objectType.id
        if (isUnknown(oid))
            return if (reflexive) Iterator(objectType) else Iterator.empty;

        if (reflexive)
            subtypeInformationMap(oid).iterator ++ Iterator(objectType)
        else
            subtypeInformationMap(oid).iterator
    }

    def allSubtypesForeachIterator(
        objectType: ObjectType,
        reflexive:  Boolean
    ): ForeachRefIterator[ObjectType] = {
        val oid = objectType.id
        if (isUnknown(oid))
            return {
                if (reflexive)
                    ForeachRefIterator.single(objectType)
                else
                    ForeachRefIterator.empty
            };

        if (reflexive)
            subtypeInformationMap(oid).foreachIterator ++ ForeachRefIterator.single(objectType)
        else
            subtypeInformationMap(oid).foreachIterator
    }

    /**
     * Enables the guided processing of all subtypes of the given type. This function enables you
     * to compute some value based on the subtypes of the given type and – at the same time – to
     * control the process of traversing the subtypes.
     *
     * @param initial The initial value of the computation. This is also the value that
     *        will be returned if the given type has no subtypes and `reflexive` is `false`.
     * @param f A function that computes the new value given the current value and which also
     *        returns the information whether the computation should be aborted using the
     *        current value of type `t` or if the subtypes of the current subtype should be
     *        traversed. The commented signature of `f` is:
     *        {{{
     *        f: (T, ObjectType) => (T /*result*/ , Boolean /*skip subtypes*/ , Boolean /*abort*/ )
     *        }}}
     */
    def processSubtypes[@specialized(Boolean) T](
        objectType: ObjectType,
        reflexive:  Boolean    = false
    )(
        initial: T
    )(
        f: (T, ObjectType) => (T /*result*/ , Boolean /*skip subtypes*/ , Boolean /*abort*/ )
    ): T = {
        if (isUnknown(objectType))
            return initial;

        var processed = UIDSet.empty[ObjectType]

        def forallSubtypes(initial: T, objectType: ObjectType): (T, Boolean /*continue*/ ) = {
            val oid = objectType.id
            var t: T = initial
            val continue = {
                subclassTypesMap(oid).forall { subtype =>
                    val (newT, continue) = process(t, subtype); t = newT; continue
                } &&
                    subinterfaceTypesMap(oid).forall { subtype =>
                        val (newT, continue) = process(t, subtype); t = newT; continue
                    }
            }
            (t, continue)
        }

        def process(t: T, objectType: ObjectType): (T, Boolean /*continue*/ ) = {
            if (processed.contains(objectType))
                return (t, true);

            processed += objectType

            val (newT, skipSubtypes, abort) = f(t, objectType)
            if (abort) (newT, false)
            else if (skipSubtypes) (newT, true)
            else forallSubtypes(newT, objectType)
        }

        val (newResult, _) = {
            if (reflexive) {
                process(initial, objectType)
            } else {
                forallSubtypes(initial, objectType)
            }
        }
        newResult
    }

    /**
     * Calls the function `f` for each known (direct or indirect) subtype of the given type.
     *
     * @note    No explicit `isKnown` check is required; if the type is unknown nothing
     *          will happen.
     * @note    For details regarding incomplete class hierarchies see [[allSubtypes]].
     * *
     * @param   objectType An `ObjectType`.
     */
    def foreachSubtype(objectType: ObjectType)(f: ObjectType => Unit): Unit = {
        val oid = objectType.id
        if (isKnown(oid)) {
            subtypeInformationMap(oid).foreach(f)
        }
    }

    /**
     * Iterates over all subtypes of the given type, by first iterating over the subclass types
     * and then iterating over the subinterface types (if the given object type defines an
     * interface type or identifies `java.lang.Object`).
     *
     * @param    process The process function will be called for each subtype of the given type.
     *           If process returns false, subtypes of the current type will no longer be traversed.
     *           However, if a subtype of the current type is reachable via another path (by means
     *           of interface inheritance) then that subtype may be processed.
     *
     * @note    Classes are always traversed first.
     */
    def foreachSubtype(
        objectType: ObjectType,
        reflexive:  Boolean    = false
    )(
        process: ObjectType => Boolean
    ): Unit = {
        var processed = UIDSet.empty[ObjectType]
        def foreachSubtype(objectType: ObjectType): Unit = {
            if (processed.contains(objectType))
                return ;

            processed += objectType

            if (process(objectType)) {
                val oid = objectType.id
                subclassTypesMap(oid) foreach { foreachSubtype }
                subinterfaceTypesMap(oid) foreach { foreachSubtype }
            }
        }

        if (objectType == ObjectType.Object) {
            if (reflexive) {
                if (!process(ObjectType.Object))
                    return ;
            };

            rootTypes foreach { rootType =>
                if (rootType ne ObjectType.Object) {
                    foreachSubtype(rootType)
                } else {
                    // java.lang.Object is always known ...
                    subclassTypesMap(ObjectType.ObjectId) foreach { foreachSubtype }
                    subinterfaceTypesMap(ObjectType.ObjectId) foreach { foreachSubtype }
                }
            }

            return ;
        }

        if (isUnknown(objectType))
            return ;

        if (reflexive)
            foreachSubtype(objectType)
        else {
            val oid = objectType.id
            subclassTypesMap(oid) foreach { foreachSubtype }
            subinterfaceTypesMap(oid) foreach { foreachSubtype }
        }
    }

    def foreachSubtypeCF(
        objectType: ObjectType,
        reflexive:  Boolean    = false
    )(
        process: ClassFile => Boolean
    )(
        implicit
        project: ClassFileRepository
    ): Unit = {
        foreachSubtype(objectType, reflexive) { subtype =>
            project.classFile(subtype) match {
                case Some(classFile) => process(classFile)
                case _ /* None */    => true
            }
        }
    }

    /**
     * Executes the given function `f` for each subclass of the given `ObjectType`.
     * In this case the subclass relation is '''not reflexive'''. Furthermore, it may be
     * possible that f is invoked multiple times using the same `ClassFile` object if
     * the given objectType identifies an interface.
     *
     * Subtypes for which no `ClassFile` object is available are ignored.
     *
     * @note    No explicit `isKnown` check is required; if the type is unknown nothing
     *          will happen.
     * @note    For details regarding incomplete class hierarchies see `foreachSubtype`.
     */
    def foreachSubclass(
        objectType: ObjectType,
        project:    ClassFileRepository
    )(
        f: ClassFile => Unit
    ): Unit = {
        foreachSubtype(objectType) { subtype => project.classFile(subtype).foreach(f) }
    }

    /**
     * Returns all (direct and indirect) subclass types of the given class type.
     *
     * @note    No explicit `isKnown` check is required; if the type is unknown, an empty
     *          iterator is returned.
     */
    def allSubclassTypes(objectType: ObjectType, reflexive: Boolean): Iterator[ObjectType] = {
        val oid = objectType.id
        if (isUnknown(oid))
            return Iterator.empty;

        val subclassTypesIterator = subtypeInformationMap(oid).classTypes.iterator
        if (reflexive)
            Iterator(objectType) ++ subclassTypesIterator
        else
            subclassTypesIterator
    }

    /**
     * Executes the given function `f` for each known direct subclass of the given `ObjectType`.
     * In this case the subclass relation is '''not reflexive''' and interfaces inheriting from
     * the given object type are ignored.
     *
     * Subtypes for which no `ClassFile` object is available are ignored.
     *
     * @note    No explicit `isKnown` check is required; if the type is unknown nothing
     *          will happen.
     */
    def foreachDirectSubclassType[T](
        objectType: ObjectType,
        project:    ClassFileRepository
    )(
        f: ClassFile => T
    ): Unit = {
        val oid = objectType.id
        if (isUnknown(oid))
            return ;

        import project.classFile
        subclassTypesMap(oid) foreach { subtype => classFile(subtype).foreach(f) }
    }

    def directSubtypesCount(objectType: ObjectType): Int = {
        directSubtypesCount(objectType.id)
    }

    def directSubtypesCount(objectTypeId: Int): Int = {
        if (isUnknown(objectTypeId))
            return 0;

        subclassTypesMap(objectTypeId).size + subinterfaceTypesMap(objectTypeId).size
    }

    /**
     * Tests if a subtype of the given `ObjectType` exists that satisfies the given predicate.
     * In this case the subtype relation is '''not reflexive'''.
     *
     * Subtypes for which no `ClassFile` object is available are ignored.
     *
     * @note    No explicit `isKnown` check is required; if the type is unknown nothing
     *          will happen.
     */
    def existsSubclass(
        objectType: ObjectType,
        project:    ClassFileRepository
    )(
        p: ClassFile => Boolean
    ): Boolean = {
        foreachSubtype(objectType) { objectType =>
            val cfOption = project.classFile(objectType) // IMPROVE implement Project.classFile(ObjectTypeID:Int)
            if (cfOption.isDefined && p(cfOption.get))
                return true;
        }
        false
    }

    def foreachDirectSupertypeCF[U](
        objectType: ObjectType
    )(
        f: ClassFile => U
    )(
        implicit
        project: ClassFileRepository
    ): Unit = {
        val oid = objectType.id
        if (isUnknown(oid))
            return ;

        val superinterfaceTypes = superinterfaceTypesMap(oid)
        if (superinterfaceTypes ne null) {
            superinterfaceTypes foreach { t => project.classFile(t).foreach(f) }
        }

        val superclassType = superclassTypeMap(oid)
        if (superclassType ne null) project.classFile(superclassType).foreach(f)

    }

    def foreachDirectSupertype(objectType: ObjectType)(f: ObjectType => Unit): Unit = {
        if (isUnknown(objectType))
            return ;

        val oid = objectType.id
        val superinterfaceTypes = superinterfaceTypesMap(oid)
        if (superinterfaceTypes ne null) superinterfaceTypes.foreach(f)
        val superclassType = superclassTypeMap(oid)
        if (superclassType ne null) f(superclassType)
    }

    /**
     * Calls the given function `f` for each of the given type's supertypes.
     */
    def foreachSupertype(
        ot:        ObjectType,
        reflexive: Boolean    = false
    )(
        f: ObjectType => Unit
    ): Unit = {
        val oid = ot.id
        if (reflexive) f(ot)
        if (isKnown(oid)) {
            supertypeInformationMap(oid).foreach(f)
        }
    }

    /**
     * Returns the list of all super types in initialization order.
     * I.e., it will return the top level super class first - i.e., `java.lang.Object`
     * and then all sub class types.
     *
     * If the given type is `java.lang.Object`, the empty list is returned.
     *
     * Interfaces are not further considered, because they generally don't need any instance
     * initialization. If the given type is an interface type, the returned list will hence only
     * contain `java.lang.Object`.
     *
     * @note    If the class hierarchy is not complete, it may happen that the super class chain
     *             is not complete. In this case an [[org.opalj.collection.IncompleteCollection]]
     *          will be returned.
     */
    def allSuperclassTypesInInitializationOrder(
        objectType: ObjectType
    ): QualifiedCollection[List[ObjectType]] = {
        val oid = objectType.id

        if (oid == ObjectType.ObjectId)
            return CompleteCollection(List());

        if (isUnknown(oid))
            return IncompleteCollection(List());

        var allTypes: List[ObjectType] = List.empty

        val superclassTypeMap = this.superclassTypeMap
        var superclassType = superclassTypeMap(oid)
        while (superclassType ne null) {
            allTypes ::= superclassType
            superclassType = superclassTypeMap(superclassType.id)
        }
        if (allTypes.head eq ObjectType.Object)
            CompleteCollection(allTypes)
        else
            IncompleteCollection(allTypes)
    }

    def directSupertypes(objectType: ObjectType): UIDSet[ObjectType] = {
        val oid = objectType.id
        if (oid == ObjectType.ObjectId || isUnknown(oid)) {
            UIDSet.empty
        } else {
            val superinterfaceTypes: UIDSet[ObjectType] = {
                val superinterfaceTypes = superinterfaceTypesMap(oid)
                if (superinterfaceTypes ne null)
                    superinterfaceTypes
                else
                    UIDSet.empty
            }
            val superclassType = superclassTypeMap(oid)
            if (superclassType ne null)
                superinterfaceTypes + superclassType
            else
                superinterfaceTypes
        }
    }

    /**
     * The set of all supertypes of the given type.
     *
     * @note  Whenever possible, one of the higher-order functions should be used to avoid the
     *        creation of intermediate data-structures.
     *
     * @param reflexive If `true`, the returned set will also contain the given type.
     */
    def allSupertypes(objectType: ObjectType, reflexive: Boolean = false): UIDSet[ObjectType] = {
        val oid = objectType.id
        if (isUnknown(oid))
            return UIDSet.empty;

        var supertypeInformation = supertypeInformationMap(oid)
        if (supertypeInformation == null) {
            // The following is thread-safe, because we will always compute the same
            // information!
            // This happens ONLY in case of broken projects where
            // the sub-supertype information is totally broken;
            // e.g., a sub type `extends C` but C is an interface.
            supertypeInformation = interpolateSupertypeInformation(objectType)
            supertypeInformationMap(oid) = supertypeInformation
        }
        val ts = supertypeInformation.allTypes
        if (reflexive)
            ts + objectType
        else
            ts
    }

    /**
     * Returns the set of all interfaces directly or indirectly implemented by the given type.
     *
     * @param reflexive If `true` the returned set will also contain the given type if
     *      it is an interface type.
     * @throws NullPointerException if the project is very broken (e.g., if a class states
     *         that it inherits from class C, but class C is actually an interface).
     */
    def allSuperinterfacetypes(
        objectType: ObjectType,
        reflexive:  Boolean    = false
    ): UIDSet[ObjectType] = {
        val oid = objectType.id
        if (isUnknown(oid))
            return UIDSet.empty

        var supertypeInformation = supertypeInformationMap(oid)
        if (supertypeInformation == null) {
            // The following is thread-safe, because we will always compute the same information!
            // This happens ONLY in case of broken projects where the sub-supertype information is
            // totally broken; e.g., a sub type `extends C` but C is an interface.
            supertypeInformation = interpolateSupertypeInformation(objectType)
            supertypeInformationMap(oid) = supertypeInformation
        }
        val superinterfacetypes = supertypeInformation.interfaceTypes
        if (reflexive && isInterfaceTypeMap(oid))
            superinterfacetypes + objectType
        else
            superinterfacetypes
    }

    private[this] def interpolateSupertypeInformation(o: ObjectType): SupertypeInformation = {
        val allClassTypes = UIDSet.empty[ObjectType] ++ allSuperclassTypesInInitializationOrder(o).s

        var allInterfaceTypes = UIDSet.empty[ObjectType]
        foreachSuperinterfaceType(o) { supertype => allInterfaceTypes += supertype; true }

        SupertypeInformation.forSubtypesOfObject(
            isKnownTypeMap,
            isInterfaceTypeMap,
            allClassTypes,
            allInterfaceTypes,
            UIDSet.empty
        )
    }

    /**
     * Calls the function `f` for each supertype of the given object type for
     * which the classfile is available.
     *
     * It is possible that the class file of the same super interface type `I`
     * is passed multiple times to `f` when `I` is implemented multiple times
     * by the given type's supertypes.
     *
     * The algorithm first iterates over the type's super classes
     * before it iterates over the super interfaces.
     *
     * @note See [[foreachSupertype]] for details.
     */
    def foreachSuperclass(
        objectType: ObjectType,
        project:    ClassFileRepository
    )(
        f: ClassFile => Unit
    ): Unit = {
        foreachSupertype(objectType) { supertype => project.classFile(supertype).foreach(f) }
    }

    /**
     * Returns the set of all classes/interfaces from which the given type inherits
     * and for which the respective class file is available.
     *
     * @return  An `Iterable` over all class files of all super types of the given
     *          `objectType` that pass the given filter and for which the class file
     *          is available.
     * @note    It may be more efficient to use `foreachSuperclass(ObjectType,
     *          ObjectType => Option[ClassFile])(ClassFile => Unit)`
     */
    def superclasses(
        objectType: ObjectType,
        project:    ClassFileRepository
    )(
        classFileFilter: ClassFile => Boolean = { _ => true }
    ): Iterable[ClassFile] = {
        // We want to make sure that every class file is returned only once,
        // but we want to avoid equals calls on `ClassFile` objects.
        var classFiles = Map[ObjectType, ClassFile]()
        foreachSuperclass(objectType, project) { classFile =>
            if (classFileFilter(classFile))
                classFiles = classFiles.updated(classFile.thisType, classFile)
        }
        classFiles.values
    }

    /**
     * Efficient, best-effort iterator over all super types of the given type.
     */
    def allSuperclassesIterator(
        ot:        ObjectType,
        reflexive: Boolean    = false
    )(
        implicit
        project: ClassFileRepository
    ): Iterator[ClassFile] = {
        val oid = ot.id

        val baseTypes = if (isKnown(oid)) {
            supertypeInformationMap(oid).iterator
        } else {
            Iterator.empty
        }

        val allTypes = if (reflexive) baseTypes ++ Iterator(ot) else baseTypes

        allTypes
            .filter(t => project.classFile(t).isDefined)
            .map(t => project.classFile(t).get)
    }

    /**
     * Returns `Some(<SUPERTYPES>)` if this type is known and information about the
     * supertypes is available. I.e., if this type is not known, `None` is returned;
     * if the given type's superinterfaces are known (even if this class does not
     * implement (directly or indirectly) any interface) `Some(UIDSet(<OBJECTTYPES>))` is
     * returned.
     */
    // TODO Rename => directSuperinterfacetypes
    def superinterfaceTypes(objectType: ObjectType): Option[UIDSet[ObjectType]] = {
        val oid = objectType.id
        if (isUnknown(oid))
            return None;

        val superinterfaceTypes = superinterfaceTypesMap(oid)
        if (superinterfaceTypes ne null)
            Some(superinterfaceTypes)
        else
            None
    }

    /**
     * Returns the immediate superclass of the given object type, if the given
     * type is known and if it has a superclass. I.e., in case of `java.lang.Object` None is
     * returned.
     */
    def superclassType(objectType: ObjectType): Option[ObjectType] = {
        val oid = objectType.id
        if (isUnknown(oid))
            return None;

        val superclassType = superclassTypeMap(oid)
        if (superclassType ne null)
            Some(superclassType)
        else
            None
    }

    /**
     * Returns the supertype of the object type identified by the given object type id or `null`
     * if the type is unknown or if the type has no supertype.
     */
    def superclassType(objectTypeId: Int): ObjectType = {
        if (isKnown(objectTypeId))
            superclassTypeMap(objectTypeId) // may also be null
        else
            null
    }

    // TODO Rename => directSupertypes
    def supertypes(objectType: ObjectType): UIDSet[ObjectType] = {
        superinterfaceTypes(objectType) match {
            case None =>
                superclassType(objectType).map(UIDSet1.apply).getOrElse(UIDSet.empty)
            case Some(superinterfaceTypes) =>
                superinterfaceTypes ++ superclassType(objectType)
        }
    }

    def foreachDirectSubtypeOf[U](objectType: ObjectType)(f: ObjectType => U): Unit = {
        val oid = objectType.id
        if (isUnknown(oid))
            return ;

        this.subclassTypesMap(oid).foreach(f)
        this.subinterfaceTypesMap(oid).foreach(f)
    }

    /**
     * The direct subtypes of the given type (not reflexive).
     */
    def directSubtypesOf(objectType: ObjectType): Iterator[ObjectType] = {
        val oid = objectType.id
        if (isUnknown(oid))
            return Iterator.empty;

        this.subclassTypesMap(oid).iterator ++ this.subinterfaceTypesMap(oid).iterator
    }

    def directSubclassesOf(objectType: ObjectType): UIDSet[ObjectType] = {
        val oid = objectType.id
        if (isUnknown(oid))
            return UIDSet.empty;

        this.subclassTypesMap(oid)
    }

    def directSubinterfacesOf(objectType: ObjectType): UIDSet[ObjectType] = {
        val oid = objectType.id
        if (isUnknown(oid))
            return UIDSet.empty;

        this.subinterfaceTypesMap(oid)
    }

    def directSuperinterfacesOf(objectType: ObjectType): UIDSet[ObjectType] = {
        val oid = objectType.id
        if (isUnknown(oid))
            return UIDSet.empty;

        this.superinterfaceTypesMap(oid)
    }

    /**
     * Iterates over all subinterfaces of the given interface type (or java.lang.Object) until
     * the callback function returns "false".
     */
    def foreachSubinterfaceType(interfaceType: ObjectType)(f: ObjectType => Boolean): Unit = {
        var processedTypes = UIDSet.empty[ObjectType]
        var typesToProcess = directSubinterfacesOf(interfaceType)
        while (typesToProcess.nonEmpty) {
            val subInterfaceType = typesToProcess.head
            typesToProcess = typesToProcess.tail
            processedTypes += subInterfaceType
            if (f(subInterfaceType)) {
                directSubinterfacesOf(subInterfaceType) foreach { i =>
                    if (!processedTypes.contains(i))
                        typesToProcess += i
                }
            }
        }
    }

    /**
     * Iterates over all direct and indirect (also by means of super classes) superinterfaces
     * of the type  until the callback function returns "false".
     */
    def foreachSuperinterfaceType(t: ObjectType)(f: ObjectType => Boolean): Unit = {
        if (isUnknown(t))
            return ;

        var processedTypes = UIDSet.empty[ObjectType]
        var typesToProcess = directSuperinterfacesOf(t) ++ superclassType(t)
        while (typesToProcess.nonEmpty) {
            val superType = typesToProcess.head
            typesToProcess = typesToProcess.tail
            processedTypes += superType
            if (!isInterface(superType.id) || f(superType)) {
                val superInterfaces: Iterator[ObjectType] =
                    if (directSuperinterfacesOf(superType) eq null) {
                        Iterator.empty
                    } else {
                        directSuperinterfacesOf(superType).iterator
                    }
                (superInterfaces ++ superclassType(superType).iterator) foreach { i =>
                    if (!processedTypes.contains(i))
                        typesToProcess += i
                }
            }
        }
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    //
    // "HIGHER-LEVEL" QUERIES RELATED TO THE JVM/JAVA SPECIFICATION
    //
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    /**
     * Determines if `subtype` is a subtype of `supertype` using this class hierarchy.
     *
     * This method can be used as a foundation for implementing the logic of the JVM's
     * `instanceof` and `classcast` instructions. But, in both cases additional logic
     * for handling `null` values and for considering the runtime type needs to be
     * implemented by the caller of this method.
     *
     * @note    The answer `false` does not necessarily imply that two '''runtime values''' for
     *          which the given types are only upper bounds are not (w.r.t. '''their
     *          runtime types''') in a subtype relation. E.g., if `subtype` denotes the type
     *          `java.util.List` and `supertype` denotes the type `java.util.ArrayList` then
     *          the answer is clearly `false`. But, at runtime, this may not be the case. I.e.,
     *          only the answer `true` is conclusive. In case of `false` further information
     *          needs to be taken into account by the caller to determine what it means that
     *          the (upper) type (bounds) of the underlying values are not in an inheritance
     *          relation.
     * @param   subtype Any class, interface  or array type.
     * @param   supertype Any class, interface or array type.
     * @return  `true` if `subtype` is indeed a subtype of the given `supertype`. `false` is
     *          returned if the typing relation is unknown OR if `subtype` is definitively not
     *          a subtype of `supertype`.
     */
    @tailrec final def isSubtypeOf(subtype: ReferenceType, supertype: ReferenceType): Boolean = {
        // necessary to get reasonable answers in case of incomplete type hierarchies
        if (subtype eq supertype)
            return true;

        if (subtype.isObjectType) {
            if (supertype.isObjectType)
                isSubtypeOf(subtype.asObjectType, supertype.asObjectType)
            else
                // the supertype is an array type..
                false
        } else {
            // the subtype is an array type
            if (supertype.isObjectType) {
                (supertype eq ObjectType.Object) ||
                    (supertype eq ObjectType.Serializable) || (supertype eq ObjectType.Cloneable)
            } else {
                // ... and the supertype is also an array type
                // The case:
                //    `componentType eq superComponentType`
                // is already handled by the very first test `subtype eq supertype` because
                // ArrayTypes are internalized.
                val componentType = subtype.asArrayType.componentType
                val superComponentType = supertype.asArrayType.componentType
                if (superComponentType.isBaseType || componentType.isBaseType)
                    false
                else
                    isSubtypeOf(componentType.asReferenceType, superComponentType.asReferenceType)
            }
        }
    }

    /**
     * Returns `true` if subtype is a subtype of `supertype`; `false` is returned if
     * the subtyping relationship is unknown OR `subtype` is not a subtype of `supertype`.
     * `isSubtypeOf` is reflexive.
     *
     * See `isASubtypeOf` if more precise information about the subtyping relationship is required.
     */
    def isSubtypeOf(subtype: ObjectType, theSupertype: ObjectType): Boolean = {
        if (subtype eq theSupertype)
            return true;

        val theSupertypeId = theSupertype.id
        if (isUnknown(theSupertypeId))
            return false;

        subtypeInformationMap(theSupertypeId).contains(subtype)
    }

    /**
     * Determines if the type described by the first set of upper type bounds is
     * a subtype of the second type. I.e., it checks, if for all types of the
     * `subtypes` upper type bound, a type in the `supertypes` type exists that is a
     * supertype of the respective subtype. If `subtypes` is empty, `true` will be
     * returned; an empty upper type bound is expected to model `null`.
     */
    def isSubtypeOf(
        subtypes:   UIDSet[_ <: ReferenceType],
        supertypes: UIDSet[_ <: ReferenceType]
    ): Boolean = {
        if (subtypes.isEmpty /*the upper type bound of "null" values*/ || subtypes == supertypes)
            return true;

        supertypes forall { supertype: ReferenceType =>
            subtypes exists { subtype: ReferenceType =>
                this.isSubtypeOf(subtype, supertype)
            }
        }
    }

    /**
     * Returns `true` if the subtype is a subtype of '''all''' given supertypes. Hence,
     * supertypes should not contain more than one class type.
     */
    def isSubtypeOf(subtype: ReferenceType, supertypes: UIDSet[_ <: ReferenceType]): Boolean = {
        if (supertypes.isEmpty /*the upper type bound of "null" values*/ )
            return false;

        supertypes forall { supertype: ReferenceType => isSubtypeOf(subtype, supertype) }
    }

    def isSubtypeOf(subtypes: UIDSet[_ <: ReferenceType], supertype: ReferenceType): Boolean = {
        if (subtypes.isEmpty) /*the upper type bound of "null" values*/
            return true;

        subtypes exists { subtype: ReferenceType => this.isSubtypeOf(subtype, supertype) }
    }

    /**
     * Determines if a value of type `elementValueType` can be stored in an array of
     * type `arrayType`. E.g., a value of type `IntegerType` can be stored in an
     * array (one-dimensional) of type `ArrayType(IntegerType)`. This method takes
     * the fact that a type may just model an upper type bound into account.
     *
     * @param   elementValueType The type of the value that should be stored in the
     *          array. This type is compared against the component type of the array.
     * @param   elementValueTypeIsPrecise Specifies if the type information is precise;
     *          i.e., whether `elementValueType` models the precise runtime type (`true`)
     *          or just an upper bound (`false`). If the `elementValueType` is a base/
     *          primitive type, then this value should be `true`; but actually it is
     *          ignored.
     * @param   arrayType The type of the array.
     * @param   arrayTypeIsPrecise Specifies if the type information is precise;
     *          i.e., whether arrayType models the precise runtime type (`true`)
     *          or just an upper bound (`false`).
     */
    @tailrec final def canBeStoredIn(
        elementValueType:          FieldType,
        elementValueTypeIsPrecise: Boolean,
        arrayType:                 ArrayType,
        arrayTypeIsPrecise:        Boolean
    ): Answer = {
        if (elementValueType.isBaseType) {
            Answer(elementValueType eq arrayType.componentType)
        } else if (elementValueType.isArrayType) {
            if (arrayType.componentType.isArrayType) {
                canBeStoredIn(
                    elementValueType.asArrayType.componentType,
                    elementValueTypeIsPrecise,
                    arrayType.componentType.asArrayType,
                    arrayTypeIsPrecise
                )
            } else if (arrayType.componentType.isBaseType) {
                No
            } else /*arrayType.componentType.isObjectType*/ {
                val componentObjectType = arrayType.componentType.asObjectType
                // Recall that `isSubtypeOf` completely handles all cases that make
                // it possible to store an array in a value of type ObjectType.
                isASubtypeOf(elementValueType.asArrayType, componentObjectType) match {
                    case Yes => if (arrayTypeIsPrecise) Yes else Unknown
                    case No  => No
                    case _   => throw new AssertionError("some array type <: some object type failed")
                }
            }
        } else /* the type of the element value is an ObjectType*/ {
            if (arrayType.elementType.isBaseType) {
                No
            } else {
                val elementValueObjectType = elementValueType.asObjectType
                val arrayComponentReferenceType = arrayType.componentType.asReferenceType
                isASubtypeOf(elementValueObjectType, arrayComponentReferenceType) match {
                    case Yes =>
                        if (arrayTypeIsPrecise || isKnownToBeFinal(elementValueObjectType))
                            Yes
                        else
                            Unknown
                    case No =>
                        if (elementValueTypeIsPrecise && arrayTypeIsPrecise)
                            No
                        else
                            Unknown
                    case unknown => unknown
                }
            }
        }
    }

    /**
     * Determines if the given class or interface type `subtype` is actually a subtype
     * of the class or interface type `supertype`.
     *
     * This method can be used as a foundation for implementing the logic of the JVM's
     * `instanceof` and `checkcast` instructions. But, in that case additional logic
     * for handling `null` values and for considering the runtime type needs to be
     * implemented by the caller of this method.
     *
     * @note    No explicit `isKnown` check is required.
     * @param   subtype Any `ObjectType`.
     * @param   theSupertype Any `ObjectType`.
     * @return  `Yes` if `subtype` is a subtype of the given `supertype`. `No`
     *          if `subtype` is not a subtype of `supertype` and `Unknown` if the analysis is
     *          not conclusive. The latter can happen if the class hierarchy is not
     *          complete and hence precise information about a type's supertypes
     *          is not available.
     *
     */
    def isASubtypeOf(subtype: ObjectType, theSupertype: ObjectType): Answer = {
        if (subtype eq theSupertype)
            return Yes;

        val Object = ObjectType.Object
        if (theSupertype eq Object)
            return Yes;

        if (subtype eq Object /* && theSupertype != ObjectType.Object is already handled */ )
            return No;

        val subtypeId = subtype.id
        val theSupertypeId = theSupertype.id

        if (isKnownToBeFinal(theSupertypeId))
            return No;

        if (isUnknown(subtypeId))
            return Unknown;

        if (isUnknown(theSupertypeId)) {
            return if (isSupertypeInformationCompleteMap(subtypeId)) No else Unknown;
        }

        val isInterfaceTypeMap = this.isInterfaceTypeMap
        val subtypeIsInterface = isInterfaceTypeMap(subtypeId)
        val supertypeIsInterface = isInterfaceTypeMap(theSupertypeId)

        if (subtypeIsInterface && !supertypeIsInterface)
            // An interface always (only) directly inherits from java.lang.Object
            // and this is already checked before.
            return No;

        val supertypeInformationMap = this.supertypeInformationMap
        if (supertypeInformationMap(subtypeId).containsId(theSupertypeId))
            Yes
        else if (isSupertypeInformationCompleteMap(subtypeId))
            No
        else if (supertypeInformationMap(theSupertypeId).containsId(subtypeId))
            No
        else
            Unknown
    }

    /**
     * Determines if `subtype` is a subtype of `supertype` using this class hierarchy.
     *
     * This method can be used as a foundation for implementing the logic of the JVM's
     * `instanceof` and `classcast` instructions. But, in both cases additional logic
     * for handling `null` values and for considering the runtime type needs to be
     * implemented by the caller of this method.
     *
     * @note    The answer `No` does not necessarily imply that two '''runtime values''' for
     *          which the given types are only upper bounds are not (w.r.t. '''their
     *          runtime types''') in a subtype relation. E.g., if `subtype` denotes the type
     *          `java.util.List` and `supertype` denotes the type `java.util.ArrayList` then
     *          the answer is clearly `No`. But, at runtime, this may not be the case. I.e.,
     *          only the answer `Yes` is conclusive. In case of `No` further information
     *          needs to be taken into account by the caller to determine what it means that
     *          the (upper) type (bounds) of the underlying values are not in an inheritance
     *          relation.
     * @param   subtype Any class, interface  or array type.
     * @param   supertype Any class, interface or array type.
     * @return  `Yes` if `subtype` is indeed a subtype of the given `supertype`. `No`
     *          if `subtype` is not a subtype of `supertype` and `Unknown` if the analysis is
     *          not conclusive. The latter can happen if the class hierarchy is not
     *          completely available and hence precise information about a type's supertypes
     *          is not available.
     */
    @tailrec final def isASubtypeOf(subtype: ReferenceType, supertype: ReferenceType): Answer = {

        // The following two tests are particularly relevant in case of incomplete
        // class hierarchies since they allow to give definitive answers in some
        // cases of missing type information.
        if ((subtype eq supertype) || (supertype eq Object))
            return Yes;

        if (subtype eq Object)
            return No; // the given supertype has to be a subtype...

        if (subtype.isObjectType) {
            if (supertype.isArrayType)
                No
            else
                // The analysis is conclusive iff we can get all supertypes
                // for the given type (ot) up until "java/lang/Object"; i.e.,
                // if there are no holes.
                isASubtypeOf(subtype.asObjectType, supertype.asObjectType)
        } else {
            // ... subtype is an ArrayType
            if (supertype.isObjectType) {
                Answer(
                    (supertype eq ObjectType.Serializable) || (supertype eq ObjectType.Cloneable)
                )
            } else {
                val componentType = subtype.asArrayType.componentType
                val superComponentType = supertype.asArrayType.componentType
                if (superComponentType.isBaseType || componentType.isBaseType)
                    // Recall that the case:
                    //
                    //    `componentType eq superComponentType`
                    //
                    // is already handled by the very first test `subtype eq supertype` because
                    // ArrayTypes are internalized.
                    No
                else
                    isASubtypeOf(componentType.asReferenceType, superComponentType.asReferenceType)
            }
        }
    }

    /**
     * Determines if the type described by the first set of upper type bounds is
     * a subtype of the second type. I.e., it checks, if for all types of the
     * `subtypes` upper type bound, a type in the `supertypes` type exists that is a
     * supertype of the respective subtype. If `subtypes` is empty, `Yes` will be
     * returned; an empty upper type bound is expected to model `null`.
     */
    def isASubtypeOf(
        subtypes:   UIDSet[_ <: ReferenceType],
        supertypes: UIDSet[_ <: ReferenceType]
    ): Answer = {
        if (subtypes.isEmpty /* <=> upper type bound of "null" values */ || subtypes == supertypes)
            return Yes;

        Answer(
            supertypes forall { supertype: ReferenceType =>
                var subtypingRelationUnknown = false
                val subtypeExists =
                    subtypes exists { subtype: ReferenceType =>
                        val isSubtypeOf = this.isASubtypeOf(subtype, supertype)
                        isSubtypeOf match {
                            case Yes     => true
                            case Unknown => { subtypingRelationUnknown = true; false /* continue */ }
                            case No      => false
                        }
                    }
                if (subtypeExists)
                    true
                else if (subtypingRelationUnknown)
                    return Unknown;
                else
                    false
            }
        )
    }

    /**
     * Returns `Yes` if the subtype is a subtype of '''all''' given supertypes. Hence,
     * supertypes should not contain more than one class type.
     */
    def isASubtypeOf(subtype: ReferenceType, supertypes: UIDSet[_ <: ReferenceType]): Answer = {
        if (supertypes.isEmpty /* <=> upper type bound of "null" values */ )
            return No;

        supertypes foreach { supertype: ReferenceType =>
            isASubtypeOf(subtype, supertype) match {
                case Yes     => /*Nothing to do*/
                case Unknown => return Unknown; // FIXME No should have precedence over Unknown even if some supertypes are Unknown...
                case No      => return No;
            }
        }
        // subtype is a subtype of all supertypes
        Yes
    }

    def isASubtypeOf(subtypes: UIDSet[_ <: ReferenceType], supertype: ReferenceType): Answer = {
        if (subtypes.isEmpty) /* <=> upper type bound of "null" values */
            return Yes;

        var subtypeRelationUnknown = false
        val subtypeExists =
            subtypes exists { subtype: ReferenceType =>
                this.isASubtypeOf(subtype, supertype) match {
                    case Yes     => true
                    case Unknown => { subtypeRelationUnknown = true; false /* continue search */ }
                    case No      => false
                }
            }
        if (subtypeExists)
            Yes
        else if (subtypeRelationUnknown)
            Unknown
        else
            No

    }

    /**
     * Computes the set of types which are subtypes (reflexive) of all types identified by the
     * given `upper type bound`. E.g., the class X which implements I and J,
     * would be a direct subtype of the upper type bound consisting of I and J.
     * If the bound consists of only one type, then the bound is returned.
     *
     * @param   upperTypeBound A set of types that are in no inheritance relationship.
     *          `upperTypeBound` must not be empty.
     */
    def directSubtypesOf(upperTypeBound: UIDSet[ObjectType]): UIDSet[ObjectType] = {
        if (upperTypeBound.isSingletonSet)
            return upperTypeBound;

        val firstType = upperTypeBound.head
        val remainingTypeBounds = upperTypeBound.tail

        // Basic Idea: Let's do a breadth-first search and for every candidate type
        // we check whether the type is a subtype of all types in the bound.
        // If so, the type is added to the result set and the search terminates
        // for this particular type.

        // The analysis is complicated by the fact that an interface may be
        // implemented multiple times, e.g.,:
        // interface I
        // interface J extends I
        // class X implements I,J
        // class Y implements J

        var directSubtypes = UIDSet.empty[ObjectType]
        var processedTypes = UIDSet.empty[ObjectType]
        val typesToProcess = new mutable.Queue ++= directSubtypesOf(firstType)
        while (typesToProcess.nonEmpty) {
            val candidateType = typesToProcess.dequeue()
            processedTypes += candidateType
            val isCommonSubtype =
                remainingTypeBounds.forall { otherTypeBound: ObjectType =>
                    isASubtypeOf(candidateType, otherTypeBound).isYesOrUnknown
                }
            if (isCommonSubtype) {
                directSubtypes =
                    directSubtypes.filter { candidateDirectSubtype =>
                        isASubtypeOf(candidateDirectSubtype, candidateType).isNoOrUnknown
                    } +
                        candidateType
            } else {
                directSubtypesOf(candidateType).foreach { candidateType =>
                    if (!processedTypes.contains(candidateType))
                        typesToProcess += candidateType
                }
            }
        }

        directSubtypes
    }

    //
    //
    // SUBTYPE RELATION W.R.T. GENERIC TYPES
    //
    //

    /*
     * This is a helper method only. TypeArguments are just a part of a generic
     * `ClassTypeSignature`. Hence, it makes no
     * sense to check subtype relation of incomplete information.
     *
     * @note At the comparison of two [[GenericTypeArgument]]s without [[VarianceIndicator]]s
     * we have to check two different things. First compare the [[ObjectType]]s, if they are equal
     * we still have to care about the [[TypeArgument]]s since we are dealing with generics.
     */
    private[this] def isASubtypeOfByTypeArgument(
        subtype:   TypeArgument,
        supertype: TypeArgument
    )(
        implicit
        p: ClassFileRepository
    ): Answer = {
        (subtype, supertype) match {
            case (ConcreteTypeArgument(et), ConcreteTypeArgument(superEt)) => Answer(et eq superEt)
            case (ConcreteTypeArgument(et), UpperTypeBound(superEt))       => isASubtypeOf(et, superEt)
            case (ConcreteTypeArgument(et), LowerTypeBound(superEt))       => isASubtypeOf(superEt, et)
            case (_, Wildcard)                                             => Yes
            case (GenericTypeArgument(varInd, cts), GenericTypeArgument(supVarInd, supCts)) =>
                (varInd, supVarInd) match {
                    case (None, None) =>
                        if (cts.objectType eq supCts.objectType) isASubtypeOf(cts, supCts) else No
                    case (None, Some(CovariantIndicator)) =>
                        isASubtypeOf(cts, supCts)
                    case (None, Some(ContravariantIndicator)) =>
                        isASubtypeOf(supCts, cts)
                    case (Some(CovariantIndicator), Some(CovariantIndicator)) =>
                        isASubtypeOf(cts, supCts)
                    case (Some(ContravariantIndicator), Some(ContravariantIndicator)) =>
                        isASubtypeOf(supCts, cts)
                    case _ => No
                }
            case (UpperTypeBound(et), UpperTypeBound(superEt)) => isASubtypeOf(et, superEt)
            case (LowerTypeBound(et), LowerTypeBound(superEt)) => isASubtypeOf(superEt, et)
            case _                                             => No
        }
    }

    @inline @tailrec private[this] final def compareTypeArguments(
        subtypeArgs:   List[TypeArgument],
        supertypeArgs: List[TypeArgument]
    )(
        implicit
        p: ClassFileRepository
    ): Answer = {

        (subtypeArgs, supertypeArgs) match {
            case (Nil, Nil)          => Yes
            case (Nil, _) | (_, Nil) => No
            case (arg :: tail, supArg :: supTail) =>
                // IMPROVE Consider implementing an "isSubtypeOfByTypeArgument:Boolean" method.
                val isASubtypeOf = isASubtypeOfByTypeArgument(arg, supArg)
                if (isASubtypeOf.isNoOrUnknown)
                    isASubtypeOf
                else
                    compareTypeArguments(tail, supTail)
        }
    }

    /**
     * Determines whether the given [[ClassSignature]] of `subtype` implements or extends
     * the given `supertype`.
     * In case that the `subtype` does implement or extend the `supertype`, an `Option` of
     * [[ClassTypeSignature]] is returned. Otherwise None will be returned.
     *
     * @example
     *  subtype: [[ClassSignature]] from class A where A extends List<String>
     *  supertype: List as [[ObjectType]]
     *
     *  This method scans all super classes and super interfaces of A in order to find
     *  the concrete class declaration of List where it is bound to String. The above example
     *  would yield the [[ClassTypeSignature]] of List<String>.
     *
     * @param subtype Any type or interface.
     * @param supertype Any type or interface.
     * @return `Option` of [[ClassTypeSignature]] if the `subtype` extends or implements
     *          the given `supertype`, `None` otherwise.
     */
    def getSupertypeDeclaration(
        subtype:   ClassSignature,
        supertype: ObjectType
    )(
        implicit
        project: ClassFileRepository
    ): Option[ClassTypeSignature] = {

        val signaturesToCheck = subtype.superClassSignature :: subtype.superInterfacesSignature
        for {
            cts <- signaturesToCheck if cts.objectType eq supertype
        } { return Some(cts) }

        for {
            cts <- signaturesToCheck
            superCs <- getClassSignature(cts.objectType)
            matchingType <- getSupertypeDeclaration(superCs, supertype)
        } { return Some(matchingType) }

        None
    }

    /**
     * Returns the object type's class signature if the class files is available and
     * a class signature is defined.
     */
    @inline private[this] final def getClassSignature(
        ot: ObjectType
    )(
        implicit
        p: ClassFileRepository
    ): Option[ClassSignature] = {
        p.classFile(ot).flatMap(cf => cf.classSignature)
    }

    /**
     * Determines if the given class or interface type encoded by the
     * [[ClassTypeSignature]] `subtype` is actually a subtype
     * of the class or interface type encoded in the [[ClassTypeSignature]] of the
     * `supertype`.
     *
     * @note This method relies – in case of a comparison of non generic types – on
     *       `isSubtypeOf(org.opalj.br.ObjectType,org.opalj.br.ObjectType)` of `Project` which
     *        performs an upwards search only. E.g., given the following
     *      type hierarchy:
     *      `class D inherits from C`
     *      `class E inherits from D`
     *      and the query isSubtypeOf(D,E) the answer will be `Unknown` if `C` is
     *      `Unknown` and `No` otherwise.
     * @param subtype Any `ClassTypeSignature`.
     * @param supertype Any `ClassTypeSignature`.
     * @return `Yes` if `subtype` is a subtype of the given `supertype`. `No`
     *      if `subtype` is not a subtype of `supertype` and `Unknown` if the analysis is
     *      not conclusive. The latter can happen if the class hierarchy is not
     *      complete and hence precise information about a type's supertypes
     *      is not available.
     * @example =========  Introduction ==========
     *
     *  Before looking in some examples, we have to set up the terminology.
     *
     *  Type definition: List<String, ? extends Number, ?>
     *
     *  ContainerType - A ContainerType is a type with parameters. In the previous type definition
     *                  is `List` the ContainerType.
     *  TypeArgument - A [[TypeArgument]] is one of the parameters of the ContainerType. The above type
     *                  definition has three [[TypeArgument]]s. (String, ? extends Number and ?)
     *  VarianceIndicator - A [[VarianceIndicator]] is defined in the context of [[TypeArgument]]s. There
     *                      is a [[CovariantIndicator]] which can be defined in the type definition by using the
     *                      `extends` keyword. (? extends Number is a covariant [[TypeArgument]]). The other
     *                      one is the [[ContravariantIndicator]] which is defined using the `super` keyword.
     * @example ========= 1 ==========
     *
     *                instance // definition
     *      subtype: List<String> // List<E>
     *      supertype: List<String> // List<E>
     *
     *      If the ContainerType of the `subtype` is equal to the ContainerType of the `supertype` and non of the
     *      [[TypeArgument]]s has a [[VarianceIndicator]], then exists a subtype relation if and only if all of the
     *      [[TypeArgument]]s are equal.
     * @example ========= 2 =========
     *
     * subtype:     SomeClass // SomeClass extends SomeInterface<String>
     * supertype:   SomeInterface<String> // SomeInterface<E>
     *
     * Is the `subtype` a [[ConcreteType]] without [[org.opalj.br.FormalTypeParameter]]s and the `supertype` is a [[GenericType]] then
     * we first have to check whether the `subtype` is a subtype of the given `supertype`. If not, then the `subtype` is not an actual
     * subtype of the given `supertype`. Otherwise we have to find the definition of the `supertype` in the type definition
     * or the type definition of a super class or a super interface (interface definition of SomeInterface<String>).
     * Once found the `supertype`, we can compare all [[TypeArgument]]s of the supertype definition of the `subtype`
     * and the given `supertype`. (We are comparing String and String in this example)
     * If all of them are equal, `subtype` is an actual subtype of the `supertype`.
     * @example ========= 3 =========
     *
     * subtype:     Foo<Integer, String> // Foo<T,E> extends Bar<E>
     * supertype:   Bar<String> // Bar<E>
     *
     * Does the `subtype` and `supertype` have [[FormalTypeParameter]]s and the ContainerType of the `subtype`
     * is a subtype of the ContainerType of the `supertype`, we have to compare the shared [[TypeArgument]]s. In
     * our example the subtype Foo has two [[FormalTypeParameter]] (T,E) and the supertype Bar has only one
     * [[FormalTypeParameter]] (E). Since both of them specify E in the [[ClassSignature]] of Foo, they share E as
     * [[FormalTypeParameter]]. So it is necessary to check whether the actual bound [[TypeArgument]] at the
     * position of E is equal. At first we have to locate the shared parameter in the [[ClassSignature]], so it is possible
     * to find the correct [[TypeArgument]]s. The above example shows that the shared parameter E is in the second position
     * of the [[FormalTypeParameter]]s of Foo and at the first position of the [[FormalTypeParameter]]s of Bar. Second and last
     * we know can compare the according [[TypeArgument]]s. All other parameters can be ignored because they are no important
     * to decide the subtype relation.
     */
    def isASubtypeOf(
        subtype:   ClassTypeSignature,
        supertype: ClassTypeSignature
    )(
        implicit
        project: ClassFileRepository
    ): Answer = {
        def compareTypeArgumentsOfClassSuffixes(
            suffix:      List[SimpleClassTypeSignature],
            superSuffix: List[SimpleClassTypeSignature]
        ): Answer = {
            if (suffix.isEmpty && superSuffix.isEmpty)
                return Yes;

            suffix.zip(superSuffix).foldLeft(Yes: Answer)((acc, value) =>
                (acc, compareTypeArguments(value._1.typeArguments, value._2.typeArguments)) match {
                    case (_, Unknown)     => return Unknown;
                    case (x, y) if x ne y => No
                    case (x, _ /*x*/ )    => x
                })
        }
        if (subtype.objectType eq supertype.objectType) {
            (subtype, supertype) match {
                case (ConcreteType(_), ConcreteType(_)) =>
                    Yes

                case (GenericType(_, _), ConcreteType(_)) =>
                    isASubtypeOf(subtype.objectType, supertype.objectType)

                case (GenericType(_, elements), GenericType(_, superElements)) =>
                    compareTypeArguments(elements, superElements)

                case (GenericTypeWithClassSuffix(_, elements, suffix), GenericTypeWithClassSuffix(_, superElements, superSuffix)) => {
                    compareTypeArguments(elements, superElements) match {
                        case Yes    => compareTypeArgumentsOfClassSuffixes(suffix, superSuffix)
                        case answer => answer
                    }
                }

                case _ => No
            }
        } else {
            val isASubtype = isASubtypeOf(subtype.objectType, supertype.objectType)
            if (isASubtype.isYes) {

                def haveSameTypeBinding(
                    subtype:            ObjectType,
                    supertype:          ObjectType,
                    supertypeArguments: List[TypeArgument],
                    isInnerClass:       Boolean            = false
                ): Answer = {
                    getClassSignature(subtype).map { cs =>
                        getSupertypeDeclaration(cs, supertype).map { matchingType =>
                            val classSuffix = matchingType.classTypeSignatureSuffix
                            if (isInnerClass && classSuffix.nonEmpty)
                                compareTypeArguments(classSuffix.last.typeArguments, supertypeArguments)
                            else
                                compareTypeArguments(matchingType.simpleClassTypeSignature.typeArguments, supertypeArguments)
                        } getOrElse No
                    } getOrElse Unknown
                }
                def compareSharedTypeArguments(
                    subtype:            ObjectType,
                    typeArguments:      List[TypeArgument],
                    supertype:          ObjectType,
                    supertypeArguments: List[TypeArgument]
                ): Answer = {

                    val cs = getClassSignature(subtype)
                    val superCs = getClassSignature(supertype)
                    if (cs.isEmpty || superCs.isEmpty)
                        return Unknown;

                    val ftp = cs.get.formalTypeParameters
                    val superFtp = superCs.get.formalTypeParameters
                    var typeArgs = List.empty[TypeArgument]
                    var supertypeArgs = List.empty[TypeArgument]

                    var i = 0
                    while (i < ftp.size) {
                        val index = superFtp.indexOf(ftp(i))
                        if (index >= 0) {
                            typeArgs = typeArguments(i) :: typeArgs
                            supertypeArgs = supertypeArguments(index) :: supertypeArgs
                        }
                        i = i + 1
                    }

                    if (typeArgs.isEmpty) {
                        if (cs.get.superClassSignature.classTypeSignatureSuffix.nonEmpty)
                            Yes
                        else
                            haveSameTypeBinding(subtype, supertype, supertypeArguments)
                    } else {
                        compareTypeArguments(typeArgs, supertypeArgs)
                    }

                }
                (subtype, supertype) match {
                    case (ConcreteType(_), ConcreteType(_))   => Yes
                    case (GenericType(_, _), ConcreteType(_)) => Yes

                    case (ConcreteType(_), GenericType(_, supertypeArguments)) =>
                        haveSameTypeBinding(subtype.objectType, supertype.objectType, supertypeArguments)

                    case (GenericType(containerType, elements), GenericType(superContainerType, superElements)) =>
                        compareSharedTypeArguments(containerType, elements, superContainerType, superElements)

                    case (GenericTypeWithClassSuffix(_, _, _), ConcreteType(_)) => Yes

                    case (GenericTypeWithClassSuffix(containerType, elements, _), GenericType(superContainerType, superElements)) =>
                        compareSharedTypeArguments(containerType, elements, superContainerType, superElements)

                    case (GenericTypeWithClassSuffix(containerType, _ /*typeArguments*/ , suffix), GenericTypeWithClassSuffix(superContainerType, _ /*supertypeArguments*/ , superSuffix)) => {
                        compareSharedTypeArguments(containerType, subtype.classTypeSignatureSuffix.last.typeArguments,
                            superContainerType, supertype.classTypeSignatureSuffix.last.typeArguments) match {
                            case Yes => compareTypeArgumentsOfClassSuffixes(suffix.dropRight(1), superSuffix.dropRight(1)) match {
                                case Yes if suffix.last.typeArguments.isEmpty && superSuffix.last.typeArguments.isEmpty => Yes
                                case Yes if suffix.last.typeArguments.isEmpty && superSuffix.last.typeArguments.nonEmpty => {
                                    val ss = getClassSignature(containerType).flatMap { cs => getSupertypeDeclaration(cs, superContainerType) }
                                    if (ss.get.classTypeSignatureSuffix.last.typeArguments.collectFirst { case x @ ProperTypeArgument(_, TypeVariableSignature(_)) => x }.size > 0)
                                        compareTypeArgumentsOfClassSuffixes(List(subtype.simpleClassTypeSignature), List(superSuffix.last))
                                    else compareTypeArgumentsOfClassSuffixes(List(ss.get.classTypeSignatureSuffix.last), List(superSuffix.last))
                                }
                                case Yes    => compareTypeArgumentsOfClassSuffixes(List(suffix.last), List(superSuffix.last))
                                case answer => answer
                            }
                            case answer => answer
                        }
                    }
                    case _ => No
                }
            } else isASubtype
        }
    }

    /**
     * Determines if the given class or interface type encoded in a [[ClassTypeSignature]]
     * `subtype` is actually a subtype of the class, interface or intersection type encoded
     * in the [[FormalTypeParameter]] of the `supertype` parameter. The subtype relation is
     * fulfilled if the subtype is a subtype of the class bound and/or all interface types
     * that are prescribed by the formal type specification.
     *
     * @note    This method does consider generics types specified within the
     *          [[FormalTypeParameter]].
     * @param   subtype Any `ClassTypeSignature`.
     * @param   supertype Any `FormalTypeParameter`.
     * @return `Yes` if `subtype` is a subtype of the given `supertype`. `No`
     *          if `subtype` is not a subtype of `supertype` and `Unknown` if the analysis is
     *          not conclusive. The latter can happen if the class hierarchy is not
     *          complete and hence precise information about a type's supertypes
     *          is not available.
     *
     */
    def isASubtypeOf(
        subtype:   ClassTypeSignature,
        supertype: FormalTypeParameter
    )(
        implicit
        p: ClassFileRepository
    ): Answer = {

        // IMPROVE Avoid creating the list by using an inner function (def).
        (supertype.classBound.toList ++ supertype.interfaceBound)
            .collect { case s: ClassTypeSignature => s }
            .foldLeft(Yes: Answer) { (a, superCTS) =>
                (a, isASubtypeOf(subtype, superCTS)) match {
                    case (_, Unknown)     => return Unknown;
                    case (x, y) if x ne y => No
                    case (x, _ /*x*/ )    => x
                }
            }
    }

    /**
     * Returns some statistical data about the class hierarchy.
     */
    def statistics: String = {
        "Class Hierarchy Statistics:"+
            "\n\tKnown types: "+knownTypesMap.count(_ != null)+
            "\n\tInterface types: "+isInterfaceTypeMap.count(isInterface => isInterface)+
            "\n\tIdentified Superclasses: "+superclassTypeMap.count(_ != null)+
            "\n\tSuperinterfaces: "+
            superinterfaceTypesMap.filter(_ != null).foldLeft(0)(_ + _.size)+
            "\n\tSubclasses: "+
            subclassTypesMap.filter(_ != null).foldLeft(0)(_ + _.size)+
            "\n\tSubinterfaces: "+
            subinterfaceTypesMap.filter(_ != null).foldLeft(0)(_ + _.size)
    }

    /**
     * Returns a view of the class hierarchy as a graph (which can then be transformed
     * into a dot representation [[http://www.graphviz.org Graphviz]]). This
     * graph can be a multi-graph if the class hierarchy contains holes.
     */
    def toGraph(): Node = new Node {

        private val nodes: mutable.Map[ObjectType, Node] = {
            val nodes = mutable.HashMap.empty[ObjectType, Node]

            foreachNonNullValue(knownTypesMap) { (id, aType) =>
                val entry: (ObjectType, Node) = (
                    aType,
                    new Node {
                        private val directSubtypes = directSubtypesOf(aType)
                        override def nodeId: Int = aType.id
                        override def toHRR: Option[String] = Some(aType.toJava)
                        override val visualProperties: Map[String, String] = {
                            Map("shape" -> "box") ++ (
                                if (isInterface(aType).isYes)
                                    Map("fillcolor" -> "aliceblue", "style" -> "filled")
                                else
                                    Map.empty
                            )
                        }
                        def foreachSuccessor(f: Node => Unit): Unit = {
                            directSubtypes foreach { subtype => f(nodes(subtype)) }
                        }
                        def hasSuccessors: Boolean = directSubtypes.nonEmpty
                    }
                )
                nodes += entry
            }
            nodes
        }

        // a virtual root node
        override def nodeId: Int = -1
        override def toHRR: Option[String] = None
        override def foreachSuccessor(f: Node => Unit): Unit = {
            /*
             * We may not see the class files of all classes that are referred
             * to in the class files that we did see. Hence, we have to be able
             * to handle partial class hierarchies.
             */
            val rootTypes = nodes filter { case (t, _) => superclassTypeMap(t.id) eq null }
            rootTypes.values.foreach(f)
        }
        override def hasSuccessors: Boolean = nodes.nonEmpty
    }

    // -----------------------------------------------------------------------------------
    //
    // COMMON FUNCTIONALITY TO CALCULATE THE MOST SPECIFIC COMMON SUPERTYPE OF TWO
    // TYPES / TWO UPPER TYPE BOUNDS
    //
    // -----------------------------------------------------------------------------------

    /**
     * Calculates the set of all supertypes of the given `types`.
     */
    def allSupertypesOf(types: UIDSet[ObjectType], reflexive: Boolean): UIDSet[ObjectType] = {
        var allSupertypesOf: UIDSet[ObjectType] = UIDSet.empty
        types foreach { t: ObjectType =>
            if (!allSupertypesOf.contains(t)) {
                if (isKnown(t))
                    allSupertypesOf ++= allSupertypes(t, reflexive)
                else if (reflexive)
                    // the project's class hierarchy is obviously not complete
                    // however, we do as much as we can...
                    allSupertypesOf += t
            }
        }
        allSupertypesOf
    }

    /**
     * Selects all types of the given set of types that '''do not have any subtype
     * in the given set'''. If the given set is empty, a set containing `java.lang.Object`
     * is returned. A set which contains only one type will directly be returned.
     *
     * @param   types A set of types that contains '''for each type stored in the
     *          set all direct and indirect supertypes or none'''. For example, the intersection
     *          of the sets of all supertypes (as returned, e.g., by
     *          `ClassHierarchy.allSupertypes`) of two (independent) types satisfies this
     *          condition. If `types` is empty, the returned leaf type is `ObjectType.Object`.
     *          which should always be a safe fallback.
     */
    def leafTypes(types: UIDSet[ObjectType]): UIDSet[ObjectType] = {
        if (types.isEmpty)
            return ClassHierarchy.JustObject;

        if (types.isSingletonSet)
            return types;

        types filter { aType =>
            isUnknown(aType) ||
                //!(directSubtypesOf(aType) exists { t => types.contains(t) })
                !(types exists { t => (t ne aType) && isSubtypeOf(t, aType) })
        }
    }

    /**
     * Calculates the most specific common supertype of the given types.
     * If `reflexive` is `false`, no two types across both sets have to be in
     * an inheritance relation; if in doubt use `true`.
     *
     * @param upperTypeBoundsB A list (set) of `ObjectType`s that are not in an
     *      inheritance relation if reflexive is `false`.
     * @example
     * {{{
     * /* Consider the following type hierarchy:
     *  *    Object <- Collection <- List
     *  *    Object <- Collection <- Set
     *  *    Object <- Externalizable
     *  *    Object <- Serializable
     *  */
     * Object o = new ...
     * if (...) {
     *      Set s = (Set) o;
     *      (( Externalizable)s).save(...)
     *      // => o(s) has to be a subtype of Set AND Externalizable
     * } else {
     *      List l = (List) l;
     *      ((Serializable)l).store(...)
     *      // => o(l) has to be a subtype of List AND Serializable
     * }
     * // Here, o is either a set or a list. Hence, it is at least a Collection,
     * // but we cannot deduce anything w.r.t. Serializable and Externalizable.
     * }}}
     */
    def joinUpperTypeBounds(
        upperTypeBoundsA: UIDSet[ObjectType],
        upperTypeBoundsB: UIDSet[ObjectType],
        reflexive:        Boolean
    ): UIDSet[ObjectType] = {

        assert(upperTypeBoundsA.nonEmpty)
        assert(upperTypeBoundsB.nonEmpty)

        upperTypeBoundsA.compare(upperTypeBoundsB) match {
            case StrictSubset   => upperTypeBoundsA
            case EqualSets      => upperTypeBoundsA /* or upperTypeBoundsB */
            case StrictSuperset => upperTypeBoundsB
            case UncomparableSets =>
                val allSupertypesOfA = allSupertypesOf(upperTypeBoundsA, reflexive)
                val allSupertypesOfB = allSupertypesOf(upperTypeBoundsB, reflexive)
                val commonSupertypes = allSupertypesOfA intersect allSupertypesOfB
                leafTypes(commonSupertypes)
        }
    }

    /**
     * Tries to calculate the most specific common supertype of the given types.
     * If `reflexive` is `false`, the given types do not have to be in an
     * inheritance relation.
     *
     * @param upperTypeBoundB A list (set) of `ObjectType`s that are not in a mutual
     *      inheritance relation.
     * @return (I) Returns (if reflexive is `true`) `upperTypeBoundA` if it is a supertype
     *      of at least one type of `upperTypeBoundB`.
     *      (II) Returns `upperTypeBoundB` if `upperTypeBoundA` is
     *      a subtype of all types of `upperTypeBoundB`. Otherwise a new upper type
     *      bound is calculated and returned.
     */
    def joinObjectTypes(
        upperTypeBoundA: ObjectType,
        upperTypeBoundB: UIDSet[ObjectType],
        reflexive:       Boolean
    ): UIDSet[ObjectType] = {

        if (upperTypeBoundB.isEmpty)
            return upperTypeBoundB;

        if (upperTypeBoundB.isSingletonSet) {
            val upperTypeBound =
                if (upperTypeBoundA eq upperTypeBoundB.head) {
                    if (reflexive)
                        upperTypeBoundB
                    else
                        directSupertypes(upperTypeBoundA)
                } else {
                    joinObjectTypes(upperTypeBoundA, upperTypeBoundB.head, reflexive)
                }
            return upperTypeBound;
        }

        if (upperTypeBoundB contains upperTypeBoundA) {
            // The upperTypeBoundB contains more than one type; hence, considering
            // "reflexive" is no longer necessary...
            // if (isKnownToBeFinal(upperTypeBoundA)) the upper type bound (hopefully)
            // deliberately contains types which are guaranteed to be in a super-/subtype
            // relation, but which are not part of the analyzed code base. Nevertheless,
            // we are performing a join and therefore, drop the information...
            return new UIDSet1(upperTypeBoundA);
        }

        if (isUnknown(upperTypeBoundA)) {
            OPALLogger.logOnce(Warn(
                "project configuration - class hierarchy", "type unknown: "+upperTypeBoundA.toJava
            ))
            // there is nothing that we can do...
            return ClassHierarchy.JustObject;
        }

        val allSupertypesOfA = allSupertypes(upperTypeBoundA, reflexive)
        val allSupertypesOfB = allSupertypesOf(upperTypeBoundB, reflexive)
        val commonSupertypes = allSupertypesOfA intersect allSupertypesOfB
        leafTypes(commonSupertypes)
    }

    def joinArrayType(
        upperTypeBoundA: ArrayType,
        upperTypeBoundB: UIDSet[_ <: ReferenceType]
    ): UIDSet[_ <: ReferenceType] = {
        upperTypeBoundB match {
            case UIDSet1(utbB: ArrayType) =>
                if (utbB eq upperTypeBoundA)
                    return upperTypeBoundB;
                else
                    joinArrayTypes(upperTypeBoundA, utbB) match {
                        case Left(newUTB)  => new UIDSet1(newUTB)
                        case Right(newUTB) => newUTB
                    }
            case UIDSet1(utbB: ObjectType) =>
                joinAnyArrayTypeWithObjectType(utbB)
            case _ =>
                val utbB = upperTypeBoundB.asInstanceOf[UIDSet[ObjectType]]
                joinAnyArrayTypeWithMultipleTypesBound(utbB)
        }
    }

    def joinReferenceType(
        upperTypeBoundA: ReferenceType,
        upperTypeBoundB: UIDSet[_ <: ReferenceType]
    ): UIDSet[_ <: ReferenceType] = {
        if (upperTypeBoundA.isArrayType)
            joinArrayType(upperTypeBoundA.asArrayType, upperTypeBoundB)
        else
            upperTypeBoundB match {
                case UIDSet1(_: ArrayType) =>
                    joinAnyArrayTypeWithObjectType(upperTypeBoundA.asObjectType)
                case UIDSet1(utbB: ObjectType) =>
                    joinObjectTypes(upperTypeBoundA.asObjectType, utbB, true)
                case _ =>
                    val utbB = upperTypeBoundB.asInstanceOf[UIDSet[ObjectType]]
                    joinObjectTypes(upperTypeBoundA.asObjectType, utbB, true)
            }
    }

    def joinReferenceTypes(
        upperTypeBoundA: UIDSet[_ <: ReferenceType],
        upperTypeBoundB: UIDSet[_ <: ReferenceType]
    ): UIDSet[_ <: ReferenceType] = {
        if ((upperTypeBoundA eq upperTypeBoundB) || upperTypeBoundA == upperTypeBoundB)
            return upperTypeBoundA;

        if (upperTypeBoundA.isEmpty)
            return upperTypeBoundB;

        if (upperTypeBoundB.isEmpty)
            return upperTypeBoundA;

        if (upperTypeBoundA.isSingletonSet)
            joinReferenceType(upperTypeBoundA.head, upperTypeBoundB)
        else if (upperTypeBoundB.isSingletonSet)
            joinReferenceType(upperTypeBoundB.head, upperTypeBoundA)
        else
            joinUpperTypeBounds(
                upperTypeBoundA.asInstanceOf[UIDSet[ObjectType]],
                upperTypeBoundB.asInstanceOf[UIDSet[ObjectType]],
                true
            ).asInstanceOf[UpperTypeBound]
    }

    /**
     * Tries to calculate the most specific common supertype of the two given types.
     * If `reflexive` is `false`, the two types do not have to be in an inheritance
     * relation.
     *
     * If the class hierarchy is not complete, a best guess is made.
     */
    def joinObjectTypes(
        upperTypeBoundA: ObjectType,
        upperTypeBoundB: ObjectType,
        reflexive:       Boolean
    ): UIDSet[ObjectType] = {

        assert(
            reflexive || (
                (upperTypeBoundA ne ObjectType.Object) && (upperTypeBoundB ne ObjectType.Object)
            )
        )

        if (upperTypeBoundA eq upperTypeBoundB) {
            if (reflexive)
                return new UIDSet1(upperTypeBoundA);
            else
                return directSupertypes(upperTypeBoundA /*or ...B*/ );
        }

        if (isSubtypeOf(upperTypeBoundB, upperTypeBoundA)) {
            if (reflexive)
                return new UIDSet1(upperTypeBoundA);
            else
                return directSupertypes(upperTypeBoundA);
        }

        if (isSubtypeOf(upperTypeBoundA, upperTypeBoundB)) {
            if (reflexive)
                return new UIDSet1(upperTypeBoundB);
            else
                return directSupertypes(upperTypeBoundB);
        }

        if (isUnknown(upperTypeBoundA) || isUnknown(upperTypeBoundB)) {
            // there is not too much that we can do...
            return ClassHierarchy.JustObject;
        }

        val allSupertypesOfA = allSupertypes(upperTypeBoundA, reflexive = false)
        val allSupertypesOfB = allSupertypes(upperTypeBoundB, reflexive = false)
        val commonSupertypes = allSupertypesOfA intersect allSupertypesOfB
        val mostSpecificCommonSupertype = leafTypes(commonSupertypes)
        mostSpecificCommonSupertype
    }

    /**
     * Calculates the most specific common supertype of any array type and some
     * class-/interface type.
     *
     * Recall that (Java) arrays implement `Cloneable` and `Serializable`.
     */
    def joinAnyArrayTypeWithMultipleTypesBound(
        thatUpperTypeBound: UIDSet[ObjectType]
    ): UIDSet[ObjectType] = {
        import ObjectType.Cloneable
        import ObjectType.Serializable
        import ObjectType.SerializableAndCloneable

        if (thatUpperTypeBound == SerializableAndCloneable)
            thatUpperTypeBound
        else {
            val isSerializable =
                thatUpperTypeBound exists { thatType => isSubtypeOf(thatType, Serializable) }
            val isCloneable =
                thatUpperTypeBound exists { thatType => isSubtypeOf(thatType, Cloneable) }
            if (isSerializable) {
                if (isCloneable)
                    SerializableAndCloneable
                else
                    new UIDSet1(Serializable)
            } else if (isCloneable) {
                new UIDSet1(Cloneable)
            } else {
                new UIDSet1(Object)
            }
        }
    }

    /**
     * Calculates the most specific common supertype of any array type and some
     * class-/interface type.
     *
     * Recall that (Java) arrays implement `Cloneable` and `Serializable`.
     */
    def joinAnyArrayTypeWithObjectType(thatUpperTypeBound: ObjectType): UIDSet[ObjectType] = {
        import ObjectType.Cloneable
        import ObjectType.Object
        import ObjectType.Serializable

        if ((thatUpperTypeBound eq Object) ||
            (thatUpperTypeBound eq Serializable) ||
            (thatUpperTypeBound eq Cloneable))
            new UIDSet1(thatUpperTypeBound)
        else {
            var newUpperTypeBound: UIDSet[ObjectType] = UIDSet.empty
            if (isSubtypeOf(thatUpperTypeBound, Serializable))
                newUpperTypeBound += Serializable
            if (isSubtypeOf(thatUpperTypeBound, Cloneable))
                newUpperTypeBound += Cloneable
            if (newUpperTypeBound.isEmpty)
                new UIDSet1(Object)
            else
                newUpperTypeBound
        }
    }

    /**
     * Calculates the most specific common supertype of two array types.
     *
     * @return `Left(<SOME_ARRAYTYPE>)` if the calculated type can be represented using
     *      an `ArrayType` and `Right(UIDList(ObjectType.Serializable, ObjectType.Cloneable))`
     *      if the two arrays do not have an `ArrayType` as a most specific common supertype.
     */
    def joinArrayTypes(
        thisUpperTypeBound: ArrayType,
        thatUpperTypeBound: ArrayType
    ): Either[ArrayType, UIDSet[ObjectType]] = {
        // We have ALSO to consider the following corner cases:
        // Foo[][] and Bar[][] => Object[][] (Object is the common super class)
        // Object[] and int[][] => Object[] (which may contain arrays of int values...)
        // Foo[] and int[][] => Object[]
        // int[] and Object[][] => SerializableAndCloneable

        import ObjectType.SerializableAndCloneable

        if (thisUpperTypeBound eq thatUpperTypeBound)
            return Left(thisUpperTypeBound);

        val thisUTBDim = thisUpperTypeBound.dimensions
        val thatUTBDim = thatUpperTypeBound.dimensions

        if (thisUTBDim < thatUTBDim) {
            if (thisUpperTypeBound.elementType.isBaseType) {
                if (thisUTBDim == 1)
                    Right(SerializableAndCloneable)
                else
                    Left(ArrayType(thisUTBDim - 1, ObjectType.Object))
            } else {
                Left(ArrayType(thisUTBDim, ObjectType.Object))
            }
        } else if (thisUTBDim > thatUTBDim) {
            if (thatUpperTypeBound.elementType.isBaseType) {
                if (thatUTBDim == 1)
                    Right(SerializableAndCloneable)
                else
                    Left(ArrayType(thatUTBDim - 1, ObjectType.Object))
            } else {
                Left(ArrayType(thatUTBDim, ObjectType.Object))
            }
        } else if (thisUpperTypeBound.elementType.isBaseType ||
            thatUpperTypeBound.elementType.isBaseType) {
            // => the number of dimensions is the same, but the elementType isn't
            //    (if the element type would be the same, both object reference would
            //    refer to the same object and this would have been handled the very
            //    first test)
            // Scenario:
            // E.g., imagine that we have a method that "just" wants to
            // serialize some data. In such a case the method may be passed
            // different arrays with different primitive values.
            if (thisUTBDim == 1 /* && thatUTBDim == 1*/ )
                Right(SerializableAndCloneable)
            else
                Left(ArrayType(thisUTBDim - 1, ObjectType.Object))
        } else {
            // When we reach this point, the dimensions are identical and both
            // elementTypes are reference types
            val thatElementType = thatUpperTypeBound.elementType.asObjectType
            val thisElementType = thisUpperTypeBound.elementType.asObjectType
            val elementType =
                joinObjectTypesUntilSingleUpperBound(thisElementType, thatElementType, true)
            Left(ArrayType(thisUTBDim, elementType))
        }
    }

    def joinObjectTypesUntilSingleUpperBound(
        upperTypeBoundA: ObjectType,
        upperTypeBoundB: ObjectType,
        reflexive:       Boolean
    ): ObjectType = {
        val newUpperTypeBound = joinObjectTypes(upperTypeBoundA, upperTypeBoundB, reflexive)
        val result =
            if (newUpperTypeBound.isSingletonSet)
                newUpperTypeBound.head
            else
                newUpperTypeBound reduce { (c, n) =>
                    // We are already one level up in the class hierarchy. Hence,
                    // we now certainly want to be reflexive!
                    joinObjectTypesUntilSingleUpperBound(c, n, true)
                }
        result
    }

    /**
     * Given an upper type bound '''a''' most specific type that is a common supertype
     * of the given types is determined.
     *
     * @see `joinObjectTypesUntilSingleUpperBound(upperTypeBoundA: ObjectType,
     *       upperTypeBoundB: ObjectType, reflexive: Boolean)` for further details.
     */
    def joinObjectTypesUntilSingleUpperBound(upperTypeBound: UIDSet[ObjectType]): ObjectType = {
        if (upperTypeBound.isSingletonSet)
            upperTypeBound.head
        else
            upperTypeBound reduce { (c, n) => joinObjectTypesUntilSingleUpperBound(c, n, true) }
    }

    def joinReferenceTypesUntilSingleUpperBound(
        upperTypeBound: UIDSet[_ <: ReferenceType]
    ): ReferenceType = {
        if (upperTypeBound.isSingletonSet)
            upperTypeBound.head
        else
            // Note that the upper type bound must never consist of more than one array type;
            // and that the type hierarchy related to arrays is "hardcoded"
            // ... (here) type erasure also has its benefits ...
            joinObjectTypesUntilSingleUpperBound(upperTypeBound.asInstanceOf[UIDSet[ObjectType]])
    }

    def joinUpperTypeBounds(
        utbA: UIDSet[_ <: ReferenceType],
        utbB: UIDSet[_ <: ReferenceType]
    ): UIDSet[_ <: ReferenceType] = {
        if (utbA == utbB)
            utbA
        else if (utbA.isEmpty)
            utbB
        else if (utbB.isEmpty)
            utbA
        else if (utbA.isSingletonSet && utbA.head.isArrayType) {
            if (utbB.isSingletonSet) {
                if (utbB.head.isArrayType) {
                    val joinedArrayType =
                        joinArrayTypes(
                            utbB.head.asInstanceOf[ArrayType],
                            utbA.head.asInstanceOf[ArrayType]
                        )
                    joinedArrayType match {
                        case Left(arrayType)       => new UIDSet1(arrayType)
                        case Right(upperTypeBound) => upperTypeBound
                    }
                } else {
                    joinAnyArrayTypeWithObjectType(utbB.head.asInstanceOf[ObjectType])
                }
            } else {
                joinAnyArrayTypeWithMultipleTypesBound(utbB.asInstanceOf[UIDSet[ObjectType]])
            }
        } else if (utbB.isSingletonSet) {
            if (utbB.head.isArrayType) {
                joinAnyArrayTypeWithMultipleTypesBound(utbA.asInstanceOf[UIDSet[ObjectType]])
            } else {
                joinObjectTypes(
                    utbB.head.asObjectType,
                    utbA.asInstanceOf[UIDSet[ObjectType]],
                    true
                )
            }
        } else {
            joinUpperTypeBounds(
                utbA.asInstanceOf[UIDSet[ObjectType]],
                utbB.asInstanceOf[UIDSet[ObjectType]],
                true
            )
        }
    }

    override def toString: String = {
        var s = "ClassHierarchy(\n\t"

        // Compute some fundamental class hierarchy statistics:

        // 1. compute subtype information
        {
            var i = 1 // let's skip java.lang.Object
            val subtypesToFrequency = new Int2IntArrayMap()
            while (i < subtypeInformationMap.length) {
                if (subtypeInformationMap(i) != null) {
                    val subtypesCount = subtypeInformationMap(i).size
                    subtypesToFrequency.put(
                        subtypesCount,
                        subtypesToFrequency.getOrDefault(subtypesCount, 0) + 1
                    )
                }
                i += 1
            }
            val subtypesCounts = subtypesToFrequency.keySet().asScala.toList.sorted
            var overallDepth: Double = 0d
            s += "subtype statistics=\n\t"
            s += " subtypes count   / frequency of occurrence (without java.lang.Object)\n\t"
            s +=
                subtypesCounts.map { subtypesCount =>
                    val i: Int = subtypesCount.intValue()
                    val frequency = subtypesToFrequency.get(i)
                    overallDepth += i * frequency
                    f"$subtypesCount%17d / $frequency"
                }.mkString("\n\t")
            s += "\n\t average number of subtypes: "
            s += s"${overallDepth / (subtypeInformationMap.count(_ != null) - 1)}\n\t"
        }
        // 2. compute supertype information
        {
            var i = 1 // let's skip java.lang.Object
            val supertypesToFrequency = new Int2IntArrayMap()
            while (i < supertypeInformationMap.length) {
                if (supertypeInformationMap(i) != null) {
                    val supertypesCount = supertypeInformationMap(i).size
                    supertypesToFrequency.put(
                        supertypesCount,
                        supertypesToFrequency.getOrDefault(supertypesCount, 0) + 1
                    )
                }
                i += 1
            }
            val supertypesCounts = supertypesToFrequency.keySet().asScala.toList.sorted
            var overallDepth: Double = 0d
            s += "supertype statistics=\n\t"
            s += " supertypes count / frequency of occurrence (without java.lang.Object)\n\t"
            s +=
                supertypesCounts.map { supertypesCount =>
                    val i: Int = supertypesCount.intValue()
                    val frequency = supertypesToFrequency.get(i)
                    overallDepth += i * frequency
                    f"$supertypesCount%17d / $frequency"
                }.mkString("\n\t")
            s += "\n\t average number of supertypes: "
            s += (overallDepth / (subtypeInformationMap.count(_ != null) - 1))
        }
        s += "\n)"
        s
    }

}

/**
 * Factory methods for creating `ClassHierarchy` objects.
 *
 * @author Michael Eichberg
 */
object ClassHierarchy {

    private[this] implicit val classHierarchyEC: ExecutionContext = OPALUnboundedExecutionContext

    final val JustObject: UIDSet[ObjectType] = new UIDSet1(ObjectType.Object)

    /**
     * Creates a `ClassHierarchy` that captures the type hierarchy related to
     * the exceptions thrown by specific Java bytecode instructions as well as
     * fundamental types such as Cloneable and Serializable and also those types
     * related to reflection..
     *
     * This class hierarchy is primarily useful for testing purposes.
     */
    lazy val PreInitializedClassHierarchy: ClassHierarchy = {
        apply(classFiles = Iterable.empty, defaultTypeHierarchyDefinitions())(GlobalLogContext)
    }

    def noDefaultTypeHierarchyDefinitions(): List[() => java.io.InputStream] = List.empty

    def defaultTypeHierarchyDefinitions(): List[() => java.io.InputStream] = List(
        () => { getClass.getResourceAsStream("ClassHierarchyJLS.ths") },
        () => { getClass.getResourceAsStream("ClassHierarchyJVMExceptions.ths") },
        () => { getClass.getResourceAsStream("ClassHierarchyJava7-java.lang.reflect.ths") }
    )

    def parseTypeHierarchyDefinition(
        in: InputStream
    )(
        implicit
        logContext: LogContext
    ): Seq[TypeDeclaration] = process(in) { in =>

        if (in eq null) {
            OPALLogger.error(
                "internal - class hierarchy",
                "loading the predefined class hierarchy failed; "+
                    "make sure that all resources are found in the correct folders and "+
                    "try to rebuild the project using \"sbt copyResources\""
            )
            Seq.empty;
        } else {
            val typeRegExp =
                """(class|interface)\s+(\S+)(\s+extends\s+(\S+)(\s+implements\s+(.+))?)?""".r
            processSource(new BufferedSource(in)) { source =>
                source.getLines().
                    map(_.trim).
                    filterNot { l => l.startsWith("#") || l.length == 0 }.
                    map { l =>
                        val typeRegExp(typeKind, theType, _, superclassType, _, superinterfaceTypes) = l
                        TypeDeclaration(
                            ObjectType(theType),
                            typeKind == "interface",
                            Option(superclassType).map(ObjectType(_)),
                            Option(superinterfaceTypes).map { superinterfaceTypes =>
                                UIDSet.fromSpecific[ObjectType](
                                    superinterfaceTypes.
                                        split(',').
                                        map(t => ObjectType(t.trim))
                                )
                            }.getOrElse(UIDSet.empty)
                        )
                    }.
                    toList
            }
        }
    }

    def apply(
        classFiles:               Iterable[ClassFile],
        typeHierarchyDefinitions: Seq[() => InputStream] = defaultTypeHierarchyDefinitions()
    )(
        implicit
        logContext: LogContext
    ): ClassHierarchy = {
        // We have to make sure that we have seen all types before we can generate
        // the arrays to store the information about the types!
        create(
            classFiles,
            typeHierarchyDefinitions flatMap { inputStreamFactory =>
                val in = inputStreamFactory.apply()
                parseTypeHierarchyDefinition(in)
            }
        )
    }

    /**
     * Creates the class hierarchy by analyzing the given class files, the predefined
     * type declarations, and the specified predefined class hierarchies.
     *
     * By default the class hierarchy related to the exceptions thrown by bytecode
     * instructions are predefined as well as the class hierarchy related to the main
     * classes of the JDK.
     * See the file `ClassHierarchyJVMExceptions.ths`, `ClassHierarchyJLS.ths` and
     * `ClassHierarchyJava7-java.lang.reflect.ths` (text files) for further details.
     *
     * Basically, only the part of a project's class hierarchy is reified that is referred
     * to in the ''class declarations'' of the analyzed classes  I.e., those classes
     * which are directly referred to in class declarations, but for which the respective
     * class file was not analyzed, are also considered to be visible and are integrated
     * in the class hierarchy.
     * However, types only referred to in the body of a method, but for which neither
     * the defining class file is analyzed nor a class exists that inherits from
     * them are not integrated.
     * For example, if the class file of the class `java.util.ArrayList` is analyzed,
     * then the class hierarchy will have some information about, e.g., `java.util.List`
     * from which `ArrayList` inherits. However, the information about `List` is incomplete
     * and `List` will be a boundary class unless we also analyze the class file that
     * defines `java.util.List`.
     */
    def create(
        classFiles:       Iterable[ClassFile],
        typeDeclarations: Iterable[TypeDeclaration]
    )(
        implicit
        logContext: LogContext
    ): ClassHierarchy = {

        val objectTypesCount = ObjectType.objectTypesCount
        val knownTypesMap = new Array[ObjectType](objectTypesCount)
        val isInterfaceTypeMap = new Array[Boolean](objectTypesCount)
        val superclassTypeMap = new Array[ObjectType](objectTypesCount)
        val isKnownToBeFinalMap = new Array[Boolean](objectTypesCount)
        val superinterfaceTypesMap = new Array[UIDSet[ObjectType]](objectTypesCount)
        val subclassTypesMap = new Array[UIDSet[ObjectType]](objectTypesCount)
        val subinterfaceTypesMap = new Array[UIDSet[ObjectType]](objectTypesCount)

        val ObjectId = ObjectType.Object.id

        // Collects those classes which declare to implement a specific interface, but which
        // is actually a regular class.
        var classesWithBrokenInterfaceInheritance: Map[ObjectType, UIDSet[ObjectType]] = {
            Map.empty.withDefaultValue(UIDSet.empty)
        }

        /*
         * Extends the class hierarchy.
         */
        def process(
            objectType:             ObjectType,
            isInterfaceType:        Boolean,
            isFinal:                Boolean,
            theSuperclassType:      Option[ObjectType],
            theSuperinterfaceTypes: UIDSet[ObjectType]
        ): Unit = {

            if (isInterfaceType && isFinal) {
                val message = s"the class file ${objectType.toJava} defines a final interface "+
                    "which violates the JVM specification and is therefore ignored"
                OPALLogger.error("project configuration - class hierarchy", message)

                return ;
            }

            def addToSet(data: Array[UIDSet[ObjectType]], index: Int, t: ObjectType): Unit = {
                val objectTypes = data(index)
                data(index) = {
                    if (objectTypes eq null)
                        new UIDSet1(t)
                    else
                        objectTypes + t
                }
            }

            def ensureHasSet(data: Array[UIDSet[ObjectType]], index: Int): Unit = {
                if (data(index) eq null) {
                    data(index) = UIDSet.empty
                }
            }

            //
            // Update the class hierarchy from the point of view of the newly added type
            //
            val objectTypeId = objectType.id
            knownTypesMap(objectTypeId) = objectType
            isInterfaceTypeMap(objectTypeId) = isInterfaceType
            isKnownToBeFinalMap(objectTypeId) = isFinal
            superclassTypeMap(objectTypeId) = theSuperclassType.orNull
            superinterfaceTypesMap(objectTypeId) = theSuperinterfaceTypes
            ensureHasSet(subclassTypesMap, objectTypeId)
            ensureHasSet(subinterfaceTypesMap, objectTypeId)

            //
            // Update the class hierarchy from the point of view of the new type's super types
            // For each super(class|interface)type make sure that it is "known"
            //
            theSuperclassType foreach { superclassType =>
                val superclassTypeId = superclassType.id
                knownTypesMap(superclassTypeId) = superclassType

                if (isInterfaceType) {
                    // an interface always has `java.lang.Object` as its super class
                    addToSet(subinterfaceTypesMap, ObjectId /*java.lang.Object*/ , objectType)
                } else {
                    addToSet(subclassTypesMap, superclassTypeId, objectType)
                    ensureHasSet(subinterfaceTypesMap, superclassTypeId)
                }
            }
            theSuperinterfaceTypes foreach { aSuperinterfaceType =>
                val aSuperinterfaceTypeId = aSuperinterfaceType.id

                if (knownTypesMap(aSuperinterfaceTypeId) eq null) {
                    knownTypesMap(aSuperinterfaceTypeId) = aSuperinterfaceType
                    isInterfaceTypeMap(aSuperinterfaceTypeId) = true
                } else if (!isInterfaceTypeMap(aSuperinterfaceTypeId)) {
                    val message = s"the class file ${objectType.toJava} defines a "+
                        s"super interface ${knownTypesMap(aSuperinterfaceTypeId).toJava} "+
                        "which is actually a regular class file"
                    classesWithBrokenInterfaceInheritance +=
                        ((
                            objectType,
                            classesWithBrokenInterfaceInheritance(objectType) + aSuperinterfaceType
                        ))
                    OPALLogger.error("project configuration - class hierarchy", message)
                }
                if (isInterfaceType) {
                    addToSet(subinterfaceTypesMap, aSuperinterfaceTypeId, objectType)
                    ensureHasSet(subclassTypesMap, aSuperinterfaceTypeId)
                } else {
                    addToSet(subclassTypesMap, aSuperinterfaceTypeId, objectType)
                    // assert(subclassTypesMap(aSuperinterfaceTypeId).contains(objectType))
                    ensureHasSet(subinterfaceTypesMap, aSuperinterfaceTypeId)
                }
            }
        }

        // Analyzes the given class files and extends the current class hierarchy.
        val processedClassType: Array[Boolean] = new Array[Boolean](objectTypesCount)
        classFiles foreach { classFile =>
            if (!classFile.isModuleDeclaration) {
                // We always keep the FIRST class file which defines a type this is inline
                // with the behavior of the class Project which prioritizes a project class file
                // over library class files.
                val classType = classFile.thisType
                if (!processedClassType(classType.id)) {
                    processedClassType(classType.id) = true
                    process(
                        classType,
                        classFile.isInterfaceDeclaration,
                        classFile.isFinal,
                        classFile.superclassType,
                        UIDSet.empty ++ classFile.interfaceTypes
                    )
                }
            }
        }

        val processedTypeDeclaration: Array[Boolean] = new Array[Boolean](objectTypesCount)
        var duplicateTypeDeclarations: Set[String] = Set.empty
        typeDeclarations foreach { typeDeclaration =>
            val objectType = typeDeclaration.objectType
            val oid = objectType.id
            if (processedTypeDeclaration(oid)) {
                duplicateTypeDeclarations += objectType.toJava
            } else {
                processedTypeDeclaration(oid) = true
                // We generally don't want to use pre-configured type hierarchy information;
                // but we want to extend the information if it is obviously incomplete...
                if (!processedClassType(oid) ||
                    // processed - but complete?
                    ((oid != ObjectType.ObjectId) && superclassTypeMap(oid) == null)) {
                    processedClassType(objectType.id) = true
                    process(
                        objectType,
                        typeDeclaration.isInterfaceType,
                        isFinal = false,
                        typeDeclaration.theSuperclassType,
                        typeDeclaration.theSuperinterfaceTypes
                    )
                } else {
                    OPALLogger.warn(
                        "project configuration",
                        s"the type declaration for ${objectType.toJava} is ignored; "+
                            "class is already defined in the code base "+
                            s"with superclassType ${superclassTypeMap(oid).toJava}"+
                            "or defined multiple times in the configured type hierarchy"
                    )
                }
            }
        }
        if (duplicateTypeDeclarations.nonEmpty) {
            OPALLogger.info(
                "project configuration",
                duplicateTypeDeclarations.mkString(
                    "ignored duplicate type declarations for: {", ", ", "}"
                )
            )
        }

        // _____________________________________________________________________________________
        //
        // WHEN WE REACH THIS POINT WE HAVE COLLECTED ALL BASE INFORMATION
        // LET'S ENSURE THAT WE DIDN'T MAKE ANY FAILURES
        // _____________________________________________________________________________________
        //

        assert(knownTypesMap.length == isInterfaceTypeMap.length)
        assert(knownTypesMap.length == isKnownToBeFinalMap.length)
        assert(knownTypesMap.length == superclassTypeMap.length)
        assert(knownTypesMap.length == superinterfaceTypesMap.length)
        assert(knownTypesMap.length == subclassTypesMap.length)
        assert(knownTypesMap.length == subinterfaceTypesMap.length)
        assert(
            knownTypesMap.indices forall { i =>
                (knownTypesMap(i) ne null) ||
                    ((subclassTypesMap(i) eq null) && (subinterfaceTypesMap(i) eq null))
            }
        )

        // _____________________________________________________________________________________
        //
        // LET'S DERIVE SOME FURTHER INFORMATION WHICH IS FREQUENTLY USED!
        // _____________________________________________________________________________________
        //

        val isKnownTypeMap: Array[Boolean] = knownTypesMap.map(_ != null)

        val rootTypesFuture = Future[UIDSet[ObjectType]] {
            knownTypesMap.foldLeft(UIDSet.empty[ObjectType]) { (rootTypes, objectType) =>
                if ((objectType ne null) && {
                    val oid = objectType.id
                    (superclassTypeMap(oid) eq null) &&
                        {
                            val superinterfaceTypes = superinterfaceTypesMap(oid)
                            (superinterfaceTypes eq null) || superinterfaceTypes.isEmpty
                        }
                }) {
                    rootTypes + objectType
                } else {
                    rootTypes
                }
            }
        }

        val subtypesFuture = Future[(UIDSet[ObjectType], Array[SubtypeInformation])] {
            val leafTypes = knownTypesMap.foldLeft(UIDSet.empty[ObjectType]) { (leafTypes, t) =>
                if ((t ne null) && {
                    val tid = t.id
                    subclassTypesMap(tid).isEmpty && subinterfaceTypesMap(tid).isEmpty
                }) {
                    leafTypes + t
                } else {
                    leafTypes
                }
            }

            // Let's compute for each type the set of all subtypes, by starting at the bottom!
            val subtypes = new Array[SubtypeInformation](knownTypesMap.length)
            var deferredTypes = UIDSet.empty[ObjectType] // we want to defer as much as possible
            val typesToProcess = mutable.Queue.empty[ObjectType]

            def scheduleSupertypes(objectType: ObjectType): Unit = {
                val oid = objectType.id
                val superclassType = superclassTypeMap(oid)
                if ((superclassType ne null) && (superclassType ne ObjectType.Object)) {
                    typesToProcess += superclassType
                }
                val superSuperinterfaceTypes = superinterfaceTypesMap(oid)
                if (superSuperinterfaceTypes ne null) {
                    typesToProcess ++= superSuperinterfaceTypes
                }
            }

            leafTypes foreach { leafType =>
                subtypes(leafType.id) = SubtypeInformation.None
                scheduleSupertypes(leafType)
            }

            var madeProgress = false
            while (typesToProcess.nonEmpty) {
                val t = typesToProcess.dequeue()
                val tid = t.id
                // it may be the case that some type was already processed
                if (subtypes(tid) == null) {
                    var allSubinterfaceTypes = UIDSet.empty[ObjectType]
                    var allSubclassTypes = UIDSet.empty[ObjectType]
                    var allSubtypes = UIDSet.empty[ObjectType]
                    val done =
                        subinterfaceTypesMap(tid).forall { subtype =>
                            subtypes(subtype.id) match {
                                case null =>
                                    false
                                case subSubtypes =>
                                    allSubinterfaceTypes ++= subSubtypes.interfaceTypes
                                    allSubclassTypes ++= subSubtypes.classTypes
                                    allSubinterfaceTypes += subtype
                                    allSubtypes ++= (subSubtypes.allTypes + subtype)
                                    true
                            }
                        } && subclassTypesMap(tid).forall { subtype =>
                            subtypes(subtype.id) match {
                                case null =>
                                    false
                                case subSubtypes =>
                                    // There will be no sub interface types!
                                    // (java.lang.Object is not considered)
                                    allSubclassTypes ++= subSubtypes.classTypes
                                    allSubclassTypes += subtype
                                    allSubtypes ++= (subSubtypes.allTypes + subtype)
                                    true
                            }
                        }

                    if (done) {
                        madeProgress = true
                        val subtypeInfo = SubtypeInformation.forSubtypesOfObject(
                            isKnownTypeMap,
                            isInterfaceTypeMap,
                            allSubclassTypes, allSubinterfaceTypes, allSubtypes
                        )
                        subtypes(t.id) = subtypeInfo
                        scheduleSupertypes(t)
                    } else {
                        deferredTypes += t
                    }
                } else {
                    madeProgress = true // this is philosophical...
                }

                // test if we have really finished processing all types
                if (typesToProcess.isEmpty && deferredTypes.nonEmpty) {
                    if (!madeProgress) {
                        // The following is NOT performance sensitive... we are lost anyway
                        // and we just want to provide some hints to the user...
                        // 1. Do we have a cycle in the extracted type information ?
                        {
                            val ns = knownTypesMap.length
                            val es: Int => IntIterator = (oid: Int) => {
                                if (knownTypesMap(oid) ne null) {
                                    val it =
                                        subinterfaceTypesMap(oid).map(_.id).iterator ++
                                            subclassTypesMap(oid).map(_.id).iterator
                                    new IntIterator {
                                        def hasNext: Boolean = it.hasNext
                                        def next(): Int = it.next()
                                    }
                                } else {
                                    IntIterator.empty
                                }
                            }
                            val cyclicTypeDependencies =
                                org.opalj.graphs.sccs(ns, es, filterSingletons = true)
                            if (cyclicTypeDependencies.nonEmpty) {
                                OPALLogger.error(
                                    "project configuration",
                                    cyclicTypeDependencies.map { scc =>
                                        scc.map { oid =>
                                            if (knownTypesMap(oid) ne null)
                                                knownTypesMap(oid).toJava
                                            else
                                                "N/A"
                                        }.mkString(", ")
                                    }.mkString("cyclic type hierarchy:\n\t", "\n\t", "\n")
                                )
                            }
                        }

                        // 2. Which type(s) cause the problem?
                        val allIssues =
                            for {
                                dt <- deferredTypes
                                subtype <- subinterfaceTypesMap(dt.id) ++ subclassTypesMap(dt.id)
                                if subtypes(subtype.id) != null
                                if !deferredTypes.contains(subtype)
                            } yield {
                                s"${dt.toJava} (waits)->(subtype) ${subtype.toJava}"
                            }
                        OPALLogger.error(
                            "project configuration",
                            allIssues.mkString(
                                "could not compute subtype information for:\n\t", "\n\t", "\n"
                            )
                        )

                    } else {
                        madeProgress = false
                        typesToProcess ++= deferredTypes
                        deferredTypes = UIDSet.empty[ObjectType]
                    }
                }
            }
            var allNoneObjectClassTypes = UIDSet.empty[ObjectType]
            var allInterfaceType = UIDSet.empty[ObjectType]
            var allNoneObjectTypes = UIDSet.empty[ObjectType]
            for {
                t <- knownTypesMap
                if t ne null
            } {
                val tid = t.id
                val theSubtypes = subtypes(tid)
                if (isInterfaceTypeMap(tid)) {
                    allInterfaceType ++= theSubtypes.interfaceTypes
                    allNoneObjectTypes ++= theSubtypes.allTypes
                } else if (t ne ObjectType.Object) {
                    allNoneObjectClassTypes ++= theSubtypes.classTypes
                    allNoneObjectClassTypes += t
                    allNoneObjectTypes ++= theSubtypes.allTypes
                }
            }
            subtypes(ObjectType.ObjectId) =
                SubtypeInformation.forObject(
                    allNoneObjectClassTypes, allInterfaceType, allNoneObjectTypes
                )

            (leafTypes, subtypes)
        }

        val supertypesFuture = Future[Array[SupertypeInformation]] {
            // Selects all interfaces which either have no superinterfaces or where we have no
            // information about all implemented superinterfaces.
            def rootInterfaceTypes: Iterator[ObjectType] = {
                superinterfaceTypesMap.iterator.zipWithIndex.filter { si =>
                    val (superinterfaceTypes, id) = si
                    isInterfaceTypeMap(id) &&
                        ((superinterfaceTypes eq null) || superinterfaceTypes.isEmpty)
                }.map { ts => knownTypesMap(ts._2) }
            }

            val supertypes = new Array[SupertypeInformation](knownTypesMap.length)
            supertypes(ObjectId) = SupertypeInformation.ForObject

            val typesToProcess = mutable.Queue.empty[ObjectType] ++ rootInterfaceTypes

            // IDEA: breadth-first traversal of the class hierarchy with some fallback if an
            // interface inherits from multiple interfaces (and maybe from the same (indirect)
            // superinterface.)

            // 1. process all interface types
            while (typesToProcess.nonEmpty) {
                val t = typesToProcess.dequeue()
                val tid = t.id
                val superinterfaceTypes = {
                    val superinterfaceTypes = superinterfaceTypesMap(tid)
                    if (superinterfaceTypes ne null)
                        superinterfaceTypes
                    else
                        UIDSet.empty[ObjectType]
                }

                // let's check if we already have complete information about all supertypes
                var allSuperSuperinterfaceTypes = UIDSet.empty[ObjectType]
                var allSupertypes = UIDSet.empty[ObjectType]
                if (superinterfaceTypes.forall { supertype =>
                    val supertypeId = supertype.id
                    supertypes(supertypeId) match {
                        case null =>
                            // It may happen that we we will never have complete information about a
                            // superinterface type, because we have an incomplete project OR
                            // that the class hierarchy is totally broken in the sense that
                            // the super interface types are actually class types.
                            // In that case, we just ignore it...
                            superinterfaceTypesMap(supertypeId) == null ||
                                classesWithBrokenInterfaceInheritance(t).containsId(supertypeId)
                        case supertypes =>
                            allSuperSuperinterfaceTypes ++= supertypes.interfaceTypes
                            allSupertypes ++= supertypes.allTypes
                            true

                    }
                }) {
                    supertypes(t.id) =
                        SupertypeInformation.forSubtypesOfObject(
                            isKnownTypeMap,
                            isInterfaceTypeMap,
                            ClassHierarchy.JustObject,
                            allSuperSuperinterfaceTypes ++ superinterfaceTypes,
                            allSupertypes ++ superinterfaceTypes
                        )
                    typesToProcess ++= subinterfaceTypesMap(t.id)
                } else {
                    typesToProcess += t
                }
            }

            // 2. process all class types
            val rootTypes = await(rootTypesFuture, Inf) // we may have to wait...
            typesToProcess ++= rootTypes.iterator.filterNot(t => isInterfaceTypeMap(t.id))
            while (typesToProcess.nonEmpty) {
                val t = typesToProcess.dequeue()
                val tid = t.id
                if (tid != ObjectId) {
                    val superinterfaceTypes = {
                        val superinterfaceTypes = superinterfaceTypesMap(tid)
                        if (superinterfaceTypes ne null)
                            superinterfaceTypes
                        else
                            UIDSet.empty[ObjectType]
                    }
                    val superclassType = superclassTypeMap(t.id)
                    val allSuperinterfaceTypes =
                        superinterfaceTypes.foldLeft(
                            if (superclassType ne null) {
                                // interfaces inherited via super class
                                supertypes(superclassType.id).interfaceTypes
                            } else {
                                UIDSet.empty[ObjectType]
                            }
                        ) { (allInterfaceTypes, nextSuperinterfacetype) =>
                                (supertypes(nextSuperinterfacetype.id) match {
                                    case null       => allInterfaceTypes
                                    case supertypes => allInterfaceTypes ++ supertypes.interfaceTypes
                                }) + nextSuperinterfacetype
                            }
                    supertypes(tid) =
                        SupertypeInformation.forSubtypesOfObject(
                            isKnownTypeMap,
                            isInterfaceTypeMap,
                            {
                                if (superclassType ne null)
                                    supertypes(superclassType.id).classTypes + superclassType
                                else
                                    ClassHierarchy.JustObject // we do our best....
                            },
                            allSuperinterfaceTypes,
                            {
                                if (superclassType ne null)
                                    supertypes(superclassType.id).allTypes + superclassType
                                else
                                    ClassHierarchy.JustObject // we do our best....
                            }
                        )
                }
                typesToProcess ++= subclassTypesMap(t.id)
            }

            supertypes
        }

        val isSupertypeInformationCompleteFuture = Future[Array[Boolean]] {
            val isSupertypeInformationCompleteMap = new Array[Boolean](knownTypesMap.length)
            java.util.Arrays.fill(isSupertypeInformationCompleteMap, true)

            val (_, subtypes) = await(subtypesFuture, Inf)
            // NOTE: The supertype information for each type that directly inherits from
            // java.lang.Object is still not necessarily complete as the type may implement an
            // unknown interface.
            for {
                rootType <- await(rootTypesFuture, Inf) // we may have to wait...
                if rootType ne ObjectType.Object
            } {
                isSupertypeInformationCompleteMap(rootType.id) = false
                subtypes(rootType.id).foreach(t => isSupertypeInformationCompleteMap(t.id) = false)
            }
            isSupertypeInformationCompleteMap
        }

        val rootTypes = await(rootTypesFuture, Inf)

        /* Validate the class hierarchy... ensure concurrent execution! */
        Future[Unit] {
            // Checks if the class hierarchy is self-consistent and fixes related issues
            // if possible. All issues and fixes are logged.
            val unexpectedRootTypes = rootTypes.iterator filter (_ ne ObjectType.Object)
            if (unexpectedRootTypes.hasNext) {
                OPALLogger.warn(
                    "project configuration - class hierarchy",
                    unexpectedRootTypes
                        .map { t =>
                            (if (isInterfaceTypeMap(t.id)) "interface " else "class ") + t.toJava
                        }
                        .toList.sorted
                        .take(10)
                        .mkString(
                            "supertype information incomplete: {",
                            ", ",
                            if (unexpectedRootTypes.size > 10) ", ...}" else "}"
                        )
                )
            }

            isKnownToBeFinalMap.iterator.zipWithIndex foreach { e =>
                val (isFinal, oid) = e
                if (isFinal) {
                    if (subclassTypesMap(oid).nonEmpty) {
                        OPALLogger.warn(
                            "project configuration - class hierarchy",
                            s"the final type ${knownTypesMap(oid).toJava} "+
                                "has subclasses: "+subclassTypesMap(oid)+
                                "; resetting the \"is final\" property."
                        )
                        isKnownToBeFinalMap(oid) = false
                    }

                    if (subinterfaceTypesMap(oid).nonEmpty) {
                        OPALLogger.warn(
                            "project configuration - class hierarchy",
                            s"the final type ${knownTypesMap(oid).toJava} "+
                                "has subinterfaces: "+subclassTypesMap(oid)+
                                "; resetting the \"is final\" property."
                        )
                        isKnownToBeFinalMap(oid) = false
                    }
                }
            }
        }

        val isSupertypeInformationCompleteMap = await(isSupertypeInformationCompleteFuture, Inf)

        val (leafTypes, subtypes) = await(subtypesFuture, Inf)

        val supertypes = await(supertypesFuture, Inf)

        new ClassHierarchy(
            // BAREBONE INFORMATION
            knownTypesMap,
            isKnownTypeMap,
            isInterfaceTypeMap,
            isKnownToBeFinalMap,
            superclassTypeMap,
            superinterfaceTypesMap,
            subclassTypesMap,
            subinterfaceTypesMap,
            // DERIVED INFORMATION
            rootTypes,
            leafTypes,
            isSupertypeInformationCompleteMap,
            supertypes,
            subtypes
        )
    }
}
