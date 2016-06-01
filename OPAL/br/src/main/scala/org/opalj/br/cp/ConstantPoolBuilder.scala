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
package org.opalj
package br
package cp

import org.opalj.br.instructions._
import org.opalj.bytecode.BytecodeProcessingFailedException

/**
 * Factory to compute a constant pool for a given class file.
 *
 * @author Andre Pacak
 * @author Michael Eichberg
 */
object ConstantPoolBuilder {

    def build(classFile: ClassFile): Array[Constant_Pool_Entry] = {
        val cp = new ConstantPoolBuffer()
        val bootstrapMethods = new BootstrapMethodsBuffer()

        def CPEElementValue(elementValue: ElementValue): Unit = elementValue match {
            case ByteValue(value)            ⇒ cp.CPEInteger(value.toInt)
            case CharValue(value)            ⇒ cp.CPEInteger(value.toInt)
            case DoubleValue(value)          ⇒ cp.CPEDouble(value)
            case FloatValue(value)           ⇒ cp.CPEFloat(value)
            case IntValue(value)             ⇒ cp.CPEInteger(value)
            case LongValue(value)            ⇒ cp.CPELong(value)
            case ShortValue(value)           ⇒ cp.CPEInteger(value.toInt)
            case BooleanValue(value)         ⇒ cp.CPEInteger(if (value) 1 else 0)
            case StringValue(value)          ⇒ cp.CPEUtf8(value)
            case ClassValue(value)           ⇒ cp.CPEUtf8(value.toJVMTypeName)
            case AnnotationValue(annotation) ⇒ processAnnotation(annotation)
            case ArrayValue(values)          ⇒ values.foreach { CPEElementValue }
            case EnumValue(enumType, constName) ⇒
                cp.CPEUtf8(enumType.toJVMTypeName)
                cp.CPEUtf8(constName)
        }

        def processAnnotation(annotation: Annotation): Unit = {
            cp.CPEUtf8(annotation.annotationType.toJVMTypeName)
            annotation.elementValuePairs.foreach { evp ⇒
                cp.CPEUtf8(evp.name)
                CPEElementValue(evp.value)
            }
        }

        def collectFromClassFileAttribute(attribute: Attribute): Unit = attribute match {
            case EnclosingMethod(clazz, name, descriptor) ⇒
                cp.CPEUtf8("EnclosingMethod")
                cp.CPEClass(clazz)
                name.foreach { name ⇒ cp.CPENameAndType(name, descriptor.get.toJVMDescriptor) }

            case SourceFile(sourceFile) ⇒
                cp.CPEUtf8("SourceFile")
                cp.CPEUtf8(sourceFile)

            case InnerClassTable(innerClasses) ⇒
                cp.CPEUtf8("InnerClasses")
                innerClasses.foreach { innerClass ⇒
                    cp.CPEClass(innerClass.innerClassType)
                    innerClass.outerClassType.foreach { cp.CPEClass }
                    innerClass.innerName.foreach { cp.CPEUtf8 }
                }

            case SourceDebugExtension(debugExtension) ⇒
                cp.CPEUtf8("SourceDebugExtension")

            case BootstrapMethodTable(methods) ⇒
                cp.CPEUtf8("BootstrapMethods")
                methods.foreach { method ⇒
                    cp.CPEMethodHandle(method.handle)
                    method.arguments.foreach { collectFromBootstrapArgument }
                }

            case signature: ClassSignature ⇒
                cp.CPEUtf8("Signature")
                cp.CPEUtf8(signature.toJVMSignature)

            case default ⇒ collectFromGeneralAttribute(default)

        }
        def collectFromGeneralAttribute(attribute: Attribute): Unit = attribute match {
            case Synthetic  ⇒ cp.CPEUtf8("Synthetic")
            case Deprecated ⇒ cp.CPEUtf8("Deprecated")

            case RuntimeVisibleAnnotationTable(annotations) ⇒
                cp.CPEUtf8("RuntimeVisibleAnnotations")
                annotations.foreach { processAnnotation }
            case RuntimeInvisibleAnnotationTable(annotations) ⇒
                cp.CPEUtf8("RuntimeInvisibleAnnotations")
                annotations.foreach { processAnnotation }
            case RuntimeVisibleTypeAnnotationTable(annotations) ⇒
                cp.CPEUtf8("RuntimeVisibleTypeAnnotations")
            case RuntimeInvisibleTypeAnnotationTable(annotations) ⇒
                cp.CPEUtf8("RuntimeInvisibleTypeAnnotations")

            case UnknownAttribute(name, info) ⇒ cp.CPEUtf8(name)

            case _                            ⇒ new BytecodeProcessingFailedException(s"unknown attribute: $attribute")
        }

        def collectFromMethodAttribute(attribute: Attribute): Unit = attribute match {
            // TODO use KindIds...
            case Code(_, _, instructions, exceptionHandlers, attributes) ⇒
                cp.CPEUtf8("Code")
                for {
                    exceptionHandler ← exceptionHandlers
                    catchType ← exceptionHandler.catchType
                } {
                    cp.CPEClass(catchType)
                }
                attributes.foreach { collectFromCodeAttribute }

            case ExceptionTable(exceptions) ⇒
                cp.CPEUtf8("Exceptions")
                exceptions.foreach(cp.CPEClass)

            case RuntimeVisibleParameterAnnotationTable(annotations) ⇒
                cp.CPEUtf8("RuntimeVisibleParameterAnnotations")
                for {
                    parameterAnnotations ← annotations
                    annotation ← parameterAnnotations
                } {
                    processAnnotation(annotation)
                }
            case RuntimeInvisibleParameterAnnotationTable(annotations) ⇒
                cp.CPEUtf8("RuntimeInvisibleParameterAnnotations")
                for {
                    parameterAnnotations ← annotations
                    annotation ← parameterAnnotations
                } {
                    processAnnotation(annotation)
                }

            case MethodParameterTable(parameters) ⇒
                cp.CPEUtf8("MethodParameters")
                parameters.foreach { mp ⇒ cp.CPEUtf8(mp.name) }

            case signature: MethodTypeSignature ⇒
                cp.CPEUtf8("Signature")
                cp.CPEUtf8(signature.toJVMSignature)

            case UnknownAttribute(name, info) ⇒ cp.CPEUtf8(name)

            case generalAttribute             ⇒ collectFromGeneralAttribute(generalAttribute)

        }

        def collectFromCodeAttribute(attribute: Attribute): Unit = attribute.kindId match {
            case LineNumberTable.KindId ⇒ cp.CPEUtf8("LineNumberTable")

            case LocalVariableTable.KindId ⇒
                val LocalVariableTable(localVariables) = attribute
                cp.CPEUtf8("LocalVariableTable")
                localVariables.foreach { lv ⇒
                    cp.CPEUtf8(lv.name)
                    cp.CPEUtf8(lv.fieldType.toJVMTypeName)
                }
            case LocalVariableTypeTable.KindId ⇒
                val LocalVariableTypeTable(localVariableTypes) = attribute
                cp.CPEUtf8("LocalVariableTypeTable")
                localVariableTypes.foreach { lvt ⇒
                    cp.CPEUtf8(lvt.name)
                    cp.CPEUtf8(lvt.signature.toJVMSignature)
                }
            case StackMapTable.KindId ⇒
                val StackMapTable(stackMapFrames) = attribute
                cp.CPEUtf8("StackMapTable")

                def CPEVerificationTypeInfo(vti: VerificationTypeInfo): Unit = {
                    if (vti.isObjectVariableInfo) {
                        cp.CPEClass(vti.asObjectVariableInfo.clazz)
                    }
                }

                stackMapFrames.foreach {
                    case SameLocals1StackItemFrame(_, verificationType) ⇒
                        CPEVerificationTypeInfo(verificationType)
                    case frame: SameLocals1StackItemFrameExtended ⇒
                        CPEVerificationTypeInfo(frame.verificationTypeInfoStackItem)
                    case frame: AppendFrame ⇒
                        frame.verificationTypeInfoLocals.foreach { CPEVerificationTypeInfo }
                    case FullFrame(_, _, verificationTypeLocals, verificationTypeStack) ⇒
                        verificationTypeLocals.foreach { CPEVerificationTypeInfo }
                        verificationTypeStack.foreach { CPEVerificationTypeInfo }

                    case _ ⇒ /*nothing to do*/
                }

            case _ ⇒ new BytecodeProcessingFailedException(s"unsupported attribute: $attribute")
        }

        def collectFromFieldAttribute(attribute: Attribute): Unit = attribute.kindId match {
            case ConstantInteger.KindId ⇒
                val ConstantInteger(value) = attribute
                cp.CPEUtf8("ConstantValue")
                cp.CPEInteger(value)
            case ConstantFloat.KindId ⇒
                val ConstantFloat(value) = attribute
                cp.CPEUtf8("ConstantValue")
                cp.CPEFloat(value)
            case ConstantLong.KindId ⇒
                val ConstantLong(value) = attribute
                cp.CPEUtf8("ConstantValue")
                cp.CPELong(value)
            case ConstantDouble.KindId ⇒
                val ConstantDouble(value) = attribute
                cp.CPEUtf8("ConstantValue")
                cp.CPEDouble(value)
            case ConstantString.KindId ⇒
                val ConstantString(value) = attribute
                cp.CPEUtf8("ConstantValue")
                cp.CPEString(value)

            case ArrayTypeSignature.KindId | ClassTypeSignature.KindId | TypeVariableSignature.KindId ⇒
                val FieldTypeJVMSignature(jvmSignature) = attribute
                cp.CPEUtf8("Signature")
                cp.CPEUtf8(jvmSignature)

            case _ ⇒ collectFromGeneralAttribute(attribute)
        }

        def collectFromBootstrapArgument(argument: BootstrapArgument): Unit =
            argument match {
                case ConstantString(value)        ⇒ cp.CPEString(value)
                case ConstantClass(refType)       ⇒ cp.CPEClass(refType)
                case ConstantInteger(value)       ⇒ cp.CPEInteger(value)
                case ConstantLong(value)          ⇒ cp.CPELong(value)
                case ConstantFloat(value)         ⇒ cp.CPEFloat(value)
                case ConstantDouble(value)        ⇒ cp.CPEDouble(value)
                case md: MethodDescriptor         ⇒ cp.CPEMethodType(md.toJVMDescriptor)
                case gfmh: GetFieldMethodHandle   ⇒ cp.CPEMethodHandle(gfmh)
                case gsmh: GetStaticMethodHandle  ⇒ cp.CPEMethodHandle(gsmh)
                case pfmh: PutFieldMethodHandle   ⇒ cp.CPEMethodHandle(pfmh)
                case psmh: PutStaticMethodHandle  ⇒ cp.CPEMethodHandle(psmh)
                case mcmh: MethodCallMethodHandle ⇒ cp.CPEMethodHandle(mcmh)
            }

        def collectFromInstruction(instruction: Instruction): Unit =
            instruction.opcode match {

                case GETFIELD.opcode | GETSTATIC.opcode ⇒
                    val FieldReadAccess(objectType, fieldName, fieldType) = instruction
                    cp.CPEFieldRef(objectType, fieldName, fieldType.toJVMTypeName)

                case PUTFIELD.opcode | PUTSTATIC.opcode ⇒
                    val FieldWriteAccess(objectType, fieldName, fieldType) = instruction
                    cp.CPEFieldRef(objectType, fieldName, fieldType.toJVMTypeName)

                case INVOKEVIRTUAL.opcode ⇒
                    val INVOKEVIRTUAL(refType, methodName, descriptor) = instruction
                    cp.CPEMethodRef(
                        refType,
                        methodName,
                        descriptor.toJVMDescriptor
                    )
                case INVOKESTATIC.opcode ⇒
                    val INVOKESTATIC(objectType, methodName, descriptor) = instruction
                    cp.CPEMethodRef(objectType, methodName, descriptor.toJVMDescriptor)

                case INVOKEINTERFACE.opcode ⇒
                    val INVOKEINTERFACE(objectType, methodName, descriptor) = instruction
                    cp.CPEInterfaceMethodRef(objectType, methodName, descriptor.toJVMDescriptor)

                case INVOKESPECIAL.opcode ⇒
                    val INVOKESPECIAL(objectType, methodName, descriptor) = instruction
                    cp.CPEMethodRef(objectType, methodName, descriptor.toJVMDescriptor)

                case INVOKEDYNAMIC.opcode ⇒
                    val INVOKEDYNAMIC(bootstrapMethod, name, descriptor) = instruction
                    cp.CPEMethodHandle(bootstrapMethod.handle)
                    bootstrapMethod.arguments.foreach { collectFromBootstrapArgument }
                    cp.CPEInvokeDynamic(
                        bootstrapMethod,
                        name,
                        descriptor.toJVMDescriptor,
                        bootstrapMethods
                    )

                case LDC.opcode | LDC_W.opcode ⇒
                    instruction match {
                        case LoadInt(value)               ⇒ cp.CPEInteger(value)
                        case LoadFloat(value)             ⇒ cp.CPEFloat(value)
                        case LoadClass(referenceType)     ⇒ cp.CPEClass(referenceType)
                        case LoadMethodHandle(handle)     ⇒ cp.CPEMethodHandle(handle)
                        case LoadMethodType(descriptor)   ⇒ cp.CPEMethodType(descriptor.toJVMDescriptor)
                        case LoadString(value)            ⇒ cp.CPEString(value)

                        case LoadInt_W(value)             ⇒ cp.CPEInteger(value)
                        case LoadFloat_W(value)           ⇒ cp.CPEFloat(value)
                        case LoadClass_W(referenceType)   ⇒ cp.CPEClass(referenceType)
                        case LoadMethodHandle_W(handle)   ⇒ cp.CPEMethodHandle(handle)
                        case LoadMethodType_W(descriptor) ⇒ cp.CPEMethodType(descriptor.toJVMDescriptor)
                        case LoadString_W(value)          ⇒ cp.CPEString(value)
                    }

                case LDC2_W.opcode ⇒
                    instruction match {
                        case LoadLong(value)   ⇒ cp.CPELong(value)
                        case LoadDouble(value) ⇒ cp.CPEDouble(value)
                    }

                case CHECKCAST.opcode ⇒
                    val CHECKCAST(referenceType) = instruction
                    cp.CPEClass(referenceType)

                case INSTANCEOF.opcode ⇒
                    val INSTANCEOF(referenceType) = instruction
                    cp.CPEClass(referenceType)

                case NEW.opcode ⇒
                    val NEW(objectType) = instruction
                    cp.CPEClass(objectType)

                case ANEWARRAY.opcode ⇒
                    val ANEWARRAY(componentType) = instruction
                    cp.CPEClass(componentType)

                case MULTIANEWARRAY.opcode ⇒
                    val MULTIANEWARRAY(componentType, _ /*dim*/ ) = instruction
                    cp.CPEClass(componentType)

                case _ ⇒ // the other instructions do not use the constant pool
            }

        val methods = classFile.methods

        cp.CPEClass(classFile.thisType)
        classFile.superclassType.foreach { cp.CPEClass }
        classFile.interfaceTypes.foreach { cp.CPEClass }

        classFile.attributes.foreach { collectFromClassFileAttribute }

        classFile.fields.foreach { field ⇒
            cp.CPEUtf8(field.name)
            cp.CPEUtf8(field.fieldType.toJVMTypeName)
            field.attributes.foreach { collectFromFieldAttribute }
        }

        methods.foreach { method ⇒
            cp.CPEUtf8(method.name)
            cp.CPEUtf8(method.descriptor.toJVMDescriptor)
            method.attributes.foreach { collectFromMethodAttribute }
            method.body.foreach { collectFromMethodAttribute }
            method.annotationDefault.foreach { ev ⇒
                cp.CPEUtf8("AnnotationDefault")
                CPEElementValue(ev)
            }
            val methodBody = method.body
            if (methodBody.isDefined) {
                methodBody.get.foreachInstruction(collectFromInstruction)
            }
        }

        cp.toArray
    }

    def apply(classFile: ClassFile): Array[Constant_Pool_Entry] = build(classFile)

}