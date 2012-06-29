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
        val instructions = new Array[Instruction](source.size)
        var previousInstruction: Instruction = null
        while (in.available > 0) {
            val index = source.length - in.available
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
            // lvIndex:
            val p1 = in.readUnsignedShort
            ALOAD(p1)
        }
        else {
            // lvIndex:
            val p1 = in.readUnsignedByte
            ALOAD(p1)
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
        val p1 = cv.toClass
        ANEWARRAY(p1)
    }

    parsers(176) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        ARETURN // instance of the instruction
    }

    parsers(190) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        ARRAYLENGTH // instance of the instruction
    }

    parsers(58) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        if (WIDE == previousInstruction) {
            // lvIndex:
            val p1 = in.readUnsignedShort
            ASTORE(p1)
        }
        else {
            // lvIndex:
            val p1 = in.readUnsignedByte
            ASTORE(p1)
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
        // value:
        val p1 = in.readByte
        BIPUSH(p1)
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
        val p1 = cv.toClass
        CHECKCAST(p1)
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
            // lvIndex:
            val p1 = in.readUnsignedShort
            DLOAD(p1)
        }
        else {
            // lvIndex:
            val p1 = in.readUnsignedByte
            DLOAD(p1)
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
            // lvIndex:
            val p1 = in.readUnsignedShort
            DSTORE(p1)
        }
        else {
            // lvIndex:
            val p1 = in.readUnsignedByte
            DSTORE(p1)
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
            // lvIndex:
            val p1 = in.readUnsignedShort
            FLOAD(p1)
        }
        else {
            // lvIndex:
            val p1 = in.readUnsignedByte
            FLOAD(p1)
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
            // lvIndex:
            val p1 = in.readUnsignedShort
            FSTORE(p1)
        }
        else {
            // lvIndex:
            val p1 = in.readUnsignedByte
            FSTORE(p1)
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
        val p1 = declaringClass
        val p2 = name
        val p3 = fieldType
        GETFIELD(p1, p2, p3)
    }

    parsers(178) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        val (declaringClass, name, fieldType) /*: (ObjectType,String,FieldType)*/ = cp(in.readUnsignedShort).asFieldref(cp) // fieldref
        val p1 = declaringClass
        val p2 = name
        val p3 = fieldType
        GETSTATIC(p1, p2, p3)
    }

    parsers(167) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // branchoffset:
        val p1 = in.readShort // GOTO
        GOTO(p1)
    }

    parsers(200) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // branchoffset:
        val p1 = in.readInt // GOTO_W
        GOTO_W(p1)
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
        // branchoffset:
        val p1 = in.readShort
        IF_ACMPEQ(p1)
    }

    parsers(166) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // branchoffset:
        val p1 = in.readShort
        IF_ACMPNE(p1)
    }

    parsers(159) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // branchoffset:
        val p1 = in.readShort
        IF_ICMPEQ(p1)
    }

    parsers(160) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // branchoffset:
        val p1 = in.readShort
        IF_ICMPNE(p1)
    }

    parsers(161) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // branchoffset:
        val p1 = in.readShort
        IF_ICMPLT(p1)
    }

    parsers(162) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // branchoffset:
        val p1 = in.readShort
        IF_ICMPGE(p1)
    }

    parsers(163) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // branchoffset:
        val p1 = in.readShort
        IF_ICMPGT(p1)
    }

    parsers(164) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // branchoffset:
        val p1 = in.readShort
        IF_ICMPLE(p1)

    }

    parsers(153) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // branchoffset:
        val p1 = in.readShort
        IFEQ(p1)
    }

    parsers(154) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // branchoffset:
        val p1 = in.readShort
        IFNE(p1)
    }

    parsers(155) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // branchoffset:
        val p1 = in.readShort
        IFLT(p1)

    }

    parsers(156) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // branchoffset:
        val p1 = in.readShort
        IFGE(p1)
    }

    parsers(157) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // branchoffset:
        val p1 = in.readShort
        IFGT(p1)
    }

    parsers(158) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // branchoffset:
        val p1 = in.readShort
        IFLE(p1)
    }

    parsers(199) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // branchoffset:
        val p1 = in.readShort
        IFNONNULL(p1)
    }

    parsers(198) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // branchoffset:
        val p1 = in.readShort
        IFNULL(p1)
    }

    parsers(132) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        if (WIDE == previousInstruction) {
            // lvIndex:
            val p1 = in.readUnsignedShort
            // constValue:
            val p2 = in.readShort
            IINC(p1, p2)
        }
        else {
            // lvIndex:
            val p1 = in.readUnsignedByte
            // constValue:
            val p2 = in.readByte
            IINC(p1, p2)
        }
    }

    parsers(21) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        if (WIDE == previousInstruction) {
            // lvIndex:
            val p1 = in.readUnsignedShort
            ILOAD(p1)
        }
        else {
            // lvIndex:
            val p1 = in.readUnsignedByte
            ILOAD(p1)
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

        {
            // referenceType
            val cv: ConstantValue[_] = cp(in.readUnsignedShort).asConstantValue(cp)
            val p1 = cv.toClass
            INSTANCEOF(p1)

        }

    }

    parsers(186) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
            /* TODO [Java 7] "invokedynamic" - resolve index into bootstrap method attribute table. */
            val (name, methodDescriptor) /*: (String, MethodDescriptor)*/ = cp(in.readUnsignedShort).asNameAndMethodDescriptor(cp) // callSiteSpecifier
            val p1: String = name
            val p2: MethodDescriptor = methodDescriptor

            in.readByte // ignored; fixed value
            in.readByte // ignored; fixed value
            INVOKEDYNAMIC( /* TODO [Java 7] "invokedynamic" - resolve valid index into the bootstrap_methods array of the bootstrap method table */ p1, p2)
    }

    parsers(185) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        val (declaringClass, name, methodDescriptor) /*: (ReferenceType,String,MethodDescriptor)*/ = cp(in.readUnsignedShort).asMethodref(cp) // methodRef
        val p1 = declaringClass
        val p2 = name
        val p3 = methodDescriptor

        in.readByte // ignored; fixed value
        in.readByte // ignored; fixed value

        INVOKEINTERFACE(p1, p2, p3)
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
            // lvIndex:
            val p1 = in.readUnsignedShort
            ISTORE(p1)
        }
        else {
            // lvIndex:
            val p1 = in.readUnsignedByte
            ISTORE(p1)
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
        // branchoffset:
        val p1 = in.readShort
        JSR(p1)
    }

    parsers(201) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // branchoffset:
        val p1 = in.readInt
        JSR_W(p1)
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
        // constantValue
        val p1: ConstantValue[_] = cp(in.readUnsignedByte).asConstantValue(cp)
        LDC(p1)
    }

    parsers(19) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // constantValue
        val p1: ConstantValue[_] = cp(in.readUnsignedShort).asConstantValue(cp)
        LDC_W(p1)
    }

    parsers(20) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // constantValue
        val p1: ConstantValue[_] = cp(in.readUnsignedShort).asConstantValue(cp)
        LDC2_W(p1)
    }

    parsers(109) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        LDIV // instance of the instruction
    }

    parsers(22) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        if (WIDE == previousInstruction) {
            // lvIndex:
            val p1 = in.readUnsignedShort
            LLOAD(p1)
        }
        else {
            // lvIndex:
            val p1 = in.readUnsignedByte
            LLOAD(p1)
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
        // defaultOffset:
        val p1 = in.readInt
        // npairsCount:
        val p2 = in.readInt
        // npairs
        val p3: IndexedSeq[(Int, Int)] = repeat(p2) {
            (in.readInt, in.readInt)
        }
        LOOKUPSWITCH(p1, p2, p3)
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
            // lvIndex:
            val p1 = in.readUnsignedShort
            LSTORE(p1)
        }
        else {
            // lvIndex:
            val p1 = in.readUnsignedByte
            LSTORE(p1)
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

        // componentType
        val cv: ConstantValue[_] = cp(in.readUnsignedShort).asConstantValue(cp)
        val p1 = cv.toClass
        // dimensions:
        val p2 = in.readUnsignedByte
        MULTIANEWARRAY(p1, p2)
    }

    parsers(187) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // objectType
        val p1 = cp(in.readUnsignedShort).asObjectType(cp)
        NEW(p1)
    }

    parsers(188) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        // atype:
        val p1 = in.readByte
        NEWARRAY(p1)
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
        val p1 = declaringClass
        val p2 = name
        val p3 = fieldType
        PUTFIELD(p1, p2, p3)
    }

    parsers(179) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        val (declaringClass, name, fieldType) /*: (ObjectType,String,FieldType)*/ = cp(in.readUnsignedShort).asFieldref(cp) // fieldref
        val p1 = declaringClass
        val p2 = name
        val p3 = fieldType
        PUTSTATIC(p1, p2, p3)
    }

    parsers(169) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        if (WIDE == previousInstruction) {
            // lvIndex:
            val p1 = in.readUnsignedShort
            RET(p1)
        }
        else {
            // lvIndex:
            val p1 = in.readUnsignedByte
            RET(p1)
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

        // value:
        val p1 = in.readShort
        SIPUSH(p1)
    }

    parsers(95) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        SWAP // instance of the instruction
    }

    parsers(170) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        in.skip(3 - (index % 4)) // skip padding bytes
        // defaultOffset:
        val p1 = in.readInt
        // low:
        val p2 = in.readInt
        // high:
        val p3 = in.readInt
        // jumpOffsets
        val p4: IndexedSeq[Int] = repeat(p3 - p2 + 1) {
            in.readInt
        }
        TABLESWITCH(p1, p2, p3, p4)
    }

    parsers(196) = (previousInstruction: Instruction, index: Int, in: DataInputStream, cp: Constant_Pool) ⇒ {
        WIDE // instance of the instruction
    }

}
