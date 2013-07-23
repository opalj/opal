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
 * Entry point into the abstract interpreter.
 *
 * @author Michael Eichberg
 */
object AI {

    /**
     *  Performs an abstract interpretation of the given method with the given domain.
     *
     *  @param classFile The method's defining class file.
     *  @param method A non-native, non-abstrat method of the given class file that
     *      will be analyzed.
     *  @param domain The domain that will be used for the abstract interpretation.
     */
    def apply(
        classFile: ClassFile,
        method: Method,
        domain: Domain) = perform(classFile, method, domain)(None)

    /**
     * Analyzes the given method using the given domain and the pre-initialized parameter
     * values (if any).
     *
     * ==Controlling the AI==
     * To abort the abstract interpretation of a method just call the interpreter's
     * `Thread`'s `interrupt` method.
     *
     * @param classFile Some class file; needed to determine the type of `this` if
     *      the method is an instance method.
     * @param method A non-abstract, non-native method of the given class file; i.e.,
     *      a method with a body.
     * @param domain The abstract domain that is used to perform "abstract" calculations
     *      w.r.t. the domain's values. This framework assumes that the same domain
     *      instance is not used for the abstract interpretation of other methods.
     * @param someLocals If the values passed to a method are already known, the
     *      abstract interpretation will be performed under that assumption.
     * @return The result of the abstract interpretation. Basically, the calculated
     *      memory layouts. Each calculated memory layout represents
     *      the layout before the instruction with the corresponding program counter
     *      was interpreted. If the interpretation was aborted, the returned result
     *      object contains all necessary information to continue the interpretation
     *      if needed.
     *      ==Note==
     *      If you are just interested in the values that are returned or passed to other
     *      functions/fields it may be effective (code and performance wise) to implement
     *      your own domain that just records the values. For an example take a look at
     *      the test cases of BATAI.
     */
    def perform(
        classFile: ClassFile,
        method: Method,
        domain: Domain)(
            someLocals: Option[IndexedSeq[domain.DomainValue]] = None): AIResult[domain.type] = {

        assume(method.body.isDefined, "The method ("+method.toJava+") has no body.")

        import domain._

        val code = method.body.get
        val initialLocals = (
            someLocals.map(l ⇒ {
                assume(l.size == method.body.get.maxLocals)
                l.toArray
            }).getOrElse({
                var locals = new Array[domain.DomainValue](method.body.get.maxLocals)
                var localVariableIndex = 0

                if (!method.isStatic) {
                    val thisType = classFile.thisClass
                    locals = locals.updated(localVariableIndex, TypedValue(thisType))
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
        perform(code, domain)(initialLocals)
    }

    def perform(
        code: Code,
        domain: Domain)(
            initialLocals: IndexedSeq[domain.DomainValue]): AIResult[domain.type] = { // TODO [AI Performance] Figure out if it is worth using an Array instead of an IndexedSeq

        assume(code.maxLocals == initialLocals.size, "code.maxLocals and initialLocals.size differ")

        type MemoryLayout = ai.MemoryLayout[domain.type, domain.DomainValue]

        val memoryLayouts = new Array[MemoryLayout](code.instructions.length)
        memoryLayouts(0) = new MemoryLayout(domain, Nil, initialLocals)

        continueInterpretation(
            code,
            domain)(
                /*worklist = */ List(0),
                memoryLayouts)
    }

    def continueInterpretation(
        code: Code,
        domain: Domain)(
            currentWorklist: List[Int],
            currentMemoryLayouts: Array[MemoryLayout[domain.type, domain.DomainValue]]): AIResult[domain.type] = { // TODO [AI Performance] Figure out if it is worth using an Array instead of an IndexedSeq

        type MemoryLayout = ai.MemoryLayout[domain.type, domain.DomainValue]

        val instructions: Array[Instruction] = code.instructions
        val memoryLayouts = currentMemoryLayouts
        var worklist = currentWorklist
        var interpretedInstructions = 0; // to collect some statistics

        import domain._

        def update(memoryLayout: MemoryLayout,
                   pc: Int,
                   instruction: Instruction): MemoryLayout = {

            MemoryLayout.update(domain)(memoryLayout, pc, instruction)
        }

        def comparisonWithFixedValue(
            memoryLayout: MemoryLayout,
            pc: Int,
            instruction: Instruction,
            domainTest: (domain.DomainValue) ⇒ Answer,
            yesConstraint: (domain.DomainValue) ⇒ domain.ValueConstraint,
            noConstraint: (domain.DomainValue) ⇒ domain.ValueConstraint) {

            val branchTarget = pc + instruction.asInstanceOf[ConditionalBranchInstruction].branchoffset
            val nextPC = instructions(pc).indexOfNextInstruction(pc, code)

            val operand = memoryLayout.operands.head
            domainTest(operand) match {
                case Yes ⇒ gotoTarget(branchTarget, update(memoryLayout, pc, instruction))
                case No  ⇒ gotoTarget(nextPC, update(memoryLayout, pc, instruction))
                case Unknown ⇒ {
                    gotoTarget(
                        branchTarget,
                        domain.addConstraint(
                            yesConstraint(operand),
                            branchTarget,
                            update(memoryLayout, pc, instruction)))
                    gotoTarget(
                        nextPC,
                        domain.addConstraint(
                            noConstraint(operand),
                            nextPC,
                            update(memoryLayout, pc, instruction)))
                }
            }
        }

        def comparisonOfTwoValues(
            memoryLayout: MemoryLayout,
            pc: Int,
            instruction: Instruction,
            domainTest: (domain.DomainValue, domain.DomainValue) ⇒ Answer,
            yesConstraint: (domain.DomainValue, domain.DomainValue) ⇒ domain.ValueConstraint,
            noConstraint: (domain.DomainValue, domain.DomainValue) ⇒ domain.ValueConstraint) {

            val branchTarget = pc + instruction.asInstanceOf[ConditionalBranchInstruction].branchoffset
            val nextPC = instructions(pc).indexOfNextInstruction(pc, code)

            val value2 = memoryLayout.operands.head
            val value1 = memoryLayout.operands.tail.head
            domainTest(value1, value2) match {
                case Yes ⇒ gotoTarget(branchTarget, update(memoryLayout, pc, instruction))
                case No  ⇒ gotoTarget(nextPC, update(memoryLayout, pc, instruction))
                case Unknown ⇒ {
                    gotoTarget(
                        branchTarget,
                        domain.addConstraint(
                            yesConstraint(value1, value2),
                            branchTarget,
                            update(memoryLayout, pc, instruction)))
                    gotoTarget(
                        nextPC,
                        domain.addConstraint(
                            noConstraint(value1, value2),
                            nextPC,
                            update(memoryLayout, pc, instruction)))
                }
            }
        }

        def gotoTarget(nextPC: Int, nextPCMemoryLayout: MemoryLayout) {
            assume(nextPC < instructions.length, "interpretation beyond code boundary")

            if (memoryLayouts(nextPC) == null) {
                worklist = nextPC :: worklist
                memoryLayouts(nextPC) = nextPCMemoryLayout
            } else {
                {
                    val thisML = memoryLayouts(nextPC)
                    val nextML = nextPCMemoryLayout
                    MemoryLayout.merge(domain)(
                        thisML.operands, thisML.locals,
                        nextML.operands, nextML.locals)
                } match {
                    case NoUpdate ⇒ /* Nothing to do */
                    case StructuralUpdate(memoryLayout) ⇒ {
                        worklist = nextPC :: worklist
                        memoryLayouts(nextPC) = memoryLayout
                    }
                    case MetaInformationUpdate(memoryLayout) ⇒ {
                        // => the evaluation context didn't change, hence
                        // it is not necessary to enque the instruction
                        memoryLayouts(nextPC) = memoryLayout
                    }
                }
            }
        }
        def gotoTargets(nextPCs: Iterable[Int], nextPCMemoryLayout: MemoryLayout) {
            for (nextPC ← nextPCs) {
                gotoTarget(nextPC, nextPCMemoryLayout)
            }
        }

        //
        // Main loop of the abstract interpreter
        //

        while (worklist.nonEmpty) {
            if (Thread.interrupted()) {
                return AIResultBuilder.aborted(code, domain)(memoryLayouts, worklist)
            }

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
                    val branchtarget = pc + instruction.asInstanceOf[GOTO].branchoffset
                    gotoTarget(branchtarget, update(memoryLayout, pc, instruction))
                case 200 /*goto_w*/ ⇒
                    val branchtarget = pc + instruction.asInstanceOf[GOTO_W].branchoffset
                    gotoTarget(branchtarget, update(memoryLayout, pc, instruction))

                case 169 /*ret*/ ⇒
                    val lvIndex = instruction.asInstanceOf[RET].lvIndex
                    memoryLayout.locals(lvIndex) match {
                        case ReturnAddressValue(returnAddress) ⇒
                            gotoTargets(returnAddress, update(memoryLayout, pc, instruction))
                        case _ ⇒
                            CodeError("the local variable ("+
                                lvIndex+
                                ") does not contain a return address value", code, lvIndex)
                    }
                case 168 /*jsr*/ ⇒
                    val branchtarget = pc + instruction.asInstanceOf[JSR].branchoffset
                    gotoTarget(branchtarget, update(memoryLayout, pc, instruction))
                case 201 /*jsr_w*/ ⇒
                    val branchtarget = pc + instruction.asInstanceOf[JSR_W].branchoffset
                    gotoTarget(branchtarget, update(memoryLayout, pc, instruction))

                //
                // CONDITIONAL TRANSFER OF CONTROL
                //

                case 165 /*if_acmpeq*/ ⇒
                    comparisonOfTwoValues(memoryLayout, pc, instruction,
                        areEqualReferences _, AreEqualReferences, AreNotEqualReferences)
                case 166 /*if_acmpne*/ ⇒
                    comparisonOfTwoValues(memoryLayout, pc, instruction,
                        areNotEqualReferences _, AreNotEqualReferences, AreEqualReferences)
                case 198 /*ifnull*/ ⇒
                    comparisonWithFixedValue(memoryLayout, pc, instruction,
                        isNull _, IsNull, IsNonNull)
                case 199 /*ifnonnull*/ ⇒
                    comparisonWithFixedValue(memoryLayout, pc, instruction,
                        isNonNull _, IsNonNull, IsNull)

                case 159 /*if_icmpeq*/ ⇒
                    comparisonOfTwoValues(memoryLayout, pc, instruction,
                        areEqualIntegers _, AreEqualIntegers, AreNotEqualIntegers)
                case 160 /*if_icmpne*/ ⇒
                    comparisonOfTwoValues(memoryLayout, pc, instruction,
                        areNotEqualIntegers _, AreNotEqualIntegers, AreEqualIntegers)
                case 161 /*if_icmplt*/ ⇒
                    comparisonOfTwoValues(memoryLayout, pc, instruction,
                        isLessThan _, IsLessThan, IsGreaterThanOrEqualTo)
                case 162 /*if_icmpge*/ ⇒
                    comparisonOfTwoValues(memoryLayout, pc, instruction,
                        isGreaterThanOrEqualTo _, IsGreaterThanOrEqualTo, IsLessThan)
                case 163 /*if_icmpgt*/ ⇒
                    comparisonOfTwoValues(memoryLayout, pc, instruction,
                        isGreaterThan _, IsGreaterThan, IsLessThanOrEqualTo)
                case 164 /*if_icmple*/ ⇒
                    comparisonOfTwoValues(memoryLayout, pc, instruction,
                        isLessThanOrEqualTo _, IsLessThanOrEqualTo, IsGreaterThan)
                case 153 /*ifeq*/ ⇒
                    comparisonWithFixedValue(memoryLayout, pc, instruction,
                        is0 _, Is0, IsNot0)
                case 154 /*ifne*/ ⇒
                    comparisonWithFixedValue(memoryLayout, pc, instruction,
                        isNot0 _, IsNot0, Is0)
                case 155 /*iflt*/ ⇒
                    comparisonWithFixedValue(memoryLayout, pc, instruction,
                        isLessThan0 _, IsLessThan0, IsGreaterThanOrEqualTo0)
                case 156 /*ifge*/ ⇒
                    comparisonWithFixedValue(memoryLayout, pc, instruction,
                        isGreaterThanOrEqualTo0 _, IsGreaterThanOrEqualTo0, IsLessThan0)
                case 157 /*ifgt*/ ⇒
                    comparisonWithFixedValue(memoryLayout, pc, instruction,
                        isGreaterThan0 _, IsGreaterThan0, IsLessThanOrEqualTo0)
                case 158 /*ifle */ ⇒
                    comparisonWithFixedValue(memoryLayout, pc, instruction,
                        isLessThanOrEqualTo0 _, IsLessThanOrEqualTo0, IsGreaterThan0)

                //
                //            case 171 /*lookupswitch*/
                //               | 170 /*tableswitch*/ ⇒ new MemoryLayout(operands.tail, locals)
                //

                //
                // STATEMENTS THAT CAN CAUSE EXCEPTIONELL TRANSFER OF CONTROL FLOW
                // 

                // TODO[AI] case ... IDIV        
                // TODO[AI] case ... INVOKE

                case 191 /*athrow*/ ⇒
                    BATError("throws are not yet supported")

                //
                // RETURN INSTRUCTIONS
                //

                case 172 /*ireturn*/
                    | 173 /*lreturn*/
                    | 174 /*freturn*/
                    | 175 /*dreturn*/
                    | 176 /*areturn*/
                    | 177 /*return*/ ⇒ update(memoryLayout, pc, instruction)

                // 
                // INSTRUCTIONS THAT FALL THROUGH / THAT DO NOT CONTROL THE 
                // CONTROL FLOW
                //
                case _ ⇒ {
                    val nextPC = pcOfNextInstruction
                    val nextMemoryLayout = update(memoryLayout, pc, instruction)
                    gotoTarget(nextPC, nextMemoryLayout)
                }
            }
            interpretedInstructions += 1;
        }

        AIResultBuilder.complete(code, domain)(memoryLayouts)
    }
}

/*
 * Design:
 * Here, we use a builder to construct a Result object in two steps. This is necessary
 * to correctly type the `MemoryLayout` structure, which depends on the given domain. 
 */
object AIResultBuilder {

    type MemoryLayout[D <: Domain] = ai.MemoryLayout[D, D#DomainValue]

    def aborted[D <: Domain](
        theCode: Code,
        theDomain: D): (Array[MemoryLayout[theDomain.type]], List[Int]) ⇒ AIResult[theDomain.type] = {

        (theMemoryLayouts: Array[MemoryLayout[theDomain.type]], theWorkList: List[Int]) ⇒
            new AIAborted[theDomain.type](theCode, theDomain) {
                def workList = theWorkList
                def memoryLayouts = theMemoryLayouts
                def continueInterpretation(): AIResult[domain.type] = {
                    AI.continueInterpretation(code, domain)(workList, theMemoryLayouts)
                }
            }
    }

    def complete[D <: Domain](
        theCode: Code,
        theDomain: D): (Array[MemoryLayout[theDomain.type]]) ⇒ AIResult[theDomain.type] = {

        (theMemoryLayouts: Array[MemoryLayout[theDomain.type]]) ⇒
            new AICompleted[theDomain.type](theCode, theDomain) {
                def memoryLayouts = theMemoryLayouts
                def restartInterpretation(): AIResult[theDomain.type] = {
                    AI.continueInterpretation(code, domain)(workList, theMemoryLayouts)
                }
            }
    }
}

/*
 * Design:
 * We use an explicit type parameter to avoid a path dependency on a concrete AIResult
 * instance. I.e., if we remove the type parameter and redefine the method memoryLayouts
 * to "memoryLayouts: IndexedSeq[MemoryLayout[domain.type, domain.DomainValue]]" 
 * we would introduce a path dependence to a particular AIResult's instance and the actual 
 * type would be "this.domain.type" and "this.domain.DomainValue". 
 */
sealed abstract class AIResult[D <: Domain](val code: Code, val domain: D) {
    def memoryLayouts: IndexedSeq[MemoryLayout[D, D#DomainValue]]
    def workList: List[Int]
    def wasAborted: Boolean

    type BoundAIResult = AIResult[domain.type]
}

abstract class AIAborted[D <: Domain](code: Code, domain: D) extends AIResult(code, domain) {
    final def wasAborted: Boolean = true
    def continueInterpretation(): BoundAIResult
}

abstract class AICompleted[D <: Domain](code: Code, domain: D) extends AIResult(code, domain) {
    final def wasAborted: Boolean = false
    def workList: List[Int] = List(0)
    def restartInterpretation(): BoundAIResult
}
