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
package ai

import scala.util.control.ControlThrowable

import org.opalj.util.{ Answer, Yes, No, Unknown }

import org.opalj.br._
import org.opalj.br.instructions._

/**
 * A highly-configurable framework for the (abstract) interpretation of Java bytecode
 * that relies on OPAL's resolved representation of Java bytecode.
 *
 * This framework basically traverses all instructions of a method in depth-first order
 * and evaluates each instruction using an exchangeable
 * [[org.opalj.ai.Domain]].
 *
 * ==Interacting with OPAL-AI==
 * The primary means how to make use of this framework is to perform
 * an abstract interpretation of a method using a customized `Domain`. That
 * customized domain can be used, e.g., to build a call graph or to
 * do other intra-/interprocedural analyses while the code is analyzed.
 * Additionally, it is possible to analyze the result of an abstract interpretation.
 *
 * ==Thread Safety==
 * This class is thread-safe. However, to make it possible to use one abstract
 * interpreter instance for the concurrent abstract interpretation of independent
 * methods, the `AITracer` (if any) has to be thread-safe to.
 *
 * Hence, it is possible to use a single instance to analyze multiple methods in parallel.
 * However, if you want to be able to selectively abort the abstract interpretation
 * of some methods or want to selectively trace the interpretation of some methods, then
 * you should use multiple abstract interpreter instances.
 * Creating new instances is extremely cheap as this class
 * does not have any significant associated state.
 *
 * ==Customizing the Abstract Interpretation Framework==
 * Customization of the abstract interpreter is done by creating new subclasses that
 * override the relevant methods (in particular: `isInterrupted` and `tracer`).
 *
 * @note
 *     OPAL-AI does not make assumptions about the number of domain objects that
 *     are used. However, if a single domain object is used by multiple instances
 *     of this class and the abstract interpretation are executed concurrently, then
 *     the domain has to be thread-safe.
 *     The latter is trivially the case when the domain object itself does not have
 *     any state.
 *
 * @author Michael Eichberg
 */
trait AI[D <: Domain] {

    import AI._

    /**
     * Determines whether a running (or to be started) abstract interpretation
     * should be interrupted (default: `false`).
     *
     * In general, interrupting the abstract interpreter may be meaningful if
     * the abstract interpretation takes too long or if the currently used domain
     * is not sufficiently precise enough/if additional information is needed to
     * continue with the analysis.
     *
     * Called by OPAL-AI during the abstract interpretation of a method to determine whether
     * the computation should be aborted. This method is ''always called directly before
     * the evaluation of the first/next instruction''. I.e., after the evaluation of an
     * instruction and the update of the memory as well as stating all constraints.
     *
     * @note When the abstract interpreter is currently waiting on the result of the
     *    interpretation of a called method it may take some time before the
     *    interpretation of the current method (this abstract interpreter) is actually
     *    aborted.
     *
     * This method '''needs to be overridden in subclasses to identify situations
     * in which a running abstract interpretation should be interrupted'''.
     */
    def isInterrupted: Boolean = false

    /**
     * The tracer (default: `None`) that is called by OPAL-AI while performing the abstract
     * interpretation of a method.
     *
     * This method is called by OPAL-AI at various different points (see
     * [[org.opalj.ai.AITracer]]) to report the analysis progress.
     *
     * '''To attach a tracer to the abstract interpreter override this
     * method in subclasses''' and return some tracer object.
     *
     * OPAL-AI enables the attachment/detachment of tracers at any time.
     */
    def tracer: Option[AITracer] = None

    /**
     *  Performs an abstract interpretation of the given method using the given domain.
     *
     *  @param classFile The method's defining class file.
     *  @param method A non-native, non-abstract method of the given class file that
     *      will be analyzed. All parameters are automatically initialized with sensible
     *      default values.
     *  @param domain The domain that will be used to perform computations related
     *  	to values.
     */
    def apply(
        classFile: ClassFile,
        method: Method,
        domain: D) = perform(classFile, method, domain)(None)

    /**
     * Returns the initial set of operands that will be used for for the abstract
     * interpretation of the given method.
     *
     * In general, an empty set is returned as the JVM specification mandates
     * that the operand stack is empty at the very beginning of a method.
     *
     * This method is called by the `perform` method with the same signature. It
     * may be overridden by subclasses to perform some additional processing.
     */
    protected def initialOperands(
        classFile: ClassFile,
        method: Method,
        domain: D): domain.Operands =
        List.empty[domain.DomainValue]

    /**
     * Returns the initial register assignment that is used when analyzing a new
     * method.
     *
     * Initially, only the registers that contain the method's parameters (including
     * the self reference (`this`)) are used.  If no initial assignment is provided
     * (`someLocals == None`) OPAL-AI will automatically create a valid assignment using
     * the domain. See `perform(...)` for further details regarding the initial
     * register assignment.
     *
     * This method is called by the `perform` method with the same signature. It
     * may be overridden by subclasses to perform some additional processing. In
     * that case, however, it is highly recommended to call this method to finalize the
     * initial assignment.
     *
     * @param classFile The class file which defines the given method.
     * @param method A non-native, non-abstract method. I.e., a method that has an
     *      implementation in Java bytecode.
     * @param domain The domain that will be used to perform computations related
     *  	to values.
     */
    protected def initialLocals(
        classFile: ClassFile,
        method: Method,
        domain: D)(
            someLocals: Option[IndexedSeq[domain.DomainValue]] = None): domain.Locals = {

        import domain.DomainValueTag

        someLocals.map { l ⇒
            if (l.size < (method.parameterTypes.size + (if (method.isStatic) 0 else 1)))
                throw new IllegalArgumentException(
                    "the number of initial values is less than the number of parameters"
                )

            val maxLocals = method.body.get.maxLocals
            if (l.size > maxLocals) {
                // l.toArray
                throw new IllegalArgumentException(
                    "the number of initial locals("+l.size+
                        ") is larger than \"maxLocals("+maxLocals+")\""
                )
            } else {
                // the number of given locals is smaller than or equal to the number of max locals
                // (the former number still has to be larger or equal to the number of 
                // parameter values (including "this"))
                val locals = org.opalj.ai.util.Locals[domain.DomainValue](maxLocals)
                // for (i ← (0 until l.size)) locals(i) = l(i)
                var i = l.size - 1
                while (i >= 0) {
                    locals.set(i, l(i))
                    i -= 1
                }
                locals
            }
        }.getOrElse { // there are no locals at all...
            val code = method.body.get
            val locals = org.opalj.ai.util.Locals[domain.DomainValue](code.maxLocals)
            var localVariableIndex = 0

            // Calculates the initial "PC" associated with a method's parameter.
            def origin(localVariableIndex: Int) = -localVariableIndex - 1

            if (!method.isStatic) {
                val thisType = classFile.thisType
                val thisValue =
                    domain.NonNullObjectValue(origin(localVariableIndex), thisType)
                locals.set(localVariableIndex, thisValue)
                localVariableIndex += 1 /*==thisType.computationalType.operandSize*/
            }
            for (parameterType ← method.descriptor.parameterTypes) {
                val ct = parameterType.computationalType
                locals.set(
                    localVariableIndex,
                    domain.TypedValue(origin(localVariableIndex), parameterType))
                localVariableIndex += ct.operandSize
            }
            locals
        }
    }

    /**
     * Analyzes the given method using the given domain and the pre-initialized parameter
     * values (if any). Basically, first the set of initial operands and locals is
     * calculated before the respective `perform(...,initialOperands,initialLocals)`
     * method is called.
     *
     * ==Controlling the AI==
     * The abstract interpretation of a method is aborted if the AI's `isInterrupted`
     * method returns true. That method is called directly before an instruction
     * is evaluated.
     *
     * @param classFile Some class file; needed to determine the type of `this` if
     *      the method is an instance method.
     * @param method A non-abstract, non-native method of the given class file. I.e.,
     *      a method with a body.
     * @param domain The abstract domain that will be used for the abstract interpretation
     *      of the given method.
     * @param someLocals The initial register assignment (the parameters passed to the
     * 		method). If the values passed to a method are already known, the
     *      abstract interpretation will be performed under that assumption. The specified
     *      number of locals has to be equal or larger than the number of parameters
     *      (including `this` in case of a non-static method.). If the number is lower
     *      than `method.body.maxLocals` it will be adjusted as required.
     * @return The result of the abstract interpretation. Basically, the calculated
     *      memory layouts; i.e., the list of operands and local variables before each
     *      instruction. Each calculated memory layout represents the layout before
     *      the instruction with the corresponding program counter was interpreted.
     *      If the interpretation was aborted, the returned result
     *      object contains all necessary information to continue the interpretation
     *      if needed/desired.
     */
    def perform(
        classFile: ClassFile,
        method: Method,
        theDomain: D)(
            someLocals: Option[IndexedSeq[theDomain.DomainValue]] = None): AIResult { val domain: theDomain.type } = {

        perform(method.body.get, theDomain)(
            initialOperands(classFile, method, theDomain),
            initialLocals(classFile, method, theDomain)(someLocals))
    }

    /**
     * Performs an abstract interpretation of the given (byte)code using
     * the given domain and the initial operand stack and initial register assignment.
     */
    protected[ai] def perform(
        code: Code,
        theDomain: D)(
            initialOperands: theDomain.Operands,
            initialLocals: theDomain.Locals): AIResult { val domain: theDomain.type } = {

        import theDomain.DomainValueTag

        val codeLength = code.instructions.length

        val operandsArray = new Array[theDomain.Operands](codeLength)
        operandsArray(0) = initialOperands

        val localsArray = new Array[theDomain.Locals](codeLength)
        localsArray(0) = initialLocals

        continueInterpretation(
            code, theDomain)(
                initialWorkList, List.empty[PC], operandsArray, localsArray, Nil)
    }

    /**
     * Continues the interpretation of the given method (code) using the given domain.
     *
     * @param code The bytecode that will be interpreted using the given domain.
     *
     * @param domain The domain that will be used to perform the domain
     *      dependent computations.
     *
     * @param initialWorklist The list of program counters with which the interpretation
     *      will continue. If the method was never analyzed before, the list should just
     *      contain the value "0"; i.e., we start with the interpretation of the
     *      first instruction.
     *      Note that the worklist may contain negative values. These values are not
     *      related to a specific instruction per-se but encode the necessary information
     *      to handle subroutines. In case of calls to a subroutine we add the special
     *      values `SUBROUTINE` and `SUBROUTINE_START` to the list to encode when the
     *      evaluation started. This is needed to completely process the subroutine
     *      (to explore all paths) before we finally return to the main method.
     *
     * @param alreadyEvaluated The list of the program counters (PC) of the instructions
     *      that were already evaluated. Initially (i.e., if the given code is analyzed
     *      the first time) this list is empty.
     *      This list is primarily needed to correctly resolve jumps to sub routines
     *      (`JSR(_W)` and `RET` instructions.)
     *
     * @param operandsArray The array that contains the operand stacks. Each value
     *      in the array contains the operand stack before the instruction with the
     *      corresponding index is executed. This array can be empty except of the
     *      indexes that are referred to by the `initialWorklist`.
     *      '''The `operandsArray` data structure is mutated by OPAL-AI and it is
     *      __recommended that a `Domain` does not directly mutate the state of
     *      this array__.'''
     *
     * @param localsArray The array that contains the local variable assignments.
     *      Each value in the array contains the local variable assignments before
     *      the instruction with the corresponding program counter is executed.
     *      '''The `localsArray` data structure is mutated by OPAL-AI and it is
     *      __recommended that a `Domain` does not directly mutate the state of
     *      this array__.'''
     */
    protected[ai] def continueInterpretation(
        code: Code,
        theDomain: D)(
            initialWorkList: List[PC],
            alreadyEvaluated: List[PC],
            theOperandsArray: theDomain.OperandsArray,
            theLocalsArray: theDomain.LocalsArray,
            theMemoryLayoutBeforeSubroutineCall: List[(theDomain.OperandsArray, theDomain.LocalsArray)]): AIResult { val domain: theDomain.type } = {

        if (tracer.isDefined)
            tracer.get.continuingInterpretation(code, theDomain)(
                initialWorkList, alreadyEvaluated,
                theOperandsArray, theLocalsArray, theMemoryLayoutBeforeSubroutineCall
            )

        import theDomain.{ DomainValue, ExceptionValue, ExceptionValues, Operands, Locals }
        import theDomain.{ SingleValueConstraint, TwoValuesConstraint }

        // import reference values related functionality 
        import theDomain.{ refAreEqual, refAreNotEqual, RefAreEqual, RefAreNotEqual }
        import theDomain.{ refEstablishAreEqual, refEstablishAreNotEqual }
        import theDomain.{ refIsNonNull, refIsNull, RefIsNonNull, RefIsNull }
        import theDomain.{ refEstablishIsNonNull, refEstablishIsNull }

        // import int values related functionality
        import theDomain.{ intAreEqual, intAreNotEqual, IntAreEqual, IntAreNotEqual }
        import theDomain.{ intIs0, intIsNot0, IntIs0, IntIsNot0 }
        import theDomain.{ intIsGreaterThan, intIsGreaterThan0, IntIsGreaterThan, IntIsGreaterThan0 }
        import theDomain.{ intIsLessThan, intIsLessThan0, IntIsLessThan, IntIsLessThan0 }
        import theDomain.{ intIsGreaterThanOrEqualTo, intIsGreaterThanOrEqualTo0, IntIsGreaterThanOrEqualTo, IntIsGreaterThanOrEqualTo0 }
        import theDomain.{ intIsLessThanOrEqualTo, intIsLessThanOrEqualTo0, IntIsLessThanOrEqualTo, IntIsLessThanOrEqualTo0 }

        import ObjectType._

        type SingleValueDomainTest = (DomainValue) ⇒ Answer
        type TwoValuesDomainTest = (DomainValue, DomainValue) ⇒ Answer

        val instructions: Array[Instruction] = code.instructions

        // The entire state of the computation is (from the perspective of the AI)
        // encapsulated by the following data-structures:
        /* 1 */ var operandsArray = theOperandsArray
        /* 2 */ var localsArray = theLocalsArray
        /* 3 */ var worklist = initialWorkList
        /* 4 */ var evaluated = alreadyEvaluated
        /* 5 */ var memoryLayoutBeforeSubroutineCall: List[(theDomain.OperandsArray, theDomain.LocalsArray)] = theMemoryLayoutBeforeSubroutineCall

        // -------------------------------------------------------------------------------
        //
        // Main loop of the abstract interpreter
        //
        // -------------------------------------------------------------------------------

        /*
         * Updates the state of the abstract interpreter to make it possible to
         * continue the abstract interpretation with the instruction
         * at the given target (`targetPC`). Basically, the operand stack
         * and the local variables are updated using the given ones and the
         * target program counter is added to the `worklist`.
         */
        def gotoTarget(
            sourcePC: PC,
            targetPC: PC,
            isExceptionalControlFlow: Boolean,
            operands: Operands,
            locals: Locals) {

            import util.removeFirstUnless

            // The worklist containing the PC is manipulated ...:
            // - here (by this method)
            // - by the JSR / RET instructions
            // - the main loop that processes the worklist

            val currentOperands = operandsArray(targetPC)
            if (currentOperands == null /* || localsArray(targetPC) == null )*/ ) {
                // we analyze the instruction for the first time ...
                operandsArray(targetPC) = operands
                localsArray(targetPC) = locals
                worklist = targetPC :: worklist
                if (tracer.isDefined)
                    tracer.get.flow(theDomain)(sourcePC, targetPC, isExceptionalControlFlow)
            } else {
                // we already evaluated the target instruction ... 
                val currentLocals = localsArray(targetPC)
                val mergeResult =
                    theDomain.join(
                        targetPC, currentOperands, currentLocals, operands, locals
                    )
                if (tracer.isDefined) tracer.get.join(theDomain)(
                    targetPC,
                    currentOperands, currentLocals, operands, locals,
                    mergeResult
                )
                mergeResult match {
                    case NoUpdate ⇒ /* nothing to do*/
                        if (tracer.isDefined) {
                            tracer.get.noFlow(theDomain)(sourcePC, targetPC)
                        }

                    case StructuralUpdate((updatedOperands, updatedLocals)) ⇒
                        operandsArray(targetPC) = updatedOperands
                        localsArray(targetPC) = updatedLocals
                        // we want depth-first evaluation (, but we do not want to 
                        // reschedule instructions that do not belong to the current
                        // evaluation context/(sub-)routine.)
                        val filteredList = removeFirstUnless(worklist, targetPC) { _ < 0 }
                        if (tracer.isDefined) {
                            if (filteredList eq worklist)
                                // the instruction was not yet scheduled for another
                                // evaluation
                                tracer.get.flow(theDomain)(
                                    sourcePC, targetPC, isExceptionalControlFlow)
                            else {
                                // the instruction was just moved to the beginning
                                tracer.get.rescheduled(theDomain)(
                                    sourcePC, targetPC, isExceptionalControlFlow)
                            }
                        }
                        worklist = targetPC :: filteredList

                    case MetaInformationUpdate((updatedOperands, updatedLocals)) ⇒
                        operandsArray(targetPC) = updatedOperands
                        localsArray(targetPC) = updatedLocals
                        // we want depth-first evaluation (, but we do not want to 
                        // reschedule instructions that do not belong to the current
                        // evaluation context/(sub-)routine.)
                        val filteredList = removeFirstUnless(worklist, targetPC) { _ < 0 }
                        if (filteredList ne worklist) {
                            // the instruction was scheduled, but not as the next one
                            // let's move the instruction to the beginning
                            worklist = targetPC :: filteredList

                            if (tracer.isDefined)
                                tracer.get.rescheduled(theDomain)(
                                    sourcePC, targetPC, isExceptionalControlFlow)
                        } else {
                            if (tracer.isDefined) {
                                tracer.get.noFlow(theDomain)(sourcePC, targetPC)
                            }
                        }
                }
            }

            worklist = theDomain.flow(
                sourcePC, targetPC, isExceptionalControlFlow,
                worklist,
                operandsArray, localsArray,
                tracer)
        }

        // THIS IS THE MAIN INTERPRETER LOOP
        while (worklist.nonEmpty) {
            if (isInterrupted) {
                val result = AIResultBuilder.aborted(
                    code,
                    theDomain)(
                        worklist,
                        evaluated,
                        operandsArray,
                        localsArray,
                        memoryLayoutBeforeSubroutineCall)
                if (tracer.isDefined)
                    tracer.get.result(result)

                return result
            }

            // The central worklist is manipulated at the following
            // places:
            // - here 
            // - by the JSR / RET instructions
            // - by the "gotoTarget" method
            val pc: PC = {
                // Check if we we have a return from the evaluation of a subroutine.
                // I.e., all paths in a subroutine are explored and we know all
                // exit points; we will now schedule the jump to the return
                // address and reset the subroutine's computation context
                while (worklist.head < 0) {
                    evaluated = SUBROUTINE_END :: evaluated
                    // the structure is:
                    // -lvIndex (:: RET_PC)* :: RETURN_ADDRESS :: SUBROUTINE
                    val lvIndex = -worklist.head
                    worklist = worklist.tail
                    var retPCs = Set.empty[PC]
                    while (worklist.tail.head != SUBROUTINE) {
                        // in case that a subroutine throws a (non-caught) exception, 
                        // we will not have encountered a single ret instruction
                        retPCs += worklist.head
                        worklist = worklist.tail
                    }
                    val returnAddress = worklist.head
                    worklist = worklist.tail.tail // let's remove the subroutine marker
                    val targets = retPCs.map { retPC ⇒
                        // reset the local variable that stores the return address
                        if (tracer.isDefined) {
                            val subroutine = evaluated.tail.takeWhile(_ != SUBROUTINE_START)
                            tracer.get.returnFromSubroutine(theDomain)(
                                retPC,
                                returnAddress,
                                subroutine
                            )
                        }

                        val operands = operandsArray(retPC)
                        val locals = localsArray(retPC)
                        val updatedLocals = locals.updated(lvIndex, theDomain.Null)
                        (retPC, operands, updatedLocals)
                    }
                    // clear all computations to make this subroutine callable again
                    val (oldOperandsArray, oldLocalsArray) = memoryLayoutBeforeSubroutineCall.head
                    operandsArray = oldOperandsArray
                    localsArray = oldLocalsArray
                    memoryLayoutBeforeSubroutineCall = memoryLayoutBeforeSubroutineCall.tail
                    targets.foreach { target ⇒
                        val (retPC, operands, updatedLocals) = target
                        gotoTarget(retPC, returnAddress, false, operands, updatedLocals)
                    }

                    // it may be possible that – after the return from a 
                    // call to a subroutine – we have nothing further to do and
                    // the interpretation ends (in the bytecode there is at least
                    // one further instruction, but we may have evaluated that one 
                    // already and the evaluation context didn't change).
                    if (worklist.isEmpty) {
                        val result =
                            AIResultBuilder.completed(
                                code, theDomain)(
                                    evaluated, operandsArray, localsArray)

                        if (tracer.isDefined) tracer.get.result(result)

                        return result
                    }
                }
                // [THE DEFAULT CASE] the PC of the next instruction...
                worklist.head
            }

            try {
                worklist = worklist.tail
                evaluated = pc :: evaluated
                val instruction = instructions(pc)
                // the memory layout before executing the instruction with the given pc
                val operands = operandsArray(pc)
                val locals = localsArray(pc)

                if (tracer.isDefined)
                    tracer.get.instructionEvalution(theDomain)(
                        pc, instruction, operands, locals
                    )

                def pcOfNextInstruction = code.pcOfNextInstruction(pc)

                /*
                 * Handles all '''if''' instructions that perform a comparison with a fixed
                 * value.
                 */
                def ifXX(domainTest: SingleValueDomainTest,
                         yesConstraint: SingleValueConstraint,
                         noConstraint: SingleValueConstraint) {

                    val branchInstruction = as[ConditionalBranchInstruction](instruction)
                    val operand = operands.head
                    val rest = operands.tail
                    val nextPC = pcOfNextInstruction
                    val branchTarget = pc + branchInstruction.branchoffset

                    domainTest(operand) match {
                        case Yes ⇒ gotoTarget(pc, branchTarget, false, rest, locals)
                        case No  ⇒ gotoTarget(pc, nextPC, false, rest, locals)
                        case Unknown ⇒ {
                            {
                                val (newOperands, newLocals) =
                                    yesConstraint(branchTarget, operand, rest, locals)
                                if (tracer.isDefined &&
                                    ((rest ne newOperands) || (locals ne newLocals))) {
                                    tracer.get.establishedConstraint(theDomain)(
                                        pc, branchTarget, rest, locals, newOperands, newLocals
                                    )
                                }
                                gotoTarget(pc, branchTarget, false, newOperands, newLocals)
                            }
                            {
                                val (newOperands, newLocals) =
                                    noConstraint(nextPC, operand, rest, locals)
                                if (tracer.isDefined &&
                                    ((rest ne newOperands) || (locals ne newLocals))) {
                                    tracer.get.establishedConstraint(theDomain)(
                                        pc, nextPC, rest, locals, newOperands, newLocals
                                    )
                                }
                                gotoTarget(pc, nextPC, false, newOperands, newLocals)
                            }
                        }
                    }
                }

                /*
                 * Handles all '''if''' instructions that perform a comparison of two
                 * stack based values.
                 */
                def ifTcmpXX(domainTest: TwoValuesDomainTest,
                             yesConstraint: TwoValuesConstraint,
                             noConstraint: TwoValuesConstraint) {

                    val branchInstruction = as[ConditionalBranchInstruction](instruction)
                    val value2 = operands.head
                    val remainingOperands = operands.tail
                    val value1 = remainingOperands.head
                    val rest = remainingOperands.tail
                    val branchTarget = pc + branchInstruction.branchoffset
                    val nextPC = code.pcOfNextInstruction(pc)
                    val testResult = domainTest(value1, value2)
                    testResult match {
                        case Yes ⇒ gotoTarget(pc, branchTarget, false, rest, locals)
                        case No  ⇒ gotoTarget(pc, nextPC, false, rest, locals)
                        case Unknown ⇒ {
                            {
                                val (newOperands, newLocals) =
                                    yesConstraint(branchTarget, value1, value2, rest, locals)
                                if (tracer.isDefined &&
                                    ((rest ne newOperands) || (locals ne newLocals))) {
                                    tracer.get.establishedConstraint(theDomain)(
                                        pc, branchTarget, rest, locals, newOperands, newLocals
                                    )
                                }
                                gotoTarget(pc, branchTarget, false, newOperands, newLocals)
                            }
                            {
                                val (newOperands, newLocals) =
                                    noConstraint(nextPC, value1, value2, rest, locals)
                                if (tracer.isDefined &&
                                    ((rest ne newOperands) || (locals ne newLocals))) {
                                    tracer.get.establishedConstraint(theDomain)(
                                        pc, nextPC, rest, locals, newOperands, newLocals
                                    )
                                }
                                gotoTarget(pc, nextPC, false, newOperands, newLocals)
                            }
                        }
                    }
                }

                /*
                 * Handles the control-flow when an exception was raised.
                 *
                 * Called when an exception was (potentially) raised as a side effect of
                 * evaluating the current instruction. In this case the corresponding
                 * handler is searched and the control is transfered to it.
                 * If no handler is found the domain is
                 * informed that the method invocation completed abruptly.
                 *
                 * @note The operand stack will only contain the raised exception.
                 *
                 * @param exceptionValue A guaranteed non-null value that represents an instance of
                 *      an object that is a subtype of `java.lang.Throwable`.
                 */
                def handleException(exceptionValue: DomainValue) {
                    val isHandled = code.handlersFor(pc) exists { eh ⇒
                        // find the exception handler that matches the given exception
                        val branchTarget = eh.handlerPC
                        val catchType = eh.catchType
                        if (catchType.isEmpty) { // this is a finally handler
                            gotoTarget(pc, branchTarget, true, List(exceptionValue), locals)
                            true
                        } else {
                            // TODO Do we have to handle the case that we know nothing about the exception type?
                            val IsReferenceValue(upperBounds) =
                                theDomain.typeOfValue(exceptionValue)

                            upperBounds forall { typeBounds ⇒
                                // as a side effect we also add the handler to the set
                                // of targets
                                typeBounds.isValueSubtypeOf(catchType.get) match {
                                    case No ⇒
                                        false
                                    case Yes ⇒
                                        gotoTarget(pc, branchTarget, true, List(exceptionValue), locals)
                                        true
                                    case Unknown ⇒
                                        val (updatedOperands, updatedLocals) =
                                            theDomain.refEstablishUpperBound(
                                                branchTarget,
                                                catchType.get,
                                                exceptionValue,
                                                List(exceptionValue),
                                                locals)
                                        gotoTarget(pc, branchTarget, true, updatedOperands, updatedLocals)
                                        false
                                }
                            }
                        }
                    }
                    // If "isHandled" is true, we are sure that at least one 
                    // handler caught the exception... hence this method
                    // invocation will not complete abruptly.
                    if (!isHandled)
                        abruptMethodExecution(pc, exceptionValue)
                }
                def handleExceptions(exceptions: Iterable[DomainValue]): Unit = {
                    exceptions.foreach(handleException)
                }

                def abruptMethodExecution(pc: Int, exception: DomainValue): Unit = {
                    if (tracer.isDefined)
                        tracer.get.abruptMethodExecution(theDomain)(pc, exception)

                    theDomain.abruptMethodExecution(pc, exception)
                }

                def fallThrough(
                    newOperands: Operands = operands,
                    newLocals: Locals = locals): Unit = {
                    gotoTarget(pc, pcOfNextInstruction, false, newOperands, newLocals)
                }

                def computationWithException(
                    computation: Computation[Nothing, ExceptionValue],
                    rest: Operands) {

                    if (computation.throwsException)
                        handleException(computation.exceptions)
                    if (computation.returnsNormally)
                        fallThrough(rest)
                }

                def computationWithExceptions(
                    computation: Computation[Nothing, ExceptionValues],
                    rest: Operands) {

                    if (computation.returnsNormally)
                        fallThrough(rest)
                    if (computation.throwsException)
                        handleExceptions(computation.exceptions)
                }

                def computationWithReturnValueAndException(
                    computation: Computation[DomainValue, ExceptionValue],
                    rest: Operands) {

                    if (computation.hasResult)
                        fallThrough(computation.result :: rest)
                    if (computation.throwsException)
                        handleException(computation.exceptions)
                }

                def computationWithReturnValueAndExceptions(
                    computation: Computation[DomainValue, ExceptionValues],
                    rest: Operands) {

                    if (computation.hasResult)
                        fallThrough(computation.result :: rest)
                    if (computation.throwsException)
                        handleExceptions(computation.exceptions)
                }

                def computationWithOptionalReturnValueAndExceptions(
                    computation: Computation[DomainValue, ExceptionValues],
                    rest: Operands) {

                    if (computation.returnsNormally) {
                        if (computation.hasResult)
                            fallThrough(computation.result :: rest)
                        else
                            fallThrough(rest)
                    }
                    if (computation.throwsException)
                        handleExceptions(computation.exceptions)
                }

                @inline def as[I <: Instruction](i: Instruction): I = i.asInstanceOf[I]

                (instruction.opcode: @annotation.switch) match {
                    //
                    // UNCONDITIONAL TRANSFER OF CONTROL
                    //

                    case 167 /*goto*/
                        | 200 /*goto_w*/ ⇒
                        val goto = as[UnconditionalBranchInstruction](instruction)
                        val offset = goto.branchoffset
                        val branchtarget = pc + offset
                        gotoTarget(pc, branchtarget, false, operands, locals)

                    // Fundamental idea: we treat a "jump to subroutine" similar to
                    // the call of a method. I.e., we make sure the operand
                    // stack and the registers are empty for all instructions that
                    // potentially belong to the subroutine by clearing all information
                    // when the exploration of all paths is finished and before we 
                    // return from the subroutine.
                    // Semantics (from the JVM Spec):
                    // - The instruction following each jsr(_w) instruction may be 
                    //      returned to only by a single ret instruction.
                    // - No jsr(_w) instruction that is returned to may be used to 
                    //      recursively call a subroutine if that subroutine is already 
                    //      present in the subroutine call chain. (Subroutines can be 
                    //      nested when using try-finally constructs from within a 
                    //      finally clause.)
                    // - Each instance of type return address can be returned to at most
                    //      once.
                    case 168 /*jsr*/
                        | 201 /*jsr_w*/ ⇒
                        val returnTarget = pcOfNextInstruction
                        worklist = SUBROUTINE_START :: returnTarget :: SUBROUTINE :: worklist
                        evaluated = SUBROUTINE_START :: evaluated
                        memoryLayoutBeforeSubroutineCall =
                            (operandsArray.clone, localsArray.clone) :: memoryLayoutBeforeSubroutineCall

                        val branchtarget = pc + as[JSRInstruction](instruction).branchoffset
                        val newOperands = theDomain.ReturnAddressValue(returnTarget) :: operands
                        gotoTarget(
                            pc,
                            branchtarget,
                            false,
                            newOperands,
                            locals)

                        if (tracer.isDefined) {
                            tracer.get.jumpToSubroutine(theDomain)(pc, branchtarget,memoryLayoutBeforeSubroutineCall.size)
                        }

                    case 169 /*ret*/ ⇒
                        val lvIndex = as[RET](instruction).lvIndex
                        // we now know the local variable that is used - we replace
                        // the SUBROUTINE_START marker by the local variable index
                        // to make it possible to later on clear it...
                        val oldWorklist = worklist
                        var beginning = List.empty[PC]
                        var tail = worklist
                        while (tail.head >= 0) { // until we found the subroutine marker or the "-local variable index" 
                            beginning = tail.head :: beginning
                            tail = tail.tail
                        }
                        worklist = beginning.reverse ::: (-lvIndex :: pc :: tail.tail)
                        if (tracer.isDefined) {
                            tracer.get.ret(theDomain)(
                                pc,
                                locals(lvIndex).asReturnAddressValue,
                                oldWorklist,
                                worklist)
                        }

                    //
                    // CONDITIONAL TRANSFER OF CONTROL
                    //

                    case 165 /*if_acmpeq*/ ⇒
                        ifTcmpXX(refAreEqual _, RefAreEqual, RefAreNotEqual)
                    case 166 /*if_acmpne*/ ⇒
                        ifTcmpXX(refAreNotEqual _, RefAreNotEqual, RefAreEqual)
                    case 198 /*ifnull*/ ⇒
                        ifXX(refIsNull _, RefIsNull, RefIsNonNull)
                    case 199 /*ifnonnull*/ ⇒
                        ifXX(refIsNonNull _, RefIsNonNull, RefIsNull)

                    case 159 /*if_icmpeq*/ ⇒
                        ifTcmpXX(intAreEqual _, IntAreEqual, IntAreNotEqual)
                    case 160 /*if_icmpne*/ ⇒
                        ifTcmpXX(intAreNotEqual _, IntAreNotEqual, IntAreEqual)
                    case 161 /*if_icmplt*/ ⇒
                        ifTcmpXX(intIsLessThan _, IntIsLessThan, IntIsGreaterThanOrEqualTo)
                    case 162 /*if_icmpge*/ ⇒
                        ifTcmpXX(intIsGreaterThanOrEqualTo _, IntIsGreaterThanOrEqualTo, IntIsLessThan)
                    case 163 /*if_icmpgt*/ ⇒
                        ifTcmpXX(intIsGreaterThan _, IntIsGreaterThan, IntIsLessThanOrEqualTo)
                    case 164 /*if_icmple*/ ⇒
                        ifTcmpXX(intIsLessThanOrEqualTo _, IntIsLessThanOrEqualTo, IntIsGreaterThan)
                    case 153 /*ifeq*/ ⇒
                        ifXX(intIs0 _, IntIs0, IntIsNot0)
                    case 154 /*ifne*/ ⇒
                        ifXX(intIsNot0 _, IntIsNot0, IntIs0)
                    case 155 /*iflt*/ ⇒
                        ifXX(intIsLessThan0 _, IntIsLessThan0, IntIsGreaterThanOrEqualTo0)
                    case 156 /*ifge*/ ⇒
                        ifXX(intIsGreaterThanOrEqualTo0 _, IntIsGreaterThanOrEqualTo0, IntIsLessThan0)
                    case 157 /*ifgt*/ ⇒
                        ifXX(intIsGreaterThan0 _, IntIsGreaterThan0, IntIsLessThanOrEqualTo0)
                    case 158 /*ifle */ ⇒
                        ifXX(intIsLessThanOrEqualTo0 _, IntIsLessThanOrEqualTo0, IntIsGreaterThan0)

                    case 171 /*lookupswitch*/ ⇒
                        val switch = instructions(pc).asInstanceOf[LOOKUPSWITCH]
                        val index = operands.head
                        val remainingOperands = operands.tail
                        if (switch.npairs.isEmpty) {
                            // in the Java 7 JDK 45 we actually had found a lookupswitch
                            // that just had a defaultBranch (glorified "goto")
                            gotoTarget(
                                pc, pc + switch.defaultOffset, false,
                                remainingOperands, locals)
                        } else {
                            var branchToDefaultRequired = false
                            val firstKey = switch.npairs(0)._1
                            var previousKey = firstKey
                            for ((key, offset) ← switch.npairs) {
                                if (!branchToDefaultRequired && (key - previousKey) > 1) {
                                    val domainValueMayBeOutOfRange: Boolean =
                                        (previousKey until key) exists { v ⇒
                                            theDomain.intIsSomeValueInRange(index, v, v).
                                                isYesOrUnknown
                                        }
                                    if (domainValueMayBeOutOfRange) {
                                        branchToDefaultRequired = true
                                    } else {
                                        previousKey = key
                                    }
                                }
                                if (theDomain.intIsSomeValueInRange(index, key, key).isYesOrUnknown) {
                                    val branchTarget = pc + offset
                                    val (updatedOperands, updatedLocals) =
                                        theDomain.intEstablishValue(
                                            branchTarget, key, index, remainingOperands, locals)
                                    if (tracer.isDefined &&
                                        ((remainingOperands ne updatedOperands) || (locals ne updatedLocals))) {
                                        tracer.get.establishedConstraint(theDomain)(
                                            pc, branchTarget, remainingOperands, locals, updatedOperands, updatedLocals
                                        )
                                    }
                                    gotoTarget(
                                        pc, branchTarget, false,
                                        updatedOperands, updatedLocals)
                                }
                            }
                            if (branchToDefaultRequired ||
                                theDomain.intIsSomeValueNotInRange(
                                    index,
                                    firstKey,
                                    switch.npairs(switch.npairs.size - 1)._1).isYesOrUnknown) {
                                gotoTarget(
                                    pc, pc + switch.defaultOffset, false,
                                    remainingOperands, locals)
                            }
                        }

                    case 170 /*tableswitch*/ ⇒
                        val tableswitch = instructions(pc).asInstanceOf[TABLESWITCH]
                        val index = operands.head
                        val remainingOperands = operands.tail
                        val low = tableswitch.low
                        val high = tableswitch.high
                        var v = low
                        while (v <= high) {

                            if (theDomain.intIsSomeValueInRange(index, v, v).isYesOrUnknown) {
                                val branchTarget = pc + tableswitch.jumpOffsets(v - low)
                                val (updatedOperands, updatedLocals) =
                                    theDomain.intEstablishValue(
                                        branchTarget, v, index, remainingOperands, locals)
                                if (tracer.isDefined &&
                                    ((remainingOperands ne updatedOperands) || (locals ne updatedLocals))) {
                                    tracer.get.establishedConstraint(theDomain)(
                                        pc, branchTarget, remainingOperands, locals, updatedOperands, updatedLocals
                                    )
                                }
                                gotoTarget(
                                    pc, branchTarget, false,
                                    updatedOperands, updatedLocals)
                            }
                            v = v + 1
                        }
                        if (theDomain.intIsSomeValueNotInRange(index, low, high).isYesOrUnknown) {
                            gotoTarget(
                                pc, pc + tableswitch.defaultOffset, false,
                                remainingOperands, locals)
                        }

                    //
                    // STATEMENTS THAT CAN CAUSE EXCEPTIONELL TRANSFER OF CONTROL FLOW
                    // 

                    case 191 /*athrow*/ ⇒
                        // In general, we either have a control flow to an exception handler 
                        // or we abort the method.
                        // EXCERPT FROM THE SPEC:
                        // Within a class file the exception handlers for each method are 
                        // stored in a table. At runtime the Java virtual machine searches 
                        // the exception handlers of the current method in the order that 
                        // they appear in the corresponding exception handler table.
                        val exceptionValue = operands.head
                        val isExceptionValueNull = theDomain.refIsNull(exceptionValue)
                        if (isExceptionValueNull.isYesOrUnknown) {
                            // if the operand of the athrow exception is null, a new 
                            // NullPointerException is raised by the JVM
                            // if the operand of the athrow exception is null, a new 
                            // NullPointerException is raised by the JVM
                            handleException(
                                theDomain.InitializedObjectValue(
                                    pc, ObjectType.NullPointerException))
                        }
                        if (isExceptionValueNull.isNoOrUnknown) {
                            val (updatedOperands, updatedLocals) = {
                                val operands = List(exceptionValue)
                                if (isExceptionValueNull.isUnknown)
                                    theDomain.refEstablishIsNonNull(
                                        pc, exceptionValue,
                                        operands, locals)
                                else
                                    (operands, locals)
                            }
                            val updatedExceptionValue = updatedOperands.head

                            theDomain.typeOfValue(exceptionValue) match {
                                case TypeUnknown ⇒
                                    code.handlersFor(pc).foreach { eh ⇒
                                        val branchTarget = eh.handlerPC
                                        // unless we have a "finally" handler, we can state
                                        // a constraint
                                        if (eh.catchType.isDefined) {
                                            eh.catchType.map { catchType ⇒
                                                val (updatedOperands2, updatedLocals2) =
                                                    theDomain.refEstablishUpperBound(
                                                        branchTarget,
                                                        catchType,
                                                        exceptionValue,
                                                        updatedOperands,
                                                        updatedLocals)
                                                gotoTarget(
                                                    pc, branchTarget, true,
                                                    updatedOperands2, updatedLocals2)
                                            }
                                        } else
                                            // finally handler
                                            gotoTarget(
                                                pc, branchTarget, true,
                                                updatedOperands, updatedLocals)
                                    }
                                    abruptMethodExecution(pc, exceptionValue)

                                case IsReferenceValue(referenceValues) ⇒
                                    // TODO [issue or documentation lacking] Shouldn't it be a foreach loop in case of "throw (if(x) ExA else ExB)"
                                    val isHandled = referenceValues.forall(referenceValue ⇒
                                        // find the exception handler that matches the given 
                                        // exception
                                        code.handlersFor(pc).exists { eh ⇒
                                            val branchTarget = eh.handlerPC
                                            val catchType = eh.catchType
                                            if (catchType.isEmpty) {
                                                gotoTarget(
                                                    pc, branchTarget, true,
                                                    updatedOperands, updatedLocals)
                                                // this is a finally handler
                                                true
                                            } else {
                                                // a "null value" is automatically converted
                                                // into a NullPointerException
                                                val subtypeOfAnswer = {
                                                    if (referenceValue.isNull.isYes)
                                                        theDomain.isSubtypeOf(ObjectType.NullPointerException, catchType.get)
                                                    else
                                                        referenceValue.isValueSubtypeOf(catchType.get)
                                                }
                                                subtypeOfAnswer match {
                                                    case No ⇒
                                                        false
                                                    case Yes ⇒
                                                        gotoTarget(
                                                            pc, branchTarget, true,
                                                            updatedOperands, updatedLocals)
                                                        true
                                                    case Unknown ⇒
                                                        val (updatedOperands2, updatedLocals2) =
                                                            theDomain.refEstablishUpperBound(
                                                                branchTarget,
                                                                catchType.get,
                                                                exceptionValue,
                                                                updatedOperands,
                                                                updatedLocals)
                                                        gotoTarget(
                                                            pc, branchTarget, true,
                                                            updatedOperands2, updatedLocals2)
                                                        false
                                                }
                                            }
                                        }
                                    )
                                    // If "isHandled" is true, we are sure that at least one 
                                    // handler will catch the exception(s)... hence the method
                                    // will not complete abruptly
                                    if (!isHandled)
                                        abruptMethodExecution(pc, exceptionValue)
                            }
                        }

                    //
                    // CREATE ARRAY
                    //

                    case 188 /*newarray*/ ⇒
                        val count :: rest = operands
                        val atype = instruction.asInstanceOf[NEWARRAY].atype
                        val computation = (atype: @annotation.switch) match {
                            case BooleanType.atype ⇒
                                theDomain.newarray(pc, count, BooleanType)
                            case CharType.atype ⇒
                                theDomain.newarray(pc, count, CharType)
                            case FloatType.atype ⇒
                                theDomain.newarray(pc, count, FloatType)
                            case DoubleType.atype ⇒
                                theDomain.newarray(pc, count, DoubleType)
                            case ByteType.atype ⇒
                                theDomain.newarray(pc, count, ByteType)
                            case ShortType.atype ⇒
                                theDomain.newarray(pc, count, ShortType)
                            case IntegerType.atype ⇒
                                theDomain.newarray(pc, count, IntegerType)
                            case LongType.atype ⇒
                                theDomain.newarray(pc, count, LongType)
                        }
                        computationWithReturnValueAndException(computation, rest)

                    case 189 /*anewarray*/ ⇒
                        val count :: rest = operands
                        val componentType = instruction.asInstanceOf[ANEWARRAY].componentType
                        val computation = theDomain.newarray(pc, count, componentType)
                        computationWithReturnValueAndException(computation, rest)

                    case 197 /*multianewarray*/ ⇒
                        val multianewarray = instruction.asInstanceOf[MULTIANEWARRAY]
                        val dimensions = multianewarray.dimensions
                        val dimensionSizes = operands.take(multianewarray.dimensions)
                        val componentType = multianewarray.componentType
                        val computation = theDomain.multianewarray(pc, dimensionSizes, componentType)
                        computationWithReturnValueAndException(computation, operands.drop(dimensions))

                    //
                    // LOAD FROM AND STORE VALUE IN ARRAYS
                    //

                    case 50 /*aaload*/ ⇒ {
                        val index :: arrayref :: rest = operands
                        val computation = theDomain.aaload(pc, index, arrayref)
                        computationWithReturnValueAndExceptions(computation, rest)
                    }
                    case 83 /*aastore*/ ⇒ {
                        val value :: index :: arrayref :: rest = operands
                        val computation = theDomain.aastore(pc, value, index, arrayref)
                        computationWithExceptions(computation, rest)
                    }

                    case 51 /*baload*/ ⇒ {
                        val index :: arrayref :: rest = operands
                        val computation = theDomain.baload(pc, index, arrayref)
                        computationWithReturnValueAndExceptions(computation, rest)
                    }
                    case 84 /*bastore*/ ⇒ {
                        val value :: index :: arrayref :: rest = operands
                        val computation = theDomain.bastore(pc, value, index, arrayref)
                        computationWithExceptions(computation, rest)
                    }

                    case 52 /*caload*/ ⇒ {
                        val index :: arrayref :: rest = operands
                        val computation = theDomain.caload(pc, index, arrayref)
                        computationWithReturnValueAndExceptions(computation, rest)
                    }
                    case 85 /*castore*/ ⇒ {
                        val value :: index :: arrayref :: rest = operands
                        val computation = theDomain.castore(pc, value, index, arrayref)
                        fallThrough(rest)
                    }

                    case 49 /*daload*/ ⇒ {
                        val index :: arrayref :: rest = operands
                        val computation = theDomain.daload(pc, index, arrayref)
                        computationWithReturnValueAndExceptions(computation, rest)
                    }
                    case 82 /*dastore*/ ⇒ {
                        val value :: index :: arrayref :: rest = operands
                        val computation = theDomain.dastore(pc, value, index, arrayref)
                        computationWithExceptions(computation, rest)
                    }

                    case 48 /*faload*/ ⇒ {
                        val index :: arrayref :: rest = operands
                        val computation = theDomain.faload(pc, index, arrayref)
                        computationWithReturnValueAndExceptions(computation, rest)
                    }
                    case 81 /*fastore*/ ⇒ {
                        val value :: index :: arrayref :: rest = operands
                        val computation = theDomain.fastore(pc, value, index, arrayref)
                        computationWithExceptions(computation, rest)
                    }

                    case 46 /*iaload*/ ⇒ {
                        val index :: arrayref :: rest = operands
                        val computation = theDomain.iaload(pc, index, arrayref)
                        computationWithReturnValueAndExceptions(computation, rest)
                    }
                    case 79 /*iastore*/ ⇒ {
                        val value :: index :: arrayref :: rest = operands
                        val computation = theDomain.iastore(pc, value, index, arrayref)
                        computationWithExceptions(computation, rest)
                    }

                    case 47 /*laload*/ ⇒ {
                        val index :: arrayref :: rest = operands
                        val computation = theDomain.laload(pc, index, arrayref)
                        computationWithReturnValueAndExceptions(computation, rest)
                    }
                    case 80 /*lastore*/ ⇒ {
                        val value :: index :: arrayref :: rest = operands
                        val computation = theDomain.lastore(pc, value, index, arrayref)
                        computationWithExceptions(computation, rest)
                    }

                    case 53 /*saload*/ ⇒ {
                        val index :: arrayref :: rest = operands
                        val computation = theDomain.saload(pc, index, arrayref)
                        computationWithReturnValueAndExceptions(computation, rest)
                    }
                    case 86 /*sastore*/ ⇒ {
                        val value :: index :: arrayref :: rest = operands
                        val computation = theDomain.sastore(pc, value, index, arrayref)
                        computationWithExceptions(computation, rest)
                    }

                    //
                    // LENGTH OF AN ARRAY
                    //

                    case 190 /*arraylength*/ ⇒ {
                        val arrayref = operands.head
                        val computation = theDomain.arraylength(pc, arrayref)
                        computationWithReturnValueAndException(computation, operands.tail)
                    }

                    //
                    // ACCESSING FIELDS
                    //
                    case 180 /*getfield*/ ⇒ {
                        val getfield = instruction.asInstanceOf[GETFIELD]
                        computationWithReturnValueAndException(
                            theDomain.getfield(
                                pc,
                                operands.head,
                                getfield.declaringClass,
                                getfield.name,
                                getfield.fieldType), operands.tail)
                    }
                    case 178 /*getstatic*/ ⇒ {
                        val getstatic = instruction.asInstanceOf[GETSTATIC]
                        computationWithReturnValueAndException(
                            theDomain.getstatic(
                                pc,
                                getstatic.declaringClass,
                                getstatic.name,
                                getstatic.fieldType), operands)
                    }
                    case 181 /*putfield*/ ⇒ {
                        val putfield = instruction.asInstanceOf[PUTFIELD]
                        val value :: objectref :: rest = operands
                        computationWithException(
                            theDomain.putfield(
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
                        computationWithException(
                            theDomain.putstatic(
                                pc,
                                value,
                                putstatic.declaringClass,
                                putstatic.name,
                                putstatic.fieldType), rest)
                    }

                    //
                    // METHOD INVOCATIONS
                    //
                    case 186 /*invokedynamic*/ ⇒
                        val invoke = instruction.asInstanceOf[INVOKEDYNAMIC]
                        val argsCount = invoke.methodDescriptor.parametersCount
                        val computation =
                            theDomain.invokedynamic(
                                pc,
                                invoke.bootstrapMethod,
                                invoke.name,
                                invoke.methodDescriptor,
                                operands.take(argsCount)
                            )
                        computationWithReturnValueAndExceptions(
                            computation,
                            operands.drop(argsCount)
                        )

                    case 185 /*invokeinterface*/ ⇒
                        val invoke = instruction.asInstanceOf[INVOKEINTERFACE]
                        val argsCount = invoke.methodDescriptor.parametersCount
                        val computation =
                            theDomain.invokeinterface(
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
                        val argsCount = invoke.methodDescriptor.parametersCount
                        val computation =
                            theDomain.invokespecial(
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
                        val argsCount = invoke.methodDescriptor.parametersCount
                        val computation =
                            theDomain.invokestatic(
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
                        val argsCount = invoke.methodDescriptor.parametersCount
                        val computation =
                            theDomain.invokevirtual(
                                pc,
                                invoke.declaringClass,
                                invoke.name,
                                invoke.methodDescriptor,
                                operands.take(argsCount + 1)
                            )
                        computationWithOptionalReturnValueAndExceptions(
                            computation,
                            operands.drop(argsCount + 1))

                    case 194 /*monitorenter*/ ⇒
                        val computation = theDomain.monitorenter(pc, operands.head)
                        computationWithException(computation, operands.tail)

                    case 195 /*monitorexit*/ ⇒
                        val computation = theDomain.monitorexit(pc, operands.head)
                        computationWithException(computation, operands.tail)

                    //
                    // RETURN FROM METHOD
                    //
                    case 176 /*areturn*/ ⇒ theDomain.areturn(pc, operands.head)
                    case 175 /*dreturn*/ ⇒ theDomain.dreturn(pc, operands.head)
                    case 174 /*freturn*/ ⇒ theDomain.freturn(pc, operands.head)
                    case 172 /*ireturn*/ ⇒ theDomain.ireturn(pc, operands.head)
                    case 173 /*lreturn*/ ⇒ theDomain.lreturn(pc, operands.head)
                    case 177 /*return*/  ⇒ theDomain.returnVoid(pc)

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
                        | 22 /*lload*/ ⇒
                        val lvIndex = as[LoadLocalVariableInstruction](instruction).lvIndex
                        fallThrough(locals(lvIndex) :: operands)
                    case 42 /*aload_0*/
                        | 38 /*dload_0*/
                        | 34 /*fload_0*/
                        | 26 /*iload_0*/
                        | 30 /*lload_0*/ ⇒
                        fallThrough(locals(0) :: operands)
                    case 43 /*aload_1*/
                        | 39 /*dload_1*/
                        | 35 /*fload_1*/
                        | 27 /*iload_1*/
                        | 31 /*lload_1*/ ⇒
                        fallThrough(locals(1) :: operands)
                    case 44 /*aload_2*/
                        | 40 /*dload_2*/
                        | 36 /*fload_2*/
                        | 28 /*iload_2*/
                        | 32 /*lload_2*/ ⇒
                        fallThrough(locals(2) :: operands)
                    case 45 /*aload_3*/
                        | 41 /*dload_3*/
                        | 37 /*fload_3*/
                        | 29 /*iload_3*/
                        | 33 /*lload_3*/ ⇒
                        fallThrough(locals(3) :: operands)

                    //
                    // STORE OPERAND IN LOCAL VARIABLE
                    //
                    case 58 /*astore*/
                        | 57 /*dstore*/
                        | 56 /*fstore*/
                        | 54 /*istore*/
                        | 55 /*lstore*/ ⇒
                        val lvIndex = as[StoreLocalVariableInstruction](instruction).lvIndex
                        fallThrough(
                            operands.tail,
                            locals.updated(lvIndex, operands.head))
                    case 75 /*astore_0*/
                        | 71 /*dstore_0*/
                        | 67 /*fstore_0*/
                        | 63 /*lstore_0*/
                        | 59 /*istore_0*/ ⇒
                        fallThrough(
                            operands.tail, locals.updated(0, operands.head))
                    case 76 /*astore_1*/
                        | 72 /*dstore_1*/
                        | 68 /*fstore_1*/
                        | 64 /*lstore_1*/
                        | 60 /*istore_1*/ ⇒
                        fallThrough(
                            operands.tail, locals.updated(1, operands.head))
                    case 77 /*astore_2*/
                        | 73 /*dstore_2*/
                        | 69 /*fstore_2*/
                        | 65 /*lstore_2*/
                        | 61 /*istore_2*/ ⇒
                        fallThrough(
                            operands.tail, locals.updated(2, operands.head))
                    case 78 /*astore_3*/
                        | 74 /*dstore_3*/
                        | 70 /*fstore_3*/
                        | 66 /*lstore_3*/
                        | 62 /*istore_3*/ ⇒
                        fallThrough(
                            operands.tail, locals.updated(3, operands.head))

                    //
                    // PUSH CONSTANT VALUE
                    //

                    case 1 /*aconst_null*/ ⇒
                        fallThrough(theDomain.NullValue(pc) :: operands)

                    case 16 /*bipush*/ ⇒
                        val value = instruction.asInstanceOf[BIPUSH].value.toByte
                        fallThrough(theDomain.ByteValue(pc, value) :: operands)

                    case 14 /*dconst_0*/ ⇒
                        fallThrough(theDomain.DoubleValue(pc, 0.0d) :: operands)
                    case 15 /*dconst_1*/ ⇒
                        fallThrough(theDomain.DoubleValue(pc, 1.0d) :: operands)

                    case 11 /*fconst_0*/ ⇒
                        fallThrough(theDomain.FloatValue(pc, 0.0f) :: operands)
                    case 12 /*fconst_1*/ ⇒
                        fallThrough(theDomain.FloatValue(pc, 1.0f) :: operands)
                    case 13 /*fconst_2*/ ⇒
                        fallThrough(theDomain.FloatValue(pc, 2.0f) :: operands)

                    case 2 /*iconst_m1*/ ⇒
                        fallThrough(theDomain.IntegerValue(pc, -1) :: operands)
                    case 3 /*iconst_0*/ ⇒
                        fallThrough(theDomain.IntegerValue(pc, 0) :: operands)
                    case 4 /*iconst_1*/ ⇒
                        fallThrough(theDomain.IntegerValue(pc, 1) :: operands)
                    case 5 /*iconst_2*/ ⇒
                        fallThrough(theDomain.IntegerValue(pc, 2) :: operands)
                    case 6 /*iconst_3*/ ⇒
                        fallThrough(theDomain.IntegerValue(pc, 3) :: operands)
                    case 7 /*iconst_4*/ ⇒
                        fallThrough(theDomain.IntegerValue(pc, 4) :: operands)
                    case 8 /*iconst_5*/ ⇒
                        fallThrough(theDomain.IntegerValue(pc, 5) :: operands)

                    case 9 /*lconst_0*/ ⇒
                        fallThrough(theDomain.LongValue(pc, 0l) :: operands)
                    case 10 /*lconst_1*/ ⇒
                        fallThrough(theDomain.LongValue(pc, 1l) :: operands)

                    case 18 /*ldc*/ ⇒ instruction match {
                        case LoadInt(v) ⇒
                            fallThrough(theDomain.IntegerValue(pc, v) :: operands)
                        case LoadFloat(v) ⇒
                            fallThrough(theDomain.FloatValue(pc, v) :: operands)
                        case LoadString(v) ⇒
                            fallThrough(theDomain.StringValue(pc, v) :: operands)
                        case LoadClass(v) ⇒
                            fallThrough(theDomain.ClassValue(pc, v) :: operands)
                        case LoadMethodHandle(v) ⇒
                            fallThrough(theDomain.MethodHandle(pc, v) :: operands)
                        case LoadMethodType(v) ⇒
                            fallThrough(theDomain.MethodType(pc, v) :: operands)

                    }
                    case 19 /*ldc_w*/ ⇒ instruction match {
                        case LoadInt_W(v) ⇒
                            fallThrough(theDomain.IntegerValue(pc, v) :: operands)
                        case LoadFloat_W(v) ⇒
                            fallThrough(theDomain.FloatValue(pc, v) :: operands)
                        case LoadString_W(v) ⇒
                            fallThrough(theDomain.StringValue(pc, v) :: operands)
                        case LoadClass_W(v) ⇒
                            fallThrough(theDomain.ClassValue(pc, v) :: operands)
                        case LoadMethodHandle_W(v) ⇒
                            fallThrough(theDomain.MethodHandle(pc, v) :: operands)
                        case LoadMethodType_W(v) ⇒
                            fallThrough(theDomain.MethodType(pc, v) :: operands)
                    }
                    case 20 /*ldc2_w*/ ⇒ instruction match {
                        case LoadLong(v) ⇒
                            fallThrough(theDomain.LongValue(pc, v) :: operands)
                        case LoadDouble(v) ⇒
                            fallThrough(theDomain.DoubleValue(pc, v) :: operands)
                    }

                    case 17 /*sipush*/ ⇒
                        val value = instruction.asInstanceOf[SIPUSH].value.toShort
                        fallThrough(theDomain.ShortValue(pc, value) :: operands)

                    //
                    // RELATIONAL OPERATORS
                    //
                    case 150 /*fcmpg*/ ⇒ {
                        val value2 :: value1 :: rest = operands
                        fallThrough(theDomain.fcmpg(pc, value1, value2) :: rest)
                    }
                    case 149 /*fcmpl*/ ⇒ {
                        val value2 :: value1 :: rest = operands
                        fallThrough(theDomain.fcmpl(pc, value1, value2) :: rest)
                    }
                    case 152 /*dcmpg*/ ⇒ {
                        val value2 :: value1 :: rest = operands
                        fallThrough(theDomain.dcmpg(pc, value1, value2) :: rest)
                    }
                    case 151 /*dcmpl*/ ⇒ {
                        val value2 :: value1 :: rest = operands
                        fallThrough(theDomain.dcmpl(pc, value1, value2) :: rest)
                    }
                    case 148 /*lcmp*/ ⇒ {
                        val value2 :: value1 :: rest = operands
                        fallThrough(theDomain.lcmp(pc, value1, value2) :: rest)
                    }

                    //
                    // UNARY EXPRESSIONS
                    //
                    case 119 /*dneg*/ ⇒
                        fallThrough(theDomain.dneg(pc, operands.head) :: (operands.tail))
                    case 118 /*fneg*/ ⇒
                        fallThrough(theDomain.fneg(pc, operands.head) :: (operands.tail))
                    case 117 /*lneg*/ ⇒
                        fallThrough(theDomain.lneg(pc, operands.head) :: (operands.tail))
                    case 116 /*ineg*/ ⇒
                        fallThrough(theDomain.ineg(pc, operands.head) :: (operands.tail))

                    //
                    // BINARY EXPRESSIONS
                    //

                    case 99 /*dadd*/ ⇒ {
                        val value2 :: value1 :: rest = operands
                        fallThrough(theDomain.dadd(pc, value1, value2) :: rest)
                    }
                    case 111 /*ddiv*/ ⇒ {
                        val value2 :: value1 :: rest = operands
                        fallThrough(theDomain.ddiv(pc, value1, value2) :: rest)
                    }
                    case 107 /*dmul*/ ⇒ {
                        val value2 :: value1 :: rest = operands
                        fallThrough(theDomain.dmul(pc, value1, value2) :: rest)
                    }
                    case 115 /*drem*/ ⇒ {
                        val value2 :: value1 :: rest = operands
                        fallThrough(theDomain.drem(pc, value1, value2) :: rest)
                    }
                    case 103 /*dsub*/ ⇒ {
                        val value2 :: value1 :: rest = operands
                        fallThrough(theDomain.dsub(pc, value1, value2) :: rest)
                    }

                    case 98 /*fadd*/ ⇒ {
                        val value2 :: value1 :: rest = operands
                        fallThrough(theDomain.fadd(pc, value1, value2) :: rest)
                    }
                    case 110 /*fdiv*/ ⇒ {
                        val value2 :: value1 :: rest = operands
                        fallThrough(theDomain.fdiv(pc, value1, value2) :: rest)
                    }
                    case 106 /*fmul*/ ⇒ {
                        val value2 :: value1 :: rest = operands
                        fallThrough(theDomain.fmul(pc, value1, value2) :: rest)
                    }
                    case 114 /*frem*/ ⇒ {
                        val value2 :: value1 :: rest = operands
                        fallThrough(theDomain.frem(pc, value1, value2) :: rest)
                    }
                    case 102 /*fsub*/ ⇒ {
                        val value2 :: value1 :: rest = operands
                        fallThrough(theDomain.fsub(pc, value1, value2) :: rest)
                    }

                    case 96 /*iadd*/ ⇒ {
                        val value2 :: value1 :: rest = operands
                        fallThrough(theDomain.iadd(pc, value1, value2) :: rest)
                    }
                    case 126 /*iand*/ ⇒ {
                        val value2 :: value1 :: rest = operands
                        fallThrough(theDomain.iand(pc, value1, value2) :: rest)
                    }
                    case 108 /*idiv*/ ⇒ {
                        val value2 :: value1 :: rest = operands
                        val computation = theDomain.idiv(pc, value1, value2)
                        computationWithReturnValueAndException(computation, rest)
                    }
                    case 104 /*imul*/ ⇒ {
                        val value2 :: value1 :: rest = operands
                        fallThrough(theDomain.imul(pc, value1, value2) :: rest)
                    }
                    case 128 /*ior*/ ⇒ {
                        val value2 :: value1 :: rest = operands
                        fallThrough(theDomain.ior(pc, value1, value2) :: rest)
                    }
                    case 112 /*irem*/ ⇒ {
                        val value2 :: value1 :: rest = operands
                        val computation = theDomain.irem(pc, value1, value2)
                        computationWithReturnValueAndException(computation, rest)
                    }
                    case 120 /*ishl*/ ⇒ {
                        val value2 :: value1 :: rest = operands
                        fallThrough(theDomain.ishl(pc, value1, value2) :: rest)
                    }
                    case 122 /*ishr*/ ⇒ {
                        val value2 :: value1 :: rest = operands
                        fallThrough(theDomain.ishr(pc, value1, value2) :: rest)
                    }
                    case 100 /*isub*/ ⇒ {
                        val value2 :: value1 :: rest = operands
                        fallThrough(theDomain.isub(pc, value1, value2) :: rest)
                    }
                    case 124 /*iushr*/ ⇒ {
                        val value2 :: value1 :: rest = operands
                        fallThrough(theDomain.iushr(pc, value1, value2) :: rest)
                    }
                    case 130 /*ixor*/ ⇒ {
                        val value2 :: value1 :: rest = operands
                        fallThrough(theDomain.ixor(pc, value1, value2) :: rest)
                    }

                    case 97 /*ladd*/ ⇒ {
                        val value2 :: value1 :: rest = operands
                        fallThrough(theDomain.ladd(pc, value1, value2) :: rest)
                    }
                    case 127 /*land*/ ⇒ {
                        val value2 :: value1 :: rest = operands
                        fallThrough(theDomain.land(pc, value1, value2) :: rest)
                    }
                    case 109 /*ldiv*/ ⇒ {
                        val value2 :: value1 :: rest = operands
                        val computation = theDomain.ldiv(pc, value1, value2)
                        computationWithReturnValueAndException(computation, rest)
                    }
                    case 105 /*lmul*/ ⇒ {
                        val value2 :: value1 :: rest = operands
                        fallThrough(theDomain.lmul(pc, value1, value2) :: rest)
                    }
                    case 129 /*lor*/ ⇒ {
                        val value2 :: value1 :: rest = operands
                        fallThrough(theDomain.lor(pc, value1, value2) :: rest)
                    }
                    case 113 /*lrem*/ ⇒ {
                        val value2 :: value1 :: rest = operands
                        val computation = theDomain.lrem(pc, value1, value2)
                        computationWithReturnValueAndException(computation, rest)
                    }
                    case 121 /*lshl*/ ⇒ {
                        val value2 :: value1 :: rest = operands
                        fallThrough(theDomain.lshl(pc, value1, value2) :: rest)
                    }
                    case 123 /*lshr*/ ⇒ {
                        val value2 :: value1 :: rest = operands
                        fallThrough(theDomain.lshr(pc, value1, value2) :: rest)
                    }
                    case 101 /*lsub*/ ⇒ {
                        val value2 :: value1 :: rest = operands
                        fallThrough(theDomain.lsub(pc, value1, value2) :: rest)
                    }
                    case 125 /*lushr*/ ⇒ {
                        val value2 :: value1 :: rest = operands
                        fallThrough(theDomain.lushr(pc, value1, value2) :: rest)
                    }
                    case 131 /*lxor*/ ⇒ {
                        val value2 :: value1 :: rest = operands
                        fallThrough(theDomain.lxor(pc, value1, value2) :: rest)
                    }
                    //
                    // GENERIC STACK MANIPULATION
                    //
                    case 89 /*dup*/ ⇒
                        fallThrough((operands.head) :: operands)
                    case 90 /*dup_x1*/ ⇒
                        val v1 :: v2 :: rest = operands
                        fallThrough(v1 :: v2 :: v1 :: rest)
                    case 91 /*dup_x2*/ ⇒ operands match {
                        case (v1 /*@ CTC1()*/ ) :: (v2 @ CTC1()) :: (v3 /*@ CTC1()*/ ) :: rest ⇒
                            fallThrough(v1 :: v2 :: v3 :: v1 :: rest)
                        case (v1 /*@ CTC1()*/ ) :: v2 /* @ CTC2()*/ :: rest ⇒
                            fallThrough(v1 :: v2 :: v1 :: rest)
                    }
                    case 92 /*dup2*/ ⇒ operands match {
                        case (v1 @ CTC1()) :: (v2 /*@ CTC1()*/ ) :: _ ⇒
                            fallThrough(v1 :: v2 :: operands)
                        case (v /*@ CTC2()*/ ) :: _ ⇒
                            fallThrough(v :: operands)
                    }
                    case 93 /*dup2_x1*/ ⇒ operands match {
                        case (v1 @ CTC1()) :: (v2 /*@ CTC1()*/ ) :: (v3 /*@ CTC1()*/ ) :: rest ⇒
                            fallThrough(v1 :: v2 :: v3 :: v1 :: v2 :: rest)
                        case (v1 @ CTC2()) :: (v2 /*@ CTC1()*/ ) :: rest ⇒
                            fallThrough(v1 :: v2 :: v1 :: rest)
                    }
                    case 94 /*dup2_x2*/ ⇒ operands match {
                        case (v1 @ CTC1()) :: (v2 @ CTC1()) :: (v3 @ CTC1()) :: (v4 /*@ CTC1()*/ ) :: rest ⇒
                            fallThrough(v1 :: v2 :: v3 :: v4 :: v1 :: v2 :: rest)
                        case (v1 @ CTC2()) :: (v2 @ CTC1()) :: (v3 @ CTC1()) :: rest ⇒
                            fallThrough(v1 :: v2 :: v3 :: v1 :: rest)
                        case (v1 @ CTC1()) :: (v2 @ CTC1()) :: (v3 @ CTC2()) :: rest ⇒
                            fallThrough(v1 :: v2 :: v3 :: v1 :: v2 :: rest)
                        case (v1 /*@ CTC2()*/ ) :: (v2 /*@ CTC1()*/ ) :: rest ⇒
                            fallThrough(v1 :: v2 :: v1 :: rest)
                    }

                    case 87 /*pop*/ ⇒
                        fallThrough(operands.tail)
                    case 88 /*pop2*/ ⇒
                        if (operands.head.computationalType.operandSize == 1)
                            fallThrough(operands.drop(2))
                        else
                            fallThrough(operands.tail)

                    case 95 /*swap*/ ⇒ {
                        val v1 :: v2 :: rest = operands
                        fallThrough(v2 :: v1 :: rest)
                    }

                    //
                    // TYPE CONVERSION
                    //
                    case 144 /*d2f*/ ⇒
                        fallThrough(theDomain.d2f(pc, operands.head) :: (operands.tail))
                    case 142 /*d2i*/ ⇒
                        fallThrough(theDomain.d2i(pc, operands.head) :: (operands.tail))
                    case 143 /*d2l*/ ⇒
                        fallThrough(theDomain.d2l(pc, operands.head) :: (operands.tail))

                    case 141 /*f2d*/ ⇒
                        fallThrough(theDomain.f2d(pc, operands.head) :: (operands.tail))
                    case 139 /*f2i*/ ⇒
                        fallThrough(theDomain.f2i(pc, operands.head) :: (operands.tail))
                    case 140 /*f2l*/ ⇒
                        fallThrough(theDomain.f2l(pc, operands.head) :: (operands.tail))

                    case 145 /*i2b*/ ⇒
                        fallThrough(theDomain.i2b(pc, operands.head) :: (operands.tail))
                    case 146 /*i2c*/ ⇒
                        fallThrough(theDomain.i2c(pc, operands.head) :: (operands.tail))
                    case 135 /*i2d*/ ⇒
                        fallThrough(theDomain.i2d(pc, operands.head) :: (operands.tail))
                    case 134 /*i2f*/ ⇒
                        fallThrough(theDomain.i2f(pc, operands.head) :: (operands.tail))
                    case 133 /*i2l*/ ⇒
                        fallThrough(theDomain.i2l(pc, operands.head) :: (operands.tail))
                    case 147 /*i2s*/ ⇒
                        fallThrough(theDomain.i2s(pc, operands.head) :: (operands.tail))

                    case 138 /*l2d*/ ⇒
                        fallThrough(theDomain.l2d(pc, operands.head) :: (operands.tail))
                    case 137 /*l2f*/ ⇒
                        fallThrough(theDomain.l2f(pc, operands.head) :: (operands.tail))
                    case 136 /*l2i*/ ⇒
                        fallThrough(theDomain.l2i(pc, operands.head) :: (operands.tail))

                    case 192 /*checkcast*/ ⇒
                        val objectref = operands.head
                        val supertype = instruction.asInstanceOf[CHECKCAST].referenceType
                        if (theDomain.refIsNull(objectref).isYes)
                            // if objectref is null => UNCHANGED (see spec. for details)
                            fallThrough()
                        else {
                            import ObjectType.ClassCastException
                            theDomain.isValueSubtypeOf(objectref, supertype) match {
                                case Yes ⇒
                                    // if objectref is a subtype => UNCHANGED
                                    fallThrough()
                                case No ⇒
                                    val ex =
                                        theDomain.InitializedObjectValue(
                                            pc, ClassCastException)
                                    handleException(ex)
                                case Unknown ⇒
                                    val ex =
                                        theDomain.InitializedObjectValue(
                                            pc, ClassCastException)
                                    handleException(ex)
                                    val (newOperands, newLocals) =
                                        theDomain.refEstablishUpperBound(
                                            pc,
                                            supertype, objectref,
                                            operands, locals)
                                    fallThrough(newOperands, newLocals)
                            }
                        }

                    //
                    // "OTHER" INSTRUCTIONS
                    //

                    case 193 /*instanceof*/ ⇒ {
                        val objectref :: rest = operands
                        val referenceType = as[INSTANCEOF](instruction).referenceType

                        val result =
                            if (theDomain.refIsNull(objectref).isYes)
                                theDomain.BooleanValue(pc, false)
                            else
                                theDomain.isValueSubtypeOf(objectref, referenceType) match {
                                    case Yes     ⇒ theDomain.BooleanValue(pc, true)
                                    case No      ⇒ theDomain.BooleanValue(pc, false)
                                    case Unknown ⇒ theDomain.BooleanValue(pc)
                                }
                        fallThrough(result :: rest)
                    }

                    case 132 /*iinc*/ ⇒ {
                        val iinc = instruction.asInstanceOf[IINC]
                        val newValue = theDomain.iinc(pc, locals(iinc.lvIndex), iinc.constValue)
                        fallThrough(operandsArray(pc), locals.updated(iinc.lvIndex, newValue))
                    }

                    case 187 /*new*/ ⇒ {
                        val newObject = instruction.asInstanceOf[NEW]
                        fallThrough(theDomain.NewObject(pc, newObject.objectType) :: operands)
                    }

                    case 0 /*nop*/    ⇒ fallThrough()
                    case 196 /*wide*/ ⇒ fallThrough()

                    case opcode ⇒
                        throw new BytecodeProcessingFailedException(
                            "unsupported opcode: "+opcode)
                }

                theDomain.evaluationCompleted(
                    pc, worklist, evaluated, operandsArray, localsArray, tracer)
            } catch {
                case ct: ControlThrowable ⇒
                    throw ct

                case cause @ DomainException(message) ⇒
                    throw InterpretationFailedException(
                        cause, theDomain)(
                            pc, worklist, evaluated,
                            operandsArray, localsArray, memoryLayoutBeforeSubroutineCall)

                case cause: Throwable ⇒
                    throw InterpretationFailedException(
                        cause, theDomain)(
                            pc, worklist, evaluated,
                            operandsArray, localsArray, memoryLayoutBeforeSubroutineCall)
            }
        }

        import AIResultBuilder.completed
        val result = completed(code, theDomain)(evaluated, operandsArray, localsArray)
        theDomain.abstractInterpretationEnded(result)
        if (tracer.isDefined) tracer.get.result(result)
        result
    }
}

/**
 * The `AI` trait's companion object that primarily defines instance independent values.
 *
 * @author Michael Eichberg
 */
private object AI {

    /**
     * The list of program counters (`List(0)`) that is used when we analysis a method
     * right from the beginning.
     */
    final val initialWorkList: List[PC] = List(0)

}

/**
 * Facilitates matching against values of computational type category 1.
 *
 * @example
 * {{{
 * case v @ CTC1() => ...
 * }}}
 *
 * @author Michael Eichberg
 */
object CTC1 {
    def unapply[D <: Domain](value: D#DomainValue): Boolean =
        value.computationalType.category == 1
}

/**
 * Facilitates matching against values of computational type category 2.
 *
 * @example
 * {{{
 * case v @ CTC2() => ...
 * }}}
 *
 * @author Michael Eichberg
 */
object CTC2 {
    def unapply[D <: Domain](value: D#DomainValue): Boolean =
        value.computationalType.category == 2
}

