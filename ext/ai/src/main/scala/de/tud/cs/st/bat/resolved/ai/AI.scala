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

import de.tud.cs.st.util.{ Answer, Yes, No, Unknown }

/**
 * A highly-configurable abstract interpreter for BAT's resolved
 * representation of Java bytecode.
 *
 * ==Thread Safety==
 * This class is thread-safe as long as the ai tracer (if any) and the used domain
 * is thread-safe.
 * Hence, it is possible to use a single instance to analyze multiple methods in parallel.
 * However, if you want to be able to selectively abort the abstract interpretation
 * of some methods or selectively trace the interpretation of some methods, then you
 * should use multiple instances.
 *
 * @define UseOfDomain
 *     BATAI does not make assumptions about the number of domain objects that
 *     are used. However, if a domain is used by multiple abstract interpreters,
 *     the domain has to be thread-safe.
 *
 * @author Michael Eichberg
 */
trait AI {

    /**
     * Called during the abstract interpretation of a method to determine whether
     * the computation should be aborted. This method is always called before the
     * evaluation of an instruction.
     *
     * @note When the abstract interpreter is currently waiting on the result of the
     *    interpretation of a called method, it may take some time before the
     *    interpretation of the current method (this abstract interpreter) is actually
     *    aborted.
     */
    def isInterrupted: Boolean

    /**
     * Returns the object that is used by BATAI for tracing the abstract interpretation.
     */
    def tracer: Option[AITracer]

    /**
     *  Performs an abstract interpretation of the given method with the given domain.
     *
     *  @param classFile The method's defining class file.
     *  @param method A non-native, non-abstract method of the given class file that
     *      will be analyzed.
     *  @param domain The domain that will be used for the abstract interpretation.
     *  @note $UseOfDomain
     */
    def apply(
        classFile: ClassFile,
        method: Method,
        domain: Domain[_]) = perform(classFile, method, domain)(None)

    /**
     * The worklist that is used when the analysis of a method starts.
     */
    private final val initialWorkList = List(0)

    /**
     * Analyzes the given method using the given domain and the pre-initialized parameter
     * values (if any).
     *
     * ==Controlling the AI==
     * The abstract interpretation of a method is aborted it the AI's `isInterrupted`
     * method returns true. That method is called directly before an instruction
     * is evaluated.
     *
     * @param classFile Some class file; needed to determine the type of `this` if
     *      the method is an instance method.
     * @param method A non-abstract, non-native method of the given class file; i.e.,
     *      a method with a body.
     * @param domain The abstract domain that is used to perform "abstract" calculations
     *      w.r.t. the domain's values.
     * @param someLocals If the values passed to a method are already known, the
     *      abstract interpretation will be performed under that assumption.
     * @return The result of the abstract interpretation. Basically, the calculated
     *      memory layouts; i.e., the list of operands and local variable before each
     *      instruction. Each calculated memory layout represents the layout before
     *      the instruction with the corresponding program counter was interpreted.
     *      If the interpretation was aborted, the returned result
     *      object contains all necessary information to continue the interpretation
     *      if needed/desired.
     * @note $UseOfDomain
     * @note If you are just interested in the values that are returned or passed to other
     *      functions/fields it may be effective (code and performance wise) to implement
     *      your own domain that just records the values. For an example take a look at
     *      the test cases of BATAI.
     */
    def perform(
        classFile: ClassFile,
        method: Method,
        domain: Domain[_])(
            someLocals: Option[IndexedSeq[domain.DomainValue]] = None): AIResult[domain.type] = {

        assume(method.body.isDefined, "ai perform - the method ("+method.toJava+") has no body")

        import domain._
        import domain.DomainValueTag

        val code = method.body.get
        val codeLength = code.instructions.length

        val initialLocals = (
            someLocals.map(l ⇒ {
                assume(l.size == method.body.get.maxLocals)
                l.toArray
            }).getOrElse({
                val locals = new Array[domain.DomainValue](method.body.get.maxLocals)
                var localVariableIndex = 0

                if (!method.isStatic) {
                    val thisType = classFile.thisClass
                    val thisValue = TypedValue(thisType)
                    locals.update(localVariableIndex, thisValue)
                    localVariableIndex += 1 /*==thisType.computationalType.operandSize*/
                }

                for (parameterType ← method.descriptor.parameterTypes) {
                    val ct = parameterType.computationalType
                    locals.update(localVariableIndex, TypedValue(parameterType))
                    localVariableIndex += ct.operandSize
                }

                locals
            })
        )

        val operandsArray = new Array[List[domain.DomainValue]](codeLength)
        val localsArray = new Array[Array[domain.DomainValue]](codeLength)
        if (!method.isStatic) {
            val (updatedOperands, updatedLocals) = domain.establishIsNonNull(0, initialLocals(0), List.empty[DomainValue], initialLocals)
            operandsArray(0) = updatedOperands
            localsArray(0) = updatedLocals
        } else {
            operandsArray(0) = List.empty[DomainValue]
            localsArray(0) = initialLocals
        }

        continueInterpretation(code, domain)(
            initialWorkList, operandsArray, localsArray)
    }

    /**
     * Continues the interpretation of the given method implementation (code) using
     * the given domain.
     *
     * @param initialWorklist The list of program counters with which the interpretation
     *      will continue. If the method was never analyzed before, the list should just
     *      contain the value "0"; i.e., we start with the interpretation of the
     *      first instruction.
     * @param operandsArray The array that contains the operand stacks. Each value
     *      in the array contains the operand stack before the instruction with the
     *      corresponding index is executed. This array can be empty except of the
     *      indexes that are referred to by the `initialWorklist`.
     *      '''The `operandsArray` data structure is mutated.'''
     * @param localsArray The array that contains the local variable assignments.
     *      Each value in the array contains the local variable assignments before
     *      the instruction with the corresponding program counter is executed.
     *      '''The `localsArray` data structure is mutated.'''
     * @note $UseOfDomain
     */
    def continueInterpretation(
        code: Code,
        domain: Domain[_])(
            initialWorkList: List[Int],
            operandsArray: Array[List[domain.DomainValue]],
            localsArray: Array[Array[domain.DomainValue]]): AIResult[domain.type] = {

        import domain._
        type SingleValueDomainTest = (DomainValue) ⇒ Answer
        type TwoValuesDomainTest = (DomainValue, DomainValue) ⇒ Answer

        val instructions: Array[Instruction] = code.instructions

        // The entire state of the computation is (from the perspective of the AI)
        // encapsulated by the following three data-structures:
        /* 1 */ // operandsArray
        /* 2 */ // localsArray
        /* 3 */ var worklist = initialWorkList

        /**
         * Prepares the AI to continue the interpretation with the instruction
         * at the given target (`targetPC`). Basically, the operand stack
         * and the local variables are updated with the given ones and the
         * target program counter is added to the `workList`.
         */
        def gotoTarget(targetPC: Int, operands: Operands, locals: Locals) {
            val currentOperands = operandsArray(targetPC)
            if (currentOperands == null /* || localsArray(targetPC) == null )*/ ) {
                // we analyze the instruction for the first time ...
                operandsArray(targetPC) = operands
                localsArray(targetPC) = locals
                worklist = targetPC :: worklist
            } else {
                val currentLocals = localsArray(targetPC)
                val mergeResult = domain.merge(
                    targetPC, currentOperands, currentLocals, operands, locals
                )
                if (tracer.isDefined)
                    tracer.get.merge[domain.type](
                        targetPC, currentOperands, currentLocals, operands, locals,
                        mergeResult
                    )
                mergeResult match {
                    case NoUpdate ⇒ /* Nothing to do */
                    case StructuralUpdate((updatedOperands, updatedLocals)) ⇒
                        operandsArray(targetPC) = updatedOperands
                        localsArray(targetPC) = updatedLocals
                        worklist = targetPC :: worklist
                    case MetaInformationUpdate((updatedOperands, updatedLocals)) ⇒
                        operandsArray(targetPC) = updatedOperands
                        localsArray(targetPC) = updatedLocals
                    // => the evaluation context didn't change, hence
                    // it is not necessary to enqueue the instruction's pc
                }
            }
        }

        def gotoTargets(targetPCs: Iterable[Int], operands: Operands, locals: Locals) {
            targetPCs.foreach(gotoTarget(_, operands, locals))
        }

        // -------------------------------------------------------------------------------
        //
        // Main loop of the abstract interpreter
        //
        // -------------------------------------------------------------------------------

        while (worklist.nonEmpty) {
            if (isInterrupted) {
                return AIResultBuilder.aborted(
                    code,
                    domain)(
                        worklist,
                        operandsArray,
                        localsArray)
            }

            val pc = worklist.head
            worklist = worklist.tail
            val instruction = instructions(pc)
            // the memory layout before executing the instruction with the given pc
            val operands = operandsArray(pc)
            val locals = localsArray(pc)

            if (tracer.isDefined)
                tracer.get.instructionEvalution[domain.type](
                    domain, pc, instruction, operands, locals
                )

            def pcOfNextInstruction = code.indexOfNextInstruction(pc)

            /**
             * Handles all '''if''' instructions that perform a comparison with a fixed
             * value.
             */
            def ifXX(domainTest: SingleValueDomainTest,
                     yesConstraint: SingleValueConstraint,
                     noConstraint: SingleValueConstraint) {

                val branchInstruction = instruction.asInstanceOf[ConditionalBranchInstruction]
                val operand = operands.head
                val rest = operands.tail
                val nextPC = pcOfNextInstruction
                val branchTarget = pc + branchInstruction.branchoffset

                domainTest(operand) match {
                    case Yes ⇒ gotoTarget(branchTarget, rest, locals)
                    case No  ⇒ gotoTarget(nextPC, rest, locals)
                    case Unknown ⇒ {
                        {
                            val (newOperands, newLocals) =
                                yesConstraint(branchTarget, operand, rest, locals)
                            gotoTarget(branchTarget, newOperands, newLocals)
                        }
                        {
                            val (newOperands, newLocals) =
                                noConstraint(nextPC, operand, rest, locals)
                            gotoTarget(nextPC, newOperands, newLocals)
                        }
                    }
                }
            }

            /**
             * Handles all '''if''' instructions that perform a comparison of two
             * stack based values.
             */
            def ifTcmpXX(domainTest: TwoValuesDomainTest,
                         yesConstraint: TwoValuesConstraint,
                         noConstraint: TwoValuesConstraint) {

                val branchInstruction = instruction.asInstanceOf[ConditionalBranchInstruction]
                val value2 = operands.head
                var remainingOperands = operands.tail
                val value1 = remainingOperands.head
                remainingOperands = remainingOperands.tail
                val branchTarget = pc + branchInstruction.branchoffset
                val nextPC = code.indexOfNextInstruction(pc)

                domainTest(value1, value2) match {
                    case Yes ⇒ gotoTarget(branchTarget, remainingOperands, locals)
                    case No  ⇒ gotoTarget(nextPC, remainingOperands, locals)
                    case Unknown ⇒ {
                        {
                            val (newOperands, newLocals) =
                                yesConstraint(branchTarget, value1, value2, remainingOperands, locals)
                            gotoTarget(branchTarget, newOperands, newLocals)
                        }
                        {
                            val (newOperands, newLocals) =
                                noConstraint(nextPC, value1, value2, remainingOperands, locals)
                            gotoTarget(nextPC, newOperands, newLocals)
                        }
                    }
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
             *
             * @param exception A guaranteed non-null value that represents an instance of
             *      an object that is a subtype of `java.lang.Throwable`.
             */
            def handleException(exceptionValue: DomainValue) {
                val nextOperands: List[domain.DomainValue] = List(exceptionValue)
                val isHandled =
                    // find the exception handler that matches the given exception
                    code.exceptionHandlersFor(pc).exists(eh ⇒ {
                        val branchTarget = eh.handlerPC
                        val catchType = eh.catchType
                        if (catchType.isEmpty) { // this is a finally handler
                            gotoTarget(branchTarget, nextOperands, locals)
                            true
                        } else {
                            types(exceptionValue) match {
                                case exceptions: IsReferenceType ⇒
                                    exceptions.forallTypes(
                                        (exceptionType: Type) ⇒
                                            domain.isSubtypeOf(exceptionType.asObjectType, catchType.get) match {
                                                case No ⇒
                                                    false
                                                case Yes ⇒
                                                    gotoTarget(branchTarget, nextOperands, locals)
                                                    true
                                                case Unknown ⇒
                                                    val (updatedOperands, updatedLocals) =
                                                        establishUpperBound(branchTarget, catchType.get, exceptionValue, nextOperands, locals)
                                                    gotoTarget(branchTarget, updatedOperands, updatedLocals)
                                                    false
                                            }
                                    )
                                case _ ⇒
                                    AIImplementationError("handleException requires concrete exception values - given: "+exceptionValue)

                            }
                        }
                    })

                // If "isHandled" is true, we are sure that at least one 
                // handler will catch the exception... hence the method
                // will not return abnormally w.r.t. the given exception
                if (!isHandled)
                    abnormalReturn(pc, exceptionValue)
            }
            def handleExceptions(exceptions: Set[DomainValue]) {
                exceptions.foreach(handleException(_))
            }

            def abnormalReturn(pc: Int, exception: DomainValue) {
                if (tracer.isDefined)
                    tracer.get.abnormalReturn[domain.type](pc, exception)

                domain.abnormalReturn(pc, exception)
            }

            def fallThrough() {
                gotoTarget(pcOfNextInstruction, operands, locals)
            }
            def fallThroughO(operands: Operands) {
                gotoTarget(pcOfNextInstruction, operands, locals)
            }
            def fallThroughOL(operands: Operands, locals: Locals) {
                gotoTarget(pcOfNextInstruction, operands, locals)
            }

            def computationWithException(
                computation: Computation[Nothing, DomainValue],
                rest: Operands) {

                if (computation.throwsException)
                    handleException(computation.exceptions)
                if (computation.returnsNormally)
                    fallThroughO(rest)
            }

            def computationWithExceptions(
                computation: Computation[Nothing, Set[DomainValue]],
                rest: Operands) {

                if (computation.throwsException)
                    handleExceptions(computation.exceptions)
                if (computation.returnsNormally)
                    fallThroughO(rest)
            }

            def computationWithReturnValueAndException(
                computation: Computation[DomainValue, DomainValue],
                rest: Operands) {

                if (computation.hasResult)
                    fallThroughO(computation.result :: rest)
                if (computation.throwsException)
                    handleException(computation.exceptions)
            }

            def computationWithReturnValueAndExceptions(
                computation: Computation[DomainValue, Set[DomainValue]],
                rest: Operands) {

                if (computation.hasResult)
                    fallThroughO(computation.result :: rest)
                if (computation.throwsException)
                    handleExceptions(computation.exceptions)
            }

            def computationWithOptionalReturnValueAndExceptions(
                computation: Computation[Option[DomainValue], Set[DomainValue]],
                rest: Operands) {

                if (computation.hasResult) {
                    computation.result match {
                        case Some(value) ⇒ fallThroughO(value :: rest)
                        case None        ⇒ fallThroughO(rest)
                    }
                }
                if (computation.throwsException)
                    handleExceptions(computation.exceptions)
            }

            /**
             * Copies the current locals variable array and updates the local variable
             * stored at the given `index` in the new locals variable array with
             * the given `domainValue`.
             *
             * @param lvIndex A valid index in the locals variable array.
             * @param domainValue A domain value.
             * @return The updated locals variable array.
             */
            def updateLocals(lvIndex: Int, domainValue: DomainValue): Locals = {
                val newLocals = locals.clone
                newLocals.update(lvIndex, domainValue)
                newLocals
            }

            (instruction.opcode: @annotation.switch) match {
                //
                // UNCONDITIONAL TRANSFER OF CONTROL
                //
                case 167 /*goto*/
                    | 200 /*goto_w*/ ⇒
                    val branchtarget = pc + instruction.asInstanceOf[UnconditionalBranchInstruction].branchoffset
                    gotoTarget(branchtarget, operands, locals)

                // FIXME The handling of JSR/RET needs to be revised. Merging the operand stacks/local variables is not meaningful! I.e., we need to reset them!
                case 169 /*ret*/ ⇒
                    val lvIndex = instruction.asInstanceOf[RET].lvIndex
                    locals(lvIndex) match {
                        case ReturnAddressValue(returnAddress) ⇒
                            gotoTargets(returnAddress, operands, locals)
                        case _ ⇒
                            CodeError("the local variable ("+
                                lvIndex+
                                ") does not contain a return address value", code, lvIndex)
                    }
                case 168 /*jsr*/
                    | 201 /*jsr_w*/ ⇒
                    val branchtarget = pc + instruction.asInstanceOf[JSRInstruction].branchoffset
                    gotoTarget(branchtarget, domain.ReturnAddressValue(pcOfNextInstruction) :: operands, locals)

                //
                // CONDITIONAL TRANSFER OF CONTROL
                //

                case 165 /*if_acmpeq*/ ⇒
                    ifTcmpXX(areEqualReferences _, AreEqualReferences, AreNotEqualReferences)
                case 166 /*if_acmpne*/ ⇒
                    ifTcmpXX(areNotEqualReferences _, AreNotEqualReferences, AreEqualReferences)
                case 198 /*ifnull*/ ⇒
                    ifXX(isNull _, IsNull, IsNonNull)
                case 199 /*ifnonnull*/ ⇒
                    ifXX(isNonNull _, IsNonNull, IsNull)

                case 159 /*if_icmpeq*/ ⇒
                    ifTcmpXX(areEqual _, AreEqual, AreNotEqual)
                case 160 /*if_icmpne*/ ⇒
                    ifTcmpXX(areNotEqual _, AreNotEqual, AreEqual)
                case 161 /*if_icmplt*/ ⇒
                    ifTcmpXX(isLessThan _, IsLessThan, IsGreaterThanOrEqualTo)
                case 162 /*if_icmpge*/ ⇒
                    ifTcmpXX(isGreaterThanOrEqualTo _, IsGreaterThanOrEqualTo, IsLessThan)
                case 163 /*if_icmpgt*/ ⇒
                    ifTcmpXX(isGreaterThan _, IsGreaterThan, IsLessThanOrEqualTo)
                case 164 /*if_icmple*/ ⇒
                    ifTcmpXX(isLessThanOrEqualTo _, IsLessThanOrEqualTo, IsGreaterThan)
                case 153 /*ifeq*/ ⇒
                    ifXX(is0 _, Is0, IsNot0)
                case 154 /*ifne*/ ⇒
                    ifXX(isNot0 _, IsNot0, Is0)
                case 155 /*iflt*/ ⇒
                    ifXX(isLessThan0 _, IsLessThan0, IsGreaterThanOrEqualTo0)
                case 156 /*ifge*/ ⇒
                    ifXX(isGreaterThanOrEqualTo0 _, IsGreaterThanOrEqualTo0, IsLessThan0)
                case 157 /*ifgt*/ ⇒
                    ifXX(isGreaterThan0 _, IsGreaterThan0, IsLessThanOrEqualTo0)
                case 158 /*ifle */ ⇒
                    ifXX(isLessThanOrEqualTo0 _, IsLessThanOrEqualTo0, IsGreaterThan0)

                case 171 /*lookupswitch*/ ⇒
                    val switch = instructions(pc).asInstanceOf[LOOKUPSWITCH]
                    val index = operands.head
                    val remainingOperands = operands.tail
                    val firstKey = switch.npairs(0)._1
                    var previousKey = firstKey
                    var branchToDefaultRequired = false
                    for ((key, offset) ← switch.npairs) {
                        if (!branchToDefaultRequired && (key - previousKey) > 1) {
                            if ((previousKey until key).exists(v ⇒ domain.isSomeValueInRange(index, v, v))) {
                                branchToDefaultRequired = true
                            } else {
                                previousKey = key
                            }
                        }
                        if (domain.isSomeValueInRange(index, key, key)) {
                            val branchTarget = pc + offset
                            val (updatedOperands, updatedLocals) =
                                domain.establishValue(branchTarget, key, index, remainingOperands, locals)
                            gotoTarget(branchTarget, updatedOperands, updatedLocals)
                        }
                    }
                    if (branchToDefaultRequired || domain.isSomeValueNotInRange(index, firstKey, switch.npairs(switch.npairs.size - 1)._1)) {
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
                        if (domain.isSomeValueInRange(index, v, v)) {
                            val branchTarget = pc + tableswitch.jumpOffsets(v - low)
                            val (updatedOperands, updatedLocals) =
                                domain.establishValue(branchTarget, v, index, remainingOperands, locals)
                            gotoTarget(branchTarget, updatedOperands, updatedLocals)
                        }
                        v = v + 1
                    }
                    if (domain.isSomeValueNotInRange(index, low, high)) {
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
                    val computation = domain.athrow(pc, operands.head)
                    if (computation.throwsException) {
                        // if the operand of the athrow exception is null, a new NullPointerException is raised
                        handleException(computation.exceptions)
                    }
                    if (computation.hasResult) {
                        val exceptionValue = computation.result
                        val nextOperands = List(exceptionValue)

                        domain.types(exceptionValue) match {
                            case answer: TypesUnknown ⇒
                                code.exceptionHandlersFor(pc).foreach(eh ⇒ {
                                    val branchTarget = eh.handlerPC
                                    // unless we have a "finally" handler, we can state
                                    // a constraint
                                    if (eh.catchType.isDefined) {
                                        eh.catchType.map(catchType ⇒ {
                                            val (updatedOperands, updatedLocals) =
                                                establishUpperBound(branchTarget, catchType, exceptionValue, nextOperands, locals)
                                            gotoTarget(branchTarget, updatedOperands, updatedLocals)
                                        })
                                    } else
                                        gotoTarget(branchTarget, nextOperands, locals)
                                })
                                abnormalReturn(pc, exceptionValue)

                            case exceptions: IsReferenceType ⇒
                                val isHandled = exceptions.forallTypes(exceptionType ⇒
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
                                                        establishUpperBound(branchTarget, catchType.get, exceptionValue, nextOperands, locals)
                                                    gotoTarget(branchTarget, updatedOperands, updatedLocals)
                                                    false
                                            }
                                        }
                                    }
                                    ))
                                // If "isHandled" is true, we are sure that at least one 
                                // handler will catch the exception(s)... hence the method
                                // will not return abnormally
                                if (!isHandled)
                                    abnormalReturn(pc, exceptionValue)

                            case types ⇒
                                AIImplementationError("non-exception type(s): "+types)
                        }
                    }

                //
                // CREATE ARRAY
                //

                case 188 /*newarray*/ ⇒
                    val count :: rest = operands
                    val atype = instruction.asInstanceOf[NEWARRAY].atype
                    val computation = (atype: @annotation.switch) match {
                        case 4 /*BooleanType.atype*/  ⇒ domain.newarray(pc, count, BooleanType)
                        case 5 /*CharType.atype*/     ⇒ domain.newarray(pc, count, CharType)
                        case 6 /*FloatType.atype*/    ⇒ domain.newarray(pc, count, FloatType)
                        case 7 /*DoubleType.atype*/   ⇒ domain.newarray(pc, count, DoubleType)
                        case 8 /*ByteType.atype*/     ⇒ domain.newarray(pc, count, ByteType)
                        case 9 /*ShortType.atype*/    ⇒ domain.newarray(pc, count, ShortType)
                        case 10 /*IntegerType.atype*/ ⇒ domain.newarray(pc, count, IntegerType)
                        case 11 /*LongType.atype*/    ⇒ domain.newarray(pc, count, LongType)
                        case _                        ⇒ BATError("newarray of unsupported \"atype\"")
                    }
                    computationWithReturnValueAndException(computation, rest)

                case 189 /*anewarray*/ ⇒
                    val count :: rest = operands
                    val componentType = instruction.asInstanceOf[ANEWARRAY].componentType
                    val computation = domain.newarray(pc, count, componentType)
                    computationWithReturnValueAndException(computation, rest)

                case 197 /*multianewarray*/ ⇒
                    val multianewarray = instruction.asInstanceOf[MULTIANEWARRAY]
                    val dimensions = multianewarray.dimensions
                    val dimensionSizes = operands.take(multianewarray.dimensions)
                    val componentType = multianewarray.componentType
                    val computation = domain.multianewarray(pc, dimensionSizes, componentType)
                    computationWithReturnValueAndException(computation, operands.drop(dimensions))

                //
                // LOAD FROM AND STORE VALUE IN ARRAYS
                //

                case 50 /*aaload*/ ⇒ {
                    val index :: arrayref :: rest = operands
                    val computation = domain.aaload(pc, index, arrayref)
                    computationWithReturnValueAndExceptions(computation, rest)
                }
                case 83 /*aastore*/ ⇒ {
                    val value :: index :: arrayref :: rest = operands
                    val computation = domain.aastore(pc, value, index, arrayref)
                    computationWithExceptions(computation, rest)
                }

                case 51 /*baload*/ ⇒ {
                    val index :: arrayref :: rest = operands
                    val computation = domain.baload(pc, index, arrayref)
                    computationWithReturnValueAndExceptions(computation, rest)
                }
                case 84 /*bastore*/ ⇒ {
                    val value :: index :: arrayref :: rest = operands
                    val computation = domain.bastore(pc, value, index, arrayref)
                    computationWithExceptions(computation, rest)
                }

                case 52 /*caload*/ ⇒ {
                    val index :: arrayref :: rest = operands
                    val computation = domain.caload(pc, index, arrayref)
                    computationWithReturnValueAndExceptions(computation, rest)
                }
                case 85 /*castore*/ ⇒ {
                    val value :: index :: arrayref :: rest = operands
                    val computation = domain.castore(pc, value, index, arrayref)
                    fallThroughO(rest)
                }

                case 49 /*daload*/ ⇒ {
                    val index :: arrayref :: rest = operands
                    val computation = domain.daload(pc, index, arrayref)
                    computationWithReturnValueAndExceptions(computation, rest)
                }
                case 82 /*dastore*/ ⇒ {
                    val value :: index :: arrayref :: rest = operands
                    val computation = domain.dastore(pc, value, index, arrayref)
                    computationWithExceptions(computation, rest)
                }

                case 48 /*faload*/ ⇒ {
                    val index :: arrayref :: rest = operands
                    val computation = domain.faload(pc, index, arrayref)
                    computationWithReturnValueAndExceptions(computation, rest)
                }
                case 81 /*fastore*/ ⇒ {
                    val value :: index :: arrayref :: rest = operands
                    val computation = domain.fastore(pc, value, index, arrayref)
                    computationWithExceptions(computation, rest)
                }

                case 46 /*iaload*/ ⇒ {
                    val index :: arrayref :: rest = operands
                    val computation = domain.iaload(pc, index, arrayref)
                    computationWithReturnValueAndExceptions(computation, rest)
                }
                case 79 /*iastore*/ ⇒ {
                    val value :: index :: arrayref :: rest = operands
                    val computation = domain.iastore(pc, value, index, arrayref)
                    computationWithExceptions(computation, rest)
                }

                case 47 /*laload*/ ⇒ {
                    val index :: arrayref :: rest = operands
                    val computation = domain.laload(pc, index, arrayref)
                    computationWithReturnValueAndExceptions(computation, rest)
                }
                case 80 /*lastore*/ ⇒ {
                    val value :: index :: arrayref :: rest = operands
                    val computation = domain.lastore(pc, value, index, arrayref)
                    computationWithExceptions(computation, rest)
                }

                case 53 /*saload*/ ⇒ {
                    val index :: arrayref :: rest = operands
                    val computation = domain.saload(pc, index, arrayref)
                    computationWithReturnValueAndExceptions(computation, rest)
                }
                case 86 /*sastore*/ ⇒ {
                    val value :: index :: arrayref :: rest = operands
                    val computation = domain.sastore(pc, value, index, arrayref)
                    computationWithExceptions(computation, rest)
                }

                //
                // LENGTH OF AN ARRAY
                //

                case 190 /*arraylength*/ ⇒ {
                    val arrayref = operands.head
                    val computation = domain.arraylength(pc, arrayref)
                    computationWithReturnValueAndException(computation, operands.tail)
                }

                //
                // ACCESSING FIELDS
                //
                case 180 /*getfield*/ ⇒ {
                    val getfield = instruction.asInstanceOf[GETFIELD]
                    computationWithReturnValueAndException(
                        domain.getfield(
                            pc,
                            operands.head,
                            getfield.declaringClass,
                            getfield.name,
                            getfield.fieldType), operands.tail)
                }
                case 178 /*getstatic*/ ⇒ {
                    val getstatic = instruction.asInstanceOf[GETSTATIC]
                    fallThroughO(
                        domain.getstatic(
                            pc,
                            getstatic.declaringClass,
                            getstatic.name,
                            getstatic.fieldType) :: operands)
                }
                case 181 /*putfield*/ ⇒ {
                    val putfield = instruction.asInstanceOf[PUTFIELD]
                    val value :: objectref :: rest = operands
                    computationWithException(
                        domain.putfield(
                            pc,
                            objectref,
                            value,
                            putfield.declaringClass,
                            putfield.name,
                            putfield.fieldType), rest)
                }
                case 179 /*putstatic*/ ⇒ {
                    val putstatic = instruction.asInstanceOf[PUTSTATIC]
                    val value :: rest = operands
                    domain.putstatic(
                        pc,
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
                    BATError("invokedynamic is not yet supported")

                case 185 /*invokeinterface*/ ⇒
                    val invoke = instruction.asInstanceOf[INVOKEINTERFACE]
                    val argsCount = invoke.methodDescriptor.parameterTypes.length
                    val computation =
                        domain.invokeinterface(
                            pc,
                            invoke.declaringClass,
                            invoke.name,
                            invoke.methodDescriptor,
                            operands.take(argsCount + 1)
                        )
                    computationWithOptionalReturnValueAndExceptions(
                        computation,
                        operands.drop(argsCount + 1))

                case 183 /*invokespecial*/ ⇒
                    val invoke = instruction.asInstanceOf[INVOKESPECIAL]
                    val argsCount = invoke.methodDescriptor.parameterTypes.length
                    val computation =
                        domain.invokespecial(
                            pc,
                            invoke.declaringClass,
                            invoke.name,
                            invoke.methodDescriptor,
                            operands.take(argsCount + 1)
                        )
                    computationWithOptionalReturnValueAndExceptions(
                        computation,
                        operands.drop(argsCount + 1))

                case 184 /*invokestatic*/ ⇒
                    val invoke = instruction.asInstanceOf[INVOKESTATIC]
                    val argsCount = invoke.methodDescriptor.parameterTypes.length
                    val computation =
                        domain.invokestatic(
                            pc,
                            invoke.declaringClass,
                            invoke.name,
                            invoke.methodDescriptor,
                            operands.take(argsCount)
                        )
                    computationWithOptionalReturnValueAndExceptions(
                        computation,
                        operands.drop(argsCount))

                case 182 /*invokevirtual*/ ⇒
                    val invoke = instruction.asInstanceOf[INVOKEVIRTUAL]
                    val argsCount = invoke.methodDescriptor.parameterTypes.length
                    val computation =
                        domain.invokevirtual(
                            pc,
                            invoke.declaringClass,
                            invoke.name,
                            invoke.methodDescriptor,
                            operands.take(argsCount + 1)
                        )
                    computationWithOptionalReturnValueAndExceptions(
                        computation,
                        operands.drop(argsCount + 1))

                case 192 /*checkcast*/ ⇒
                    val objectref = operands.head
                    val referenceType = instruction.asInstanceOf[CHECKCAST].referenceType
                    val computation = domain.checkcast(pc, objectref, referenceType)
                    computationWithReturnValueAndException(computation, operands.tail)

                case 194 /*monitorenter*/ ⇒
                    val computation = domain.monitorenter(pc, operands.head)
                    computationWithException(computation, operands.tail)

                case 195 /*monitorexit*/ ⇒
                    val computation = domain.monitorexit(pc, operands.head)
                    computationWithException(computation, operands.tail)

                //
                // RETURN FROM METHOD
                //
                case 176 /*areturn*/ ⇒ domain.areturn(pc, operands.head)
                case 175 /*dreturn*/ ⇒ domain.dreturn(pc, operands.head)
                case 174 /*freturn*/ ⇒ domain.freturn(pc, operands.head)
                case 172 /*ireturn*/ ⇒ domain.ireturn(pc, operands.head)
                case 173 /*lreturn*/ ⇒ domain.lreturn(pc, operands.head)
                case 177 /*return*/  ⇒ domain.returnVoid(pc)

                // -----------------------------------------------------------------------
                //
                // INSTRUCTIONS THAT FALL THROUGH / THAT DO NOT CONTROL THE 
                // CONTROL FLOW (WHICH WILL NEVER THROW AN EXCEPTION)
                //
                // -----------------------------------------------------------------------

                //
                // PUT LOCAL VARIABLE VALUE ONTO STACK
                //
                case 25 /*aload*/
                    | 24 /*dload*/
                    | 23 /*fload*/
                    | 21 /*iload*/
                    | 22 /*lload*/ ⇒
                    val lvIndex = instruction.asInstanceOf[LoadLocalVariableInstruction].lvIndex
                    fallThroughO(locals(lvIndex) :: operands)
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
                case 58 /*astore*/
                    | 57 /*dstore*/
                    | 56 /*fstore*/
                    | 54 /*istore*/
                    | 55 /*lstore*/ ⇒
                    val lvIndex = instruction.asInstanceOf[StoreLocalVariableInstruction].lvIndex
                    fallThroughOL(operands.tail, updateLocals(lvIndex, operands.head))
                case 75 /*astore_0*/
                    | 71 /*dstore_0*/
                    | 67 /*fstore_0*/
                    | 63 /*lstore_0*/
                    | 59 /*istore_0*/ ⇒
                    fallThroughOL(operands.tail, updateLocals(0, operands.head))
                case 76 /*astore_1*/
                    | 72 /*dstore_1*/
                    | 68 /*fstore_1*/
                    | 64 /*lstore_1*/
                    | 60 /*istore_1*/ ⇒
                    fallThroughOL(operands.tail, updateLocals(1, operands.head))
                case 77 /*astore_2*/
                    | 73 /*dstore_2*/
                    | 69 /*fstore_2*/
                    | 65 /*lstore_2*/
                    | 61 /*istore_2*/ ⇒
                    fallThroughOL(operands.tail, updateLocals(2, operands.head))
                case 78 /*astore_3*/
                    | 74 /*dstore_3*/
                    | 70 /*fstore_3*/
                    | 66 /*lstore_3*/
                    | 62 /*istore_3*/ ⇒
                    fallThroughOL(operands.tail, updateLocals(3, operands.head))

                //
                // PUSH CONSTANT VALUE
                //

                case 1 /*aconst_null*/ ⇒
                    fallThroughO(domain.nullValue(pc) :: operands)

                case 16 /*bipush*/ ⇒
                    val value = instruction.asInstanceOf[BIPUSH].value
                    fallThroughO(domain.byteValue(pc, value) :: operands)

                case 14 /*dconst_0*/ ⇒
                    fallThroughO(domain.doubleValue(pc, 0.0d) :: operands)
                case 15 /*dconst_1*/ ⇒
                    fallThroughO(domain.doubleValue(pc, 1.0d) :: operands)

                case 11 /*fconst_0*/ ⇒
                    fallThroughO(domain.floatValue(pc, 0.0f) :: operands)
                case 12 /*fconst_1*/ ⇒
                    fallThroughO(domain.floatValue(pc, 1.0f) :: operands)
                case 13 /*fconst_2*/ ⇒
                    fallThroughO(domain.floatValue(pc, 2.0f) :: operands)

                case 2 /*iconst_m1*/ ⇒
                    fallThroughO(domain.intValue(pc, -1) :: operands)
                case 3 /*iconst_0*/ ⇒
                    fallThroughO(domain.intValue(pc, 0) :: operands)
                case 4 /*iconst_1*/ ⇒
                    fallThroughO(domain.intValue(pc, 1) :: operands)
                case 5 /*iconst_2*/ ⇒
                    fallThroughO(domain.intValue(pc, 2) :: operands)
                case 6 /*iconst_3*/ ⇒
                    fallThroughO(domain.intValue(pc, 3) :: operands)
                case 7 /*iconst_4*/ ⇒
                    fallThroughO(domain.intValue(pc, 4) :: operands)
                case 8 /*iconst_5*/ ⇒
                    fallThroughO(domain.intValue(pc, 5) :: operands)

                case 9 /*lconst_0*/ ⇒
                    fallThroughO(domain.longValue(pc, 0l) :: operands)
                case 10 /*lconst_1*/ ⇒
                    fallThroughO(domain.longValue(pc, 1l) :: operands)

                case 18 /*ldc*/ ⇒ {
                    instruction match {
                        case LoadInt(v) ⇒
                            fallThroughO(domain.intValue(pc, v) :: operands)
                        case LoadFloat(v) ⇒
                            fallThroughO(domain.floatValue(pc, v) :: operands)
                        case LoadString(v) ⇒
                            fallThroughO(domain.stringValue(pc, v) :: operands)
                        case LoadClass(v) ⇒
                            fallThroughO(domain.classValue(pc, v) :: operands)
                    }
                }
                case 19 /*ldc_w*/ ⇒ {
                    instruction match {
                        case LoadInt_W(v) ⇒
                            fallThroughO(domain.intValue(pc, v) :: operands)
                        case LoadFloat_W(v) ⇒
                            fallThroughO(domain.floatValue(pc, v) :: operands)
                        case LoadString_W(v) ⇒
                            fallThroughO(domain.stringValue(pc, v) :: operands)
                        case LoadClass_W(v) ⇒
                            fallThroughO(domain.classValue(pc, v) :: operands)
                    }
                }
                case 20 /*ldc2_w*/ ⇒ {
                    instruction match {
                        case LoadLong(v) ⇒
                            fallThroughO(domain.longValue(pc, v) :: operands)
                        case LoadDouble(v) ⇒
                            fallThroughO(domain.doubleValue(pc, v) :: operands)
                    }
                }

                case 17 /*sipush*/ ⇒
                    val value = instruction.asInstanceOf[SIPUSH].value
                    fallThroughO(domain.shortValue(pc, value) :: operands)

                //
                // RELATIONAL OPERATORS
                //
                case 150 /*fcmpg*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.fcmpg(pc, value1, value2) :: rest)
                }
                case 149 /*fcmpl*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.fcmpl(pc, value1, value2) :: rest)
                }
                case 152 /*dcmpg*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.dcmpg(pc, value1, value2) :: rest)
                }
                case 151 /*dcmpl*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.dcmpl(pc, value1, value2) :: rest)
                }
                case 148 /*lcmp*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.lcmp(pc, value1, value2) :: rest)
                }

                //
                // UNARY EXPRESSIONS
                //
                case 119 /*dneg*/ ⇒
                    fallThroughO(domain.dneg(pc, operands.head) :: (operands.tail))
                case 118 /*fneg*/ ⇒
                    fallThroughO(domain.fneg(pc, operands.head) :: (operands.tail))
                case 117 /*lneg*/ ⇒
                    fallThroughO(domain.lneg(pc, operands.head) :: (operands.tail))
                case 116 /*ineg*/ ⇒
                    fallThroughO(domain.ineg(pc, operands.head) :: (operands.tail))

                //
                // BINARY EXPRESSIONS
                //

                case 99 /*dadd*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.dadd(pc, value1, value2) :: rest)
                }
                case 111 /*ddiv*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.ddiv(pc, value1, value2) :: rest)
                }
                case 107 /*dmul*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.dmul(pc, value1, value2) :: rest)
                }
                case 115 /*drem*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.drem(pc, value1, value2) :: rest)
                }
                case 103 /*dsub*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.dsub(pc, value1, value2) :: rest)
                }

                case 98 /*fadd*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.fadd(pc, value1, value2) :: rest)
                }
                case 110 /*fdiv*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.fdiv(pc, value1, value2) :: rest)
                }
                case 106 /*fmul*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.fmul(pc, value1, value2) :: rest)
                }
                case 114 /*frem*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.frem(pc, value1, value2) :: rest)
                }
                case 102 /*fsub*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.fsub(pc, value1, value2) :: rest)
                }

                case 96 /*iadd*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.iadd(pc, value1, value2) :: rest)
                }
                case 126 /*iand*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.iand(pc, value1, value2) :: rest)
                }
                case 108 /*idiv*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    val computation = domain.idiv(pc, value1, value2)
                    computationWithReturnValueAndException(computation, rest)
                }
                case 104 /*imul*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.imul(pc, value1, value2) :: rest)
                }
                case 128 /*ior*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.ior(pc, value1, value2) :: rest)
                }
                case 112 /*irem*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.irem(pc, value1, value2) :: rest)
                }
                case 120 /*ishl*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.ishl(pc, value1, value2) :: rest)
                }
                case 122 /*ishr*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.ishr(pc, value1, value2) :: rest)
                }
                case 100 /*isub*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.isub(pc, value1, value2) :: rest)
                }
                case 124 /*iushr*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.iushr(pc, value1, value2) :: rest)
                }
                case 130 /*ixor*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.ixor(pc, value1, value2) :: rest)
                }

                case 97 /*ladd*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.ladd(pc, value1, value2) :: rest)
                }
                case 127 /*land*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.land(pc, value1, value2) :: rest)
                }
                case 109 /*ldiv*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    val computation = domain.ldiv(pc, value1, value2)
                    computationWithReturnValueAndException(computation, rest)
                }
                case 105 /*lmul*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.lmul(pc, value1, value2) :: rest)
                }
                case 129 /*lor*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.lor(pc, value1, value2) :: rest)
                }
                case 113 /*lrem*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.lrem(pc, value1, value2) :: rest)
                }
                case 121 /*lshl*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.lshl(pc, value1, value2) :: rest)
                }
                case 123 /*lshr*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.lshr(pc, value1, value2) :: rest)
                }
                case 101 /*lsub*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.lsub(pc, value1, value2) :: rest)
                }
                case 125 /*lushr*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.lushr(pc, value1, value2) :: rest)
                }
                case 131 /*lxor*/ ⇒ {
                    val value2 :: value1 :: rest = operands
                    fallThroughO(domain.lxor(pc, value1, value2) :: rest)
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

                case 87 /*pop*/ ⇒
                    fallThroughO(operands.tail)
                case 88 /*pop2*/ ⇒
                    if (operands.head.computationalType.operandSize == 1)
                        fallThroughO(operands.drop(2))
                    else
                        fallThroughO(operands.tail)

                case 95 /*swap*/ ⇒ {
                    val v1 :: v2 :: rest = operands
                    fallThroughO(v2 :: v1 :: rest)
                }

                //
                // TYPE CONVERSION
                //

                case 193 /*instanceof*/ ⇒ {
                    val objectref :: rest = operands
                    val referenceType = instruction.asInstanceOf[INSTANCEOF].referenceType
                    val newOperand = domain.instanceof(pc, objectref, referenceType)
                    fallThroughOL(newOperand :: rest, locals)
                }

                case 144 /*d2f*/ ⇒
                    fallThroughO(domain.d2f(pc, operands.head) :: (operands.tail))
                case 142 /*d2i*/ ⇒
                    fallThroughO(domain.d2i(pc, operands.head) :: (operands.tail))
                case 143 /*d2l*/ ⇒
                    fallThroughO(domain.d2l(pc, operands.head) :: (operands.tail))

                case 141 /*f2d*/ ⇒
                    fallThroughO(domain.f2d(pc, operands.head) :: (operands.tail))
                case 139 /*f2i*/ ⇒
                    fallThroughO(domain.f2i(pc, operands.head) :: (operands.tail))
                case 140 /*f2l*/ ⇒
                    fallThroughO(domain.f2l(pc, operands.head) :: (operands.tail))

                case 145 /*i2b*/ ⇒
                    fallThroughO(domain.i2b(pc, operands.head) :: (operands.tail))
                case 146 /*i2c*/ ⇒
                    fallThroughO(domain.i2c(pc, operands.head) :: (operands.tail))
                case 135 /*i2d*/ ⇒
                    fallThroughO(domain.i2d(pc, operands.head) :: (operands.tail))
                case 134 /*i2f*/ ⇒
                    fallThroughO(domain.i2f(pc, operands.head) :: (operands.tail))
                case 133 /*i2l*/ ⇒
                    fallThroughO(domain.i2l(pc, operands.head) :: (operands.tail))
                case 147 /*i2s*/ ⇒
                    fallThroughO(domain.i2s(pc, operands.head) :: (operands.tail))

                case 138 /*l2d*/ ⇒
                    fallThroughO(domain.l2d(pc, operands.head) :: (operands.tail))
                case 137 /*l2f*/ ⇒
                    fallThroughO(domain.l2f(pc, operands.head) :: (operands.tail))
                case 136 /*l2i*/ ⇒
                    fallThroughO(domain.l2i(pc, operands.head) :: (operands.tail))

                //
                // "OTHER" INSTRUCTIONS
                //
                case 132 /*iinc*/ ⇒ {
                    val iinc = instruction.asInstanceOf[IINC]
                    val newValue = domain.iinc(pc, locals(iinc.lvIndex), iinc.constValue)
                    fallThroughOL(operandsArray(pc), updateLocals(iinc.lvIndex, newValue))
                }

                case 187 /*new*/ ⇒ {
                    val newObject = instruction.asInstanceOf[NEW]
                    fallThroughO(domain.newObject(pc, newObject.objectType) :: operands)
                }

                case 0 /*nop*/    ⇒ fallThrough()
                case 196 /*wide*/ ⇒ fallThrough()

                case opcode       ⇒ BATError("instruction with unsupported opcode: "+opcode)
            }
        }

        AIResultBuilder.completed(code, domain)(operandsArray, localsArray)
    }
}

/**
 * A base abstract interpreter useful for testing and debugging purposes. The base
 * interpreter can be interrupted by calling the `interrupt` method of the
 * AI's thread.
 */
object AI extends AI {

    def isInterrupted = Thread.interrupted()

    val tracer = None

}

