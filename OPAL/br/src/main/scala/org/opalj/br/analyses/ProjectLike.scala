/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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
package analyses

import scala.annotation.tailrec

import scala.collection.mutable

//import org.opalj.log.LogContext
import org.opalj.br.instructions.FieldAccess
import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.collection.immutable.Result
import org.opalj.collection.immutable.Failure
import org.opalj.collection.immutable.Success
import org.opalj.collection.immutable.Empty
import org.opalj.collection.immutable.UIDSet
import org.opalj.collection.immutable.UIDSet0
//import org.opalj.br.instructions.INVOKESTATIC

/**
 * Enables project wide lookups of methods and fields.
 *
 * @note 	The current implementation is based on the '''correct project assumption''';
 * 			i.e., if the bytecode as a whole is not valid, the result is generally undefined.
 * 			One example would be, if we have two interfaces which define a non-abstract method
 * 			with the same signature and both interfaces are implemented by a third interface
 * 			which does not override these methods. In this case the result of a
 * 			[[resolveMethodReference]] is not defined, because the code base as a whole is not
 * 			valid.
 *
 * @note
 *
 * @author Michael Eichberg
 */
trait ProjectLike extends ClassFileRepository { project ⇒

    implicit def classHierarchy: ClassHierarchy

    /**
     * Returns the class file of `java.lang.Object` if available.
     */
    val ObjectClassFile: Option[ClassFile]

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    //
    //
    // RESOLVING FIELD REFERENCES
    //
    //
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    /**
     * Resolves a symbolic reference to a field. Basically, the search starts with
     * the given class `c` and then continues with `c`'s superinterfaces before the
     * search is continued with `c`'s superclass (as prescribed by the JVM specification
     * for the resolution of unresolved symbolic references.)
     *
     * Resolving a symbolic reference is particularly required to, e.g., get a field's
     * annotations or to get a field's value (if it is `static`, `final` and has a
     * constant value).
     *
     * @note    This implementation does not check for `IllegalAccessError`. This check
     *          needs to be done by the caller. The same applies for the check that the
     *          field is non-static if get-/putfield is used and static if a get/putstatic is
     *          used to access the field. In the latter case the JVM would throw a
     *          `LinkingException`.
     *          Furthermore, if the field cannot be found, it is the responsibility of the
     *          caller to handle that situation.
     * @note    Resolution is final. I.e., either this algorithm has found the defining field
     *          or the field is not defined by one of the loaded classes. Searching for the
     *          field in subclasses is not meaningful as it is not possible to override
     *          fields.
     *
     * @param   declaringClassType The class (or a superclass thereof) that is expected
     *          to define the reference field.
     * @param   fieldName The name of the accessed field.
     * @param   fieldType The type of the accessed field (the field descriptor).
     * @param   project The project associated with this class hierarchy.
     * @return  The field that is referred to; if any. To get the defining `ClassFile`
     *          you can use the `project`.
     */
    def resolveFieldReference(
        declaringClassType: ObjectType,
        fieldName:          String,
        fieldType:          FieldType
    ): Option[Field] = {
        // More details: JVM 7/8 Spec. Section 5.4.3.2
        project.classFile(declaringClassType) flatMap { classFile ⇒
            classFile findField (fieldName, fieldType) orElse {
                classFile.interfaceTypes collectFirst { supertype ⇒
                    resolveFieldReference(supertype, fieldName, fieldType) match {
                        case Some(resolvedFieldReference) ⇒ resolvedFieldReference
                    }
                } orElse {
                    classFile.superclassType flatMap { supertype ⇒
                        resolveFieldReference(supertype, fieldName, fieldType)
                    }
                }
            }
        }
    }

    /**
     * @see `resolveFieldReference(ObjectTypeString,FieldType):Option[Field]`
     */
    final def resolveFieldReference(fieldAccess: FieldAccess): Option[Field] = {
        resolveFieldReference(fieldAccess.declaringClass, fieldAccess.name, fieldAccess.fieldType)
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    //
    //
    // RESOLVING METHOD REFERENCES / DETERMING THE INVOKED METHOD(S)
    //
    //
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    /**
     * Stores for each non-private, non-final instance method that is not an instance
     * initialization method the set of methods which override the specific method. To safe
     * memory, methods which are not overridden are not stored in the may.
     */
    protected[this] val overridingMethods: scala.collection.Map[Method, Set[Method]]

    def overriddenBy(m: Method): Set[Method] = {
        assert(!m.isPrivate, s"private methods $m cannot be overridden")
        assert(!m.isStatic, s"static methods $m cannot be overridden")
        assert(!m.isInitializer, s"initializers $m cannot be overridden")

        overridingMethods.get(m).getOrElse(Set.empty)
    }

    /* GENERAL NOTES
     *
     * (Accessibilty checks are done by the JVM when the method is resolved; this is, however not
     * done by the following methods as it does not affect the search for the potential target
     * methods.)
     *
     * Invokestatic 	=> 	the resolved method is called (if it is accessible, static etc...)
     * Invokespecial 	=>	the resolved (interface) method is called if it is a constructor
     * 						call, or the resolved method belongs to the calling class (calls of
     * 						private methods). Otherwise, the calling class' supertypes are
     * 						searched for a method with the same signature up until the resolved
     * 						method. When we search the type hierarchy upwards, we first search
     * 						for a method defined by a superclass (unless the current class defines
     * 						an interface type) before we search the interfaces. In the later
     * 						case we compute the set of the maximally specific matching methods
     * 						and select THE concrete one (it is an error if multiple concrete ones
     * 						exist!)
     * Invokevirtual	=>	the resolved method is called, if the resolved method is signature
     * 						polymorphic; in this case the concrete type of the method receiver
     * 						is not relevant. Otherwise, the type of the receiver is used to
     * 						start searching for the method that is to be invoked. That method
     * 						however, has to override the resolved method (which is generally not
     * 						the case if the method is private; and is only the case if the resolved
     * 						method and the current type belong to the same package in case of
     * 						a method with default visibility).
     * Invokeinterface 	=>	the resolved interface method just defines an upper bound; the
     * 						called method is determined as in case of invokevirtual; but
     * 						signature polymorphic calls are not relevant
     */

    /**
     * Tries to resolve a method reference as specified by the JVM specification.
     * I.e., the algorithm tries to find the class that actually declares the referenced
     * method. Resolution of '''signature polymorphic''' method calls is also
     * supported.
     *
     * This method is the basis for the implementation of the semantics
     * of the `invokeXXX` instructions. However, it does not check whether the resolved
     * method can be accessed by the caller or if it is abstract. Additionally, it is still
     * necessary that the caller makes a distinction between the statically
     * (at compile time) identified declaring class and the dynamic type of the receiver
     * in case of `invokevirtual` and `invokeinterface` instructions. I.e.,
     * additional processing is necessary on the client side.
     *
     * @note    This method just resolves a method reference. Additional checks,
     *          such as whether the resolved method is accessible, may be necessary.
     *
     * @param   receiverType The type of the object that receives the method call. The
     *          type must be a class type and must not be an interface type.
     *          I.e., no check w.r.t. a potential `IncompatibleClassChangeError` is done
     *          by this method.
     * @param   lookupInSuperinterfacesOnFailure If true (default: false) the method tries
     *          to look up the method in a super interface if it can't find it in the available
     *          super classes. (This setting is only relevant if the class hierarchy is not
     *          complete.)
     * @return  The resolved method `Some(`'''METHOD'''`)` or `None`.
     *          To get the defining class file use the project's respective method.
     */
    def resolveMethodReference(
        receiverType:                     ObjectType,
        name:                             String,
        descriptor:                       MethodDescriptor,
        lookupInSuperinterfacesOnFailure: Boolean          = false
    ): Option[Method] = {
        assert(classHierarchy.isInterface(receiverType).isNoOrUnknown)

        resolveClassMethodReference(receiverType, name, descriptor) match {
            case Success(method)                              ⇒ Some(method)
            case Failure if !lookupInSuperinterfacesOnFailure ⇒ None
            case _ /*Empty | (Failure && lookupInSuperinterfacesOnFailure) */ ⇒
                val superinterfaceTypes = classHierarchy.superinterfaceTypes(receiverType).get
                val (_, methods) =
                    findMaximallySpecificSuperinterfaceMethods(
                        superinterfaceTypes, name, descriptor,
                        analyzedSuperinterfaceTypes = UIDSet0
                    )
                methods.headOption // either it is THE max. specific method or some ...
        }
    }

    def resolveInterfaceMethodReference(
        receiverType: ObjectType,
        name:         String,
        descriptor:   MethodDescriptor
    ): Option[Method] = {
        assert(classHierarchy.isInterface(receiverType).isYesOrUnknown)

        def lookupInObject(): Option[Method] = {
            ObjectClassFile flatMap { classFile ⇒
                classFile.findMethod(name, descriptor) filter { m ⇒ m.isPublic && !m.isStatic }
            }
        }

        project.classFile(receiverType) flatMap { classFile ⇒
            assert(classFile.isInterfaceDeclaration)
            classFile.findMethod(name, descriptor) orElse {
                lookupInObject() orElse {
                    classHierarchy.superinterfaceTypes(receiverType) flatMap { superinterfaceTypes ⇒
                        val (_, methods) = findMaximallySpecificSuperinterfaceMethods(
                            superinterfaceTypes, name, descriptor, UIDSet0
                        )
                        methods.headOption
                    }
                }
            }
        }
    }

    /**
     * Computes the set of maximally specific superinterface methods with the
     * given name and descriptor.
     *
     * @note 	'''This method does not consider methods defined by `java.lang.Object`'''!
     * 			Those methods have precedence over respective methods defined by
     * 			superinterfaces! A corresponding check needs to be done before calling
     * 			this method.
     */
    def findMaximallySpecificSuperinterfaceMethods(
        superinterfaceType:          ObjectType,
        name:                        String,
        descriptor:                  MethodDescriptor,
        analyzedSuperinterfaceTypes: UIDSet[ObjectType] = UIDSet0
    ): ( /*analyzed types*/ UIDSet[ObjectType], Set[Method]) = {

        val newAnalyzedSuperinterfaceTypes = analyzedSuperinterfaceTypes + superinterfaceType

        // the superinterfaceTypes in which it is potentially relevant to search for methods
        val superinterfaceTypes: UIDSet[ObjectType] =
            classHierarchy.superinterfaceTypes(superinterfaceType).getOrElse(UIDSet0) --
                analyzedSuperinterfaceTypes

        project.classFile(superinterfaceType) match {
            case Some(classFile) ⇒
                assert(classFile.isInterfaceDeclaration)

                classFile.findMethod(name, descriptor) match {
                    case Some(method) if !method.isPrivate && !method.isStatic ⇒
                        val analyzedTypes = newAnalyzedSuperinterfaceTypes ++ superinterfaceTypes
                        (analyzedTypes, Set(method))

                    case None ⇒
                        if (superinterfaceTypes.isEmpty) {
                            (newAnalyzedSuperinterfaceTypes, Set.empty)
                        } else if (superinterfaceTypes.isSingletonSet) {
                            findMaximallySpecificSuperinterfaceMethods(
                                superinterfaceTypes.head,
                                name, descriptor,
                                newAnalyzedSuperinterfaceTypes
                            )
                        } else {
                            findMaximallySpecificSuperinterfaceMethods(
                                superinterfaceTypes,
                                name, descriptor,
                                newAnalyzedSuperinterfaceTypes
                            )
                        }
                }

            case None ⇒
                (analyzedSuperinterfaceTypes ++ superinterfaceTypes + superinterfaceType, Set.empty)
        }

    }

    /**
     * Computes the maximally specific superinterface method with the given name
     * and descriptor
     *
     * @param   interfaceTypes A set of interfaces which potentially declare a method
     * 			with the given name and descriptor.
     */
    def findMaximallySpecificSuperinterfaceMethods(
        superinterfaceTypes:         UIDSet[ObjectType],
        name:                        String,
        descriptor:                  MethodDescriptor,
        analyzedSuperinterfaceTypes: UIDSet[ObjectType]
    ): ( /*analyzed types*/ UIDSet[ObjectType], Set[Method]) = {
        val anchor = ((analyzedSuperinterfaceTypes, Set.empty[Method]))
        superinterfaceTypes.foldLeft(anchor) { (currentResult, interfaceType) ⇒
            val (currentAnalyzedSuperinterfaceTypes, currentMethods) = currentResult
            val (analyzedSuperinterfaceTypes, methods) =
                findMaximallySpecificSuperinterfaceMethods(
                    interfaceType, name, descriptor,
                    currentAnalyzedSuperinterfaceTypes
                )

            val allMethods = currentMethods ++ methods
            if (allMethods.isEmpty || allMethods.size == 1) {
                (analyzedSuperinterfaceTypes, allMethods /*empty or singleton set*/ )
            } else {
                // When we reach this point, we may have a situation such as:
                //     intf A { default void foo(){} }
                //     intf B extends A { default void foo(){} }
                //     intf C extends A { }
                //     intf D extends B { }
                // and we started the analysis with the set {C,D} and
                // first selected C (hence, first found A.foo).
                //
                // We now have to determine the maximally specific method.

                // both, the set of `currentMethods` and also the set of `methods`
                // each only contains maximally specific methods w.r.t. their
                // set
                var currentMaximallySpecificMethods = currentMethods
                var additionalMaximallySpecificMethods = Set.empty[Method]
                methods.view.filter(!currentMethods.contains(_)) foreach { method ⇒
                    val newMethodDeclaringClassType = project.classFile(method).thisType
                    var addNewMethod = true
                    currentMaximallySpecificMethods = currentMaximallySpecificMethods.filter { method ⇒
                        val specificMethodDeclaringClassType = project.classFile(method).thisType
                        if ((specificMethodDeclaringClassType isSubtyeOf newMethodDeclaringClassType).isYes) {
                            addNewMethod = false
                            true
                        } else if ((newMethodDeclaringClassType isSubtyeOf specificMethodDeclaringClassType).isYes) {
                            false
                        } else {
                            //... we have an incomplete class hierarchy; let's keep both methods
                            true
                        }
                    }
                    if (addNewMethod) additionalMaximallySpecificMethods += method
                }
                currentMaximallySpecificMethods ++= additionalMaximallySpecificMethods

                val concreteMaximallySpecificMethods = currentMaximallySpecificMethods.filter(!_.isAbstract)
                if (concreteMaximallySpecificMethods.isEmpty)
                    // We have not yet found any method or we may have multiple abstract methods...
                    (analyzedSuperinterfaceTypes, currentMaximallySpecificMethods)
                else
                    (analyzedSuperinterfaceTypes, concreteMaximallySpecificMethods)
            }
        }
    }

    def resolveClassMethodReference(
        receiverType: ObjectType,
        name:         String,
        descriptor:   MethodDescriptor
    ): Result[Method] = {

        project.classFile(receiverType) match {
            case Some(classFile) ⇒
                assert(!classFile.isInterfaceDeclaration)

                def resolveSuperclassMethodReference(): Result[Method] = {
                    classFile.superclassType match {
                        case Some(superclassType) ⇒
                            resolveClassMethodReference(superclassType, name, descriptor)
                        case None ⇒ Empty //the current type is already java.lang.Object
                    }
                }

                // [FROM THE SPECIFICATION]]
                // A method is signature polymorphic if all of the following conditions hold :
                // - It is declared in the java.lang.invoke.MethodHandle class.
                // - It has a single formal parameter of type Object[].
                // - It has a return type of Object.
                // - It has the ACC_VARARGS and ACC_NATIVE flags set.
                val isPotentiallySignaturePolymorphicCall =
                    (receiverType eq ObjectType.MethodHandle) &&
                        (descriptor == MethodDescriptor.SignaturePolymorphicMethod)

                if (isPotentiallySignaturePolymorphicCall) {
                    val methods = classFile.findMethod(name)
                    if (methods.isSingletonList) {
                        val method = methods.head
                        if (method.isNativeAndVarargs)
                            Success(method) // the resolved method is signature polymorphic
                        else if (method.descriptor == descriptor)
                            Success(method) // "normal" resolution of a method
                        else
                            resolveSuperclassMethodReference()
                    } else {
                        methods.find(method ⇒ method.descriptor == descriptor) match {
                            case None                 ⇒ resolveSuperclassMethodReference()
                            case Some(resolvedMethod) ⇒ Success(resolvedMethod)
                        }
                    }
                } else {
                    classFile.findMethod(name, descriptor) match {
                        case None                 ⇒ resolveSuperclassMethodReference()
                        case Some(resolvedMethod) ⇒ Success(resolvedMethod)
                    }
                }

            case None ⇒ Failure
        }
    }

    //    invokestaticTarget(i : INVOKESTATIC) : Option[Method] = {
    //        declaringClass:   ObjectType, // a class type to be precise
    //        isInterface:      Boolean,
    //        name:             String,
    //        methodDescriptor: MethodDescriptor
    //    }

    /////////// OLD OLD OLD OLD OLD OLD //////////
    /////////// OLD OLD OLD OLD OLD OLD //////////
    /////////// OLD OLD OLD OLD OLD OLD //////////
    /////////// OLD OLD OLD OLD OLD OLD //////////
    /////////// OLD OLD OLD OLD OLD OLD //////////
    /////////// OLD OLD OLD OLD OLD OLD //////////

    /*
    def resolveInterfaceMethodReference(
        receiverType:     ObjectType,
        methodName:       String,
        methodDescriptor: MethodDescriptor
    ): Option[Method] = {

        project.classFile(receiverType) flatMap { classFile ⇒
            assert(classFile.isInterfaceDeclaration)

            {
                lookupMethodInInterface(classFile, methodName, methodDescriptor)
            } orElse {
                lookupMethodDefinition(ObjectType.Object, methodName, methodDescriptor)
            }
        }
    }
    */

    def lookupMethodInInterface(
        classFile:        ClassFile,
        methodName:       String,
        methodDescriptor: MethodDescriptor
    ): Option[Method] = {
        classFile.findMethod(methodName, methodDescriptor) orElse
            lookupMethodInSuperinterfaces(classFile, methodName, methodDescriptor)
    }

    def lookupMethodInSuperinterfaces(
        classFile:        ClassFile,
        methodName:       String,
        methodDescriptor: MethodDescriptor
    ): Option[Method] = {

        classFile.interfaceTypes foreach { superinterface: ObjectType ⇒
            project.classFile(superinterface) foreach { superclass ⇒
                val result = lookupMethodInInterface(superclass, methodName, methodDescriptor)
                if (result.isDefined)
                    return result;
            }
        }
        None
    }

    /**
     * @see `lookupMethodDefinition(ObjectType,String,MethodDescriptor,ClassFileRepository)`
     */
    def lookupMethodDefinition(invocation: MethodInvocationInstruction): Option[Method] = {
        val receiverType = invocation.declaringClass

        val effectiveReceiverType =
            if (receiverType.isObjectType)
                receiverType.asObjectType
            else
                // the receiver is an array type...
                ObjectType.Object

        lookupMethodDefinition(effectiveReceiverType, invocation.name, invocation.methodDescriptor)
    }

    /**
     * Looks up the class file and method which actually defines the method that is
     * referred to by the given receiver type, method name and method descriptor. Given
     * that we are searching for method definitions the search is limited to the
     * superclasses of the class of the given receiver type.
     *
     * This method does not take visibility modifiers or the static modifier into account.
     * If necessary, such checks need to be done by the caller.
     *
     * This method supports resolution of `signature polymorphic methods`
     * (in this case however, it needs to be checked that the respective invoke
     * instruction is an `invokevirtual` instruction.)
     *
     * @note    In case that you ''analyze static source code dependencies'' and if an invoke
     *          instruction refers to a method that is not defined by the receiver's class, then
     *          it might be more meaningful to still create a dependency to the receiver's class
     *          than to look up the actual definition in one of the receiver's super classes.
     * @return  `Some(Method)` if the method is found. `None` if the method
     *          is not found. This can basically happen under two circumstances:
     *          First, not all class files referred to/used by the project are (yet) analyzed;
     *          i.e., we do not have all class files belonging to this project.
     *          Second, the analyzed class files do not belong together (they either belong to
     *          different projects or to incompatible versions of the same project.)
     *
     *          To get the method's defining class file use the project's respective method.
     */
    def lookupMethodDefinition(
        receiverType:     ObjectType,
        methodName:       String,
        methodDescriptor: MethodDescriptor
    ): Option[Method] = {

        // TODO [Java8] Support Extension Methods!
        assert(
            classHierarchy.isInterface(receiverType).isNoOrUnknown,
            s"${receiverType.toJava} is classified as an interface (looking up ${methodDescriptor.toJava(methodName)}); "+
                project.classFile(receiverType).map(_.toString).getOrElse("<precise information missing>")
        )

        @tailrec def lookupMethodDefinition(receiverType: ObjectType): Option[Method] = {
            import MethodDescriptor.SignaturePolymorphicMethod

            val classFileOption = project.classFile(receiverType)
            if (classFileOption.isEmpty)
                return None;
            val classFile = classFileOption.get

            val methodOption =
                classFile.findMethod(methodName, methodDescriptor) orElse {
                    /* FROM THE SPECIFICATION:
                     * Method resolution attempts to look up the referenced method in C and
                     * its superclasses:
                     * If C declares exactly one method with the name specified by the
                     * method reference, and the declaration is a signature polymorphic
                     * method, then method lookup succeeds.
                     * [...]
                     *
                     * A method is signature polymorphic if:
                     * - It is declared in the java.lang.invoke.MethodHandle class.
                     * - It has a single formal parameter of type Object[].
                     * - It has a return type of Object.
                     * - It has the ACC_VARARGS and ACC_NATIVE flags set.
                     */
                    if (receiverType eq ObjectType.MethodHandle)
                        classFile.findMethod(methodName, SignaturePolymorphicMethod).find(
                            _.isNativeAndVarargs
                        )
                    else
                        None
                }

            if (methodOption.isDefined) {
                methodOption
            } else {
                val superclassType = classHierarchy.superclassType(receiverType.id)
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
     *  @param classesFilter A function that returns `true`, if the runtime type of
     *       the `receiverType` may be of the type defined by the given object type. For
     *       example, if you analyze a project and perform a lookup of all methods that
     *       implement the method `toString`, then this set would probably be very large.
     *       But, if you know that only instances of the class (e.g.) `ArrayList` have
     *       been created so far
     *       (up to the point in your analysis where you call this method), it is
     *       meaningful to sort out all other classes (such as `Vector`).
     */
    def lookupImplementingMethods(
        receiverType:     ObjectType,
        methodName:       String,
        methodDescriptor: MethodDescriptor,
        classesFilter:    ObjectType ⇒ Boolean
    ): Set[Method] = {

        val receiverIsInterface = classHierarchy.isInterface(receiverType).isYes
        // TODO [Improvement] Implement an "UnsafeListSet" that does not check for the set property if (by construction) it has to be clear that all elements are unique
        var implementingMethods: Set[Method] =
            {
                if (receiverIsInterface)
                    // to handle calls such as toString on a (e.g.) "java.util.List"
                    lookupMethodDefinition(ObjectType.Object, methodName, methodDescriptor)
                else
                    lookupMethodDefinition(receiverType, methodName, methodDescriptor)
            } match {
                case Some(method) if !method.isAbstract ⇒ Set(method)
                case _                                  ⇒ Set.empty
            }

        // Search all subclasses
        val seenSubtypes = mutable.HashSet.empty[ObjectType]
        classHierarchy.foreachSubtype(receiverType) { (subtype: ObjectType) ⇒
            if (!classHierarchy.isInterface(subtype).isYes && !seenSubtypes.contains(subtype)) {
                seenSubtypes += subtype
                if (classesFilter(subtype)) {
                    classFile(subtype) foreach { classFile ⇒
                        val methodOption =
                            if (receiverIsInterface) {
                                lookupMethodDefinition(subtype, methodName, methodDescriptor)
                            } else {
                                classFile.findMethod(methodName, methodDescriptor)
                            }
                        if (methodOption.isDefined) {
                            val method = methodOption.get
                            if (!method.isAbstract)
                                implementingMethods += method
                        }
                    }
                }
            }
        }

        implementingMethods
    }

}
