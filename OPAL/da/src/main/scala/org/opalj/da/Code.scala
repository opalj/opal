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
import scala.util.Random

/**
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 */
case class Code(instructions: Array[Byte]) {

    def toXHTML(
        attributes: Attributes,
        exception_handlers: IndexedSeq[ExceptionTableEntry])(
            implicit cp: Constant_Pool): Node = {

        val instructionslist = InstructionsToXHTML(instructions)
        CodeAttributesLinking(instructionslist, attributes)
        ExceptionsLinking(instructionslist, exception_handlers)
        val rows =
            for (Index ← (0 until instructionslist.length))
                yield if (instructionslist(Index) != null)
                InstructionToXHTML(Index, instructionslist(Index))

        <table style="width:100%;">{ rows }</table>
    }

    def InstructionToXHTML(pc: Int, instruction: Node): Node =
        <tr><td><span class="pc">{ pc }</span></td><td> { instruction }</td></tr>

    def InstructionsToXHTML(source: Array[Byte])(implicit cp: Constant_Pool): Array[Node] = {
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

        while (in.available > 0) {
            val index = codeLength - in.available
            instructions(index) = (in.readUnsignedByte: @scala.annotation.switch) match {
                case 50 ⇒ <span title="aaload">aaload</span>
                case 83 ⇒ <span title="aastore">aastore</span>
                case 1  ⇒ <span title="aconst_null">aconst_null</span>
                case 25 ⇒ <span title="aload">aload { lvIndex() }</span>
                case 42 ⇒ <span title="aload_0">aload_0</span>
                case 43 ⇒ <span title="aload_1">aload_1</span>
                case 44 ⇒ <span title="aload_2">aload_2</span>
                case 45 ⇒ <span title="aload_3">aload_3</span>
                case 189 ⇒
                    <span title="anewarray">
                        anewarray&nbsp;{ asObjectType(in.readUnsignedShort) }
                    </span>
                case 176 ⇒ <span title="areturn">areturn</span>
                case 190 ⇒ <span title="arraylength">arraylength</span>
                case 58  ⇒ <span title="astore">astore { lvIndex }</span>
                case 75  ⇒ <span title="astore_0">astore_0</span>
                case 76  ⇒ <span title="astore_1">astore_1</span>
                case 77  ⇒ <span title="astore_2">astore_2</span>
                case 78  ⇒ <span title="astore_3">astore_3</span>
                case 191 ⇒ <span title="athrow">athrow</span>
                case 51  ⇒ <span title="baload">baload</span>
                case 84  ⇒ <span title="bastore">bastore</span>
                case 16  ⇒ <span title="bipush">bipush { in.readByte }</span>
                case 52  ⇒ <span title="caload">caload</span>
                case 85  ⇒ <span title="castore">castore</span>
                case 192 ⇒
                    <span title="checkcast">
                        checkcast&nbsp;{ asReferenceType(in.readUnsignedShort) }
                    </span>
                case 144 ⇒ <span title="d2f">d2f</span>
                case 142 ⇒ <span title="d2i">d2i</span>
                case 143 ⇒ <span title="d2l">d2l</span>
                case 99  ⇒ <span title="dadd">dadd</span>
                case 49  ⇒ <span title="daload">daload</span>
                case 82  ⇒ <span title="dastore">dastore</span>
                case 152 ⇒ <span title="dcmpg">dcmpg </span>
                case 151 ⇒ <span title="dcmpl">dcmpl </span>
                case 14  ⇒ <span title="dconst_0">dconst_0</span>
                case 15  ⇒ <span title="dconst_1">dconst_1</span>
                case 111 ⇒ <span title="ddiv">ddiv </span>
                case 24  ⇒ <span title="dload">dload { lvIndex }</span>
                case 38  ⇒ <span title="dload_0">dload_0</span>
                case 39  ⇒ <span title="dload_1">dload_1</span>
                case 40  ⇒ <span title="dload_2">dload_2</span>
                case 41  ⇒ <span title="dload_3">dload_3</span>
                case 107 ⇒ <span title="dmul">dmul</span>
                case 119 ⇒ <span title="dneg">dneg</span>
                case 115 ⇒ <span title="drem">drem</span>
                case 175 ⇒ <span title="dreturn">dreturn</span>
                case 57  ⇒ <span title="dstore">dstore { lvIndex }</span>
                case 71  ⇒ <span title="dstore_0">dstore_0</span>
                case 72  ⇒ <span title="dstore_1">dstore_1</span>
                case 73  ⇒ <span title="dstore_2">dstore_2</span>
                case 74  ⇒ <span title="dstore_3">dstore_3</span>
                case 103 ⇒ <span title="dsub">dsub</span>
                case 89  ⇒ <span title="dsub">dup</span>
                case 90  ⇒ <span title="dup_x1">dup_x1</span>
                case 91  ⇒ <span title="dup_x2">dup_x2</span>
                case 92  ⇒ <span title="dup2">dup2</span>
                case 93  ⇒ <span title="dup2_x1">dup2_x1</span>
                case 94  ⇒ <span title="dup2_x2">dup2_x2</span>
                case 141 ⇒ <span title="f2d">f2d</span>
                case 139 ⇒ <span title="f2i">f2i</span>
                case 140 ⇒ <span title="f2l">f2l</span>
                case 98  ⇒ <span title="fadd">fadd</span>
                case 48  ⇒ <span title="faload">faload</span>
                case 81  ⇒ <span title="fastore">fastore</span>
                case 150 ⇒ <span title="fastore">fcmpg</span>
                case 149 ⇒ <span title="fcmpl">fcmpl</span>
                case 11  ⇒ <span title="fconst_0">fconst_0</span>
                case 12  ⇒ <span title="fconst_1">fconst_1</span>
                case 13  ⇒ <span title="fconst_2">fconst_2</span>
                case 110 ⇒ <span title="fdiv">fdiv</span>
                case 23  ⇒ <span title="fload">fload { lvIndex }</span>
                case 34  ⇒ <span title="fload_0">fload_0</span>
                case 35  ⇒ <span title="fload_1">fload_1</span>
                case 36  ⇒ <span title="fload_2">fload_2</span>
                case 37  ⇒ <span title="fload_3">fload_3</span>
                case 106 ⇒ <span title="fmul">fmul</span>
                case 118 ⇒ <span title="fneg">fneg</span>
                case 114 ⇒ <span title="frem">frem</span>
                case 174 ⇒ <span title="freturn">freturn</span>
                case 56  ⇒ <span title="fstore">fstore { lvIndex }</span>
                case 67  ⇒ <span title="fstore_0">fstore_0</span>
                case 68  ⇒ <span title="fstore_1">fstore_1</span>
                case 69  ⇒ <span title="fstore_2">fstore_2</span>
                case 70  ⇒ <span title="fstore_3">fstore_3</span>
                case 102 ⇒ <span title="fsub">fsub</span>
                case 180 ⇒ <span title="getfield">getfield { val c = in.readUnsignedShort; cp(c).toString(cp)+" ["+c+"]" }</span>
                case 178 ⇒ <span title="getstatic">getstatic { val c = in.readUnsignedShort; cp(c).toString(cp)+" ["+c+"]" }</span>
                case 167 ⇒ <span title="goto">goto <span class="index">{ in.readShort + index }</span></span>
                case 200 ⇒ <span title="goto_w">goto_w <span class="index">{ in.readInt + index }</span></span>
                case 145 ⇒ <span title="i2b">i2b</span>
                case 146 ⇒ <span title="i2c">i2c</span>
                case 135 ⇒ <span title="i2d">i2d</span>
                case 134 ⇒ <span title="i2f">i2f</span>
                case 133 ⇒ <span title="i2l">i2l</span>
                case 147 ⇒ <span title="i2s">i2s</span>
                case 96  ⇒ <span title="iadd">iadd</span>
                case 46  ⇒ <span title="iadd">iaload</span>
                case 126 ⇒ <span title="iand">iand</span>
                case 79  ⇒ <span title="iastore">iastore</span>
                case 2   ⇒ <span title="iconst_m1">iconst_m1</span>
                case 3   ⇒ <span title="iconst_0">iconst_0</span>
                case 4   ⇒ <span title="iconst_1">iconst_1</span>
                case 5   ⇒ <span title="iconst_2">iconst_2</span>
                case 6   ⇒ <span title="iconst_3">iconst_3</span>
                case 7   ⇒ <span title="iconst_4">iconst_4</span>
                case 8   ⇒ <span title="iconst_5">iconst_5</span>
                case 108 ⇒ <span title="idiv">idiv</span>
                case 165 ⇒ <span title="if_acmpeq">if_acmpeq <span class="index">{ in.readShort + index }</span></span>
                case 166 ⇒ <span title="if_acmpne">if_acmpne <span class="index">{ in.readShort + index }</span></span>
                case 159 ⇒ <span title="if_icmpeq">if_icmpeq <span class="index">{ in.readShort + index }</span></span>
                case 160 ⇒ <span title="if_icmpne">if_icmpne <span class="index">{ in.readShort + index }</span></span>
                case 161 ⇒ <span title="if_icmplt">if_icmplt <span class="index">{ in.readShort + index }</span></span>
                case 162 ⇒ <span title="if_icmpge">if_icmpge <span class="index">{ in.readShort + index }</span></span>
                case 163 ⇒ <span title="if_icmpgt">if_icmpgt <span class="index">{ in.readShort + index }</span></span>
                case 164 ⇒ <span title="if_icmple">if_icmple <span class="index">{ in.readShort + index }</span></span>
                case 153 ⇒ <span title="ifeq">ifeq <span class="index">{ in.readShort + index }</span></span>
                case 154 ⇒ <span title="ifeq">ifne <span class="index">{ in.readShort + index }</span></span>
                case 155 ⇒ <span title="iflt">iflt <span class="index">{ in.readShort + index }</span></span>
                case 156 ⇒ <span title="ifge">ifge <span class="index">{ in.readShort + index }</span></span>
                case 157 ⇒ <span title="ifgt">ifgt <span class="index">{ in.readShort + index }</span></span>
                case 158 ⇒ <span title="ifle">ifle <span class="index">{ in.readShort + index }</span></span>
                case 199 ⇒ <span title="ifnonnull">ifnonnull <span class="index">{ in.readShort + index }</span></span>
                case 198 ⇒ <span title="ifnull">ifnull <span class="index">{ in.readShort + index }</span></span>
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
                    <span title="iinc">iinc { params }</span>
                case 21  ⇒ <span title="iload">iload { lvIndex }</span>
                case 26  ⇒ <span title="iload_0">iload_0</span>
                case 27  ⇒ <span title="iload_1">iload_1</span>
                case 28  ⇒ <span title="iload_2">iload_2</span>
                case 29  ⇒ <span title="iload_3">iload_3</span>
                case 104 ⇒ <span title="imul">imul</span>
                case 116 ⇒ <span title="ineg">ineg</span>
                case 193 ⇒
                    val referenceType = cp(in.readUnsignedShort).toString(cp)
                    <span title="instanceof">instanceof { referenceType }</span>
                case 186 ⇒ <span title="unresolved_invokedynamic">unresolved_invokedynamic {
                    val c = in.readUnsignedShort
                    in.readByte // ignored; fixed value
                    in.readByte // ignored; fixed value
                    cp(c).toString(cp)
                }</span>
                case 185 ⇒ <span title="invokeinterface">invokeinterface {
                    val c = in.readUnsignedShort
                    in.readByte // ignored; fixed value
                    in.readByte // ignored; fixed value
                    cp(c).toString(cp)+" ["+c+"]"
                }</span>
                case 183 ⇒
                    val c = in.readUnsignedShort
                    val signature = cp(c).toString(cp)+" ["+c+"]"
                    <span title="invokespecial">invokespecial  { signature } </span>
                case 184 ⇒
                    val c = in.readUnsignedShort
                    val signature = cp(c).toString(cp)+" ["+c+"]"
                    <span title="invokespecial">invokestatic  { signature } </span>
                case 182 ⇒
                    val c = in.readUnsignedShort
                    val signature = cp(c).toString(cp)+" ["+c+"]"
                    <span title="invokevirtual">invokevirtual { signature } </span>
                case 128 ⇒ <span title="ior">ior</span>
                case 112 ⇒ <span title="irem">irem</span>
                case 172 ⇒ <span title="ireturn">ireturn</span>
                case 120 ⇒ <span title="ireturn">ishl</span>
                case 122 ⇒ <span title="ishr">ishr</span>
                case 54  ⇒ <span title="istore">istore { lvIndex }</span>
                case 59  ⇒ <span title="istore_0">istore_0</span>
                case 60  ⇒ <span title="istore_1">istore_1</span>
                case 61  ⇒ <span title="istore_2">istore_2</span>
                case 62  ⇒ <span title="istore_3">istore_3</span>
                case 100 ⇒ <span title="isub">isub</span>
                case 124 ⇒ <span title="iushr">iushr</span>
                case 130 ⇒ <span title="ixor">ixor</span>
                case 168 ⇒ <span title="jsr">jsr { in.readShort + index }</span>
                case 201 ⇒ <span title="jsr_w">jsr_w { in.readInt + index }</span>
                case 138 ⇒ <span title="l2d">l2d</span>
                case 137 ⇒ <span title="l2d">l2f</span>
                case 136 ⇒ <span title="l2i">l2i</span>
                case 97  ⇒ <span title="ladd">ladd</span>
                case 47  ⇒ <span title="laload">laload</span>
                case 127 ⇒ <span title="land">land</span>
                case 80  ⇒ <span title="lastore">lastore</span>
                case 148 ⇒ <span title="lcmp">lcmp</span>
                case 9   ⇒ <span title="lconst_0">lconst_0</span>
                case 10  ⇒ <span title="lconst_1">lconst_1</span>
                case 18 ⇒
                    val constantValue = cp(in.readUnsignedByte()).toString(cp)
                    <span title="ldc">ldc { constantValue }</span>
                case 19 ⇒
                    val constantValue = cp(in.readUnsignedShort).toString(cp)
                    <span title="ldc_w">ldc_w { constantValue }</span>
                case 20 ⇒
                    val constantValue = cp(in.readUnsignedShort).toString(cp)
                    <span title="ldc2_w">ldc2_w { constantValue }</span>
                case 109 ⇒ <span title="ldiv">ldiv</span>
                case 22  ⇒ <span title="lload">lload { lvIndex }</span>
                case 30  ⇒ <span title="lload_0">lload_0</span>
                case 31  ⇒ <span title="lload_1">lload_1</span>
                case 32  ⇒ <span title="lload_2">lload_2</span>
                case 33  ⇒ <span title="lload_3">lload_3</span>
                case 105 ⇒ <span title="lmul">lmul</span>
                case 117 ⇒ <span title="lneg">lneg</span>
                case 171 ⇒
                    // LOOKUPSWITCH
                    in.skip(3 - (index % 4)) // skip padding bytes
                    val defaultOffset = in.readInt + index
                    val npairsCount = in.readInt
                    val table = new StringBuilder("");
                    repeat(npairsCount) {
                        table.append("(case:"+in.readInt+","+(in.readInt + index)+") ")
                    }
                    <span title="lookupswitch">lookupswitch default:{ defaultOffset } [{ table }]</span>
                case 129 ⇒ <span title="lor">lor</span>
                case 113 ⇒ <span title="lrem">lrem</span>
                case 173 ⇒ <span title="lreturn">lreturn</span>
                case 121 ⇒ <span title="lshl">lshl</span>
                case 123 ⇒ <span title="lshl">lshr</span>
                case 55  ⇒ <span title="lstore">lstore { lvIndex }</span>
                case 63  ⇒ <span title="lstore_0">lstore_0</span>
                case 64  ⇒ <span title="lstore_1">lstore_1</span>
                case 65  ⇒ <span title="lstore_2">lstore_2</span>
                case 66  ⇒ <span title="lstore_3">lstore_3</span>
                case 101 ⇒ <span title="lsub">lsub</span>
                case 125 ⇒ <span title="lushr">lushr</span>
                case 131 ⇒ <span title="lushr">lxor</span>
                case 194 ⇒ <span title="monitorenter">monitorenter</span>
                case 195 ⇒ <span title="monitorexit">monitorexit</span>
                case 197 ⇒
                    val referenceType = cp(in.readUnsignedShort).toString(cp)
                    val dim = in.readUnsignedByte
                    <span title="multianewarray">multianewarray { referenceType } { dim }</span>
                case 187 ⇒
                    val referenceType = cp(in.readUnsignedShort).toString(cp).replace('/', '.')
                    <span title="new">new { referenceType }</span>
                case 188 ⇒ <span title="newarray">newarray { in.readByte }</span>
                case 0   ⇒ <span title="nop">nop</span>
                case 87  ⇒ <span title="pop">pop</span>
                case 88  ⇒ <span title="pop2">pop2</span>
                case 181 ⇒ <span title="putfield">putfield { val c = in.readUnsignedShort; cp(c).toString(cp)+" ["+c+"]" }</span>
                case 179 ⇒ <span title="putstatic">putstatic { val c = in.readUnsignedShort; cp(c).toString(cp)+" ["+c+"]" }</span>
                case 169 ⇒
                    val lvIndex =
                        if (wide) {
                            wide = false
                            in.readUnsignedShort
                        } else {
                            in.readUnsignedByte
                        }
                    <span title="ret">ret { lvIndex }</span>
                case 177 ⇒ <span title="return"><span class="reservedwords">return</span></span>
                case 53  ⇒ <span title="saload">saload</span>
                case 86  ⇒ <span title="sastore">sastore</span>
                case 17  ⇒ <span title="sipush">sipush { in.readShort /* value */ }</span>
                case 95  ⇒ <span title="swap">swap </span>
                case 170 ⇒
                    in.skip(3 - (index % 4)) // skip padding bytes
                    val defaultOffset = in.readInt + index
                    val low = in.readInt
                    val high = in.readInt
                    var offsetcounter: Int = 0;
                    val table = new StringBuilder("");
                    repeat(high - low + 1) {
                        table.append("(case:"+(low + offsetcounter)+","+(in.readInt + index)+") ")
                        offsetcounter += 1;
                    }
                    <span title="tableswitch">tableswitch default:{ defaultOffset } [{ table }]</span>
                case 196 ⇒
                    wide = true
                    <span title="wide">wide</span>
                case opcode ⇒ throw new UnknownError("unknown opcode: "+opcode)
            }

        }
        InstructionsToLinking(instructions)
    }

    def InstructionsToLinking(instructions: Array[Node]): Array[Node] = {
        for { cpIndex ← (1 until instructions.length) } yield {
            if (instructions(cpIndex) != null)
                instructions(cpIndex).child(0).text match {
                    case "goto " ⇒ {
                        val goto_index = Integer.parseInt(instructions(cpIndex).child(1).text)
                        val random = Random.nextInt();
                        instructions(goto_index) = { <a id={ goto_index.toString + random }>{ instructions(goto_index) }</a> }
                        instructions(cpIndex) =
                            <a href={ "#"+goto_index.toString + random }>
                                <span>goto <span class="index">{ goto_index }</span></span>
                            </a>
                    }
                    case other ⇒
                }
        }
        instructions
    }

    def CodeAttributesLinking(instructions: Array[Node], attributes: Attributes): Array[Node] = {
        for { atIndex ← (0 until attributes.length) } yield {
            if (attributes(atIndex) != null)
                attributes(atIndex) match {
                    case LineNumberTable_attribute(attribute_name_index, line_number_table) ⇒ {
                        for (line ← line_number_table) yield {
                            val LNT_index = line.start_pc;
                            instructions(LNT_index) = <span> { instructions(LNT_index) }  <a href="#" class="tooltip">LN<span>Line Number :{ line.line_number }</span></a></span>
                        }
                    }
                    case other ⇒
                }
        }
        instructions
    }
    def ExceptionsLinking(instructions: Array[Node], exception_handlers: IndexedSeq[ExceptionTableEntry]): Array[Node] = {
        for { atIndex ← (0 until exception_handlers.length) } yield {
            for (i ← exception_handlers(atIndex).start_pc to exception_handlers(atIndex).end_pc) {
                if (instructions(i) != null)
                    instructions(i) = <span> { instructions(i) } <span class="exception" alt="the ranges in the code
                 array at which the exception handler is active"></span></span>
            }

            if (instructions(exception_handlers(atIndex).handler_pc) != null)
                instructions(exception_handlers(atIndex).handler_pc) =
                    <span>
                        { instructions(atIndex) }
                        <span class="exceptionHandler" alt="the start of the exception handler"></span>
                    </span>
        }
        instructions
    }
}
