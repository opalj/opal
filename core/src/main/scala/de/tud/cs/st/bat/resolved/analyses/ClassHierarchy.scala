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

import util.graphs.{ Node, toDot }
import util.{ Answer, Yes, No, Unknown }

/**
 * Encapsulates the type hierarchy information directly related to a specific type,
 * that is, its immediate super-/subtypes.
 *
 * @param objectType The type for which the type hierarchy information is made available.
 *    This information may be incomplete if not all of a Java project's own classes as
 *    well as dependent libraries are analyzed.
 *
 * @author Michael Eichberg
 */
case class TypeHierarchyInformation(
        objectType: ObjectType,
        superclassType: Option[ObjectType],
        superinterfaceTypes: Set[ObjectType],
        subclassTypes: Set[ObjectType],
        subinterfaceTypes: Set[ObjectType]) {

    /**
     * Returns `true` if the supertype information is complete; that is this method returns
     * `true` if the type was explicitly previously added. Returns `false` if this type
     * was referred to by some class that was previously added, but the defining class
     * file was not analyzed.
     */
    def isSupertypeInformationComplete: Boolean =
        superclassType.isDefined || objectType == ObjectType.Object
}

/**
 * Represents the visible part of a project's class hierarchy.
 *
 * Only the part of a project's class hierarchy is visible that is referred to in
 * the class declarations of the analyzed class. I.e., those classes
 * which are directly referred to in a classes declaration, but for which the respective
 * class file was not analyzed, are also considered to be visible. For example, if the 
 * class file of the class `java.util.ArrayList` is analyzed, the class hierarchy will 
 * have some preliminary information about, e.g., `java.util.List` from which `ArrayList` 
 * inherits. However, the information about `List` is incomplete and `List` will be a 
 * boundary class.
 *
 * ==Usage==
 * To build the class hierarchy use the `++` and `+` method.
 *
 * ==Thread safety==
 * This class is immutable. Hence, concurrent access to the class hierarchy is supported.
 *
 * However, this also means that an update of the class hierarchy results in a new
 * class hierarchy object and, therefore, some external synchronization
 * may be needed to make sure that the complete class hierarchy is constructed.
 * This decision was made to avoid any need for synchronization
 * once the class hierarchy is completely constructed.
 *
 * @param superclassTypes Contains type information about a type's immediate superclass.
 *      This value is always defined except of the case where the key identifies the
 *      object type `java.lang.Object` or when the respective class files was not
 *      analyzed and the respective type was only seen in another classe's declaration.
 * @param superinterfaceTypes Contains type information about a type's directly implemented
 *      interfaces; if any.
 * @param subclassTypes Contains type information about a type's subclasses; if any.
 * @param subinterfaceTypes Contains type information about a type's subinterfaces.
 *      They only ''class type'' that is allowed to have a non-empty set of subinterfaces
 *      is `java.lang.Object`.
 * @note It is generally considered to be an error to pass an instance of an `ObjectType`
 *      to any method if the `ObjectType` was not previously added!
 *
 * @define noIncrementalMaintenance Maintaining the class hierarchy after a
 *      change of a previously analyzed/added class file is not supported.
 *
 * @author Michael Eichberg
 */
class ClassHierarchy(
    val superclassType: Map[ObjectType, Option[ObjectType]] = Map(),
    val superinterfaceTypes: Map[ObjectType, Set[ObjectType]] = Map(),
    val subclassTypes: Map[ObjectType, Set[ObjectType]] = Map(),
    val subinterfaceTypes: Map[ObjectType, Set[ObjectType]] = Map())
        extends (ObjectType ⇒ Option[TypeHierarchyInformation]) {

    /**
     * The set of all interface types.
     */
    // We have to distinguish two cases:
    // 1) The interface type was directly added to the class hierarchy. In this case
    //    the interface is in the set of sub interfaces of java.lang.Object.
    // 2) The interface types was not directly added to the class hierarchy, but
    //    was implemented by another classe's/interface's declaration. In
    //    this case the interface type is only found in superinterfaceTypes.
    lazy val interfaceTypes: Set[ObjectType] =
        subinterfaceTypes(ObjectType.Object) ++ superinterfaceTypes.values.flatten.toSet

    def isInterface(objectType: ObjectType) = interfaceTypes.contains(objectType)

    /**
     * Returns true if the type hierarchy information w.r.t. the given types supertypes
     * is complete.
     */
    def isDirectSupertypeInformationComplete(objectType: ObjectType): Boolean =
        objectType == ObjectType.Object || {
            val superclassType = this.superclassType.get(objectType)
            superclassType.isDefined /* <=> the type is known */ &&
                superclassType.get.isDefined /* <=> the direct supertypes are all known */
        }

    /**
     * Returns the type hierarchy information related to the given type.
     */
    def apply(objectType: ObjectType): Option[TypeHierarchyInformation] =
        if (isKnown(objectType))
            Some(
                TypeHierarchyInformation(
                    objectType,
                    superclassType(objectType),
                    superinterfaceTypes.get(objectType).getOrElse(Set.empty),
                    subclassTypes.get(objectType).getOrElse(Set.empty),
                    subinterfaceTypes.get(objectType).getOrElse(Set.empty)
                )
            )
        else
            None

    /**
     * Returns `true` if the class hierarchy has some information about the given
     * type.
     */
    def isKnown(objectType: ObjectType): Boolean = superclassType.contains(objectType)

    /**
     * Returns `true` if the type is unknown.
     */
    def isUnknown(objectType: ObjectType): Boolean = !isKnown(objectType)

    /**
     * Analyzes the given class files and extends the current class hierarchy.
     *
     * @note $noIncrementalMaintenance
     */
    def ++(classFiles: Traversable[ClassFile]): ClassHierarchy =
        (this /: classFiles)(_ + _)

    /**
     * Analyzes the given class file and extends the current class hierarchy.
     *
     *  @note $noIncrementalMaintenance
     */
    def +(classFile: ClassFile): ClassHierarchy =
        this + (
            classFile.thisClass,
            classFile.isInterfaceDeclaration,
            classFile.superClass,
            classFile.interfaces.toSet)

    /**
     * Extends the class hierarchy.
     *
     * @note $noIncrementalMaintenance
     */
    private def +(
        objectType: ObjectType,
        isInterfaceType: Boolean,
        theSuperclassType: Option[ObjectType],
        theSuperinterfaceTypes: Set[ObjectType]): ClassHierarchy = {

        var newSuperclassType =
            superclassType.updated(objectType, theSuperclassType)

        val newSuperinterfaceTypes =
            if (theSuperinterfaceTypes.isEmpty)
                superinterfaceTypes
            else
                superinterfaceTypes.updated(objectType, theSuperinterfaceTypes)

        //
        // establish subtype information; i.e, for each super(class|interface)type make 
        // sure that it is "seen" in "superclassType" 
        //
        if (theSuperclassType.isDefined &&
            newSuperclassType.get(theSuperclassType.get).isEmpty)
            // we set it 
            newSuperclassType = newSuperclassType.updated(theSuperclassType.get, None)
        theSuperinterfaceTypes.foreach { aSuperinterfaceType ⇒
            if (newSuperclassType.get(aSuperinterfaceType).isEmpty)
                newSuperclassType = newSuperclassType.updated(aSuperinterfaceType, None)
        }

        if (isInterfaceType) {
            var newSubinterfaceTypes =
                // an interface always has `java.lang.Object` as its super class
                subinterfaceTypes.updated(
                    theSuperclassType.get,
                    subinterfaceTypes.get(theSuperclassType.get).getOrElse(Set.empty) + objectType)
            theSuperinterfaceTypes.foreach { aSuperinterfaceType ⇒
                newSubinterfaceTypes = newSubinterfaceTypes.updated(
                    aSuperinterfaceType,
                    newSubinterfaceTypes.get(aSuperinterfaceType).getOrElse(Set.empty) + objectType)
            }
            new ClassHierarchy(
                newSuperclassType,
                newSuperinterfaceTypes,
                subclassTypes,
                newSubinterfaceTypes)
        } else {
            var newSubclassTypes =
                if (theSuperclassType.isDefined) {
                    subclassTypes.updated(
                        theSuperclassType.get,
                        subclassTypes.get(theSuperclassType.get).getOrElse(Set.empty) + objectType)
                } else
                    subclassTypes
            theSuperinterfaceTypes.foreach { aSuperinterfaceType ⇒
                newSubclassTypes = newSubclassTypes.updated(
                    aSuperinterfaceType,
                    newSubclassTypes.get(aSuperinterfaceType).getOrElse(Set.empty) + objectType)
            }
            new ClassHierarchy(
                newSuperclassType,
                newSuperinterfaceTypes,
                newSubclassTypes,
                subinterfaceTypes)
        }

    }

    /**
     * Calculates this project's root types. A Java project's root types
     * generally only contains the single class `java.lang.Object`. However, if an
     * analysis only analyzes a subset of all classes of an application then it may
     * be possible that multiple root types exist. E.g., if you define a
     * class that inherits from some not-analyzed library class then
     * it will be considered as a root type. Recall that every interface inherits
     * from `java.lang.Object` and is therefore never a root type.
     *
     * @note This set contains types seen by the class hierarchy analysis, but
     *      it is not necessarily the case that the defining class file is available
     *      (`Project.classes("SOME ROOT TYPE")`). Imagine that you just analyze an
     *      application's class files and not also the JRE. In this case it is extremely
     *      likely that you will have seen the type `java.lang.Object`, however the
     *      class file will not be available.
     */
    def rootTypes: Iterable[ObjectType] =
        superclassType.view.filter(c_sc ⇒ c_sc._2.isEmpty).map(_._1)

    /**
     * The set of all class- and interface-types that (directly or indirectly)
     * inherit from the given type. The given type is not included in the returned
     * set.
     *
     * @return The set of all direct and indirect subtypes of the given type.
     * @note If you don't need the set, it is more efficient to use `foreachSubtype
     */
    def allSubtypes(objectType: ObjectType): Set[ObjectType] = {
        var subtypes = Set.empty[ObjectType]
        foreachSubtype(objectType) { subtype ⇒ subtypes += subtype }
        subtypes
    }

    /**
     * Calls the function `f` for each (direct or indirect) subtype of the given type.
     *
     * @param objectType An object type.
     */
    def foreachSubtype(objectType: ObjectType)(f: ObjectType ⇒ Unit) {
        subclassTypes.get(objectType) foreach { subclassTypes ⇒
            subclassTypes foreach { subclassType ⇒
                f(subclassType)
                foreachSubtype(subclassType)(f)
            }
        }
        subinterfaceTypes.get(objectType) foreach { subinterfaceTypes ⇒
            subinterfaceTypes foreach { subinterfaceType ⇒
                f(subinterfaceType)
                foreachSubtype(subinterfaceType)(f)
            }
        }
    }

    /**
     * Calls the given function `f` for each of the given type's supertypes.
     * It is possible that the same super interface type `I` is passed multiple
     * times to `f` when `I` is implemented multiple times by the given type's supertypes.
     *
     * The algorithm first iterates over the type's super classes
     * before it iterates over the super interfaces.
     *
     * @param objectType A type known to the class hierarchy.
     */
    def foreachSupertype(objectType: ObjectType)(f: ObjectType ⇒ Unit) {
        superclassType(objectType).foreach { superclassType ⇒
            f(superclassType)
            foreachSupertype(superclassType)(f)
        }

        superinterfaceTypes.get(objectType) foreach { superinterfaceTypes ⇒
            superinterfaceTypes foreach { superinterfaceType ⇒
                f(superinterfaceType)
                foreachSupertype(superinterfaceType)(f)
            }
        }
    }

    def allSupertypes(objectType: ObjectType): Set[ObjectType] = {
        var supertypes = Set.empty[ObjectType]
        foreachSupertype(objectType) { supertypes += _ }
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
     * @param objectType A type known to the class hierarchy.
     */
    def foreachSuperclass(
        objectType: ObjectType,
        classes: ObjectType ⇒ Option[ClassFile])(
            f: ClassFile ⇒ Unit): Unit = {
        foreachSupertype(objectType) { supertype ⇒
            classes(supertype) match {
                case Some(classFile) ⇒ f(classFile)
                case _               ⇒ /*Do nothing*/
            }
        }
    }

    /**
     * Returns the set of all classes/interfaces from which the given type inherits
     * and for which the respective class file is available.
     *
     * @note It may be more efficient to use `foreachSuperclass(ObjectType,
     *      ClassFile => Unit, ObjectType ⇒ Option[ClassFile])`
     *
     * @return An iterable over all class files of all super types of the given
     *      `objectType` that pass the given filter and for which the class file
     *      is available. The iterable guarantees
     */
    def superclasses(
        objectType: ObjectType,
        classes: ObjectType ⇒ Option[ClassFile])(
            classFileFilter: ClassFile ⇒ Boolean = { _ ⇒ true }): Iterable[ClassFile] = {
        // We want to make sure that every class file is returned only once,
        // but we want to avoid equals calls on `ClassFile` objects. 
        var classFiles = Map[ObjectType, ClassFile]()
        foreachSuperclass(objectType, classes) { classFile ⇒
            if (classFileFilter(classFile))
                classFiles = classFiles.updated(classFile.thisClass, classFile)
        }
        classFiles.values
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
     * @return `Yes` if `subtype` is a subtype of the given `supertype`. `No`
     *      if `subtype` is not a subtype of `supertype` and `Unknown` if the analysis is
     *      not conclusive. The latter can happen if the class hierarchy is not
     *      completely available and hence precise information about a type's supertypes
     *      is not available.
     */
    def isSubtypeOf(subtype: ObjectType, theSupertype: ObjectType): Answer = {
        if (subtype == theSupertype || theSupertype == ObjectType.Object)
            return Yes

        if (subtype == ObjectType.Object /* && theSupertype != ObjectType.Object*/ )
            return No

        val subtypeIsInterface = interfaceTypes.contains(subtype)
        val supertypeIsInterface = interfaceTypes.contains(theSupertype)

        if (subtypeIsInterface && !supertypeIsInterface)
            // An interface always (only) directly inherits from java.lang.Object
            // and this is already checked before.
            return No

        def implementsInterface(subinterfaceType: ObjectType, theSupertype: ObjectType): Answer = {
            if (subinterfaceType == theSupertype)
                return Yes

            superinterfaceTypes.get(subinterfaceType) match {
                case None ⇒
                    if (isDirectSupertypeInformationComplete(subinterfaceType))
                        No
                    else
                        Unknown
                case Some(intermediateTypes) ⇒
                    var answer: Answer = No
                    intermediateTypes.foreach { intermediateType ⇒
                        var anotherAnswer = implementsInterface(intermediateType, theSupertype)
                        if (anotherAnswer.yes)
                            return Yes
                        answer &= anotherAnswer
                    }
                    answer
            }
        }

        def isSubtypeOf(subclassType: ObjectType): Answer = {
            def inheritsFromInterface(answerSoFar: Answer): Answer = {
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

            superclassType.get(subclassType) match {
                case Some(Some(intermediateType)) ⇒
                    if (intermediateType == theSupertype)
                        return Yes

                    var answer = isSubtypeOf(intermediateType)
                    if (answer.yes)
                        Yes
                    else
                        inheritsFromInterface(answer)

                case Some(None) ⇒
                    /* we have reached the top (visible) class Type*/
                    var answer: Answer =
                        if (subclassType == ObjectType.Object)
                            No
                        else
                            Unknown
                    inheritsFromInterface(answer)

                case None ⇒
                    /*the given subclassType is unknown*/
                    Unknown
            }
        }

        if (subtypeIsInterface /*&& supertypeIsInterface*/ )
            implementsInterface(subtype, theSupertype)
        else
            isSubtypeOf(subtype)
    }

    /**
     * Determines if `subtype` is a subtype of `supertype` given the available
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
     * @note The answer `No` does not necessarily imply that two runtime values for
     *    which the given types are only upper boundaries are not (w.r.t. their
     *    runtime types) in a subtype relation. E.g., if `subtype` denotes the type
     *    `java.util.List` and `supertype` denotes the type `java.util.ArrayList` then
     *    the answer is clearly `No`. But, at runtime, this may not be the case.
     */
    def isSubtypeOf(subtype: ReferenceType, supertype: ReferenceType): Answer = {
        if (subtype == supertype || supertype == ObjectType.Object)
            return Yes

        subtype match {
            case ObjectType.Object ⇒ No // the given supertype has to be a subtype...
            case ot: ObjectType ⇒
                if (supertype.isArrayType)
                    No
                else
                    // The analysis is conclusive iff we can get all supertypes
                    // for the given type (ot) up until "java/lang/Object"; i.e.,
                    // if there are no holes.
                    isSubtypeOf(ot.asObjectType, supertype.asObjectType)
            case ArrayType(componentType) ⇒ {
                supertype match {
                    case ObjectType.Serializable ⇒ Yes
                    case ObjectType.Cloneable    ⇒ Yes
                    case _: ObjectType           ⇒ No
                    case ArrayType(superComponentType: BaseType) ⇒
                        if (componentType == superComponentType)
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
    }

    /**
     * Resolves a symbolic reference to a field. Basically, the search starts with
     * the given class `c` and then continues with `c`'s superinterfaces before the
     * search is continued with `c`'s superclass (as prescribed by the JVM specification
     * for the resolution of unresolved symbolic references.)
     *
     * Resolving a symbolic reference is particularly required to, e.g., get a field's
     * annotations or to get a field's value (if it is static and a constant value).
     *
     * @note This implementation does not check for `IllegalAccessError`. This check
     *      needs to be done by the caller. The same applies for the check that the
     *      field is non-static if get-/putfield is used and static if a get/putstatic is
     *      used to access the field. In the latter case the JVM would throw a
     *      `LinkingException`.
     *      Furthermore, if the field cannot be found it is the responsibility of the
     *      caller to handle that situation.
     *
     * @note Resolution is final. I.e., either this algorithm has found the defining field
     *      or the field is not defined by one of the loaded classes. Searching for the
     *      field in subclasses is not meaningful as Java does not do dynamic field
     *      reference resolution. For further details study the following code:
     *      {{{
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
     *      }}}
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
        classes: ObjectType ⇒ Option[ClassFile]): Option[(ClassFile, Field)] = {

        // More details: JVM 7 Spec. Section 5.4.3.2 

        classes(c) flatMap { classFile ⇒
            classFile.fields.collectFirst {
                case field @ Field(_, `fieldName`, `fieldType`, _) ⇒ (classFile, field)
            } orElse {
                classFile.interfaces collectFirst { supertype ⇒
                    resolveFieldReference(supertype, fieldName, fieldType, classes) match {
                        case Some(resolvedFieldReference) ⇒ resolvedFieldReference
                    }
                } orElse {
                    classFile.superClass flatMap { supertype ⇒
                        resolveFieldReference(supertype, fieldName, fieldType, classes)
                    }
                }
            }
        }
    }

    /**
     * Tries to resolve a method reference as specified by the JVM specification.
     * I.e., the algorithm tries to find the class that actually declares the referenced
     * method.
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
     *    subtypes should also be searched for method implementation (at least those
     *    classes that may be instantiated).
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
        classes: ObjectType ⇒ Option[ClassFile]): Option[(ClassFile, Method)] = {

        // TODO [Java 7] Implement support for handling signature polymorphic method resolution.

        classes(receiverType) flatMap { classFile ⇒
            assume(!classFile.isInterfaceDeclaration)

            lookupMethodDefinition(
                receiverType,
                methodName,
                methodDescriptor,
                classes
            ) orElse
                lookupMethodInSuperinterfaces(
                    classFile,
                    methodName,
                    methodDescriptor,
                    classes
                )
        }
    }

    def resolveInterfaceMethodReference(
        receiverType: ObjectType,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        classes: ObjectType ⇒ Option[ClassFile]): Option[(ClassFile, Method)] = {

        classes(receiverType) flatMap { classFile ⇒
            assume(classFile.isInterfaceDeclaration)

            lookupMethodInInterface(
                classFile,
                methodName,
                methodDescriptor,
                classes
            ) orElse
                lookupMethodDefinition(
                    ObjectType.Object,
                    methodName,
                    methodDescriptor,
                    classes
                )
        }
    }

    def lookupMethodInInterface(
        classFile: ClassFile,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        classes: ObjectType ⇒ Option[ClassFile]): Option[(ClassFile, Method)] = {

        classFile.methods.collectFirst {
            case method @ Method(_, `methodName`, `methodDescriptor`, _) ⇒
                (classFile, method)
        } orElse {
            lookupMethodInSuperinterfaces(classFile, methodName, methodDescriptor, classes)
        }
    }

    def lookupMethodInSuperinterfaces(
        classFile: ClassFile,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        classes: ObjectType ⇒ Option[ClassFile]): Option[(ClassFile, Method)] = {

        classFile.interfaces.foreach { superinterface: ObjectType ⇒
            classes(superinterface) map { superclass ⇒
                val result =
                    lookupMethodInInterface(
                        superclass,
                        methodName,
                        methodDescriptor,
                        classes)
                if (result.isDefined)
                    return result
            }
        }
        None
    }

    /**
     * Looks up the class file and method which actually declares the method that is
     * referred to by the given receiver type, method name and method descriptor. Given
     * that we are searching for method declarations the search is limited to the
     * superclasses of the class of the given receiver type.
     *
     * This method does not take visibility modifiers or the static modifier into account.
     * If necessary, such checks needs to be done by the caller.
     *
     * @note In case that you analyze static source code dependencies and if an invoke
     *    instruction refers to a method that is not declared by the receiver's class, then
     *    it might be more meaningful to still create a dependency to the receiver's class
     *    than to look up the actual definition in one of the receiver's super classes.
     *
     * @return `Some((ClassFile,Method))` if the method is found. `None` if the method
     *    is not found. This can basically happen under three circumstances:
     *    First, not all class files referred to/used by the project are (yet) analyzed;
     *    i.e., we do not have the complete view on all class files belonging to the
     *    project.
     *    The function classes did not return the class file that defines a specific
     *    objectType for some (anaylsis-specific) reason.
     *    Third, the analyzed class files do not belong together (they either belong to
     *    different projects or to incompatible versions of the same project.)
     */
    def lookupMethodDefinition(
        receiverType: ObjectType,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        classes: ObjectType ⇒ Option[ClassFile]): Option[(ClassFile, Method)] = {

        assume(!isInterface(receiverType))

        def lookupMethodDefinition(
            receiverType: ObjectType): Option[(ClassFile, Method)] = {
            classes(receiverType).flatMap { classFile ⇒
                classFile.methods.collectFirst {
                    case method @ Method(_, `methodName`, `methodDescriptor`, _) ⇒
                        (classFile, method)
                }
            } orElse {
                superclassType(receiverType) flatMap { superclassType ⇒
                    lookupMethodDefinition(superclassType)
                }
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
     *  @param isCandidate
     *  @param classes A function to get a type's implementing class file. (Usually
     *       an instance of the class [[de.tud.cs.st.bat.resolved.analyses.Project]]).
     *       This method expectes unrestricted access to the pool of all class files.
     *  @param classesFiler A function that is expected to return true, if the runtime type of
     *       the `receiverType` may be of the type defined by the given object type. For
     *       example, if you analyze a project and perform a lookup of all methods that
     *       implement the method `toString`, then set would be very large. But, if you
     *       know that only instances of the class (e.g.) `ArrayList` have been created
     *       (up to the point in your analysis where you call this method), it is
     *       meaningful to sort out all other classes (such as `Vector`).
     */
    def lookupImplementingMethods(
        receiverType: ObjectType,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        classes: ObjectType ⇒ Option[ClassFile],
        classesFilter: ObjectType ⇒ Boolean = { _ ⇒ true }): Iterable[(ClassFile, Method)] = {

        var implementingMethods: List[(ClassFile, Method)] =
            {
                if (isInterface(receiverType))
                    lookupMethodDefinition(
                        ObjectType.Object, // to handle calls such as toString on a (e.g.) "java.util.List"
                        methodName,
                        methodDescriptor,
                        classes)
                else
                    lookupMethodDefinition(
                        receiverType,
                        methodName,
                        methodDescriptor,
                        classes)
            } match {
                case Some(method) ⇒ List(method)
                case None         ⇒ List.empty
            }

        // Search all subclasses
        var seenSubtypes = Set.empty[ObjectType]
        foreachSubtype(receiverType) { (subtype: ObjectType) ⇒
            if (!seenSubtypes.contains(subtype)) {
                seenSubtypes += subtype
                if (classesFilter(subtype)) {
                    classes(subtype) foreach { classFile ⇒
                        classFile.methods collectFirst {
                            case method @ Method(_, `methodName`, `methodDescriptor`, _) ⇒
                                (classFile, method)
                        } match {
                            case Some(anImplementation) ⇒
                                implementingMethods = anImplementation :: implementingMethods
                            case _ ⇒
                            /*don't care*/
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
    def directSubtypesOf(objectType: ObjectType): Set[ObjectType] =
        subclassTypes.getOrElse(objectType, Set.empty[ObjectType]) ++
            subinterfaceTypes.getOrElse(objectType, Set.empty[ObjectType])

    /**
     * Returns a view of the class hierarchy as a graph (which can then be transformed
     * into a dot representation [[http://www.graphviz.org Graphviz]]). This
     * graph can be a multi-graph if we don't see the complete class hierarchy.
     */
    def toGraph: Node = new Node {

        val sourceElementIDs = new SourceElementIDsMap {}

        import sourceElementIDs.{ sourceElementID ⇒ id }

        private val nodes: Map[ObjectType, Node] =
            Map.empty ++ (superclassType.keys).map { aType ⇒
                val entry: (ObjectType, Node) = (
                    aType,
                    new Node {
                        private val directSubtypes = directSubtypesOf(aType)
                        def uniqueId = id(aType)
                        def toHRR: Option[String] = Some(aType.toJava)
                        def backgroundColor: Option[String] =
                            if (interfaceTypes.contains(aType))
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
                entry
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
            val rootTypes = nodes filter { case (t, _) ⇒ superclassType(t).isEmpty }
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

    /**
     * Returns all available `ClassFile` objects for the given `objectTypes` that
     * pass the given `filter`. `ObjectType`s for which no `ClassFile` is available
     * are ignored.
     *
     * @param classes A function that returns the `ClassFile` object that defines
     *      the given `ObjectType`, if available.
     *      If you have a [[de.tud.cs.st.bat.resolved.analyses.Project]] object
     *      you can just use the `Project` object.
     */
    def lookupClassFiles(
        objectTypes: Traversable[ObjectType],
        classes: ObjectType ⇒ Option[ClassFile])(
            filter: ClassFile ⇒ Boolean): Traversable[ClassFile] =
        objectTypes.map(classes(_)).filter(_.isDefined).map(_.get).filter(filter)

    /**
     * The empty class hierarchy.
     *
     * @note It is generally not recommended to use an empty class hierarchy unless
     *    you also (also) analyze the JDK.
     */
    def empty: ClassHierarchy = new ClassHierarchy()

    /**
     * Creates a new ClassHierarchy object that predefines the type hierarchy related to
     * the exceptions thrown by specific Java bytecode instructions. See the file
     * ClassHierarchyJVMExceptions.ths (text file) for further details.
     */
    def preInitializedClassHierarchy: ClassHierarchy = {
        var theInitialClassHierarchy = ClassHierarchy.empty
        theInitialClassHierarchy =
            processPredefinedClassHierarchy(
                getClass().getResourceAsStream("ClassHierarchyJLS.ths"),
                theInitialClassHierarchy
            )
        theInitialClassHierarchy =
            processPredefinedClassHierarchy(
                getClass().getResourceAsStream("ClassHierarchyJVMExceptions.ths"),
                theInitialClassHierarchy
            )
        theInitialClassHierarchy
    }

    def processPredefinedClassHierarchy(
        createInputStream: ⇒ java.io.InputStream,
        classHierarchy: ClassHierarchy): ClassHierarchy = {
        import util.ControlAbstractions.process

        var updatedClassHierarchy = classHierarchy
        process(createInputStream) { in ⇒
            val SpecLineExtractor =
                """(class|interface)\s+(\S+)(\s+extends\s+(\S+)(\s+implements\s+(.+))?)?""".r

            val source = new scala.io.BufferedSource(in)
            val specLines =
                source.getLines.map(_.trim).filterNot {
                    l ⇒ l.startsWith("#") || l.length == 0
                }
            for {
                SpecLineExtractor(typeKind, theType, _, superclassType, _, superinterfaceTypes) ← specLines
            } {
                updatedClassHierarchy += (
                    ObjectType(theType),
                    typeKind == "interface",
                    Option(superclassType).map(ObjectType(_)),
                    Option(superinterfaceTypes).map { superinterfaceTypes ⇒
                        superinterfaceTypes.split(",").map(_.trim).map(ObjectType(_)).toSet
                    }.getOrElse(Set.empty)
                )
            }
        }
        updatedClassHierarchy
    }
}

