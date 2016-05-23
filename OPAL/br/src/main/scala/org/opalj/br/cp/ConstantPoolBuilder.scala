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

        def insertElementValue(elementValue: ElementValue): Unit = elementValue match {
            case ByteValue(value)            ⇒ cp.insertInteger(value.toInt)
            case CharValue(value)            ⇒ cp.insertInteger(value.toInt)
            case DoubleValue(value)          ⇒ cp.insertDouble(value)
            case FloatValue(value)           ⇒ cp.insertFloat(value)
            case IntValue(value)             ⇒ cp.insertInteger(value)
            case LongValue(value)            ⇒ cp.insertLong(value)
            case ShortValue(value)           ⇒ cp.insertInteger(value.toInt)
            case BooleanValue(value)         ⇒ cp.insertInteger(if (value) 1 else 0)
            case StringValue(value)          ⇒ cp.insertUtf8(value)
            case ClassValue(value)           ⇒ cp.insertUtf8(value.toJVMTypeName)
            case AnnotationValue(annotation) ⇒ processAnnotation(annotation)
            case ArrayValue(values)          ⇒ values.foreach { insertElementValue }
            case EnumValue(enumType, constName) ⇒
                cp.insertUtf8(enumType.toJVMTypeName)
                cp.insertUtf8(constName)
        }

        def processAnnotation(annotation: Annotation): Unit = {
            cp.insertUtf8(annotation.annotationType.toJVMTypeName)
            annotation.elementValuePairs.foreach { evp ⇒
                cp.insertUtf8(evp.name)
                insertElementValue(evp.value)
            }
        }

        def collectFromClassFileAttribute(attribute: Attribute): Unit = attribute match {
            case EnclosingMethod(clazz, name, descriptor) ⇒
                cp.insertUtf8("EnclosingMethod")
                cp.insertClass(clazz)
                for { // FIXME This looks very suspicious
                    n ← name
                    d ← descriptor
                } {
                    cp.insertNameAndType(n, d.toJVMDescriptor)
                }
            case SourceFile(sourceFile) ⇒
                cp.insertUtf8("SourceFile")
                cp.insertUtf8(sourceFile)
            case InnerClassTable(innerClasses) ⇒
                cp.insertUtf8("InnerClasses")
                innerClasses.foreach { innerClass ⇒
                    cp.insertClass(innerClass.innerClassType)
                    innerClass.outerClassType.foreach { cp.insertClass }
                    innerClass.innerName.foreach { cp.insertUtf8 }
                }
            case SourceDebugExtension(debugExtension) ⇒
                cp.insertUtf8("SourceDebugExtension")
            case BootstrapMethodTable(methods) ⇒
                cp.insertUtf8("BootstrapMethods")
                methods.foreach { method ⇒
                    cp.insertMethodHandle(method.handle)
                    method.arguments.foreach { collectFromBootstrapArgument }
                }
            case signature: ClassSignature ⇒
                cp.insertUtf8("Signature")
                cp.insertUtf8(signature.toJVMSignature)

            case default ⇒ collectFromGeneralAttribute(default)

        }
        def collectFromGeneralAttribute(attribute: Attribute): Unit = attribute match {
            case Synthetic  ⇒ cp.insertUtf8("Synthetic")
            case Deprecated ⇒ cp.insertUtf8("Deprecated")

            case RuntimeVisibleAnnotationTable(annotations) ⇒
                cp.insertUtf8("RuntimeVisibleAnnotations")
                annotations.foreach { processAnnotation }
            case RuntimeInvisibleAnnotationTable(annotations) ⇒
                cp.insertUtf8("RuntimeInvisibleAnnotations")
                annotations.foreach { processAnnotation }
            case RuntimeVisibleTypeAnnotationTable(annotations) ⇒
                cp.insertUtf8("RuntimeVisibleTypeAnnotations")
            case RuntimeInvisibleTypeAnnotationTable(annotations) ⇒
                cp.insertUtf8("RuntimeInvisibleTypeAnnotations")

            case UnknownAttribute(name, info) ⇒ cp.insertUtf8(name)

            case _                            ⇒ new BytecodeProcessingFailedException(s"unknown attribute: $attribute")
        }

        def collectFromMethodAttribute(attribute: Attribute): Unit = attribute match {
            // TODO use KindIds...
            case Code(_, _, instructions, exceptionHandlers, attributes) ⇒
                cp.insertUtf8("Code")
                for {
                    exceptionHandler ← exceptionHandlers
                    catchType ← exceptionHandler.catchType
                } {
                    cp.insertClass(catchType)
                }
                attributes.foreach { collectFromCodeAttribute }
            case ExceptionTable(exceptions) ⇒
                cp.insertUtf8("Exceptions")
                exceptions.foreach(cp.insertClass)
            case RuntimeVisibleParameterAnnotationTable(annotations) ⇒
                cp.insertUtf8("RuntimeVisibleParameterAnnotations")
                for {
                    parameterAnnotations ← annotations
                    annotation ← parameterAnnotations
                } {
                    processAnnotation(annotation)
                }
            case RuntimeInvisibleParameterAnnotationTable(annotations) ⇒
                cp.insertUtf8("RuntimeInvisibleParameterAnnotations")
                for {
                    parameterAnnotations ← annotations
                    annotation ← parameterAnnotations
                } {
                    processAnnotation(annotation)
                }
            case MethodParameterTable(parameters) ⇒
                cp.insertUtf8("MethodParameters")
                parameters.foreach {
                    case MethodParameter(name, _) ⇒
                        cp.insertUtf8(name)
                }
            case signature: MethodTypeSignature ⇒
                cp.insertUtf8("Signature")
                cp.insertUtf8(signature.toJVMSignature)

            case UnknownAttribute(name, info) ⇒ cp.insertUtf8(name)

            case generalAttribute             ⇒ collectFromGeneralAttribute(generalAttribute)

        }

        def collectFromCodeAttribute(attribute: Attribute): Unit = attribute.kindId match {
            case LineNumberTable.KindId ⇒ cp.insertUtf8("LineNumberTable")

            case LocalVariableTable.KindId ⇒
                val LocalVariableTable(localVariables) = attribute
                cp.insertUtf8("LocalVariableTable")
                localVariables.foreach { lv ⇒
                    cp.insertUtf8(lv.name)
                    cp.insertUtf8(lv.fieldType.toJVMTypeName)
                }
            case LocalVariableTypeTable.KindId ⇒
                val LocalVariableTypeTable(localVariableTypes) = attribute
                cp.insertUtf8("LocalVariableTypeTable")
                localVariableTypes.foreach { lvt ⇒
                    cp.insertUtf8(lvt.name)
                    cp.insertUtf8("Signature")
                    cp.insertUtf8(lvt.signature.toJVMSignature)
                }
            case StackMapTable.KindId ⇒
                val StackMapTable(stackMapFrames) = attribute
                cp.insertUtf8("StackMapTable")

                def insertVerificationTypeInfo(vti: VerificationTypeInfo): Unit = {
                    if (vti.isObjectVariableInfo) {
                        cp.insertClass(vti.asObjectVariableInfo.clazz)
                    }
                }

                stackMapFrames.foreach {
                    case SameLocals1StackItemFrame(_, verificationType) ⇒
                        insertVerificationTypeInfo(verificationType)
                    case frame: SameLocals1StackItemFrameExtended ⇒
                        insertVerificationTypeInfo(frame.verificationTypeInfoStackItem)
                    case frame: AppendFrame ⇒
                        frame.verificationTypeInfoLocals.foreach { insertVerificationTypeInfo }
                    case FullFrame(_, _, verificationTypeLocals, verificationTypeStack) ⇒
                        verificationTypeLocals.foreach { insertVerificationTypeInfo }
                        verificationTypeStack.foreach { insertVerificationTypeInfo }

                    case _ ⇒ /*nothing to do*/
                }

            case _ ⇒ new BytecodeProcessingFailedException(s"unsupported attribute: $attribute")
        }

        def collectFromFieldAttribute(attribute: Attribute): Unit = attribute.kindId match {
            case ConstantInteger.KindId ⇒
                val ConstantInteger(value) = attribute
                cp.insertUtf8("ConstantValue")
                cp.insertInteger(value)
            case ConstantFloat.KindId ⇒
                val ConstantFloat(value) = attribute
                cp.insertUtf8("ConstantValue")
                cp.insertFloat(value)
            case ConstantLong.KindId ⇒
                val ConstantLong(value) = attribute
                cp.insertUtf8("ConstantValue")
                cp.insertLong(value)
            case ConstantDouble.KindId ⇒
                val ConstantDouble(value) = attribute
                cp.insertUtf8("ConstantValue")
                cp.insertDouble(value)
            case ConstantString.KindId ⇒
                val ConstantString(value) = attribute
                cp.insertUtf8("ConstantValue")
                cp.insertString(value)

            case ArrayTypeSignature.KindId | ClassTypeSignature.KindId | TypeVariableSignature.KindId ⇒
                val FieldTypeJVMSignature(jvmSignature) = attribute
                cp.insertUtf8("Signature")
                cp.insertUtf8(jvmSignature)

            case _ ⇒ collectFromGeneralAttribute(attribute)
        }

        def collectFromBootstrapArgument(argument: BootstrapArgument): Unit =
            argument match {
                case ConstantString(value)        ⇒ cp.insertString(value)
                case ConstantClass(refType)       ⇒ cp.insertClass(refType)
                case ConstantInteger(value)       ⇒ cp.insertInteger(value)
                case ConstantLong(value)          ⇒ cp.insertLong(value)
                case ConstantFloat(value)         ⇒ cp.insertFloat(value)
                case ConstantDouble(value)        ⇒ cp.insertDouble(value)
                case md: MethodDescriptor         ⇒ cp.insertMethodType(md.toJVMDescriptor)
                case gfmh: GetFieldMethodHandle   ⇒ cp.insertMethodHandle(gfmh)
                case gsmh: GetStaticMethodHandle  ⇒ cp.insertMethodHandle(gsmh)
                case pfmh: PutFieldMethodHandle   ⇒ cp.insertMethodHandle(pfmh)
                case psmh: PutStaticMethodHandle  ⇒ cp.insertMethodHandle(psmh)
                case mcmh: MethodCallMethodHandle ⇒ cp.insertMethodHandle(mcmh)
            }

        def collectFromInstruction(instruction: Instruction): Unit =
            instruction.opcode match {

                case GETFIELD.opcode | GETSTATIC.opcode ⇒
                    val FieldReadAccess(objectType, fieldName, fieldType) = instruction
                    cp.insertFieldRef(objectType, fieldName, fieldType.toJVMTypeName)

                case PUTFIELD.opcode | PUTSTATIC.opcode ⇒
                    val FieldWriteAccess(objectType, fieldName, fieldType) = instruction
                    cp.insertFieldRef(objectType, fieldName, fieldType.toJVMTypeName)

                case INVOKEVIRTUAL.opcode ⇒
                    val INVOKEVIRTUAL(refType, methodName, descriptor) = instruction
                    cp.insertMethodRef(
                        refType,
                        methodName,
                        descriptor.toJVMDescriptor
                    )
                case INVOKESTATIC.opcode ⇒
                    val INVOKESTATIC(objectType, methodName, descriptor) = instruction
                    cp.insertMethodRef(objectType, methodName, descriptor.toJVMDescriptor)
                    cp.insertInterfaceMethodRef(
                        objectType,
                        methodName,
                        descriptor.toJVMDescriptor
                    )
                case INVOKEINTERFACE.opcode ⇒
                    val INVOKEINTERFACE(objectType, methodName, descriptor) = instruction
                    cp.insertInterfaceMethodRef(
                        objectType,
                        methodName,
                        descriptor.toJVMDescriptor
                    )
                case INVOKESPECIAL.opcode ⇒
                    val INVOKESPECIAL(objectType, methodName, descriptor) = instruction
                    cp.insertMethodRef(objectType, methodName, descriptor.toJVMDescriptor)
                    cp.insertInterfaceMethodRef(
                        objectType,
                        methodName,
                        descriptor.toJVMDescriptor
                    )

                case INVOKEDYNAMIC.opcode ⇒
                    val INVOKEDYNAMIC(bootstrapMethod, name, descriptor) = instruction
                    cp.insertMethodHandle(bootstrapMethod.handle)
                    bootstrapMethod.arguments.foreach { collectFromBootstrapArgument }
                    cp.insertInvokeDynamic(
                        bootstrapMethod,
                        name,
                        descriptor.toJVMDescriptor,
                        bootstrapMethods
                    )

                case LDC.opcode | LDC_W.opcode ⇒
                    instruction match {
                        case LoadInt(value)               ⇒ cp.insertInteger(value)
                        case LoadFloat(value)             ⇒ cp.insertFloat(value)
                        case LoadClass(referenceType)     ⇒ cp.insertClass(referenceType)
                        case LoadMethodHandle(handle)     ⇒ cp.insertMethodHandle(handle)
                        case LoadMethodType(descriptor)   ⇒ cp.insertMethodType(descriptor.toJVMDescriptor)
                        case LoadString(value)            ⇒ cp.insertString(value)

                        case LoadInt_W(value)             ⇒ cp.insertInteger(value)
                        case LoadFloat_W(value)           ⇒ cp.insertFloat(value)
                        case LoadClass_W(referenceType)   ⇒ cp.insertClass(referenceType)
                        case LoadMethodHandle_W(handle)   ⇒ cp.insertMethodHandle(handle)
                        case LoadMethodType_W(descriptor) ⇒ cp.insertMethodType(descriptor.toJVMDescriptor)
                        case LoadString_W(value)          ⇒ cp.insertString(value)
                    }

                case LDC2_W.opcode ⇒
                    instruction match {
                        case LoadLong(value)   ⇒ cp.insertLong(value)
                        case LoadDouble(value) ⇒ cp.insertDouble(value)
                    }

                case CHECKCAST.opcode ⇒
                    val CHECKCAST(referenceType) = instruction
                    cp.insertClass(referenceType)

                case INSTANCEOF.opcode ⇒
                    val INSTANCEOF(referenceType) = instruction
                    cp.insertClass(referenceType)

                case NEW.opcode ⇒
                    val NEW(objectType) = instruction
                    cp.insertClass(objectType)

                case ANEWARRAY.opcode ⇒
                    val ANEWARRAY(componentType) = instruction
                    cp.insertClass(componentType)

                case MULTIANEWARRAY.opcode ⇒
                    val MULTIANEWARRAY(componentType, _ /*dim*/ ) = instruction
                    cp.insertClass(componentType)

                case _ ⇒ // the other instructions do not use the constant pool
            }

        val methods = classFile.methods

        cp.insertClass(classFile.thisType)
        classFile.superclassType.foreach { cp.insertClass }
        classFile.interfaceTypes.foreach { cp.insertClass }

        classFile.attributes.foreach { collectFromClassFileAttribute }

        classFile.fields.foreach { field ⇒
            cp.insertNameAndType(field.name, field.fieldType.toJVMTypeName)
            field.attributes.foreach { collectFromFieldAttribute }
        }

        methods.foreach { method ⇒
            cp.insertNameAndType(method.name, method.descriptor.toJVMDescriptor)
            method.attributes.foreach { collectFromMethodAttribute }
            method.body.foreach { collectFromMethodAttribute }
            method.annotationDefault.foreach {
                cp.insertUtf8("AnnotationDefault")
                insertElementValue
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