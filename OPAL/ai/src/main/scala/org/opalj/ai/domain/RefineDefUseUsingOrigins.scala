/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
package ai
package domain

import scala.annotation.switch

import org.opalj.collection.immutable.IntArraySet
import org.opalj.collection.immutable.IntArraySet1
import org.opalj.br.instructions._

/**
 * Collects the abstract interpretation time definition/use information.
 *
 * ===Reusability===
 * No; the underlying operands arrays is directly queried.
 *
 * @note ReturnAddressValues are ignored by this domain; however, the parent domain
 *       [[RecordDefUse]] has appropriate handling.
 *
 * @author Michael Eichberg
 */
trait RefineDefUseUsingOrigins extends RecordDefUse {
    defUseDomain: Domain with TheCode with Origin with TheMemoryLayout ⇒

    // Basically mirrors the "used" array defined by "RecordDefUse", but contains information
    // based on the origin information.
    // If used(<PC>) is ''null'', then we don't have any origin based def-use information.
    private[this] var used: Array[ValueOrigins] = _ // initialized by initProperties

    override protected[this] def thisProperty(pc: Int): Option[String] = {
        // Let's check if we have origin information; if so, let's use them...
        val usedBy = this.usedBy(pc)
        if (usedBy eq null)
            super.thisProperty(pc).map("DefUseBased"+_)
        else
            Some(usedBy.mkString("OriginBasedUsedBy={", ",", "}"))
    }

    override def operandOrigin(pc: PC, stackIndex: Int): ValueOrigins = {
        val operands = operandsArray(pc)
        if (operands ne null) {
            operands(stackIndex) match {
                case vo: ValueWithOriginInformation ⇒
                    vo.origins.foldLeft(IntArraySet.empty)(_ + _)
                case _ ⇒
                    super.operandOrigin(pc, stackIndex)
            }
        } else {
            null
        }
    }

    override def localOrigin(pc: PC, registerIndex: Int): ValueOrigins = {
        val locals = localsArray(pc)
        if (locals ne null) {
            locals(registerIndex) match {
                case vo: ValueWithOriginInformation ⇒
                    vo.origins.foldLeft(IntArraySet.empty)(_ + _)
                case _ ⇒
                    super.localOrigin(pc, registerIndex)
            }
        } else {
            null
        }
    }

    override def usedBy(valueOrigin: ValueOrigin): ValueOrigins = {
        val usedBy = used(valueOrigin + parametersOffset)
        if (usedBy ne null)
            usedBy
        else
            super.usedBy(valueOrigin)
    }

    /**
     * Completes the computation of the definition/use information by using the recorded cfg.
     */
    abstract override def abstractInterpretationEnded(
        aiResult: AIResult { val domain: defUseDomain.type }
    ): Unit = {
        super.abstractInterpretationEnded(aiResult)

        if (aiResult.wasAborted)
            return /* nothing to do */ ;

        val used = new Array[ValueOrigins](code.codeSize + parametersOffset)
        this.used = used

        // ... go over all use sites to collect them and to make the def => use information
        // available.
        code.iterate { (pc, instruction) ⇒

            val operands = operandsArray(pc)
            if (operands ne null) { // <=> the instruction was executed...

                // HELPER METHODS

                def processDomainValue(v: DomainValue): Unit = {
                    v match {
                        case v: ValueWithOriginInformation ⇒
                            v.origins.foreach { vo ⇒
                                val normalizedVO = vo + parametersOffset
                                val currentUsages = used(normalizedVO)
                                if (currentUsages eq null)
                                    used(normalizedVO) = new IntArraySet1(pc)
                                else
                                    used(normalizedVO) = currentUsages + pc
                            }

                        case _ ⇒
                        /* nothing to do; the domain value does not have origin information */
                    }
                }

                def usedOperands(count: Int) = operands.forFirstN(count)(processDomainValue)

                def usedLocal(index: Int) = processDomainValue(localsArray(pc)(index))

                // PROCESS THE INSTRUCTIONS

                (instruction.opcode: @switch) match {

                    case IF_ACMPEQ.opcode | IF_ACMPNE.opcode |
                        IF_ICMPEQ.opcode | IF_ICMPNE.opcode |
                        IF_ICMPGT.opcode | IF_ICMPGE.opcode |
                        IF_ICMPLT.opcode | IF_ICMPLE.opcode ⇒
                        usedOperands(2)

                    case IFNULL.opcode | IFNONNULL.opcode |
                        IFEQ.opcode | IFNE.opcode |
                        IFGT.opcode | IFGE.opcode | IFLT.opcode | IFLE.opcode |
                        LOOKUPSWITCH.opcode | TABLESWITCH.opcode ⇒
                        usedOperands(1)

                    case ATHROW.opcode                      ⇒ usedOperands(1)

                    case NEWARRAY.opcode | ANEWARRAY.opcode ⇒ usedOperands(1) // dimension
                    case ARRAYLENGTH.opcode                 ⇒ usedOperands(1) // array

                    case MULTIANEWARRAY.opcode ⇒
                        val dimensions = instruction.asInstanceOf[MULTIANEWARRAY].dimensions
                        usedOperands(dimensions)

                    case 50 /*aaload*/ |
                        49 /*daload*/ | 48 /*faload*/ |
                        51 /*baload*/ |
                        52 /*caload*/ | 46 /*iaload*/ | 47 /*laload*/ | 53 /*saload*/ ⇒
                        usedOperands(2)

                    case 83 /*aastore*/ |
                        84 /*bastore*/ |
                        85 /*castore*/ | 79 /*iastore*/ | 80 /*lastore*/ | 86 /*sastore*/ |
                        82 /*dastore*/ | 81 /*fastore*/ ⇒
                        usedOperands(3)

                    //
                    // FIELD ACCESS
                    //
                    case 180 /*getfield*/                           ⇒ usedOperands(1)
                    case 181 /*putfield*/                           ⇒ usedOperands(2)
                    case 179 /*putstatic*/                          ⇒ usedOperands(1)

                    //
                    // MONITOR
                    //

                    case 194 /*monitorenter*/ | 195 /*monitorexit*/ ⇒ usedOperands(1)

                    //
                    // METHOD INVOCATIONS
                    //
                    case 184 /*invokestatic*/ |
                        185 /*invokeinterface*/ | 183 /*invokespecial*/ | 182 /*invokevirtual*/ |
                        186 /*invokedynamic*/ ⇒
                        val invoke = instruction.asInvocationInstruction
                        usedOperands(
                            invoke.numberOfPoppedOperands(ComputationalTypeCategoryNotAvailable)
                        )

                    //
                    // RELATIONAL OPERATORS
                    //
                    case 148 /*lcmp*/ |
                        150 /*fcmpg*/ | 149 /*fcmpl*/ | 152 /*dcmpg*/ | 151 /*dcmpl*/ ⇒
                        usedOperands(2)

                    //
                    // UNARY EXPRESSIONS
                    //
                    case 116 /*ineg*/ | 117 /*lneg*/ | 119 /*dneg*/ | 118 /*fneg*/ ⇒
                        usedOperands(1)

                    //
                    // BINARY EXPRESSIONS
                    //
                    case IINC.opcode ⇒
                        val IINC(index, _) = instruction
                        usedLocal(index)

                    case 99 /*dadd*/ | 111 /*ddiv*/ | 107 /*dmul*/ | 115 /*drem*/ | 103 /*dsub*/ |
                        98 /*fadd*/ | 110 /*fdiv*/ | 106 /*fmul*/ | 114 /*frem*/ | 102 /*fsub*/ |
                        109 /*ldiv*/ | 105 /*lmul*/ | 113 /*lrem*/ | 101 /*lsub*/ | 97 /*ladd*/ |
                        96 /*iadd*/ | 108 /*idiv*/ | 104 /*imul*/ | 112 /*irem*/ | 100 /*isub*/ |
                        126 /*iand*/ | 128 /*ior*/ | 130 /*ixor*/ |
                        127 /*land*/ | 129 /*lor*/ | 131 /*lxor*/ |
                        120 /*ishl*/ | 122 /*ishr*/ | 124 /*iushr*/ |
                        121 /*lshl*/ | 123 /*lshr*/ | 125 /*lushr*/ ⇒
                        usedOperands(2)

                    //
                    // VALUE CONVERSIONS
                    //
                    case 144 /*d2f*/ | 142 /*d2i*/ | 143 /*d2l*/ |
                        141 /*f2d*/ | 139 /*f2i*/ | 140 /*f2l*/ |
                        145 /*i2b*/ | 146 /*i2c*/ | 147 /*i2s*/ |
                        135 /*i2d*/ | 134 /*i2f*/ | 133 /*i2l*/ |
                        138 /*l2d*/ | 137 /*l2f*/ | 136 /*l2i*/ |
                        193 /*instanceof*/ ⇒
                        usedOperands(1)

                    case CHECKCAST.opcode ⇒ usedOperands(1)

                    //
                    // xRETURN
                    //
                    case 176 /*a...*/ | 175 /*d...*/ | 174 /*f...*/ | 172 /*i...*/ | 173 /*l...*/ ⇒
                        usedOperands(1)

                    case _ ⇒ // nothing to do... no relevant use
                }
            }
        }
    }

}
