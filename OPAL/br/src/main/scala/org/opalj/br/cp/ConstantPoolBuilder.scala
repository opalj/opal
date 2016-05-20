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
import scala.collection.mutable.ArrayBuffer
import org.opalj.bytecode.BytecodeProcessingFailedException

/**
 * Reconstructs the constant_pool of the given classfile.
 *
 * @author Andre Pacak
 */
class ConstantPoolBuilder {

    def build(classFile: ClassFile): Array[Constant_Pool_Entry] = {
        val cp = new ConstantPoolBuffer()
        val bootstrapMethods = new BootstrapMethodsBuffer()

        def insertElementValue(
            elementValue: ElementValue
        ): Unit = elementValue match {
            case ByteValue(value) ⇒
                cp.insertInteger(value.toInt)
            case CharValue(value) ⇒
                cp.insertInteger(value.toInt)
            case DoubleValue(value) ⇒
                cp.insertDouble(value)
            case FloatValue(value) ⇒
                cp.insertFloat(value)
            case IntValue(value) ⇒
                cp.insertInteger(value)
            case LongValue(value) ⇒
                cp.insertLong(value)
            case ShortValue(value) ⇒
                cp.insertInteger(value.toInt)
            case BooleanValue(value) ⇒
                cp.insertInteger(if (value) 1 else 0)
            case StringValue(value) ⇒
                cp.insertUtf8(value)
            case EnumValue(enumType, constName) ⇒
                cp.insertUtf8(enumType.toJVMTypeName)
                cp.insertUtf8(constName)
            case ClassValue(value) ⇒
                cp.insertUtf8(value.toJVMTypeName)
            case AnnotationValue(annotation) ⇒
                collectFromAnnotation(annotation)
            case ArrayValue(values) ⇒
                values.foreach { insertElementValue }
        }
        def collectFromAnnotation(annotation: Annotation): Unit = {
            cp.insertUtf8(annotation.annotationType.toJVMTypeName)
            annotation.elementValuePairs.foreach {
                case ElementValuePair(name, value) ⇒
                    cp.insertUtf8(name)
                    insertElementValue(value)
            }
        }
        def collectFromClassFileAttribute(attribute: Attribute): Unit = attribute match {
            case EnclosingMethod(clazz, name, descriptor) ⇒
                cp.insertUtf8("EnclosingMethod")
                cp.insertClass(clazz)
                for {
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
                    cp.insertMethodHandle(method.methodHandle)
                    method.bootstrapArguments.foreach {
                        collectFromBootstrapArgument
                    }
                }
            case sig @ ClassSignature(_, _, _) ⇒
                cp.insertUtf8("Signature")
                cp.insertUtf8(sig.toJVMSignature)
            case default ⇒ collectFromGeneralAttribute(default)

        }
        def collectFromGeneralAttribute(attribute: Attribute): Unit = attribute match {
            case Synthetic ⇒
                cp.insertUtf8("Synthetic")
            case Deprecated ⇒
                cp.insertUtf8("Deprecated")
            case RuntimeVisibleAnnotationTable(annotations) ⇒
                cp.insertUtf8("RuntimeVisibleAnnotations")
                annotations.foreach { collectFromAnnotation }
            case RuntimeInvisibleAnnotationTable(annotations) ⇒
                cp.insertUtf8("RuntimeInvisibleAnnotations")
                annotations.foreach { collectFromAnnotation }
            case RuntimeVisibleTypeAnnotationTable(annotations) ⇒
                cp.insertUtf8("RuntimeVisibleTypeAnnotations")
            case RuntimeInvisibleTypeAnnotationTable(annotations) ⇒
                cp.insertUtf8("RuntimeInvisibleTypeAnnotations")
            case UnknownAttribute(name, info) ⇒
                cp.insertUtf8(name)
            case _ ⇒ new BytecodeProcessingFailedException("Attribute is not known")
        }

        def collectFromMethodAttribute(attribute: Attribute): Unit = attribute match {
            case Code(_,
                _,
                instructions,
                exceptionHandlers,
                attributes) ⇒
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
                exceptions.foreach { cp.insertClass }
            case RuntimeVisibleParameterAnnotationTable(annotations) ⇒
                cp.insertUtf8("RuntimeVisibleParameterAnnotations")
                for {
                    paramAnnotation ← annotations
                    singleAnnotation ← paramAnnotation
                } {
                    collectFromAnnotation(singleAnnotation)
                }
            case RuntimeInvisibleParameterAnnotationTable(annotations) ⇒
                cp.insertUtf8("RuntimeInvisibleParameterAnnotations")
                for {
                    paramAnnotation ← annotations
                    singleAnnotation ← paramAnnotation
                } {
                    collectFromAnnotation(singleAnnotation)
                }
            case MethodParameterTable(parameters) ⇒
                cp.insertUtf8("MethodParameters")
                parameters.foreach {
                    case MethodParameter(name, _) ⇒
                        cp.insertUtf8(name)
                }
            case sig @ MethodTypeSignature(_, _, _, _) ⇒
                cp.insertUtf8("Signature")
                cp.insertUtf8(sig.toJVMSignature)
            case UnknownAttribute(name, info) ⇒
                cp.insertUtf8(name)
            case default ⇒ collectFromGeneralAttribute(default)

        }
        def collectFromCodeAttribute(attribute: Attribute): Unit = attribute match {
            case UnpackedLineNumberTable(_) ⇒
                cp.insertUtf8("LineNumberTable")
            case CompactLineNumberTable(_) ⇒
                cp.insertUtf8("LineNumberTable")
            case LocalVariableTable(localVariables) ⇒
                cp.insertUtf8("LocalVariableTable")
                localVariables.foreach {
                    case LocalVariable(_, _, name, fieldType, index) ⇒
                        cp.insertUtf8(name)
                        cp.insertUtf8(fieldType.toJVMTypeName)
                }
            case LocalVariableTypeTable(localVariableTypes) ⇒
                cp.insertUtf8("LocalVariableTypeTable")
                localVariableTypes.foreach {
                    case LocalVariableType(_, _, name, signature, _) ⇒
                        cp.insertUtf8(name)
                        cp.insertUtf8("Signature")
                        cp.insertUtf8(signature.toJVMSignature)
                }
            case StackMapTable(stackMapFrames) ⇒
                cp.insertUtf8("StackMapTable")
                def insertVerificationTypeInfo(
                    typ: VerificationTypeInfo
                ): Unit = typ match {
                    case ObjectVariableInfo(clazz) ⇒
                        cp.insertClass(clazz)
                    case _ ⇒ ()
                }
                stackMapFrames.foreach {
                    case SameLocals1StackItemFrame(_, verificationType) ⇒
                        insertVerificationTypeInfo(verificationType)
                    case SameLocals1StackItemFrameExtended(_, _, verificationType) ⇒
                        insertVerificationTypeInfo(verificationType)
                    case AppendFrame(_, _, verificationTypeLocals) ⇒
                        verificationTypeLocals.foreach { insertVerificationTypeInfo }
                    case FullFrame(_, _, verificationTypeLocals, verificationTypeStack) ⇒
                        verificationTypeLocals.foreach { insertVerificationTypeInfo }
                        verificationTypeStack.foreach { insertVerificationTypeInfo }
                    case _ ⇒ ()
                }
            case _ ⇒ new BytecodeProcessingFailedException("Attribute is not known")
        }

        def collectFromFieldAttribute(attribute: Attribute): Unit = attribute match {
            case ConstantInteger(value) ⇒
                cp.insertUtf8("ConstantValue")
                cp.insertInteger(value)
            case ConstantFloat(value) ⇒
                cp.insertUtf8("ConstantValue")
                cp.insertFloat(value)
            case ConstantLong(value) ⇒
                cp.insertUtf8("ConstantValue")
                cp.insertLong(value)
            case ConstantDouble(value) ⇒
                cp.insertUtf8("ConstantValue")
                cp.insertDouble(value)
            case ConstantString(value) ⇒
                cp.insertUtf8("ConstantValue")
                cp.insertString(value)
            case sig @ FieldTypeSignature() ⇒
                cp.insertUtf8("Signature")
                cp.insertUtf8(sig.toJVMSignature)
            case default ⇒ collectFromGeneralAttribute(default)
        }

        def collectFromBootstrapArgument(argument: BootstrapArgument): Unit =
            argument match {
                case ConstantString(value) ⇒
                    cp.insertString(value)
                case ConstantClass(refType) ⇒
                    cp.insertClass(refType)
                case ConstantInteger(value) ⇒
                    cp.insertInteger(value)
                case ConstantLong(value) ⇒
                    cp.insertLong(value)
                case ConstantFloat(value) ⇒
                    cp.insertFloat(value)
                case ConstantDouble(value) ⇒
                    cp.insertDouble(value)
                case md @ MethodDescriptor(_, _) ⇒
                    cp.insertMethodType(md.toJVMDescriptor)
                case mh @ GetFieldMethodHandle(_, _, _) ⇒
                    cp.insertMethodHandle(mh)
                case mh @ GetStaticMethodHandle(_, _, _) ⇒
                    cp.insertMethodHandle(mh)
                case mh @ PutFieldMethodHandle(_, _, _) ⇒
                    cp.insertMethodHandle(mh)
                case mh @ PutStaticMethodHandle(_, _, _) ⇒
                    cp.insertMethodHandle(mh)
                case mh @ MethodCallMethodHandle(_, _, _) ⇒
                    cp.insertMethodHandle(mh)
            }
        def collectFromInstruction(
            instruction: Instruction
        ): Unit = instruction match {
            case FieldReadAccess(objectType, fieldName, fieldType) ⇒
                cp.insertFieldRef(objectType, fieldName, fieldType.toJVMTypeName)
            case FieldWriteAccess(objectType, fieldName, fieldType) ⇒
                cp.insertFieldRef(objectType, fieldName, fieldType.toJVMTypeName)
            case INVOKEVIRTUAL(refType, methodName, descriptor) ⇒
                cp.insertMethodRef(
                    refType,
                    methodName,
                    descriptor.toJVMDescriptor
                )
            case INVOKESTATIC(objectType, methodName, descriptor) ⇒
                cp.insertMethodRef(objectType, methodName, descriptor.toJVMDescriptor)
                cp.insertInterfaceMethodRef(
                    objectType,
                    methodName,
                    descriptor.toJVMDescriptor
                )
            case INVOKEINTERFACE(objectType, methodName, descriptor) ⇒
                cp.insertInterfaceMethodRef(
                    objectType,
                    methodName,
                    descriptor.toJVMDescriptor
                )
            case INVOKEDYNAMIC(bootstrapMethod, name, descriptor) ⇒
                cp.insertMethodHandle(bootstrapMethod.methodHandle)
                bootstrapMethod.bootstrapArguments.foreach {
                    collectFromBootstrapArgument
                }
                cp.insertInvokeDynamic(
                    bootstrapMethod,
                    name,
                    descriptor.toJVMDescriptor,
                    bootstrapMethods
                )
            case INVOKESPECIAL(objectType, methodName, descriptor) ⇒
                cp.insertMethodRef(objectType, methodName, descriptor.toJVMDescriptor)
                cp.insertInterfaceMethodRef(
                    objectType,
                    methodName,
                    descriptor.toJVMDescriptor
                )
            case LoadInt(value) ⇒
                cp.insertInteger(value)
            case LoadFloat(value) ⇒
                cp.insertFloat(value)
            case LoadClass(referenceType) ⇒
                cp.insertClass(referenceType)
            case LoadMethodHandle(handle) ⇒
                cp.insertMethodHandle(handle)
            case LoadMethodType(descriptor) ⇒
                cp.insertMethodType(descriptor.toJVMDescriptor)
            case LoadString(s) ⇒
                cp.insertString(s)
            case LoadLong(value) ⇒
                cp.insertLong(value)
            case LoadDouble(value) ⇒
                cp.insertDouble(value)
            case LoadInt_W(value) ⇒
                cp.insertInteger(value)
            case LoadFloat_W(value) ⇒
                cp.insertFloat(value)
            case LoadClass_W(referenceType) ⇒
                cp.insertClass(referenceType)
            case LoadMethodHandle_W(handle) ⇒
                cp.insertMethodHandle(handle)
            case LoadMethodType_W(descriptor) ⇒
                cp.insertMethodType(descriptor.toJVMDescriptor)
            case LoadString_W(s) ⇒
                cp.insertString(s)
            case CHECKCAST(referenceType) ⇒
                cp.insertClass(referenceType)
            case INSTANCEOF(referenceType) ⇒
                cp.insertClass(referenceType)
            case NEW(objectType) ⇒
                cp.insertClass(objectType)
            case ANEWARRAY(componentType) ⇒
                cp.insertClass(componentType)
            case MULTIANEWARRAY(componentType, dim) ⇒
                cp.insertClass(componentType)
            case _ ⇒ () //other instructions that don't reference the constant pool
        }

        val fields = classFile.fields
        val methods = classFile.methods
        val attributes = classFile.attributes

        cp.insertClass(classFile.thisType)
        classFile.superclassType.foreach { cp.insertClass }
        classFile.interfaceTypes.foreach { typ ⇒
            cp.insertClass(typ)
        }

        fields.foreach { field ⇒
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
        }

        attributes.foreach { collectFromClassFileAttribute }

        for {
            method ← methods
            body ← method.body
            instr ← body.instructions

        } {
            collectFromInstruction(instr)
        }

        cp.toArray
    }
}

object ConstantPoolBuilder {
    def apply(classFile: ClassFile): Array[Constant_Pool_Entry] = {
        val builder = new ConstantPoolBuilder()
        builder.build(classFile)
    }
}