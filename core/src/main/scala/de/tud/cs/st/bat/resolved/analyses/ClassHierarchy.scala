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
 * Represents the visible part of a project's class hierarchy.
 *
 * The visible part of a project's class hierarchy consists of all classes defined in
 * the analyzed class files and all boundary classes/interfaces. I.e., those classes
 * which are seen when analyzing a specific class file, but for which the respective
 * class file is not seen.
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
 * @note It is generally considered to be an error to pass an instance of an `ObjectType`s
 *      to any method if the `ObjectType` was not previously added!
 *
 * @define noIncrementalMaintenance Maintaining the class hierarchy after the
 *      change of previously analyzed class file is not supported.
 *
 * @author Michael Eichberg
 */
class ClassHierarchy(
    protected[this] val supertypes: Map[ObjectType, Set[ObjectType]] = Map(),
    protected[this] val subtypes: Map[ObjectType, Set[ObjectType]] = Map())
        extends (ObjectType ⇒ Option[(Set[ObjectType], Set[ObjectType])]) {

    type Supertypes = Set[ObjectType] // just a type alias
    type Subtypes = Set[ObjectType] // just a type alias

    /**
     * Returns the given type's supertypes and subtypes if the given type is known.
     */
    def apply(objectType: ObjectType): Option[(Supertypes, Subtypes)] =
        if (isKnown(objectType))
            Some((supertypes.apply(objectType), subtypes.apply(objectType)))
        else
            None

    /**
     * Returns `true` if the class hierarchy knows the given type.
     */
    def isKnown(objectType: ObjectType): Boolean = supertypes.contains(objectType)

    /**
     * Returns `true` if the class hierarchy does not know the given type.
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
    def +(classFile: ClassFile): ClassHierarchy = {
        this + (classFile.thisClass, classFile.superClass.toSeq ++ classFile.interfaces)
    }

    /**
     * Extends the class hierarchy.
     *
     * @note $noIncrementalMaintenance
     */
    def +(theNewSubtype: ObjectType,
          theNewSupertypes: Traversable[ObjectType]): ClassHierarchy = {

        val newSupertypes =
            supertypes.updated(
                theNewSubtype,
                supertypes.getOrElse(theNewSubtype, Set.empty) ++ theNewSupertypes)

        // we want to make sure that this type is seen even if there are no subtypes
        val subtypesWithTheNewSubtype = subtypes.updated(
            theNewSubtype,
            subtypes.getOrElse(theNewSubtype, Set.empty))
        val newSubtypes = (subtypesWithTheNewSubtype /: theNewSupertypes)(
            (newSubtypes, aNewSupertype) ⇒ {
                newSubtypes.updated(
                    aNewSupertype,
                    newSubtypes.getOrElse(aNewSupertype, Set.empty) + theNewSubtype)
            })

        new ClassHierarchy(newSupertypes, newSubtypes)
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
     * @note This set contains all types seen by the class hierarchy analysis, but
     *      it is not necessarily the case that the defining class file is available
     *      (`Project.classes("SOME ROOT TYPE")`). Imagine that you just analyze an
     *      application's class files. In this case it is extremely likely that you will have
     *      seen the type `java.lang.Object`, however the class file will not be available.
     */
    def rootTypes: Iterable[ObjectType] = {
        supertypes.view.filter((_: (ObjectType, Set[ObjectType]))._2.isEmpty).map(_._1)
    }

    /**
     * The classes (and interfaces if the given type is an interface type)
     * that '''directly''' inherit from the given type.
     *
     * If the class hierarchy does not contain any information about the given type
     * an exception is thrown.
     *
     * However, if you analyzed all class files of a project
     * and then ask for the subtypes of a specific type and an
     * empty set is returned, then you have the guarantee that no class in the
     * project '''directly''' inherits form the given type.
     *
     * @return The direct subtypes of the given type.
     */
    def subtypes(objectType: ObjectType): Set[ObjectType] = subtypes.apply(objectType)

    /**
     * The set of all classes (and interfaces) that (directly or indirectly)
     * inherit from the given type.
     *
     * @see `subtypes(ObjectType)` for general remarks about the
     *    precision of the analysis.
     * @return The set of all direct and indirect subtypes of the given type.
     * @note It may be more efficient to use `foreachSubtyp(ObjectType,ObjectType => Unit)`
     */
    def allSubtypes(objectType: ObjectType): Set[ObjectType] = {
        val theSubtypes = subtypes.apply(objectType)
        for {
            directSubtype ← theSubtypes
            indirectSubtype ← allSubtypes(directSubtype) + directSubtype
        } yield indirectSubtype
    }

    /**
     * Calls the function `f` for each (direct or indirect) subtype of the given type.
     */
    def foreachSubtype(objectType: ObjectType, f: ObjectType ⇒ Unit) {
        subtypes.apply(objectType) foreach { directSubtype ⇒
            f(directSubtype)
            foreachSubtype(directSubtype, f)
        }
    }

    /**
     * The classes and interfaces from which the given type directly inherits.
     *
     * The empty set will only be returned, if the class file of `java.lang.Object`
     * was analyzed, and the given object type represents `java.lang.Object`.
     * Recall, that interfaces always (implicitly) inherit from java.lang.Object.
     *
     * @return The direct supertypes of the given type.
     * @note It may be more efficient to use
     *      `foreachSupertype(ObjectType, ObjectType => Unit)`.
     */
    def supertypes(objectType: ObjectType): Set[ObjectType] = supertypes.apply(objectType)

    /**
     * Iterates over the given type's supertypes and calls the given function `f`
     * for each supertype.
     */
    def foreachSupertype(objectType: ObjectType, f: ObjectType ⇒ Unit) {
        supertypes.apply(objectType) foreach { supertype ⇒
            f(supertype)
            foreachSupertype(supertype, f)
        }
    }

    /**
     * Iterates over the given type's supertypes and tries to look up the supertype's
     * class file which is then passed to the given function `f`.
     */
    def foreachSuperclass(
        objectType: ObjectType,
        f: ClassFile ⇒ Unit,
        classes: ObjectType ⇒ Option[ClassFile]) {
        foreachSupertype(objectType, supertype ⇒ {
            classes(supertype) match {
                case Some(classFile) ⇒ f(classFile)
                case _               ⇒ /*Do nothing*/
            }
        })
    }

    /**
     * Returns the set of all classes/interfaces from which the given type inherits
     * and for which the respective class file is available.
     *
     * @note It may be more efficient to use `foreachSuperclass(ObjectType,
     *      ClassFile => Unit, ObjectType ⇒ Option[ClassFile])`
     *
     * @return An iterator over all class files of all super types of the given
     *      `objectType` that pass the given filter and for which the class file
     *      is available.
     */
    def superclasses(
        objectType: ObjectType,
        classFileFilter: ClassFile ⇒ Boolean,
        classes: ObjectType ⇒ Option[ClassFile]): Iterator[ClassFile] = {

        new Iterator[ClassFile] {

            var returnedSupertypes: Set[ObjectType] = Set.empty

            var nextSuperclasses: Traversable[ClassFile] = {
                var supertypes: Set[ObjectType] =
                    ClassHierarchy.this.supertypes.get(objectType).getOrElse(Set.empty)
                ClassHierarchy.lookupClassFiles(supertypes, classFileFilter, classes)
            }

            def hasNext: Boolean = nextSuperclasses.nonEmpty

            def next: ClassFile = {
                val nextSuperclass = nextSuperclasses.head
                nextSuperclasses = nextSuperclasses.tail
                nextSuperclasses ++= {
                    ClassHierarchy.lookupClassFiles(
                        supertypes.get(nextSuperclass.thisClass).getOrElse(Set.empty),
                        classFile ⇒ {
                            !returnedSupertypes.contains(classFile.thisClass) &&
                                classFileFilter(classFile)
                        },
                        classes)
                }

                returnedSupertypes += nextSuperclass.thisClass

                nextSuperclass
            }
        }
    }

    /**
     * Determines if the given `subtype` is actually a subtype of the class type
     * `supertype`.
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

        this.supertypes.get(subtype) match {
            case Some(intermediateTypes) if intermediateTypes.isEmpty ⇒
                // we have found a type without information about its supertypes
                // and this type is known not to be java.lang.Object (due to a previous test)  
                Unknown
            case Some(intermediateTypes) ⇒
                intermediateTypes.foreach { intermediateType ⇒
                    isSubtypeOf(intermediateType, theSupertype) match {
                        case Yes           ⇒ return Yes
                        case No            ⇒ /*do nothing*/
                        case _ /*Unknown*/ ⇒ return Unknown
                    }
                }
                No
            case _ ⇒ Unknown
        }
    }

    /**
     * Determines if `subtype` is a subtype of `supertype`.
     *
     * This method can be used as a foundation for implementing the logic of the JVM's
     * `instanceof` and `classcast` instructions. But, in that case additional logic
     * for handling `null` values and for considering the runtime type needs to be
     * implemented by the caller of this method.
     *
     * @param subtype A class or array type.
     * @param supertype A class or array type.
     * @return `Yes` if `subtype` is indeed a subtype of the given `supertype`. `No`
     *      if `subtype` is not a subtype of `supertype` and `Unknown` if the analysis is
     *      not conclusive. The latter can happen if the class hierarchy is not
     *      completely available and hence precise information about a type's supertypes
     *      is not available.
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
     * Calculates the set of classes and interfaces from which the given types
     * directly inherit.
     *
     * If the class file of a given object type was not previously analyzed
     * a `NoSuchElementException` will be raised.
     */
    def supertypes(objectTypes: Traversable[ObjectType]): Set[ObjectType] =
        (Set.empty[ObjectType] /: objectTypes)(_ ++ supertypes.apply(_))

    /**
     * Resolves a symbolic reference to a field. Basically, the search starts with
     * the given class `c` and then continues with `c`'s superinterfaces before the
     * search is continued with `c`'s superclass (as prescribed by the JVM specification
     * for the resolution of unresolved symbolic references.)
     *
     * Resolving a symbolic reference is particularly required to, e.g., get a field's
     * annotations or to get a field's value (if it is a constant value).
     *
     * @note This implementation does not check for `IllegalAccessError`. This check
     *      needs to be done by the caller. The same applies for the check that the
     *      field is non-static if get-/putfield is used and static if a get/putstatic is
     *      used to access the field. In the latter case the JVM would throw a
     *      `LinkingException`.
     *      Furthermore, if the field cannot be found it is the responsibility of the
     *      caller to handle that situation.
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
                classFile.interfaces.collectFirst { supertype ⇒
                    resolveFieldReference(supertype, fieldName, fieldType, classes) match {
                        case Some(resolvedFieldReference) ⇒ resolvedFieldReference
                    }
                } orElse {
                    classFile.superClass.flatMap { supertype ⇒
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
     * method can be accessed by the caller or if it is abstract. Additionally it is still
     * necessary that the caller makes a distinction between the statically (at compile time)
     * identified declaring class and the dynamic type of the receiver in case of
     * `invokevirtual` and `invokeinterface` instructions. I.e., additional processing
     * is necessary.
     *
     * @note Generally, if the type of the receiver is not precise the receiver object's
     *    subtypes should also be searched for method implementation (at least those
     *    classes that may be instantiated).
     *
     * @note This method just resolve a method reference. Additional checks,
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

            lookupMethodDefinition(receiverType, methodName, methodDescriptor, classes).
                orElse {
                    lookupMethodInSuperinterfaces(
                        classFile,
                        methodName,
                        methodDescriptor,
                        classes)
                }
        }
    }

    def resolveInterfaceMethodReference(
        receiverType: ObjectType,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        classes: ObjectType ⇒ Option[ClassFile]): Option[(ClassFile, Method)] = {

        classes(receiverType) flatMap { classFile ⇒
            assume(classFile.isInterfaceDeclaration)

            lookupMethodInInterface(classFile, methodName, methodDescriptor, classes).
                orElse {
                    lookupMethodDefinition(ObjectType.Object,
                        methodName,
                        methodDescriptor,
                        classes)
                }
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
                val result = lookupMethodInInterface(superclass, methodName, methodDescriptor, classes)
                if (result.isDefined) return result
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
     * If necessary such checks needs to be done by the caller.
     *
     * @note In case that you analyze static source code dependencies and if an invoke
     *    instruction refers to a method that is not declared by the receiver's class, then
     *    it might be more meaningful to still create a dependency to the receiver's class
     *    than to look up the actual definition in one of the receiver's super classes.
     *
     * @return `Some((ClassFile,Method))` if the method is found. `None` if the method
     *    is not found. This can happen under two circumstances:
     *    First, not all class files referred to/used by the project are (yet) analyzed;
     *    i.e., we do not have the complete view on all class files belonging to the
     *    project.
     *    Second, the analyzed class files do not belong together (they either belong to
     *    different projects or to incompatible versions of the same project.)
     */
    def lookupMethodDefinition(
        receiverType: ObjectType,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        classes: ObjectType ⇒ Option[ClassFile]): Option[(ClassFile, Method)] = {

        def lookupMethodDefinition(classFile: ClassFile): Option[(ClassFile, Method)] = {
            classFile.methods.collectFirst {
                case method @ Method(_, `methodName`, `methodDescriptor`, _) ⇒
                    (classFile, method)
            } orElse {
                classFile.superClass.flatMap { superclassType ⇒
                    classes(superclassType) flatMap { superclass ⇒
                        lookupMethodDefinition(superclass)
                    }
                }
            }
        }

        classes(receiverType) flatMap { classFile ⇒
            assume(!classFile.isInterfaceDeclaration)

            lookupMethodDefinition(classFile)
        }
    }

    /**
     * Returns a view of the class hierarchy as a graph (which can then be transformed
     * into a dot representation [[http://www.graphviz.org Graphviz]]). This
     * graph can be a multi-graph if we don't see the complete class hierarchy.
     */
    def toGraph: Node = new Node {

        val sourceElementIDs = new SourceElementIDsMap {}

        import sourceElementIDs.{ sourceElementID ⇒ id }

        private val nodes: Map[ObjectType, Node] =
            Map.empty ++ subtypes.keys.map { t ⇒
                val entry: (ObjectType, Node) = (
                    t,
                    new Node {
                        def uniqueId = id(t)
                        def toHRR: Option[String] = Some(t.className)
                        def foreachSuccessor(f: Node ⇒ _) {
                            subtypes.apply(t) foreach { subtype ⇒
                                f(nodes(subtype))
                            }
                        }
                        def hasSuccessors(): Boolean = subtypes.apply(t).nonEmpty
                    }
                )
                entry
            }

        // a virtual root node
        def uniqueId = -1
        def toHRR = None
        def foreachSuccessor(f: Node ⇒ _) {
            /**
             * We may not see the class files of all classes that are referred
             * to in the class files that we did see. Hence, we have to be able
             * to handle partial class hierarchies.
             */
            val rootTypes = nodes.filterNot { case (t, _) ⇒ supertypes.isDefinedAt(t) }
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
     *      you can just pass that object.
     */
    def lookupClassFiles(
        objectTypes: Traversable[ObjectType],
        filter: ClassFile ⇒ Boolean,
        classes: ObjectType ⇒ Option[ClassFile]): Traversable[ClassFile] = {
        objectTypes.map(classes(_)).filter(_.isDefined).map(_.get).filter(filter)
    }

    /**
     * The empty class hierarchy.
     */
    val empty: ClassHierarchy = new ClassHierarchy()

    /**
     * Creates a new ClassHierarchy object that predefines the type hierarchy related to
     * the exceptions thrown by specific Java bytecode instructions. See the file
     * ClassHierarchyJVMExceptions.ths (text file) for further details.
     */
    lazy val preInitializedClassHierarchy: ClassHierarchy = {
        import util.ControlAbstractions.process

        def processPredefinedClassHierarchy(
            fileName: String,
            initialClassHierarchy: ClassHierarchy): ClassHierarchy = {

            var classHierarchy = initialClassHierarchy
            process(getClass().getResourceAsStream(fileName)) { in ⇒

                val SpecLineExtractor = """(\S+)\s*>\s*(.+)""".r
                val source = new scala.io.BufferedSource(in)
                val specLines =
                    source.getLines.map(_.trim).filterNot(
                        l ⇒ l.startsWith("#") || l.length == 0
                    )
                for {
                    SpecLineExtractor(supertype, subtypes) ← specLines
                    supertypes = List(ObjectType(supertype))
                    subtype ← subtypes.split(",").map(_.trim)
                } {
                    classHierarchy += (ObjectType(subtype), supertypes)
                }
                classHierarchy
            }
        }

        processPredefinedClassHierarchy(
            "ClassHierarchyJVMExceptions.ths",
            processPredefinedClassHierarchy(
                "ClassHierarchyJLS.ths",
                empty))
    }
}

