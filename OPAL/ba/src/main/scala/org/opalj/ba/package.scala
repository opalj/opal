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
            val opcode = i.opcode
            instructions.writeByte(opcode)
            i match {

                /*
                
                case ALOAD_0.opcode ⇒ loadInstruction(0, ComputationalTypeReference)
                case ALOAD_1.opcode ⇒ loadInstruction(1, ComputationalTypeReference)
                case ALOAD_2.opcode ⇒ loadInstruction(2, ComputationalTypeReference)
                case ALOAD_3.opcode ⇒ loadInstruction(3, ComputationalTypeReference)
                case ALOAD.opcode ⇒
                    val lvIndex = as[ALOAD](instruction).lvIndex
                    loadInstruction(lvIndex, ComputationalTypeReference)

                case ASTORE_0.opcode ⇒ storeInstruction(0)
                case ASTORE_1.opcode ⇒ storeInstruction(1)
                case ASTORE_2.opcode ⇒ storeInstruction(2)
                case ASTORE_3.opcode ⇒ storeInstruction(3)
                case ASTORE.opcode   ⇒ storeInstruction(as[ASTORE](instruction).lvIndex)

                case ILOAD_0.opcode  ⇒ loadInstruction(0, ComputationalTypeInt)
                case ILOAD_1.opcode  ⇒ loadInstruction(1, ComputationalTypeInt)
                case ILOAD_2.opcode  ⇒ loadInstruction(2, ComputationalTypeInt)
                case ILOAD_3.opcode  ⇒ loadInstruction(3, ComputationalTypeInt)
                case ILOAD.opcode ⇒
                    loadInstruction(as[ILOAD](instruction).lvIndex, ComputationalTypeInt)

                case ISTORE_0.opcode ⇒ storeInstruction(0)
                case ISTORE_1.opcode ⇒ storeInstruction(1)
                case ISTORE_2.opcode ⇒ storeInstruction(2)
                case ISTORE_3.opcode ⇒ storeInstruction(3)
                case ISTORE.opcode   ⇒ storeInstruction(as[ISTORE](instruction).lvIndex)

                case DLOAD_0.opcode  ⇒ loadInstruction(0, ComputationalTypeDouble)
                case DLOAD_1.opcode  ⇒ loadInstruction(1, ComputationalTypeDouble)
                case DLOAD_2.opcode  ⇒ loadInstruction(2, ComputationalTypeDouble)
                case DLOAD_3.opcode  ⇒ loadInstruction(3, ComputationalTypeDouble)
                case DLOAD.opcode ⇒
                    loadInstruction(as[DLOAD](instruction).lvIndex, ComputationalTypeDouble)

                case DSTORE_0.opcode ⇒ storeInstruction(0)
                case DSTORE_1.opcode ⇒ storeInstruction(1)
                case DSTORE_2.opcode ⇒ storeInstruction(2)
                case DSTORE_3.opcode ⇒ storeInstruction(3)
                case DSTORE.opcode   ⇒ storeInstruction(as[DSTORE](instruction).lvIndex)

                case FLOAD_0.opcode  ⇒ loadInstruction(0, ComputationalTypeFloat)
                case FLOAD_1.opcode  ⇒ loadInstruction(1, ComputationalTypeFloat)
                case FLOAD_2.opcode  ⇒ loadInstruction(2, ComputationalTypeFloat)
                case FLOAD_3.opcode  ⇒ loadInstruction(3, ComputationalTypeFloat)
                case FLOAD.opcode ⇒
                    loadInstruction(as[FLOAD](instruction).lvIndex, ComputationalTypeFloat)

                case FSTORE_0.opcode ⇒ storeInstruction(0)
                case FSTORE_1.opcode ⇒ storeInstruction(1)
                case FSTORE_2.opcode ⇒ storeInstruction(2)
                case FSTORE_3.opcode ⇒ storeInstruction(3)
                case FSTORE.opcode   ⇒ storeInstruction(as[FSTORE](instruction).lvIndex)

                case LLOAD_0.opcode  ⇒ loadInstruction(0, ComputationalTypeLong)
                case LLOAD_1.opcode  ⇒ loadInstruction(1, ComputationalTypeLong)
                case LLOAD_2.opcode  ⇒ loadInstruction(2, ComputationalTypeLong)
                case LLOAD_3.opcode  ⇒ loadInstruction(3, ComputationalTypeLong)
                case LLOAD.opcode ⇒
                    loadInstruction(as[LLOAD](instruction).lvIndex, ComputationalTypeLong)

                case LSTORE_0.opcode ⇒ storeInstruction(0)
                case LSTORE_1.opcode ⇒ storeInstruction(1)
                case LSTORE_2.opcode ⇒ storeInstruction(2)
                case LSTORE_3.opcode ⇒ storeInstruction(3)
                case LSTORE.opcode   ⇒ storeInstruction(as[LSTORE](instruction).lvIndex)

                case IRETURN.opcode  ⇒ returnInstruction(OperandVar.IntReturnValue)
                case LRETURN.opcode  ⇒ returnInstruction(OperandVar.LongReturnValue)
                case FRETURN.opcode  ⇒ returnInstruction(OperandVar.FloatReturnValue)
                case DRETURN.opcode  ⇒ returnInstruction(OperandVar.DoubleReturnValue)
                case ARETURN.opcode  ⇒ returnInstruction(OperandVar.ReferenceReturnValue)
                case RETURN.opcode   ⇒ statements(pc) = List(Return(pc))

                case AALOAD.opcode   ⇒ arrayLoad(ComputationalTypeReference)
                case DALOAD.opcode   ⇒ arrayLoad(ComputationalTypeDouble)
                case FALOAD.opcode   ⇒ arrayLoad(ComputationalTypeFloat)
                case IALOAD.opcode   ⇒ arrayLoad(ComputationalTypeInt)
                case LALOAD.opcode   ⇒ arrayLoad(ComputationalTypeLong)
                case SALOAD.opcode   ⇒ arrayLoad(ComputationalTypeInt)
                case BALOAD.opcode   ⇒ arrayLoad(ComputationalTypeInt)
                case CALOAD.opcode   ⇒ arrayLoad(ComputationalTypeInt)

                case AASTORE.opcode | DASTORE.opcode |
                    FASTORE.opcode | IASTORE.opcode |
                    LASTORE.opcode | SASTORE.opcode |
                    BASTORE.opcode | CASTORE.opcode ⇒
                case ARRAYLENGTH.opcode ⇒
                case BIPUSH.opcode | SIPUSH.opcode ⇒
                case IF_ICMPEQ.opcode | IF_ICMPNE.opcode |
                    IF_ICMPLT.opcode | IF_ICMPLE.opcode |
                    IF_ICMPGT.opcode | IF_ICMPGE.opcode ⇒
                case IFEQ.opcode | IFNE.opcode |
                    IFLT.opcode | IFLE.opcode |
                    IFGT.opcode | IFGE.opcode ⇒
                case IF_ACMPEQ.opcode | IF_ACMPNE.opcode ⇒
                case IFNONNULL.opcode | IFNULL.opcode ⇒
                case DCMPG.opcode | FCMPG.opcode ⇒ compareValues(CMPG)
                case DCMPL.opcode | FCMPL.opcode ⇒ compareValues(CMPL)
                case LCMP.opcode                 ⇒ compareValues(CMP)
                case SWAP.opcode ⇒
                case DADD.opcode | FADD.opcode | IADD.opcode | LADD.opcode ⇒
                case DDIV.opcode | FDIV.opcode | IDIV.opcode | LDIV.opcode ⇒
                case DNEG.opcode | FNEG.opcode | INEG.opcode | LNEG.opcode ⇒
                case DMUL.opcode | FMUL.opcode | IMUL.opcode | LMUL.opcode ⇒
                case DREM.opcode | FREM.opcode | IREM.opcode | LREM.opcode ⇒
                case DSUB.opcode | FSUB.opcode | ISUB.opcode | LSUB.opcode ⇒
                case IINC.opcode ⇒
                case IAND.opcode | LAND.opcode   ⇒ binaryArithmeticOperation(And)
                case IOR.opcode | LOR.opcode     ⇒ binaryArithmeticOperation(Or)
                case ISHL.opcode | LSHL.opcode   ⇒ binaryArithmeticOperation(ShiftLeft)
                case ISHR.opcode | LSHR.opcode   ⇒ binaryArithmeticOperation(ShiftRight)
                case IUSHR.opcode | LUSHR.opcode ⇒ binaryArithmeticOperation(UnsignedShiftRight)
                case IXOR.opcode | LXOR.opcode   ⇒ binaryArithmeticOperation(XOr)
              case ICONST_0.opcode | ICONST_1.opcode |
                    ICONST_2.opcode | ICONST_3.opcode |
                    ICONST_4.opcode | ICONST_5.opcode |
                    ICONST_M1.opcode ⇒
                case ACONST_NULL.opcode ⇒
                case DCONST_0.opcode | DCONST_1.opcode ⇒
                case FCONST_0.opcode | FCONST_1.opcode | FCONST_2.opcode ⇒
                case LCONST_0.opcode | LCONST_1.opcode ⇒
                case LDC.opcode | LDC_W.opcode | LDC2_W.opcode ⇒
                case INVOKEINTERFACE.opcode |
                    INVOKESPECIAL.opcode |
                    INVOKEVIRTUAL.opcode ⇒
                  case INVOKESTATIC.opcode ⇒
                case INVOKEDYNAMIC.opcode ⇒
                case PUTSTATIC.opcode ⇒
                case PUTFIELD.opcode ⇒
                case GETSTATIC.opcode ⇒
                case GETFIELD.opcode ⇒
                case NEW.opcode ⇒
                case NEWARRAY.opcode ⇒
                case ANEWARRAY.opcode ⇒
                case MULTIANEWARRAY.opcode ⇒
                case GOTO.opcode | GOTO_W.opcode ⇒
                case JSR.opcode | JSR_W.opcode ⇒
                case RET.opcode ⇒
                case NOP.opcode ⇒
                case POP.opcode ⇒
                case POP2.opcode ⇒
                case INSTANCEOF.opcode ⇒
                case CHECKCAST.opcode ⇒
                case MONITORENTER.opcode ⇒
                case MONITOREXIT.opcode ⇒
                case TABLESWITCH.opcode ⇒
                case LOOKUPSWITCH.opcode ⇒
                case DUP.opcode ⇒
                case DUP_X1.opcode ⇒
                case DUP_X2.opcode ⇒
                case DUP2.opcode ⇒
                case DUP2_X1.opcode ⇒
                case DUP2_X2.opcode ⇒
                case D2F.opcode | I2F.opcode | L2F.opcode ⇒ primitiveCastOperation(FloatType)
                case D2I.opcode | F2I.opcode | L2I.opcode ⇒ primitiveCastOperation(IntegerType)
                case D2L.opcode | I2L.opcode | F2L.opcode ⇒ primitiveCastOperation(LongType)
                case F2D.opcode | I2D.opcode | L2D.opcode ⇒ primitiveCastOperation(DoubleType)
                case I2C.opcode                           ⇒ primitiveCastOperation(CharType)
                case I2B.opcode                           ⇒ primitiveCastOperation(ByteType)
                case I2S.opcode                           ⇒ primitiveCastOperation(ShortType)
                case ATHROW.opcode ⇒
                case WIDE.opcode ⇒
                
                
                */

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
        constantPoolBuffer.toArray.map { cpEntry ⇒
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

    implicit class BRConstantPoolBuffer(constantPoolBuffer: ConstantPoolBuffer) {
        def toDA: Array[da.Constant_Pool_Entry] = ba.toDA(constantPoolBuffer)
    }

}
