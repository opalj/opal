/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.opalj.control.fillArraySeq
import org.opalj.control.fillIntArray
import org.opalj.bytecode.BytecodeProcessingFailedException
import org.opalj.br.instructions._
import org.opalj.collection.immutable.IntIntPair

/**
 * Defines a method to parse an array of bytes (containing Java bytecode instructions) and
 * to return an array of [[org.opalj.br.instructions.Instruction]]`s.
 *
 * The target array has the same size as the source array to make sure that branch offsets
 * etc. point to the correct instruction.
 *
 * This reader caches the instructions – primarily – to save memory once all class
 * files are loaded.
 *
 * @author Michael Eichberg
 */
trait CachedBytecodeReaderAndBinding extends InstructionsDeserializer {

    def cache: BytecodeInstructionsCache

    /**
     * Transforms an array of bytes into an array of
     * [[org.opalj.br.instructions.Instruction]]s.
     */
    def Instructions(
        cp:                  Constant_Pool,
        ap_name_index:       Constant_Pool_Index,
        ap_descriptor_index: Constant_Pool_Index,
        source:              Array[Byte]
    ): Instructions = {
        import java.io.DataInputStream
        import java.io.ByteArrayInputStream

        val bas = new ByteArrayInputStream(source)
        val in = new DataInputStream(bas)
        val codeLength = source.size
        val instructions = new Array[Instruction](codeLength)

        var wide: Boolean = false

        def lvIndex(): Int =
            if (wide) {
                wide = false
                in.readUnsignedShort
            } else {
                in.readUnsignedByte
            }

        while (in.available > 0) {
            val index = codeLength - in.available

            instructions(index) = (in.readUnsignedByte: @scala.annotation.switch) match {
                case 50 => AALOAD
                case 83 => AASTORE
                case 1  => ACONST_NULL
                case 25 => cache.ALOAD(lvIndex())
                case 42 => ALOAD_0
                case 43 => ALOAD_1
                case 44 => ALOAD_2
                case 45 => ALOAD_3
                case 189 =>
                    val rt = cp(in.readUnsignedShort).asConstantValue(cp).toReferenceType
                    cache.ANEWARRAY(rt)
                case 176 => ARETURN
                case 190 => ARRAYLENGTH
                case 58  => cache.ASTORE(lvIndex())
                case 75  => ASTORE_0
                case 76  => ASTORE_1
                case 77  => ASTORE_2
                case 78  => ASTORE_3
                case 191 => ATHROW
                case 51  => BALOAD
                case 84  => BASTORE
                case 16  => BIPUSH(in.readByte.toInt /* value */ ) // internally cached
                case 52  => CALOAD
                case 85  => CASTORE
                case 192 =>
                    val rt = cp(in.readUnsignedShort).asConstantValue(cp).toReferenceType
                    cache.CHECKCAST(rt)
                case 144 => D2F
                case 142 => D2I
                case 143 => D2L
                case 99  => DADD
                case 49  => DALOAD
                case 82  => DASTORE
                case 152 => DCMPG
                case 151 => DCMPL
                case 14  => DCONST_0
                case 15  => DCONST_1
                case 111 => DDIV
                case 24  => cache.DLOAD(lvIndex())
                case 38  => DLOAD_0
                case 39  => DLOAD_1
                case 40  => DLOAD_2
                case 41  => DLOAD_3
                case 107 => DMUL
                case 119 => DNEG
                case 115 => DREM
                case 175 => DRETURN
                case 57  => cache.DSTORE(lvIndex())
                case 71  => DSTORE_0
                case 72  => DSTORE_1
                case 73  => DSTORE_2
                case 74  => DSTORE_3
                case 103 => DSUB
                case 89  => DUP
                case 90  => DUP_X1
                case 91  => DUP_X2
                case 92  => DUP2
                case 93  => DUP2_X1
                case 94  => DUP2_X2
                case 141 => F2D
                case 139 => F2I
                case 140 => F2L
                case 98  => FADD
                case 48  => FALOAD
                case 81  => FASTORE
                case 150 => FCMPG
                case 149 => FCMPL
                case 11  => FCONST_0
                case 12  => FCONST_1
                case 13  => FCONST_2
                case 110 => FDIV
                case 23  => cache.FLOAD(lvIndex())
                case 34  => FLOAD_0
                case 35  => FLOAD_1
                case 36  => FLOAD_2
                case 37  => FLOAD_3
                case 106 => FMUL
                case 118 => FNEG
                case 114 => FREM
                case 174 => FRETURN
                case 56  => cache.FSTORE(lvIndex())
                case 67  => FSTORE_0
                case 68  => FSTORE_1
                case 69  => FSTORE_2
                case 70  => FSTORE_3
                case 102 => FSUB
                case 180 =>
                    val (declaringClass, name, fieldType): (ObjectType, String, FieldType) =
                        cp(in.readUnsignedShort).asFieldref(cp)
                    GETFIELD(declaringClass, cache.FieldName(name), fieldType)
                case 178 =>
                    val (declaringClass, name, fieldType): (ObjectType, String, FieldType) =
                        cp(in.readUnsignedShort).asFieldref(cp)
                    GETSTATIC(declaringClass, cache.FieldName(name), fieldType)
                case 167 => cache.GOTO(in.readShort.toInt /* branchoffset */ )
                case 200 => GOTO_W(in.readInt /* branchoffset */ )
                case 145 => I2B
                case 146 => I2C
                case 135 => I2D
                case 134 => I2F
                case 133 => I2L
                case 147 => I2S
                case 96  => IADD
                case 46  => IALOAD
                case 126 => IAND
                case 79  => IASTORE
                case 2   => ICONST_M1
                case 3   => ICONST_0
                case 4   => ICONST_1
                case 5   => ICONST_2
                case 6   => ICONST_3
                case 7   => ICONST_4
                case 8   => ICONST_5
                case 108 => IDIV
                case 165 => cache.IF_ACMPEQ(in.readShort.toInt)
                case 166 => cache.IF_ACMPNE(in.readShort.toInt)
                case 159 => cache.IF_ICMPEQ(in.readShort.toInt)
                case 160 => cache.IF_ICMPNE(in.readShort.toInt)
                case 161 => cache.IF_ICMPLT(in.readShort.toInt)
                case 162 => cache.IF_ICMPGE(in.readShort.toInt)
                case 163 => cache.IF_ICMPGT(in.readShort.toInt)
                case 164 => cache.IF_ICMPLE(in.readShort.toInt)
                case 153 => cache.IFEQ(in.readShort.toInt)
                case 154 => cache.IFNE(in.readShort.toInt)
                case 155 => cache.IFLT(in.readShort.toInt)
                case 156 => cache.IFGE(in.readShort.toInt)
                case 157 => cache.IFGT(in.readShort.toInt)
                case 158 => cache.IFLE(in.readShort.toInt)
                case 199 => cache.IFNONNULL(in.readShort.toInt)
                case 198 => cache.IFNULL(in.readShort.toInt)
                case 132 =>
                    if (wide) {
                        wide = false
                        val lvIndex = in.readUnsignedShort
                        val constValue = in.readShort.toInt
                        IINC(lvIndex, constValue)
                    } else {
                        val lvIndex = in.readUnsignedByte
                        val constValue = in.readByte.toInt
                        IINC(lvIndex, constValue)
                    }
                case 21  => cache.ILOAD(lvIndex())
                case 26  => ILOAD_0
                case 27  => ILOAD_1
                case 28  => ILOAD_2
                case 29  => ILOAD_3
                case 104 => IMUL
                case 116 => INEG
                case 193 =>
                    cache.INSTANCEOF(cp(in.readUnsignedShort).asConstantValue(cp).toReferenceType)
                case 186 => // INVOKEDYNAMIC
                    val cpEntry = cp(in.readUnsignedShort).asInvokeDynamic
                    in.readByte // ignored; fixed value
                    in.readByte // ignored; fixed value
                    registerDeferredAction(cp) { classFile =>
                        deferredInvokedynamicResolution(
                            classFile,
                            cp,
                            ap_name_index,
                            ap_descriptor_index,
                            cpEntry,
                            instructions,
                            index
                        )
                    }
                    //INVOKEDYNAMIC(cpe.bootstrapMethodAttributeIndex, cpe.methodName, cpe.methodDescriptor)
                    INCOMPLETE_INVOKEDYNAMIC
                case 185 =>
                    val methodRef = cp(in.readUnsignedShort).asMethodref(cp)
                    val (declaringClass, _, name, methodDescriptor) = methodRef
                    in.readByte // ignored; fixed value
                    in.readByte // ignored; fixed value
                    INVOKEINTERFACE(
                        declaringClass.asObjectType,
                        cache.MethodName(name),
                        methodDescriptor
                    )
                case 183 =>
                    val methodRef = cp(in.readUnsignedShort).asMethodref(cp)
                    val (declaringClass, isInterface, name, methodDescriptor) = methodRef
                    INVOKESPECIAL(
                        declaringClass.asObjectType, isInterface,
                        cache.MethodName(name), methodDescriptor
                    )
                case 184 =>
                    val methodRef = cp(in.readUnsignedShort).asMethodref(cp)
                    val (declaringClass, isInterface, name, methodDescriptor) = methodRef
                    INVOKESTATIC(
                        declaringClass.asObjectType, isInterface,
                        cache.MethodName(name), methodDescriptor
                    )
                case 182 =>
                    val methodRef = cp(in.readUnsignedShort).asMethodref(cp)
                    val (declaringClass, _, name, methodDescriptor) = methodRef
                    INVOKEVIRTUAL(declaringClass, cache.MethodName(name), methodDescriptor)
                case 128 => IOR
                case 112 => IREM
                case 172 => IRETURN
                case 120 => ISHL
                case 122 => ISHR
                case 54  => cache.ISTORE(lvIndex())
                case 59  => ISTORE_0
                case 60  => ISTORE_1
                case 61  => ISTORE_2
                case 62  => ISTORE_3
                case 100 => ISUB
                case 124 => IUSHR
                case 130 => IXOR
                case 168 => JSR(in.readShort.toInt)
                case 201 => JSR_W(in.readInt)
                case 138 => L2D
                case 137 => L2F
                case 136 => L2I
                case 97  => LADD
                case 47  => LALOAD
                case 127 => LAND
                case 80  => LASTORE
                case 148 => LCMP
                case 9   => LCONST_0
                case 10  => LCONST_1
                case 18 =>
                    val constant = cp(in.readUnsignedByte())
                    if (constant.isDynamic) {
                        registerDeferredAction(cp) { classFile =>
                            deferredDynamicConstantResolution(
                                classFile,
                                cp,
                                ap_name_index,
                                ap_descriptor_index,
                                constant.asDynamic,
                                instructions,
                                index /* <=> pc */
                            )
                        }
                        INCOMPLETE_LDC
                    } else {
                        LDC(constant.asConstantValue(cp))
                    }
                case 19 =>
                    val constant = cp(in.readUnsignedShort())
                    if (constant.isDynamic) {
                        registerDeferredAction(cp) { classFile =>
                            deferredDynamicConstantResolution(
                                classFile,
                                cp,
                                ap_name_index,
                                ap_descriptor_index,
                                constant.asDynamic,
                                instructions,
                                index /* <=> pc */
                            )
                        }
                        INCOMPLETE_LDC_W
                    } else {
                        LDC_W(constant.asConstantValue(cp))
                    }
                case 20 =>
                    val constant = cp(in.readUnsignedShort())
                    if (constant.isDynamic) {
                        registerDeferredAction(cp) { classFile =>
                            deferredDynamicConstantResolution(
                                classFile,
                                cp,
                                ap_name_index,
                                ap_descriptor_index,
                                constant.asDynamic,
                                instructions,
                                index /* <=> pc */
                            )
                        }
                        INCOMPLETE_LDC2_W
                    } else {
                        LDC2_W(constant.asConstantValue(cp))
                    }
                case 109 => LDIV
                case 22  => cache.LLOAD(lvIndex())
                case 30  => LLOAD_0
                case 31  => LLOAD_1
                case 32  => LLOAD_2
                case 33  => LLOAD_3
                case 105 => LMUL
                case 117 => LNEG
                case 171 =>
                    in.skip((3 - (index % 4)).toLong) // skip padding bytes
                    val defaultOffset = in.readInt
                    val npairsCount = in.readInt
                    val npairs = fillArraySeq(npairsCount) { IntIntPair(in.readInt, in.readInt) }
                    LOOKUPSWITCH(defaultOffset, npairs)
                case 129 => LOR
                case 113 => LREM
                case 173 => LRETURN
                case 121 => LSHL
                case 123 => LSHR
                case 55  => cache.LSTORE(lvIndex())
                case 63  => LSTORE_0
                case 64  => LSTORE_1
                case 65  => LSTORE_2
                case 66  => LSTORE_3
                case 101 => LSUB
                case 125 => LUSHR
                case 131 => LXOR
                case 194 => MONITORENTER
                case 195 => MONITOREXIT
                case 197 =>
                    MULTIANEWARRAY(
                        // componentType
                        cp(in.readUnsignedShort).asConstantValue(cp).toReferenceType.asArrayType,
                        //  dimensions
                        in.readUnsignedByte
                    )
                case 187 => cache.NEW(cp(in.readUnsignedShort).asObjectType(cp))
                case 188 => NEWARRAY(in.readByte.toInt) // is internally cached
                case 0   => NOP
                case 87  => POP
                case 88  => POP2
                case 181 =>
                    val (declaringClass, name, fieldType): (ObjectType, String, FieldType) =
                        cp(in.readUnsignedShort).asFieldref(cp)
                    PUTFIELD(declaringClass, cache.FieldName(name), fieldType)
                case 179 =>
                    val (declaringClass, name, fieldType): (ObjectType, String, FieldType) =
                        cp(in.readUnsignedShort).asFieldref(cp)
                    PUTSTATIC(declaringClass, cache.FieldName(name), fieldType)
                case 169 =>
                    cache.RET(
                        if (wide) {
                            wide = false
                            in.readUnsignedShort
                        } else {
                            in.readUnsignedByte
                        }
                    )
                case 177 => RETURN
                case 53  => SALOAD
                case 86  => SASTORE
                case 17  => cache.SIPUSH(in.readShort.toInt /* value */ )
                case 95  => SWAP
                case 170 =>
                    in.skip((3 - (index % 4)).toLong) // skip padding bytes
                    val defaultOffset = in.readInt
                    val low = in.readInt
                    val high = in.readInt
                    val jumpOffsets = fillIntArray(high - low + 1) { in.readInt }
                    TABLESWITCH(defaultOffset, low, high, jumpOffsets)
                case 196 =>
                    wide = true
                    WIDE

                case opcode =>
                    throw new BytecodeProcessingFailedException("unsupported opcode: "+opcode)
            }

        }
        instructions
    }

}
