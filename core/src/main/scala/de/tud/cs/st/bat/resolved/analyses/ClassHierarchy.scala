/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package de.tud.cs.st
package bat
package resolved
package analyses

import util.ControlAbstractions.foreachNonNullValueOf
import util.graphs.{ Node, toDot }
import util.{ Answer, Yes, No, Unknown }

import annotation.tailrec
import scala.collection.{ Map, Set }
import scala.collection.mutable.HashSet
import scala.collection.mutable.HashMap

import ObjectType.Object

/**
 * Represents the '''visible part of a project's class hierarchy'''.
 *
 * Only the part of a project's class hierarchy is reified that is referred to in
 * the ''class declarations'' of the analyzed classes. I.e., those classes
 * which are directly referred to in class declarations, but for which the respective
 * class file was not analyzed, are also considered to be visible and are integrated in
 * the class hierarchy. However, types only referred to in the body of a method, but for
 * which neither the defining class file is analyzed nor a class exists that inherits from
 * them are not integrated.
 * For example, if the class file of the class `java.util.ArrayList` is analyzed, then the
 * class hierarchy will have some information about, e.g., `java.util.List`
 * from which `ArrayList` inherits. However, the information about `List` is incomplete
 * and `List` will be a boundary class unless we also analyze the class file that
 * defines `java.util.List`.
 *
 * The type `java.lang.Object` is always part of the class hierarchy.
 *
 * ==Thread safety==
 * This class is immutable. Hence, concurrent access to the class hierarchy is supported.
 *
 * @param superclassTypeMap Contains type information about a type's immediate superclass.
 *      This value is defined unless the key identifies the
 *      object type `java.lang.Object` or when the respective class files was not
 *      analyzed and the respective type was only seen in the declaration of another class.
 * @param superinterfaceTypesMap Contains type information about a type's directly
 *      implemented interfaces; if any.
 * @param subclassTypesMap Contains type information about a type's subclasses; if any.
 * @param subinterfaceTypesMap Contains type information about a type's subinterfaces.
 *      They only ''class type'' that is allowed to have a non-empty set of subinterfaces
 *      is `java.lang.Object`.
 *
 * @note It is generally considered to be an error to pass an instance of an `ObjectType`
 *      to any method if the `ObjectType` was not previously added. If in doubt, first
 *      check if the type is known (`isKnown`/`ifKnown`).
 * @author Michael Eichberg
 */
class ClassHierarchy private (
        // the case "java.lang.Object" is handled explicitly!
        private[this] val knownTypesMap: Array[ObjectType],
        private[this] val interfaceTypesMap: Array[Boolean],
        private[this] val superclassTypeMap: Array[ObjectType],
        private[this] val superinterfaceTypesMap: Array[HashSet[ObjectType]],
        private[this] val subclassTypesMap: Array[HashSet[ObjectType]],
        private[this] val subinterfaceTypesMap: Array[HashSet[ObjectType]]) {

    assume(knownTypesMap.length == superclassTypeMap.length)
    assume(knownTypesMap.length == interfaceTypesMap.length)
    assume(knownTypesMap.length == superinterfaceTypesMap.length)
    assume(knownTypesMap.length == subclassTypesMap.length)
    assume(knownTypesMap.length == subinterfaceTypesMap.length)

    import java.util.concurrent.locks.ReentrantReadWriteLock

    private[this] val objectTypesMapRWLock =
        new ReentrantReadWriteLock();

    private[this] var objectTypesMap: Array[ObjectType] =
        new Array(ObjectType.objectTypesCount)

    private[this] val objectTypesCreationListener =
        (objectType: ObjectType) ⇒ {
            val id = objectType.id
            try {
                objectTypesMapRWLock.writeLock().lock()
                if (id >= objectTypesMap.length) {
                    val newLength = Math.max(ObjectType.objectTypesCount, id) + 20
                    val newObjectTypesMap = new Array[ObjectType](newLength)
                    Array.copy(objectTypesMap, 0, newObjectTypesMap, 0, objectTypesMap.length)
                    objectTypesMap = newObjectTypesMap
                }
                objectTypesMap(id) = objectType
            } finally {
                objectTypesMapRWLock.writeLock().unlock()
            }
        }

    ObjectType.setObjectTypeCreationListener(objectTypesCreationListener)

    /**
     * Returns the `ObjectType` with the given Id.
     */
    final def getObjectType(objectTypeId: Int): Option[ObjectType] = {
        Option(
            try {
                objectTypesMapRWLock.readLock().lock()
                objectTypesMap(objectTypeId)
            } finally {
                objectTypesMapRWLock.readLock().unlock()
            }
        )
    }

    /**
     * Returns `true` if the class hierarchy has some information about the given
     * type.
     */
    def isKnown(objectType: ObjectType): Boolean = {
        val id = objectType.id
        (id < knownTypesMap.length) && (knownTypesMap(id) ne null)
    }

    /**
     * @example
     * {{{
     * ifKnown(ObjectType.Serializable)(isDirectSupertypeInformationComplete)
     * }}}
     */
    @inline final def ifKnown[T](objectType: ObjectType)(f: ObjectType ⇒ T): Option[T] = {
        if (isKnown(objectType))
            Some(f(objectType))
        else
            None
    }

    /**
     * Returns `true` if the type is unknown. This is true for all types that are
     * referred to in the body of a method, but which are not referred to in the
     * declaration of some class (file) that was analyzed.
     */
    def isUnknown(objectType: ObjectType): Boolean = !isKnown(objectType)

    /**
     * Tests if the given `objectType` defines an interface type.
     *
     * @note This method is only defined if the type is known.
     */
    @inline def isInterface(objectType: ObjectType) = interfaceTypesMap(objectType.id)

    /**
     * Returns true if the type hierarchy information w.r.t. the given type's supertypes
     * is complete.
     */
    @inline def isDirectSupertypeInformationComplete(objectType: ObjectType): Boolean =
        (objectType eq Object) ||
            isKnown(objectType) && (superclassTypeMap(objectType.id) ne null)

    @inline def isSupertypeInformationComplete(objectType: ObjectType): Boolean = {
        (objectType eq Object) || {
            val id = objectType.id
            isDirectSupertypeInformationComplete(objectType) &&
                isSupertypeInformationComplete(superclassTypeMap(id)) &&
                superinterfaceTypesMap(id).forall(isSupertypeInformationComplete(_))
        }
    }

    def hasSubtypes(objectType: ObjectType): Answer = {
        val id = objectType.id
        if (id < subclassTypesMap.length /*& id < subinterfaceTypesMap.length */ ) {
            Answer((subclassTypesMap(id) ne null) || (subinterfaceTypesMap(id) ne null))
        } else {
            Unknown
        }
    }

    /**
     * The set of all class- and interface-types that (directly or indirectly)
     * inherit from the given type. The given type is not included in the returned
     * set.
     *
     * @return The set of all direct and indirect subtypes of the given type.
     * @note If you don't need the set, it is more efficient to use `foreachSubtype
     */
    def allSubtypes(objectType: ObjectType): Set[ObjectType] = {
        val subtypes = HashSet.empty[ObjectType]
        foreachSubtype(objectType) { subtype ⇒ subtypes add subtype }
        subtypes
    }

    /**
     * Calls the function `f` for each (direct or indirect) subtype of the given type.
     * If the given `objectType` identifies an interface type then it is possible
     * that `f` is passed the same `ObjectType` multiple times.
     *
     * @note This method is only defined if the type is known.
     *
     * @param objectType An object type.
     */
    def foreachSubtype(objectType: ObjectType)(f: ObjectType ⇒ Unit) {

        // We had to change this method to get better performance.
        // The naive implementation using foreach and (mutual) recursion
        // didn't perform well.
        @inline def processAllSubtypes() {
            var allSubtypes: List[HashSet[ObjectType]] = Nil
            val id = objectType.id
            val subclassTypes = subclassTypesMap(id)
            if (subclassTypes ne null) {
                allSubtypes = subclassTypes :: allSubtypes
            }
            val subinterfaceTypes = subinterfaceTypesMap(id)
            if (subinterfaceTypes ne null) {
                allSubtypes = subinterfaceTypes :: allSubtypes
            }

            while (allSubtypes.nonEmpty) {
                val subtypes = allSubtypes.head
                allSubtypes = allSubtypes.tail
                val subtypesIterator = subtypes.iterator
                while (subtypesIterator.hasNext) {
                    val subtype = subtypesIterator.next
                    f(subtype)

                    val id = subtype.id
                    val subclassTypes = subclassTypesMap(id)
                    if (subclassTypes ne null) {
                        allSubtypes = subclassTypes :: allSubtypes
                    }
                    val subinterfaceTypes = subinterfaceTypesMap(id)
                    if (subinterfaceTypes ne null) {
                        allSubtypes = subinterfaceTypes :: allSubtypes
                    }
                }
            }
        }

        processAllSubtypes()
    }

    /**
     * Calls the given function `f` for each of the given type's supertypes.
     * It is possible that the same super interface type `I` is passed multiple
     * times to `f` when `I` is implemented multiple times by the given type's supertypes.
     *
     * @note This method is only defined if the type is known.
     *
     * @param objectType A type known to the class hierarchy.
     */
    def foreachSupertype(objectType: ObjectType)(f: ObjectType ⇒ Unit) {
        val id = objectType.id
        val superclassType = superclassTypeMap(id)
        if (superclassType != null) {
            f(superclassType)
            foreachSupertype(superclassType)(f)
        }

        val superinterfaceTypes = superinterfaceTypesMap(id)
        if (superinterfaceTypes != null) {
            superinterfaceTypes foreach { superinterfaceType ⇒
                f(superinterfaceType)
                foreachSupertype(superinterfaceType)(f)
            }
        }
    }

    /**
     * The set of all supertypes of the given type.
     *
     * @note This method is only defined if the type is known.
     */
    def allSupertypes(objectType: ObjectType): Set[ObjectType] = {
        val supertypes = HashSet.empty[ObjectType]
        foreachSupertype(objectType) { supertypes.add(_) }
        supertypes
    }

    /**
     * Calls the function `f` for each supertype of the given object type for
     * which the classfile is available.
     * It is possible that the class file of the same super interface type `I`
     * is passed multiple times to `f` when `I` is implemented multiple times
     * by the given type's supertypes.
     *
     * The algorithm first iterates over the type's super classes
     * before it iterates over the super interfaces.
     *
     * @note This method is only defined if the type is known.
     *
     * @param objectType A type known to the class hierarchy.
     */
    def foreachSuperclass(
        objectType: ObjectType,
        classes: SomeProject)(
            f: ClassFile ⇒ Unit): Unit =
        foreachSupertype(objectType) { supertype ⇒
            classes(supertype) match {
                case Some(classFile) ⇒ f(classFile)
                case _               ⇒ /*Do nothing*/
            }
        }

    /**
     * Returns the set of all classes/interfaces from which the given type inherits
     * and for which the respective class file is available.
     *
     * @note It may be more efficient to use `foreachSuperclass(ObjectType,
     *      ObjectType ⇒ Option[ClassFile])(ClassFile => Unit)`
     * @note This method is only defined if the type is known.
     *
     * @return An iterable over all class files of all super types of the given
     *      `objectType` that pass the given filter and for which the class file
     *      is available. The iterable guarantees
     */
    def superclasses(
        objectType: ObjectType,
        classes: SomeProject)(
            classFileFilter: ClassFile ⇒ Boolean = { _ ⇒ true }): Iterable[ClassFile] = {
        // We want to make sure that every class file is returned only once,
        // but we want to avoid equals calls on `ClassFile` objects. 
        var classFiles = Map[ObjectType, ClassFile]()
        foreachSuperclass(objectType, classes) { classFile ⇒
            if (classFileFilter(classFile))
                classFiles = classFiles.updated(classFile.thisType, classFile)
        }
        classFiles.values
    }

    def superinterfaceTypes(objectType: ObjectType): Option[Set[ObjectType]] = {
        if (isKnown(objectType)) {
            val superinterfaceTypes = superinterfaceTypesMap(objectType.id)
            if (superinterfaceTypes ne null)
                Some(superinterfaceTypes)
            else
                Some(HashSet.empty)
        } else {
            None
        }
    }

    def superclassType(objectType: ObjectType): Option[ObjectType] = {
        if (isKnown(objectType)) {
            val superclassType = superclassTypeMap(objectType.id)
            if (superclassType ne null)
                Some(superclassType)
            else
                None
        } else {
            None
        }
    }

    /**
     * Determines if the given class or interface type `subtype` is actually a subtype
     * of the class or interface type `supertype`.
     *
     * This method can be used as a foundation for implementing the logic of the JVM's
     * `instanceof` and `classcast` instructions. But, in that case additional logic
     * for handling `null` values and for considering the runtime type needs to be
     * implemented by the caller of this method.
     *
     * @note This method is only defined if the type is known.
     *
     * @return `Yes` if `subtype` is a subtype of the given `supertype`. `No`
     *      if `subtype` is not a subtype of `supertype` and `Unknown` if the analysis is
     *      not conclusive. The latter can happen if the class hierarchy is not
     *      completely available and hence precise information about a type's supertypes
     *      is not available.
     */
    def isSubtypeOf(subtype: ObjectType, theSupertype: ObjectType): Answer = {
        if ((subtype eq theSupertype) || (theSupertype eq Object))
            return Yes

        if (subtype eq Object /* && theSupertype != ObjectType.Object*/ )
            return No

        if (isUnknown(subtype))
            return Unknown

        val subtypeIsInterface = isInterface(subtype)

        if (isUnknown(theSupertype)) {
            if (isSupertypeInformationComplete(subtype))
                return No
            else
                return Unknown
        }
        val supertypeIsInterface = isInterface(theSupertype)

        if (subtypeIsInterface && !supertypeIsInterface)
            // An interface always (only) directly inherits from java.lang.Object
            // and this is already checked before.
            return No

        @inline def implementsInterface(
            subinterfaceType: ObjectType,
            theSupertype: ObjectType): Answer = {
            if (subinterfaceType eq theSupertype)
                return Yes

            val superinterfaceTypes = superinterfaceTypesMap(subinterfaceType.id)
            if (superinterfaceTypes eq null) {
                if (isDirectSupertypeInformationComplete(subinterfaceType))
                    No
                else
                    Unknown
            } else {
                var answer: Answer = No
                superinterfaceTypes foreach { intermediateType ⇒
                    var anotherAnswer = implementsInterface(intermediateType, theSupertype)
                    if (anotherAnswer.yes)
                        return Yes
                    answer &= anotherAnswer
                }
                answer
            }
        }

        @inline def isSubtypeOf(subclassType: ObjectType): Answer = {
            @inline def inheritsFromInterface(answerSoFar: Answer): Answer = {
                if (supertypeIsInterface) {
                    var doesInheritFromInterface =
                        implementsInterface(subclassType, theSupertype)
                    if (doesInheritFromInterface.yes)
                        Yes
                    else
                        answerSoFar & doesInheritFromInterface
                } else
                    No
            }

            val superSubclassType = superclassTypeMap(subclassType.id)
            if (superSubclassType ne null) {
                if (superSubclassType eq theSupertype)
                    return Yes

                var answer = isSubtypeOf(superSubclassType)
                if (answer.yes)
                    Yes
                else
                    inheritsFromInterface(answer)
            } else {
                /* we have reached the top (visible) class Type*/
                inheritsFromInterface(
                    if (subclassType eq ObjectType.Object)
                        No
                    else
                        Unknown
                )
            }
        }

        if (subtypeIsInterface /*&& supertypeIsInterface*/ )
            implementsInterface(subtype, theSupertype)
        else
            isSubtypeOf(subtype)
    }

    /**
     * Determines if `subtype` is a subtype of `supertype` using this 
     * class hierarchy.
     *
     * This method can be used as a foundation for implementing the logic of the JVM's
     * `instanceof` and `classcast` instructions. But, in both cases additional logic
     * for handling `null` values and for considering the runtime type needs to be
     * implemented by the caller of this method.
     *
     * @param subtype A class or array type.
     * @param supertype A class or array type.
     * @return `Yes` if `subtype` is indeed a subtype of the given `supertype`. `No`
     *    if `subtype` is not a subtype of `supertype` and `Unknown` if the analysis is
     *    not conclusive. The latter can happen if the class hierarchy is not
     *    completely available and hence precise information about a type's supertypes
     *    is not available.
     * @note The answer `No` does not necessarily imply that two '''runtime values''' for
     *    which the given types are only upper bounds are not (w.r.t. their
     *    runtime types) in a subtype relation. E.g., if `subtype` denotes the type
     *    `java.util.List` and `supertype` denotes the type `java.util.ArrayList` then
     *    the answer is clearly `No`. But, at runtime, this may not be the case. I.e.,
     *    only the answer `Yes` is conclusive. In case of `No` further information
     *    needs to be taken into account by the caller to determine what it means that
     *    the (upper) type (bounds) of the underlying values are not in an inheritance 
     *    relation.
     */
    def isSubtypeOf(subtype: ReferenceType, supertype: ReferenceType): Answer = {
        if ((subtype eq supertype) || (supertype eq Object))
            return Yes

        if (subtype eq Object)
            return No // the given supertype has to be a subtype...

        subtype match {
            case ot: ObjectType ⇒
                if (supertype.isArrayType)
                    No
                else
                    // The analysis is conclusive iff we can get all supertypes
                    // for the given type (ot) up until "java/lang/Object"; i.e.,
                    // if there are no holes.
                    isSubtypeOf(ot.asObjectType, supertype.asObjectType)
            case ArrayType(componentType) ⇒
                supertype match {
                    case ot: ObjectType ⇒
                        if ((ot eq ObjectType.Serializable) ||
                            (ot eq ObjectType.Cloneable))
                            Yes
                        else
                            No
                    case ArrayType(superComponentType: BaseType) ⇒
                        if (componentType eq superComponentType)
                            Yes
                        else
                            No
                    case ArrayType(superComponentType: ReferenceType) ⇒
                        if (componentType.isBaseType)
                            No
                        else
                            isSubtypeOf(componentType.asReferenceType, superComponentType)
                }
        }
    }

    /**
     * Resolves a symbolic reference to a field. Basically, the search starts with
     * the given class `c` and then continues with `c`'s superinterfaces before the
     * search is continued with `c`'s superclass (as prescribed by the JVM specification
     * for the resolution of unresolved symbolic references.)
     *
     * Resolving a symbolic reference is particularly required to, e.g., get a field's
     * annotations or to get a field's value (if it is static, final and has a constant
     * value).
     *
     * @note This implementation does not check for `IllegalAccessError`. This check
     *      needs to be done by the caller. The same applies for the check that the
     *      field is non-static if get-/putfield is used and static if a get/putstatic is
     *      used to access the field. In the latter case the JVM would throw a
     *      `LinkingException`.
     *      Furthermore, if the field cannot be found, it is the responsibility of the
     *      caller to handle that situation.
     *
     * @note Resolution is final. I.e., either this algorithm has found the defining field
     *      or the field is not defined by one of the loaded classes. Searching for the
     *      field in subclasses is not meaningful as Java does not do dynamic field
     *      reference resolution. For further details study the following code:
     * <pre><code>
     * static class Super {
     *
     *  public int x = 0;
     *
     *  public int y = 0;
     *
     *  public int z = 0;
     *
     *  public String toString() { return String.valueOf(x); }
     * }
     *
     * static interface I { int y = -1; }
     *
     * static class Sub extends Super implements I {
     *
     *  public int x = 1;
     *
     *
     *  // public java.lang.String toString(); [FILTERED]
     *  //
     *  //  4  ldc <String "super.x="> [24]
     *  //  6  invokespecial java.lang.StringBuilder(java.lang.String) [26]
     *  //  9  aload_0 [this]
     *  // 10  invokespecial fields.FieldReferenceResolution$Super.toString() : java.lang.String [29]
     *  //
     *  // 21  ldc <String "sub.x="> [37]
     *  // 26  aload_0 [this]
     *  // 27  getfield fields.FieldReferenceResolution$Sub.x : int [14]
     *  //
     *  // 38  ldc <String "((Super)this).y="> [42]
     *  // 43  aload_0 [this]
     *  // 44  getfield fields.FieldReferenceResolution$Super.y : int [44]
     *  //
     *  // 55  ldc <String "super.y="> [47]
     *  // 60  aload_0 [this]
     *  // 61  getfield fields.FieldReferenceResolution$Super.y : int [44]
     *  //
     *  // 72  ldc <String "((I)this).y="> [49]
     *  // 77  iconst_m1
     *  //
     *  // 86  ldc <String "this.z="> [51]
     *  // 91  aload_0 [this]
     *  // 92  getfield fields.FieldReferenceResolution$Sub.z : int [53] // <= HERE, we need to resolve the reference!
     *
     *  public String toString() {
     *   return
     *   "super.x=" + super.toString()/* super.x */+ "; " +
     *   "sub.x=" + this.x + "; " + // => super.x=0; sub.x=1
     *   "((Super)this).y=" + ((Super) this).y + "; " +
     *   "super.y=" + super.y + "; " +
     *   "((I)this).y=" + ((I) this).y + "; " +
     *   "this.z=" + this.z;
     *   // <=> super.x=0; sub.x=1; ((Super)this).y=0; super.y=0; ((I)this).y=-1; this.z=0
     *  }
     * }
     * </code></pre>
     *
     * @param c The class (or a superclass thereof) that is expected to define the
     *      reference field.
     * @param fieldName The name of the accessed field.
     * @param fieldType The type of the accessed field (the field descriptor).
     * @param classes A function to lookup the class that implements a given `ObjectType`.
     * @return The concrete class that defines the referenced field.
     */
    def resolveFieldReference(
        c: ObjectType,
        fieldName: String,
        fieldType: FieldType,
        project: SomeProject): Option[Field] = {
        // More details: JVM 7 Spec. Section 5.4.3.2 
        project(c) flatMap { classFile ⇒
            classFile.fields find { field ⇒
                (field.fieldType eq fieldType) && (field.name == fieldName)
            } orElse {
                classFile.interfaceTypes collectFirst { supertype ⇒
                    resolveFieldReference(supertype, fieldName, fieldType, project) match {
                        case Some(resolvedFieldReference) ⇒ resolvedFieldReference
                    }
                } orElse {
                    classFile.superclassType flatMap { supertype ⇒
                        resolveFieldReference(supertype, fieldName, fieldType, project)
                    }
                }
            }
        }
    }

    /**
     * Tries to resolve a method reference as specified by the JVM specification.
     * I.e., the algorithm tries to find the class that actually declares the referenced
     * method. Resolution of signature polymorphic method calls is also supported; for
     * details see `lookupMethodDefinition`).
     *
     * This method is the basis for the implementation of the semantics
     * of the `invokeXXX` instructions. However, it does not check whether the resolved
     * method can be accessed by the caller or if it is abstract. Additionally, it is still
     * necessary that the caller makes a distinction between the statically (at compile time)
     * identified declaring class and the dynamic type of the receiver in case of
     * `invokevirtual` and `invokeinterface` instructions. I.e., additional processing
     * is necessary.
     *
     * @note Generally, if the type of the receiver is not precise the receiver object's
     *    subtypes should also be searched for method implementations (at least those
     *    classes should be taken into consideration that may be instantiated).
     *
     * @note This method just resolves a method reference. Additional checks,
     *    such as whether the resolved method is accessible, may be necessary.
     *
     * @param receiverType The type of the object that receives the method call. The
     *    type must be a class type and must not be an interface type.
     * @return The resolved method and its defining class or `None`.
     */
    def resolveMethodReference(
        receiverType: ObjectType,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        project: SomeProject): Option[Method] = {

        project(receiverType) flatMap { classFile ⇒
            assume(!classFile.isInterfaceDeclaration)

            lookupMethodDefinition(
                receiverType,
                methodName,
                methodDescriptor,
                project
            ) orElse
                lookupMethodInSuperinterfaces(
                    classFile,
                    methodName,
                    methodDescriptor,
                    project
                )
        }
    }

    def resolveInterfaceMethodReference(
        receiverType: ObjectType,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        project: SomeProject): Option[Method] = {

        project(receiverType) flatMap { classFile ⇒
            assume(classFile.isInterfaceDeclaration)

            {
                lookupMethodInInterface(
                    classFile,
                    methodName,
                    methodDescriptor,
                    project)
            } orElse {
                lookupMethodDefinition(
                    ObjectType.Object,
                    methodName,
                    methodDescriptor,
                    project)
            }
        }
    }

    def lookupMethodInInterface(
        classFile: ClassFile,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        project: SomeProject): Option[Method] = {

        {
            classFile.findMethod(methodName, methodDescriptor)
        } orElse {
            lookupMethodInSuperinterfaces(classFile, methodName, methodDescriptor, project)
        }
    }

    def lookupMethodInSuperinterfaces(
        classFile: ClassFile,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        project: SomeProject): Option[Method] = {

        classFile.interfaceTypes foreach { superinterface: ObjectType ⇒
            project(superinterface) map { superclass ⇒
                val result =
                    lookupMethodInInterface(
                        superclass,
                        methodName,
                        methodDescriptor,
                        project)
                if (result.isDefined)
                    return result
            }
        }
        None
    }

    /**
     * Looks up the class file and method which actually defines the method that is
     * referred to by the given receiver type, method name and method descriptor. Given
     * that we are searching for method definitions the search is limited to the
     * superclasses of the class of the given receiver type.
     *
     * This method does not take visibility modifiers or the static modifier into account.
     * If necessary, such checks needs to be done by the caller.
     *
     * This method supports resolution of `signature polymorphic methods`
     * (in this case however, it needs to be checked that the respective invoke
     * instruction is an `invokevirtual` instruction.)
     *
     * @note In case that you ''analyze static source code dependencies'' and if an invoke
     *    instruction refers to a method that is not defined by the receiver's class, then
     *    it might be more meaningful to still create a dependency to the receiver's class
     *    than to look up the actual definition in one of the receiver's super classes.
     *
     * @return `Some((ClassFile,Method))` if the method is found. `None` if the method
     *    is not found. This can basically happen under two circumstances:
     *    First, not all class files referred to/used by the project are (yet) analyzed;
     *    i.e., we do not have all class files belonging to the project.
     *    Second, the analyzed class files do not belong together (they either belong to
     *    different projects or to incompatible versions of the same project.)
     */
    def lookupMethodDefinition(
        receiverType: ObjectType,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        project: SomeProject): Option[Method] = {

        assume(!isInterface(receiverType))

        @tailrec def lookupMethodDefinition(receiverType: ObjectType): Option[Method] = {
            val classFileOption = project(receiverType)
            var methodOption =
                if (classFileOption.isDefined) {
                    val classFile = classFileOption.get
                    classFile.findMethod(methodName, methodDescriptor).
                        orElse {
                            /* FROM THE SPECIFICATION:
                         * Method resolution attempts to look up the referenced method in C and 
                         * its superclasses:
                         * If C declares exactly one method with the name specified by the 
                         * method reference, and the declaration is a signature polymorphic 
                         * method, then method lookup succeeds. 
                         * [...]
                         * 
                         * A method is signature polymorphic if:
                         * - It is declared in the java.lang.invoke.MethodHandleclass.
                         * - It has a single formal parameter of type Object[].
                         * - It has a return type of Object.
                         * - It has the ACC_VARARGS and ACC_NATIVE flags set.
                         */
                            if (receiverType eq ObjectType.MethodHandle)
                                classFile.findMethod(
                                    methodName,
                                    MethodDescriptor.SignaturePolymorphicMethod).find(
                                        _.isNativeAndVarargs)
                            else
                                None
                        }
                } else
                    None

            if (methodOption.isDefined)
                methodOption
            else {
                val superclassType = superclassTypeMap(receiverType.id)
                if (superclassType ne null)
                    lookupMethodDefinition(superclassType)
                else
                    None
            }
        }

        lookupMethodDefinition(receiverType)
    }

    /**
     * Returns all classes that implement the given method by searching all subclasses
     * of `receiverType` for implementations of the given method and also considering
     * the superclasses of the `receiverType` up until the class (not interface) that
     * defines the respective method.
     *
     *  @param receiverType An upper bound of the runtime type of some value. __If the type
     *       is known to  be precise (i.e., it is no approximation of the runtime type)
     *       then it is far more meaningful to directly call `lookupMethodDefinition`.__
     *  @param methodName The name of the method.
     *  @param methodDescriptor The method's descriptor.
     *  @param project Required to get a type's implementing class file.
     *       This method expects unrestricted access to the pool of all class files.
     *  @param classesFiler A function that returns true, if the runtime type of
     *       the `receiverType` may be of the type defined by the given object type. For
     *       example, if you analyze a project and perform a lookup of all methods that
     *       implement the method `toString`, then this set would probably be very large.
     *       But, if you know that only instances of the class (e.g.) `ArrayList` have
     *       been created so far
     *       (up to the point in your analysis where you call this method), it is
     *       meaningful to sort out all other classes (such as `Vector`).
     */
    def lookupImplementingMethods(
        receiverType: ObjectType,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        project: SomeProject,
        classesFilter: ObjectType ⇒ Boolean = { _ ⇒ true }): Iterable[Method] = {

        var implementingMethods: List[Method] =
            {
                if (isInterface(receiverType))
                    lookupMethodDefinition(
                        ObjectType.Object, // to handle calls such as toString on a (e.g.) "java.util.List"
                        methodName,
                        methodDescriptor,
                        project)
                else
                    lookupMethodDefinition(
                        receiverType,
                        methodName,
                        methodDescriptor,
                        project)
            } match {
                case Some(method) if !method.isAbstract ⇒ List(method)
                case _                                  ⇒ List.empty
            }

        // Search all subclasses
        var seenSubtypes = HashSet.empty[ObjectType]
        foreachSubtype(receiverType) { (subtype: ObjectType) ⇒
            if (!isInterface(subtype) && !seenSubtypes.contains(subtype)) {
                seenSubtypes += subtype
                if (classesFilter(subtype)) {
                    project(subtype) foreach { classFile ⇒
                        val methodOption =
                            classFile.findMethod(methodName, methodDescriptor)
                        if (methodOption.isDefined) {
                            val method = methodOption.get
                            if (!method.isAbstract)
                                implementingMethods = method :: implementingMethods
                        }
                    }
                }
            }
        }

        implementingMethods
    }

    /**
     * The direct subtypes of the given types.
     */
    def directSubtypesOf(objectType: ObjectType): Set[ObjectType] = {
        val id = objectType.id

        val directSubtypes = {
            val subclassTypes = this.subclassTypesMap(id)
            if (subclassTypes ne null)
                subclassTypes
            else
                HashSet.empty[ObjectType]
        }

        val subinterfaceTypes = this.subinterfaceTypesMap(id)
        if (subinterfaceTypes ne null)
            directSubtypes ++= subinterfaceTypes

        directSubtypes
    }

    /**
     * Returns a view of the class hierarchy as a graph (which can then be transformed
     * into a dot representation [[http://www.graphviz.org Graphviz]]). This
     * graph can be a multi-graph if we don't see the complete class hierarchy.
     */
    def toGraph(): Node = new Node {

        private val nodes: Map[ObjectType, Node] = {
            val nodes = HashMap.empty[ObjectType, Node]

            foreachNonNullValueOf(knownTypesMap) { (id, aType) ⇒
                val entry: (ObjectType, Node) = (
                    aType,
                    new Node {
                        private val directSubtypes = directSubtypesOf(aType)
                        def uniqueId = aType.id
                        def toHRR: Option[String] = Some(aType.toJava)
                        def backgroundColor: Option[String] =
                            if (isInterface(aType))
                                Some("aliceblue")
                            else
                                None
                        def foreachSuccessor(f: Node ⇒ Unit) {
                            directSubtypes foreach { subtype ⇒
                                f(nodes(subtype))
                            }
                        }
                        def hasSuccessors(): Boolean = directSubtypes.nonEmpty
                    }
                )
                nodes += entry
            }
            nodes
        }

        // a virtual root node
        def uniqueId = -1
        def toHRR = None
        def backgroundColor = None
        def foreachSuccessor(f: Node ⇒ Unit) {
            /**
             * We may not see the class files of all classes that are referred
             * to in the class files that we did see. Hence, we have to be able
             * to handle partial class hierarchies.
             */
            val rootTypes = nodes filter { case (t, _) ⇒ superclassTypeMap(t.id) eq null }
            rootTypes.values.foreach(f)
        }
        def hasSuccessors(): Boolean = nodes.nonEmpty
    }
}

/**
 * Defines factory methods for creating `ClassHierarchy` objects.
 *
 * @author Michael Eichberg
 */
object ClassHierarchy {

    import scala.collection.mutable.HashSet
    import scala.collection.mutable.HashMap
    import de.tud.cs.st.util.ControlAbstractions.foreachNonNullValueOf

    /**
     * Creates a `ClassHierarchy` that captures the type hierarchy related to
     * the exceptions thrown by specific Java bytecode instructions.
     *
     * This class hierarchy is primarily useful for testing purposes.
     */
    def preInitializedClassHierarchy: ClassHierarchy = apply(Traversable.empty)

    /**
     * Create the class hierarchy by analyzing the given class files and
     * the specified predefined class hierarchies. By default the class hierarchy
     * related to the exceptions thrown by bytecode instructions are predefined
     * as well as the class hierarchy related to the main classes of the JDK.
     * See the file `ClassHierarchyJVMExceptions.ths` and `ClassHierarchyJLS.ths`
     * (text files) for further details.
     */
    def apply(
        classFiles: Traversable[ClassFile],
        predefinedClassHierarchies: Seq[() ⇒ java.io.InputStream] = List(
            () ⇒ { getClass().getResourceAsStream("ClassHierarchyJLS.ths") },
            () ⇒ { getClass().getResourceAsStream("ClassHierarchyJVMExceptions.ths") }
        )): ClassHierarchy = {

        def processPredefinedClassHierarchy(
            createInputStream: () ⇒ java.io.InputStream): Iterator[TypeDeclaration] = {
            val in = createInputStream()
            util.ControlAbstractions.process(new scala.io.BufferedSource(in)) { source ⇒
                if (source == null) {
                    import Console._
                    err.println(BOLD+"Loading the predefined class hierarchy failed."+RESET)
                    err.println("Make sure that all resources are found in the correct folders.")
                    err.println("Try to rebuild the project using"+BOLD + BLUE+"sbt copy-resources"+RESET+".")
                    return Iterator.empty
                }

                val SpecLineExtractor =
                    """(class|interface)\s+(\S+)(\s+extends\s+(\S+)(\s+implements\s+(.+))?)?""".r

                val specLines =
                    source.getLines.map(_.trim).filterNot {
                        l ⇒ l.startsWith("#") || l.length == 0
                    }
                (
                    for {
                        SpecLineExtractor(typeKind, theType, _, superclassType, _, superinterfaceTypes) ← specLines
                    } yield {
                        TypeDeclaration(
                            ObjectType(theType),
                            typeKind == "interface",
                            Option(superclassType).map(ObjectType(_)),
                            Option(superinterfaceTypes).map { superinterfaceTypes ⇒
                                HashSet.empty ++ superinterfaceTypes.split(",").map(_.trim).map(ObjectType(_))
                            }.getOrElse(HashSet.empty)
                        )
                    }
                )
            }
        }

        // We have to make sure that we have seen all types before we can generate
        // the arrays to store the information about the types! 
        val typeDeclarations = (
            for (predefinedClassHierarchy ← predefinedClassHierarchies)
                yield processPredefinedClassHierarchy(predefinedClassHierarchy)
        ).flatten

        def addToSet(data: Array[HashSet[ObjectType]], index: Int, elem: ObjectType) = {
            val set: HashSet[ObjectType] = data(index)
            if (set eq null) {
                data(index) = HashSet(elem)
            } else
                set += elem
        }

        val objectTypesCount = ObjectType.objectTypesCount
        val knownTypesMap = new Array[ObjectType](objectTypesCount)
        val interfaceTypesMap = new Array[Boolean](objectTypesCount)
        val superclassTypeMap = new Array[ObjectType](objectTypesCount)
        val superinterfaceTypesMap = new Array[HashSet[ObjectType]](objectTypesCount)
        val subclassTypesMap = new Array[HashSet[ObjectType]](objectTypesCount)
        val subinterfaceTypesMap = new Array[HashSet[ObjectType]](objectTypesCount)

        val ObjectId = ObjectType.Object.id

        /**
         * Analyzes the given class file and extends the current class hierarchy.
         */
        def processClassFile(classFile: ClassFile) =
            process(
                classFile.thisType,
                classFile.isInterfaceDeclaration,
                classFile.superclassType,
                HashSet.empty ++ classFile.interfaceTypes
            )

        /**
         * Extends the class hierarchy.
         */
        def process(
            objectType: ObjectType,
            isInterfaceType: Boolean,
            theSuperclassType: Option[ObjectType],
            theSuperinterfaceTypes: HashSet[ObjectType]) {

            //
            // Update the class hierarchy from the point of view the newly added type 
            //
            knownTypesMap(objectType.id) = objectType
            interfaceTypesMap(objectType.id) == isInterfaceType
            superclassTypeMap(objectType.id) = theSuperclassType.getOrElse(null)
            superinterfaceTypesMap(objectType.id) = theSuperinterfaceTypes

            //
            // For each super(class|interface)type make sure that it is "known" 
            //
            theSuperclassType.foreach { superclassType ⇒
                knownTypesMap(superclassType.id) = superclassType
            }
            theSuperinterfaceTypes.foreach { aSuperinterfaceType ⇒
                knownTypesMap(aSuperinterfaceType.id) = aSuperinterfaceType
                interfaceTypesMap(aSuperinterfaceType.id) = true
            }

            //
            // Update the subtype information - i.e., update the class hierarchy 
            // from the point of view of the new type's super types 
            //
            if (isInterfaceType) {
                // an interface always has `java.lang.Object` as its super class
                addToSet(subinterfaceTypesMap, ObjectId /*java.lang.Object*/ , objectType)
            } else if (theSuperclassType.isDefined) {
                addToSet(subclassTypesMap, theSuperclassType.get.id, objectType)
            }
            theSuperinterfaceTypes.foreach { aSuperinterfaceType ⇒
                addToSet(subinterfaceTypesMap, aSuperinterfaceType.id, objectType)
            }
        }

        typeDeclarations foreach { typeDecl ⇒
            process(
                typeDecl.objectType,
                typeDecl.isInterfaceType,
                typeDecl.theSuperclassType,
                typeDecl.theSuperinterfaceTypes)
        }
        classFiles foreach { processClassFile(_) }

        new ClassHierarchy(
            knownTypesMap,
            interfaceTypesMap,
            superclassTypeMap,
            superinterfaceTypesMap,
            subclassTypesMap,
            subinterfaceTypesMap
        )
    }
}

case class TypeDeclaration(
    objectType: ObjectType,
    isInterfaceType: Boolean,
    theSuperclassType: Option[ObjectType],
    theSuperinterfaceTypes: HashSet[ObjectType])
