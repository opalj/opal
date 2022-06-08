/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package de

import org.opalj.log.OPALLogger
import org.opalj.log.LogContext
import org.opalj.log.GlobalLogContext
import org.opalj.log.BasicLogMessage
import org.opalj.bytecode.BytecodeProcessingFailedException
import org.opalj.br._
import org.opalj.br.instructions._

/**
 * Traverses a [[org.opalj.br.SourceElement]] and identifies all dependencies between the element
 * ([[org.opalj.br.ClassFile]], [[org.opalj.br.Field]] or [[org.opalj.br.Method]] declaration)
 * and any element it depends on. The kind of the dependencies that are extracted are defined
 * by the [[DependencyType]] enumeration.
 *
 * ==Concurrency==
 * The `DependencyExtractor` does not perform any kind of parallelization on its own.
 * Users of a `DependencyExtractor` are expected to perform the parallelization (e.g.,
 * on the level of source elements) if desired.
 *
 * ==Thread Safety==
 * The `DependencyExtractor` does not define any relevant state and, hence, this
 * class is thread-safe.
 *
 * However, if multiple dependency extractors are executed concurrently and
 * share the same [[DependencyProcessor]] or the same `DepencencyExtractor`
 * is used by multiple threads concurrently, the [[DependencyProcessor]] has to be
 * thread-safe.
 *
 * @note By default, self dependencies will be reported (e.g., a method that calls
 *      itself, a class that defines a field with the same type). If necessary or
 *      undesired, self dependencies can easily be filtered by a
 *      [[DependencyProcessor]]'s `processDependency` method.
 *
 * @note If the DependencyExtractor is extended, it is important to delegate all
 *      creations of `VirtualSourceElements` to the [[DependencyProcessor]] to make
 *      sure that the dependency processor can perform, e.g., some ''internalization''.
 *
 * @author Thomas Schlosser
 * @author Michael Eichberg
 */
class DependencyExtractor(protected[this] val dependencyProcessor: DependencyProcessor) {

    import DependencyTypes._

    /**
     * Extracts all inter source element dependencies of the given class file
     * (including all fields, methods, annotations,... declared by it).
     * I.e., it extracts all source code dependencies that start from the given
     * class file, its fields or methods, respectively.
     *
     * For each extracted dependency the respective dependencyProcessor is called.
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

        classFile.attributes foreach { attribute =>
            val kindId = attribute.kindId
            (kindId: @scala.annotation.switch) match {
                case RuntimeInvisibleAnnotationTable.KindId
                    | RuntimeVisibleAnnotationTable.KindId =>
                    attribute.asInstanceOf[AnnotationTable].annotations foreach {
                        process(vc, _, ANNOTATED_WITH, ANNOTATION_ELEMENT_TYPE)
                    }

                case RuntimeInvisibleTypeAnnotationTable.KindId
                    | RuntimeVisibleTypeAnnotationTable.KindId =>
                    attribute.asInstanceOf[TypeAnnotationTable].typeAnnotations foreach {
                        process(vc, _, ANNOTATED_WITH)
                    }

                case EnclosingMethod.KindId => {
                    val EnclosingMethod(enclosingClass, name, descriptor) = attribute
                    // Check whether the enclosing method attribute refers to an enclosing
                    // method or an enclosing class.
                    if (name.isDefined /*&& descriptor.isDefined*/ )
                        dependencyProcessor.processDependency(
                            vc,
                            dependencyProcessor.asVirtualMethod(enclosingClass, name.get, descriptor.get),
                            ENCLOSED
                        )
                    else
                        processDependency(
                            vc,
                            enclosingClass,
                            ENCLOSED
                        )
                }

                case InnerClassTable.KindId => {
                    val innerClasses = attribute.asInstanceOf[InnerClassTable].innerClasses
                    val thisType = classFile.thisType
                    for {
                        // Check whether the outer class of the inner class attribute
                        // is equal to the currently processed class. If this is the case,
                        // a dependency from inner class to this class will be added.
                        innerClassesEntry <- innerClasses
                        outerClassType <- innerClassesEntry.outerClassType
                        if outerClassType == thisType
                    } {
                        processDependency(
                            vc,
                            innerClassesEntry.innerClassType,
                            OUTER_CLASS
                        )
                        processDependency(
                            innerClassesEntry.innerClassType,
                            vc,
                            INNER_CLASS
                        )
                    }
                }

                case ClassSignature.KindId
                    | MethodTypeSignature.KindId
                    | ArrayTypeSignature.KindId
                    | ClassTypeSignature.KindId
                    | TypeVariableSignature.KindId =>
                    processSignature(vc, attribute.asInstanceOf[Signature])

                // The following attributes do not create dependencies.
                case Synthetic.KindId             => /*do nothing*/
                case SourceFile.KindId            => /*do nothing*/
                case Deprecated.KindId            => /*do nothing*/
                case SourceDebugExtension.KindId  => /*do nothing*/

                // The Java 7 BootstrapMethodTable Attribute is resolved and related
                // dependencies will be extracted when the respective invokedynamic
                // instructions are processed.

                // We know nothing about:
                case UnknownAttribute.KindId      => /*do nothing*/

                // These are "custom attributes"
                case SynthesizedClassFiles.KindId => /*ignore*/
                case VirtualTypeFlag.KindId       => /*ignore*/

                case _ =>
                    val classInfo = classFile.thisType.toJava
                    val message = s"unexpected class attribute: $attribute ($classInfo)"
                    throw BytecodeProcessingFailedException(message)
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
            if (field.isStatic) CLASS_MEMBER else INSTANCE_MEMBER
        )

        processDependency(vf, field.fieldType, FIELD_TYPE)

        field.attributes foreach { attribute =>
            attribute.kindId match {
                case RuntimeInvisibleAnnotationTable.KindId
                    | RuntimeVisibleAnnotationTable.KindId =>
                    attribute.asInstanceOf[AnnotationTable].annotations foreach {
                        process(
                            vf, _, /*the DECLARATION is*/ ANNOTATED_WITH, ANNOTATION_ELEMENT_TYPE
                        )
                    }

                case RuntimeInvisibleTypeAnnotationTable.KindId
                    | RuntimeVisibleTypeAnnotationTable.KindId =>
                    attribute.asInstanceOf[TypeAnnotationTable].typeAnnotations foreach {
                        process(vf, _, ANNOTATED_WITH) // IMPROVE Should be TYPE_ANNOTATED_WITH
                    }

                case ConstantInteger.KindId =>
                    processDependency(vf, IntegerType, CONSTANT_VALUE)
                case ConstantLong.KindId =>
                    processDependency(vf, LongType, CONSTANT_VALUE)
                case ConstantFloat.KindId =>
                    processDependency(vf, FloatType, CONSTANT_VALUE)
                case ConstantDouble.KindId =>
                    processDependency(vf, DoubleType, CONSTANT_VALUE)
                case ConstantString.KindId =>
                    processDependency(vf, ObjectType.String, CONSTANT_VALUE)

                case ClassSignature.KindId
                    | MethodTypeSignature.KindId
                    | ArrayTypeSignature.KindId
                    | ClassTypeSignature.KindId
                    | TypeVariableSignature.KindId =>
                    processSignature(vf, attribute.asInstanceOf[Signature])

                // Synthetic and Deprecated do not introduce new dependencies
                case Synthetic.KindId  => /*do nothing*/
                case Deprecated.KindId => /*do nothing*/

                case _ =>
                    val fieldInfo = field.toJava
                    val message = s"unexpected field attribute: $attribute ($fieldInfo)"
                    throw new BytecodeProcessingFailedException(message)
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
            method.descriptor
        )

        val parameterTypes = method.parameterTypes
        val returnType = method.returnType

        dependencyProcessor.processDependency(
            vm,
            declaringClass,
            if (method.isStatic) CLASS_MEMBER else INSTANCE_MEMBER
        )

        processDependency(vm, returnType, RETURN_TYPE)
        parameterTypes foreach { processDependency(vm, _, PARAMETER_TYPE) }

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
            process(vm, code)

            // add dependencies from the method to all throwables that are used in catch statements
            for {
                exceptionHandler <- code.exceptionHandlers
                if exceptionHandler.catchType.isDefined
            } {
                processDependency(vm, exceptionHandler.catchType.get, CATCHES)
            }

            // The Java 8 specification defines the following attributes:
            // LineNumberTable, LocalVariableTable, LocalVariableTypeTable,
            // RuntimeVisibleTypeAnnotations, RuntimeInvisibleTypeAnnotations
            code.attributes foreach { attribute =>
                (attribute.kindId: @scala.annotation.switch) match {
                    case LocalVariableTable.KindId =>
                        val lvt = attribute.asInstanceOf[LocalVariableTable].localVariables
                        lvt foreach { entry =>
                            processDependency(vm, entry.fieldType, LOCAL_VARIABLE_TYPE)
                        }

                    case LocalVariableTypeTable.KindId =>
                        val lvtt = attribute.asInstanceOf[LocalVariableTypeTable].localVariableTypes
                        lvtt foreach { entry =>
                            processSignature(vm, entry.signature)
                        }

                    case RuntimeInvisibleTypeAnnotationTable.KindId
                        | RuntimeVisibleTypeAnnotationTable.KindId =>
                        attribute.asInstanceOf[TypeAnnotationTable].typeAnnotations foreach {
                            process(vm, _, ANNOTATED_WITH)
                        }

                    // The LineNumberTable and StackMapTable attributes do not define
                    // relevant dependencies.
                    case StackMapTable.KindId   => /* Do Nothing */
                    case LineNumberTable.KindId => /* Do Nothing */

                    case _ =>
                        val methodSignature = method.toJava
                        val message = s"unexpected code attribute: $attribute ($methodSignature)"
                        throw BytecodeProcessingFailedException(message)
                }
            }
        }

        method.attributes foreach { attribute =>
            val kindId = attribute.kindId
            (kindId: @scala.annotation.switch) match {
                case ExceptionTable.KindId =>
                    val et = attribute.asInstanceOf[ExceptionTable].exceptions
                    et foreach { processDependency(vm, _, THROWN_EXCEPTION) }

                case ClassSignature.KindId
                    | MethodTypeSignature.KindId
                    | ArrayTypeSignature.KindId
                    | ClassTypeSignature.KindId
                    | TypeVariableSignature.KindId =>
                    processSignature(vm, attribute.asInstanceOf[Signature])

                case RuntimeInvisibleAnnotationTable.KindId
                    | RuntimeVisibleAnnotationTable.KindId =>
                    attribute.asInstanceOf[AnnotationTable].annotations foreach {
                        process(vm, _, ANNOTATED_WITH, ANNOTATION_ELEMENT_TYPE)
                    }

                case RuntimeInvisibleTypeAnnotationTable.KindId
                    | RuntimeVisibleTypeAnnotationTable.KindId =>
                    attribute.asInstanceOf[TypeAnnotationTable].typeAnnotations foreach {
                        process(vm, _, ANNOTATED_WITH)
                    }

                case RuntimeInvisibleParameterAnnotationTable.KindId
                    | RuntimeVisibleParameterAnnotationTable.KindId =>
                    val pas = attribute.asInstanceOf[ParameterAnnotationTable].parameterAnnotations
                    pas foreach { pa =>
                        pa foreach { process(vm, _, PARAMETER_ANNOTATED_WITH, ANNOTATION_ELEMENT_TYPE) }
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
                    | ArrayValue.KindId =>
                    processElementValue(
                        vm,
                        attribute.asInstanceOf[ElementValue],
                        ANNOTATION_DEFAULT_VALUE_TYPE
                    )

                // Synthetic and Deprecated do not introduce new dependencies
                case Synthetic.KindId            => /* nothing to do */
                case Deprecated.KindId           => /* nothing to do */

                case MethodParameterTable.KindId => /* nothing to do */

                case _ =>
                    val methodSignature = method.toJava
                    val message = s"unexpected method attribute: $attribute ($methodSignature)"
                    throw BytecodeProcessingFailedException(message)
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
        declaringElement:   VirtualSourceElement,
        signature:          SignatureElement,
        isInTypeParameters: Boolean              = false
    ): Unit = {

        /*
         * Processes the given formal type parameter list.
         * Since they are always part of a type parameter, all types that
         * are found will be extracted, i.e., dependencies to them will be
         * added.
         *
         * Calls the outer `processSignature` method for each
         * defined class and interface bound. The `isInTypeParameters`
         * parameter is set to `true`.
         */
        def processFormalTypeParameters(
            formalTypeParameters: List[FormalTypeParameter]
        ): Unit = {
            for (ftp <- formalTypeParameters) {
                val classBound = ftp.classBound
                if (classBound.isDefined)
                    processSignature(declaringElement, classBound.get, true)
                val interfaceBound = ftp.interfaceBound
                interfaceBound.foreach { interfaceBound =>
                    processSignature(declaringElement, interfaceBound, true)
                }
            }
        }

        /*
         * Processes the given `SimpleClassTypeSignature in the way
         * that its type arguments will be processed by the `processTypeArguments`
         * method.
         */
        def processSimpleClassTypeSignature(
            simpleClassTypeSignatures: SimpleClassTypeSignature
        ): Unit = {

            processTypeArguments(simpleClassTypeSignatures.typeArguments)
        }

        /*
         * Processes the given option of a type argument list.
         * Since they are always part of a type parameter, all types that
         * are found will be extracted, i.e., dependencies to them will be
         * added.
         *
         * Calls the outer `processSignature` method for each
         * signature that is part of a proper type argument.The
         * `isInTypeParameters` parameter is set to `true`.
         *
         * @param typeArguments The option of a type argument list that should be processed.
         */
        def processTypeArguments(typeArguments: List[TypeArgument]): Unit = {
            typeArguments foreach {
                case pta: ProperTypeArgument =>
                    processSignature(declaringElement, pta.fieldTypeSignature, true)

                case _ =>
                // Wildcards refer to no type, hence there is nothing to do in this case.
            }
        }

        // process different signature types in a different way
        signature match {

            case ClassSignature(formalTypeParameters, superClassSignature, superInterfacesSignature) =>
                processFormalTypeParameters(formalTypeParameters)
                processSignature(declaringElement, superClassSignature)
                superInterfacesSignature foreach { processSignature(declaringElement, _) }

            case MethodTypeSignature(formalTypeParameters, parametersTypeSignatures, returnTypeSignature, throwsSignature) =>
                processFormalTypeParameters(formalTypeParameters)
                parametersTypeSignatures foreach { processSignature(declaringElement, _) }
                processSignature(declaringElement, returnTypeSignature)
                throwsSignature foreach { processSignature(declaringElement, _) }

            case cts: ClassTypeSignature =>
                // If the class is used in a type parameter, a dependency between the given
                // source and the class is added.
                if (isInTypeParameters)
                    processDependency(declaringElement, cts.objectType, TYPE_IN_TYPE_PARAMETERS)
                processSimpleClassTypeSignature(cts.simpleClassTypeSignature)

            case pta: ProperTypeArgument =>
                processSignature(declaringElement, pta.fieldTypeSignature)

            case bt: BaseType =>
                if (isInTypeParameters)
                    processDependency(declaringElement, bt, TYPE_IN_TYPE_PARAMETERS)

            case ArrayTypeSignature(typeSignature) =>
                processSignature(declaringElement, typeSignature)

            case _ =>
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
        elementValue:     ElementValue,
        dType:            DependencyType
    ): Unit = {

        //        case BooleanValue.KindId =>
        //                    processDependency(vm, BooleanType, USES_DEFAULT_ANNOTATION_VALUE_TYPE)

        //        case ByteValue.KindId =>
        //                    processDependency(vm, ByteType, USES_DEFAULT_ANNOTATION_VALUE_TYPE)
        //                case CharValue.KindId =>
        //                    processDependency(vm, CharType, USES_DEFAULT_ANNOTATION_VALUE_TYPE)
        //case ShortValue.KindId =>
        //                    processDependency(vm, ShortType, USES_DEFAULT_ANNOTATION_VALUE_TYPE)
        //case IntValue.KindId =>
        //                    processDependency(vm, IntegerType, USES_DEFAULT_ANNOTATION_VALUE_TYPE)
        //case LongValue.KindId =>
        //                    processDependency(vm, LongType, USES_DEFAULT_ANNOTATION_VALUE_TYPE)
        //case FloatValue.KindId =>
        //                    processDependency(vm, FloatType, USES_DEFAULT_ANNOTATION_VALUE_TYPE)
        //case DoubleValue.KindId =>
        //                    processDependency(vm, DoubleType, USES_DEFAULT_ANNOTATION_VALUE_TYPE)

        //case StringValue.KindId =>
        //                    processDependency(vm, ObjectType.String , USES_DEFAULT_ANNOTATION_VALUE_TYPE)
        //case AnnotationValue.KindId =>
        //                    processDependency(vm, AnnotationType, USES_DEFAULT_ANNOTATION_VALUE_TYPE)
        // case ClassValue.KindId =>
        //                    processDependency(vm, ByteType, USES_DEFAULT_ANNOTATION_VALUE_TYPE)
        // case EnumValue.KindId =>
        //                    processDependency(vm, ByteType, USES_DEFAULT_ANNOTATION_VALUE_TYPE)
        // case ArrayValue.KindId =>
        //                    processDependency(vm, ByteType, USES_DEFAULT_ANNOTATION_VALUE_TYPE)

        elementValue match {

            case EnumValue(enumType, constName) =>
                processDependency(declaringElement, enumType, dType)
                dependencyProcessor.processDependency(
                    declaringElement,
                    dependencyProcessor.asVirtualField(enumType, constName, enumType),
                    USES_ENUM_VALUE
                )

            case ClassValue(classType) =>
                processDependency(declaringElement, classType, dType)

            case ArrayValue(values) =>
                values foreach { processElementValue(declaringElement, _, dType) }

            case AnnotationValue(annotation) =>
                process(declaringElement, annotation, dType, dType)

            case _: StringValue =>
                processDependency(declaringElement, ObjectType.String, dType)

            case btev: BaseTypeElementValue =>
                processDependency(declaringElement, btev.baseType, dType)
        }
    }

    /**
     * Extracts dependencies related to the given annotation.
     *
     * @param declaringElement The (source) element with the annotation.
     * @param annotation The annotation that will be analyzed.
     */
    private def process(
        declaringElement:         VirtualSourceElement,
        annotation:               Annotation,
        annotationDependencyType: DependencyType,
        elementDependencyType:    DependencyType
    ): Unit = {

        processDependency(declaringElement, annotation.annotationType, annotationDependencyType)

        annotation.elementValuePairs foreach { evp =>
            processElementValue(declaringElement, evp.value, elementDependencyType)
        }
    }

    private def process(
        declaringElement: VirtualSourceElement,
        typeAnnotation:   TypeAnnotation,
        dependencyType:   DependencyType
    ): Unit = {

        processDependency(declaringElement, typeAnnotation.annotationType, dependencyType)

        typeAnnotation.elementValuePairs foreach { evp =>
            processElementValue(declaringElement, evp.value, ANNOTATION_ELEMENT_TYPE)
        }
    }

    /**
     * Extracts dependencies related to the method's implementation.
     *
     * @param declaringMethod The method.
     * @param code The code that should be analyzed for dependencies.
     */
    private def process(
        declaringMethod: VirtualMethod,
        code:            Code
    ): Unit = {

        def as[TargetType <: Instruction](instruction: Instruction) =
            instruction.asInstanceOf[TargetType]
        val instructions = code.instructions
        val instructionCount = instructions.length
        var i = 0
        while (i < instructionCount) {
            val instruction = instructions(i)
            (instruction.opcode: @scala.annotation.switch) match {

                case LDC.opcode /*18*/ =>
                    instruction match {
                        case LoadString(_) =>
                            processDependency(declaringMethod, ObjectType.String, LOADS_CONSTANT)
                        case LoadClass(value: ReferenceType) =>
                            processDependency(declaringMethod, value, LOADS_CONSTANT)
                            processDependency(declaringMethod, ObjectType.Class, LOADS_CONSTANT)
                        case LoadMethodHandle(_) =>
                            processDependency(declaringMethod, ObjectType.MethodHandle, LOADS_CONSTANT)
                        case LoadMethodType(_) =>
                            processDependency(declaringMethod, ObjectType.MethodType, LOADS_CONSTANT)

                        case _ => /*not relevant*/
                    }

                case LDC_W.opcode /*19*/ =>
                    instruction match {
                        case LoadString_W(_) =>
                            processDependency(declaringMethod, ObjectType.String, LOADS_CONSTANT)
                        case LoadClass_W(value: ReferenceType) =>
                            processDependency(declaringMethod, value, LOADS_CONSTANT)
                            processDependency(declaringMethod, ObjectType.Class, LOADS_CONSTANT)
                        case LoadMethodHandle_W(_) =>
                            processDependency(declaringMethod, ObjectType.MethodHandle, LOADS_CONSTANT)
                        case LoadMethodType_W(_) =>
                            processDependency(declaringMethod, ObjectType.MethodType, LOADS_CONSTANT)

                        case _ => /*not relevant*/
                    }

                case 178 =>
                    val getstatic = as[GETSTATIC](instruction)
                    val declaringClass = getstatic.declaringClass
                    val fieldName = getstatic.name
                    val fieldType = getstatic.fieldType
                    processDependency(declaringMethod, declaringClass, DECLARING_CLASS_OF_ACCESSED_FIELD)
                    processDependency(declaringMethod, fieldType, TYPE_OF_ACCESSED_FIELD)
                    dependencyProcessor.processDependency(
                        declaringMethod,
                        dependencyProcessor.asVirtualField(declaringClass, fieldName, fieldType),
                        READS_FIELD
                    )

                case 179 =>
                    val putstatic = as[PUTSTATIC](instruction)
                    val declaringClass = putstatic.declaringClass
                    val fieldName = putstatic.name
                    val fieldType = putstatic.fieldType
                    processDependency(declaringMethod, declaringClass, DECLARING_CLASS_OF_ACCESSED_FIELD)
                    processDependency(declaringMethod, fieldType, TYPE_OF_ACCESSED_FIELD)
                    dependencyProcessor.processDependency(
                        declaringMethod,
                        dependencyProcessor.asVirtualField(declaringClass, fieldName, fieldType),
                        WRITES_FIELD
                    )

                case 180 =>
                    val getfield = as[GETFIELD](instruction)
                    val declaringClass = getfield.declaringClass
                    val fieldName = getfield.name
                    val fieldType = getfield.fieldType
                    processDependency(declaringMethod, declaringClass, DECLARING_CLASS_OF_ACCESSED_FIELD)
                    processDependency(declaringMethod, fieldType, TYPE_OF_ACCESSED_FIELD)
                    dependencyProcessor.processDependency(
                        declaringMethod,
                        dependencyProcessor.asVirtualField(declaringClass, fieldName, fieldType),
                        READS_FIELD
                    )

                case 181 =>
                    val putfield = as[PUTFIELD](instruction)
                    val declaringClass = putfield.declaringClass
                    val fieldName = putfield.name
                    val fieldType = putfield.fieldType
                    processDependency(declaringMethod, declaringClass, DECLARING_CLASS_OF_ACCESSED_FIELD)
                    processDependency(declaringMethod, fieldType, TYPE_OF_ACCESSED_FIELD)
                    dependencyProcessor.processDependency(
                        declaringMethod,
                        dependencyProcessor.asVirtualField(declaringClass, fieldName, fieldType),
                        WRITES_FIELD
                    )

                case 182 =>
                    val invokeInstruction = as[INVOKEVIRTUAL](instruction)
                    val declaringClass = invokeInstruction.declaringClass
                    val name = invokeInstruction.name
                    val descriptor = invokeInstruction.methodDescriptor
                    processDependency(declaringMethod, declaringClass, DECLARING_CLASS_OF_CALLED_METHOD)
                    descriptor.parameterTypes foreach { parameterType =>
                        processDependency(declaringMethod, parameterType, PARAMETER_TYPE_OF_CALLED_METHOD)
                    }
                    processDependency(declaringMethod, descriptor.returnType, RETURN_TYPE_OF_CALLED_METHOD)
                    dependencyProcessor.processDependency(
                        declaringMethod,
                        dependencyProcessor.asVirtualMethod(declaringClass, name, descriptor),
                        CALLS_METHOD
                    )

                case 183 =>
                    val invokeInstruction = as[INVOKESPECIAL](instruction)
                    val declaringClass = invokeInstruction.declaringClass
                    val name = invokeInstruction.name
                    val descriptor = invokeInstruction.methodDescriptor
                    processDependency(declaringMethod, declaringClass, DECLARING_CLASS_OF_CALLED_METHOD)
                    descriptor.parameterTypes foreach { parameterType =>
                        processDependency(declaringMethod, parameterType, PARAMETER_TYPE_OF_CALLED_METHOD)
                    }
                    processDependency(declaringMethod, descriptor.returnType, RETURN_TYPE_OF_CALLED_METHOD)
                    dependencyProcessor.processDependency(
                        declaringMethod,
                        dependencyProcessor.asVirtualMethod(declaringClass, name, descriptor),
                        CALLS_METHOD
                    )

                case 184 =>
                    val invokeInstruction = as[INVOKESTATIC](instruction)
                    val declaringClass = invokeInstruction.declaringClass
                    val name = invokeInstruction.name
                    val descriptor = invokeInstruction.methodDescriptor
                    processDependency(declaringMethod, declaringClass, DECLARING_CLASS_OF_CALLED_METHOD)
                    descriptor.parameterTypes foreach { parameterType =>
                        processDependency(declaringMethod, parameterType, PARAMETER_TYPE_OF_CALLED_METHOD)
                    }
                    processDependency(declaringMethod, descriptor.returnType, RETURN_TYPE_OF_CALLED_METHOD)
                    dependencyProcessor.processDependency(
                        declaringMethod,
                        dependencyProcessor.asVirtualMethod(declaringClass, name, descriptor),
                        CALLS_METHOD
                    )

                case 185 =>
                    val invokeInstruction = as[INVOKEINTERFACE](instruction)
                    val declaringClass = invokeInstruction.declaringClass
                    val name = invokeInstruction.name
                    val descriptor = invokeInstruction.methodDescriptor
                    processDependency(declaringMethod, declaringClass, DECLARING_CLASS_OF_CALLED_METHOD)
                    descriptor.parameterTypes foreach { parameterType =>
                        processDependency(declaringMethod, parameterType, PARAMETER_TYPE_OF_CALLED_METHOD)
                    }
                    processDependency(declaringMethod, descriptor.returnType, RETURN_TYPE_OF_CALLED_METHOD)
                    dependencyProcessor.processDependency(
                        declaringMethod,
                        dependencyProcessor.asVirtualMethod(declaringClass, name, descriptor),
                        CALLS_METHOD
                    )

                case 186 =>
                    processInvokedynamic(declaringMethod, as[INVOKEDYNAMIC](instruction))

                case 187 =>
                    processDependency(declaringMethod, as[NEW](instruction).objectType, CREATES)

                case NEWARRAY.opcode =>
                    processDependency(
                        declaringMethod,
                        as[NEWARRAY](instruction).arrayType,
                        CREATES_ARRAY
                    )

                case 189 =>
                    processDependency(
                        declaringMethod,
                        as[ANEWARRAY](instruction).arrayType,
                        CREATES_ARRAY
                    )

                case 197 =>
                    processDependency(
                        declaringMethod,
                        as[MULTIANEWARRAY](instruction).arrayType,
                        CREATES_ARRAY
                    )

                case 192 =>
                    processDependency(
                        declaringMethod,
                        as[CHECKCAST](instruction).referenceType,
                        TYPECAST
                    )

                case 193 =>
                    processDependency(
                        declaringMethod,
                        as[INSTANCEOF](instruction).referenceType,
                        TYPECHECK
                    )

                case _ =>
            }
            i = instruction.indexOfNextInstruction(i)(code)
        }
    }

    /**
     * Prints a warning that the handling of
     * [[org.opalj.br.instructions.INVOKEDYNAMIC]] instructions is incomplete
     * (from the point of view of the dependencies to the "real" methods) and then
     * calls [[processInvokedynamicRuntimeDependencies]] to handle the instruction.
     *
     * The warning is only shown once.
     *
     * ==Overriding==
     * This method should be overridden by subclasses that resolve dependencies
     * related to an `Invokedynamic` instruction. However, in that case the
     * method [[processInvokedynamicRuntimeDependencies]] should be called explicitly.
     */
    protected[this] def processInvokedynamic(
        declaringMethod: VirtualMethod,
        instruction:     INVOKEDYNAMIC
    ): Unit = {
        DependencyExtractor.warnAboutIncompleteHandlingOfInvokedynamic()
        processInvokedynamicRuntimeDependencies(declaringMethod, instruction)
    }

    /**
     * Default implementation for handling `Invokedynamic` instructions that only
     * extracts dependencies on the runtime infrastructure (e.g., the bootstrap method
     * and its surrounding class, the types of its arguments and its return type).
     *
     * To gain more information about invokedynamic instructions, a special subclass of
     * DependencyExtractor must be created which overrides this method and performs
     * deeper-going analysis on invokedynamic instructions.
     */
    protected[this] def processInvokedynamicRuntimeDependencies(
        declaringMethod: VirtualMethod,
        instruction:     INVOKEDYNAMIC
    ): Unit = {

        val INVOKEDYNAMIC(bootstrapMethod, _ /*name*/ , methodDescriptor) = instruction

        // Dependencies related to the invokedynamic instruction's method descriptor.
        // (Most likely simply java/lang/Object for both the parameter and return types.)
        methodDescriptor.parameterTypes foreach { parameterType =>
            processDependency(declaringMethod, parameterType, PARAMETER_TYPE_OF_CALLED_METHOD)
        }
        processDependency(declaringMethod, methodDescriptor.returnType, RETURN_TYPE_OF_CALLED_METHOD)

        bootstrapMethod.handle match {

            case handle: MethodCallMethodHandle => {
                // the class containing the bootstrap method
                processDependency(
                    declaringMethod,
                    handle.receiverType,
                    DECLARING_CLASS_OF_CALLED_METHOD
                )
                // the type of method call
                val callType = handle match {
                    case _: InvokeInterfaceMethodHandle => CALLS_METHOD
                    case _                              => CALLS_METHOD
                }
                dependencyProcessor.processDependency(
                    declaringMethod,
                    dependencyProcessor.asVirtualMethod(
                        handle.receiverType,
                        handle.name,
                        handle.methodDescriptor
                    ),
                    callType
                )
            }

            case handle: FieldAccessMethodHandle => {
                processDependency(
                    declaringMethod,
                    handle.declaringClassType,
                    DECLARING_CLASS_OF_ACCESSED_FIELD
                )

                handle match {

                    case _: FieldReadAccessMethodHandle =>
                        dependencyProcessor.processDependency(
                            declaringMethod,
                            dependencyProcessor.asVirtualField(
                                handle.declaringClassType,
                                handle.name,
                                handle.fieldType
                            ),
                            READS_FIELD
                        )
                        processDependency(declaringMethod, handle.fieldType, TYPE_OF_ACCESSED_FIELD)
                        processDependency(
                            declaringMethod,
                            handle.declaringClassType,
                            DECLARING_CLASS_OF_ACCESSED_FIELD
                        )

                    case _: FieldWriteAccessMethodHandle =>
                        dependencyProcessor.processDependency(
                            declaringMethod,
                            dependencyProcessor.asVirtualField(
                                handle.declaringClassType,
                                handle.name,
                                handle.fieldType
                            ),
                            WRITES_FIELD
                        )
                        processDependency(
                            declaringMethod,
                            handle.fieldType,
                            TYPE_OF_ACCESSED_FIELD
                        )
                        processDependency(
                            declaringMethod,
                            handle.declaringClassType,
                            DECLARING_CLASS_OF_ACCESSED_FIELD
                        )
                }
            }
        }
    }

    protected[this] def processDependency(
        source: ObjectType,
        target: VirtualSourceElement,
        dType:  DependencyType
    ): Unit = {
        dependencyProcessor.processDependency(
            dependencyProcessor.asVirtualClass(source),
            target,
            dType
        )
    }

    protected[this] def processDependency(
        source: ClassFile,
        target: VirtualSourceElement,
        dType:  DependencyType
    ): Unit = {
        dependencyProcessor.processDependency(
            dependencyProcessor.asVirtualClass(source.thisType),
            target,
            dType
        )
    }

    @inline protected[this] def processDependency(
        source: ClassFile,
        target: ObjectType,
        dType:  DependencyType
    ): Unit = {
        dependencyProcessor.processDependency(
            dependencyProcessor.asVirtualClass(source.thisType),
            dependencyProcessor.asVirtualClass(target),
            dType
        )
    }

    @inline protected[this] def processDependency(
        source: VirtualSourceElement,
        target: ObjectType,
        dType:  DependencyType
    ): Unit = {
        dependencyProcessor.processDependency(
            source,
            dependencyProcessor.asVirtualClass(target),
            dType
        )
    }

    @inline protected[this] def processDependency(
        source: VirtualSourceElement,
        target: BaseType,
        dType:  DependencyType
    ): Unit = {
        dependencyProcessor.processDependency(source, target, dType)
    }

    @inline protected[this] def processDependency(
        source: VirtualSourceElement,
        target: ArrayType,
        dType:  DependencyType
    ): Unit = {
        dependencyProcessor.processDependency(source, target, dType)
    }

    protected[this] def processDependency(
        source: VirtualSourceElement,
        target: Type,
        dType:  DependencyType
    ): Unit = {

        if (target eq VoidType)
            return ;

        target match {
            case ot: ObjectType =>
                processDependency(source, ot, dType)
            case bt: BaseType =>
                processDependency(source, bt, dType)
            case at: ArrayType =>
                processDependency(source, at, dType)

            case _ /*vt: VoidType*/ =>
            // nothing to do
        }
    }

}
/**
 * @author Arne Lottmann
 * @author Michael Eichberg
 */
private object DependencyExtractor {

    private[this] final val incompleteHandlingOfInvokedynamicMessage: String = {
        "for the code's invokedynamic instructions only dependencies to the runtime are resolved"
    }

    @volatile private[this] var incompleteHandlingOfInvokedynamicWasLogged = false

    def warnAboutIncompleteHandlingOfInvokedynamic(): Unit = {
        if (!incompleteHandlingOfInvokedynamicWasLogged) this.synchronized {
            if (!incompleteHandlingOfInvokedynamicWasLogged) {
                incompleteHandlingOfInvokedynamicWasLogged = true
                implicit val logContext: LogContext = GlobalLogContext
                OPALLogger.log(BasicLogMessage(message = incompleteHandlingOfInvokedynamicMessage))
            }
        }
    }
}
