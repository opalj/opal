/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package de.tud.cs.st
package bat
package resolved
package ai

/**
 * @author Michael Eichberg
 */
object AI {

    /**
     * Analyzes the given method using the given domain.
     *
     * @param classFile Some class file.
     * @param method A non-abstract, non-native method of the given class file.
     * @param domain The abstract domain that is used during the interpretation.
     * @return The memory layout that was calculated while performing the abstract interpretation of
     * the method.
     */
    def apply(classFile: ClassFile, method: Method)(implicit domain: Domain): Array[MemoryLayout] = {
        val code = method.body.get.instructions
        val initialLocals = {
            var locals: IndexedSeq[Value] = new Array[Value](method.body.get.maxLocals)
            var localVariableIndex = 0

            if (!method.isStatic) {
                val thisType = classFile.thisClass
                locals = locals.updated(localVariableIndex, AReferenceTypeValue(thisType))
                localVariableIndex += 1 /*==thisType.computationalType.operandSize*/
            }

            for (parameterType ← method.descriptor.parameterTypes) {
                val ct = parameterType.computationalType
                locals = locals.updated(localVariableIndex, TypedValue(parameterType))
                localVariableIndex += ct.operandSize
            }
            locals
        }
        apply(code, initialLocals)
    }

    def apply(code: Array[Instruction], initialLocals: IndexedSeq[Value])(implicit domain: Domain): Array[MemoryLayout] = {
        // true if the instruction with the respective program counter is already transformed
        val memoryLayouts = new Array[MemoryLayout](code.length)
        memoryLayouts(0) = new MemoryLayout(Nil, initialLocals)

        var worklist: List[Int /*program counter*/ ] = List(0)

        def gotoTarget(nextPC: Int, nextPCMemoryLayout: MemoryLayout) {
            if (nextPC >= code.length) return ; // we have reached the end of the code

            if (memoryLayouts(nextPC) == null) {
                worklist = nextPC :: worklist
                memoryLayouts(nextPC) = nextPCMemoryLayout
            } else {
                val mergedMemoryLayout = memoryLayouts(nextPC) update nextPCMemoryLayout
                if (mergedMemoryLayout != memoryLayouts(nextPC)) {
                    worklist = nextPC :: worklist
                    memoryLayouts(nextPC) = mergedMemoryLayout
                }
            }
        }
        def gotoTargets(nextPCs: Seq[Int], nextPCMemoryLayout: MemoryLayout) {
            for (nextPC ← nextPCs) {
                gotoTarget(nextPC, nextPCMemoryLayout)
            }
        }

        while (worklist.nonEmpty) {
            val pc = worklist.head
            worklist = worklist.tail
            val instruction = code(pc)
            val memoryLayout = memoryLayouts(pc) // the memory layout before executing the instruction with the given pc

            def pcOfNextInstruction = {
                var nextPC = pc + 1
                /* TODO Add a method to (all) instruction(s) to get the PC of the next instruction and to avoid that we have to traverse the code. */
                while (nextPC < code.length && (code(nextPC) eq null)) nextPC += 1
                nextPC
            }

            (instruction.opcode: @annotation.switch) match {
                //
                // UNCONDITIONAL TRANSFER OF CONTROL
                //
                case 167 /*goto*/   ⇒ gotoTarget(pc + instruction.asInstanceOf[GOTO].branchoffset, memoryLayout.update(pc, instruction))
                case 200 /*goto_w*/ ⇒ gotoTarget(pc + instruction.asInstanceOf[GOTO_W].branchoffset, memoryLayout.update(pc, instruction))

                case 169 /*ret*/ ⇒ memoryLayout.locals(instruction.asInstanceOf[RET].lvIndex) match {
                    case ReturnAddressValue(returnAddress) ⇒ gotoTarget(returnAddress, memoryLayout.update(pc, instruction))
                    case _                                 ⇒ sys.error("internal implementation error or invalid bytecode")
                }
                case 168 /*jsr*/   ⇒ gotoTarget(pc + instruction.asInstanceOf[JSR].branchoffset, memoryLayout.update(pc, instruction))
                case 201 /*jsr_w*/ ⇒ gotoTarget(pc + instruction.asInstanceOf[JSR_W].branchoffset, memoryLayout.update(pc, instruction))
                //
                //            case 171 /*lookupswitch*/
                //               | 170 /*tableswitch*/ ⇒ new MemoryLayout(operands.tail, locals)
                //
                //            case 165 /*if_acmpeq*/
                //               | 166 /*if_acmpne*/
                //               | 159 /*if_icmpeq*/
                //               | 160 /*if_icmpne*/
                //               | 161 /*if_icmplt*/
                //               | 162 /*if_icmpge*/
                //               | 163 /*if_icmpgt*/
                //               | 164 /*if_icmple*/ ⇒ new MemoryLayout(operands.drop(2), locals)
                //            case 153 /*ifeq*/
                //               | 154 /*ifne*/
                //               | 155 /*iflt*/
                //               | 156 /*ifge*/
                //               | 157 /*ifgt*/
                //               | 158 /*ifle */
                case 199 /*ifnonnull*/ ⇒ {
                    val operand = memoryLayout.operands.head
                    domain.isNull(operand) match {
                        case BooleanAnswer.YES ⇒ gotoTarget(pcOfNextInstruction, memoryLayout.update(pc, instruction))
                        case BooleanAnswer.NO  ⇒ gotoTarget(pc + instruction.asInstanceOf[IFNONNULL].branchoffset, memoryLayout.update(pc, instruction))
                        case BooleanAnswer.UNKNOWN ⇒ {
                            gotoTarget(
                                pc + instruction.asInstanceOf[IFNONNULL].branchoffset,
                                domain.addIsNonNullConstraint(operand, memoryLayout.update(pc, instruction)))

                            gotoTarget(
                                pcOfNextInstruction,
                                domain.addIsNullConstraint(operand, memoryLayout.update(pc, instruction)))
                        }
                    }
                }
                case 198 /*ifnull*/ ⇒ {
                    val operand = memoryLayout.operands.head
                    domain.isNull(operand) match {
                        case BooleanAnswer.YES ⇒ gotoTarget(pc + instruction.asInstanceOf[IFNULL].branchoffset, memoryLayout.update(pc, instruction))
                        case BooleanAnswer.NO  ⇒ gotoTarget(pcOfNextInstruction, memoryLayout.update(pc, instruction))
                        case BooleanAnswer.UNKNOWN ⇒ {
                            gotoTarget(
                                pc + instruction.asInstanceOf[IFNULL].branchoffset,
                                domain.addIsNullConstraint(operand, memoryLayout.update(pc, instruction)))

                            gotoTarget(
                                pcOfNextInstruction,
                                domain.addIsNonNullConstraint(operand, memoryLayout.update(pc, instruction)))
                        }
                    }
                }
                case _ ⇒ {
                    val nextPC = pcOfNextInstruction
                    val nextMemoryLayout = memoryLayout.update(pc, instruction)
                    gotoTarget(nextPC, nextMemoryLayout)
                }
            }
        }

        memoryLayouts
    }
}
