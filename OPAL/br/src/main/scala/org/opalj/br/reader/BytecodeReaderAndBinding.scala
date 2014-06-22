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
package reader

import instructions._

/**
 * Defines a method to parse an array of bytes (containing Java bytecode instructions) and
 * to return an array of [[org.opalj.br.instructions.Instruction]]`s.
 *
 * The target array has the same size as the source array to make sure that branch offsets
 * etc. point to the correct instruction.
 *
 * @author Michael Eichberg
 */
trait BytecodeReaderAndBinding extends DeferredInvokedynamicResolution {

    /**
     * Transforms an array of bytes into an array of
     * [[org.opalj.br.instructions.Instruction]]s.
     */
    def Instructions(
        cp: Constant_Pool,
        source: Array[Byte]): Instructions = {
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
                case 50  ⇒ AALOAD
                case 83  ⇒ AASTORE
                case 1   ⇒ ACONST_NULL
                case 25  ⇒ ALOAD(lvIndex())
                case 42  ⇒ ALOAD_0
                case 43  ⇒ ALOAD_1
                case 44  ⇒ ALOAD_2
                case 45  ⇒ ALOAD_3
                case 189 ⇒ ANEWARRAY(cp(in.readUnsignedShort).asConstantValue(cp).toReferenceType)
                case 176 ⇒ ARETURN
                case 190 ⇒ ARRAYLENGTH
                case 58  ⇒ ASTORE(lvIndex)
                case 75  ⇒ ASTORE_0
                case 76  ⇒ ASTORE_1
                case 77  ⇒ ASTORE_2
                case 78  ⇒ ASTORE_3
                case 191 ⇒ ATHROW
                case 51  ⇒ BALOAD
                case 84  ⇒ BASTORE
                case 16  ⇒ BIPUSH(in.readByte /* value */ )
                case 52  ⇒ CALOAD
                case 85  ⇒ CASTORE
                case 192 ⇒
                    val referenceType = cp(in.readUnsignedShort).asConstantValue(cp).toReferenceType
                    CHECKCAST(referenceType)
                case 144 ⇒ D2F
                case 142 ⇒ D2I
                case 143 ⇒ D2L
                case 99  ⇒ DADD
                case 49  ⇒ DALOAD
                case 82  ⇒ DASTORE
                case 152 ⇒ DCMPG
                case 151 ⇒ DCMPL
                case 14  ⇒ DCONST_0
                case 15  ⇒ DCONST_1
                case 111 ⇒ DDIV
                case 24  ⇒ DLOAD(lvIndex)
                case 38  ⇒ DLOAD_0
                case 39  ⇒ DLOAD_1
                case 40  ⇒ DLOAD_2
                case 41  ⇒ DLOAD_3
                case 107 ⇒ DMUL
                case 119 ⇒ DNEG
                case 115 ⇒ DREM
                case 175 ⇒ DRETURN
                case 57  ⇒ DSTORE(lvIndex)
                case 71  ⇒ DSTORE_0
                case 72  ⇒ DSTORE_1
                case 73  ⇒ DSTORE_2
                case 74  ⇒ DSTORE_3
                case 103 ⇒ DSUB
                case 89  ⇒ DUP
                case 90  ⇒ DUP_X1
                case 91  ⇒ DUP_X2
                case 92  ⇒ DUP2
                case 93  ⇒ DUP2_X1
                case 94  ⇒ DUP2_X2
                case 141 ⇒ F2D
                case 139 ⇒ F2I
                case 140 ⇒ F2L
                case 98  ⇒ FADD
                case 48  ⇒ FALOAD
                case 81  ⇒ FASTORE
                case 150 ⇒ FCMPG
                case 149 ⇒ FCMPL
                case 11  ⇒ FCONST_0
                case 12  ⇒ FCONST_1
                case 13  ⇒ FCONST_2
                case 110 ⇒ FDIV
                case 23  ⇒ FLOAD(lvIndex)
                case 34  ⇒ FLOAD_0
                case 35  ⇒ FLOAD_1
                case 36  ⇒ FLOAD_2
                case 37  ⇒ FLOAD_3
                case 106 ⇒ FMUL
                case 118 ⇒ FNEG
                case 114 ⇒ FREM
                case 174 ⇒ FRETURN
                case 56  ⇒ FSTORE(lvIndex)
                case 67  ⇒ FSTORE_0
                case 68  ⇒ FSTORE_1
                case 69  ⇒ FSTORE_2
                case 70  ⇒ FSTORE_3
                case 102 ⇒ FSUB
                case 180 ⇒
                    val (declaringClass, name, fieldType): (ObjectType, String, FieldType) =
                        cp(in.readUnsignedShort).asFieldref(cp)
                    GETFIELD(declaringClass, name, fieldType)
                case 178 ⇒
                    val (declaringClass, name, fieldType): (ObjectType, String, FieldType) =
                        cp(in.readUnsignedShort).asFieldref(cp)
                    GETSTATIC(declaringClass, name, fieldType)
                case 167 ⇒ GOTO(in.readShort /* branchoffset */ )
                case 200 ⇒ GOTO_W(in.readInt /* branchoffset */ )
                case 145 ⇒ I2B
                case 146 ⇒ I2C
                case 135 ⇒ I2D
                case 134 ⇒ I2F
                case 133 ⇒ I2L
                case 147 ⇒ I2S
                case 96  ⇒ IADD
                case 46  ⇒ IALOAD
                case 126 ⇒ IAND
                case 79  ⇒ IASTORE
                case 2   ⇒ ICONST_M1
                case 3   ⇒ ICONST_0
                case 4   ⇒ ICONST_1
                case 5   ⇒ ICONST_2
                case 6   ⇒ ICONST_3
                case 7   ⇒ ICONST_4
                case 8   ⇒ ICONST_5
                case 108 ⇒ IDIV
                case 165 ⇒ IF_ACMPEQ(in.readShort)
                case 166 ⇒ IF_ACMPNE(in.readShort)
                case 159 ⇒ IF_ICMPEQ(in.readShort)
                case 160 ⇒ IF_ICMPNE(in.readShort)
                case 161 ⇒ IF_ICMPLT(in.readShort)
                case 162 ⇒ IF_ICMPGE(in.readShort)
                case 163 ⇒ IF_ICMPGT(in.readShort)
                case 164 ⇒ IF_ICMPLE(in.readShort)
                case 153 ⇒ IFEQ(in.readShort)
                case 154 ⇒ IFNE(in.readShort)
                case 155 ⇒ IFLT(in.readShort)
                case 156 ⇒ IFGE(in.readShort)
                case 157 ⇒ IFGT(in.readShort)
                case 158 ⇒ IFLE(in.readShort)
                case 199 ⇒ IFNONNULL(in.readShort)
                case 198 ⇒ IFNULL(in.readShort)
                case 132 ⇒
                    if (wide) {
                        wide = false
                        val lvIndex = in.readUnsignedShort
                        val constValue = in.readShort
                        IINC(lvIndex, constValue)
                    } else {
                        val lvIndex = in.readUnsignedByte
                        val constValue = in.readByte
                        IINC(lvIndex, constValue)
                    }
                case 21  ⇒ ILOAD(lvIndex)
                case 26  ⇒ ILOAD_0
                case 27  ⇒ ILOAD_1
                case 28  ⇒ ILOAD_2
                case 29  ⇒ ILOAD_3
                case 104 ⇒ IMUL
                case 116 ⇒ INEG
                case 193 ⇒ INSTANCEOF(cp(in.readUnsignedShort).asConstantValue(cp).toReferenceType)
                case 186 ⇒
                    val cpEntry = cp(in.readUnsignedShort).asInvokeDynamic
                    in.readByte // ignored; fixed value
                    in.readByte // ignored; fixed value
                    registerDeferredAction(cp) { classFile ⇒
                        deferredInvokedynamicResolution(
                            classFile, cp, cpEntry, instructions, index
                        )
                    }
                    //INVOKEDYNAMIC(cpe.bootstrapMethodAttributeIndex, cpe.methodName, cpe.methodDescriptor)
                    UNRESOLVED_INVOKEDYNAMIC
                case 185 ⇒
                    val (declaringClass, name, methodDescriptor) /*: (ReferenceType,String,MethodDescriptor)*/ =
                        cp(in.readUnsignedShort).asMethodref(cp) // methodRef
                    in.readByte // ignored; fixed value
                    in.readByte // ignored; fixed value
                    INVOKEINTERFACE(declaringClass.asObjectType, name, methodDescriptor)
                case 183 ⇒
                    val (declaringClass, name, methodDescriptor) /*: (ReferenceType,String,MethodDescriptor)*/ =
                        cp(in.readUnsignedShort).asMethodref(cp)
                    INVOKESPECIAL(declaringClass.asObjectType, name, methodDescriptor)
                case 184 ⇒
                    val (declaringClass, name, methodDescriptor) /*: (ReferenceType,String,MethodDescriptor)*/ =
                        cp(in.readUnsignedShort).asMethodref(cp) // methodRef
                    INVOKESTATIC(declaringClass.asObjectType, name, methodDescriptor)
                case 182 ⇒
                    val (declaringClass, name, methodDescriptor) /*: (ReferenceType,String,MethodDescriptor)*/ =
                        cp(in.readUnsignedShort).asMethodref(cp) // methodRef
                    INVOKEVIRTUAL(declaringClass, name, methodDescriptor)
                case 128 ⇒ IOR
                case 112 ⇒ IREM
                case 172 ⇒ IRETURN
                case 120 ⇒ ISHL
                case 122 ⇒ ISHR
                case 54  ⇒ ISTORE(lvIndex)
                case 59  ⇒ ISTORE_0
                case 60  ⇒ ISTORE_1
                case 61  ⇒ ISTORE_2
                case 62  ⇒ ISTORE_3
                case 100 ⇒ ISUB
                case 124 ⇒ IUSHR
                case 130 ⇒ IXOR
                case 168 ⇒ JSR(in.readShort)
                case 201 ⇒ JSR_W(in.readInt)
                case 138 ⇒ L2D
                case 137 ⇒ L2F
                case 136 ⇒ L2I
                case 97  ⇒ LADD
                case 47  ⇒ LALOAD
                case 127 ⇒ LAND
                case 80  ⇒ LASTORE
                case 148 ⇒ LCMP
                case 9   ⇒ LCONST_0
                case 10  ⇒ LCONST_1
                case 18  ⇒ LDC(cp(in.readUnsignedByte).asConstantValue(cp))
                case 19  ⇒ LDC_W(cp(in.readUnsignedShort).asConstantValue(cp))
                case 20  ⇒ LDC2_W(cp(in.readUnsignedShort).asConstantValue(cp))
                case 109 ⇒ LDIV
                case 22  ⇒ LLOAD(lvIndex)
                case 30  ⇒ LLOAD_0
                case 31  ⇒ LLOAD_1
                case 32  ⇒ LLOAD_2
                case 33  ⇒ LLOAD_3
                case 105 ⇒ LMUL
                case 117 ⇒ LNEG
                case 171 ⇒
                    in.skip(3 - (index % 4)) // skip padding bytes
                    val defaultOffset = in.readInt
                    val npairsCount = in.readInt
                    val npairs: IndexedSeq[(Int, Int)] =
                        repeat(npairsCount) {
                            (in.readInt, in.readInt)
                        }
                    LOOKUPSWITCH(defaultOffset, npairs)
                case 129 ⇒ LOR
                case 113 ⇒ LREM
                case 173 ⇒ LRETURN
                case 121 ⇒ LSHL
                case 123 ⇒ LSHR
                case 55  ⇒ LSTORE(lvIndex)
                case 63  ⇒ LSTORE_0
                case 64  ⇒ LSTORE_1
                case 65  ⇒ LSTORE_2
                case 66  ⇒ LSTORE_3
                case 101 ⇒ LSUB
                case 125 ⇒ LUSHR
                case 131 ⇒ LXOR
                case 194 ⇒ MONITORENTER
                case 195 ⇒ MONITOREXIT
                case 197 ⇒
                    MULTIANEWARRAY(
                        // componentType 
                        cp(in.readUnsignedShort).asConstantValue(cp).toReferenceType.asArrayType,
                        //  dimensions 
                        in.readUnsignedByte
                    )
                case 187 ⇒ NEW(cp(in.readUnsignedShort).asObjectType(cp))
                case 188 ⇒ NEWARRAY(in.readByte)
                case 0   ⇒ NOP
                case 87  ⇒ POP
                case 88  ⇒ POP2
                case 181 ⇒
                    val (declaringClass, name, fieldType): (ObjectType, String, FieldType) =
                        cp(in.readUnsignedShort).asFieldref(cp)
                    PUTFIELD(declaringClass, name, fieldType)
                case 179 ⇒
                    val (declaringClass, name, fieldType): (ObjectType, String, FieldType) =
                        cp(in.readUnsignedShort).asFieldref(cp)
                    PUTSTATIC(declaringClass, name, fieldType)
                case 169 ⇒
                    RET(
                        if (wide) {
                            wide = false
                            in.readUnsignedShort
                        } else {
                            in.readUnsignedByte
                        }
                    )
                case 177 ⇒ RETURN
                case 53  ⇒ SALOAD
                case 86  ⇒ SASTORE
                case 17  ⇒ SIPUSH(in.readShort /* value */ )
                case 95  ⇒ SWAP
                case 170 ⇒
                    in.skip(3 - (index % 4)) // skip padding bytes
                    val defaultOffset = in.readInt
                    val low = in.readInt
                    val high = in.readInt
                    val jumpOffsets: IndexedSeq[Int] = repeat(high - low + 1) { in.readInt }
                    TABLESWITCH(defaultOffset, low, high, jumpOffsets)
                case 196 ⇒
                    wide = true
                    WIDE

                case opcode ⇒
                    throw new BytecodeProcessingFailedException(
                        "unsupported opcode: "+opcode)
            }

        }
        instructions
    }

}

