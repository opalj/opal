/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import scala.annotation.tailrec
import scala.language.implicitConversions

import java.io.InputStream
import java.util.concurrent.locks.ReentrantReadWriteLock
import scala.collection.mutable
import scala.concurrent.Await.result as await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.Duration.Inf
import scala.io.BufferedSource
import scala.jdk.CollectionConverters.*
import scala.util.boundary
import scala.util.boundary.break

import org.opalj.br.ClassType.Object
import org.opalj.collection.CompleteCollection
import org.opalj.collection.EqualSets
import org.opalj.collection.ForeachRefIterator
import org.opalj.collection.IncompleteCollection
import org.opalj.collection.IntIterator
import org.opalj.collection.QualifiedCollection
import org.opalj.collection.StrictSubset
import org.opalj.collection.StrictSuperset
import org.opalj.collection.UncomparableSets
import org.opalj.collection.immutable.UIDSet
import org.opalj.collection.immutable.UIDSet1
import org.opalj.concurrent.OPALUnboundedExecutionContext
import org.opalj.control.foreachNonNullValue
import org.opalj.graphs.Node
import org.opalj.io.process
import org.opalj.io.processSource
import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger
import org.opalj.log.Warn

import it.unimi.dsi.fastutil.ints.Int2IntArrayMap

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
 * @note    Unless explicitly documented, it is an error to pass an instance of `ClassType`
 *          to any method if the `ClassType` was not previously added. If in doubt, first
 *          check if the type is known (`isKnown`/`ifKnown`).
 *
 * @param   knownTypesMap A mapping between the id of a class type and the class type;
 *          implicitly encodes which types are known.
 *
 * @param   isInterfaceTypeMap `true` iff the type is an interface otherwise `false`;
 *          '''only defined for those types that are known'''.
 *
 * @param   isKnownToBeFinalMap `true` if the class is known to be `final`. I.e.,
 *          if the class is final `isFinal(ClassFile(classType)) =>
 *          isFinal(classHierarchy(classType))`.
 *
 * @param   superclassTypeMap Contains type information about a type's immediate superclass.
 *          This value is always defined (i.e., not null) unless the key identifies the
 *          class type `java.lang.Object` or when the respective class file was not
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
 * @param   supertypeInformationMap Contains for each class type the set of classes and
 *          interfaces it inherits from. This set is computed on a best-effort basis.
 *
 *          In some cases the supertype information may be incomplete, because the project
 *          as such is incomplete. Whether the type information is complete for a given type
 *          or not can be checked using `isSupertypeInformationComplete`.
 *
 * @author Michael Eichberg
 */
class ClassHierarchy private (
    // the case "java.lang.Object" is handled explicitly!
    private val knownTypesMap:       Array[ClassType],
    private val isKnownTypeMap:      Array[Boolean],
    private val isInterfaceTypeMap:  Array[Boolean],
    private val isKnownToBeFinalMap: Array[Boolean],

    // The element is null for types for which we have no complete information
    // (unless it is java.lang.Object)!
    private val superclassTypeMap:      Array[ClassType],
    private val superinterfaceTypesMap: Array[UIDSet[ClassType]],

    // In the following all elements are non-null for each known type!
    private val subclassTypesMap:     Array[UIDSet[ClassType]],
    private val subinterfaceTypesMap: Array[UIDSet[ClassType]],

    // DERIVED INFORMATION
    val rootTypes:                                 UIDSet[ClassType],
    val leafTypes:                                 UIDSet[ClassType],
    private val isSupertypeInformationCompleteMap: Array[Boolean],
    private val supertypeInformationMap:           Array[SupertypeInformation],
    private val subtypeInformationMap:             Array[SubtypeInformation]
)(
    implicit val logContext: LogContext
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
            using newLogContext
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
    def rootClassTypesIterator: Iterator[ClassType] = {
        knownTypesMap.iterator filter { classType =>
            (classType ne null) && {
                val cid = classType.id
                (superclassTypeMap(cid) eq null) && !isInterfaceTypeMap(cid)
            }
        }
    }

    def leafClassTypesIterator: Iterator[ClassType] = {
        leafTypes.iterator filterNot { classType => isInterfaceTypeMap(classType.id) }
    }

    /**
     * Iterates over all interfaces which only inherit from `java.lang.Object` and adds the
     * types to the given `Growable` collection. I.e., iterates
     * over all interfaces which are at the top of the interface inheritance hierarchy.
     */
    def rootInterfaceTypes(collection: mutable.Growable[ClassType]): collection.type = {
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

        implicit def classTypeToString(ct: ClassType): String = {
            if (ct ne null) ct.toJava else "N/A"
        }

        implicit def classTypesToString(cts: UIDSet[ClassType]): String = {
            if (cts ne null) cts.map(_.toJava).mkString("{", ",", "}") else "N/A"
        }

        case class TypeInfo(
            classType:                      String,
            classTypeId:                    Int,
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
                s"$classType \t$classTypeId \t$isInterface \t$isFinal \t$isRootType \t$isLeafType \t" +
                    s"$isSupertypeInformationComplete \t$superclassType \t$superinterfaceTypes \t" +
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
            "type \tid \tinterface \tfinal" +
                " \troot type \tleaf type \tsupertype information complete" +
                " \tsuper class \tsuper interfaces \tsub classes \tsub interfaces\n"
        typeInfos.sorted.mkString(header, "\n", "\n")
    }

    //
    //
    // IMPLEMENTS THE MAPPING BETWEEN A ClassType AND IT'S ID
    //
    //

    private var classTypesMap: Array[ClassType] = new Array(ClassType.classTypesCount)
    private final val classTypesMapRWLock = new ReentrantReadWriteLock()

    private final def classTypesCreationListener(classType: ClassType): Unit = {
        val id = classType.id
        val writeLock = classTypesMapRWLock.writeLock()
        writeLock.lock()
        try {
            val thisClassTypesMap = classTypesMap
            if (id >= thisClassTypesMap.length) {
                val newLength = Math.max(ClassType.classTypesCount, id) + 100
                val newClassTypesMap = new Array[ClassType](newLength)
                Array.copy(thisClassTypesMap, 0, newClassTypesMap, 0, thisClassTypesMap.length)
                newClassTypesMap(id) = classType
                classTypesMap = newClassTypesMap
            } else {
                thisClassTypesMap(id) = classType
            }
        } finally {
            writeLock.unlock()
        }
    }

    ClassType.setClassTypeCreationListener(classTypesCreationListener)

    /**
     * Returns the `ClassType` with the given Id. The id has to be the id of a valid
     * ClassType.
     */
    final def getClassType(classTypeId: Int): ClassType = {
        val readLock = classTypesMapRWLock.readLock()
        readLock.lock()
        try {
            val ct = classTypesMap(classTypeId)
            if (ct eq null) {
                throw new IllegalArgumentException("ClassType id invalid: " + classTypeId)
            }
            ct
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
    def supertypeInformation(classType: ClassType): Option[SupertypeInformation] = {
        val cid = classType.id
        if (isKnown(cid)) {
            Some(supertypeInformationMap(cid))
        } else {
            None
        }
    }

    /**
     * Returns the subtype information if the given type is known. If the given type is unknown
     * `None` is returned.
     */
    def subtypeInformation(classType: ClassType): Option[SubtypeInformation] = {
        val cid = classType.id
        if (isKnown(cid)) {
            Some(subtypeInformationMap(cid))
        } else {
            None
        }
    }

    /**
     * Returns `true` if the class hierarchy has some information about the given
     * type.
     *
     * @note    Consider using isKnown(classTypeId : Int) if you need the object ids anyway.
     */
    @inline final def isKnown(classType: ClassType): Boolean = isKnown(classType.id)

    @inline final def isKnown(classTypeId: Int): Boolean = {
        val isKnownTypeMap = this.isKnownTypeMap
        classTypeId < isKnownTypeMap.length && isKnownTypeMap(classTypeId)
    }

    /**
     * Returns `true` if the type is unknown. This is `true` for all types that are
     * referred to in the body of a method, but which are not referred to in the
     * declarations of the class files that were analyzed.
     *
     * @note    Consider using isUnknown(classTypeId : Int) if you need the object ids anyway.
     */
    @inline final def isUnknown(classType: ClassType): Boolean = isUnknown(classType.id)

    @inline final def isUnknown(classTypeId: Int): Boolean = {
        val isKnownTypeMap = this.isKnownTypeMap
        classTypeId >= knownTypesMap.length || !isKnownTypeMap(classTypeId)
    }

    /**
     * Tests if the given classType is known and if so executes the given function.
     *
     * @example
     * {{{
     * ifKnown(ClassType.Serializable){isDirectSupertypeInformationComplete}
     * }}}
     */
    @inline final def ifKnown[T](classType: ClassType)(f: ClassType => T): Option[T] = {
        if (isKnown(classType))
            Some(f(classType))
        else
            None
    }

    /**
     * Calls the given function `f` for each type that is known to the class hierarchy.
     */
    def foreachKnownType[T](f: ClassType => T): Unit = {
        foreachNonNullValue(knownTypesMap)((_ /*index*/, t) => f(t))
    }

    /**
     * Returns `true` if the given type is `final`. I.e., the declaring class
     * was explicitly declared `final` and no subtypes exist.
     *
     * @note No explicit `isKnown` check is required.
     *
     * @return  `false` is returned if:
     *  - the class type is unknown,
     *  - the class type is known not to be final or
     *  - the information is incomplete
     */
    @inline def isKnownToBeFinal(classType: ClassType): Boolean = {
        isKnownToBeFinal(classType.id)
    }

    @inline def isKnownToBeFinal(classTypeId: Int): Boolean = {
        isKnown(classTypeId) && isKnownToBeFinalMap(classTypeId)
    }

    /**
     * Returns `true` if the given type is known and is `final`. I.e., the declaring class
     * was explicitly declared final or – if the type identifies an array type –
     * the component type is either known to be final or is a primitive/base type.
     *
     * @note No explicit `isKnown` check is required.
     *
     * @return `false` is returned if:
     *  - the class type/component type is unknown,
     *  - the class type/component type is known not to be final or
     *  - the information about the class type/component type is incomplete
     */
    @inline def isKnownToBeFinal(referenceType: ReferenceType): Boolean = {
        referenceType match {
            case classType: ClassType =>
                isKnownToBeFinal(classType)
            case arrayType: ArrayType =>
                val elementType = arrayType.elementType
                elementType.isBaseType || isKnownToBeFinal(elementType.asClassType)
        }
    }

    /**
     * Returns `true` if the given `classType` is known and defines an interface type.
     *
     * @note No explicit `isKnown` check is required.
     *
     * @param classType An `ClassType`.
     */
    @inline def isInterface(classType: ClassType): Answer = {
        val cid = classType.id
        if (isUnknown(cid))
            Unknown
        else
            Answer(isInterfaceTypeMap(cid))
    }

    /** Returns  `true` if and only if the given type is known to define an interface! */
    @inline private def isInterface(classTypeId: Int): Boolean = {
        isKnown(classTypeId) && isInterfaceTypeMap(classTypeId)
    }

    @inline private[br] def unsafeIsInterface(classTypeId: Int): Boolean = {
        isInterfaceTypeMap(classTypeId)
    }

    /**
     * Returns `true` if the type hierarchy information related to the given type's
     * supertypes is complete.
     *
     * @note No explicit `isKnown` check is required.
     */
    @inline def isDirectSuperclassTypeInformationComplete(classType: ClassType): Boolean = {
        (classType eq Object) || {
            val cid = classType.id
            isKnown(cid) && superclassTypeMap(cid) != null
        }
    }

    /**
     * Returns `true` if the type hierarchy has complete information about all supertypes
     * of the given type.
     *
     * @note No explicit `isKnown` check is required.
     */
    @inline final def isSupertypeInformationComplete(classType: ClassType): Boolean = {
        val cid = classType.id
        isKnown(cid) && isSupertypeInformationCompleteMap(cid)
    }

    /**
     * Returns `Yes` if the class hierarchy contains subtypes of the given type and `No` if
     * it contains no subtypes. `Unknown` is returned if the given type is not known.
     *
     * Please note, that the answer will be `No` even though the (running) project contains
     * (in)direct subtypes of the given type, but  the class hierarchy is not
     * complete. I.e., not all class files (libraries) used by the project are analyzed.
     * A second case is that some class files are generated at runtime that inherit from
     * the given `ClassType`.
     *
     * @note    No explicit `isKnown` check is required.
     * @param   classType Some `ClassType`.
     */
    def hasSubtypes(classType: ClassType): Answer = {
        val cid = classType.id
        if (isUnknown(cid)) {
            Unknown
        } else {
            Answer(subclassTypesMap(cid).nonEmpty || subinterfaceTypesMap(cid).nonEmpty)
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
     * @param   classType An `ClassType`.
     * @param   reflexive If `true` the given type is also included in the returned
     *          set.
     * @return  The set of all direct and indirect subtypes of the given type.
     */
    def allSubtypes(classType: ClassType, reflexive: Boolean): Set[ClassType] = {
        val cid = classType.id
        if (isUnknown(cid))
            return if (reflexive) UIDSet1(classType) else UIDSet.empty

        if (reflexive)
            subtypeInformationMap(cid).allTypes + classType
        else
            subtypeInformationMap(cid).allTypes
    }

    def allSubtypesIterator(classType: ClassType, reflexive: Boolean): Iterator[ClassType] = {
        val cid = classType.id
        if (isUnknown(cid))
            return if (reflexive) Iterator(classType) else Iterator.empty;

        if (reflexive)
            subtypeInformationMap(cid).iterator ++ Iterator(classType)
        else
            subtypeInformationMap(cid).iterator
    }

    def allSubtypesForeachIterator(
        classType: ClassType,
        reflexive: Boolean
    ): ForeachRefIterator[ClassType] = {
        val cid = classType.id
        if (isUnknown(cid))
            return {
                if (reflexive)
                    ForeachRefIterator.single(classType)
                else
                    ForeachRefIterator.empty
            };

        if (reflexive)
            subtypeInformationMap(cid).foreachIterator ++ ForeachRefIterator.single(classType)
        else
            subtypeInformationMap(cid).foreachIterator
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
     *        f: (T, ClassType) => (T /*result*/ , Boolean /*skip subtypes*/ , Boolean /*abort*/ )
     *        }}}
     */
    def processSubtypes[@specialized(Boolean) T](
        classType: ClassType,
        reflexive: Boolean = false
    )(
        initial: T
    )(
        f: (T, ClassType) => (T /*result*/, Boolean /*skip subtypes*/, Boolean /*abort*/ )
    ): T = {
        if (isUnknown(classType))
            return initial;

        var processed = UIDSet.empty[ClassType]

        def forallSubtypes(initial: T, classType: ClassType): (T, Boolean /*continue*/ ) = {
            val cid = classType.id
            var t: T = initial
            val continue = {
                subclassTypesMap(cid).forall { subtype =>
                    val (newT, continue) = process(t, subtype); t = newT; continue
                } &&
                subinterfaceTypesMap(cid).forall { subtype =>
                    val (newT, continue) = process(t, subtype); t = newT; continue
                }
            }
            (t, continue)
        }

        def process(t: T, classType: ClassType): (T, Boolean /*continue*/ ) = {
            if (processed.contains(classType))
                return (t, true);

            processed += classType

            val (newT, skipSubtypes, abort) = f(t, classType)
            if (abort) (newT, false)
            else if (skipSubtypes) (newT, true)
            else forallSubtypes(newT, classType)
        }

        val (newResult, _) = {
            if (reflexive) {
                process(initial, classType)
            } else {
                forallSubtypes(initial, classType)
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
     * @param   classType An `ClassType`.
     */
    def foreachSubtype(classType: ClassType)(f: ClassType => Unit): Unit = {
        val cid = classType.id
        if (isKnown(cid)) {
            subtypeInformationMap(cid).foreach(f)
        }
    }

    /**
     * Iterates over all subtypes of the given type, by first iterating over the subclass types
     * and then iterating over the subinterface types (if the given class type defines an
     * interface or identifies `java.lang.Object`).
     *
     * @param    process The process function will be called for each subtype of the given type.
     *           If process returns false, subtypes of the current type will no longer be traversed.
     *           However, if a subtype of the current type is reachable via another path (by means
     *           of interface inheritance) then that subtype may be processed.
     *
     * @note    Classes are always traversed first.
     */
    def foreachSubtype(
        classType: ClassType,
        reflexive: Boolean = false
    )(
        process: ClassType => Boolean
    ): Unit = {
        var processed = UIDSet.empty[ClassType]
        def foreachSubtype(classType: ClassType): Unit = {
            if (processed.contains(classType))
                return;

            processed += classType

            if (process(classType)) {
                val cid = classType.id
                subclassTypesMap(cid) foreach { foreachSubtype }
                subinterfaceTypesMap(cid) foreach { foreachSubtype }
            }
        }

        if (classType == ClassType.Object) {
            if (reflexive) {
                if (!process(ClassType.Object))
                    return;
            };

            rootTypes foreach { rootType =>
                if (rootType ne ClassType.Object) {
                    foreachSubtype(rootType)
                } else {
                    // java.lang.Object is always known ...
                    subclassTypesMap(ClassType.ObjectId) foreach { foreachSubtype }
                    subinterfaceTypesMap(ClassType.ObjectId) foreach { foreachSubtype }
                }
            }

            return;
        }

        if (isUnknown(classType))
            return;

        if (reflexive)
            foreachSubtype(classType)
        else {
            val cid = classType.id
            subclassTypesMap(cid) foreach { foreachSubtype }
            subinterfaceTypesMap(cid) foreach { foreachSubtype }
        }
    }

    def foreachSubtypeCF(
        classType: ClassType,
        reflexive: Boolean = false
    )(
        process: ClassFile => Boolean
    )(
        implicit project: ClassFileRepository
    ): Unit = {
        foreachSubtype(classType, reflexive) { subtype =>
            project.classFile(subtype) match {
                case Some(classFile) => process(classFile)
                case _ /* None */    => true
            }
        }
    }

    /**
     * Executes the given function `f` for each subclass of the given `ClassType`.
     * In this case the subclass relation is '''not reflexive'''. Furthermore, it may be
     * possible that f is invoked multiple times using the same `ClassFile` object if
     * the given classType identifies an interface.
     *
     * Subtypes for which no `ClassFile` object is available are ignored.
     *
     * @note    No explicit `isKnown` check is required; if the type is unknown nothing
     *          will happen.
     * @note    For details regarding incomplete class hierarchies see `foreachSubtype`.
     */
    def foreachSubclass(
        classType: ClassType,
        project:   ClassFileRepository
    )(
        f: ClassFile => Unit
    ): Unit = {
        foreachSubtype(classType) { subtype => project.classFile(subtype).foreach(f) }
    }

    /**
     * Returns all (direct and indirect) subclass types of the given class type.
     *
     * @note    No explicit `isKnown` check is required; if the type is unknown, an empty
     *          iterator is returned.
     */
    def allSubclassTypes(classType: ClassType, reflexive: Boolean): Iterator[ClassType] = {
        val cid = classType.id
        if (isUnknown(cid))
            return Iterator.empty;

        val subclassTypesIterator = subtypeInformationMap(cid).classTypes.iterator
        if (reflexive)
            Iterator(classType) ++ subclassTypesIterator
        else
            subclassTypesIterator
    }

    /**
     * Executes the given function `f` for each known direct subclass of the given `ClassType`.
     * In this case the subclass relation is '''not reflexive''' and interfaces inheriting from
     * the given class type are ignored.
     *
     * Subtypes for which no `ClassFile` object is available are ignored.
     *
     * @note    No explicit `isKnown` check is required; if the type is unknown nothing
     *          will happen.
     */
    def foreachDirectSubclassType[T](
        classType: ClassType,
        project:   ClassFileRepository
    )(
        f: ClassFile => T
    ): Unit = {
        val cid = classType.id
        if (isUnknown(cid))
            return;

        import project.classFile
        subclassTypesMap(cid) foreach { subtype => classFile(subtype).foreach(f) }
    }

    def directSubtypesCount(classType: ClassType): Int = {
        directSubtypesCount(classType.id)
    }

    def directSubtypesCount(classTypeId: Int): Int = {
        if (isUnknown(classTypeId))
            return 0;

        subclassTypesMap(classTypeId).size + subinterfaceTypesMap(classTypeId).size
    }

    /**
     * Tests if a subtype of the given `ClassType` exists that satisfies the given predicate.
     * In this case the subtype relation is '''not reflexive'''.
     *
     * Subtypes for which no `ClassFile` object is available are ignored.
     *
     * @note    No explicit `isKnown` check is required; if the type is unknown nothing
     *          will happen.
     */
    def existsSubclass(
        classType: ClassType,
        project:   ClassFileRepository
    )(
        p: ClassFile => Boolean
    ): Boolean = boundary {
        foreachSubtype(classType) { classType =>
            val cfOption = project.classFile(classType) // IMPROVE implement Project.classFile(ObjectTypeID:Int)
            if (cfOption.isDefined && p(cfOption.get))
                break(true);
        }
        false
    }

    def foreachDirectSupertypeCF[U](
        classType: ClassType
    )(
        f: ClassFile => U
    )(
        implicit project: ClassFileRepository
    ): Unit = {
        val cid = classType.id
        if (isUnknown(cid))
            return;

        val superinterfaceTypes = superinterfaceTypesMap(cid)
        if (superinterfaceTypes ne null) {
            superinterfaceTypes foreach { t => project.classFile(t).foreach(f) }
        }

        val superclassType = superclassTypeMap(cid)
        if (superclassType ne null) project.classFile(superclassType).foreach(f)

    }

    def foreachDirectSupertype(classType: ClassType)(f: ClassType => Unit): Unit = {
        if (isUnknown(classType))
            return;

        val cid = classType.id
        val superinterfaceTypes = superinterfaceTypesMap(cid)
        if (superinterfaceTypes ne null) superinterfaceTypes.foreach(f)
        val superclassType = superclassTypeMap(cid)
        if (superclassType ne null) f(superclassType)
    }

    /**
     * Calls the given function `f` for each of the given type's supertypes.
     */
    def foreachSupertype(
        ct:        ClassType,
        reflexive: Boolean = false
    )(
        f: ClassType => Unit
    ): Unit = {
        val cid = ct.id
        if (reflexive) f(ct)
        if (isKnown(cid)) {
            supertypeInformationMap(cid).foreach(f)
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
        classType: ClassType
    ): QualifiedCollection[List[ClassType]] = {
        val cid = classType.id

        if (cid == ClassType.ObjectId)
            return CompleteCollection(List());

        if (isUnknown(cid))
            return IncompleteCollection(List());

        var allTypes: List[ClassType] = List.empty

        val superclassTypeMap = this.superclassTypeMap
        var superclassType = superclassTypeMap(cid)
        while (superclassType ne null) {
            allTypes ::= superclassType
            superclassType = superclassTypeMap(superclassType.id)
        }
        if (allTypes.head eq ClassType.Object)
            CompleteCollection(allTypes)
        else
            IncompleteCollection(allTypes)
    }

    def directSupertypes(classType: ClassType): UIDSet[ClassType] = {
        val cid = classType.id
        if (cid == ClassType.ObjectId || isUnknown(cid)) {
            UIDSet.empty
        } else {
            val superinterfaceTypes: UIDSet[ClassType] = {
                val superinterfaceTypes = superinterfaceTypesMap(cid)
                if (superinterfaceTypes ne null)
                    superinterfaceTypes
                else
                    UIDSet.empty
            }
            val superclassType = superclassTypeMap(cid)
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
    def allSupertypes(classType: ClassType, reflexive: Boolean = false): UIDSet[ClassType] = {
        val cid = classType.id
        if (isUnknown(cid))
            return UIDSet.empty;

        var supertypeInformation = supertypeInformationMap(cid)
        if (supertypeInformation == null) {
            // The following is thread-safe, because we will always compute the same
            // information!
            // This happens ONLY in case of broken projects where
            // the sub-supertype information is totally broken;
            // e.g., a sub type `extends C` but C is an interface.
            supertypeInformation = interpolateSupertypeInformation(classType)
            supertypeInformationMap(cid) = supertypeInformation
        }
        val ts = supertypeInformation.allTypes
        if (reflexive)
            ts + classType
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
        classType: ClassType,
        reflexive: Boolean = false
    ): UIDSet[ClassType] = {
        val cid = classType.id
        if (isUnknown(cid))
            return UIDSet.empty

        var supertypeInformation = supertypeInformationMap(cid)
        if (supertypeInformation == null) {
            // The following is thread-safe, because we will always compute the same information!
            // This happens ONLY in case of broken projects where the sub-supertype information is
            // totally broken; e.g., a sub type `extends C` but C is an interface.
            supertypeInformation = interpolateSupertypeInformation(classType)
            supertypeInformationMap(cid) = supertypeInformation
        }
        val superinterfacetypes = supertypeInformation.interfaceTypes
        if (reflexive && isInterfaceTypeMap(cid))
            superinterfacetypes + classType
        else
            superinterfacetypes
    }

    private def interpolateSupertypeInformation(ct: ClassType): SupertypeInformation = {
        val allClassTypes = UIDSet.empty[ClassType] ++ allSuperclassTypesInInitializationOrder(ct).s

        var allInterfaceTypes = UIDSet.empty[ClassType]
        foreachSuperinterfaceType(ct) { supertype =>
            allInterfaceTypes += supertype; true
        }

        SupertypeInformation.forSubtypesOfObject(
            isKnownTypeMap,
            isInterfaceTypeMap,
            allClassTypes,
            allInterfaceTypes,
            UIDSet.empty
        )
    }

    /**
     * Calls the function `f` for each supertype of the given class type for
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
        classType: ClassType,
        project:   ClassFileRepository
    )(
        f: ClassFile => Unit
    ): Unit = {
        foreachSupertype(classType) { supertype => project.classFile(supertype).foreach(f) }
    }

    /**
     * Returns the set of all classes/interfaces from which the given type inherits
     * and for which the respective class file is available.
     *
     * @return  An `Iterable` over all class files of all super types of the given
     *          `classType` that pass the given filter and for which the class file
     *          is available.
     * @note    It may be more efficient to use `foreachSuperclass(ClassType,
     *          ClassType => Option[ClassFile])(ClassFile => Unit)`
     */
    def superclasses(
        classType: ClassType,
        project:   ClassFileRepository
    )(
        classFileFilter: ClassFile => Boolean = { _ => true }
    ): Iterable[ClassFile] = {
        // We want to make sure that every class file is returned only once,
        // but we want to avoid equals calls on `ClassFile` objects.
        var classFiles = Map[ClassType, ClassFile]()
        foreachSuperclass(classType, project) { classFile =>
            if (classFileFilter(classFile))
                classFiles = classFiles.updated(classFile.thisType, classFile)
        }
        classFiles.values
    }

    /**
     * Efficient, best-effort iterator over all super types of the given type.
     */
    def allSuperclassesIterator(
        ct:        ClassType,
        reflexive: Boolean = false
    )(
        implicit project: ClassFileRepository
    ): Iterator[ClassFile] = {
        val cid = ct.id

        val baseTypes = if (isKnown(cid)) {
            supertypeInformationMap(cid).iterator
        } else {
            Iterator.empty
        }

        val allTypes = if (reflexive) baseTypes ++ Iterator(ct) else baseTypes

        allTypes
            .filter(t => project.classFile(t).isDefined)
            .map(t => project.classFile(t).get)
    }

    /**
     * Returns `Some(<SUPERTYPES>)` if this type is known and information about the
     * supertypes is available. I.e., if this type is not known, `None` is returned;
     * if the given type's superinterfaces are known (even if this class does not
     * implement (directly or indirectly) any interface) `Some(UIDSet(<CLASSTYPES>))` is
     * returned.
     */
    // TODO Rename => directSuperinterfacetypes
    def superinterfaceTypes(classType: ClassType): Option[UIDSet[ClassType]] = {
        val cid = classType.id
        if (isUnknown(cid))
            return None;

        val superinterfaceTypes = superinterfaceTypesMap(cid)
        if (superinterfaceTypes ne null)
            Some(superinterfaceTypes)
        else
            None
    }

    /**
     * Returns the immediate superclass of the given class type, if the given
     * type is known and if it has a superclass. I.e., in case of `java.lang.Object` None is
     * returned.
     */
    def superclassType(classType: ClassType): Option[ClassType] = {
        val cid = classType.id
        if (isUnknown(cid))
            return None;

        val superclassType = superclassTypeMap(cid)
        if (superclassType ne null)
            Some(superclassType)
        else
            None
    }

    /**
     * Returns the supertype of the class type identified by the given class type id or `null`
     * if the type is unknown or if the type has no supertype.
     */
    def superclassType(classTypeId: Int): ClassType = {
        if (isKnown(classTypeId))
            superclassTypeMap(classTypeId) // may also be null
        else
            null
    }

    // TODO Rename => directSupertypes
    def supertypes(classType: ClassType): UIDSet[ClassType] = {
        superinterfaceTypes(classType) match {
            case None =>
                superclassType(classType).map(UIDSet1.apply).getOrElse(UIDSet.empty)
            case Some(superinterfaceTypes) =>
                superinterfaceTypes ++ superclassType(classType)
        }
    }

    def foreachDirectSubtypeOf[U](classType: ClassType)(f: ClassType => U): Unit = {
        val cid = classType.id
        if (isUnknown(cid))
            return;

        this.subclassTypesMap(cid).foreach(f)
        this.subinterfaceTypesMap(cid).foreach(f)
    }

    /**
     * The direct subtypes of the given type (not reflexive).
     */
    def directSubtypesOf(classType: ClassType): Iterator[ClassType] = {
        val cid = classType.id
        if (isUnknown(cid))
            return Iterator.empty;

        this.subclassTypesMap(cid).iterator ++ this.subinterfaceTypesMap(cid).iterator
    }

    def directSubclassesOf(classType: ClassType): UIDSet[ClassType] = {
        val cid = classType.id
        if (isUnknown(cid))
            return UIDSet.empty;

        this.subclassTypesMap(cid)
    }

    def directSubinterfacesOf(classType: ClassType): UIDSet[ClassType] = {
        val cid = classType.id
        if (isUnknown(cid))
            return UIDSet.empty;

        this.subinterfaceTypesMap(cid)
    }

    def directSuperinterfacesOf(classType: ClassType): UIDSet[ClassType] = {
        val cid = classType.id
        if (isUnknown(cid))
            return UIDSet.empty;

        this.superinterfaceTypesMap(cid)
    }

    /**
     * Iterates over all subinterfaces of the given interface type (or java.lang.Object) until
     * the callback function returns "false".
     */
    def foreachSubinterfaceType(interfaceType: ClassType)(f: ClassType => Boolean): Unit = {
        var processedTypes = UIDSet.empty[ClassType]
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
    def foreachSuperinterfaceType(t: ClassType)(f: ClassType => Boolean): Unit = {
        if (isUnknown(t))
            return;

        var processedTypes = UIDSet.empty[ClassType]
        var typesToProcess = directSuperinterfacesOf(t) ++ superclassType(t)
        while (typesToProcess.nonEmpty) {
            val superType = typesToProcess.head
            typesToProcess = typesToProcess.tail
            processedTypes += superType
            if (!isInterface(superType.id) || f(superType)) {
                val superInterfaces: Iterator[ClassType] =
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

        if (subtype.isClassType) {
            if (supertype.isClassType)
                isSubtypeOf(subtype.asClassType, supertype.asClassType)
            else
                // the supertype is an array type..
                false
        } else {
            // the subtype is an array type
            if (supertype.isClassType) {
                (supertype eq ClassType.Object) ||
                (supertype eq ClassType.Serializable) || (supertype eq ClassType.Cloneable)
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
    def isSubtypeOf(subtype: ClassType, theSupertype: ClassType): Boolean = {
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
        subtypes:   UIDSet[? <: ReferenceType],
        supertypes: UIDSet[? <: ReferenceType]
    ): Boolean = {
        if (subtypes.isEmpty /*the upper type bound of "null" values*/ || subtypes == supertypes)
            return true;

        supertypes forall { (supertype: ReferenceType) =>
            subtypes exists { (subtype: ReferenceType) => this.isSubtypeOf(subtype, supertype) }
        }
    }

    /**
     * Returns `true` if the subtype is a subtype of '''all''' given supertypes. Hence,
     * supertypes should not contain more than one class type.
     */
    def isSubtypeOf(subtype: ReferenceType, supertypes: UIDSet[? <: ReferenceType]): Boolean = {
        if (supertypes.isEmpty /*the upper type bound of "null" values*/ )
            return false;

        supertypes forall { (supertype: ReferenceType) => isSubtypeOf(subtype, supertype) }
    }

    def isSubtypeOf(subtypes: UIDSet[? <: ReferenceType], supertype: ReferenceType): Boolean = {
        if (subtypes.isEmpty) /*the upper type bound of "null" values*/
            return true;

        subtypes exists { (subtype: ReferenceType) => this.isSubtypeOf(subtype, supertype) }
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
            } else /*arrayType.componentType.isClassType*/ {
                val componentClassType = arrayType.componentType.asClassType
                // Recall that `isSubtypeOf` completely handles all cases that make
                // it possible to store an array in a value of type ClassType.
                isASubtypeOf(elementValueType.asArrayType, componentClassType) match {
                    case Yes => if (arrayTypeIsPrecise) Yes else Unknown
                    case No  => No
                    case _   => throw new AssertionError("some array type <: some class type failed")
                }
            }
        } else /* the type of the element value is a ClassType*/ {
            if (arrayType.elementType.isBaseType) {
                No
            } else {
                val elementValueClassType = elementValueType.asClassType
                val arrayComponentReferenceType = arrayType.componentType.asReferenceType
                isASubtypeOf(elementValueClassType, arrayComponentReferenceType) match {
                    case Yes =>
                        if (arrayTypeIsPrecise || isKnownToBeFinal(elementValueClassType))
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
     * @param   subtype Any `ClassType`.
     * @param   theSupertype Any `ClassType`.
     * @return  `Yes` if `subtype` is a subtype of the given `supertype`. `No`
     *          if `subtype` is not a subtype of `supertype` and `Unknown` if the analysis is
     *          not conclusive. The latter can happen if the class hierarchy is not
     *          complete and hence precise information about a type's supertypes
     *          is not available.
     */
    def isASubtypeOf(subtype: ClassType, theSupertype: ClassType): Answer = {
        if (subtype eq theSupertype)
            return Yes;

        val Object = ClassType.Object
        if (theSupertype eq Object)
            return Yes;

        if (subtype eq Object /* && theSupertype != ClassType.Object is already handled */ )
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

        if (subtype.isClassType) {
            if (supertype.isArrayType)
                No
            else
                // The analysis is conclusive iff we can get all supertypes
                // for the given type (ct) up until "java/lang/Object"; i.e.,
                // if there are no holes.
                isASubtypeOf(subtype.asClassType, supertype.asClassType)
        } else {
            // ... subtype is an ArrayType
            if (supertype.isClassType) {
                Answer(
                    (supertype eq ClassType.Serializable) || (supertype eq ClassType.Cloneable)
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
        subtypes:   UIDSet[? <: ReferenceType],
        supertypes: UIDSet[? <: ReferenceType]
    ): Answer = boundary {
        if (subtypes.isEmpty /* <=> upper type bound of "null" values */ || subtypes == supertypes)
            return Yes;

        Answer(
            supertypes forall { (supertype: ReferenceType) =>
                var subtypingRelationUnknown = false
                val subtypeExists =
                    subtypes exists { (subtype: ReferenceType) =>
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
                    break(Unknown);
                else
                    false
            }
        )
    }

    /**
     * Returns `Yes` if the subtype is a subtype of '''all''' given supertypes. Hence,
     * supertypes should not contain more than one class type.
     */
    def isASubtypeOf(subtype: ReferenceType, supertypes: UIDSet[? <: ReferenceType]): Answer = boundary {
        if (supertypes.isEmpty /* <=> upper type bound of "null" values */ )
            return No;

        supertypes foreach { (supertype: ReferenceType) =>
            isASubtypeOf(subtype, supertype) match {
                case Yes     => /*Nothing to do*/
                case Unknown => break(Unknown); // FIXME No should have precedence over Unknown even if some supertypes are Unknown...
                case No      => break(No);
            }
        }
        // subtype is a subtype of all supertypes
        Yes
    }

    def isASubtypeOf(subtypes: UIDSet[? <: ReferenceType], supertype: ReferenceType): Answer = {
        if (subtypes.isEmpty) /* <=> upper type bound of "null" values */
            return Yes;

        var subtypeRelationUnknown = false
        val subtypeExists =
            subtypes exists { (subtype: ReferenceType) =>
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
    def directSubtypesOf(upperTypeBound: UIDSet[ClassType]): UIDSet[ClassType] = {
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

        var directSubtypes = UIDSet.empty[ClassType]
        var processedTypes = UIDSet.empty[ClassType]
        val typesToProcess = new mutable.Queue ++= directSubtypesOf(firstType)
        while (typesToProcess.nonEmpty) {
            val candidateType = typesToProcess.dequeue()
            processedTypes += candidateType
            val isCommonSubtype =
                remainingTypeBounds.forall { (otherTypeBound: ClassType) =>
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
     * we have to check two different things. First compare the [[ClassType]]s, if they are equal
     * we still have to care about the [[TypeArgument]]s since we are dealing with generics.
     */
    private def isASubtypeOfByTypeArgument(
        subtype:   TypeArgument,
        supertype: TypeArgument
    )(
        implicit p: ClassFileRepository
    ): Answer = {
        (subtype, supertype) match {
            case (ConcreteTypeArgument(et), ConcreteTypeArgument(superEt))                  => Answer(et eq superEt)
            case (ConcreteTypeArgument(et), UpperTypeBound(superEt))                        => isASubtypeOf(et, superEt)
            case (ConcreteTypeArgument(et), LowerTypeBound(superEt))                        => isASubtypeOf(superEt, et)
            case (_, Wildcard)                                                              => Yes
            case (GenericTypeArgument(varInd, cts), GenericTypeArgument(supVarInd, supCts)) =>
                (varInd, supVarInd) match {
                    case (None, None) =>
                        if (cts.classType eq supCts.classType) isASubtypeOf(cts, supCts) else No
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

    @inline @tailrec private final def compareTypeArguments(
        subtypeArgs:   List[TypeArgument],
        supertypeArgs: List[TypeArgument]
    )(
        implicit p: ClassFileRepository
    ): Answer = {

        (subtypeArgs, supertypeArgs) match {
            case (Nil, Nil)                       => Yes
            case (Nil, _) | (_, Nil)              => No
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
     *  supertype: List as [[ClassType]]
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
        supertype: ClassType
    )(
        implicit project: ClassFileRepository
    ): Option[ClassTypeSignature] = boundary {
        val signaturesToCheck = subtype.superClassSignature :: subtype.superInterfacesSignature
        for {
            cts <- signaturesToCheck if cts.classType eq supertype
        } { break(Some(cts)); }

        for {
            cts <- signaturesToCheck
            superCs <- getClassSignature(cts.classType)
            matchingType <- getSupertypeDeclaration(superCs, supertype)
        } { break(Some(matchingType)); }

        None
    }

    /**
     * Returns the class type's class signature if the class files is available and
     * a class signature is defined.
     */
    @inline private final def getClassSignature(
        ct: ClassType
    )(
        implicit p: ClassFileRepository
    ): Option[ClassSignature] = {
        p.classFile(ct).flatMap(cf => cf.classSignature)
    }

    /**
     * Determines if the given class or interface type encoded by the
     * [[ClassTypeSignature]] `subtype` is actually a subtype
     * of the class or interface type encoded in the [[ClassTypeSignature]] of the
     * `supertype`.
     *
     * @note This method relies – in case of a comparison of non generic types – on
     *       `isSubtypeOf(org.opalj.br.ClassType,org.opalj.br.ClassType)` of `Project` which
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
        implicit project: ClassFileRepository
    ): Answer = boundary {
        def compareTypeArgumentsOfClassSuffixes(
            suffix:      List[SimpleClassTypeSignature],
            superSuffix: List[SimpleClassTypeSignature]
        ): Answer = {
            if (suffix.isEmpty && superSuffix.isEmpty)
                return Yes;

            suffix.zip(superSuffix).foldLeft(Yes: Answer)((acc, value) =>
                (acc, compareTypeArguments(value._1.typeArguments, value._2.typeArguments)) match {
                    case (_, Unknown)     => break(Unknown);
                    case (x, y) if x ne y => No
                    case (x, _ /*x*/ )    => x
                }
            )
        }
        if (subtype.classType eq supertype.classType) {
            (subtype, supertype) match {
                case (ConcreteType(_), ConcreteType(_)) =>
                    Yes

                case (GenericType(_, _), ConcreteType(_)) =>
                    isASubtypeOf(subtype.classType, supertype.classType)

                case (GenericType(_, elements), GenericType(_, superElements)) =>
                    compareTypeArguments(elements, superElements)

                case (
                        GenericTypeWithClassSuffix(_, elements, suffix),
                        GenericTypeWithClassSuffix(_, superElements, superSuffix)
                    ) => {
                    compareTypeArguments(elements, superElements) match {
                        case Yes    => compareTypeArgumentsOfClassSuffixes(suffix, superSuffix)
                        case answer => answer
                    }
                }

                case _ => No
            }
        } else {
            val isASubtype = isASubtypeOf(subtype.classType, supertype.classType)
            if (isASubtype.isYes) {

                def haveSameTypeBinding(
                    subtype:            ClassType,
                    supertype:          ClassType,
                    supertypeArguments: List[TypeArgument],
                    isInnerClass:       Boolean = false
                ): Answer = {
                    getClassSignature(subtype).map { cs =>
                        getSupertypeDeclaration(cs, supertype).map { matchingType =>
                            val classSuffix = matchingType.classTypeSignatureSuffix
                            if (isInnerClass && classSuffix.nonEmpty)
                                compareTypeArguments(classSuffix.last.typeArguments, supertypeArguments)
                            else
                                compareTypeArguments(
                                    matchingType.simpleClassTypeSignature.typeArguments,
                                    supertypeArguments
                                )
                        } getOrElse No
                    } getOrElse Unknown
                }
                def compareSharedTypeArguments(
                    subtype:            ClassType,
                    typeArguments:      List[TypeArgument],
                    supertype:          ClassType,
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
                        haveSameTypeBinding(subtype.classType, supertype.classType, supertypeArguments)

                    case (GenericType(containerType, elements), GenericType(superContainerType, superElements)) =>
                        compareSharedTypeArguments(containerType, elements, superContainerType, superElements)

                    case (GenericTypeWithClassSuffix(_, _, _), ConcreteType(_)) => Yes

                    case (
                            GenericTypeWithClassSuffix(containerType, elements, _),
                            GenericType(superContainerType, superElements)
                        ) =>
                        compareSharedTypeArguments(containerType, elements, superContainerType, superElements)

                    case (
                            GenericTypeWithClassSuffix(containerType, _ /*typeArguments*/, suffix),
                            GenericTypeWithClassSuffix(superContainerType, _ /*supertypeArguments*/, superSuffix)
                        ) => {
                        compareSharedTypeArguments(
                            containerType,
                            subtype.classTypeSignatureSuffix.last.typeArguments,
                            superContainerType,
                            supertype.classTypeSignatureSuffix.last.typeArguments
                        ) match {
                            case Yes =>
                                compareTypeArgumentsOfClassSuffixes(suffix.dropRight(1), superSuffix.dropRight(1)) match {
                                    case Yes
                                        if suffix.last.typeArguments.isEmpty && superSuffix.last.typeArguments.isEmpty =>
                                        Yes
                                    case Yes
                                        if suffix.last.typeArguments.isEmpty && superSuffix.last.typeArguments.nonEmpty => {
                                        val ss = getClassSignature(containerType).flatMap { cs =>
                                            getSupertypeDeclaration(cs, superContainerType)
                                        }
                                        if (ss.get.classTypeSignatureSuffix.last.typeArguments.collectFirst {
                                                case x @ ProperTypeArgument(_, TypeVariableSignature(_)) => x
                                            }.size > 0
                                        )
                                            compareTypeArgumentsOfClassSuffixes(
                                                List(subtype.simpleClassTypeSignature),
                                                List(superSuffix.last)
                                            )
                                        else compareTypeArgumentsOfClassSuffixes(
                                            List(ss.get.classTypeSignatureSuffix.last),
                                            List(superSuffix.last)
                                        )
                                    }
                                    case Yes =>
                                        compareTypeArgumentsOfClassSuffixes(List(suffix.last), List(superSuffix.last))
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
     */
    def isASubtypeOf(
        subtype:   ClassTypeSignature,
        supertype: FormalTypeParameter
    )(
        implicit p: ClassFileRepository
    ): Answer = boundary {
        // IMPROVE Avoid creating the list by using an inner function (def).
        (supertype.classBound.toList ++ supertype.interfaceBound)
            .collect { case s: ClassTypeSignature => s }
            .foldLeft(Yes: Answer) { (a, superCTS) =>
                (a, isASubtypeOf(subtype, superCTS)) match {
                    case (_, Unknown)     => break(Unknown);
                    case (x, y) if x ne y => No
                    case (x, _ /*x*/ )    => x
                }
            }
    }

    /**
     * Returns some statistical data about the class hierarchy.
     */
    def statistics: String = {
        "Class Hierarchy Statistics:" +
            "\n\tKnown types: " + knownTypesMap.count(_ != null) +
            "\n\tInterface types: " + isInterfaceTypeMap.count(isInterface => isInterface) +
            "\n\tIdentified Superclasses: " + superclassTypeMap.count(_ != null) +
            "\n\tSuperinterfaces: " +
            superinterfaceTypesMap.filter(_ != null).foldLeft(0)(_ + _.size) +
            "\n\tSubclasses: " +
            subclassTypesMap.filter(_ != null).foldLeft(0)(_ + _.size) +
            "\n\tSubinterfaces: " +
            subinterfaceTypesMap.filter(_ != null).foldLeft(0)(_ + _.size)
    }

    /**
     * Returns a view of the class hierarchy as a graph (which can then be transformed
     * into a dot representation [[http://www.graphviz.org Graphviz]]). This
     * graph can be a multi-graph if the class hierarchy contains holes.
     */
    def toGraph(): Node = new Node {

        private val nodes: mutable.Map[ClassType, Node] = {
            val nodes = mutable.HashMap.empty[ClassType, Node]

            foreachNonNullValue(knownTypesMap) { (id, aType) =>
                val entry: (ClassType, Node) = (
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
    def allSupertypesOf(types: UIDSet[ClassType], reflexive: Boolean): UIDSet[ClassType] = {
        var allSupertypesOf: UIDSet[ClassType] = UIDSet.empty
        types foreach { (t: ClassType) =>
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
     *          condition. If `types` is empty, the returned leaf type is `ClassType.Object`.
     *          which should always be a safe fallback.
     */
    def leafTypes(types: UIDSet[ClassType]): UIDSet[ClassType] = {
        if (types.isEmpty)
            return ClassHierarchy.JustObject;

        if (types.isSingletonSet)
            return types;

        types filter { aType =>
            isUnknown(aType) ||
            // !(directSubtypesOf(aType) exists { t => types.contains(t) })
            !(types exists { t => (t ne aType) && isSubtypeOf(t, aType) })
        }
    }

    /**
     * Calculates the most specific common supertype of the given types.
     * If `reflexive` is `false`, no two types across both sets have to be in
     * an inheritance relation; if in doubt use `true`.
     *
     * @param upperTypeBoundsB A list (set) of `ClassType`s that are not in an
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
        upperTypeBoundsA: UIDSet[ClassType],
        upperTypeBoundsB: UIDSet[ClassType],
        reflexive:        Boolean
    ): UIDSet[ClassType] = {

        assert(upperTypeBoundsA.nonEmpty)
        assert(upperTypeBoundsB.nonEmpty)

        upperTypeBoundsA.compare(upperTypeBoundsB) match {
            case StrictSubset     => upperTypeBoundsA
            case EqualSets        => upperTypeBoundsA /* or upperTypeBoundsB */
            case StrictSuperset   => upperTypeBoundsB
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
     * @param upperTypeBoundB A list (set) of `ClassType`s that are not in a mutual
     *      inheritance relation.
     * @return (I) Returns (if reflexive is `true`) `upperTypeBoundA` if it is a supertype
     *      of at least one type of `upperTypeBoundB`.
     *      (II) Returns `upperTypeBoundB` if `upperTypeBoundA` is
     *      a subtype of all types of `upperTypeBoundB`. Otherwise a new upper type
     *      bound is calculated and returned.
     */
    def joinClassTypes(
        upperTypeBoundA: ClassType,
        upperTypeBoundB: UIDSet[ClassType],
        reflexive:       Boolean
    ): UIDSet[ClassType] = {

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
                    joinClassTypes(upperTypeBoundA, upperTypeBoundB.head, reflexive)
                }
            return upperTypeBound;
        }

        if (upperTypeBoundB.contains(upperTypeBoundA)) {
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
                "project configuration - class hierarchy",
                "type unknown: " + upperTypeBoundA.toJava
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
        upperTypeBoundB: UIDSet[? <: ReferenceType]
    ): UIDSet[? <: ReferenceType] = {
        upperTypeBoundB match {
            case UIDSet1(utbB: ArrayType) =>
                if (utbB eq upperTypeBoundA)
                    return upperTypeBoundB;
                else
                    joinArrayTypes(upperTypeBoundA, utbB) match {
                        case Left(newUTB)  => new UIDSet1(newUTB)
                        case Right(newUTB) => newUTB
                    }
            case UIDSet1(utbB: ClassType) =>
                joinAnyArrayTypeWithClassType(utbB)
            case _ =>
                val utbB = upperTypeBoundB.asInstanceOf[UIDSet[ClassType]]
                joinAnyArrayTypeWithMultipleTypesBound(utbB)
        }
    }

    def joinReferenceType(
        upperTypeBoundA: ReferenceType,
        upperTypeBoundB: UIDSet[? <: ReferenceType]
    ): UIDSet[? <: ReferenceType] = {
        if (upperTypeBoundA.isArrayType)
            joinArrayType(upperTypeBoundA.asArrayType, upperTypeBoundB)
        else
            upperTypeBoundB match {
                case UIDSet1(_: ArrayType) =>
                    joinAnyArrayTypeWithClassType(upperTypeBoundA.asClassType)
                case UIDSet1(utbB: ClassType) =>
                    joinClassTypes(upperTypeBoundA.asClassType, utbB, true)
                case _ =>
                    val utbB = upperTypeBoundB.asInstanceOf[UIDSet[ClassType]]
                    joinClassTypes(upperTypeBoundA.asClassType, utbB, true)
            }
    }

    def joinReferenceTypes(
        upperTypeBoundA: UIDSet[? <: ReferenceType],
        upperTypeBoundB: UIDSet[? <: ReferenceType]
    ): UIDSet[? <: ReferenceType] = {
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
                upperTypeBoundA.asInstanceOf[UIDSet[ClassType]],
                upperTypeBoundB.asInstanceOf[UIDSet[ClassType]],
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
    def joinClassTypes(
        upperTypeBoundA: ClassType,
        upperTypeBoundB: ClassType,
        reflexive:       Boolean
    ): UIDSet[ClassType] = {

        assert(
            reflexive || (
                (upperTypeBoundA ne ClassType.Object) && (upperTypeBoundB ne ClassType.Object)
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
        thatUpperTypeBound: UIDSet[ClassType]
    ): UIDSet[ClassType] = {
        import ClassType.Cloneable
        import ClassType.Serializable
        import ClassType.SerializableAndCloneable

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
    def joinAnyArrayTypeWithClassType(thatUpperTypeBound: ClassType): UIDSet[ClassType] = {
        import ClassType.Cloneable
        import ClassType.Object
        import ClassType.Serializable

        if ((thatUpperTypeBound eq Object) ||
            (thatUpperTypeBound eq Serializable) ||
            (thatUpperTypeBound eq Cloneable)
        )
            new UIDSet1(thatUpperTypeBound)
        else {
            var newUpperTypeBound: UIDSet[ClassType] = UIDSet.empty
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
     *      an `ArrayType` and `Right(UIDList(ClassType.Serializable, ClassType.Cloneable))`
     *      if the two arrays do not have an `ArrayType` as a most specific common supertype.
     */
    def joinArrayTypes(
        thisUpperTypeBound: ArrayType,
        thatUpperTypeBound: ArrayType
    ): Either[ArrayType, UIDSet[ClassType]] = {
        // We have ALSO to consider the following corner cases:
        // Foo[][] and Bar[][] => Object[][] (Object is the common super class)
        // Object[] and int[][] => Object[] (which may contain arrays of int values...)
        // Foo[] and int[][] => Object[]
        // int[] and Object[][] => SerializableAndCloneable

        import ClassType.SerializableAndCloneable

        if (thisUpperTypeBound eq thatUpperTypeBound)
            return Left(thisUpperTypeBound);

        val thisUTBDim = thisUpperTypeBound.dimensions
        val thatUTBDim = thatUpperTypeBound.dimensions

        if (thisUTBDim < thatUTBDim) {
            if (thisUpperTypeBound.elementType.isBaseType) {
                if (thisUTBDim == 1)
                    Right(SerializableAndCloneable)
                else
                    Left(ArrayType(thisUTBDim - 1, ClassType.Object))
            } else {
                Left(ArrayType(thisUTBDim, ClassType.Object))
            }
        } else if (thisUTBDim > thatUTBDim) {
            if (thatUpperTypeBound.elementType.isBaseType) {
                if (thatUTBDim == 1)
                    Right(SerializableAndCloneable)
                else
                    Left(ArrayType(thatUTBDim - 1, ClassType.Object))
            } else {
                Left(ArrayType(thatUTBDim, ClassType.Object))
            }
        } else if (thisUpperTypeBound.elementType.isBaseType ||
                   thatUpperTypeBound.elementType.isBaseType
        ) {
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
                Left(ArrayType(thisUTBDim - 1, ClassType.Object))
        } else {
            // When we reach this point, the dimensions are identical and both
            // elementTypes are reference types
            val thatElementType = thatUpperTypeBound.elementType.asClassType
            val thisElementType = thisUpperTypeBound.elementType.asClassType
            val elementType =
                joinClassTypesUntilSingleUpperBound(thisElementType, thatElementType, true)
            Left(ArrayType(thisUTBDim, elementType))
        }
    }

    def joinClassTypesUntilSingleUpperBound(
        upperTypeBoundA: ClassType,
        upperTypeBoundB: ClassType,
        reflexive:       Boolean
    ): ClassType = {
        val newUpperTypeBound = joinClassTypes(upperTypeBoundA, upperTypeBoundB, reflexive)
        val result =
            if (newUpperTypeBound.isSingletonSet)
                newUpperTypeBound.head
            else
                newUpperTypeBound reduce { (c, n) =>
                    // We are already one level up in the class hierarchy. Hence,
                    // we now certainly want to be reflexive!
                    joinClassTypesUntilSingleUpperBound(c, n, true)
                }
        result
    }

    /**
     * Given an upper type bound '''a''' most specific type that is a common supertype
     * of the given types is determined.
     *
     * @see `joinClassTypesUntilSingleUpperBound(upperTypeBoundA: ClassType,
     *       upperTypeBoundB: ClassType, reflexive: Boolean)` for further details.
     */
    def joinClassTypesUntilSingleUpperBound(upperTypeBound: UIDSet[ClassType]): ClassType = {
        if (upperTypeBound.isSingletonSet)
            upperTypeBound.head
        else
            upperTypeBound reduce { (c, n) => joinClassTypesUntilSingleUpperBound(c, n, true) }
    }

    def joinReferenceTypesUntilSingleUpperBound(
        upperTypeBound: UIDSet[? <: ReferenceType]
    ): ReferenceType = {
        if (upperTypeBound.isSingletonSet)
            upperTypeBound.head
        else
            // Note that the upper type bound must never consist of more than one array type;
            // and that the type hierarchy related to arrays is "hardcoded"
            // ... (here) type erasure also has its benefits ...
            joinClassTypesUntilSingleUpperBound(upperTypeBound.asInstanceOf[UIDSet[ClassType]])
    }

    def joinUpperTypeBounds(
        utbA: UIDSet[? <: ReferenceType],
        utbB: UIDSet[? <: ReferenceType]
    ): UIDSet[? <: ReferenceType] = {
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
                    joinAnyArrayTypeWithClassType(utbB.head.asInstanceOf[ClassType])
                }
            } else {
                joinAnyArrayTypeWithMultipleTypesBound(utbB.asInstanceOf[UIDSet[ClassType]])
            }
        } else if (utbB.isSingletonSet) {
            if (utbB.head.isArrayType) {
                joinAnyArrayTypeWithMultipleTypesBound(utbA.asInstanceOf[UIDSet[ClassType]])
            } else {
                joinClassTypes(
                    utbB.head.asClassType,
                    utbA.asInstanceOf[UIDSet[ClassType]],
                    true
                )
            }
        } else {
            joinUpperTypeBounds(
                utbA.asInstanceOf[UIDSet[ClassType]],
                utbB.asInstanceOf[UIDSet[ClassType]],
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

    private implicit val classHierarchyEC: ExecutionContext = OPALUnboundedExecutionContext

    final val JustObject: UIDSet[ClassType] = new UIDSet1(ClassType.Object)

    /**
     * Creates a `ClassHierarchy` that captures the type hierarchy related to
     * the exceptions thrown by specific Java bytecode instructions as well as
     * fundamental types such as Cloneable and Serializable and also those types
     * related to reflection..
     *
     * This class hierarchy is primarily useful for testing purposes.
     */
    lazy val PreInitializedClassHierarchy: ClassHierarchy = {
        apply(classFiles = Iterable.empty, defaultTypeHierarchyDefinitions())(using GlobalLogContext)
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
        implicit logContext: LogContext
    ): Seq[TypeDeclaration] = process(in) { in =>
        if (in eq null) {
            OPALLogger.error(
                "internal - class hierarchy",
                "loading the predefined class hierarchy failed; " +
                    "make sure that all resources are found in the correct folders and " +
                    "try to rebuild the project using \"sbt copyResources\""
            )
            Seq.empty;
        } else {
            val typeRegExp =
                """(class|interface)\s+(\S+)(\s+extends\s+(\S+)(\s+implements\s+(.+))?)?""".r
            processSource(new BufferedSource(in)) { source =>
                source.getLines().map(_.trim).filterNot { l => l.startsWith("#") || l.length == 0 }.map { l =>
                    val typeRegExp(typeKind, theType, _, superclassType, _, superinterfaceTypes) = l: @unchecked
                    TypeDeclaration(
                        ClassType(theType),
                        typeKind == "interface",
                        Option(superclassType).map(ClassType(_)),
                        Option(superinterfaceTypes).map { superinterfaceTypes =>
                            UIDSet.fromSpecific[ClassType](
                                superinterfaceTypes.split(',').map(t => ClassType(t.trim))
                            )
                        }.getOrElse(UIDSet.empty)
                    )
                }.toList
            }
        }
    }

    def apply(
        classFiles:               Iterable[ClassFile],
        typeHierarchyDefinitions: Seq[() => InputStream] = defaultTypeHierarchyDefinitions()
    )(
        implicit logContext: LogContext
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
        implicit logContext: LogContext
    ): ClassHierarchy = {

        val classTypesCount = ClassType.classTypesCount
        val knownTypesMap = new Array[ClassType](classTypesCount)
        val isInterfaceTypeMap = new Array[Boolean](classTypesCount)
        val superclassTypeMap = new Array[ClassType](classTypesCount)
        val isKnownToBeFinalMap = new Array[Boolean](classTypesCount)
        val superinterfaceTypesMap = new Array[UIDSet[ClassType]](classTypesCount)
        val subclassTypesMap = new Array[UIDSet[ClassType]](classTypesCount)
        val subinterfaceTypesMap = new Array[UIDSet[ClassType]](classTypesCount)

        val ObjectId = ClassType.Object.id

        // Collects those classes which declare to implement a specific interface, but which
        // is actually a regular class.
        var classesWithBrokenInterfaceInheritance: Map[ClassType, UIDSet[ClassType]] = {
            Map.empty.withDefaultValue(UIDSet.empty)
        }

        /*
         * Extends the class hierarchy.
         */
        def process(
            classType:              ClassType,
            isInterfaceType:        Boolean,
            isFinal:                Boolean,
            theSuperclassType:      Option[ClassType],
            theSuperinterfaceTypes: UIDSet[ClassType]
        ): Unit = {

            if (isInterfaceType && isFinal) {
                val message = s"the class file ${classType.toJava} defines a final interface " +
                    "which violates the JVM specification and is therefore ignored"
                OPALLogger.error("project configuration - class hierarchy", message)

                return;
            }

            def addToSet(data: Array[UIDSet[ClassType]], index: Int, t: ClassType): Unit = {
                val classTypes = data(index)
                data(index) = {
                    if (classTypes eq null)
                        new UIDSet1(t)
                    else
                        classTypes + t
                }
            }

            def ensureHasSet(data: Array[UIDSet[ClassType]], index: Int): Unit = {
                if (data(index) eq null) {
                    data(index) = UIDSet.empty
                }
            }

            //
            // Update the class hierarchy from the point of view of the newly added type
            //
            val classTypeId = classType.id
            knownTypesMap(classTypeId) = classType
            isInterfaceTypeMap(classTypeId) = isInterfaceType
            isKnownToBeFinalMap(classTypeId) = isFinal
            superclassTypeMap(classTypeId) = theSuperclassType.orNull
            superinterfaceTypesMap(classTypeId) = theSuperinterfaceTypes
            ensureHasSet(subclassTypesMap, classTypeId)
            ensureHasSet(subinterfaceTypesMap, classTypeId)

            //
            // Update the class hierarchy from the point of view of the new type's super types
            // For each super(class|interface)type make sure that it is "known"
            //
            theSuperclassType foreach { superclassType =>
                val superclassTypeId = superclassType.id
                knownTypesMap(superclassTypeId) = superclassType

                if (isInterfaceType) {
                    // an interface always has `java.lang.Object` as its super class
                    addToSet(subinterfaceTypesMap, ObjectId /*java.lang.Object*/, classType)
                } else {
                    addToSet(subclassTypesMap, superclassTypeId, classType)
                    ensureHasSet(subinterfaceTypesMap, superclassTypeId)
                }
            }
            theSuperinterfaceTypes foreach { aSuperinterfaceType =>
                val aSuperinterfaceTypeId = aSuperinterfaceType.id

                if (knownTypesMap(aSuperinterfaceTypeId) eq null) {
                    knownTypesMap(aSuperinterfaceTypeId) = aSuperinterfaceType
                    isInterfaceTypeMap(aSuperinterfaceTypeId) = true
                } else if (!isInterfaceTypeMap(aSuperinterfaceTypeId)) {
                    val message = s"the class file ${classType.toJava} defines a " +
                        s"super interface ${knownTypesMap(aSuperinterfaceTypeId).toJava} " +
                        "which is actually a regular class file"
                    classesWithBrokenInterfaceInheritance +=
                        ((
                            classType,
                            classesWithBrokenInterfaceInheritance(classType) + aSuperinterfaceType
                        ))
                    OPALLogger.error("project configuration - class hierarchy", message)
                }
                if (isInterfaceType) {
                    addToSet(subinterfaceTypesMap, aSuperinterfaceTypeId, classType)
                    ensureHasSet(subclassTypesMap, aSuperinterfaceTypeId)
                } else {
                    addToSet(subclassTypesMap, aSuperinterfaceTypeId, classType)
                    // assert(subclassTypesMap(aSuperinterfaceTypeId).contains(classType))
                    ensureHasSet(subinterfaceTypesMap, aSuperinterfaceTypeId)
                }
            }
        }

        // Analyzes the given class files and extends the current class hierarchy.
        val processedClassType: Array[Boolean] = new Array[Boolean](classTypesCount)
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

        val processedTypeDeclaration: Array[Boolean] = new Array[Boolean](classTypesCount)
        var duplicateTypeDeclarations: Set[String] = Set.empty
        typeDeclarations foreach { typeDeclaration =>
            val classType = typeDeclaration.classType
            val cid = classType.id
            if (processedTypeDeclaration(cid)) {
                duplicateTypeDeclarations += classType.toJava
            } else {
                processedTypeDeclaration(cid) = true
                // We generally don't want to use pre-configured type hierarchy information;
                // but we want to extend the information if it is obviously incomplete...
                if (!processedClassType(cid) ||
                    // processed - but complete?
                    ((cid != ClassType.ObjectId) && superclassTypeMap(cid) == null)
                ) {
                    processedClassType(classType.id) = true
                    process(
                        classType,
                        typeDeclaration.isInterfaceType,
                        isFinal = false,
                        typeDeclaration.theSuperclassType,
                        typeDeclaration.theSuperinterfaceTypes
                    )
                } else {
                    OPALLogger.warn(
                        "project configuration",
                        s"the type declaration for ${classType.toJava} is ignored; " +
                            "class is already defined in the code base " +
                            s"with superclassType ${superclassTypeMap(cid).toJava}" +
                            "or defined multiple times in the configured type hierarchy"
                    )
                }
            }
        }
        if (duplicateTypeDeclarations.nonEmpty) {
            OPALLogger.info(
                "project configuration",
                duplicateTypeDeclarations.mkString("ignored duplicate type declarations for: {", ", ", "}")
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

        val rootTypesFuture = Future[UIDSet[ClassType]] {
            knownTypesMap.foldLeft(UIDSet.empty[ClassType]) { (rootTypes, classType) =>
                if ((classType ne null) && {
                        val cid = classType.id
                        (superclassTypeMap(cid) eq null) && {
                            val superinterfaceTypes = superinterfaceTypesMap(cid)
                            (superinterfaceTypes eq null) || superinterfaceTypes.isEmpty
                        }
                    }
                ) {
                    rootTypes + classType
                } else {
                    rootTypes
                }
            }
        }

        val subtypesFuture = Future[(UIDSet[ClassType], Array[SubtypeInformation])] {
            val leafTypes = knownTypesMap.foldLeft(UIDSet.empty[ClassType]) { (leafTypes, t) =>
                if ((t ne null) && {
                        val tid = t.id
                        subclassTypesMap(tid).isEmpty && subinterfaceTypesMap(tid).isEmpty
                    }
                ) {
                    leafTypes + t
                } else {
                    leafTypes
                }
            }

            // Let's compute for each type the set of all subtypes, by starting at the bottom!
            val subtypes = new Array[SubtypeInformation](knownTypesMap.length)
            var deferredTypes = UIDSet.empty[ClassType] // we want to defer as much as possible
            val typesToProcess = mutable.Queue.empty[ClassType]

            def scheduleSupertypes(classType: ClassType): Unit = {
                val cid = classType.id
                val superclassType = superclassTypeMap(cid)
                if ((superclassType ne null) && (superclassType ne ClassType.Object)) {
                    typesToProcess += superclassType
                }
                val superSuperinterfaceTypes = superinterfaceTypesMap(cid)
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
                    var allSubinterfaceTypes = UIDSet.empty[ClassType]
                    var allSubclassTypes = UIDSet.empty[ClassType]
                    var allSubtypes = UIDSet.empty[ClassType]
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
                            allSubclassTypes,
                            allSubinterfaceTypes,
                            allSubtypes
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
                            val es: Int => IntIterator = (cid: Int) => {
                                if (knownTypesMap(cid) ne null) {
                                    val it =
                                        subinterfaceTypesMap(cid).map(_.id).iterator ++
                                            subclassTypesMap(cid).map(_.id).iterator
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
                                        scc.map { cid =>
                                            if (knownTypesMap(cid) ne null)
                                                knownTypesMap(cid).toJava
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
                            allIssues.mkString("could not compute subtype information for:\n\t", "\n\t", "\n")
                        )

                    } else {
                        madeProgress = false
                        typesToProcess ++= deferredTypes
                        deferredTypes = UIDSet.empty[ClassType]
                    }
                }
            }
            var allNoneObjectClassTypes = UIDSet.empty[ClassType]
            var allInterfaceType = UIDSet.empty[ClassType]
            var allNoneClassTypes = UIDSet.empty[ClassType]
            for {
                t <- knownTypesMap
                if t ne null
            } {
                val tid = t.id
                val theSubtypes = subtypes(tid)
                if (isInterfaceTypeMap(tid)) {
                    allInterfaceType ++= theSubtypes.interfaceTypes
                    allNoneClassTypes ++= theSubtypes.allTypes
                } else if (t ne ClassType.Object) {
                    allNoneObjectClassTypes ++= theSubtypes.classTypes
                    allNoneObjectClassTypes += t
                    allNoneClassTypes ++= theSubtypes.allTypes
                }
            }
            subtypes(ClassType.ObjectId) =
                SubtypeInformation.forObject(allNoneObjectClassTypes, allInterfaceType, allNoneClassTypes)

            (leafTypes, subtypes)
        }

        val supertypesFuture = Future[Array[SupertypeInformation]] {
            // Selects all interfaces which either have no superinterfaces or where we have no
            // information about all implemented superinterfaces.
            def rootInterfaceTypes: Iterator[ClassType] = {
                superinterfaceTypesMap.iterator.zipWithIndex.filter { si =>
                    val (superinterfaceTypes, id) = si
                    isInterfaceTypeMap(id) &&
                        ((superinterfaceTypes eq null) || superinterfaceTypes.isEmpty)
                }.map { ts => knownTypesMap(ts._2) }
            }

            val supertypes = new Array[SupertypeInformation](knownTypesMap.length)
            supertypes(ObjectId) = SupertypeInformation.ForObject

            val typesToProcess = mutable.Queue.empty[ClassType] ++ rootInterfaceTypes

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
                        UIDSet.empty[ClassType]
                }

                // let's check if we already have complete information about all supertypes
                var allSuperSuperinterfaceTypes = UIDSet.empty[ClassType]
                var allSupertypes = UIDSet.empty[ClassType]
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
                    }
                ) {
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
                            UIDSet.empty[ClassType]
                    }
                    val superclassType = superclassTypeMap(t.id)
                    val allSuperinterfaceTypes =
                        superinterfaceTypes.foldLeft(
                            if (superclassType ne null) {
                                // interfaces inherited via super class
                                supertypes(superclassType.id).interfaceTypes
                            } else {
                                UIDSet.empty[ClassType]
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
                            isInterfaceTypeMap, {
                                if (superclassType ne null)
                                    supertypes(superclassType.id).classTypes + superclassType
                                else
                                    ClassHierarchy.JustObject // we do our best....
                            },
                            allSuperinterfaceTypes, {
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
                if rootType ne ClassType.Object
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
            val unexpectedRootTypes = rootTypes.iterator filter (_ ne ClassType.Object)
            if (unexpectedRootTypes.hasNext) {
                OPALLogger.warn(
                    "project configuration - class hierarchy",
                    unexpectedRootTypes
                        .map { t => (if (isInterfaceTypeMap(t.id)) "interface " else "class ") + t.toJava }
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
                val (isFinal, cid) = e
                if (isFinal) {
                    if (subclassTypesMap(cid).nonEmpty) {
                        OPALLogger.warn(
                            "project configuration - class hierarchy",
                            s"the final type ${knownTypesMap(cid).toJava} " +
                                "has subclasses: " + subclassTypesMap(cid) +
                                "; resetting the \"is final\" property."
                        )
                        isKnownToBeFinalMap(cid) = false
                    }

                    if (subinterfaceTypesMap(cid).nonEmpty) {
                        OPALLogger.warn(
                            "project configuration - class hierarchy",
                            s"the final type ${knownTypesMap(cid).toJava} " +
                                "has subinterfaces: " + subclassTypesMap(cid) +
                                "; resetting the \"is final\" property."
                        )
                        isKnownToBeFinalMap(cid) = false
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
