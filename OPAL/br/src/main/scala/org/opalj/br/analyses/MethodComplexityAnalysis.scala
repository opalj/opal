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
package analyses

import org.opalj.UShort
import org.opalj.bytecode.BytecodeProcessingFailedException
import org.opalj.br.instructions._

/**
 * Implements a very naive approach for estimating the complexity of evaluating
 * functions (we are only concerned about methods that take and return values). This
 * rating can then be used to decide whether to "inline" method calls.
 *
 * Currently, we just try to determine the complexity of methods
 * that are guaranteed to have no side-effects (they don't call other
 * methods, they don't write fields) and which have no exception handling.
 * Furthermore, we count the number of conditional branches and throw instructions to
 * approximate the complexity of evaluating the methods.
 *
 * @author Michael Eichberg
 */
object MethodComplexityAnalysis {

    def doAnalyze(
        project: SomeProject,
        maxComplexity: Int,
        isInterrupted: () ⇒ Boolean): Map[Method, Int] = {

        val ratings = new java.util.concurrent.ConcurrentLinkedQueue[(Method, Int)]()
        project.parForeachMethodWithBody(isInterrupted) { m ⇒
            val (_, _, method) = m
            val body = method.body.get
            if ((method.returnType ne VoidType) && body.exceptionHandlers.isEmpty) {
                val complexity = analyzeMethod(method, maxComplexity)
                if (complexity < Int.MaxValue) ratings.add((method, complexity))
            }
        }
        import scala.collection.JavaConverters._
        ratings.asScala.toMap
    }

    protected[analyses] def analyzeMethod(method: Method, maxComplexity: Int): Int = {
        val body = method.body.get
        val instructions = body.instructions

        var complexity = instructions.size;
        var hasLoop = false;
        var evaluatedPCs: Set[PC] = Set.empty
        var nextPCs: Set[PC] = Set(0);

        while (nextPCs.nonEmpty && complexity < maxComplexity) {
            val pc = nextPCs.head
            evaluatedPCs += pc
            nextPCs = nextPCs.tail
            val currentInstruction = instructions(pc)
            (currentInstruction.opcode: @annotation.switch) match {
                //
                // UNCONDITIONAL TRANSFER OF CONTROL
                //
                case JSR.opcode | JSR_W.opcode | RET.opcode ⇒
                    throw new UnknownError("encountered unexpected instructions")

                case GOTO.opcode | GOTO_W.opcode ⇒ /*complexity += 0*/
                //
                // CONDITIONAL TRANSFER OF CONTROL
                //
                case 165 /*if_acmpeq*/           ⇒ complexity += 2
                case 166 /*if_acmpne*/           ⇒ complexity += 2
                case 198 /*ifnull*/              ⇒ complexity += 2
                case 199 /*ifnonnull*/           ⇒ complexity += 2
                case 159 /*if_icmpeq*/           ⇒ complexity += 2
                case 160 /*if_icmpne*/           ⇒ complexity += 2
                case 161 /*if_icmplt*/           ⇒ complexity += 2
                case 162 /*if_icmpge*/           ⇒ complexity += 2
                case 163 /*if_icmpgt*/           ⇒ complexity += 2
                case 164 /*if_icmple*/           ⇒ complexity += 2
                case 153 /*ifeq*/                ⇒ complexity += 2
                case 154 /*ifne*/                ⇒ complexity += 2
                case 155 /*iflt*/                ⇒ complexity += 2
                case 156 /*ifge*/                ⇒ complexity += 2
                case 157 /*ifgt*/                ⇒ complexity += 2
                case 158 /*ifle */               ⇒ complexity += 2
                case 171 /*lookupswitch*/ ⇒
                    val switch = instructions(pc).asInstanceOf[LOOKUPSWITCH]
                    complexity += switch.npairs.size
                case 170 /*tableswitch*/ ⇒
                    val tableswitch = instructions(pc).asInstanceOf[TABLESWITCH]
                    val low = tableswitch.low
                    val high = tableswitch.high
                    complexity += high - low

                //
                // STATEMENTS THAT CAN CAUSE EXCEPTIONAL TRANSFER OF CONTROL FLOW
                //
                case 191 /*athrow*/         ⇒ complexity += 5

                //
                // CREATE ARRAY
                //
                case 188 /*newarray*/       ⇒ /*complexity += 0*/
                case 189 /*anewarray*/      ⇒ /*complexity += 0*/
                case 197 /*multianewarray*/ ⇒ /*complexity += 0*/

                //
                // LOAD FROM AND STORE VALUE IN ARRAYS
                //
                case 50 /*aaload*/          ⇒ /*complexity += 0*/
                case 83 /*aastore*/         ⇒ /*complexity += 0*/
                case 51 /*baload*/          ⇒ /*complexity += 0*/
                case 84 /*bastore*/         ⇒ /*complexity += 0*/
                case 52 /*caload*/          ⇒ /*complexity += 0*/
                case 85 /*castore*/         ⇒ /*complexity += 0*/
                case 49 /*daload*/          ⇒ /*complexity += 0*/
                case 82 /*dastore*/         ⇒ /*complexity += 0*/
                case 48 /*faload*/          ⇒ /*complexity += 0*/
                case 81 /*fastore*/         ⇒ /*complexity += 0*/
                case 46 /*iaload*/          ⇒ /*complexity += 0*/
                case 79 /*iastore*/         ⇒ /*complexity += 0*/
                case 47 /*laload*/          ⇒ /*complexity += 0*/
                case 80 /*lastore*/         ⇒ /*complexity += 0*/
                case 53 /*saload*/          ⇒ /*complexity += 0*/
                case 86 /*sastore*/         ⇒ /*complexity += 0*/

                //
                // LENGTH OF AN ARRAY
                //
                case 190 /*arraylength*/    ⇒ /*complexity += 0*/

                //
                // METHOD INVOCATIONS & ACCESSING FIELDS
                //
                case 180 /*getfield*/       ⇒ complexity += 4
                case 178 /*getstatic*/      ⇒ complexity += 3

                case 181 /*putfield*/ |
                    179 /*putstatic*/ |
                    186 /*invokedynamic*/ |
                    185 /*invokeinterface*/ |
                    183 /*invokespecial*/ |
                    184 /*invokestatic*/ |
                    182 /*invokevirtual*/ ⇒
                    nextPCs = Set.empty
                    complexity = Int.MaxValue

                //
                // RETURN FROM METHOD
                //
                case 176 /*areturn*/ ⇒ /*complexity += 0*/
                case 175 /*dreturn*/ ⇒ /*complexity += 0*/
                case 174 /*freturn*/ ⇒ /*complexity += 0*/
                case 172 /*ireturn*/ ⇒ /*complexity += 0*/
                case 173 /*lreturn*/ ⇒ /*complexity += 0*/
                case 177 /*return*/  ⇒ /*complexity += 0*/

                // -----------------------------------------------------------------------
                //
                // INSTRUCTIONS THAT ALWAYS JUST FALL THROUGH AND WILL
                // NEVER THROW AN EXCEPTION
                //
                // -----------------------------------------------------------------------

                //
                // PUT LOCAL VARIABLE VALUE ONTO STACK
                //
                case 25 /*aload*/
                    | 24 /*dload*/
                    | 23 /*fload*/
                    | 21 /*iload*/
                    | 22 /*lload*/ ⇒ /*complexity += 0*/
                case 42 /*aload_0*/
                    | 38 /*dload_0*/
                    | 34 /*fload_0*/
                    | 26 /*iload_0*/
                    | 30 /*lload_0*/ ⇒ /*complexity += 0*/
                case 43 /*aload_1*/
                    | 39 /*dload_1*/
                    | 35 /*fload_1*/
                    | 27 /*iload_1*/
                    | 31 /*lload_1*/ ⇒ /*complexity += 0*/
                case 44 /*aload_2*/
                    | 40 /*dload_2*/
                    | 36 /*fload_2*/
                    | 28 /*iload_2*/
                    | 32 /*lload_2*/ ⇒ /*complexity += 0*/
                case 45 /*aload_3*/
                    | 41 /*dload_3*/
                    | 37 /*fload_3*/
                    | 29 /*iload_3*/
                    | 33 /*lload_3*/ ⇒ /*complexity += 0*/

                //
                // STORE OPERAND IN LOCAL VARIABLE
                //
                case 58 /*astore*/
                    | 57 /*dstore*/
                    | 56 /*fstore*/
                    | 54 /*istore*/
                    | 55 /*lstore*/ ⇒ /*complexity += 0*/
                case 75 /*astore_0*/
                    | 71 /*dstore_0*/
                    | 67 /*fstore_0*/
                    | 63 /*lstore_0*/
                    | 59 /*istore_0*/ ⇒ /*complexity += 0*/
                case 76 /*astore_1*/
                    | 72 /*dstore_1*/
                    | 68 /*fstore_1*/
                    | 64 /*lstore_1*/
                    | 60 /*istore_1*/ ⇒ /*complexity += 0*/
                case 77 /*astore_2*/
                    | 73 /*dstore_2*/
                    | 69 /*fstore_2*/
                    | 65 /*lstore_2*/
                    | 61 /*istore_2*/ ⇒ /*complexity += 0*/
                case 78 /*astore_3*/
                    | 74 /*dstore_3*/
                    | 70 /*fstore_3*/
                    | 66 /*lstore_3*/
                    | 62 /*istore_3*/ ⇒ /*complexity += 0*/

                //
                // PUSH CONSTANT VALUE
                //
                case 1 /*aconst_null*/                          ⇒ /*complexity += 0*/
                case 16 /*bipush*/                              ⇒ /*complexity += 0*/
                case 14 /*dconst_0*/                            ⇒ /*complexity += 0*/
                case 15 /*dconst_1*/                            ⇒ /*complexity += 0*/
                case 11 /*fconst_0*/                            ⇒ /*complexity += 0*/
                case 12 /*fconst_1*/                            ⇒ /*complexity += 0*/
                case 13 /*fconst_2*/                            ⇒ /*complexity += 0*/
                case 2 /*iconst_m1*/                            ⇒ /*complexity += 0*/
                case 3 /*iconst_0*/                             ⇒ /*complexity += 0*/
                case 4 /*iconst_1*/                             ⇒ /*complexity += 0*/
                case 5 /*iconst_2*/                             ⇒ /*complexity += 0*/
                case 6 /*iconst_3*/                             ⇒ /*complexity += 0*/
                case 7 /*iconst_4*/                             ⇒ /*complexity += 0*/
                case 8 /*iconst_5*/                             ⇒ /*complexity += 0*/
                case 9 /*lconst_0*/                             ⇒ /*complexity += 0*/
                case 10 /*lconst_1*/                            ⇒ /*complexity += 0*/
                case 18 /*ldc*/                                 ⇒ /*complexity += 0*/
                case 19 /*ldc_w*/                               ⇒ /*complexity += 0*/
                case 20 /*ldc2_w*/                              ⇒ /*complexity += 0*/
                case 17 /*sipush*/                              ⇒ /*complexity += 0*/

                //
                // RELATIONAL OPERATORS
                //
                case 150 /*fcmpg*/                              ⇒ /*complexity += 0*/
                case 149 /*fcmpl*/                              ⇒ /*complexity += 0*/
                case 152 /*dcmpg*/                              ⇒ /*complexity += 0*/
                case 151 /*dcmpl*/                              ⇒ /*complexity += 0*/
                case 148 /*lcmp*/                               ⇒ /*complexity += 0*/

                //
                // UNARY EXPRESSIONS
                //
                case 119 /*dneg*/                               ⇒ /*complexity += 0*/
                case 118 /*fneg*/                               ⇒ /*complexity += 0*/
                case 117 /*lneg*/                               ⇒ /*complexity += 0*/
                case 116 /*ineg*/                               ⇒ /*complexity += 0*/

                //
                // BINARY EXPRESSIONS
                //
                case 99 /*dadd*/                                ⇒ /*complexity += 0*/
                case 111 /*ddiv*/                               ⇒ /*complexity += 0*/
                case 107 /*dmul*/                               ⇒ /*complexity += 0*/
                case 115 /*drem*/                               ⇒ /*complexity += 0*/
                case 103 /*dsub*/                               ⇒ /*complexity += 0*/
                case 98 /*fadd*/                                ⇒ /*complexity += 0*/
                case 110 /*fdiv*/                               ⇒ /*complexity += 0*/
                case 106 /*fmul*/                               ⇒ /*complexity += 0*/
                case 114 /*frem*/                               ⇒ /*complexity += 0*/
                case 102 /*fsub*/                               ⇒ /*complexity += 0*/
                case 96 /*iadd*/                                ⇒ /*complexity += 0*/
                case 126 /*iand*/                               ⇒ /*complexity += 0*/
                case 108 /*idiv*/                               ⇒ /*complexity += 0*/
                case 104 /*imul*/                               ⇒ /*complexity += 0*/
                case 128 /*ior*/                                ⇒ /*complexity += 0*/
                case 112 /*irem*/                               ⇒ /*complexity += 0*/
                case 120 /*ishl*/                               ⇒ /*complexity += 0*/
                case 122 /*ishr*/                               ⇒ /*complexity += 0*/
                case 100 /*isub*/                               ⇒ /*complexity += 0*/
                case 124 /*iushr*/                              ⇒ /*complexity += 0*/
                case 130 /*ixor*/                               ⇒ /*complexity += 0*/
                case 97 /*ladd*/                                ⇒ /*complexity += 0*/
                case 127 /*land*/                               ⇒ /*complexity += 0*/
                case 109 /*ldiv*/                               ⇒ /*complexity += 0*/
                case 105 /*lmul*/                               ⇒ /*complexity += 0*/
                case 129 /*lor*/                                ⇒ /*complexity += 0*/
                case 113 /*lrem*/                               ⇒ /*complexity += 0*/
                case 121 /*lshl*/                               ⇒ /*complexity += 0*/
                case 123 /*lshr*/                               ⇒ /*complexity += 0*/
                case 101 /*lsub*/                               ⇒ /*complexity += 0*/
                case 125 /*lushr*/                              ⇒ /*complexity += 0*/
                case 131 /*lxor*/                               ⇒ /*complexity += 0*/

                //
                // GENERIC STACK MANIPULATION
                //
                case 89 /*dup*/                                 ⇒ /*complexity += 0*/
                case 90 /*dup_x1*/                              ⇒ /*complexity += 0*/
                case 91 /*dup_x2*/                              ⇒ /*complexity += 0*/
                case 92 /*dup2*/                                ⇒ /*complexity += 0*/
                case 93 /*dup2_x1*/                             ⇒ /*complexity += 0*/
                case 94 /*dup2_x2*/                             ⇒ /*complexity += 0*/

                case 87 /*pop*/                                 ⇒ /*complexity += 0*/
                case 88 /*pop2*/                                ⇒ /*complexity += 0*/

                case 95 /*swap*/                                ⇒ /*complexity += 0*/

                //
                // TYPE CONVERSION
                //
                case 144 /*d2f*/                                ⇒ /*complexity += 0*/
                case 142 /*d2i*/                                ⇒ /*complexity += 0*/
                case 143 /*d2l*/                                ⇒ /*complexity += 0*/
                case 141 /*f2d*/                                ⇒ /*complexity += 0*/
                case 139 /*f2i*/                                ⇒ /*complexity += 0*/
                case 140 /*f2l*/                                ⇒ /*complexity += 0*/
                case 145 /*i2b*/                                ⇒ /*complexity += 0*/
                case 146 /*i2c*/                                ⇒ /*complexity += 0*/
                case 135 /*i2d*/                                ⇒ /*complexity += 0*/
                case 134 /*i2f*/                                ⇒ /*complexity += 0*/
                case 133 /*i2l*/                                ⇒ /*complexity += 0*/
                case 147 /*i2s*/                                ⇒ /*complexity += 0*/
                case 138 /*l2d*/                                ⇒ /*complexity += 0*/
                case 137 /*l2f*/                                ⇒ /*complexity += 0*/
                case 136 /*l2i*/                                ⇒ /*complexity += 0*/

                case 192 /*checkcast*/                          ⇒ complexity += 2

                //
                // "OTHER" INSTRUCTIONS
                //

                case 194 /*monitorenter*/ | 195 /*monitorexit*/ ⇒ complexity += 5

                case 193 /*instanceof*/                         ⇒ complexity += 2

                case 132 /*iinc*/                               ⇒ /*complexity += 0*/

                case 187 /*new*/                                ⇒ /*complexity += 0*/

                case 0 /*nop*/                                  ⇒ /*complexity += 0*/
                case 196 /*wide*/                               ⇒ /*complexity += 0*/

                case opcode ⇒
                    throw new BytecodeProcessingFailedException(
                        s"unsupported opcode: $opcode"
                    )

            }
            if (complexity < 0) {
                // we had an overflow...
                complexity = Int.MaxValue
                nextPCs = Set.empty
            } else {
                currentInstruction.nextInstructions(pc, body).foreach { nextPC ⇒
                    if (evaluatedPCs.contains(nextPC)) {
                        if (nextPC <= pc) {
                            // we have detected a loop
                            hasLoop = true
                        }
                    } else
                        nextPCs += nextPC
                }
            }
        }

        if (hasLoop) complexity = complexity + UShort.MaxValue
        if (complexity >= 0 && complexity <= maxComplexity)
            complexity
        else
            Int.MaxValue
    }
}
