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

import scala.annotation.switch
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
import org.opalj.bi.ConstantPoolTags.CONSTANT_Class_ID
import org.opalj.bi.ConstantPoolTags.CONSTANT_Fieldref_ID
import org.opalj.bi.ConstantPoolTags.CONSTANT_Methodref_ID
import org.opalj.bi.ConstantPoolTags.CONSTANT_InterfaceMethodref_ID
import org.opalj.bi.ConstantPoolTags.CONSTANT_String_ID
import org.opalj.bi.ConstantPoolTags.CONSTANT_Integer_ID
import org.opalj.bi.ConstantPoolTags.CONSTANT_Float_ID
import org.opalj.bi.ConstantPoolTags.CONSTANT_Long_ID
import org.opalj.bi.ConstantPoolTags.CONSTANT_Double_ID
import org.opalj.bi.ConstantPoolTags.CONSTANT_NameAndType_ID
import org.opalj.bi.ConstantPoolTags.CONSTANT_Utf8_ID
import org.opalj.bi.ConstantPoolTags.CONSTANT_MethodHandle_ID
import org.opalj.bi.ConstantPoolTags.CONSTANT_MethodType_ID
import org.opalj.bi.ConstantPoolTags.CONSTANT_InvokeDynamic_ID
import org.opalj.br.Attribute
import org.opalj.br.Code
import org.opalj.br.ExceptionHandler
import org.opalj.br.cp._ // we need ALL of them...
import org.opalj.br.instructions._ // we need ALL of them...

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

    def createBoostrapMethodTableAttribute(constantsPool: ConstantsPool): da.Attribute = {
        import constantsPool._
        val bootstrap_methods = bootstrapMethods map { bootstrapMethod ⇒
            new da.BootstrapMethod(
                CPEMethodHandle(bootstrapMethod.handle, false),
                bootstrapMethod.arguments map { arguement ⇒
                    new da.BootstrapArgument(
                        CPEntryForBootstrapArgument(arguement)
                    )
                }
            )
        }
        val attributeNameIndex = constantsPool.CPEUtf8(bi.BootstrapMethodsAttribute.Name)
        new da.BootstrapMethods_attribute(attributeNameIndex, bootstrap_methods)
    }

    /**
     * Converts a [[org.opalj.br.ClassFile]] to a [[org.opalj.da.ClassFile]] and all its attributes
     * to the attributes in [[org.opalj.da]].
     */
    def toDA(classFile: br.ClassFile): da.ClassFile = {
        implicit val constantsBuffer = ConstantsBuffer(ConstantsBuffer.collectLDCs(classFile))
        val thisTypeCPRef = constantsBuffer.CPEClass(classFile.thisType, false)
        val superClassCPRef = classFile.superclassType match {
            case Some(superclassType) ⇒ constantsBuffer.CPEClass(superclassType, false)
            case None                 ⇒ 0
        }

        val interfaces = classFile.interfaceTypes.map(i ⇒ constantsBuffer.CPEClass(i, false))
        val fields = classFile.fields.map(toDA)
        val methods = classFile.methods.map(toDA)
        var attributes = classFile.attributes.map(toDA)
        val (constantPoolEntries, constantsPool) = constantsBuffer.build
        if (constantsPool.bootstrapMethods.nonEmpty) {
            attributes :+= createBoostrapMethodTableAttribute(constantsPool)
        }
        val constant_pool = constantPoolEntries.toDA
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

    def toDA(field: br.Field)(implicit constantsBuffer: ConstantsBuffer): da.Field_Info = {
        da.Field_Info(
            access_flags = field.accessFlags,
            name_index = constantsBuffer.CPEUtf8(field.name),
            descriptor_index = constantsBuffer.CPEUtf8(field.fieldType.toJVMTypeName),
            attributes = field.attributes.map(toDA)
        )
    }

    def toDA(method: br.Method)(implicit constantsBuffer: ConstantsBuffer): da.Method_Info = {
        var attributes = method.attributes.map(toDA)
        if (method.body.isDefined) {
            attributes = toDA(method.body.get) +: attributes
        }
        da.Method_Info(
            access_flags = method.accessFlags,
            name_index = constantsBuffer.CPEUtf8(method.name),
            descriptor_index = constantsBuffer.CPEUtf8(method.descriptor.toJVMDescriptor),
            attributes = attributes
        )
    }

    def toDA(code: Code)(implicit constantsBuffer: ConstantsBuffer): da.Code_attribute = {
        import constantsBuffer._
        val data = new ByteArrayOutputStream(code.instructions.size)
        val instructions = new DataOutputStream(data)

        var modifiedByWide = false
        code foreach { e ⇒
            val (pc, i) = e
            val opcode = i.opcode
            instructions.writeByte(opcode)

            i.opcode match {
                // the

                case ALOAD_0.opcode | ALOAD_1.opcode | ALOAD_2.opcode | ALOAD_3.opcode |
                    ASTORE_0.opcode | ASTORE_1.opcode | ASTORE_2.opcode | ASTORE_3.opcode |
                    ILOAD_0.opcode | ILOAD_1.opcode | ILOAD_2.opcode | ILOAD_3.opcode |
                    ISTORE_0.opcode | ISTORE_1.opcode | ISTORE_2.opcode | ISTORE_3.opcode |
                    DLOAD_0.opcode | DLOAD_1.opcode | DLOAD_2.opcode | DLOAD_3.opcode |
                    DSTORE_0.opcode | DSTORE_1.opcode | DSTORE_2.opcode | DSTORE_3.opcode |
                    FLOAD_0.opcode | FLOAD_1.opcode | FLOAD_2.opcode | FLOAD_3.opcode |
                    FSTORE_0.opcode | FSTORE_1.opcode | FSTORE_2.opcode | FSTORE_3.opcode |
                    LLOAD_0.opcode | LLOAD_1.opcode | LLOAD_2.opcode | LLOAD_3.opcode |
                    LSTORE_0.opcode | LSTORE_1.opcode | LSTORE_2.opcode | LSTORE_3.opcode |

                    IRETURN.opcode | LRETURN.opcode | FRETURN.opcode | DRETURN.opcode |
                    ARETURN.opcode |
                    RETURN.opcode |

                    ARRAYLENGTH.opcode |
                    AASTORE.opcode |
                    IASTORE.opcode | SASTORE.opcode | BASTORE.opcode | CASTORE.opcode |
                    FASTORE.opcode |
                    LASTORE.opcode |
                    DASTORE.opcode |
                    AALOAD.opcode |
                    DALOAD.opcode | FALOAD.opcode |
                    IALOAD.opcode | LALOAD.opcode | SALOAD.opcode | BALOAD.opcode | CALOAD.opcode |

                    DADD.opcode | FADD.opcode | IADD.opcode | LADD.opcode |
                    DDIV.opcode | FDIV.opcode | IDIV.opcode | LDIV.opcode |
                    DNEG.opcode | FNEG.opcode | INEG.opcode | LNEG.opcode |
                    DMUL.opcode | FMUL.opcode | IMUL.opcode | LMUL.opcode |
                    DREM.opcode | FREM.opcode | IREM.opcode | LREM.opcode |
                    DSUB.opcode | FSUB.opcode | ISUB.opcode | LSUB.opcode |

                    IAND.opcode | LAND.opcode |
                    IOR.opcode | LOR.opcode |
                    IXOR.opcode | LXOR.opcode |
                    ISHL.opcode | LSHL.opcode |
                    ISHR.opcode | LSHR.opcode |
                    IUSHR.opcode | LUSHR.opcode |

                    ICONST_0.opcode | ICONST_1.opcode |
                    ICONST_2.opcode | ICONST_3.opcode |
                    ICONST_4.opcode | ICONST_5.opcode |
                    ICONST_M1.opcode |
                    ACONST_NULL.opcode |
                    DCONST_0.opcode | DCONST_1.opcode |
                    FCONST_0.opcode | FCONST_1.opcode | FCONST_2.opcode |
                    LCONST_0.opcode | LCONST_1.opcode |
                    DCMPG.opcode | FCMPG.opcode | DCMPL.opcode | FCMPL.opcode |
                    LCMP.opcode |

                    NOP.opcode |
                    POP.opcode | POP2.opcode |
                    SWAP.opcode |
                    DUP.opcode | DUP_X1.opcode | DUP_X2.opcode |
                    DUP2.opcode | DUP2_X1.opcode | DUP2_X2.opcode |
                    D2F.opcode | I2F.opcode | L2F.opcode |
                    D2I.opcode | F2I.opcode | L2I.opcode |
                    D2L.opcode | I2L.opcode | F2L.opcode |
                    F2D.opcode | I2D.opcode | L2D.opcode |
                    I2C.opcode | I2B.opcode | I2S.opcode |
                    MONITORENTER.opcode | MONITOREXIT.opcode |
                    ATHROW.opcode ⇒
                // Nothing to do; the opcode is already written!

                case ALOAD.opcode | ASTORE.opcode |
                    ILOAD.opcode | ISTORE.opcode |
                    DLOAD.opcode | DSTORE.opcode |
                    FLOAD.opcode | FSTORE.opcode |
                    LLOAD.opcode | LSTORE.opcode ⇒
                    val ExplicitLocalVariableIndex(index) = i
                    if (modifiedByWide) {
                        modifiedByWide = false
                        instructions.writeShort(index)
                    } else {
                        instructions.writeByte(index)
                    }

                case BIPUSH.opcode ⇒
                    val BIPUSH(value) = i
                    instructions.writeByte(value)

                case SIPUSH.opcode ⇒
                    val SIPUSH(value) = i
                    instructions.writeShort(value)

                case NEW.opcode ⇒
                    val NEW(objectType) = i
                    instructions.writeShort(CPEClass(objectType, false))

                case CHECKCAST.opcode ⇒
                    val CHECKCAST(referenceType) = i
                    val cpeRef = CPEClass(referenceType, false)
                    instructions.writeShort(cpeRef)

                case INSTANCEOF.opcode ⇒
                    val INSTANCEOF(referenceType) = i
                    instructions.writeShort(CPEClass(referenceType, false))

                case IINC.opcode ⇒
                    val IINC(lvIndex, constValue) = i
                    if (modifiedByWide) {
                        modifiedByWide = false
                        instructions.writeShort(lvIndex)
                        instructions.writeShort(constValue)
                    } else {
                        instructions.writeByte(lvIndex)
                        instructions.writeByte(constValue)
                    }

                case JSR.opcode ⇒
                    val JSR(branchoffset) = i
                    instructions.writeShort(branchoffset)
                case JSR_W.opcode ⇒
                    val JSR_W(branchoffset) = i
                    instructions.writeInt(branchoffset)
                case RET.opcode ⇒
                    val RET(lvIndex) = i
                    instructions.writeByte(lvIndex)

                case GOTO.opcode ⇒
                    val GOTO(branchoffset) = i
                    instructions.writeShort(branchoffset)
                case GOTO_W.opcode ⇒
                    val GOTO_W(branchoffset) = i
                    instructions.writeInt(branchoffset)

                case IF_ICMPEQ.opcode | IF_ICMPNE.opcode |
                    IF_ICMPLT.opcode | IF_ICMPLE.opcode |
                    IF_ICMPGT.opcode | IF_ICMPGE.opcode |
                    IFEQ.opcode | IFNE.opcode |
                    IFLT.opcode | IFLE.opcode |
                    IFGT.opcode | IFGE.opcode |
                    IF_ACMPEQ.opcode | IF_ACMPNE.opcode |
                    IFNONNULL.opcode | IFNULL.opcode ⇒
                    val SimpleConditionalBranchInstruction(branchoffset) = i
                    instructions.writeShort(branchoffset)

                case PUTSTATIC.opcode | PUTFIELD.opcode | GETSTATIC.opcode | GETFIELD.opcode ⇒
                    val FieldAccess(declaringClass, fieldName, fieldType) = i
                    val jvmFieldType = fieldType.toJVMTypeName
                    val cpeRef = CPEFieldRef(declaringClass, fieldName, jvmFieldType)
                    instructions.writeShort(cpeRef)

                case INVOKEINTERFACE.opcode |
                    INVOKESPECIAL.opcode |
                    INVOKEVIRTUAL.opcode |
                    INVOKESTATIC.opcode ⇒
                    val MethodInvocationInstruction(declaringClass, isInterface, name, descriptor) =
                        i
                    val cpeRef =
                        if (isInterface)
                            CPEInterfaceMethodRef(declaringClass, name, descriptor.toJVMDescriptor)
                        else
                            CPEMethodRef(declaringClass, name, descriptor.toJVMDescriptor)
                    instructions.writeShort(cpeRef)

                case NEWARRAY.opcode ⇒
                    instructions.writeByte(i.asInstanceOf[NEWARRAY].atype)

                case ANEWARRAY.opcode ⇒
                    val ANEWARRAY(referenceType) = i
                    instructions.writeShort(CPEClass(referenceType, false))

                case MULTIANEWARRAY.opcode ⇒
                    val MULTIANEWARRAY(arrayType, dimensions) = i
                    instructions.writeShort(CPEClass(arrayType, false))
                    instructions.writeByte(dimensions)

                case LDC.opcode ⇒
                    val cpIndex = ConstantsBuffer.getOrCreateCPEntry(i.asInstanceOf[LDC[_]])
                    instructions.writeByte(cpIndex)

                case LDC_W.opcode ⇒
                    instructions.writeShort(
                        i match {
                            case LoadInt_W(value)          ⇒ CPEInteger(value, false)
                            case LoadFloat_W(value)        ⇒ CPEFloat(value, false)
                            case LoadClass_W(value)        ⇒ CPEClass(value, false)
                            case LoadString_W(value)       ⇒ CPEString(value, false)
                            case LoadMethodHandle_W(value) ⇒ CPEMethodHandle(value, false)
                            case LoadMethodType_W(value) ⇒
                                CPEMethodType(value.toJVMDescriptor, false)
                        }
                    )

                case LDC2_W.opcode ⇒
                    i match {
                        case LoadLong(value)   ⇒ instructions.writeShort(CPELong(value))
                        case LoadDouble(value) ⇒ instructions.writeShort(CPEDouble(value))
                    }

                case INVOKEDYNAMIC.opcode ⇒
                    val INVOKEDYNAMIC(bootstrapMethod, name, descriptor) = i
                    val jvmDescriptor = descriptor.toJVMDescriptor
                    val cpEntryIndex = CPEInvokeDynamic(bootstrapMethod, name, jvmDescriptor)
                    // CPEInvokeDynamic automatically creates all cp entries required to
                    // later on tranform the bootstrap method
                    instructions.writeShort(cpEntryIndex)
                    instructions.writeByte(0)
                    instructions.writeByte(0)

                case TABLESWITCH.opcode ⇒
                    val TABLESWITCH(defaultOffset, low, high, jumpOffsets) = i
                    val padding = 3 - (pc % 4)
                    instructions.write(Array.fill(padding) { 0.toByte })
                    instructions.writeInt(defaultOffset)
                    instructions.writeInt(low)
                    instructions.writeInt(high)
                    jumpOffsets.foreach { offset ⇒
                        instructions.writeInt(offset)
                    }
                case LOOKUPSWITCH.opcode ⇒
                    val LOOKUPSWITCH(defaultOffset, npairs) = i
                    val padding = 3 - (pc % 4)
                    instructions.write(Array.fill(padding) { 0.toByte })
                    instructions.writeInt(defaultOffset)
                    instructions.writeInt(npairs.size)
                    npairs.foreach { pair ⇒
                        val (matchValue, offset) = pair
                        instructions.writeInt(matchValue)
                        instructions.writeInt(offset)
                    }

                case WIDE.opcode ⇒
                    if (modifiedByWide)
                        throw new IllegalArgumentException(s"the wide at $pc follows a wide")
                    // modifiedByWide will be set to false by the subsequent instruction
                    modifiedByWide = true
            }
        }

        instructions.flush

        da.Code_attribute(
            attribute_name_index = constantsBuffer.CPEUtf8(bi.CodeAttribute.Name),
            max_stack = code.maxStack,
            max_locals = code.maxLocals,
            code = da.Code(data.toByteArray),
            exceptionTable = code.exceptionHandlers.map(toDA),
            attributes = code.attributes.map(toDA)
        )
    }

    def toDA(
        exceptionHandler: ExceptionHandler
    )(
        implicit
        constantsBuffer: ConstantsBuffer
    ): da.ExceptionTableEntry = {
        val index = if (exceptionHandler.catchType.isDefined) {
            constantsBuffer.CPEClass(exceptionHandler.catchType.get, false)
        } else 0
        da.ExceptionTableEntry(
            exceptionHandler.startPC,
            exceptionHandler.endPC,
            exceptionHandler.handlerPC,
            index
        )
    }

    def toDA(
        attribute: Attribute
    )(
        implicit
        constantsBuffer: ConstantsBuffer
    ): da.Attribute = {
        import constantsBuffer._
        attribute match {
            case code: Code ⇒ toDA(code)

            // direct conversions
            case br.SourceFile(s) ⇒
                da.SourceFile_attribute(CPEUtf8(bi.SourceFileAttribute.Name), CPEUtf8(s))

            case br.Deprecated ⇒
                da.Deprecated_attribute(CPEUtf8(bi.DeprecatedAttribute.Name))

            case br.Synthetic ⇒
                da.Synthetic_attribute(CPEUtf8(bi.SyntheticAttribute.Name))

            case br.SourceDebugExtension(data) ⇒
                val attributeName = bi.SourceDebugExtensionAttribute.Name
                da.SourceDebugExtension_attribute(CPEUtf8(attributeName), data)

            case br.EnclosingMethod(classType, nameOption, descriptorOption) ⇒
                val classIndex = CPEClass(classType, false)
                val nameAndTypeIndex = nameOption match {
                    case Some(name) ⇒ CPENameAndType(name, descriptorOption.get.toJVMDescriptor)
                    case None       ⇒ 0
                }
                val attributeName = bi.EnclosingMethodAttribute.Name
                da.EnclosingMethod_attribute(
                    CPEUtf8(attributeName), classIndex, nameAndTypeIndex
                )

            // ALL CONSTANT FIELD VALUES
            case br.ConstantFloat(value) ⇒
                da.ConstantValue_attribute(
                    CPEUtf8(bi.ConstantValueAttribute.Name),
                    CPEFloat(value, false)
                )
            case br.ConstantInteger(value) ⇒
                da.ConstantValue_attribute(
                    CPEUtf8(bi.ConstantValueAttribute.Name),
                    CPEInteger(value, false)
                )
            case br.ConstantString(value) ⇒
                da.ConstantValue_attribute(
                    CPEUtf8(bi.ConstantValueAttribute.Name),
                    CPEString(value, false)
                )

            case br.ConstantDouble(value) ⇒
                da.ConstantValue_attribute(CPEUtf8(bi.ConstantValueAttribute.Name), CPEDouble(value))
            case br.ConstantLong(value) ⇒
                da.ConstantValue_attribute(CPEUtf8(bi.ConstantValueAttribute.Name), CPELong(value))

        }
    }

    def toDA(constantPool: Array[Constant_Pool_Entry]): Array[da.Constant_Pool_Entry] = {
        constantPool.map { cpEntry ⇒
            if (cpEntry eq null)
                null
            else {
                (cpEntry.tag: @switch) match {
                    case CONSTANT_Class_ID ⇒
                        val CONSTANT_Class_info(nameIndex) = cpEntry
                        da.CONSTANT_Class_info(nameIndex)

                    case CONSTANT_Fieldref_ID ⇒
                        val CONSTANT_Fieldref_info(classIndex, nameAndTypeIndex) = cpEntry
                        da.CONSTANT_Fieldref_info(classIndex, nameAndTypeIndex)

                    case CONSTANT_Methodref_ID ⇒
                        val CONSTANT_Methodref_info(classIndex, nameAndTypeIndex) = cpEntry
                        da.CONSTANT_Methodref_info(classIndex, nameAndTypeIndex)

                    case CONSTANT_InterfaceMethodref_ID ⇒
                        val CONSTANT_InterfaceMethodref_info(classIndex, nameAndTypeIndex) = cpEntry
                        da.CONSTANT_InterfaceMethodref_info(classIndex, nameAndTypeIndex)

                    case CONSTANT_String_ID ⇒
                        val CONSTANT_String_info(s) = cpEntry
                        da.CONSTANT_String_info(s)

                    case CONSTANT_Integer_ID ⇒
                        val CONSTANT_Integer_info(i) = cpEntry
                        da.CONSTANT_Integer_info(i.value)

                    case CONSTANT_Float_ID ⇒
                        val CONSTANT_Float_info(f) = cpEntry
                        da.CONSTANT_Float_info(f.value)

                    case CONSTANT_Long_ID ⇒
                        val CONSTANT_Long_info(l) = cpEntry
                        da.CONSTANT_Long_info(l.value)

                    case CONSTANT_Double_ID ⇒
                        val CONSTANT_Double_info(d) = cpEntry
                        da.CONSTANT_Double_info(d.value)

                    case CONSTANT_NameAndType_ID ⇒
                        val CONSTANT_NameAndType_info(nameIndex, descriptorIndex) = cpEntry
                        da.CONSTANT_NameAndType_info(nameIndex, descriptorIndex)

                    case CONSTANT_Utf8_ID ⇒
                        val CONSTANT_Utf8_info(u) = cpEntry
                        da.CONSTANT_Utf8(u)

                    case CONSTANT_MethodHandle_ID ⇒
                        val CONSTANT_MethodHandle_info(referenceKind, referenceIndex) = cpEntry
                        da.CONSTANT_MethodHandle_info(referenceKind, referenceIndex)

                    case CONSTANT_MethodType_ID ⇒
                        val CONSTANT_MethodType_info(index) = cpEntry
                        da.CONSTANT_MethodType_info(index)

                    case CONSTANT_InvokeDynamic_ID ⇒
                        val CONSTANT_InvokeDynamic_info(bootstrapIndex, nameAndTypeIndex) = cpEntry
                        da.CONSTANT_InvokeDynamic_info(bootstrapIndex, nameAndTypeIndex)

                }
            }
        }
    }

    implicit class BRConstantsBuffer(constantPool: Array[Constant_Pool_Entry]) {
        def toDA: Array[da.Constant_Pool_Entry] = ba.toDA(constantPool)
    }

}
