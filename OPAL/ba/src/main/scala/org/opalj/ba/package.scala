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

import scala.language.implicitConversions

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

import org.opalj.log.GlobalLogContext
import org.opalj.log.OPALLogger
import org.opalj.bi.ACC_PUBLIC
import org.opalj.bi.ACC_FINAL
import org.opalj.bi.ACC_SUPER
import org.opalj.bi.ACC_INTERFACE
import org.opalj.bi.ACC_ABSTRACT
import org.opalj.bi.ACC_ENUM
import org.opalj.bi.ACC_ANNOTATION
import org.opalj.bi.ACC_STATIC
import org.opalj.bi.ACC_PROTECTED
import org.opalj.bi.ACC_SYNCHRONIZED
import org.opalj.bi.ACC_BRIDGE
import org.opalj.bi.ACC_SYNTHETIC
import org.opalj.bi.ACC_PRIVATE
import org.opalj.bi.ACC_VARARGS
import org.opalj.bi.ACC_TRANSIENT
import org.opalj.bi.ACC_VOLATILE
import org.opalj.bi.ACC_NATIVE
import org.opalj.bi.ACC_STRICT
import org.opalj.br.Attribute
import org.opalj.br.Code
import org.opalj.br.ExceptionHandler
import org.opalj.br.MethodDescriptor
import org.opalj.br.MethodHandle
import org.opalj.br.ReferenceType
import org.opalj.br.SourceFile
import org.opalj.br.cp._ // we need ALL of them...
import org.opalj.br.instructions._ // we need NEARY ALL of them...

/**
 * Implementation of an EDSL for creating Java bytecode. The EDSL is designed to facilitate
 * the creation of correct class files; i.e., whenever possible it tries to fill wholes. For
 * example, when an interface is specified the library automatically ensures that the super
 * class type is (initially) set to `java.lang.Object` as required by the JVM specification.
 *
 * This package in particular provides functionality to convert [[org.opalj.br]] classes to
 * [[org.opalj.da]] classes.
 *
 * @author Michael Eichberg
 * @author Malte Limmeroth
 */
package object ba { ba ⇒

    {
        // Log the information whether a production build or a development build is used.
        implicit val logContext = GlobalLogContext
        import OPALLogger.info
        try {
            scala.Predef.assert(false)
            info("OPAL", "Bytecode Assembler - Production Build")
        } catch {
            case ae: AssertionError ⇒
                info("OPAL", "Bytecode Assembler - Development Build (Assertions are enabled)")
        }
    }

    final val PUBLIC = new AccessModifier(ACC_PUBLIC.mask)
    final val FINAL = new AccessModifier(ACC_FINAL.mask)
    final val SUPER = new AccessModifier(ACC_SUPER.mask)
    final val INTERFACE = new AccessModifier(ACC_INTERFACE.mask)
    final val ABSTRACT = new AccessModifier(ACC_ABSTRACT.mask)
    final val SYNTHETIC = new AccessModifier(ACC_SYNTHETIC.mask)
    final val ANNOTATION = new AccessModifier(ACC_ANNOTATION.mask)
    final val ENUM = new AccessModifier(ACC_ENUM.mask)
    final val PRIVATE = new AccessModifier(ACC_PRIVATE.mask)
    final val PROTECTED = new AccessModifier(ACC_PROTECTED.mask)
    final val STATIC = new AccessModifier(ACC_STATIC.mask)
    final val SYNCHRONIZED = new AccessModifier(ACC_SYNCHRONIZED.mask)
    final val BRIDGE = new AccessModifier(ACC_BRIDGE.mask)
    final val VARARGS = new AccessModifier(ACC_VARARGS.mask)
    final val NATIVE = new AccessModifier(ACC_NATIVE.mask)
    final val STRICT = new AccessModifier(ACC_STRICT.mask)
    final val VOLATILE = new AccessModifier(ACC_VOLATILE.mask)
    final val TRANSIENT = new AccessModifier(ACC_TRANSIENT.mask)

    // *********************************************************************************************
    //
    //          F U N C T I O N A L I T Y   T O  C R E A T E    "br." C L A S S F I L E S
    //
    // *********************************************************************************************

    /**
     * Converts a [[org.opalj.br.ClassFile]] to a [[org.opalj.da.ClassFile]] and all its attributes
     * to the attributes in [[org.opalj.da]].
     */
    def toDA(classFile: br.ClassFile): da.ClassFile = {
        implicit val constantPoolBuffer = new ConstantPoolBuffer()
        val thisTypeCPRef = constantPoolBuffer.CPEClass(classFile.thisType)
        val superClassCPRef = classFile.superclassType match {
            case Some(superclassType) ⇒ constantPoolBuffer.CPEClass(superclassType)
            case None                 ⇒ 0
        }

        val interfaces = classFile.interfaceTypes.map(constantPoolBuffer.CPEClass)
        val fields = classFile.fields.map(toDA)
        val methods = classFile.methods.map(toDA)
        val attributes = classFile.attributes.map(toDA)
        val constant_pool = constantPoolBuffer.toDA

        da.ClassFile(
            constant_pool = constant_pool,
            minor_version = classFile.version.minor,
            major_version = classFile.version.major,
            access_flags = classFile.accessFlags,
            this_class = thisTypeCPRef,
            super_class = superClassCPRef,
            interfaces = interfaces.toIndexedSeq,
            fields = fields,
            methods = methods,
            attributes = attributes
        )
    }

    implicit class BRClassFile(classFile: br.ClassFile) {
        def toDA: da.ClassFile = ba.toDA(classFile)
    }

    def toDA(field: br.Field)(implicit constantPoolBuffer: ConstantPoolBuffer): da.Field_Info = {
        da.Field_Info(
            access_flags = field.accessFlags,
            name_index = constantPoolBuffer.CPEUtf8(field.name),
            descriptor_index = constantPoolBuffer.CPEUtf8(field.fieldType.toJVMTypeName),
            attributes = field.attributes.map(toDA)
        )
    }

    def toDA(method: br.Method)(implicit constantPoolBuffer: ConstantPoolBuffer): da.Method_Info = {
        var attributes = method.attributes.map(toDA)
        if (method.body.isDefined) {
            attributes = method.body.get.toDA +: attributes
        }
        da.Method_Info(
            access_flags = method.accessFlags,
            name_index = constantPoolBuffer.CPEUtf8(method.name),
            descriptor_index = constantPoolBuffer.CPEUtf8(method.descriptor.toJVMDescriptor),
            attributes = attributes
        )
    }

    def toDA(code: Code)(implicit constantPoolBuffer: ConstantPoolBuffer): da.Code_attribute = {
        val data = new ByteArrayOutputStream(code.instructions.size)
        val instructions = new DataOutputStream(data)

        var modifiedByWide = false
        code foreach { e ⇒
            val (pc, i) = e
            instructions.writeByte(i.opcode)
            i match {
                // TODO use opcode to enable efficient switching (by means of a tableswitch)
                case BIPUSH(value) ⇒ instructions.writeShort(value)
                case CHECKCAST(referenceType) ⇒
                    val cpeRef = constantPoolBuffer.CPEClass(referenceType)
                    instructions.writeShort(cpeRef)

                case IINC(lvIndex, constValue) ⇒
                    if (modifiedByWide) {
                        modifiedByWide = false
                        instructions.writeShort(lvIndex)
                        instructions.writeShort(constValue)
                    } else {
                        instructions.writeByte(lvIndex)
                        instructions.writeByte(constValue)
                    }

                case INSTANCEOF(referenceType) ⇒
                    instructions.writeShort(constantPoolBuffer.CPEClass(referenceType))

                case JSR_W(branchoffset) ⇒ instructions.writeInt(branchoffset)
                case _: LDC[_] | _: LDC_W[_] ⇒ {
                    val value = i.asInstanceOf[LoadConstantInstruction[_]].value
                    val cpeRef = value match {
                        case int: Int ⇒ constantPoolBuffer.CPEInteger(int)

                        case float: Float ⇒
                            constantPoolBuffer.CPEFloat(float)
                        case string: String ⇒
                            constantPoolBuffer.CPEString(string)
                        case mh: MethodHandle ⇒
                            constantPoolBuffer.CPEMethodHandle(mh)
                        case md: MethodDescriptor ⇒
                            constantPoolBuffer.CPEMethodType(md.toJVMDescriptor)
                        case reference: ReferenceType ⇒
                            constantPoolBuffer.CPEClass(reference)
                    }

                    if (i.isInstanceOf[LDC_W[_]]) {
                        instructions.writeInt(cpeRef)
                    } else {
                        instructions.writeShort(cpeRef)
                    }
                }
                case l: LDC2_W[_] ⇒ {
                    l.value match {
                        case l: Long   ⇒ instructions.writeShort(constantPoolBuffer.CPELong(l))
                        case d: Double ⇒ instructions.writeShort(constantPoolBuffer.CPEDouble(d))
                    }
                }
                //TODO: LOOKUPSWITCH, MULTIANEWARRAY
                case NEW(objectType) ⇒
                    instructions.writeShort(constantPoolBuffer.CPEClass(objectType))

                //TODO: NEWARRAY
                case RET(lvIndex)  ⇒ instructions.writeByte(lvIndex)
                case SIPUSH(value) ⇒ instructions.writeShort(value)
                //TODO: TABLESWITCH

                //TODO: INVOKEDYNAMIC
                case methodInvocation: MethodInvocationInstruction ⇒
                    val cpeRef = constantPoolBuffer.CPEMethodRef(
                        methodInvocation.declaringClass,
                        methodInvocation.name,
                        methodInvocation.methodDescriptor.toJVMDescriptor
                    )
                    instructions.writeShort(cpeRef)
                case s: SimpleConditionalBranchInstruction ⇒ instructions.writeShort(s.branchoffset)
                case u: UnconditionalBranchInstruction     ⇒ instructions.writeShort(u.branchoffset)

                case f: FieldAccess ⇒ {
                    val fieldType = f.fieldType.toJVMTypeName
                    val cpeRef = constantPoolBuffer.CPEFieldRef(f.declaringClass, f.name, fieldType)
                    instructions.writeShort(cpeRef)
                }

                case e: ExplicitLocalVariableIndex ⇒ {
                    if (modifiedByWide) {
                        modifiedByWide = false
                        instructions.writeShort(e.lvIndex)
                    } else {
                        instructions.writeByte(e.lvIndex)
                    }
                }

                case WIDE /*.opcode*/ ⇒
                    if (modifiedByWide)
                        throw new IllegalArgumentException(s"the wide at $pc follows a wide")
                    // modifiedByWide will be set to false by the subsequent instruction
                    modifiedByWide = true

                case ALOAD_0 /*.opcode | ALOAD_1.opcode | ALOAD_2.opcode | ALOAD_3.opcode*/ |
                    RETURN ⇒
                // Nothing to do; these instructions have no parameters and the opcode
                // is already written.
            }

        }

        instructions.flush

        da.Code_attribute(
            attribute_name_index = constantPoolBuffer.CPEUtf8(bi.CodeAttribute.Name),
            max_stack = code.maxStack,
            max_locals = code.maxLocals,
            code = da.Code(data.toByteArray),
            exceptionTable = code.exceptionHandlers.map(toDA),
            attributes = code.attributes.map(toDA)
        )
    }

    implicit class BRCode(
            code: br.Code
    )(
            implicit
            constantPoolBuffer: ConstantPoolBuffer
    ) {
        def toDA: da.Code_attribute = ba.toDA(code)
    }

    implicit def toDA(
        exceptionHandler: ExceptionHandler
    )(
        implicit
        constantPoolBuffer: ConstantPoolBuffer
    ): da.ExceptionTableEntry = {
        val index = if (exceptionHandler.catchType.isDefined) {
            constantPoolBuffer.CPEClass(exceptionHandler.catchType.get)
        } else 0
        da.ExceptionTableEntry(
            exceptionHandler.startPC,
            exceptionHandler.endPC,
            exceptionHandler.handlerPC,
            index
        )
    }

    implicit def toDA(
        attribute: Attribute
    )(
        implicit
        constantPoolBuffer: ConstantPoolBuffer
    ): da.Attribute = {
        attribute match {
            case c: Code ⇒ c.toDA

            // direct conversions
            case SourceFile(s) ⇒
                da.SourceFile_attribute(
                    constantPoolBuffer.CPEUtf8(bi.SourceFileAttribute.Name),
                    constantPoolBuffer.CPEUtf8(s)
                )

            // TODO: Support the other attributes...
        }
    }

    implicit def toDA(
        implicit
        constantPoolBuffer: ConstantPoolBuffer
    ): Array[da.Constant_Pool_Entry] = {
        constantPoolBuffer.toArray.map {
            // TODO use tags to enable efficient switching (by means of a tableswitch)
            case CONSTANT_NameAndType_info(name_index, descriptor_index) ⇒
                da.CONSTANT_NameAndType_info(name_index, descriptor_index)

            case CONSTANT_InterfaceMethodref_info(class_index, name_and_type_index) ⇒
                da.CONSTANT_InterfaceMethodref_info(class_index, name_and_type_index)

            case CONSTANT_Fieldref_info(class_index, name_and_type_index) ⇒
                da.CONSTANT_Fieldref_info(class_index, name_and_type_index)

            case CONSTANT_MethodHandle_info(referenceKind, referenceIndex) ⇒
                da.CONSTANT_MethodHandle_info(referenceKind, referenceIndex)

            case CONSTANT_Methodref_info(class_index, name_and_type_index) ⇒
                da.CONSTANT_Methodref_info(class_index, name_and_type_index)

            case CONSTANT_InvokeDynamic_info(bootstrapMethodIndex, nameAndTypeIndex) ⇒
                da.CONSTANT_InvokeDynamic_info(bootstrapMethodIndex, nameAndTypeIndex)

            case CONSTANT_Class_info(name_index) ⇒ da.CONSTANT_Class_info(name_index)
            case CONSTANT_MethodType_info(index) ⇒ da.CONSTANT_MethodType_info(index)

            case CONSTANT_Double_info(d)         ⇒ da.CONSTANT_Double_info(d.value)
            case CONSTANT_Float_info(f)          ⇒ da.CONSTANT_Float_info(f.value)
            case CONSTANT_Long_info(l)           ⇒ da.CONSTANT_Long_info(l.value)
            case CONSTANT_Integer_info(i)        ⇒ da.CONSTANT_Integer_info(i.value)
            case CONSTANT_String_info(s)         ⇒ da.CONSTANT_String_info(s)
            case CONSTANT_Utf8_info(u)           ⇒ da.CONSTANT_Utf8(u)

            case null                            ⇒ null

            case cpe                             ⇒ throw new IllegalArgumentException(cpe.toString)
        }
    }

    implicit class BRConstantPoolBuffer(constantPoolBuffer: ConstantPoolBuffer) {
        def toDA: Array[da.Constant_Pool_Entry] = ba.toDA(constantPoolBuffer)
    }

}
