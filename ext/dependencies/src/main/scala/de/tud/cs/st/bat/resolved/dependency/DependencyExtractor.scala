/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package de.tud.cs.st
package bat
package resolved
package dependency

import instructions._

import analyses.SomeProject

/**
 * Traverses a [[SourceElement]] and identifies all dependencies between the element
 * ([[ClassFile]], [[Field]] or [[Method]] declaration) and any element it
 * depends on. The kind of the dependencies that are extracted are defined
 * by the [[DependencyType]] enumeration.
 *
 * ==Concurrency==
 * The `DependencyExtractor` does not perform any kind of parallelization on its own.
 * Users of a `DependencyExtractor` are expected to perform the parallelization (on, e.g.,
 * the level of source elements) if desired.
 *
 * ==Thread Safety==
 * The `DependencyExtractor` does not define any relevant state and, hence, this class is thread
 * compatible.
 * However, if multiple dependency extractors are executed concurrently and
 * share the same [[DependencyProcessor]] or the same `DepencencyExtractor`
 * is used by multiple threads concurrently, the [[DependencyProcessor]] has to be
 * thread safe.
 *
 * @note By default, self dependencies will be reported (e.g. a method that calls
 *      itself). If necessary or undesired, self dependencies can easily be filtered by a
 *      [[DependencyProcessor]]'s `processDependency` method.
 *
 * @note If the DependencyExtractor is extended it is important to delegate all
 *      creations of `VirtualSourceElements` to the [[DependencyProcessor]] to make
 *      sure that the dependency processor can perform, e.g., some ''internalization''.
 *
 * @author Thomas Schlosser
 * @author Michael Eichberg
 */
class DependencyExtractor(
        protected[this] val dependencyProcessor: DependencyProcessor) {

    import DependencyType._

    /**
     * Processes the entire given class file (including all fields, methods,
     * annotations,...).
     * I.e. it extracts all source code dependencies that start from the given
     * class file, its fields or methods, respectively.
     *
     * @param classFile The class file whose dependencies should be extracted.
     */
    def process(classFile: ClassFile): Unit = {
        val vc = dependencyProcessor.asVirtualClass(classFile.thisType)

        if (classFile.superclassType.isDefined)
            processDependency(vc, classFile.superclassType.get, EXTENDS)

        classFile.interfaceTypes foreach { processDependency(vc, _, IMPLEMENTS) }

        classFile.fields foreach { process(vc, _) }

        classFile.methods foreach { process(vc, _) }

        classFile.attributes foreach { attribute ⇒
            val kindId = attribute.kindId
            (kindId: @scala.annotation.switch) match {
                case RuntimeInvisibleAnnotationTable.KindId
                    | RuntimeVisibleAnnotationTable.KindId ⇒
                    attribute.asInstanceOf[AnnotationTable].annotations foreach {
                        process(vc, _)
                    }

                case RuntimeInvisibleTypeAnnotationTable.KindId
                    | RuntimeVisibleTypeAnnotationTable.KindId ⇒
                    attribute.asInstanceOf[TypeAnnotationTable].typeAnnotations foreach {
                        process(vc, _, ANNOTATED_WITH)
                    }

                case EnclosingMethod.KindId ⇒ {
                    val EnclosingMethod(enclosingClass, name, descriptor) = attribute
                    // Check whether the enclosing method attribute refers to an enclosing 
                    // method or an enclosing class.
                    if (name != null && descriptor != null)
                        dependencyProcessor.processDependency(
                            vc,
                            dependencyProcessor.asVirtualMethod(enclosingClass, name, descriptor),
                            IS_ENCLOSED)
                    else
                        processDependency(
                            vc,
                            enclosingClass,
                            IS_ENCLOSED)
                }

                case InnerClassTable.KindId ⇒ {
                    val innerClasses = attribute.asInstanceOf[InnerClassTable].innerClasses
                    val thisType = classFile.thisType
                    for {
                        // Check whether the outer class of the inner class attribute
                        // is equal to the currently processed class. If this is the case,
                        // a dependency from inner class to this class will be added.
                        innerClassesEntry ← innerClasses
                        outerClassType ← innerClassesEntry.outerClassType
                        if outerClassType == thisType
                    } {
                        processDependency(
                            vc,
                            innerClassesEntry.innerClassType,
                            IS_OUTER_CLASS)
                        processDependency(
                            innerClassesEntry.innerClassType,
                            vc,
                            IS_INNER_CLASS)
                    }
                }

                case ClassSignature.KindId
                    | MethodTypeSignature.KindId
                    | ArrayTypeSignature.KindId
                    | ClassTypeSignature.KindId
                    | TypeVariableSignature.KindId ⇒
                    processSignature(vc, attribute.asInstanceOf[Signature])

                // The Synthetic, SourceFile, Deprecated, and 
                // SourceDebugExtension attributes do not create dependencies.
                case Synthetic.KindId            ⇒ /*do nothing*/
                case SourceFile.KindId           ⇒ /*do nothing*/
                case Deprecated.KindId           ⇒ /*do nothing*/
                case SourceDebugExtension.KindId ⇒ /*do nothing*/

                case UnknownAttribute.KindId     ⇒ /*do nothing*/

                // The Java 7 BootstrapMethodTable Attribute is resolved and related
                // dependencies will be extracted when the respective invokedynamic 
                // instructions are processed.

                case _ ⇒
                    throw new BATException(
                        "Unexpected attribute: "+attribute+" for class declarations."
                    )
            }
        }
    }

    /**
     * Extracts all dependencies related to the given field.
     *
     * @param declaringClass This field's declaring class.
     * @param field The field whose dependencies will be extracted.
     */
    protected def process(declaringClass: VirtualClass, field: Field): Unit = {
        val vf = dependencyProcessor.asVirtualField(
            declaringClass.thisType,
            field.name,
            field.fieldType
        )

        dependencyProcessor.processDependency(
            vf,
            declaringClass,
            if (field.isStatic) IS_CLASS_MEMBER else IS_INSTANCE_MEMBER)

        processDependency(vf, field.fieldType, IS_OF_TYPE)

        field.attributes foreach { attribute ⇒
            attribute.kindId match {
                case RuntimeInvisibleAnnotationTable.KindId
                    | RuntimeVisibleAnnotationTable.KindId ⇒
                    attribute.asInstanceOf[AnnotationTable].annotations foreach {
                        process(vf, _)
                    }

                case RuntimeInvisibleTypeAnnotationTable.KindId
                    | RuntimeVisibleTypeAnnotationTable.KindId ⇒
                    attribute.asInstanceOf[TypeAnnotationTable].typeAnnotations foreach {
                        process(vf, _, ANNOTATED_WITH)
                    }

                case ConstantInteger.KindId ⇒
                    processDependency(vf, IntegerType, USES_CONSTANT_VALUE_OF_TYPE)
                case ConstantLong.KindId ⇒
                    processDependency(vf, LongType, USES_CONSTANT_VALUE_OF_TYPE)
                case ConstantFloat.KindId ⇒
                    processDependency(vf, FloatType, USES_CONSTANT_VALUE_OF_TYPE)
                case ConstantDouble.KindId ⇒
                    processDependency(vf, DoubleType, USES_CONSTANT_VALUE_OF_TYPE)
                case ConstantString.KindId ⇒
                    processDependency(vf, ObjectType.String, USES_CONSTANT_VALUE_OF_TYPE)

                case ClassSignature.KindId
                    | MethodTypeSignature.KindId
                    | ArrayTypeSignature.KindId
                    | ClassTypeSignature.KindId
                    | TypeVariableSignature.KindId ⇒
                    processSignature(vf, attribute.asInstanceOf[Signature])

                // Synthetic and Deprecated do not introduce new dependencies
                case Synthetic.KindId  ⇒ /*do nothing*/
                case Deprecated.KindId ⇒ /*do nothing*/

                case _ ⇒
                    throw new BATException(
                        "Unexpected attribute: "+attribute+" for class declarations."
                    )
            }
        }
    }

    /**
     * Extracts all dependencies related to the given method.
     *
     * @param declaringClass The method's declaring class.
     * @param method The method whose dependencies should be extracted.
     */
    protected def process(declaringClass: VirtualClass, method: Method): Unit = {

        val vm = dependencyProcessor.asVirtualMethod(
            declaringClass.thisType,
            method.name,
            method.descriptor)

        val parameterTypes = method.parameterTypes
        val returnType = method.returnType

        dependencyProcessor.processDependency(
            vm,
            declaringClass,
            if (method.isStatic) IS_CLASS_MEMBER else IS_INSTANCE_MEMBER)

        processDependency(vm, returnType, RETURNS)
        parameterTypes foreach { processDependency(vm, _, HAS_PARAMETER_OF_TYPE) }

        // The Java 8 specification defines the following attributes:
        // Code, Exceptions, Synthetic, Signature, Deprecated, RuntimeVisibleAnnotations,
        // RuntimeInvisibleAnnotations, RuntimeVisibleParameterAnnotations,
        // RuntimeInvisibleParameterAnnotations, and AnnotationDefault,
        // RuntimeVisibleTypeAnnotations, RuntimeInvisibleTypeAnnotations, 
        // MethodParameters

        if (method.body.isDefined) {
            val code = method.body.get

            // Process code instructions by calling the process method which is defined in
            // the generated InstructionDependencyExtractor super class.
            process(vm, code.instructions)

            // add dependencies from the method to all throwables that are used in catch statements
            for {
                exceptionHandler ← code.exceptionHandlers
                if exceptionHandler.catchType.isDefined
            } {
                processDependency(vm, exceptionHandler.catchType.get, CATCHES)
            }

            // The Java 8 specification defines the following attributes:
            // LineNumberTable, LocalVariableTable, LocalVariableTypeTable,
            // RuntimeVisibleTypeAnnotations, RuntimeInvisibleTypeAnnotations
            code.attributes foreach { attribute ⇒
                (attribute.kindId: @scala.annotation.switch) match {
                    case LocalVariableTable.KindId ⇒
                        val lvt = attribute.asInstanceOf[LocalVariableTable].localVariables
                        lvt foreach { entry ⇒
                            processDependency(vm, entry.fieldType, HAS_LOCAL_VARIABLE_OF_TYPE)
                        }

                    case LocalVariableTypeTable.KindId ⇒
                        val lvtt = attribute.asInstanceOf[LocalVariableTypeTable].localVariableTypes
                        lvtt foreach { entry ⇒
                            processSignature(vm, entry.signature)
                        }

                    case RuntimeInvisibleTypeAnnotationTable.KindId
                        | RuntimeVisibleTypeAnnotationTable.KindId ⇒
                        attribute.asInstanceOf[TypeAnnotationTable].typeAnnotations foreach {
                            process(vm, _, ANNOTATED_WITH)
                        }

                    // The LineNumberTable and StackMapTable attributes do not define 
                    // relevant dependencies.
                    case StackMapTable.KindId   ⇒ /* Do Nothing */
                    case LineNumberTable.KindId ⇒ /* Do Nothing */

                    case _ ⇒
                        throw new BATException(
                            "Unexpected attribute: "+attribute+" for class declarations."
                        )
                }
            }
        }

        method.attributes foreach { attribute ⇒
            val kindId = attribute.kindId
            (kindId: @scala.annotation.switch) match {
                case ExceptionTable.KindId ⇒
                    val et = attribute.asInstanceOf[ExceptionTable].exceptions
                    et foreach { processDependency(vm, _, THROWS) }

                case ClassSignature.KindId
                    | MethodTypeSignature.KindId
                    | ArrayTypeSignature.KindId
                    | ClassTypeSignature.KindId
                    | TypeVariableSignature.KindId ⇒
                    processSignature(vm, attribute.asInstanceOf[Signature])

                case RuntimeInvisibleAnnotationTable.KindId
                    | RuntimeVisibleAnnotationTable.KindId ⇒
                    attribute.asInstanceOf[AnnotationTable].annotations foreach {
                        process(vm, _, ANNOTATED_WITH)
                    }

                case RuntimeInvisibleTypeAnnotationTable.KindId
                    | RuntimeVisibleTypeAnnotationTable.KindId ⇒
                    attribute.asInstanceOf[TypeAnnotationTable].typeAnnotations foreach {
                        process(vm, _, ANNOTATED_WITH)
                    }

                case RuntimeInvisibleParameterAnnotationTable.KindId
                    | RuntimeVisibleParameterAnnotationTable.KindId ⇒
                    val pas = attribute.asInstanceOf[ParameterAnnotationTable].parameterAnnotations
                    pas foreach { pa ⇒
                        pa foreach { process(vm, _, PARAMETER_ANNOTATED_WITH) }
                    }

                // ElementValues encode annotation default attributes
                case BooleanValue.KindId
                    | ByteValue.KindId
                    | ShortValue.KindId
                    | CharValue.KindId
                    | IntValue.KindId
                    | LongValue.KindId
                    | FloatValue.KindId
                    | DoubleValue.KindId
                    | StringValue.KindId
                    | ClassValue.KindId
                    | EnumValue.KindId
                    | AnnotationValue.KindId
                    | ArrayValue.KindId ⇒
                    processElementValue(vm, attribute.asInstanceOf[ElementValue])

                // Synthetic and Deprecated do not introduce new dependencies
                case Synthetic.KindId  ⇒ /*do nothing*/
                case Deprecated.KindId ⇒ /*do nothing*/

                case _ ⇒
                    throw new BATException(
                        "Unexpected attribute: "+attribute+" for class declarations."
                    )
            }
        }
    }

    /**
     * Processes the given signature.
     *
     * Processing a signature means extracting all references to types
     * that are used in the type parameters that occur in the signature.
     *
     * @param declaringElement The signature's element.
     * @param signature The signature whose type parameters should be analyzed.
     * @param isInTypeParameters `true` if the current signature (part)
     *      is already a part of a type parameter.
     */
    protected def processSignature(
        declaringElement: VirtualSourceElement,
        signature: SignatureElement,
        isInTypeParameters: Boolean = false): Unit = {

        /*
         * Processes the given formal type parameter list.
         * Since they are always part of a type parameter, all types that
         * are found will be extracted, i.e. dependencies to them will be
         * added.
         *
         * Calls the outer `processSignature` method for each
         * defined class and interface bound. The `isInTypeParameters`
         * parameter is set to `true`.
         */
        def processFormalTypeParameters(
            formalTypeParameters: Option[List[FormalTypeParameter]]) {
            formalTypeParameters foreach { list ⇒
                for (ftp ← list) {
                    val classBound = ftp.classBound
                    if (classBound.isDefined)
                        processSignature(declaringElement, classBound.get, true)
                    val interfaceBound = ftp.interfaceBound
                    if (interfaceBound.isDefined)
                        processSignature(declaringElement, interfaceBound.get, true)
                }
            }
        }

        /*
         * Processes the given `SimpleClassTypeSignature in the way
         * that its type arguments will be processed by the `processTypeArguments`
         * method.
         */
        def processSimpleClassTypeSignature(simpleClassTypeSignatures: SimpleClassTypeSignature) {
            processTypeArguments(simpleClassTypeSignatures.typeArguments)
        }

        /*
         * Processes the given option of a type argument list.
         * Since they are always part of a type parameter, all types that
         * are found will be extracted, i.e. dependencies to them will be
         * added.
         *
         * Calls the outer `processSignature` method for each
         * signature that is part of a proper type argument.The
         * `isInTypeParameters` parameter is set to `true`.
         *
         * @param typeArguments The option of a type argument list that should be processed.
         */
        def processTypeArguments(typeArguments: Option[List[TypeArgument]]) {
            typeArguments foreach { args ⇒
                args foreach {

                    case pta: ProperTypeArgument ⇒
                        processSignature(declaringElement, pta.fieldTypeSignature, true)

                    case _ ⇒
                    // Wildcards refer to no type, hence there is nothing to do in this case.
                }
            }
        }

        // process different signature types in a different way
        signature match {

            case ClassSignature(formalTypeParameters, superClassSignature, superInterfacesSignature) ⇒
                processFormalTypeParameters(formalTypeParameters)
                processSignature(declaringElement, superClassSignature)
                superInterfacesSignature foreach { processSignature(declaringElement, _) }

            case MethodTypeSignature(formalTypeParameters, parametersTypeSignatures, returnTypeSignature, throwsSignature) ⇒
                processFormalTypeParameters(formalTypeParameters)
                parametersTypeSignatures foreach { processSignature(declaringElement, _) }
                processSignature(declaringElement, returnTypeSignature)
                throwsSignature foreach { processSignature(declaringElement, _) }

            case cts: ClassTypeSignature ⇒
                // If the class is used in a type parameter, a dependency between the given
                // source and the class is added.
                if (isInTypeParameters)
                    processDependency(declaringElement, cts.objectType, USES_TYPE_IN_TYPE_PARAMETERS)
                processSimpleClassTypeSignature(cts.simpleClassTypeSignature)

            case pta: ProperTypeArgument ⇒
                processSignature(declaringElement, pta.fieldTypeSignature)

            case bt: BaseType ⇒
                if (isInTypeParameters)
                    processDependency(declaringElement, bt, USES_TYPE_IN_TYPE_PARAMETERS)

            case ArrayTypeSignature(typeSignature) ⇒
                processSignature(declaringElement, typeSignature)

            case _ ⇒
            // VoidType, TypeVariableSignature and Wildcard refer to no concrete 
            // type, hence there is nothing to do in this case.
        }
    }

    /**
     * Extracts dependencies related to the given [[ElementValue]].
     *
     * @param declaringElement The element that declares the given element value.
     * @param elementValue The element value that will be analyzed.
     */
    private def processElementValue(
        declaringElement: VirtualSourceElement,
        elementValue: ElementValue): Unit = {

        //        case BooleanValue.KindId ⇒
        //                    processDependency(vm, BooleanType, USES_DEFAULT_ANNOTATION_VALUE_TYPE)

        //        case ByteValue.KindId ⇒
        //                    processDependency(vm, ByteType, USES_DEFAULT_ANNOTATION_VALUE_TYPE)
        //                case CharValue.KindId ⇒
        //                    processDependency(vm, CharType, USES_DEFAULT_ANNOTATION_VALUE_TYPE)
        //case ShortValue.KindId ⇒
        //                    processDependency(vm, ShortType, USES_DEFAULT_ANNOTATION_VALUE_TYPE)
        //case IntValue.KindId ⇒
        //                    processDependency(vm, IntegerType, USES_DEFAULT_ANNOTATION_VALUE_TYPE)
        //case LongValue.KindId ⇒
        //                    processDependency(vm, LongType, USES_DEFAULT_ANNOTATION_VALUE_TYPE)
        //case FloatValue.KindId ⇒
        //                    processDependency(vm, FloatType, USES_DEFAULT_ANNOTATION_VALUE_TYPE)
        //case DoubleValue.KindId ⇒
        //                    processDependency(vm, DoubleType, USES_DEFAULT_ANNOTATION_VALUE_TYPE)

        //case StringValue.KindId ⇒
        //                    processDependency(vm, ObjectType.String , USES_DEFAULT_ANNOTATION_VALUE_TYPE)
        //case AnnotationValue.KindId ⇒
        //                    processDependency(vm, AnnotationType, USES_DEFAULT_ANNOTATION_VALUE_TYPE)
        // case ClassValue.KindId ⇒
        //                    processDependency(vm, ByteType, USES_DEFAULT_ANNOTATION_VALUE_TYPE)
        // case EnumValue.KindId ⇒
        //                    processDependency(vm, ByteType, USES_DEFAULT_ANNOTATION_VALUE_TYPE)
        // case ArrayValue.KindId ⇒
        //                    processDependency(vm, ByteType, USES_DEFAULT_ANNOTATION_VALUE_TYPE)

        elementValue match {

            case EnumValue(enumType, constName) ⇒
                processDependency(declaringElement, enumType, USES_DEFAULT_ENUM_VALUE_TYPE)
                dependencyProcessor.processDependency(
                    declaringElement,
                    dependencyProcessor.asVirtualField(enumType, constName, enumType),
                    USES_ENUM_VALUE)

            case ClassValue(classType) ⇒
                processDependency(declaringElement, classType, USES_DEFAULT_CLASS_VALUE_TYPE)

            case ArrayValue(values) ⇒
                values foreach { processElementValue(declaringElement, _) }

            case AnnotationValue(annotation) ⇒
                process(declaringElement, annotation, USES_DEFAULT_ANNOTATION_VALUE_TYPE)

            case _: StringValue ⇒
                processDependency(declaringElement, ObjectType.String, USES_DEFAULT_ANNOTATION_VALUE_TYPE)

            case btev: BaseTypeElementValue ⇒
                processDependency(declaringElement, btev.baseType, USES_DEFAULT_BASE_TYPE)
        }
    }

    /**
     * Extracts dependencies related to the given annotation.
     *
     * @param declaringElement The (source) element with the annotation.
     * @param annotation The annotation that will be analyzed.
     * @param dependencyType The type of the dependency; default: `ANNOTATED_WITH`.
     */
    private def process(
        declaringElement: VirtualSourceElement,
        annotation: Annotation,
        dependencyType: DependencyType = ANNOTATED_WITH): Unit = {

        processDependency(declaringElement, annotation.annotationType, dependencyType)

        annotation.elementValuePairs foreach { evp ⇒
            processElementValue(declaringElement, evp.value)
        }
    }

    private def process(
        declaringElement: VirtualSourceElement,
        typeAnnotation: TypeAnnotation,
        dependencyType: DependencyType): Unit = {

        processDependency(declaringElement, typeAnnotation.annotationType, dependencyType)

        typeAnnotation.elementValuePairs foreach { evp ⇒
            processElementValue(declaringElement, evp.value)
        }
    }

    /**
     * Extracts dependencies related to the method's implementation.
     *
     * @param declaringMethod The method.
     * @param instructions The instructions that should be analyzed for dependencies.
     */
    private def process(
        declaringMethod: VirtualMethod,
        instructions: Instructions) {

        def as[TargetType <: Instruction](instruction: Instruction) =
            instruction.asInstanceOf[TargetType]

        for (i ← instructions if i != null) {
            (i.opcode: @scala.annotation.switch) match {
                case 178 ⇒
                    val field @ GETSTATIC(declaringClass, fieldName, fieldType) =
                        as[GETSTATIC](i)
                    processDependency(declaringMethod, declaringClass, USES_FIELD_DECLARING_TYPE)
                    processDependency(declaringMethod, fieldType, USES_FIELD_READ_TYPE)
                    dependencyProcessor.processDependency(
                        declaringMethod,
                        dependencyProcessor.asVirtualField(declaringClass, fieldName, fieldType),
                        READS_FIELD)

                case 179 ⇒
                    val field @ PUTSTATIC(declaringClass, fieldName, fieldType) =
                        as[PUTSTATIC](i)
                    processDependency(declaringMethod, declaringClass, USES_FIELD_DECLARING_TYPE)
                    processDependency(declaringMethod, fieldType, USES_FIELD_WRITE_TYPE)
                    dependencyProcessor.processDependency(
                        declaringMethod,
                        dependencyProcessor.asVirtualField(declaringClass, fieldName, fieldType),
                        WRITES_FIELD)

                case 180 ⇒
                    val field @ GETFIELD(declaringClass, fieldName, fieldType) =
                        as[GETFIELD](i)
                    processDependency(declaringMethod, declaringClass, USES_FIELD_DECLARING_TYPE)
                    processDependency(declaringMethod, fieldType, USES_FIELD_READ_TYPE)
                    dependencyProcessor.processDependency(
                        declaringMethod,
                        dependencyProcessor.asVirtualField(declaringClass, fieldName, fieldType),
                        READS_FIELD)

                case 181 ⇒
                    val field @ PUTFIELD(declaringClass, fieldName, fieldType) =
                        as[PUTFIELD](i)
                    processDependency(declaringMethod, declaringClass, USES_FIELD_DECLARING_TYPE)
                    processDependency(declaringMethod, fieldType, USES_FIELD_WRITE_TYPE)
                    dependencyProcessor.processDependency(
                        declaringMethod,
                        dependencyProcessor.asVirtualField(declaringClass, fieldName, fieldType),
                        WRITES_FIELD)

                case 182 ⇒
                    val method @ INVOKEVIRTUAL(declaringClass, name, descriptor) =
                        as[INVOKEVIRTUAL](i)
                    processDependency(declaringMethod, declaringClass, USES_METHOD_DECLARING_TYPE)
                    descriptor.parameterTypes foreach { parameterType ⇒
                        processDependency(declaringMethod, parameterType, USES_PARAMETER_TYPE)
                    }
                    processDependency(declaringMethod, descriptor.returnType, USES_RETURN_TYPE)
                    dependencyProcessor.processDependency(
                        declaringMethod,
                        dependencyProcessor.asVirtualMethod(declaringClass, name, descriptor),
                        CALLS_METHOD)

                case 183 ⇒
                    val method @ INVOKESPECIAL(declaringClass, name, descriptor) =
                        as[INVOKESPECIAL](i)
                    processDependency(declaringMethod, declaringClass, USES_METHOD_DECLARING_TYPE)
                    descriptor.parameterTypes foreach { parameterType ⇒
                        processDependency(declaringMethod, parameterType, USES_PARAMETER_TYPE)
                    }
                    processDependency(declaringMethod, descriptor.returnType, USES_RETURN_TYPE)
                    dependencyProcessor.processDependency(
                        declaringMethod,
                        dependencyProcessor.asVirtualMethod(declaringClass, name, descriptor),
                        CALLS_METHOD)

                case 184 ⇒
                    val method @ INVOKESTATIC(declaringClass, name, descriptor) =
                        as[INVOKESTATIC](i)
                    processDependency(declaringMethod, declaringClass, USES_METHOD_DECLARING_TYPE)
                    descriptor.parameterTypes foreach { parameterType ⇒
                        processDependency(declaringMethod, parameterType, USES_PARAMETER_TYPE)
                    }
                    processDependency(declaringMethod, descriptor.returnType, USES_RETURN_TYPE)
                    dependencyProcessor.processDependency(
                        declaringMethod,
                        dependencyProcessor.asVirtualMethod(declaringClass, name, descriptor),
                        CALLS_METHOD)

                case 185 ⇒
                    val method @ INVOKEINTERFACE(declaringClass, name, descriptor) =
                        as[INVOKEINTERFACE](i)
                    processDependency(declaringMethod, declaringClass, USES_METHOD_DECLARING_TYPE)
                    descriptor.parameterTypes foreach { parameterType ⇒
                        processDependency(declaringMethod, parameterType, USES_PARAMETER_TYPE)
                    }
                    processDependency(declaringMethod, descriptor.returnType, USES_RETURN_TYPE)
                    dependencyProcessor.processDependency(
                        declaringMethod,
                        dependencyProcessor.asVirtualMethod(declaringClass, name, descriptor),
                        CALLS_METHOD)

                case 186 ⇒
                    processInvokedynamic(declaringMethod, as[INVOKEDYNAMIC](i))

                case 187 ⇒
                    processDependency(declaringMethod, as[NEW](i).objectType, CREATES)

                case 189 ⇒
                    processDependency(
                        declaringMethod,
                        as[ANEWARRAY](i).componentType,
                        CREATES_ARRAY_OF_TYPE)

                case 197 ⇒
                    processDependency(
                        declaringMethod,
                        as[MULTIANEWARRAY](i).componentType,
                        CREATES_ARRAY_OF_TYPE)

                case 192 ⇒
                    processDependency(
                        declaringMethod,
                        as[CHECKCAST](i).referenceType,
                        CASTS_INTO)

                case 193 ⇒
                    processDependency(
                        declaringMethod,
                        as[INSTANCEOF](i).referenceType,
                        CHECKS_INSTANCEOF)

                case _ ⇒
            }
        }
    }

    /**
     * Prints a warning that the handling of [[Invokedynamic]] instructions is incomplete
     * (from the point of view of the dependencies to the "real" methods) and then
     * calls [[processInvokedynamicRuntimeDependencies]] to handle the instruction.
     *
     * The warning is only shown once.
     *
     * ==Overriding==
     * This method should be overridden by subclasses that resolve dependencies
     * related to an [[Invokedynamic]] instruction. However, in that case the
     * method [[processInvokedynamicRuntimeDependencies]] should be called explicitly.
     */
    protected[this] def processInvokedynamic(
        declaringMethod: VirtualMethod,
        instruction: INVOKEDYNAMIC): Unit = {

        DependencyExtractor.warnAboutIncompleteHandlingOfInvokedynamic()

        processInvokedynamicRuntimeDependencies(declaringMethod, instruction)
    }

    /**
     * Default implementation for handling [[Invokedynamic]] instructions that only
     * extracts dependencies on the runtime infrastructure (e.g. the bootstrap method
     * and its surrounding class, the types of its arguments and its return type).
     *
     * To gain more information about invokedynamic instructions, a special subclass of
     * DependencyExtractor must be created which overrides this method and performs
     * deeper-going analysis on invokedynamic instructions.
     */
    protected[this] def processInvokedynamicRuntimeDependencies(
        declaringMethod: VirtualMethod,
        instruction: INVOKEDYNAMIC): Unit = {

        val INVOKEDYNAMIC(bootstrapMethod, name, methodDescriptor) = instruction

        // Dependencies related to the invokedynamic instruction's method descriptor.
        // (Most likely simply java/lang/Object for both the parameter and return types.)
        methodDescriptor.parameterTypes foreach { parameterType ⇒
            processDependency(declaringMethod, parameterType, USES_PARAMETER_TYPE)
        }
        processDependency(declaringMethod, methodDescriptor.returnType, USES_RETURN_TYPE)

        bootstrapMethod.methodHandle match {

            case handle: MethodCallMethodHandle ⇒ {
                // the class containing the bootstrap method
                processDependency(
                    declaringMethod,
                    handle.receiverType,
                    USES_METHOD_DECLARING_TYPE)
                // the type of method call
                val callType = handle match {
                    case _: InvokeInterfaceMethodHandle ⇒ CALLS_INTERFACE_METHOD
                    case _                              ⇒ CALLS_METHOD
                }
                dependencyProcessor.processDependency(
                    declaringMethod,
                    dependencyProcessor.asVirtualMethod(
                        handle.receiverType,
                        handle.name,
                        handle.methodDescriptor),
                    callType)
            }

            case handle: FieldAccessMethodHandle ⇒ {
                processDependency(declaringMethod, handle.declaringClassType, USES_FIELD_DECLARING_TYPE)
                handle match {

                    case _: FieldReadAccessMethodHandle ⇒
                        dependencyProcessor.processDependency(
                            declaringMethod,
                            dependencyProcessor.asVirtualField(
                                handle.declaringClassType,
                                handle.name,
                                handle.fieldType),
                            READS_FIELD)
                        processDependency(
                            declaringMethod,
                            handle.fieldType,
                            USES_FIELD_READ_TYPE)
                        processDependency(
                            declaringMethod,
                            handle.declaringClassType,
                            USES_FIELD_DECLARING_TYPE)

                    case _: FieldWriteAccessMethodHandle ⇒
                        dependencyProcessor.processDependency(
                            declaringMethod,
                            dependencyProcessor.asVirtualField(
                                handle.declaringClassType,
                                handle.name,
                                handle.fieldType),
                            WRITES_FIELD)
                        processDependency(
                            declaringMethod,
                            handle.fieldType,
                            USES_FIELD_WRITE_TYPE)
                        processDependency(
                            declaringMethod,
                            handle.declaringClassType,
                            USES_FIELD_DECLARING_TYPE)
                }
            }
        }
    }

    protected[this] def processDependency(
        source: ObjectType,
        target: VirtualSourceElement,
        dType: DependencyType) {
        dependencyProcessor.processDependency(
            dependencyProcessor.asVirtualClass(source),
            target,
            dType)
    }

    protected[this] def processDependency(
        source: ClassFile,
        target: VirtualSourceElement,
        dType: DependencyType): Unit = {
        dependencyProcessor.processDependency(
            dependencyProcessor.asVirtualClass(source.thisType),
            target,
            dType)
    }

    @inline protected[this] def processDependency(
        source: ClassFile,
        target: ObjectType,
        dType: DependencyType): Unit = {
        dependencyProcessor.processDependency(
            dependencyProcessor.asVirtualClass(source.thisType),
            dependencyProcessor.asVirtualClass(target),
            dType)
    }

    @inline protected[this] def processDependency(
        source: VirtualSourceElement,
        target: ObjectType,
        dType: DependencyType): Unit = {
        dependencyProcessor.processDependency(
            source,
            dependencyProcessor.asVirtualClass(target),
            dType)
    }

    @inline protected[this] def processDependency(
        source: VirtualSourceElement,
        target: BaseType,
        dType: DependencyType): Unit = {
        dependencyProcessor.processDependency(source, target, dType)
    }

    @inline protected[this] def processDependency(
        source: VirtualSourceElement,
        target: ArrayType,
        dType: DependencyType): Unit = {
        dependencyProcessor.processDependency(source, target, dType)
    }

    protected[this] def processDependency(
        source: VirtualSourceElement,
        target: Type,
        dType: DependencyType): Unit = {

        if (target eq VoidType)
            return ;

        target match {
            case ot: ObjectType ⇒
                processDependency(source, ot, dType)
            case bt: BaseType ⇒
                processDependency(source, bt, dType)
            case at: ArrayType ⇒
                processDependency(source, at, dType)
                // Recursive call for the dependency on the element type, 
                // which is either an ObjectType or a BaseType.
                processDependency(source, at.elementType, dType)
            case _ /*vt: VoidType*/ ⇒
            // nothing to do
        }
    }

}
/**
 * @author Arne Lottmann
 * @author Michael Eichberg
 */
private object DependencyExtractor {

    private[this] var wasIncompleteHandlingOfInvokedynamicWarningShown = false

    private val incompleteHandlingOfInvokedynamicMessage: String =
        "[info] This project contains invokedynamic instructions. "+
            "Using the currently configured strategy only dependencies to the runtime are resolved."

    def warnAboutIncompleteHandlingOfInvokedynamic(): Unit = {
        if (!wasIncompleteHandlingOfInvokedynamicWarningShown)
            this.synchronized {
                if (!wasIncompleteHandlingOfInvokedynamicWarningShown) {
                    wasIncompleteHandlingOfInvokedynamicWarningShown = true
                    println(incompleteHandlingOfInvokedynamicMessage)
                }
            }
    }
}
