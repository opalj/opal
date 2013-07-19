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
     * @param classFile Some class file; needed to determine the type of `this` if
     *      the method is an instance method.
     * @param method A non-abstract, non-native method of the given class file.
     * @param domain The abstract domain that is used during the interpretation.
     * @param someLocals If the values passed to a method are already known, the
     *     abstract interpretation will be performed under that assumption.
     * @return The memory layout that was in effect before the execution of each
     *     instruction while performing the abstract interpretation of the method.
     */
    def apply(
        classFile: ClassFile,
        method: Method,
        someLocals: Option[IndexedSeq[Value]] = None)(
            implicit domain: Domain): IndexedSeq[MemoryLayout] = {

        import domain._

        val code = method.body.get
        val initialLocals = (
            someLocals.map(l ⇒ {
                assume(l.size == method.body.get.maxLocals)
                l.toArray
            }).getOrElse({
                var locals: Array[Value] = new Array[Value](method.body.get.maxLocals)
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
            })
        )
        apply(code, initialLocals)(domain)
    }

    def apply(
        code: Code,
        initialLocals: IndexedSeq[Value])(
            implicit domain: Domain): IndexedSeq[MemoryLayout] = {

        import domain._

        val instructions: Array[Instruction] = code.instructions

        // The memory lacout that we associate with each instruction
        val memoryLayouts = new Array[MemoryLayout](instructions.length)
        memoryLayouts(0) = new MemoryLayout(Nil, initialLocals)(domain)

        // true if the instruction with the respective program counter is already transformed
        var worklist: List[Int /*program counter*/ ] = List(0)

        def gotoTarget(nextPC: Int, nextPCMemoryLayout: MemoryLayout) {
            if (nextPC >= instructions.length) return ; // we have reached the end of the code

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
        def gotoTargets(nextPCs: Iterable[Int], nextPCMemoryLayout: MemoryLayout) {
            for (nextPC ← nextPCs) {
                gotoTarget(nextPC, nextPCMemoryLayout)
            }
        }

        while (worklist.nonEmpty) {
            val pc = worklist.head
            worklist = worklist.tail
            val instruction = instructions(pc)
            // the memory layout before executing the instruction with the given pc
            val memoryLayout = memoryLayouts(pc)

            def pcOfNextInstruction = instructions(pc).indexOfNextInstruction(pc, code)

            (instruction.opcode: @annotation.switch) match {
                //
                // UNCONDITIONAL TRANSFER OF CONTROL
                //
                case 167 /*goto*/ ⇒
                    gotoTarget(
                        pc + instruction.asInstanceOf[GOTO].branchoffset,
                        memoryLayout.update(pc, instruction))
                case 200 /*goto_w*/ ⇒
                    gotoTarget(
                        pc + instruction.asInstanceOf[GOTO_W].branchoffset,
                        memoryLayout.update(pc, instruction))

                case 169 /*ret*/ ⇒ {
                    val lvIndex = instruction.asInstanceOf[RET].lvIndex
                    memoryLayout.locals(lvIndex) match {
                        case ReturnAddressValue(returnAddress) ⇒
                            gotoTargets(returnAddress, memoryLayout.update(pc, instruction))
                        case _ ⇒
                            CodeError("the local variable ("+
                                lvIndex+
                                ") does not contain a return address value", code, lvIndex)
                    }
                }
                case 168 /*jsr*/ ⇒
                    gotoTarget(
                        pc + instruction.asInstanceOf[JSR].branchoffset,
                        memoryLayout.update(pc, instruction))
                case 201 /*jsr_w*/ ⇒
                    gotoTarget(
                        pc + instruction.asInstanceOf[JSR_W].branchoffset,
                        memoryLayout.update(pc, instruction))
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
                        case BooleanAnswer.YES ⇒
                            gotoTarget(
                                pcOfNextInstruction,
                                memoryLayout.update(pc, instruction))
                        case BooleanAnswer.NO ⇒
                            gotoTarget(
                                pc + instruction.asInstanceOf[IFNONNULL].branchoffset,
                                memoryLayout.update(pc, instruction))
                        case BooleanAnswer.UNKNOWN ⇒ {
                            gotoTarget(
                                pc + instruction.asInstanceOf[IFNONNULL].branchoffset,
                                domain.addIsNonNullConstraint(
                                    operand,
                                    memoryLayout.update(pc, instruction)))

                            gotoTarget(
                                pcOfNextInstruction,
                                domain.addIsNullConstraint(
                                    operand,
                                    memoryLayout.update(pc, instruction)))
                        }
                    }
                }
                case 198 /*ifnull*/ ⇒ {
                    val operand = memoryLayout.operands.head
                    domain.isNull(operand) match {
                        case BooleanAnswer.YES ⇒
                            gotoTarget(
                                pc + instruction.asInstanceOf[IFNULL].branchoffset,
                                memoryLayout.update(pc, instruction))
                        case BooleanAnswer.NO ⇒
                            gotoTarget(
                                pcOfNextInstruction,
                                memoryLayout.update(pc, instruction))
                        case BooleanAnswer.UNKNOWN ⇒ {
                            gotoTarget(
                                pc + instruction.asInstanceOf[IFNULL].branchoffset,
                                domain.addIsNullConstraint(
                                    operand,
                                    memoryLayout.update(pc, instruction)))

                            gotoTarget(
                                pcOfNextInstruction,
                                domain.addIsNonNullConstraint(
                                    operand,
                                    memoryLayout.update(pc, instruction)))
                        }
                    }
                }

                case 172 /*ireturn*/
                    | 173 /*lreturn*/
                    | 174 /*freturn*/
                    | 175 /*dreturn*/
                    | 176 /*areturn*/
                    | 177 /*return*/ ⇒ memoryLayout.update(pc, instruction)

                case 191 /*athrow*/ ⇒
                    sys.error("well ... some support is needed")

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
