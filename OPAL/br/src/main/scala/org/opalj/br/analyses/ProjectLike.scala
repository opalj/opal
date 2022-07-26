/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import scala.collection.{mutable, Map => SomeMap, Set => SomeSet}
import com.typesafe.config.Config
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger
import org.opalj.log.OPALLogger.error
import org.opalj.log.OPALLogger.info
import org.opalj.collection.immutable.UIDSet
import org.opalj.bi.Java11MajorVersion
import org.opalj.bi.Java1MajorVersion
import org.opalj.br.instructions.FieldAccess
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.NonVirtualMethodInvocationInstruction
import org.opalj.br.MethodDescriptor.SignaturePolymorphicMethodBoolean
import org.opalj.br.MethodDescriptor.SignaturePolymorphicMethodObject
import org.opalj.br.MethodDescriptor.SignaturePolymorphicMethodVoid

import scala.collection.immutable.ArraySeq
import control.find

/**
 * Enables project wide lookups of methods and fields as required to determine the target(s) of an
 * invoke or field access instruction.
 *
 * @note    The current implementation is based on the '''correct project assumption''';
 *          i.e., if the bytecode of the project as a whole is not valid, the result is generally
 *          undefined.
 *          Just one example of a violation of the assumption would be,
 *          if we have two interfaces which define a non-abstract
 *          method with the same signature and both interfaces are implemented by a third
 *          interface which does not override these methods. In this case the result of a
 *          `resolveMethodReference` is not defined, because the code base as a whole is
 *          not valid.
 *
 * @author Michael Eichberg
 */
abstract class ProjectLike extends ClassFileRepository { project =>

    private[this] final implicit val thisProjectLike: this.type = this

    implicit val classHierarchy: ClassHierarchy
    implicit val config: Config

    protected[this] val allClassFiles: Iterable[ClassFile]

    /**
     * Returns the minimum version number of the JVM required to run the code of the project, i.e.,
     * the maximum class file major version number of any class file in the project.
     */
    def requiredJVMVersion: Int = {
        if (allClassFiles.isEmpty) Java1MajorVersion
        else allClassFiles.maxBy(_.version._2).version._2
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    //
    //
    // RESOLVING FIELD REFERENCES
    //
    //
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    /**
     * @see [[#resolveFieldReference(declaringClassFile:*]]
     */
    final def resolveFieldReference(
        declaringClassType: ObjectType,
        fieldName:          String,
        fieldType:          FieldType
    ): Option[Field] = {
        // for more details see JVM 7/8 Spec. Section 5.4.3.2
        project.classFile(declaringClassType) flatMap { classFile =>
            resolveFieldReference(classFile, fieldName, fieldType)
        }
    }

    /**
     * Resolves a symbolic reference to a field. Basically, the search starts with
     * the given class `c` and then continues with `c`'s superinterfaces before the
     * search is continued with `c`'s superclass (as prescribed by the JVM specification
     * for the resolution of unresolved symbolic references).
     *
     * Resolving a symbolic reference is particularly required to, e.g., get a field's
     * annotations or to get a field's value (if it is `static`, `final` and has a
     * constant value).
     *
     * @note    This implementation does not check for `IllegalAccessError`. This check
     *          needs to be done by the caller. The same applies for the check that the
     *          field is non-static if get-/putfield is used and static if a get-/putstatic is
     *          used to access the field. In the latter case the JVM would throw a
     *          `LinkingException`.
     *          Furthermore, if the field cannot be found, it is the responsibility of the
     *          caller to handle that situation.
     * @note    Resolution is final. I.e., either this algorithm has found the defining field
     *          or the field is not defined by one of the loaded classes. Searching for the
     *          field in subclasses is not meaningful as it is not possible to ''override''
     *          fields.
     *
     * @param   declaringClassFile The class (or a superclass thereof) that is expected
     *          to define the specified field.
     * @param   fieldName The name of the field.
     * @param   fieldType The type of the field (the field descriptor).
     */
    def resolveFieldReference(
        declaringClassFile: ClassFile,
        fieldName:          String,
        fieldType:          FieldType
    ): Option[Field] = {
        // for more details see JVM 7/8 Spec. Section 5.4.3.2
        declaringClassFile findField (fieldName, fieldType) orElse {
            declaringClassFile.interfaceTypes collectFirst { supertype =>
                resolveFieldReference(supertype, fieldName, fieldType) match {
                    case Some(resolvedFieldReference) => resolvedFieldReference
                }
            } orElse {
                declaringClassFile.superclassType flatMap { supertype =>
                    resolveFieldReference(supertype, fieldName, fieldType)
                }
            }
        }
    }

    /**
     * @see [[#resolveFieldReference(declaringClassFile:*]]
     */
    final def resolveFieldReference(fieldAccess: FieldAccess): Option[Field] = {
        resolveFieldReference(fieldAccess.declaringClass, fieldAccess.name, fieldAccess.fieldType)
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    //
    //
    // RESOLVING METHOD REFERENCES / LOCATING THE INVOKED METHOD(S)
    //
    //
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    /**
     * The class file of `java.lang.Object`, if available.
     */
    val ObjectClassFile: Option[ClassFile]

    /**
     * The class file of `java.lang.invoke.MethodHandle`, if available.
     */
    val MethodHandleClassFile: Option[ClassFile]

    /**
     * The class file of `java.lang.invoke.VarHandle`, if available.
     */
    val VarHandleClassFile: Option[ClassFile]

    /**
     * The set of all subtypes of `java.lang.invoke.MethodHandle`; in particular required to resolve
     * signature polymorphic method calls.
     */
    val MethodHandleSubtypes: SomeSet[ObjectType]

    /**
     * The set of all subtypes of `java.lang.invoke.VarHandle`; in particular required to resolve
     * signature polymorphic method calls.
     */
    val VarHandleSubtypes: SomeSet[ObjectType]

    /**
     * Stores for each non-private, non-initializer method the set of methods which override
     * a specific method. If the given method is a concrete method, this method is also
     * included in the set of `overridingMethods`.
     */
    protected[this] val overridingMethods: SomeMap[Method, SomeSet[Method]]

    /**
     * Returns the set of methods which directly override the given method. Note that
     * `overriddenBy` is not context aware. I.e., if a given method `m` is an interface
     * method, then it may happen that we have an implementation of that method
     * in a class which is inherited from a superclass which is not a subtype of the
     * interface. That method - since it is not defined by a subtype of the interface -
     * would not be included in the returned set. An example is shown next:
     * {{{
     * class X { void m(){ System.out.println("X.m"); }
     * interface Y { default void m(){ System.out.println("Y.m"); }
     * class Z extends X implements Y {
     *  // Z inherits m() from X; hence, X.m() (in this context) "overrides" Y.m(), but is not
     *  // returned by this function. To also identify X.m() you have to combine the results
     *  // of overridenBy and instanceMethods(!).
     * }
     * }}}
     */
    def overriddenBy(m: Method): SomeSet[Method] = {
        assert(!m.isPrivate, s"private methods $m cannot be overridden")
        assert(!m.isStatic, s"static methods $m cannot be overridden")
        assert(!m.isInitializer, s"initializers $m cannot be overridden")

        overridingMethods.getOrElse(m, Set.empty)
    }

    /**
     * Returns the set of all non-private, non-abstract, non-static methods that are not
     * initializers and which are potentially callable by clients when we have an object that
     * has the specified type and a method is called using
     * [[org.opalj.br.instructions.INVOKEINTERFACE]], [[org.opalj.br.instructions.INVOKEVIRTUAL]] or
     * [[org.opalj.br.instructions.INVOKEDYNAMIC]].
     *
     * The array of methods is sorted using [[MethodDeclarationContextOrdering]] to
     * enable fast look-up of the target method. (See [[MethodDeclarationContext]]'s
     * `compareAccessibilityAware` method for further details.)
     */
    protected[this] val instanceMethods: SomeMap[ObjectType, ArraySeq[MethodDeclarationContext]]

    /**
     * Returns the nest host (see JVM 11 Spec. 5.4.4) for the given type, if explicitly given. For
     * classes without an explicit NestHost or NestMembers attribute, the type itself is the nest
     * host, but this is NOT recorded in this map.
     */
    val nests: SomeMap[ObjectType, ObjectType]

    /**
     * Tests if the given method belongs to the interface of an '''object''' identified by
     * the given `objectType`.
     * I.e., returns `true` if a virtual method call, where the receiver type is known
     * to have the given `objectType`, would lead to the direct invocation of the given `method`.
     * The given method can be an inherited method, but it will never return `Yes` if
     * the given method is overridden by `objectType` or a supertype of it which is a
     * sub type of the declaring type of `method`.
     *
     * @note    The computation is based on the computed set of [[instanceMethods]] and generally
     *          requires at most O(n log n) steps where n is the number of callable instance
     *          methods of the given object type; the class hierarchy is not traversed.
     */
    def hasVirtualMethod(objectType: ObjectType, method: Method): Answer = {
        //  ... instanceMethods: Map[ObjectType, Array[MethodDeclarationContext]]
        val definedMethodsOption = instanceMethods.get(objectType)
        if (definedMethodsOption.isEmpty) {
            return Unknown;
        }
        val definedMethods: ArraySeq[MethodDeclarationContext] = definedMethodsOption.get
        val declaringPackageName = method.classFile.thisType.packageName

        val result: Option[MethodDeclarationContext] = find(definedMethods) { definedMethodContext =>
            val definedMethod = definedMethodContext.method
            if (definedMethod eq method)
                0
            else {
                val methodComparison = definedMethod compare method
                if (methodComparison == 0) {
                    if (definedMethod.isPrivate) {
                        // If there is a matching private method, the given method could still be
                        // invoked by a virtual call for a supertype
                        return hasVirtualMethod(
                            classFile(objectType).get.superclassType.get,
                            method
                        );
                    } else {
                        // We may have multiple methods with the same signature, but which belong
                        // to different packages!
                        definedMethodContext.packageName compare declaringPackageName
                    }
                } else
                    methodComparison
            }
        }
        Answer(result)
    }

    /**
     * Looks up the method (declaration context) which is accessible/callable by an
     * [[org.opalj.br.instructions.INVOKEVIRTUAL]] or [[org.opalj.br.instructions.INVOKEINTERFACE]]
     * call which was done by a method belonging to `callingContextType`.
     * The `callingContextType` is only relevant in case the target method has default visibility;
     * in this case it is checked whether the caller belongs to the same context.
     *
     * @note    This method uses the pre-computed information about instance methods and,
     *          therefore, does not require a type hierarchy based lookup.
     *
     * @note    It supports the lookup of polymorphic methods.
     *
     * @return  [[Success]] if the method is found; [[Empty$]] if the method cannot be found and
     *          [[Failure$]] if the method cannot be found because the project is
     *          definitively inconsistent. `Failure$` is used on a best-effort basis.
     */
    def lookupVirtualMethod(
        callingContextType: ObjectType,
        receiverType:       ObjectType,
        name:               String,
        descriptor:         MethodDescriptor
    ): Result[MethodDeclarationContext] = {

        def lookupSignaturePolymorphicMethod(descriptor: MethodDescriptor): Result[MethodDeclarationContext] = {
            lookupVirtualMethod(
                callingContextType,
                if (MethodHandleSubtypes.contains(receiverType)) ObjectType.MethodHandle
                else ObjectType.VarHandle,
                name,
                descriptor
            ) match {
                    case r @ Success(mdc) if mdc.method.isNativeAndVarargs => r
                    case _                                                 => Empty
                }
        }

        val definedMethodsOption = instanceMethods.get(receiverType)
        if (definedMethodsOption.isEmpty) {
            return Empty;
        }
        find(definedMethodsOption.get) { mdc =>
            mdc.compareAccessibilityAware(callingContextType.packageName, name, descriptor)
        } match {
            case Some(mdc) => Success(mdc)
            case None =>
                // we have to avoid endless recursion if we can't find the target method
                if ((MethodHandleSubtypes.contains(receiverType) ||
                    VarHandleSubtypes.contains(receiverType)) &&
                    !isSignaturePolymorphic(receiverType, descriptor)) {
                    // At least in Java 15 the signature polymorphic methods are not overloaded and
                    // it actually doesn't make sense to do so. Therefore we decided to only
                    // make this lookup if strictly required.
                    lookupSignaturePolymorphicMethod(SignaturePolymorphicMethodObject) match {
                        case Empty if VarHandleSubtypes.contains(receiverType) =>
                            lookupSignaturePolymorphicMethod(SignaturePolymorphicMethodVoid) match {
                                case Empty =>
                                    lookupSignaturePolymorphicMethod(
                                        SignaturePolymorphicMethodBoolean
                                    )
                                case r => r
                            }
                        case r => r
                    }
                } else {
                    Empty // here, we don't know if the project is incomplete or inconsistent
                }
        }
    }

    /* GENERAL NOTES
     *
     * (Accessibility checks are done by the JVM when the method is resolved; this is, however, not
     * done by the following methods as it does not affect the search for the potential target
     * methods.)
     *
     * Invokestatic     =>  the resolved method is called (if it is accessible, static etc...)
     * Invokespecial    =>  the resolved (interface) method is called if it is a constructor
     *                      call, or the resolved method belongs to the calling class (calls of
     *                      private methods). Otherwise, the calling class' supertypes are
     *                      searched for a method with the same signature up until the resolved
     *                      method. When we search the type hierarchy upwards, we first search
     *                      for a method defined by a superclass (unless the current class defines
     *                      an interface type) before we search the interfaces. In the later
     *                      case we compute the set of the maximally specific matching methods
     *                      and select THE concrete one (it is an error if multiple concrete ones
     *                      exist!)
     * Invokevirtual    =>  the resolved method is called, if the resolved method is signature
     *                      polymorphic; in this case the concrete type of the method receiver
     *                      is not relevant. Otherwise, the type of the receiver is used to
     *                      start searching for the method that is to be invoked. That method
     *                      however, has to override the resolved method (which is generally not
     *                      the case if the method is private; and is only the case if the resolved
     *                      method and the current type belong to the same package in case of
     *                      a method with default visibility).
     * Invokeinterface  =>  the resolved interface method just defines an upper bound; the
     *                      called method is determined as in case of invokevirtual; but
     *                      signature polymorphic calls are not relevant
     */

    /**
     * Tries to resolve a method reference as specified by the JVM specification.
     * I.e., the algorithm tries to find the class that actually declares the referenced
     * method. Resolution of '''signature polymorphic''' method calls is also
     * supported.
     *
     * This method can be used as the basis for the implementation of the semantics
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
     * @param   declaringClassType The type of the object that receives the method call. The
     *          type must be a class type and must not be an interface type.
     *          No check w.r.t. a potential `IncompatibleClassChangeError` is done
     *          by this method.
     * @param   forceLookupInSuperinterfacesOnFailure If true (default: false) the method tries
     *          to look up the method in a super interface if it can't find it in the available
     *          super classes.
     * @return  The resolved method `Some(`'''METHOD'''`)` or `None`.
     *          (To get the defining class file use the project's respective method.)
     */
    def resolveMethodReference(
        declaringClassType:                    ReferenceType,
        name:                                  String,
        descriptor:                            MethodDescriptor,
        forceLookupInSuperinterfacesOnFailure: Boolean          = false
    ): Option[Method] = {
        val receiverType =
            if (declaringClassType.isArrayType) {
                ObjectType.Object
            } else {
                declaringClassType.asObjectType
            }

        resolveClassMethodReference(receiverType, name, descriptor) match {
            case Success(method)                                 => Some(method)
            case Empty if !forceLookupInSuperinterfacesOnFailure => None
            case _ /*Failure | (Empty && lookupInSuperinterfacesOnFailure) */ =>
                val superinterfaceTypes = classHierarchy.allSuperinterfacetypes(receiverType)
                val (_, methods) =
                    findMaximallySpecificSuperinterfaceMethods(
                        superinterfaceTypes, name, descriptor,
                        analyzedSuperinterfaceTypes = UIDSet.empty[ObjectType]
                    )
                // Either it is THE max. specific method or some "arbitrary" method.
                // recall that we already give precedence to non-abstract
                // methods in the find... methods
                methods.headOption
        }
    }

    /**
     * Resolves a method reference to all possible methods. I.e., this is identical to
     * `resolveMethodReference` or `resolveInterfaceMethodReference` for class and interface types
     * respectively except for the case where there are multiple maximally specific interface
     * methods in which case all of them are returned instead of only a single one.
     *
     * @param declaringClassType The type of the object that receives the method call. The type may
     *         be a class or interface type.
     *
     * @return The set of resolved methods; empty if the resolution fails, more than one if
     *         resolution finds several maximally specific interface methods - in the latter case
     *         it is not possible to call the method on objects of the declaring class type, but
     *         only on subclasses overriding the method uniquely.
     */
    def resolveAllMethodReferences(
        declaringClassType: ReferenceType,
        name:               String,
        descriptor:         MethodDescriptor
    ): Set[Method] = {
        val receiverType =
            if (declaringClassType.isArrayType) {
                ObjectType.Object
            } else {
                declaringClassType.asObjectType
            }

        def lookupInObject(): Option[Method] = {
            ObjectClassFile flatMap { classFile =>
                classFile.findMethod(name, descriptor) filter { m => m.isPublic && !m.isStatic }
            }
        }

        project.classFile(receiverType) match {
            case Some(classFile) =>
                val classMethod =
                    if (classFile.isInterfaceDeclaration)
                        Result(classFile.findMethod(name, descriptor) orElse lookupInObject())
                    else
                        resolveClassMethodReference(receiverType, name, descriptor)

                classMethod match {
                    case Success(method) => Set(method)
                    case _ =>
                        val superinterfaces = classHierarchy.allSuperinterfacetypes(receiverType)
                        val (_, methods) = findMaximallySpecificSuperinterfaceMethods(
                            superinterfaces, name, descriptor, UIDSet.empty[ObjectType]
                        )
                        methods
                }

            case None => Set.empty
        }
    }

    /**
     * See [[#resolveMethodReference(declaringClassType:*]] for details.
     */
    def resolveMethodReference(i: INVOKEVIRTUAL): Option[Method] = {
        resolveMethodReference(i.declaringClass, i.name, i.methodDescriptor)
    }

    def resolveInterfaceMethodReference(
        declaringClassType: ObjectType,
        name:               String,
        descriptor:         MethodDescriptor
    ): Option[Method] = {
        def lookupInObject(): Option[Method] = {
            ObjectClassFile flatMap { classFile =>
                classFile.findMethod(name, descriptor) filter { m => m.isPublic && !m.isStatic }
            }
        }

        project.classFile(declaringClassType) flatMap { classFile =>
            assert(classFile.isInterfaceDeclaration)
            classFile.findMethod(name, descriptor) orElse {
                lookupInObject() orElse {
                    classHierarchy.superinterfaceTypes(declaringClassType) flatMap { superT =>
                        val (_, methods) = findMaximallySpecificSuperinterfaceMethods(
                            superT, name, descriptor, UIDSet.empty[ObjectType]
                        )
                        methods.headOption
                    }
                }
            }
        }
    }

    /**
     * See [[#resolveInterfaceMethodReference(declaringClassType:*]] for details.
     */
    def resolveInterfaceMethodReference(i: INVOKEINTERFACE): Option[Method] = {
        resolveInterfaceMethodReference(i.declaringClass, i.name, i.methodDescriptor)
    }

    /**
     * Computes the set of maximally specific superinterface methods with the
     * given name and descriptor.
     *
     * @note    '''This method does not consider methods defined by `java.lang.Object`'''!
     *          Those methods have precedence over respective methods defined by
     *          superinterfaces! A corresponding check needs to be done before calling
     *          this method.
     */
    def findMaximallySpecificSuperinterfaceMethods(
        superinterfaceType:          ObjectType,
        name:                        String,
        descriptor:                  MethodDescriptor,
        analyzedSuperinterfaceTypes: UIDSet[ObjectType] = UIDSet.empty
    ): ( /*analyzed types*/ UIDSet[ObjectType], Set[Method]) = {
        ProjectLike.findMaximallySpecificSuperinterfaceMethods(
            superinterfaceType, name, descriptor, analyzedSuperinterfaceTypes
        )(this.classFile, this.classHierarchy, this.logContext)
    }

    /**
     * Computes the maximally specific superinterface method with the given name
     * and descriptor
     *
     * @param   superinterfaceTypes A set of interfaces which potentially declare a method
     *          with the given name and descriptor.
     */
    def findMaximallySpecificSuperinterfaceMethods(
        superinterfaceTypes:         UIDSet[ObjectType],
        name:                        String,
        descriptor:                  MethodDescriptor,
        analyzedSuperinterfaceTypes: UIDSet[ObjectType]
    ): ( /*analyzed types*/ UIDSet[ObjectType], Set[Method]) = {
        ProjectLike.findMaximallySpecificSuperinterfaceMethods(
            superinterfaceTypes, name, descriptor, analyzedSuperinterfaceTypes
        )(this.classFile, this.classHierarchy, this.logContext)
    }

    /**
     * Resolves a symbolic reference to a method defined by a class (not interface) type.
     *
     * @return  [[org.opalj.Success]]`(method)` if the method was found;
     *          `Empty` if the project is incomplete and the method could not be found;
     *          `Failure` if the method could not be found though the project is seemingly complete.
     *          I.e., if `Failure` is returned the method is not defined by a concrete class
     *          and is either a default method defined by an interface or the analyzed code
     *          basis is inconsistent.
     */
    def resolveClassMethodReference(
        receiverType: ObjectType,
        name:         String,
        descriptor:   MethodDescriptor
    ): Result[Method] = {

        project.classFile(receiverType) match {
            case Some(classFile) =>
                assert(
                    !classFile.isInterfaceDeclaration,
                    {
                        val methodInfo = descriptor.toJava(receiverType.toJava, name)
                        s"the method is defined in an interface $methodInfo"
                    }
                )

                def resolveSuperclassMethodReference(): Result[Method] = {
                    classFile.superclassType match {
                        case Some(superclassType) =>
                            resolveClassMethodReference(superclassType, name, descriptor)
                        case None =>
                            // the current type is java.lang.Object and the method was not found
                            Failure
                    }
                }

                // [FROM THE SPECIFICATION]]
                // A method is signature polymorphic if all of the following conditions hold :
                // - It is declared in the java.lang.invoke.MethodHandle class.
                // - It has a single formal parameter of type Object[].
                // - It has the ACC_VARARGS and ACC_NATIVE flags set.
                val isPotentiallySignaturePolymorphicCall =
                    (receiverType eq ObjectType.MethodHandle) ||
                        (receiverType eq ObjectType.VarHandle)

                if (isPotentiallySignaturePolymorphicCall) {
                    val methods = classFile.findMethod(name)
                    methods match {
                        case List(method) =>
                            if (method.isNativeAndVarargs &&
                                (method.descriptor == SignaturePolymorphicMethodObject ||
                                    method.descriptor == SignaturePolymorphicMethodVoid ||
                                    method.descriptor == SignaturePolymorphicMethodBoolean))
                                Success(method) // the resolved method is signature polymorphic
                            else if (method.descriptor == descriptor)
                                Success(method) // "normal" resolution of a method
                            else
                                resolveSuperclassMethodReference()
                        case _ =>
                            methods.find(m => m.descriptor == descriptor) match {
                                case None                 => resolveSuperclassMethodReference()
                                case Some(resolvedMethod) => Success(resolvedMethod)
                            }
                    }

                } else {
                    classFile.findMethod(name, descriptor) match {
                        case None                 => resolveSuperclassMethodReference()
                        case Some(resolvedMethod) => Success(resolvedMethod)
                    }
                }

            case None => Empty
        }
    }

    /**
     * Returns true if the method defined by the given class type is a signature polymorphic
     * method. (See JVM 9 Spec. for details.)
     */
    //TODO add method that lookup the defining class type
    def isSignaturePolymorphic(definingClassType: ObjectType, method: Method): Boolean = {
        (
            (definingClassType eq ObjectType.MethodHandle) ||
            (definingClassType eq ObjectType.VarHandle)
        ) &&
            method.descriptor.parametersCount == 1 &&
            method.descriptor.parameterType(0) == ArrayType.ArrayOfObject &&
            method.isNativeAndVarargs
    }

    /**
     * Returns true if the descriptor is a signature polymorphic method for the given class type.
     * (See JVM 9 Spec. for details.)
     */
    def isSignaturePolymorphic(
        definingClassType: ObjectType,
        descriptor:        MethodDescriptor
    ): Boolean = {
        (definingClassType eq ObjectType.MethodHandle) &&
            descriptor == SignaturePolymorphicMethodObject ||
            (definingClassType eq ObjectType.VarHandle) &&
            (descriptor == SignaturePolymorphicMethodObject ||
                descriptor == SignaturePolymorphicMethodVoid ||
                descriptor == SignaturePolymorphicMethodBoolean)
    }

    /**
     * Returns true if the signature is a signature polymorphic method for the given class type.
     * (See JVM 9 Spec. for details.)
     */
    def isSignaturePolymorphic(
        definingClassType: ObjectType,
        name:              String,
        descriptor:        MethodDescriptor
    ): Boolean = {
        (definingClassType eq ObjectType.MethodHandle) &&
            descriptor == SignaturePolymorphicMethodObject &&
            (name == "invoke" || name == "invokeExact") ||
            (definingClassType eq ObjectType.VarHandle) &&
            (descriptor == SignaturePolymorphicMethodObject ||
                descriptor == SignaturePolymorphicMethodVoid ||
                descriptor == SignaturePolymorphicMethodBoolean)
    }

    /**
     * Returns the method which will be called by the respective
     * [[org.opalj.br.instructions.INVOKESTATIC]] instruction.
     */
    def staticCall(callerClassType: ObjectType, i: INVOKESTATIC): Result[Method] = {
        staticCall(callerClassType, i.declaringClass, i.isInterface, i.name, i.methodDescriptor)
    }

    /**
     * Returns the method that will be called by the respective invokestatic call.
     *
     * @return  [[org.opalj.Success]] `(method)` if the method was found;
     *          `Failure` if the project is inconsistent.
     *          `Empty` if the method could not be found in the available classes (i.e., the
     *          project is incomplete).
     */
    def staticCall(
        callerClassType:    ObjectType,
        declaringClassType: ObjectType,
        isInterface:        Boolean,
        name:               String,
        descriptor:         MethodDescriptor
    ): Result[Method] = {
        // Recall that the invokestatic instruction:
        // "... gives the name and descriptor of the method as well as a symbolic reference to
        // the class or interface in which the method is to be found.
        // However, in case of interfaces no lookup in superclasses is done!
        if (isInterface) {
            classFile(declaringClassType) match {
                case Some(cf) => cf.findMethod(name, descriptor) match {
                    case Some(method) if method.isAccessibleBy(callerClassType, nests) =>
                        Success(method)
                    case _ => Empty
                }
                case None => Empty
            }
        } else {
            resolveClassMethodReference(declaringClassType, name, descriptor) match {
                case s @ Success(method) =>
                    if (method.isAccessibleBy(callerClassType, nests)) s else Empty
                case e =>
                    e
            }
        }
    }

    def specialCall(callerClassType: ObjectType, i: INVOKESPECIAL): Result[Method] = {
        specialCall(callerClassType, i.declaringClass, i.isInterface, i.name, i.methodDescriptor)
    }

    def nonVirtualCall(
        callerClassType: ObjectType,
        i:               NonVirtualMethodInvocationInstruction
    ): Result[Method] = {
        if (i.opcode == INVOKESPECIAL.opcode) {
            specialCall(callerClassType, i.asINVOKESPECIAL)
        } else { // i.opcode == INVOKESTATIC.opcode
            staticCall(callerClassType, i.asINVOKESTATIC)
        }
    }

    /**
     * Returns the instance method/initializer which is called by an invokespecial instruction.
     *
     * @note    Virtual method call resolution is not necessary; the call target is
     *          either a constructor, a method in the given class or a super method/constructor.
     *          However, in the first and last case it may be possible that we can't find the method
     *          because of an inconsistent or incomplete project.
     *
     * @return  One of the following three values:
     *           - [[org.opalj.Success]] `(method)` if the method was found;
     *           - `Failure` if the project is inconsistent; i.e., the target class file is found,
     *             but the method cannot be found. `Failure` is returned on a best effort basis.
     *           - `Empty`.
     */
    def specialCall(
        callerClassType:           ObjectType,
        initialDeclaringClassType: ObjectType, // an interface or class type to be precise
        isInterface:               Boolean,
        name:                      String, // an interface or class type to be precise
        descriptor:                MethodDescriptor
    ): Result[Method] = {
        /* FROM THE SPEC
            If all of the following are true, let C be the direct superclass of the current class:
            • The resolved method is not an instance initialization method (§2.9.1).
            • If the symbolic reference names a class (not an interface), then that class is a
              superclass of the current class.
            • The ACC_SUPER flag is set for the class file (§4.1).
            Otherwise, let C be the class or interface named by the symbolic reference.
         */
        val declaringClassType =
            if (name != "<init>" &&
                (callerClassType ne initialDeclaringClassType) && // <= handles private calls
                classHierarchy.isInterface(initialDeclaringClassType).isNo) {
                // Let's select the direct superclass (if it is available; otherwise we use the
                // declared class as a fallback.)
                classHierarchy.superclassType(callerClassType).getOrElse(initialDeclaringClassType)
            } else {
                initialDeclaringClassType
            }

        // ...  default methods cannot override methods from java.lang.Object
        // ...  in case of super method calls (not initializers), we can use
        //      "instanceMethods" to find the method, because the method has to
        //      be an instance method, must not be abstract and must not be private.
        // ...  the receiver type of super initializer calls is always explicitly given
        classFile(declaringClassType) match {
            case Some(classFile) =>
                if (classFile.isInterfaceDeclaration != isInterface)
                    Failure
                else {
                    classFile.findMethod(name, descriptor) match {
                        case Some(method) =>
                            if (method.isAccessibleBy(callerClassType, nests))
                                Success(method)
                            else
                                Empty

                        case None if name == "<init>" => Failure // initializer not found...

                        case _ =>
                            // We have to find the (maximally specific) super method, which is,
                            // unless we have an inconsistent code base, unique.
                            find(instanceMethods(declaringClassType)) { definedMethodContext =>
                                val definedMethod = definedMethodContext.method
                                definedMethod.compare(name, descriptor)
                            } match {
                                case Some(mdc) =>
                                    if (mdc.method.isAccessibleBy(callerClassType, nests))
                                        Success(mdc.method)
                                    else
                                        Empty
                                case None => Empty
                            }
                    }
                }
            case None => Empty
        }
    }

    /**
     * Returns the (instance) method that would be called when we have an instance of
     * the given receiver type. I.e., using this method is suitable only when the runtime
     * type, which is the receiver of the method call, is precisely known!
     *
     * == Examples ==
     * {{{
     * class A {def foo() = {} }
     * class B extends A {/*inherits, but does not override foo()*/}
     * class C extends B { def foo() = {} }
     * val b = new B();
     * b.foo() // <= in this case the method defined by A will be returned.
     * val c = new C();
     * c.foo() // <= in this case the method defined by C will be returned.
     * }}}
     *
     * This method supports default methods and signature polymorphic calls; i.e., the
     * descriptor of the retuned methods may not be equal to the given method descriptor.
     *
     * @param   callerClassType The object type which defines the method which performs the call.
     *          This information is required if the call target has (potentially) default
     *          visibility. (Note that this - in general - does not replace the need to perform an
     *          accessibility check.)
     * @param   receiverType A class type or an array type; never an interface type.
     */
    def instanceCall(
        callerClassType: ObjectType,
        receiverType:    ReferenceType,
        name:            String,
        descriptor:      MethodDescriptor
    ): Result[Method] = {
        if (receiverType.isArrayType) {
            return Result(ObjectClassFile flatMap { cf => cf.findMethod(name, descriptor) });
        }

        val receiverClassType = receiverType.asObjectType
        val mdcResult = lookupVirtualMethod(callerClassType, receiverClassType, name, descriptor)
        mdcResult flatMap { mdc =>
            if (!mdc.method.isPrivate || mdc.method.isAccessibleBy(callerClassType, nests))
                Success(mdc.method)
            else
                Empty
        }
    }

    def interfaceCall(callerType: ObjectType, i: INVOKEINTERFACE): Set[Method] = {
        interfaceCall(callerType, i.declaringClass, i.name, i.methodDescriptor)
    }

    private val useJava11CallSemantics: Boolean = {
        val key = ProjectLike.EnforceJava11CallSemanticsConfigKey
        val forceJ11semantics: Boolean =
            try {
                config.getBoolean(key)
            } catch {
                case t: Throwable =>
                    error("project configuration", s"couldn't read: $key", t)
                    false
            }
        val (useJ11semantics, reason) = if (forceJ11semantics) {
            (true, "(enforced by config)")
        } else {
            val requiredVersion = requiredJVMVersion
            (requiredVersion >= Java11MajorVersion, s"(required JVM version is $requiredVersion)")
        }
        info(
            "project configuration",
            s"${if (useJ11semantics) "" else "not "}using Java 11+ call semantics "+reason
        )
        useJ11semantics
    }

    /**
     * Returns the methods that may be called by an [[org.opalj.br.instructions.INVOKEINTERFACE]]
     * call if the precise runtime type is not known. (If the precise runtime type is known, use
     * `instanceCall` to get the target method.)
     *
     * @note    '''Caching the result (in particular when the call graph is computed)
     *          is recommended as the computation is expensive.''' In other words, this
     *          function is meant to be used as a foundation for call graph construction
     *          algorithms.
     *
     * @note    Keep in mind that the following is legal (byte)code:
     *          {{{
     *          class X { void m(){ System.out.println("X.m"); } }
     *          interface I { void m(); }
     *          class Z extends X implements I {}
     *          }}}
     *          Hence, we also have to consider inherited methods and just considering the
     *          methods defined by subclasses is not sufficient! In other words, the result
     *          can contain methods (here, `X.m`) defined by classes which are not subtypes
     *          of the given interface type!
     *
     * @return  The set of potentially called methods. The set will be empty if the target class
     *          is not defined as part of the analyzed code base.
     */
    def interfaceCall(
        callerType:     ObjectType,
        declaringClass: ObjectType, // an interface or class type to be precise
        name:           String,
        descriptor:     MethodDescriptor
    ): Set[Method] = {
        var methods = Set.empty[Method]

        // (1) consider the method defined by the super type or this type...
        // Depending on the set of open/closed packages it may be the case that the method
        // cannot be a receiver, because it is actually always overridden; however, we don't
        // do any checks related to this issue.
        val initialMethodsOption = instanceMethods.get(declaringClass)
        if (initialMethodsOption.isEmpty)
            return methods;
        find(initialMethodsOption.get) { mdc =>
            mdc.method.compare(name, descriptor)
        } foreach (mdc => methods += mdc.method)

        if (methods.nonEmpty) {
            val method = methods.head
            if (!method.isPublic) {
                if (method.isPrivate && useJava11CallSemantics) {
                    // The method is private, thus it is selected (JVM 11 Spec Section 5.4.6)
                    // However, access control may still fail
                    if (!method.isAccessibleBy(callerType, nests))
                        return Set.empty[Method];
                    return methods;
                } else {
                    methods = Set.empty[Method]
                }
            }
        }

        // (2) methods of strict subtypes (always necessary, because we have an interface)
        classHierarchy.foreachSubtypeCF(declaringClass, reflexive = false) { subtypeCF =>
            val subtype = subtypeCF.thisType
            val mdc = find(instanceMethods(subtype)) { mdc => mdc.method.compare(name, descriptor) }
            mdc match {
                case Some(mdc) =>
                    if (mdc.isPublic)
                        methods += mdc.method
                    // This is an overapproximation, if the inherited concrete method is
                    // always overridden by all concrete subtypes and subtypeCF
                    // is an abstract class in a closed package/module
                    if (!mdc.method.isPrivate)
                        methods ++=
                            overriddenBy(mdc.method).iterator.filter { m =>
                                m.classFile.thisType isSubtypeOf subtype
                            }

                    // for interfaces we have to continue, because we may have inherited a
                    // a concrete method from a class type which is not in the set of
                    // overriddenBy methods
                    subtypeCF.isInterfaceDeclaration
                case _ /*None*/ =>
                    true
            }
        }
        methods
    }

    /**
     * Convenience method; see `virtualCall(callerPackageName:String,declaringType:ReferenceType*`
     * for details.
     */
    def virtualCall(callerType: ObjectType, i: INVOKEVIRTUAL): SomeSet[Method] = {
        virtualCall(callerType, i.declaringClass, i.name, i.methodDescriptor)
    }

    /**
     * Returns the set of methods that may be called by an invokevirtual call, if
     * the receiver type is unknown or effectively encompasses all subtypes it
     * is recommended to use [[instanceCall]].
     *
     * @note    As in case of instance call, the returned method may have a different
     *          descriptor if we have a signature polymorphic call!
     */
    def virtualCall(
        callerType:    ObjectType,
        declaringType: ReferenceType, // an interface, class or array type to be precise
        name:          String,
        descriptor:    MethodDescriptor
    ): SomeSet[Method] = {
        if (declaringType.isArrayType) {
            return instanceCall(ObjectType.Object, ObjectType.Object, name, descriptor).toSet
        }

        // In the following we opted for implementing some support for the
        // different possibilities that exist w.r.t. where the defined method
        // is found. This is done to speed up the computation
        // of the set of methods (vs. using a very generic approach)!

        val declaringClassType = declaringType.asObjectType
        var methods = mutable.Set.empty[Method]

        val initialMethodsOption = instanceMethods.get(declaringClassType)
        if (initialMethodsOption.isEmpty)
            return methods;

        val callerPackageName = callerType.packageName

        // Let's find the (concrete) method defined by this type or a supertype if it exists.
        // We have to check the declaring package if the method has package visibility to ensure
        // that we find the correct method!
        find(initialMethodsOption.get) { mdc =>
            mdc.compareAccessibilityAware(callerPackageName, name, descriptor)
        } foreach (mdc => methods += mdc.method)

        if (methods.nonEmpty) {
            val method = methods.head
            if (method.isPrivate) {
                // The concrete method is private, thus it is selected (JVM 11 Spec Section 5.4.6)
                // However, access control may still fail
                if (!method.isAccessibleBy(callerType, nests))
                    return SomeSet.empty[Method];
                return methods;
            } else if (method.classFile.thisType eq declaringClassType) {
                // The (concrete) method belongs to this class... hence, we just need to
                // get all methods which override (reflexive) this method and are done.
                return overriddenBy(method);
            } else {
                // ... we cannot use the overriddenBy methods because this could return
                // methods belonging to classes which are not a subtype of the given
                // declaring class.
                classHierarchy.foreachSubtypeCF(declaringClassType, reflexive = false) { subtypeCF =>
                    val subtype = subtypeCF.thisType
                    val mdcOption = find(instanceMethods(subtype)) { mdc =>
                        mdc.compareAccessibilityAware(callerPackageName, name, descriptor)
                    }
                    if (mdcOption.nonEmpty && (mdcOption.get.method ne method)
                        && !mdcOption.get.method.isPrivate) {
                        methods ++= overriddenBy(mdcOption.get.method)
                        false // we don't have to look into furthersubtypes
                    } else {
                        true
                    }
                }
                return methods;
            }
        }

        // We just have to collect the methods in the subtypes... unless we have
        // a call to a signature polymorphic method!

        def findSignaturePolymorphicMethod(
            descriptor: MethodDescriptor
        ): Option[MethodDeclarationContext] = {
            find(instanceMethods(declaringClassType)) { mdc =>
                mdc.compareAccessibilityAware(callerPackageName, name, descriptor)
            }
        }

        if (MethodHandleClassFile.isDefined && MethodHandleSubtypes.contains(declaringClassType)) {
            val mdcOption = findSignaturePolymorphicMethod(SignaturePolymorphicMethodObject)
            if (mdcOption.isDefined) {
                val method = mdcOption.get.method
                if (method.isNativeAndVarargs && (method.classFile eq MethodHandleClassFile.get)) {
                    return Set(method);
                }
            }
        }

        if (VarHandleClassFile.isDefined && VarHandleSubtypes.contains(declaringClassType)) {
            val mdcOption =
                findSignaturePolymorphicMethod(SignaturePolymorphicMethodObject).orElse(
                    findSignaturePolymorphicMethod(SignaturePolymorphicMethodVoid).orElse(
                        findSignaturePolymorphicMethod(SignaturePolymorphicMethodBoolean)
                    )
                )
            if (mdcOption.isDefined) {
                val method = mdcOption.get.method
                if (method.isNativeAndVarargs && (method.classFile eq VarHandleClassFile.get)) {
                    return Set(method);
                }
            }
        }

        classHierarchy.foreachSubtypeCF(declaringClassType, reflexive = false) { subtypeCF =>
            val subtype = subtypeCF.thisType
            val mdcOption = find(instanceMethods(subtype)) { mdc =>
                mdc.compareAccessibilityAware(callerPackageName, name, descriptor)
            }
            mdcOption match {
                case Some(mdc) if !mdc.method.isPrivate =>
                    if (methods.isEmpty) {
                        methods = mutable.Set.from(overriddenBy(mdc.method))
                    } else {
                        methods ++= overriddenBy(mdc.method)
                    }
                    false // we don't have to look into furthersubtypes
                case _ /*None*/ =>
                    true
            }
        }
        methods

    }

}

object ProjectLike {

    /**
     * Computes the set of maximally specific superinterface methods with the
     * given name and descriptor.
     *
     * @note    This method requires that the class hierarchy is already computed.
     *          It does not require `instanceMethods`.
     * @note    '''This method does not consider methods defined by `java.lang.Object`'''!
     *          Those methods have precedence over respective methods defined by
     *          superinterfaces! A corresponding check needs to be done before calling
     *          this method.
     */
    def findMaximallySpecificSuperinterfaceMethods(
        superinterfaceType:          ObjectType,
        name:                        String,
        descriptor:                  MethodDescriptor,
        analyzedSuperinterfaceTypes: UIDSet[ObjectType] = UIDSet.empty
    )(
        implicit
        objectTypeToClassFile: ObjectType => Option[ClassFile],
        classHierarchy:        ClassHierarchy,
        logContext:            LogContext
    ): ( /*analyzed types*/ UIDSet[ObjectType], Set[Method]) = {

        val newAnalyzedSuperinterfaceTypes = analyzedSuperinterfaceTypes + superinterfaceType

        // the superinterfaceTypes in which it is potentially relevant to search for methods
        val superinterfaceTypes: UIDSet[ObjectType] =
            classHierarchy.superinterfaceTypes(superinterfaceType).getOrElse(UIDSet.empty) --
                analyzedSuperinterfaceTypes

        objectTypeToClassFile(superinterfaceType) match {
            case Some(classFile) =>
                if (!classFile.isInterfaceDeclaration) {
                    OPALLogger.warn(
                        "project configuration",
                        "finding the maximally specific superinterface methods failed: "+
                            s"${superinterfaceType.toJava} is not an interface and ignored"
                    )
                    (analyzedSuperinterfaceTypes ++ superinterfaceTypes + superinterfaceType, Set.empty)
                } else {
                    classFile.findMethod(name, descriptor) match {
                        case Some(method) if !method.isPrivate && !method.isStatic =>
                            val analyzedTypes = newAnalyzedSuperinterfaceTypes ++ superinterfaceTypes
                            (analyzedTypes, Set(method))

                        case _ /* None OR "the method was either private or static" */ =>
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
                }

            case None =>
                (analyzedSuperinterfaceTypes ++ superinterfaceTypes + superinterfaceType, Set.empty)
        }
    }

    /**
     * Computes the maximally specific superinterface method with the given name
     * and descriptor
     *
     * @note    This method requires that the class hierarchy is already computed.
     *          It does not required `instanceMethods`.
     * @param   superinterfaceTypes A set of interfaces which potentially declare a method
     *          with the given name and descriptor.
     */
    def findMaximallySpecificSuperinterfaceMethods(
        superinterfaceTypes:         UIDSet[ObjectType],
        name:                        String,
        descriptor:                  MethodDescriptor,
        analyzedSuperinterfaceTypes: UIDSet[ObjectType]
    )(
        implicit
        objectTypeToClassFile: (ObjectType) => Option[ClassFile],
        classHierarchy:        ClassHierarchy,
        logContext:            LogContext
    ): ( /*analyzed types*/ UIDSet[ObjectType], Set[Method]) = {

        val anchor = ((analyzedSuperinterfaceTypes, Set.empty[Method]))

        superinterfaceTypes.foldLeft(anchor) { (currentResult, interfaceType) =>
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

                // Both, the set of `currentMethods` and also the set of `methods`
                // each only contains maximally specific methods w.r.t. their
                // set.
                var currentMaximallySpecificMethods = currentMethods
                var additionalMaximallySpecificMethods = Set.empty[Method]
                methods foreach { method =>
                    if (!currentMethods.contains(method)) {
                        val newMethodDeclaringClassType = method.classFile.thisType
                        var addNewMethod = true
                        currentMaximallySpecificMethods =
                            currentMaximallySpecificMethods.filter { currentMaximallySpecificMethod =>
                                val specificMethodDeclaringClassType = currentMaximallySpecificMethod.classFile.thisType
                                if (specificMethodDeclaringClassType isSubtypeOf newMethodDeclaringClassType) {
                                    addNewMethod = false
                                    true
                                } else if (newMethodDeclaringClassType isSubtypeOf specificMethodDeclaringClassType) {
                                    false
                                } else {
                                    //... we have an incomplete class hierarchy;
                                    // let's keep both methods
                                    true
                                }
                            }
                        if (addNewMethod) additionalMaximallySpecificMethods += method
                    }
                }
                currentMaximallySpecificMethods ++= additionalMaximallySpecificMethods

                val concreteMaximallySpecificMethods = currentMaximallySpecificMethods.filter(!_.isAbstract)
                if (concreteMaximallySpecificMethods.isEmpty) {
                    // We have not yet found any method or we may have multiple abstract methods...
                    (analyzedSuperinterfaceTypes, currentMaximallySpecificMethods)
                } else {
                    (analyzedSuperinterfaceTypes, concreteMaximallySpecificMethods)
                }
            }
        }
    }

    private val EnforceJava11CallSemanticsConfigKey =
        "org.opalj.br.Project.enforceJava11CallSemantics"
}
