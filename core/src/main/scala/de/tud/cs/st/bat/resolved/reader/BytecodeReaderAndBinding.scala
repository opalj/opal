/*
 License (BSD Style License):
 Copyright (c) 2009, 2011
 Software Technology Group
 Department of Computer Science
 Technische Universität Darmstadt
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 - Redistributions of source code must retain the above copyright notice,
   this list of conditions and the following disclaimer.
 - Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.
 - Neither the name of the Software Technology Group or Technische
   Universität Darmstadt nor the names of its contributors may be used to
   endorse or promote products derived from this software without specific
   prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
*/

package de.tud.cs.st.bat.resolved
package reader

import de.tud.cs.st.util.ControlAbstractions.repeat

/**
  * Defines a method to parse an array of bytes (with Java bytecode instructions) and to return an array
  * of `Instruction`s.
  *
  * The target array has the same size to make sure that jump offsets etc.
  * point to the correct instruction.
  *
  * @author Michael Eichberg
  */
trait BytecodeReaderAndBinding extends ConstantPoolBinding with CodeBinding {

    import java.io.DataInputStream
    import java.io.ByteArrayInputStream

    override type Constant_Pool = Array[Constant_Pool_Entry]

    /**
      * Transforms an array of bytes into an array of [[de.tud.cs.st.bat.resolved.Instruction]]s.
      */
    def Instructions(source: Array[Byte])(implicit cp: Constant_Pool): Instructions = {
        val bas = new ByteArrayInputStream(source)
        val in = new DataInputStream(bas)
        val codeLength = source.size
        val instructions = new Array[Instruction](codeLength)
        var previousInstruction: Instruction = null
        while (in.available > 0) {
            val index = codeLength - in.available
            previousInstruction = parsers(in.readUnsignedByte)(previousInstruction, index, in, cp)
            instructions(index) = previousInstruction
        }
        instructions
    }

    // (previousInstruction: Instruction,
    //  index : Int,
    //  in : DataInputStream,
    //  cp : Constant_Pool
    // ) => Instruction
    private val parsers: Array[(Instruction, Int, DataInputStream, Constant_Pool) ⇒ Instruction] = new Array(256)

    // _____________________________________________________________________________________________
    //
    // INITIALIZE THE PARSERS ARRAY
    // _____________________________________________________________________________________________
    //

    parsers(50) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        AALOAD // instance of the instruction
    }

    parsers(83) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        AASTORE // instance of the instruction
    }

    parsers(1) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        ACONST_NULL // instance of the instruction
    }

    parsers(25) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        if (WIDE == previousInstruction) {
            ALOAD(in.readUnsignedShort /* lvIndex */ )
        }
        else {
            ALOAD(in.readUnsignedByte /* lvIndex */ )
        }
    }

    parsers(42) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        ALOAD_0 // instance of the instruction
    }

    parsers(43) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        ALOAD_1 // instance of the instruction
    }

    parsers(44) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        ALOAD_2 // instance of the instruction
    }

    parsers(45) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        ALOAD_3 // instance of the instruction
    }

    parsers(189) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // componentType
        val cv: ConstantValue[_] = cp(in.readUnsignedShort).asConstantValue(cp)
        ANEWARRAY(cv.toClass)
    }

    parsers(176) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        ARETURN // instance of the instruction
    }

    parsers(190) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        ARRAYLENGTH // instance of the instruction
    }

    parsers(58) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        if (WIDE == previousInstruction) {
            ASTORE(in.readUnsignedShort /* lvIndex */ )
        }
        else {
            ASTORE(in.readUnsignedByte /* lvIndex */ )
        }
    }

    parsers(75) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        ASTORE_0 // instance of the instruction
    }

    parsers(76) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        ASTORE_1 // instance of the instruction
    }

    parsers(77) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        ASTORE_2 // instance of the instruction
    }

    parsers(78) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        ASTORE_3 // instance of the instruction
    }

    parsers(191) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        ATHROW // instance of the instruction
    }

    parsers(51) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        BALOAD // instance of the instruction
    }

    parsers(84) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        BASTORE // instance of the instruction
    }

    parsers(16) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        BIPUSH(in.readByte /* value */ )
    }

    parsers(52) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        CALOAD // instance of the instruction
    }

    parsers(85) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        CASTORE // instance of the instruction
    }

    parsers(192) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // referenceType
        val cv: ConstantValue[_] = cp(in.readUnsignedShort).asConstantValue(cp)
        CHECKCAST(cv.toClass)
    }

    parsers(144) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        D2F // instance of the instruction
    }

    parsers(142) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        D2I // instance of the instruction
    }

    parsers(143) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        D2L // instance of the instruction
    }

    parsers(99) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        DADD // instance of the instruction
    }

    parsers(49) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        DALOAD // instance of the instruction
    }

    parsers(82) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        DASTORE // instance of the instruction
    }

    parsers(152) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        DCMPG // instance of the instruction
    }

    parsers(151) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        DCMPL // instance of the instruction
    }

    parsers(14) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        DCONST_0 // instance of the instruction
    }

    parsers(15) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        DCONST_1 // instance of the instruction
    }

    parsers(111) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        DDIV // instance of the instruction
    }

    parsers(24) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        if (WIDE == previousInstruction) {
            DLOAD(in.readUnsignedShort /* lvIndex */ )
        }
        else {
            DLOAD(in.readUnsignedByte /* lvIndex */ )
        }
    }

    parsers(38) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        DLOAD_0 // instance of the instruction
    }

    parsers(39) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        DLOAD_1 // instance of the instruction
    }

    parsers(40) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        DLOAD_2 // instance of the instruction
    }

    parsers(41) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        DLOAD_3 // instance of the instruction
    }

    parsers(107) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        DMUL // instance of the instruction
    }

    parsers(119) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        DNEG // instance of the instruction
    }

    parsers(115) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        DREM // instance of the instruction
    }

    parsers(175) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        DRETURN // instance of the instruction
    }

    parsers(57) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        if (WIDE == previousInstruction) {
            DSTORE(in.readUnsignedShort /* lv_index */ )
        }
        else {
            DSTORE(in.readUnsignedByte /* lv_index*/ )
        }
    }

    parsers(71) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        DSTORE_0 // instance of the instruction
    }

    parsers(72) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        DSTORE_1 // instance of the instruction
    }

    parsers(73) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        DSTORE_2 // instance of the instruction
    }

    parsers(74) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        DSTORE_3 // instance of the instruction
    }

    parsers(103) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        DSUB // instance of the instruction
    }

    parsers(89) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        DUP // instance of the instruction
    }

    parsers(90) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        DUP_X1 // instance of the instruction
    }

    parsers(91) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        DUP_X2 // instance of the instruction
    }

    parsers(92) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        DUP2 // instance of the instruction
    }

    parsers(93) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        DUP2_X1 // instance of the instruction
    }

    parsers(94) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        DUP2_X2 // instance of the instruction
    }

    parsers(141) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        F2D // instance of the instruction
    }

    parsers(139) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        F2I // instance of the instruction
    }

    parsers(140) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        F2L // instance of the instruction
    }

    parsers(98) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        FADD // instance of the instruction
    }

    parsers(48) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        FALOAD // instance of the instruction
    }

    parsers(81) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        FASTORE // instance of the instruction
    }

    parsers(150) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        FCMPG // instance of the instruction
    }

    parsers(149) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        FCMPL // instance of the instruction
    }

    parsers(11) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        FCONST_0 // instance of the instruction
    }

    parsers(12) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        FCONST_1 // instance of the instruction
    }

    parsers(13) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        FCONST_2 // instance of the instruction
    }

    parsers(110) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        FDIV // instance of the instruction
    }

    parsers(23) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        if (WIDE == previousInstruction) {
            FLOAD(in.readUnsignedShort)
        }
        else {
            FLOAD(in.readUnsignedByte)
        }
    }

    parsers(34) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        FLOAD_0 // instance of the instruction
    }

    parsers(35) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        FLOAD_1 // instance of the instruction
    }

    parsers(36) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        FLOAD_2 // instance of the instruction
    }

    parsers(37) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        FLOAD_3 // instance of the instruction
    }

    parsers(106) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        FMUL // instance of the instruction
    }

    parsers(118) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        FNEG // instance of the instruction
    }

    parsers(114) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        FREM // instance of the instruction
    }

    parsers(174) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        FRETURN // instance of the instruction
    }

    parsers(56) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        if (WIDE == previousInstruction) {
            FSTORE(in.readUnsignedShort)
        }
        else {
            FSTORE(in.readUnsignedByte)
        }
    }

    parsers(67) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        FSTORE_0 // instance of the instruction
    }

    parsers(68) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        FSTORE_1 // instance of the instruction
    }

    parsers(69) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        FSTORE_2 // instance of the instruction
    }

    parsers(70) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        FSTORE_3 // instance of the instruction
    }

    parsers(102) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        FSUB // instance of the instruction
    }

    parsers(180) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        val (declaringClass, name, fieldType) /*: (ObjectType,String,FieldType)*/ = cp(in.readUnsignedShort).asFieldref(cp) // fieldref
        GETFIELD(declaringClass, name, fieldType)
    }

    parsers(178) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        val (declaringClass, name, fieldType) /*: (ObjectType,String,FieldType)*/ = cp(in.readUnsignedShort).asFieldref(cp) // fieldref
        GETSTATIC(declaringClass, name, fieldType)
    }

    parsers(167) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        GOTO(in.readShort /* branchoffset */ )
    }

    parsers(200) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        GOTO_W(in.readInt /* branchoffset */ )
    }

    parsers(145) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        I2B // instance of the instruction
    }

    parsers(146) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        I2C // instance of the instruction
    }

    parsers(135) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        I2D // instance of the instruction
    }

    parsers(134) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        I2F // instance of the instruction
    }

    parsers(133) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        I2L // instance of the instruction
    }

    parsers(147) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        I2S // instance of the instruction
    }

    parsers(96) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        IADD // instance of the instruction
    }

    parsers(46) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        IALOAD // instance of the instruction
    }

    parsers(126) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        IAND // instance of the instruction
    }

    parsers(79) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        IASTORE // instance of the instruction
    }

    parsers(2) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        ICONST_M1 // instance of the instruction
    }

    parsers(3) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        ICONST_0 // instance of the instruction
    }

    parsers(4) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        ICONST_1 // instance of the instruction
    }

    parsers(5) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        ICONST_2 // instance of the instruction
    }

    parsers(6) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        ICONST_3 // instance of the instruction
    }

    parsers(7) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        ICONST_4 // instance of the instruction
    }

    parsers(8) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        ICONST_5 // instance of the instruction
    }

    parsers(108) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        IDIV // instance of the instruction
    }

    parsers(165) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // branchoffset
        IF_ACMPEQ(in.readShort)
    }

    parsers(166) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // branchoffset
        IF_ACMPNE(in.readShort)
    }

    parsers(159) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // branchoffset
        IF_ICMPEQ(in.readShort)
    }

    parsers(160) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        val branchoffset = in.readShort
        IF_ICMPNE(branchoffset)
    }

    parsers(161) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // branchoffset
        IF_ICMPLT(in.readShort)
    }

    parsers(162) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // branchoffset
        IF_ICMPGE(in.readShort)
    }

    parsers(163) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // branchoffset
        IF_ICMPGT(in.readShort)
    }

    parsers(164) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // branchoffset
        IF_ICMPLE(in.readShort)
    }

    parsers(153) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // branchoffset
        IFEQ(in.readShort)
    }

    parsers(154) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // branchoffset
        IFNE(in.readShort)
    }

    parsers(155) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // branchoffset
        IFLT(in.readShort)
    }

    parsers(156) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // branchoffset
        IFGE(in.readShort)
    }

    parsers(157) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // branchoffset
        IFGT(in.readShort)
    }

    parsers(158) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // branchoffset
        IFLE(in.readShort)
    }

    parsers(199) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // branchoffset      
        IFNONNULL(in.readShort)
    }

    parsers(198) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // branchoffset
        IFNULL(in.readShort)
    }

    parsers(132) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        if (WIDE == previousInstruction) {
            val lvIndex = in.readUnsignedShort
            val constValue = in.readShort
            IINC(lvIndex, constValue)
        }
        else {
            val lvIndex = in.readUnsignedByte
            val constValue = in.readByte
            IINC(lvIndex, constValue)
        }
    }

    parsers(21) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        if (WIDE == previousInstruction) {
            ILOAD(in.readUnsignedShort)
        }
        else {
            ILOAD(in.readUnsignedByte)
        }
    }

    parsers(26) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        ILOAD_0 // instance of the instruction
    }

    parsers(27) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        ILOAD_1 // instance of the instruction
    }

    parsers(28) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        ILOAD_2 // instance of the instruction
    }

    parsers(29) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        ILOAD_3 // instance of the instruction
    }

    parsers(104) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        IMUL // instance of the instruction
    }

    parsers(116) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        INEG // instance of the instruction
    }

    parsers(193) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // referenceType
        val cv: ConstantValue[_] = cp(in.readUnsignedShort).asConstantValue(cp)
        INSTANCEOF(cv.toClass)
    }

    parsers(186) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        /* TODO [Java 7] "invokedynamic" - resolve index into bootstrap method attribute table. */
        val (name, methodDescriptor) /*: (String, MethodDescriptor)*/ = cp(in.readUnsignedShort).asNameAndMethodDescriptor(cp) // callSiteSpecifier
        in.readByte // ignored; fixed value
        in.readByte // ignored; fixed value
        INVOKEDYNAMIC( /* TODO [Java 7] "invokedynamic" - resolve valid index into the bootstrap_methods array of the bootstrap method table */ name, methodDescriptor)
    }

    parsers(185) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        val (declaringClass, name, methodDescriptor) /*: (ReferenceType,String,MethodDescriptor)*/ = cp(in.readUnsignedShort).asMethodref(cp) // methodRef
        in.readByte // ignored; fixed value
        in.readByte // ignored; fixed value
        INVOKEINTERFACE(declaringClass, name, methodDescriptor)
    }

    parsers(183) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        //        val (declaringClass, name, methodDescriptor) /*: (ReferenceType,String,MethodDescriptor)*/ = cp(in.readUnsignedShort).asMethodref(cp) // methodRef
        //        INVOKESPECIAL(declaringClass, name, methodDescriptor)
        cp(in.readUnsignedShort).asInvoke(INVOKESPECIAL)(cp)
    }

    parsers(184) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        val (declaringClass, name, methodDescriptor) /*: (ReferenceType,String,MethodDescriptor)*/ = cp(in.readUnsignedShort).asMethodref(cp) // methodRef
        INVOKESTATIC(declaringClass, name, methodDescriptor)
    }

    parsers(182) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        val (declaringClass, name, methodDescriptor) /*: (ReferenceType,String,MethodDescriptor)*/ = cp(in.readUnsignedShort).asMethodref(cp) // methodRef
        INVOKEVIRTUAL(declaringClass, name, methodDescriptor)
    }

    parsers(128) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        IOR // instance of the instruction
    }

    parsers(112) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        IREM // instance of the instruction
    }

    parsers(172) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        IRETURN // instance of the instruction
    }

    parsers(120) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        ISHL // instance of the instruction
    }

    parsers(122) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        ISHR // instance of the instruction
    }

    parsers(54) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        if (WIDE == previousInstruction) {
            ISTORE(in.readUnsignedShort)
        }
        else {
            ISTORE(in.readUnsignedByte)
        }
    }

    parsers(59) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        ISTORE_0 // instance of the instruction
    }

    parsers(60) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        ISTORE_1 // instance of the instruction
    }

    parsers(61) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        ISTORE_2 // instance of the instruction
    }

    parsers(62) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        ISTORE_3 // instance of the instruction
    }

    parsers(100) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        ISUB // instance of the instruction
    }

    parsers(124) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        IUSHR // instance of the instruction
    }

    parsers(130) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        IXOR // instance of the instruction
    }

    parsers(168) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // branchoffset
        JSR(in.readShort)
    }

    parsers(201) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // branchoffset
        JSR_W(in.readInt)
    }

    parsers(138) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        L2D // instance of the instruction
    }

    parsers(137) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        L2F // instance of the instruction
    }

    parsers(136) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        L2I // instance of the instruction
    }

    parsers(97) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        LADD // instance of the instruction
    }

    parsers(47) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        LALOAD // instance of the instruction
    }

    parsers(127) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        LAND // instance of the instruction
    }

    parsers(80) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        LASTORE // instance of the instruction
    }

    parsers(148) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        LCMP // instance of the instruction
    }

    parsers(9) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        LCONST_0 // instance of the instruction
    }

    parsers(10) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        LCONST_1 // instance of the instruction
    }

    parsers(18) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        LDC(cp(in.readUnsignedByte).asConstantValue(cp))
    }

    parsers(19) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        LDC_W(cp(in.readUnsignedShort).asConstantValue(cp))
    }

    parsers(20) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        LDC2_W(cp(in.readUnsignedShort).asConstantValue(cp))
    }

    parsers(109) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        LDIV // instance of the instruction
    }

    parsers(22) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        if (WIDE == previousInstruction) {
            val lvIndex = in.readUnsignedShort
            LLOAD(lvIndex)
        }
        else {
            val lvIndex = in.readUnsignedByte
            LLOAD(lvIndex)
        }
    }

    parsers(30) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        LLOAD_0 // instance of the instruction
    }

    parsers(31) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        LLOAD_1 // instance of the instruction
    }

    parsers(32) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        LLOAD_2 // instance of the instruction
    }

    parsers(33) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        LLOAD_3 // instance of the instruction
    }

    parsers(105) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        LMUL // instance of the instruction
    }

    parsers(117) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        LNEG // instance of the instruction
    }

    parsers(171) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        in.skip(3 - (index % 4)) // skip padding bytes
        val defaultOffset = in.readInt
        val npairsCount = in.readInt
        val npairs: IndexedSeq[(Int, Int)] = repeat(npairsCount) { (in.readInt, in.readInt) }
        LOOKUPSWITCH(defaultOffset, npairsCount, npairs)
    }

    parsers(129) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        LOR // instance of the instruction
    }

    parsers(113) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        LREM // instance of the instruction
    }

    parsers(173) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        LRETURN // instance of the instruction
    }

    parsers(121) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        LSHL // instance of the instruction
    }

    parsers(123) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        LSHR // instance of the instruction
    }

    parsers(55) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        if (WIDE == previousInstruction) {
            val lvIndex = in.readUnsignedShort
            LSTORE(lvIndex)
        }
        else {
            val lvIndex = in.readUnsignedByte
            LSTORE(lvIndex)
        }
    }

    parsers(63) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        LSTORE_0 // instance of the instruction
    }

    parsers(64) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        LSTORE_1 // instance of the instruction
    }

    parsers(65) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        LSTORE_2 // instance of the instruction
    }

    parsers(66) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        LSTORE_3 // instance of the instruction
    }

    parsers(101) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        LSUB // instance of the instruction
    }

    parsers(125) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        LUSHR // instance of the instruction
    }

    parsers(131) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        LXOR // instance of the instruction
    }

    parsers(194) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        MONITORENTER // instance of the instruction
    }

    parsers(195) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        MONITOREXIT // instance of the instruction
    }

    parsers(197) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        val cv: ConstantValue[_] = cp(in.readUnsignedShort).asConstantValue(cp)
        MULTIANEWARRAY(cv.toClass /* componentType */ , in.readUnsignedByte /* dimensions */ )
    }

    parsers(187) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        NEW(cp(in.readUnsignedShort).asObjectType(cp))
    }

    parsers(188) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        NEWARRAY(in.readByte)
    }

    parsers(0) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        NOP // instance of the instruction
    }

    parsers(87) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        POP // instance of the instruction
    }

    parsers(88) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        POP2 // instance of the instruction
    }

    parsers(181) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        val (declaringClass, name, fieldType) /*: (ObjectType,String,FieldType)*/ = cp(in.readUnsignedShort).asFieldref(cp) // fieldref
        PUTFIELD(declaringClass, name, fieldType)
    }

    parsers(179) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        val (declaringClass, name, fieldType) /*: (ObjectType,String,FieldType)*/ = cp(in.readUnsignedShort).asFieldref(cp) // fieldref
        PUTSTATIC(declaringClass, name, fieldType)
    }

    parsers(169) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        if (WIDE == previousInstruction) {
            val lvIndex = in.readUnsignedShort
            RET(lvIndex)
        }
        else {
            val lvIndex = in.readUnsignedByte
            RET(lvIndex)
        }
    }

    parsers(177) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        RETURN // instance of the instruction
    }

    parsers(53) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        SALOAD // instance of the instruction
    }

    parsers(86) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        SASTORE // instance of the instruction
    }

    parsers(17) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        SIPUSH(in.readShort /* value */ )
    }

    parsers(95) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        SWAP // instance of the instruction
    }

    parsers(170) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        in.skip(3 - (index % 4)) // skip padding bytes
        val defaultOffset = in.readInt
        val low = in.readInt
        val high = in.readInt
        val jumpOffsets: IndexedSeq[Int] = repeat(high - low + 1) { in.readInt }
        TABLESWITCH(defaultOffset, low, high, jumpOffsets)
    }

    parsers(196) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        WIDE // instance of the instruction
    }

}
