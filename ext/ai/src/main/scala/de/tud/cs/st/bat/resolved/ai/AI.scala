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
trait AI {

    /**
     * Called during the abstract interpretation of a method to determine whether
     * the computation should be aborted. When the abstract interpreter is currently
     * waiting on the result of the interpretation of a called method, it may take
     * some time before the interpretation of the current method is actually aborted.
     */
    def isInterrupted(): Boolean

    def tracer: Option[AITracer]

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
     *      memory layouts; i.e., the list of operands and locals. Each calculated
     *      memory layout represents the layout before the instruction with the
     *      corresponding program counter was interpreted.
     *      If the interpretation was aborted, the returned result
     *      object contains all necessary information to continue the interpretation
     *      if needed/desired.
     * @note
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

        assume(method.body.isDefined, "ai perform - the method ("+method.toJava+") has no body")

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
            initialLocals: Array[domain.DomainValue]): AIResult[domain.type] = {

        assume(code.maxLocals == initialLocals.size, "ai perform - code.maxLocals and initialLocals.size differ")

        import domain.DomainValueTag

        val currentLocalsArray = new Array[Array[domain.DomainValue]](code.instructions.length)
        currentLocalsArray(0) = initialLocals
        val currentOperandsArray = new Array[List[domain.DomainValue]](code.instructions.length)
        currentOperandsArray(0) = Nil

        continueInterpretation(
            code,
            domain)(
                /*worklist = */ List(0),
                currentOperandsArray,
                currentLocalsArray)
    }

    /**
     * Continues the interpretation of the given method implementation (code) using
     * the given domain.
     */
    def continueInterpretation(
        code: Code,
        domain: Domain)(
            currentWorklist: List[Int],
            currentOperandsArray: Array[List[domain.DomainValue]],
            currentLocalsArray: Array[Array[domain.DomainValue]]): AIResult[domain.type] = {

        import domain._

        type SingleValueDomainTest = (DomainValue) ⇒ Answer
        type TwoValuesDomainTest = (DomainValue, DomainValue) ⇒ Answer

        val instructions: Array[Instruction] = code.instructions

        var worklist = currentWorklist
        val operandsArray = currentOperandsArray
        val localsArray = currentLocalsArray

        def ifXX(
            pc: Int,
            instruction: Instruction,
            domainTest: SingleValueDomainTest,
            yesConstraint: SingleValueConstraint,
            noConstraint: SingleValueConstraint) {

            val branchTarget = pc + instruction.asInstanceOf[ConditionalBranchInstruction].branchoffset
            val nextPC = instructions(pc).indexOfNextInstruction(pc, code)
            val operand = operandsArray(pc).head
            val remainingOperands = operandsArray(pc).tail

            domainTest(operand) match {
                case Yes ⇒ gotoTarget(branchTarget, remainingOperands, localsArray(pc))
                case No  ⇒ gotoTarget(nextPC, remainingOperands, localsArray(pc))
                case Unknown ⇒ {
                    {
                        val (newOperands, newLocals) = yesConstraint(branchTarget, operand, remainingOperands, localsArray(pc))
                        gotoTarget(branchTarget, newOperands, newLocals)
                    }
                    {
                        val (newOperands, newLocals) = noConstraint(nextPC, operand, remainingOperands, localsArray(pc))
                        gotoTarget(nextPC, newOperands, newLocals)
                    }
                }
            }
        }

        def ifTcmpXX(
            pc: Int,
            instruction: Instruction,
            domainTest: TwoValuesDomainTest,
            yesConstraint: TwoValuesConstraint,
            noConstraint: TwoValuesConstraint) {

            val branchTarget = pc + instruction.asInstanceOf[ConditionalBranchInstruction].branchoffset
            val nextPC = instructions(pc).indexOfNextInstruction(pc, code)

            val value2 = operandsArray(pc).head
            var remainingOperands = operandsArray(pc).tail
            val value1 = remainingOperands.head
            remainingOperands = remainingOperands.tail

            domainTest(value1, value2) match {
                case Yes ⇒ gotoTarget(branchTarget, remainingOperands, localsArray(pc))
                case No  ⇒ gotoTarget(nextPC, remainingOperands, localsArray(pc))
                case Unknown ⇒ {
                    {
                        val (newOperands, newLocals) =
                            yesConstraint(branchTarget, value1, value2, remainingOperands, localsArray(pc))
                        gotoTarget(branchTarget, newOperands, newLocals)
                    }
                    {
                        val (newOperands, newLocals) =
                            noConstraint(nextPC, value1, value2, remainingOperands, localsArray(pc))
                        gotoTarget(nextPC, newOperands, newLocals)
                    }
                }
            }
        }

        def gotoTarget(targetPC: Int, operands: Operands, locals: Locals) {
            val currentOperands = operandsArray(targetPC)
            val currentLocals = localsArray(targetPC)

            if (currentOperands == null /* || localsArray(targetPC) == null )*/ ) {
                operandsArray(targetPC) = operands
                localsArray(targetPC) = locals
                worklist = targetPC :: worklist
            } else {
                try {
                    domain.merge(currentOperands, currentLocals, operands, locals) match {
                        case NoUpdate ⇒ /* Nothing to do */
                        case StructuralUpdate((updatedOperands, updatedLocals)) ⇒
                            worklist = targetPC :: worklist
                            operandsArray(targetPC) = updatedOperands
                            localsArray(targetPC) = updatedLocals
                        case MetaInformationUpdate((updatedOperands, updatedLocals)) ⇒
                            // => the evaluation context didn't change, hence
                            // it is not necessary to enqueue the instruction
                            operandsArray(targetPC) = updatedOperands
                            localsArray(targetPC) = updatedLocals
                    }
                } catch {
                    case ae: AssertionError ⇒
                        //                        val dump = util.Util.dump(None, None, code, operandsArray, localsArray, Some(ae.getMessage()+" targetPC: "+targetPC+" remaining worklist: "+worklist.mkString(", ")))
                        //                        util.Util.writeAndOpenDump(dump)
                        //                        println("Press enter to continue..."); System.in.read()
                        throw ae
                }
            }
        }

        def gotoTargets(targetPCs: Iterable[Int], operands: Operands, locals: Locals) {
            for (targetPC ← targetPCs) {
                gotoTarget(targetPC, operands, locals)
            }
        }

        /**
         * Handles the control-flow when an (new) exception was raised.
         *
         * Called when an exception was (potentially) raised during the interpretation
         * of the method. In this case the corresponding handler is searched and then
         * the control is transfered to it. If no handler is found the domain is
         * informed about an abnormal return.
         *
         * @note The operand stack will only contain the raised exception.
         * @param currentPC The program counter of the instruction that raised the
         *      exception.
         * @param exception A guaranteed non-null value that represents an instance of
         *      an object that inherits from `java.lang.Throwable`.
         */
        def handleException(currentPC: Int, exception: DomainTypedValue[ObjectType], locals: Locals) {
            val nextOperands: List[domain.DomainValue] = List(exception)
            val isHandled =
                // find the exception handler that matches the given exception
                code.exceptionHandlersFor(currentPC).exists(eh ⇒ {
                    val branchTarget = eh.handlerPC
                    val catchType = eh.catchType
                    if (catchType.isEmpty) { // this is a finally handler
                        gotoTarget(branchTarget, nextOperands, locals)
                        true
                    } else {
                        domain.isSubtypeOf(exception, catchType.get) match {
                            case No ⇒
                                false
                            case Yes ⇒
                                gotoTarget(branchTarget, nextOperands, locals)
                                true
                            case Unknown ⇒
                                val (updatedOperands, updatedLocals) =
                                    UpperBound(branchTarget, catchType.get, exception, nextOperands, locals)
                                gotoTarget(branchTarget, updatedOperands, updatedLocals)
                                false
                        }
                    }
                })

            // If "isHandled" is true, we are sure that at least one 
            // handler will catch the exception... hence the method
            // will not return abnormally
            if (!isHandled)
                domain.abnormalReturn(exception)
        }
        def handleExceptions(pc: Int, exceptions: Set[DomainTypedValue[ObjectType]], locals: Locals) {
            exceptions.foreach(handleException(pc, _, locals))
        }

        // -------------------------------------------------------------------------------
        //
        // Main loop of the abstract interpreter
        //
        // -------------------------------------------------------------------------------

        while (worklist.nonEmpty) {
            if (isInterrupted()) {
                return AIResultBuilder.aborted(code, domain)(worklist, operandsArray, localsArray)
            }

            val pc = worklist.head
            worklist = worklist.tail
            val instruction = instructions(pc)
            // the memory layout before executing the instruction with the given pc
            val operands = operandsArray(pc)
            val locals = localsArray(pc)

            tracer.map(_.traceInstructionEvalution(pc, instruction, operands, locals))

            def pcOfNextInstruction = code.indexOfNextInstruction(pc)

            def fallThrough() {
                gotoTarget(pcOfNextInstruction, operands, locals)
            }
            def fallThroughO(operands: Operands) {
                gotoTarget(pcOfNextInstruction, operands, locals)
            }
            def fallThroughOL(operands: Operands, locals: Locals) {
                gotoTarget(pcOfNextInstruction, operands, locals)
            }

            def handleStackBasedComputation(computation: Computation[DomainValue, Set[DomainTypedValue[ObjectType]]], rest: Operands) {
                if (computation.hasValue)
                    fallThroughO(computation.value :: rest)
                if (computation.throwsException)
                    handleExceptions(pc, computation.exceptions, locals)
            }

            (instruction.opcode: @annotation.switch) match {
                //
                // UNCONDITIONAL TRANSFER OF CONTROL
                //
                case 167 /*goto*/
                    | 200 /*goto_w*/ ⇒
                    val branchtarget = pc + instruction.asInstanceOf[UnconditionalBranchInstruction].branchoffset
                    gotoTarget(branchtarget, operands, locals)

                case 169 /*ret*/ ⇒
                    val lvIndex = instruction.asInstanceOf[RET].lvIndex
                    localsArray(pc)(lvIndex) match {
                        case ReturnAddressValue(returnAddress) ⇒
                            gotoTargets(returnAddress, operands, locals)
                        case _ ⇒
                            CodeError("the local variable ("+
                                lvIndex+
                                ") does not contain a return address value", code, lvIndex)
                    }
                case 168 /*jsr*/ ⇒
                    val branchtarget = pc + instruction.asInstanceOf[JSRInstruction].branchoffset
                    gotoTarget(branchtarget, domain.ReturnAddressValue(pcOfNextInstruction) :: operands, locals)
                case 201 /*jsr_w*/ ⇒
                    val branchtarget = pc + instruction.asInstanceOf[JSRInstruction].branchoffset
                    gotoTarget(branchtarget, domain.ReturnAddressValue(pcOfNextInstruction) :: operands, locals)

                //
                // CONDITIONAL TRANSFER OF CONTROL
                //

                case 165 /*if_acmpeq*/ ⇒
                    ifTcmpXX(pc, instruction,
                        areEqualReferences _, AreEqualReferences, AreNotEqualReferences)
                case 166 /*if_acmpne*/ ⇒
                    ifTcmpXX(pc, instruction,
                        areNotEqualReferences _, AreNotEqualReferences, AreEqualReferences)
                case 198 /*ifnull*/ ⇒
                    ifXX(pc, instruction, isNull _, IsNull, IsNonNull)
                case 199 /*ifnonnull*/ ⇒
                    ifXX(pc, instruction,
                        isNonNull _, IsNonNull, IsNull)

                case 159 /*if_icmpeq*/ ⇒
                    ifTcmpXX(pc, instruction,
                        areEqualIntegers _, AreEqualIntegers, AreNotEqualIntegers)
                case 160 /*if_icmpne*/ ⇒
                    ifTcmpXX(pc, instruction,
                        areNotEqualIntegers _, AreNotEqualIntegers, AreEqualIntegers)
                case 161 /*if_icmplt*/ ⇒
                    ifTcmpXX(pc, instruction,
                        isLessThan _, IsLessThan, IsGreaterThanOrEqualTo)
                case 162 /*if_icmpge*/ ⇒
                    ifTcmpXX(pc, instruction,
                        isGreaterThanOrEqualTo _, IsGreaterThanOrEqualTo, IsLessThan)
                case 163 /*if_icmpgt*/ ⇒
                    ifTcmpXX(pc, instruction,
                        isGreaterThan _, IsGreaterThan, IsLessThanOrEqualTo)
                case 164 /*if_icmple*/ ⇒
                    ifTcmpXX(pc, instruction,
                        isLessThanOrEqualTo _, IsLessThanOrEqualTo, IsGreaterThan)
                case 153 /*ifeq*/ ⇒
                    ifXX(pc, instruction, is0 _, Is0, IsNot0)
                case 154 /*ifne*/ ⇒
                    ifXX(pc, instruction, isNot0 _, IsNot0, Is0)
                case 155 /*iflt*/ ⇒
                    ifXX(pc, instruction, isLessThan0 _, IsLessThan0, IsGreaterThanOrEqualTo0)
                case 156 /*ifge*/ ⇒
                    ifXX(pc, instruction,
                        isGreaterThanOrEqualTo0 _, IsGreaterThanOrEqualTo0, IsLessThan0)
                case 157 /*ifgt*/ ⇒
                    ifXX(pc, instruction,
                        isGreaterThan0 _, IsGreaterThan0, IsLessThanOrEqualTo0)
                case 158 /*ifle */ ⇒
                    ifXX(pc, instruction,
                        isLessThanOrEqualTo0 _, IsLessThanOrEqualTo0, IsGreaterThan0)

                case 171 /*lookupswitch*/ ⇒
                    val switch = instructions(pc).asInstanceOf[LOOKUPSWITCH]
                    val index = operands.head
                    val remainingOperands = operands.tail
                    val firstKey = switch.npairs(0)._1
                    var previousKey = firstKey
                    var branchToDefaultRequired = false
                    for ((key, offset) ← switch.npairs) {
                        if (!branchToDefaultRequired && (key - previousKey) > 1) {
                            if ((previousKey until key).exists(v ⇒ domain.isValueInRange(index, v, v))) {
                                branchToDefaultRequired = true
                            } else {
                                previousKey = key
                            }
                        }
                        if (domain.isValueInRange(index, key, key)) {
                            val branchTarget = pc + offset
                            val (updatedOperands, updatedLocals) = domain.hasValue(branchTarget, key, index, remainingOperands, locals)
                            gotoTarget(branchTarget, updatedOperands, updatedLocals)
                        }
                    }
                    if (branchToDefaultRequired || domain.isValueNotInRange(index, firstKey, switch.npairs(switch.npairs.size - 1)._1)) {
                        gotoTarget(pc + switch.defaultOffset, remainingOperands, locals)
                    }

                case 170 /*tableswitch*/ ⇒
                    val tableswitch = instructions(pc).asInstanceOf[TABLESWITCH]
                    val index = operands.head
                    val remainingOperands = operands.tail
                    val low = tableswitch.low
                    val high = tableswitch.high
                    var v = low
                    while (v < high) {
                        if (domain.isValueInRange(index, v, v)) {
                            val branchTarget = pc + tableswitch.jumpOffsets(v - low)
                            val (updatedOperands, updatedLocals) = domain.hasValue(branchTarget, v, index, remainingOperands, locals)
                            gotoTarget(branchTarget, updatedOperands, updatedLocals)
                        }
                        v = v + 1
                    }
                    if (domain.isValueNotInRange(index, low, high)) {
                        gotoTarget(pc + tableswitch.defaultOffset, remainingOperands, locals)
                    }

                //
                // STATEMENTS THAT CAN CAUSE EXCEPTIONELL TRANSFER OF CONTROL FLOW
                // 

                case 191 /*athrow*/ ⇒
                    // In general, we either have a control flow to an exception handler 
                    // or we abort the method.
                    /* EXCERPT FROM THE SPEC:
                     * Within a class file the exception handlers for each method are 
                     * stored in a table. At runtime the Java virtual machine searches 
                     * the exception handlers of the current method in the order that 
                     * they appear in the corresponding exception handler table. 
                     */
                    val computation = domain.athrow(operandsArray(pc).head)
                    if (computation.throwsException) {
                        handleException(pc, computation.exceptions, locals)
                    }
                    if (computation.hasValue) {
                        val exception = computation.value
                        val nextOperands = List(exception)

                        domain.types(exception) match {
                            case ValuesUnknown ⇒
                                code.exceptionHandlersFor(pc).foreach(eh ⇒ {
                                    val branchTarget = eh.handlerPC
                                    // unless we have a "finally" handler, we can state
                                    // a constraint
                                    if (eh.catchType.isDefined) {
                                        eh.catchType.map(catchType ⇒ {
                                            val (updatedOperands, updatedLocals) =
                                                UpperBound(branchTarget, catchType, exception, nextOperands, locals)
                                            gotoTarget(branchTarget, updatedOperands, updatedLocals)
                                        })
                                    } else
                                        gotoTarget(branchTarget, nextOperands, locals)
                                })
                                domain.abnormalReturn(exception)

                            case Values(exceptionTypes) ⇒
                                val isHandled = exceptionTypes.forall(exceptionType ⇒
                                    // find the exception handler that matches the given 
                                    // exception
                                    code.exceptionHandlersFor(pc).exists(eh ⇒ {
                                        val branchTarget = eh.handlerPC
                                        val catchType = eh.catchType
                                        if (catchType.isEmpty) {
                                            gotoTarget(branchTarget, nextOperands, locals)
                                            // this is a finally handler
                                            true
                                        } else {
                                            domain.isSubtypeOf(exceptionType.asInstanceOf[ReferenceType], catchType.get) match {
                                                case No ⇒
                                                    false
                                                case Yes ⇒
                                                    gotoTarget(branchTarget, nextOperands, locals)
                                                    true
                                                case Unknown ⇒
                                                    val (updatedOperands, updatedLocals) =
                                                        UpperBound(branchTarget, catchType.get, exception, nextOperands, locals)
                                                    gotoTarget(branchTarget, updatedOperands, updatedLocals)
                                                    false
                                            }
                                        }
                                    }
                                    )
                                )
                                // If "isHandled" is true, we are sure that at least one 
                                // handler will catch the exception(s)... hence the method
                                // will not return abnormally
                                if (!isHandled)
                                    domain.abnormalReturn(exception)
                        }
                    }

                //
                // CREATE ARRAY
                //

                case 188 /*newarray*/ ⇒ {
                    val count :: rest = operands
                    val computation = ((instruction.asInstanceOf[NEWARRAY].atype: @annotation.switch) match {
                        case 4 /*BooleanType.atype*/  ⇒ domain.newarray(count, BooleanType)
                        case 5 /*CharType.atype*/     ⇒ domain.newarray(count, CharType)
                        case 6 /*FloatType.atype*/    ⇒ domain.newarray(count, FloatType)
                        case 7 /*DoubleType.atype*/   ⇒ domain.newarray(count, DoubleType)
                        case 8 /*ByteType.atype*/     ⇒ domain.newarray(count, ByteType)
                        case 9 /*ShortType.atype*/    ⇒ domain.newarray(count, ShortType)
                        case 10 /*IntegerType.atype*/ ⇒ domain.newarray(count, IntegerType)
                        case 11 /*LongType.atype*/    ⇒ domain.newarray(count, LongType)
                        case _                        ⇒ BATError("newarray of unsupported \"atype\"")
                    })
                    if (computation.hasValue) {
                        fallThroughO(computation.value :: rest)
                    }
                    if (computation.throwsException) {
                        handleException(pc, computation.exceptions, locals)
                    }
                }

                case 189 /*anewarray*/ ⇒ {
                    val count :: rest = operands
                    val computation = domain.newarray(count, instruction.asInstanceOf[ANEWARRAY].componentType)
                    if (computation.hasValue) {
                        fallThroughO(computation.value :: rest)
                    }
                    if (computation.throwsException) {
                        handleException(pc, computation.exceptions, locals)
                    }
                }

                case 197 /*multianewarray*/ ⇒ {
                    val multianewarray = instruction.asInstanceOf[MULTIANEWARRAY]
                    val initDimensions = operands.take(multianewarray.dimensions)
                    val computation = domain.multianewarray(initDimensions, multianewarray.componentType)
                    if (computation.hasValue) {
                        fallThroughO(computation.value :: (operands.drop(multianewarray.dimensions)))
                    }
                    if (computation.throwsException) {
                        handleException(pc, computation.exceptions, locals)
                    }
                }

                ///////////////////////// TODO [AI] WE NEED TO DEAL WITH EXCEPTIONELL CONTROL FLOW

                //
                // LOAD FROM AND STORE VALUE IN ARRAYS
                //

                case 50 /*aaload*/ ⇒ {
                    val index :: arrayref :: rest = operands
                    handleStackBasedComputation(domain.aaload(index, arrayref), rest)
                }
                case 83 /*aastore*/ ⇒ {
                    val value :: index :: arrayref :: rest = operands
                    domain.aastore(value, index, arrayref)
                    fallThroughO(rest) // <<<<<<===== TOTOTOTOTOTTOTOTOTOT RESULT
                }

                case 51 /*baload*/ ⇒ {
                    val index :: arrayref :: rest = operands
                    handleStackBasedComputation(domain.baload(index, arrayref), rest)
                }
                case 84 /*bastore*/ ⇒ {
                    val value :: index :: arrayref :: rest = operands
                    domain.bastore(value, index, arrayref)
                    fallThroughO(rest)
                }

                case 52 /*caload*/ ⇒ {
                    val index :: arrayref :: rest = operands
                    handleStackBasedComputation(domain.caload(index, arrayref), rest)
                }
                case 85 /*castore*/ ⇒ {
                    val value :: index :: arrayref :: rest = operands
                    domain.castore(value, index, arrayref)
                    fallThroughO(rest)
                }

                case 49 /*daload*/ ⇒ {
                    val index :: arrayref :: rest = operands
                    handleStackBasedComputation(domain.daload(index, arrayref), rest)
                }
                case 82 /*dastore*/ ⇒ {
                    val value :: index :: arrayref :: rest = operands
                    domain.dastore(value, index, arrayref)
                    fallThroughO(rest)
                }

                case 48 /*faload*/ ⇒ {
                    val index :: arrayref :: rest = operands
                    handleStackBasedComputation(domain.faload(index, arrayref), rest)
                }
                case 81 /*fastore*/ ⇒ {
                    val value :: index :: arrayref :: rest = operands
                    domain.fastore(value, index, arrayref)
                    fallThroughO(rest)
                }

                case 46 /*iaload*/ ⇒ {
                    val index :: arrayref :: rest = operands
                    handleStackBasedComputation(domain.iaload(index, arrayref), rest)
                }
                case 79 /*iastore*/ ⇒ {
                    val value :: index :: arrayref :: rest = operands
                    domain.iastore(value, index, arrayref)
                    fallThroughO(rest)
                }

                case 47 /*laload*/ ⇒ {
                    val index :: arrayref :: rest = operands
                    handleStackBasedComputation(domain.laload(index, arrayref), rest)
                }
                case 80 /*lastore*/ ⇒ {
                    val value :: index :: arrayref :: rest = operands
                    domain.lastore(value, index, arrayref)
                    fallThroughO(rest)
                }

                case 53 /*saload*/ ⇒ {
                    val index :: arrayref :: rest = operands
                    handleStackBasedComputation(domain.laload(index, arrayref), rest)
                }
                case 86 /*sastore*/ ⇒ {
                    val value :: index :: arrayref :: rest = operands
                    domain.sastore(value, index, arrayref)
                    fallThroughO(rest)
                }

                //
                // LENGTH OF AN ARRAY
                //

                case 190 /*arraylength*/ ⇒ {
                    val arrayref = operands.head
                    val computation = domain.arraylength(arrayref)
                    if (computation.hasValue) {
                        val newOperands = computation.value :: operands.tail
                        fallThroughO(newOperands)
                    }
                    if (computation.throwsException) {
                        handleException(pc, computation.exceptions, locals)
                    }
                }

                //

                //
                // ACCESSING FIELDS
                //
                case 180 /*getfield*/ ⇒ {
                    val getfield = instruction.asInstanceOf[GETFIELD]
                    fallThroughO(
                        domain.getfield(
                            operands.head,
                            getfield.declaringClass,
                            getfield.name,
                            getfield.fieldType) :: (operands.tail))
                }
                case 178 /*getstatic*/ ⇒ {
                    val getstatic = instruction.asInstanceOf[GETSTATIC]
                    fallThroughO(
                        domain.getstatic(
                            getstatic.declaringClass,
                            getstatic.name,
                            getstatic.fieldType) :: operands)
                }
                case 181 /*putfield*/ ⇒ {
                    val putfield = instruction.asInstanceOf[PUTFIELD]
                    val value :: objectref :: rest = operands
                    domain.putfield(
                        objectref,
                        value,
                        putfield.declaringClass,
                        putfield.name,
                        putfield.fieldType)
                    fallThroughO(rest)
                }
                case 179 /*putstatic*/ ⇒ {
                    val putstatic = instruction.asInstanceOf[PUTSTATIC]
                    val value :: rest = operands
                    domain.putstatic(
                        value,
                        putstatic.declaringClass,
                        putstatic.name,
                        putstatic.fieldType)
                    fallThroughO(rest)
                }

                //
                // METHOD INVOCATIONS
                //
                case 186 /*invokedynamic*/ ⇒
                    //                val invoke = instruction.asInstanceOf[INVOKEDYNAMIC]
                    //                val bootstrapMethod = invoke.bootstrapMethod
                    //                val bootbootstrapMethod.bootstrapArguments
                    //                //methodHandle.
                    sys.error("invokedynamic is not yet supported")

                case 185 /*invokeinterface*/ ⇒ {
                    val invoke = instruction.asInstanceOf[INVOKEINTERFACE]
                    val argsCount = invoke.methodDescriptor.parameterTypes.length
                    domain.invokeinterface(
                        invoke.declaringClass,
                        invoke.name,
                        invoke.methodDescriptor,
                        operands.take(argsCount + 1).reverse
                    ) match {
                            case Some(v) ⇒ fallThroughO(v :: (operands.drop(argsCount + 1)))
                            case None    ⇒ fallThroughO(operands.drop(argsCount + 1))
                        }
                }
                case 183 /*invokespecial*/ ⇒ {
                    val invoke = instruction.asInstanceOf[INVOKESPECIAL]
                    val argsCount = invoke.methodDescriptor.parameterTypes.length
                    domain.invokespecial(
                        invoke.declaringClass,
                        invoke.name,
                        invoke.methodDescriptor,
                        operands.take(argsCount + 1).reverse
                    ) match {
                            case Some(v) ⇒ fallThroughO(v :: (operands.drop(argsCount + 1)))
                            case None    ⇒ fallThroughO(operands.drop(argsCount + 1))
                        }
                }
                case 184 /*invokestatic*/ ⇒ {
                    val invoke = instruction.asInstanceOf[INVOKESTATIC]
                    val argsCount = invoke.methodDescriptor.parameterTypes.length
                    domain.invokestatic(
                        invoke.declaringClass,
                        invoke.name,
                        invoke.methodDescriptor,
                        operands.take(argsCount)
                    ) match {
                            case Some(v) ⇒ fallThroughO(v :: (operands.drop(argsCount)))
                            case None    ⇒ fallThroughO(operands.drop(argsCount))
                        }
                }
                case 182 /*invokevirtual*/ ⇒ {
                    val invoke = instruction.asInstanceOf[INVOKEVIRTUAL]
                    val argsCount = invoke.methodDescriptor.parameterTypes.length
                    domain.invokevirtual(
                        invoke.declaringClass,
                        invoke.name,
                        invoke.methodDescriptor,
                        operands.take(argsCount + 1)
                    ) match {
                            case Some(v) ⇒ fallThroughO(v :: (operands.drop(argsCount + 1)))
                            case None    ⇒ fallThroughO(operands.drop(argsCount + 1))
                        }
                }
                case 192 /*checkcast*/ ⇒ {
                    val objectref :: rest = operands
                    val newOperands = domain.checkcast(objectref, instruction.asInstanceOf[CHECKCAST].referenceType) :: rest
                    fallThroughO(newOperands)
                }

                case 194 /*monitorenter*/ ⇒ {
                    val computation = domain.monitorenter(operands.head)
                    if (computation.throwsException)
                        handleException(pc, computation.exceptions, locals)
                    if (computation.returnsNormally)
                        fallThroughO(operands.tail)
                }
                case 195 /*monitorexit*/ ⇒ {
                    val computation = domain.monitorexit(operands.head)
                    if (computation.throwsException)
                        handleException(pc, computation.exceptions, locals)
                    if (computation.returnsNormally)
                        fallThroughO(operands.tail)
                }

                //
                // RETURN FROM METHOD
                //
                case 176 /*areturn*/ ⇒ domain.areturn(operands.head)
                case 175 /*dreturn*/ ⇒ domain.dreturn(operands.head)
                case 174 /*freturn*/ ⇒ domain.freturn(operands.head)
                case 172 /*ireturn*/ ⇒ domain.ireturn(operands.head)
                case 173 /*lreturn*/ ⇒ domain.lreturn(operands.head)
                case 177 /*return*/  ⇒ domain.returnVoid()

                // -----------------------------------------------------------------------
                //
                // INSTRUCTIONS THAT FALL THROUGH / THAT DO NOT CONTROL THE 
                // CONTROL FLOW (WHICH WILL NEVER THROW AN EXCEPTION)
                //
                // -----------------------------------------------------------------------

                //
                // PUT LOCAL VARIABLE VALUE ONTO STACK
                //
                case 25 /*aload*/ ⇒
                    fallThroughO(locals(instruction.asInstanceOf[ALOAD].lvIndex) :: operands)
                case 24 /*dload*/ ⇒
                    fallThroughO(locals(instruction.asInstanceOf[DLOAD].lvIndex) :: operands)
                case 23 /*fload*/ ⇒
                    fallThroughO(locals(instruction.asInstanceOf[FLOAD].lvIndex) :: operands)
                case 21 /*iload*/ ⇒
                    fallThroughO(locals(instruction.asInstanceOf[ILOAD].lvIndex) :: operands)
                case 22 /*lload*/ ⇒
                    fallThroughO(locals(instruction.asInstanceOf[LLOAD].lvIndex) :: operands)
                case 42 /*aload_0*/
                    | 38 /*dload_0*/
                    | 34 /*fload_0*/
                    | 26 /*iload_0*/
                    | 30 /*lload_0*/ ⇒
                    fallThroughO(locals(0) :: operands)
                case 43 /*aload_1*/
                    | 39 /*dload_1*/
                    | 35 /*fload_1*/
                    | 27 /*iload_1*/
                    | 31 /*lload_1*/ ⇒
                    fallThroughO(locals(1) :: operands)
                case 44 /*aload_2*/
                    | 40 /*dload_2*/
                    | 36 /*fload_2*/
                    | 28 /*iload_2*/
                    | 32 /*lload_2*/ ⇒
                    fallThroughO(locals(2) :: operands)
                case 45 /*aload_3*/
                    | 41 /*dload_3*/
                    | 37 /*fload_3*/
                    | 29 /*iload_3*/
                    | 33 /*lload_3*/ ⇒
                    fallThroughO(locals(3) :: operands)

                //
                // STORE OPERAND IN LOCAL VARIABLE
                //
                case 58 /*astore*/ ⇒
                    fallThroughOL(operands.tail,
                        locals.updated(instruction.asInstanceOf[ASTORE].lvIndex, operands.head))
                case 57 /*dstore*/ ⇒
                    fallThroughOL(operands.tail,
                        locals.updated(instruction.asInstanceOf[DSTORE].lvIndex, operands.head))
                case 56 /*fstore*/ ⇒
                    fallThroughOL(operands.tail,
                        locals.updated(instruction.asInstanceOf[FSTORE].lvIndex, operands.head))
                case 54 /*istore*/ ⇒
                    fallThroughOL(operands.tail,
                        locals.updated(instruction.asInstanceOf[ISTORE].lvIndex, operands.head))
                case 55 /*lstore*/ ⇒
                    fallThroughOL(operands.tail,
                        locals.updated(instruction.asInstanceOf[LSTORE].lvIndex, operands.head))
                case 75 /*astore_0*/
                    | 71 /*dstore_0*/
                    | 67 /*fstore_0*/
                    | 63 /*lstore_0*/
                    | 59 /*istore_0*/ ⇒
                    fallThroughOL(operands.tail, locals.updated(0, operands.head))
                case 76 /*astore_1*/
                    | 72 /*dstore_1*/
                    | 68 /*fstore_1*/
                    | 64 /*lstore_1*/
                    | 60 /*istore_1*/ ⇒
                    fallThroughOL(operands.tail, locals.updated(1, operands.head))
                case 77 /*astore_2*/
                    | 73 /*dstore_2*/
                    | 69 /*fstore_2*/
                    | 65 /*lstore_2*/
                    | 61 /*istore_2*/ ⇒
                    fallThroughOL(operands.tail, locals.updated(2, operands.head))
                case 78 /*astore_3*/
                    | 74 /*dstore_3*/
                    | 70 /*fstore_3*/
                    | 66 /*lstore_3*/
                    | 62 /*istore_3*/ ⇒
                    fallThroughOL(operands.tail, locals.updated(3, operands.head))

                //
                // PUSH CONSTANT VALUE
                //

                case 1 /*aconst_null*/ ⇒
                    fallThroughO(domain.theNullValue :: operands)

                case 16 /*bipush*/ ⇒
                    fallThroughO(domain.byteValue(instruction.asInstanceOf[BIPUSH].value) :: operands)

                case 14 /*dconst_0*/ ⇒ fallThroughO(domain.doubleValue(0.0d) :: operands)
                case 15 /*dconst_1*/ ⇒ fallThroughO(domain.doubleValue(1.0d) :: operands)

                case 11 /*fconst_0*/ ⇒ fallThroughO(domain.floatValue(0.0f) :: operands)
                case 12 /*fconst_1*/ ⇒ fallThroughO(domain.floatValue(1.0f) :: operands)
                case 13 /*fconst_2*/ ⇒ fallThroughO(domain.floatValue(2.0f) :: operands)

                case 2 /*iconst_m1*/ ⇒ fallThroughO(domain.intValue(-1) :: operands)
                case 3 /*iconst_0*/  ⇒ fallThroughO(domain.intValue(0) :: operands)
                case 4 /*iconst_1*/  ⇒ fallThroughO(domain.intValue(1) :: operands)
                case 5 /*iconst_2*/  ⇒ fallThroughO(domain.intValue(2) :: operands)
                case 6 /*iconst_3*/  ⇒ fallThroughO(domain.intValue(3) :: operands)
                case 7 /*iconst_4*/  ⇒ fallThroughO(domain.intValue(4) :: operands)
                case 8 /*iconst_5*/  ⇒ fallThroughO(domain.intValue(5) :: operands)

                case 9 /*lconst_0*/  ⇒ fallThroughO(domain.longValue(0l) :: operands)
                case 10 /*lconst_1*/ ⇒ fallThroughO(domain.longValue(1l) :: operands)

                case 18 /*ldc*/ ⇒ {
                    instruction match {
                        case LoadInt(v)    ⇒ fallThroughO(domain.intValue(v) :: operands)
                        case LoadFloat(v)  ⇒ fallThroughO(domain.floatValue(v) :: operands)
                        case LoadString(v) ⇒ fallThroughO(domain.stringValue(v) :: operands)
                        case LoadClass(v)  ⇒ fallThroughO(domain.classValue(v) :: operands)
                    }
                }
                case 19 /*ldc_w*/ ⇒ {
                    instruction match {
                        case LoadInt_W(v)    ⇒ fallThroughO(domain.intValue(v) :: operands)
                        case LoadFloat_W(v)  ⇒ fallThroughO(domain.floatValue(v) :: operands)
                        case LoadString_W(v) ⇒ fallThroughO(domain.stringValue(v) :: operands)
                        case LoadClass_W(v)  ⇒ fallThroughO(domain.classValue(v) :: operands)
                    }
                }
                case 20 /*ldc2_w*/ ⇒ {
                    instruction match {
                        case LoadLong(v)   ⇒ fallThroughO(domain.longValue(v) :: operands)
                        case LoadDouble(v) ⇒ fallThroughO(domain.doubleValue(v) :: operands)
                    }
                }

                case 17 /*sipush*/ ⇒
                    fallThroughO(domain.shortValue(instruction.asInstanceOf[SIPUSH].value) :: operands)

                //
                // RELATIONAL OPERATORS
                //
                case 150 /*fcmpg*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.fcmpg(value1, value2) :: rest)
                }
                case 149 /*fcmpl*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.fcmpl(value1, value2) :: rest)
                }
                case 152 /*dcmpg*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.dcmpg(value1, value2) :: rest)
                }
                case 151 /*dcmpl*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.dcmpl(value1, value2) :: rest)
                }
                case 148 /*lcmp*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.lcmp(value1, value2) :: rest)
                }

                //
                // UNARY EXPRESSIONS
                //
                case 119 /*dneg*/ ⇒
                    fallThroughO(domain.dneg(operands.head) :: (operands.tail))
                case 118 /*fneg*/ ⇒
                    fallThroughO(domain.fneg(operands.head) :: (operands.tail))
                case 117 /*lneg*/ ⇒
                    fallThroughO(domain.lneg(operands.head) :: (operands.tail))
                case 116 /*ineg*/ ⇒
                    fallThroughO(domain.ineg(operands.head) :: (operands.tail))

                //
                // BINARY EXPRESSIONS
                //

                case 99 /*dadd*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughOL(domain.dadd(value1, value2) :: rest, locals)
                }
                case 111 /*ddiv*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughOL(domain.ddiv(value1, value2) :: rest, locals)
                }
                case 107 /*dmul*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughOL(domain.dmul(value1, value2) :: rest, locals)
                }
                case 115 /*drem*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughOL(domain.drem(value1, value2) :: rest, locals)
                }
                case 103 /*dsub*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughOL(domain.dsub(value1, value2) :: rest, locals)
                }

                case 98 /*fadd*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughOL(domain.fadd(value1, value2) :: rest, locals)
                }
                case 110 /*fdiv*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughOL(domain.fdiv(value1, value2) :: rest, locals)
                }
                case 106 /*fmul*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughOL(domain.fmul(value1, value2) :: rest, locals)
                }
                case 114 /*frem*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughOL(domain.frem(value1, value2) :: rest, locals)
                }
                case 102 /*fsub*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughOL(domain.fsub(value1, value2) :: rest, locals)
                }

                case 96 /*iadd*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughOL(domain.iadd(value1, value2) :: rest, locals)
                }
                case 126 /*iand*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughOL(domain.iand(value1, value2) :: rest, locals)
                }
                case 108 /*idiv*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    val computation = domain.idiv(value1, value2)
                    if (computation.hasValue)
                        fallThroughO(computation.value :: rest)
                    if (computation.throwsException)
                        handleException(pc, computation.exceptions, locals)
                }
                case 104 /*imul*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.imul(value1, value2) :: rest)
                }
                case 128 /*ior*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.ior(value1, value2) :: rest)
                }
                case 112 /*irem*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.irem(value1, value2) :: rest)
                }
                case 120 /*ishl*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.ishl(value1, value2) :: rest)
                }
                case 122 /*ishr*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.ishr(value1, value2) :: rest)
                }
                case 100 /*isub*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.isub(value1, value2) :: rest)
                }
                case 124 /*iushr*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.iushr(value1, value2) :: rest)
                }
                case 130 /*ixor*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.ixor(value1, value2) :: rest)
                }

                case 97 /*ladd*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.ladd(value1, value2) :: rest)
                }
                case 127 /*land*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.land(value1, value2) :: rest)
                }
                case 109 /*ldiv*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    val computation = domain.idiv(value1, value2)
                    if (computation.hasValue)
                        fallThroughO(computation.value :: rest)
                    if (computation.throwsException)
                        handleException(pc, computation.exceptions, locals)
                }
                case 105 /*lmul*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.lmul(value1, value2) :: rest)
                }
                case 129 /*lor*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.lor(value1, value2) :: rest)
                }
                case 113 /*lrem*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.lrem(value1, value2) :: rest)
                }
                case 121 /*lshl*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.lshl(value1, value2) :: rest)
                }
                case 123 /*lshr*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.lshr(value1, value2) :: rest)
                }
                case 101 /*lsub*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.lsub(value1, value2) :: rest)
                }
                case 125 /*lushr*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.lushr(value1, value2) :: rest)
                }
                case 131 /*lxor*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.lxor(value1, value2) :: rest)
                }
                //
                // GENERIC STACK MANIPULATION
                //
                case 89 /*dup*/ ⇒
                    fallThroughO((operands.head) :: operands)
                case 90 /*dup_x1*/ ⇒
                    val v1 :: v2 :: rest = operands
                    fallThroughO(v1 :: v2 :: v1 :: rest)
                case 91 /*dup_x2*/ ⇒ operands match {
                    case (v1 /*@ CTC1()*/ ) :: (v2 @ CTC1()) :: (v3 /*@ CTC1()*/ ) :: rest ⇒
                        fallThroughO(v1 :: v2 :: v3 :: v1 :: rest)
                    case (v1 /*@ CTC1()*/ ) :: v2 /* @ CTC2()*/ :: rest ⇒
                        fallThroughO(v1 :: v2 :: v1 :: rest)
                }
                case 92 /*dup2*/ ⇒ operands match {
                    case (v1 @ CTC1()) :: (v2 /*@ CTC1()*/ ) :: _ ⇒
                        fallThroughO(v1 :: v2 :: operands)
                    case (v /*@ CTC2()*/ ) :: _ ⇒
                        fallThroughO(v :: operands)
                }
                case 93 /*dup2_x1*/ ⇒ operands match {
                    case (v1 @ CTC1()) :: (v2 /*@ CTC1()*/ ) :: (v3 /*@ CTC1()*/ ) :: rest ⇒
                        fallThroughO(v1 :: v2 :: v3 :: v1 :: v2 :: rest)
                    case (v1 @ CTC2()) :: (v2 /*@ CTC1()*/ ) :: rest ⇒
                        fallThroughO(v1 :: v2 :: v1 :: rest)
                }
                case 94 /*dup2_x2*/ ⇒ operands match {
                    case (v1 @ CTC1()) :: (v2 @ CTC1()) :: (v3 @ CTC1()) :: (v4 /*@ CTC1()*/ ) :: rest ⇒
                        fallThroughO(v1 :: v2 :: v3 :: v4 :: v1 :: v2 :: rest)
                    case (v1 @ CTC2()) :: (v2 @ CTC1()) :: (v3 @ CTC1()) :: rest ⇒
                        fallThroughO(v1 :: v2 :: v3 :: v1 :: rest)
                    case (v1 @ CTC1()) :: (v2 @ CTC1()) :: (v3 @ CTC2()) :: rest ⇒
                        fallThroughO(v1 :: v2 :: v3 :: v1 :: v2 :: rest)
                    case (v1 /*@ CTC2()*/ ) :: (v2 /*@ CTC1()*/ ) :: rest ⇒
                        fallThroughO(v1 :: v2 :: v1 :: rest)
                }

                case 87 /*pop*/ ⇒ fallThroughO(operands.tail)
                case 88 /*pop2*/ ⇒ operands.head match {
                    case CTC1() ⇒ fallThroughO(operands.drop(2))
                    case CTC2() ⇒ fallThroughO(operands.tail)
                }

                case 95 /*swap*/ ⇒ {
                    val v1 :: v2 :: rest = operands
                    fallThroughOL(v2 :: v1 :: rest, locals)
                }

                //
                // TYPE CONVERSION
                //

                case 193 /*instanceof*/ ⇒ {
                    val objectref :: rest = operands
                    val newOperand = domain.instanceof(objectref, instruction.asInstanceOf[INSTANCEOF].referenceType)
                    fallThroughOL(newOperand :: rest, locals)
                }

                case 144 /*d2f*/ ⇒ fallThroughO(domain.d2f(operands.head) :: (operands.tail))
                case 142 /*d2i*/ ⇒ fallThroughO(domain.d2i(operands.head) :: (operands.tail))
                case 143 /*d2l*/ ⇒ fallThroughO(domain.d2l(operands.head) :: (operands.tail))

                case 141 /*f2d*/ ⇒ fallThroughO(domain.f2d(operands.head) :: (operands.tail))
                case 139 /*f2i*/ ⇒ fallThroughO(domain.f2i(operands.head) :: (operands.tail))
                case 140 /*f2l*/ ⇒ fallThroughO(domain.f2l(operands.head) :: (operands.tail))

                case 145 /*i2b*/ ⇒ fallThroughO(domain.i2b(operands.head) :: (operands.tail))
                case 146 /*i2c*/ ⇒ fallThroughO(domain.i2c(operands.head) :: (operands.tail))
                case 135 /*i2d*/ ⇒ fallThroughO(domain.i2d(operands.head) :: (operands.tail))
                case 134 /*i2f*/ ⇒ fallThroughO(domain.i2f(operands.head) :: (operands.tail))
                case 133 /*i2l*/ ⇒ fallThroughO(domain.i2l(operands.head) :: (operands.tail))
                case 147 /*i2s*/ ⇒ fallThroughO(domain.i2s(operands.head) :: (operands.tail))

                case 138 /*l2d*/ ⇒ fallThroughO(domain.l2d(operands.head) :: (operands.tail))
                case 137 /*l2f*/ ⇒ fallThroughO(domain.l2f(operands.head) :: (operands.tail))
                case 136 /*l2i*/ ⇒ fallThroughO(domain.l2i(operands.head) :: (operands.tail))

                //
                // "OTHER" INSTRUCTIONS
                //
                case 132 /*iinc*/ ⇒ {
                    val iinc = instruction.asInstanceOf[IINC]
                    val newValue = domain.iinc(locals(iinc.lvIndex), iinc.constValue)
                    fallThroughOL(operandsArray(pc), locals.updated(iinc.lvIndex, newValue))
                }

                case 187 /*new*/ ⇒ {
                    val newObject = instruction.asInstanceOf[NEW]
                    fallThroughO(domain.newObject(newObject.objectType) :: operands)
                }

                case 0 /*nop*/    ⇒ fallThrough()
                case 196 /*wide*/ ⇒ fallThrough()

                case opcode       ⇒ BATError("instruction with unsupported opcode: "+opcode)
            }
        }

        AIResultBuilder.complete(code, domain)(operandsArray, localsArray)
    }
}

/**
 * Base abstract interpreter usefull for testing and debugging purposes. The base
 * interpreter can be interrupted by calling the `interrupt` method of the AI's thread.
 */
object AI extends AI {

    def isInterrupted = Thread.interrupted()

    val tracer = Some(new ConsoleTracer {})
}

trait AITracer {

    def traceInstructionEvalution(pc: Int,
                                  instruction: Instruction,
                                  operands: List[_ <: AnyRef],
                                  locals: Array[_ <: AnyRef]): Unit
}

trait ConsoleTracer extends AITracer {
    def traceInstructionEvalution(pc: Int,
                                  instruction: Instruction,
                                  operands: List[_ <: AnyRef],
                                  locals: Array[_ <: AnyRef]): Unit = {
        println(pc+":"+instruction+" ["+operands+";"+locals+"]")
    }
}