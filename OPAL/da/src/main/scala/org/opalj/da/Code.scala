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
package da

import scala.xml.Node
import scala.xml.Text
import scala.util.Random

/**
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 */
case class Code(instructions: Array[Byte]) {

    def toXHTML(
        methodIndex: Int,
        exceptionTable: IndexedSeq[ExceptionTableEntry],
        lineNumberTable: Option[Seq[LineNumberTableEntry]])(
            implicit cp: Constant_Pool): Node = {

        val instructions = InstructionsToXHTML(methodIndex, this.instructions)
        ExceptionsLinking(instructions, exceptionTable)

        <table class="method_bytecode">
            <tr>
                <th class="pc">PC</th>
                {
                    lineNumberTable.map { _ ⇒
                        <th class="line">Line</th>
                    }.getOrElse {
                        Seq.empty
                    }
                }
                <th>Instruction</th>
            </tr>
            { // One instruction per row
                for {
                    pc ← (0 until instructions.length)
                    if instructions(pc) != null
                } yield {
                    createTableRowForInstruction(
                        methodIndex, instructions(pc), pc, lineNumberTable)
                }
            }
        </table>
    }

    private[this] def id(methodIndex: Int, pc: Int): String = s"m${methodIndex}_pc$pc"

    private[this] def createTableRowForInstruction(
        methodIndex: Int,
        instruction: Node,
        pc: Int,
        lineNumberTable: Option[Seq[LineNumberTableEntry]]): Node = {

        <tr>
            <td class="pc" id={ id(methodIndex, pc) }>{ pc }</td>
            {
                lineNumberTable.map { lineNumberTable ⇒
                    val ln = lineNumberTable.find(e ⇒ e.start_pc == pc).map(_.line_number)
                    <td class="line">{ ln.getOrElse("|") }</td>
                }.getOrElse {
                    Seq.empty
                }
            }
            <td> { instruction }</td>
        </tr>
    }

    private[this] def InstructionsToXHTML(
        methodIndex: Int,
        source: Array[Byte])(
            implicit cp: Constant_Pool): Array[Node] = {
        import java.io.DataInputStream
        import java.io.ByteArrayInputStream
        val bas = new ByteArrayInputStream(source)
        val in = new DataInputStream(bas)
        val codeLength = source.size
        val instructions = new Array[Node](codeLength)

        var wide: Boolean = false

        def lvIndex(): Int =
            if (wide) {
                wide = false
                in.readUnsignedShort
            } else {
                in.readUnsignedByte
            }

        def ifToString(mnemonic: String, pc: Int): Node = {
            val targetPC = in.readShort + pc
            val targetID = "#"+id(methodIndex, targetPC)
            <span>
                <span class={ "instruction "+mnemonic }>{ mnemonic }</span>
                <a href={ targetID } class="pc">{ targetPC }</a>
            </span>
        }

        while (in.available > 0) {
            val pc = codeLength - in.available
            instructions(pc) = (in.readUnsignedByte: @scala.annotation.switch) match {
                case 50 ⇒ <span class="instruction aaload">aaload</span>
                case 83 ⇒ <span class="instruction aastore">aastore</span>
                case 1  ⇒ <span class="instruction aconst_null">aconst_null</span>
                case 25 ⇒
                    <span>
                        <span class="instruction aload">aload </span>
                        { lvIndex() }
                    </span>
                case 42 ⇒ <span class="instruction aload_0">aload_0</span>
                case 43 ⇒ <span class="instruction aload_1">aload_1</span>
                case 44 ⇒ <span class="instruction aload_2">aload_2</span>
                case 45 ⇒ <span class="instruction aload_3">aload_3</span>
                case 189 ⇒
                    <span>
                        <span class="instruction anewarray">anewarray </span>
                        { asObjectType(in.readUnsignedShort) }
                    </span>
                case 176 ⇒ <span class="instruction areturn">areturn</span>
                case 190 ⇒ <span class="instruction arraylength">arraylength</span>
                case 58 ⇒
                    <span>
                        <span class="instruction astore">astore </span>{ lvIndex }
                    </span>
                case 75  ⇒ <span class="instruction astore_0">astore_0</span>
                case 76  ⇒ <span class="instruction astore_1">astore_1</span>
                case 77  ⇒ <span class="instruction astore_2">astore_2</span>
                case 78  ⇒ <span class="instruction astore_3">astore_3</span>
                case 191 ⇒ <span class="instruction athrow">athrow</span>
                case 51  ⇒ <span class="instruction baload">baload</span>
                case 84  ⇒ <span class="instruction bastore">bastore</span>
                case 16 ⇒
                    <span>
                        <span class="instruction bipush">bipush </span>{ in.readByte }
                    </span>
                case 52 ⇒ <span class="instruction caload">caload</span>
                case 85 ⇒ <span class="instruction castore">castore</span>
                case 192 ⇒
                    <span>
                        <span class="instruction checkcast">checkcast </span>
                        { asReferenceType(in.readUnsignedShort) }
                    </span>
                case 144 ⇒ <span class="instruction d2f">d2f</span>
                case 142 ⇒ <span class="instruction d2i">d2i</span>
                case 143 ⇒ <span class="instruction d2l">d2l</span>
                case 99  ⇒ <span class="instruction dadd">dadd</span>
                case 49  ⇒ <span class="instruction daload">daload</span>
                case 82  ⇒ <span class="instruction dastore">dastore</span>
                case 152 ⇒ <span class="instruction dcmpg">dcmpg</span>
                case 151 ⇒ <span class="instruction dcmpl">dcmpl</span>
                case 14  ⇒ <span class="instruction dconst_0">dconst_0</span>
                case 15  ⇒ <span class="instruction dconst_1">dconst_1</span>
                case 111 ⇒ <span class="instruction ddiv">ddiv </span>
                case 24 ⇒
                    <span>
                        <span class="instruction dload">dload </span>{ lvIndex }
                    </span>
                case 38  ⇒ <span class="instruction dload_0">dload_0</span>
                case 39  ⇒ <span class="instruction dload_1">dload_1</span>
                case 40  ⇒ <span class="instruction dload_2">dload_2</span>
                case 41  ⇒ <span class="instruction dload_3">dload_3</span>
                case 107 ⇒ <span class="instruction dmul">dmul</span>
                case 119 ⇒ <span class="instruction dneg">dneg</span>
                case 115 ⇒ <span class="instruction drem">drem</span>
                case 175 ⇒ <span class="instruction dreturn">dreturn</span>
                case 57 ⇒
                    <span>
                        <span class="instruction dstore">dstore </span>{ lvIndex }
                    </span>
                case 71  ⇒ <span class="instruction dstore_0">dstore_0</span>
                case 72  ⇒ <span class="instruction dstore_1">dstore_1</span>
                case 73  ⇒ <span class="instruction dstore_2">dstore_2</span>
                case 74  ⇒ <span class="instruction dstore_3">dstore_3</span>
                case 103 ⇒ <span class="instruction dsub">dsub</span>
                case 89  ⇒ <span class="instruction dup">dup</span>
                case 90  ⇒ <span class="instruction dup_x1">dup_x1</span>
                case 91  ⇒ <span class="instruction dup_x2">dup_x2</span>
                case 92  ⇒ <span class="instruction dup2">dup2</span>
                case 93  ⇒ <span class="instruction dup2_x1">dup2_x1</span>
                case 94  ⇒ <span class="instruction dup2_x2">dup2_x2</span>
                case 141 ⇒ <span class="instruction f2d">f2d</span>
                case 139 ⇒ <span class="instruction f2i">f2i</span>
                case 140 ⇒ <span class="instruction f2l">f2l</span>
                case 98  ⇒ <span class="instruction fadd">fadd</span>
                case 48  ⇒ <span class="instruction faload">faload</span>
                case 81  ⇒ <span class="instruction fastore">fastore</span>
                case 150 ⇒ <span class="instruction fcmpg">fcmpg</span>
                case 149 ⇒ <span class="instruction fcmpl">fcmpl</span>
                case 11  ⇒ <span class="instruction fconst_0">fconst_0</span>
                case 12  ⇒ <span class="instruction fconst_1">fconst_1</span>
                case 13  ⇒ <span class="instruction fconst_2">fconst_2</span>
                case 110 ⇒ <span class="instruction fdiv">fdiv</span>
                case 23 ⇒
                    <span>
                        <span class="instruction fload">fload </span>{ lvIndex }
                    </span>
                case 34  ⇒ <span class="instruction fload_0">fload_0</span>
                case 35  ⇒ <span class="instruction fload_1">fload_1</span>
                case 36  ⇒ <span class="instruction fload_2">fload_2</span>
                case 37  ⇒ <span class="instruction fload_3">fload_3</span>
                case 106 ⇒ <span class="instruction fmul">fmul</span>
                case 118 ⇒ <span class="instruction fneg">fneg</span>
                case 114 ⇒ <span class="instruction frem">frem</span>
                case 174 ⇒ <span class="instruction freturn">freturn</span>
                case 56 ⇒
                    <span>
                        <span class="instruction fstore">fstore </span>{ lvIndex }
                    </span>
                case 67  ⇒ <span class="instruction fstore_0">fstore_0</span>
                case 68  ⇒ <span class="instruction fstore_1">fstore_1</span>
                case 69  ⇒ <span class="instruction fstore_2">fstore_2</span>
                case 70  ⇒ <span class="instruction fstore_3">fstore_3</span>
                case 102 ⇒ <span class="instruction fsub">fsub</span>
                case 180 ⇒
                    <span>
                        <span class="instruction getfield">getfield </span>
                        { val c = in.readUnsignedShort; cp(c).toString(cp)+" ["+c+"]" }
                    </span>
                case 178 ⇒
                    <span>
                        <span class="instruction getstatic">getstatic </span>
                        { val c = in.readUnsignedShort; cp(c).toString(cp)+" ["+c+"]" }
                    </span>
                case 167 ⇒
                    val targetPC = in.readShort + pc
                    val targetID = "#"+id(methodIndex, targetPC)
                    <span>
                        <span class="instruction goto">goto </span>
                        <a href={ targetID } class="pc">{ targetPC }</a>
                    </span>
                case 200 ⇒
                    val targetPC = in.readInt + pc
                    val targetID = "#"+id(methodIndex, targetPC)
                    <span>
                        <span class="instruction goto_w">goto_w </span>
                        <a href={ targetID } class="pc">{ targetPC }</a>
                    </span>
                case 145 ⇒ <span class="instruction i2b">i2b</span>
                case 146 ⇒ <span class="instruction i2c">i2c</span>
                case 135 ⇒ <span class="instruction i2d">i2d</span>
                case 134 ⇒ <span class="instruction i2f">i2f</span>
                case 133 ⇒ <span class="instruction i2l">i2l</span>
                case 147 ⇒ <span class="instruction i2s">i2s</span>
                case 96  ⇒ <span class="instruction iadd">iadd</span>
                case 46  ⇒ <span class="instruction iaload">iaload</span>
                case 126 ⇒ <span class="instruction iand">iand</span>
                case 79  ⇒ <span class="instruction iastore">iastore</span>
                case 2   ⇒ <span class="instruction iconst_m1">iconst_m1</span>
                case 3   ⇒ <span class="instruction iconst_0">iconst_0</span>
                case 4   ⇒ <span class="instruction iconst_1">iconst_1</span>
                case 5   ⇒ <span class="instruction iconst_2">iconst_2</span>
                case 6   ⇒ <span class="instruction iconst_3">iconst_3</span>
                case 7   ⇒ <span class="instruction iconst_4">iconst_4</span>
                case 8   ⇒ <span class="instruction iconst_5">iconst_5</span>
                case 108 ⇒ <span class="instruction idiv">idiv</span>
                case 165 ⇒
                    ifToString("if_acmpeq", pc)
                case 166 ⇒
                    ifToString("if_acmpne", pc)
                case 159 ⇒
                    ifToString("if_icmpeq", pc)
                case 160 ⇒
                    ifToString("if_icmpne", pc)
                case 161 ⇒
                    ifToString("if_icmplt", pc)
                case 162 ⇒
                    ifToString("if_icmpge", pc)
                case 163 ⇒
                    ifToString("if_icmpgt", pc)
                case 164 ⇒
                    ifToString("if_icmple", pc)
                case 153 ⇒
                    ifToString("ifeq", pc)
                case 154 ⇒
                    ifToString("ifeq", pc)
                case 155 ⇒
                    ifToString("iflt", pc)
                case 156 ⇒
                    ifToString("ifge", pc)
                case 157 ⇒
                    ifToString("ifgt", pc)
                case 158 ⇒
                    ifToString("ifle", pc)
                case 199 ⇒
                    ifToString("ifnonnull", pc)
                case 198 ⇒
                    ifToString("ifnull", pc)
                case 132 ⇒
                    val params = if (wide) {
                        wide = false
                        val lvIndex = in.readUnsignedShort
                        val constValue = in.readShort
                        lvIndex+" "+constValue
                    } else {
                        val lvIndex = in.readUnsignedByte
                        val constValue = in.readByte
                        lvIndex+" "+constValue
                    }
                    <span class="instruction iinc">iinc { params }</span>
                case 21 ⇒
                    <span>
                        <span class="instruction iload">iload </span>{ lvIndex }
                    </span>
                case 26  ⇒ <span class="instruction iload_0">iload_0</span>
                case 27  ⇒ <span class="instruction iload_1">iload_1</span>
                case 28  ⇒ <span class="instruction iload_2">iload_2</span>
                case 29  ⇒ <span class="instruction iload_3">iload_3</span>
                case 104 ⇒ <span class="instruction imul">imul</span>
                case 116 ⇒ <span class="instruction ineg">ineg</span>
                case 193 ⇒
                    val referenceType = cp(in.readUnsignedShort).toString(cp)
                    <span>
                        <span class="instruction instanceof">instanceof </span>
                        { referenceType }
                    </span>
                case 186 ⇒
                    val c = in.readUnsignedShort
                    in.readByte // ignored; fixed value
                    in.readByte // ignored; fixed value
                    val signature = cp(c).toString(cp)
                    <span>
                        <span class="instruction invokedynamic">invokedynamic </span>
                        { signature }
                    </span>
                case 185 ⇒
                    val c = in.readUnsignedShort
                    in.readByte // ignored; fixed value
                    in.readByte // ignored; fixed value
                    val signature = cp(c).toString(cp)+" ["+c+"]"
                    <span>
                        <span class="instruction invokeinterface">invokeinterface </span>
                        { signature }
                    </span>
                case 183 ⇒
                    val c = in.readUnsignedShort
                    val signature = cp(c).toString(cp)+" ["+c+"]"
                    <span>
                        <span class="instruction invokespecial">invokespecial </span>
                        { signature }
                    </span>
                case 184 ⇒
                    val c = in.readUnsignedShort
                    val signature = cp(c).toString(cp)+" ["+c+"]"
                    <span>
                        <span class="instruction invokespecial">invokestatic  </span>
                        { signature }
                    </span>
                case 182 ⇒
                    val c = in.readUnsignedShort
                    val signature = cp(c).toString(cp)+" ["+c+"]"
                    <span>
                        <span class="instruction invokevirtual">invokevirtual </span>
                        { signature }
                    </span>
                case 128 ⇒ <span class="instruction ior">ior</span>
                case 112 ⇒ <span class="instruction irem">irem</span>
                case 172 ⇒ <span class="instruction ireturn">ireturn</span>
                case 120 ⇒ <span class="instruction ishl">ishl</span>
                case 122 ⇒ <span class="instruction ishr">ishr</span>
                case 54  ⇒ <span><span class="instruction istore">istore </span>{ lvIndex }</span>
                case 59  ⇒ <span class="instruction istore_0">istore_0</span>
                case 60  ⇒ <span class="instruction istore_1">istore_1</span>
                case 61  ⇒ <span class="instruction istore_2">istore_2</span>
                case 62  ⇒ <span class="instruction istore_3">istore_3</span>
                case 100 ⇒ <span class="instruction isub">isub</span>
                case 124 ⇒ <span class="instruction iushr">iushr</span>
                case 130 ⇒ <span class="instruction ixor">ixor</span>
                case 168 ⇒ <span><span class="instruction jsr">jsr </span>{ in.readShort + pc }</span>
                case 201 ⇒ <span><span class="instruction jsr_w">jsr_w </span>{ in.readInt + pc }</span>
                case 138 ⇒ <span class="instruction l2d">l2d</span>
                case 137 ⇒ <span class="instruction l2d">l2f</span>
                case 136 ⇒ <span class="instruction l2i">l2i</span>
                case 97  ⇒ <span class="instruction ladd">ladd</span>
                case 47  ⇒ <span class="instruction laload">laload</span>
                case 127 ⇒ <span class="instruction land">land</span>
                case 80  ⇒ <span class="instruction lastore">lastore</span>
                case 148 ⇒ <span class="instruction lcmp">lcmp</span>
                case 9   ⇒ <span class="instruction lconst_0">lconst_0</span>
                case 10  ⇒ <span class="instruction lconst_1">lconst_1</span>
                case 18 ⇒
                    val constantValue = cp(in.readUnsignedByte()).toLDCString(cp)
                    <span><span class="instruction ldc">ldc </span>{ constantValue }</span>
                case 19 ⇒
                    val constantValue = cp(in.readUnsignedShort).toLDCString(cp)
                    <span><span class="instruction ldc_w">ldc_w </span>{ constantValue }</span>
                case 20 ⇒
                    val constantValue = cp(in.readUnsignedShort).toLDCString(cp)
                    <span><span class="instruction ldc2_w">ldc2_w </span>{ constantValue }</span>
                case 109 ⇒ <span class="instruction ldiv">ldiv</span>
                case 22  ⇒ <span><span class="instruction lload">lload </span>{ lvIndex }</span>
                case 30  ⇒ <span class="instruction lload_0">lload_0</span>
                case 31  ⇒ <span class="instruction lload_1">lload_1</span>
                case 32  ⇒ <span class="instruction lload_2">lload_2</span>
                case 33  ⇒ <span class="instruction lload_3">lload_3</span>
                case 105 ⇒ <span class="instruction lmul">lmul</span>
                case 117 ⇒ <span class="instruction lneg">lneg</span>
                case 171 ⇒
                    // LOOKUPSWITCH
                    in.skip(3 - (pc % 4)) // skip padding bytes
                    val defaultTarget = in.readInt + pc
                    val npairsCount = in.readInt
                    val table = new StringBuilder("");
                    repeat(npairsCount) {
                        table.append("(case:"+in.readInt+","+(in.readInt + pc)+") ")
                    }
                    <span><span class="instruction lookupswitch">lookupswitch </span>default:{ defaultTarget } [{ table }]</span>
                case 129 ⇒ <span class="instruction lor">lor</span>
                case 113 ⇒ <span class="instruction lrem">lrem</span>
                case 173 ⇒ <span class="instruction lreturn">lreturn</span>
                case 121 ⇒ <span class="instruction lshl">lshl</span>
                case 123 ⇒ <span class="instruction lshr">lshr</span>
                case 55  ⇒ <span><span class="instruction lstore">lstore </span>{ lvIndex }</span>
                case 63  ⇒ <span class="instruction lstore_0">lstore_0</span>
                case 64  ⇒ <span class="instruction lstore_1">lstore_1</span>
                case 65  ⇒ <span class="instruction lstore_2">lstore_2</span>
                case 66  ⇒ <span class="instruction lstore_3">lstore_3</span>
                case 101 ⇒ <span class="instruction lsub">lsub</span>
                case 125 ⇒ <span class="instruction lushr">lushr</span>
                case 131 ⇒ <span class="instruction lxor">lxor</span>
                case 194 ⇒ <span class="instruction monitorenter">monitorenter</span>
                case 195 ⇒ <span class="instruction monitorexit">monitorexit</span>
                case 197 ⇒
                    val referenceType = cp(in.readUnsignedShort).toString(cp)
                    val dim = in.readUnsignedByte
                    <span><span class="instruction multianewarray">multianewarray </span>{ referenceType } { dim } </span>
                case 187 ⇒
                    val referenceType = cp(in.readUnsignedShort).toString(cp).replace('/', '.')
                    <span><span class="instruction new">new </span>{ referenceType } </span>
                case 188 ⇒ <span><span class="instruction newarray">newarray </span>{ in.readByte } </span>
                case 0   ⇒ <span class="instruction nop">nop</span>
                case 87  ⇒ <span class="instruction pop">pop</span>
                case 88  ⇒ <span class="instruction pop2">pop2</span>
                case 181 ⇒
                    val c = in.readUnsignedShort
                    val fieldSignature = cp(c).toString(cp)+" ["+c+"]"
                    <span><span class="instruction putfield">putfield </span>{ fieldSignature }</span>
                case 179 ⇒
                    val c = in.readUnsignedShort
                    val fieldSignature = cp(c).toString(cp)+" ["+c+"]"
                    <span><span class="instruction putstatic">putstatic </span>{ fieldSignature }</span>
                case 169 ⇒
                    <span><span class="instruction ret">ret </span>{ lvIndex }</span>
                case 177 ⇒ <span class="instruction return">return</span>
                case 53  ⇒ <span class="instruction saload">saload</span>
                case 86  ⇒ <span class="instruction sastore">sastore</span>
                case 17  ⇒ <span><span class="instruction sipush">sipush </span>{ in.readShort /* value */ }</span>
                case 95  ⇒ <span class="instruction swap">swap</span>
                case 170 ⇒
                    in.skip(3 - (pc % 4)) // skip padding bytes
                    val defaultTarget = in.readInt + pc
                    val low = in.readInt
                    val high = in.readInt
                    var offsetcounter: Int = 0;
                    val table = new StringBuilder("");
                    repeat(high - low + 1) {
                        table.append("(case:"+(low + offsetcounter)+","+(in.readInt + pc)+") ")
                        offsetcounter += 1;
                    }
                    <span><span class="instruction tableswitch">tableswitch </span>default:{ defaultTarget } [{ table }]</span>
                case 196 ⇒
                    wide = true
                    <span class="instruction wide">wide</span>
                case opcode ⇒ throw new UnknownError("unknown opcode: "+opcode)
            }
        }
        instructions
    }

    def ExceptionsLinking(
        instructions: Array[Node],
        exceptionTable: IndexedSeq[ExceptionTableEntry]): Array[Node] = {
        for { exceptionHandler ← exceptionTable } yield {
            for (i ← exceptionHandler.start_pc until exceptionHandler.end_pc) {
                if (instructions(i) != null)
                    instructions(i) = <span> { instructions(i) } <span class="exception" alt="the ranges in the code
                 array at which the exception handler is active"></span></span>
            }

            instructions(exceptionHandler.handler_pc) =
                <span>
                    { instructions(exceptionHandler.handler_pc) }
                    <span class="exceptionHandler" alt="the start of the exception handler"></span>
                </span>
        }
        instructions
    }
}
