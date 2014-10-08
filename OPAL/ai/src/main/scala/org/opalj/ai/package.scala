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

import scala.language.existentials

import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.Code
import org.opalj.br.instructions.Instruction

/**
 * Implementation of an abstract interpretation framework – also referred to as OPAL.
 *
 * Please note, that OPAL/the abstract interpreter just refers to the classes and traits
 * defined in this package (`ai`). The classes and traits defined in the sub-packages
 * (in particular in `domain`) are not considered to be part of the core of OPAL/the
 * abstract interpreter.
 *
 * @note This framework assumes that the analyzed bytecode is valid; i.e., the JVM's
 *      bytecode verifier would be able to verify the code. Furthermore, load-time errors
 *      (e.g., `LinkageErrors`) are – by default – completely ignored to facilitate the
 *      analysis of parts of a project. In general, if the presented bytecode is not valid,
 *      the result is undefined (i.e., OPAL may report meaningless results, crash or run
 *      indefinitely).
 *
 * @see [[org.opalj.ai.AI]] - Implements the abstract interpreter that
 *      processes a methods code and uses an analysis-specific domain to perform the
 *      abstract computations.
 * @see [[org.opalj.ai.Domain]] - The core interface between the abstract
 *      interpretation framework and the abstract domain that is responsible for
 *      performing the abstract computations.
 *
 * @author Michael Eichberg
 */
package object ai {

    /**
     * Type alias that can be used if the AI can use all kinds of domains.
     *
     * @note This type alias serves comprehension purposes only.
     */
    type SomeAI[D <: Domain] = AI[_ >: D]

    /**
     * A value of type `ValueOrigin` identifies the origin of a value. In most cases the
     * value is equal to the program counter of the instruction that created the value.
     * However, for the values passed to a method, the index is conceptually:
     *  `-1-(isStatic ? 0 : 1)-(the index of the parameter adjusted by the computational
     * type of the previous parameters)`.
     *
     * For example, in case of an instance method with the signature:
     * {{{
     * public void (double d/*parameter index:0*/, Object o/*parameter index:1*/){...}
     * }}}
     *
     *  - The value `-1` is used to identify the implicit `this` reference.
     *
     *  - The value `-2` identifies the value of the parameter `d`.
     *
     *  - The value `-4` identifies the parameter `o`. (The parameter `d` is a value of
     * computational-type category 2 and needs two stack/operands values.)
     *
     * The range of values is: [-257,65535]. Hence, whenever a value of type `ValueOrigin`
     * is required/is expected it is possible to use a value with type `PC` unless
     * the program counters identifies the start of a subroutine ([[SUBROUTINE_START]],
     * [[SUBROUTINE_END]], [[SUBROUTINE]]).
     *
     * Recall that the maximum size of the method
     * parameters array is 255. If necessary, the first slot is required for the `this`
     * reference. Furthermore, for `long` and `double` values two slots are necessary; hence
     * the smallest number used to encode that the value is an actual parameter is
     * `-256`.
     *
     * The value `-257` is used to encode that the origin of the value is out
     * of the scope of the analyzed program ([[ConstantValueOrigin]]). This value is
     * currently only used for the implicit value of `IF_XXX` instructions.
     */
    type ValueOrigin = Int

    type PrimitiveValuesFactory = IntegerValuesFactory with LongValuesFactory with FloatValuesFactory with DoubleValuesFactory
    type ValuesFactory = PrimitiveValuesFactory with ReferenceValuesFactory with VMLevelExceptionsFactory with TypedValuesFactory
    type TargetDomain = ValuesDomain with ValuesFactory

    /**
     * Special value that is added to the ''work list''/''list of evaluated instructions''
     * before the '''program counter of the first instruction''' of a subroutine.
     *
     * The marker [[SUBROUTINE]] is used to mark the place in the worklist where we
     * start having information about subroutines.
     */
    // Some value smaller than -65536 to avoid confusion with local variable indexes.
    final val SUBROUTINE_START = -80000008

    /**
     * Special value that is added to the list of `evaluated instructions`
     * to mark the end of the evaluation of a subroutine. (I.e., this value
     * is not directly used by the AI during the interpretation, but to record the
     * progress.)
     */
    final val SUBROUTINE_END = -88888888

    /**
     * A special value that is larger than all other values used to mark boundaries
     * and information related to the handling of subroutines and which is smaller
     * that all other regular values.
     */
    final val SUBROUTINE_INFORMATION_BLOCK_SEPARATOR_BOUND = -80000000

    final val SUBROUTINE_RETURN_ADDRESS_LOCAL_VARIABLE = -88880008

    final val SUBROUTINE_RETURN_TO_TARGET = -80008888

    /**
     * Special value that is added to the work list to mark the beginning of a
     * subroutine call.
     */
    final val SUBROUTINE = -90000009 // some value smaller than -2^16

    /**
     * Used to identify that the origin of the value is outside of the program.
     *
     * For example, the VM sometimes performs comparisons against predetermined fixed
     * values (specified in the JVM Spec.). The origin associated with such values is
     * determined by this value.
     */
    final val ConstantValueOrigin: ValueOrigin = -257

    type Operands[T >: Null <: ValuesDomain#DomainValue] = List[T]
    type AnOperandsArray[T >: Null <: ValuesDomain#DomainValue] = Array[Operands[T]]
    type TheOperandsArray[T >: Null <: (ValuesDomain with Singleton)#Operands] = Array[T]

    type Locals[T >: Null <: ValuesDomain#DomainValue] = org.opalj.collection.mutable.Locals[T]
    type ALocalsArray[T >: Null <: ValuesDomain#DomainValue] = Array[Locals[T]]
    type TheLocalsArray[T >: Null <: (ValuesDomain with Singleton)#Locals] = Array[T]

    /**
     * Creates a human-readable textual representation of the current memory layout.
     */
    def memoryLayoutToText(
        domain: Domain)(
            operandsArray: domain.OperandsArray,
            localsArray: domain.LocalsArray): String = {
        (
            for {
                ((operands, locals), pc) ← operandsArray.zip(localsArray).zipWithIndex
                if operands != null /*|| locals != null*/
            } yield {
                val localsWithIndex =
                    for {
                        (local, index) ← locals.zipWithIndex
                        if local ne null
                    } yield {
                        "("+index+":"+local+")"
                    }

                "PC: "+pc + operands.mkString("\n\tOperands: ", " <- ", "") +
                    localsWithIndex.mkString("\n\tLocals: [", ",", "]")
            }
        ).mkString("Operands and Locals: \n", "\n", "\n")
    }

    /**
     * Calculates the initial "PC" associated with a method's parameter.
     *
     * @param isStaticMethod True if method is static and, hence, has no implicit
     *      parameter for `this`.
     * @see [[mapOperandsToParameters]]
     */
    def parameterToValueIndex(
        isStaticMethod: Boolean,
        descriptor: MethodDescriptor,
        parameterIndex: Int): Int = {

        def origin(localVariableIndex: Int) = -localVariableIndex - 1

        var localVariableIndex = 0

        if (!isStaticMethod) {
            localVariableIndex += 1 /*=="this".computationalType.operandSize*/
        }
        val parameterTypes = descriptor.parameterTypes
        var currentIndex = 0
        while (currentIndex < parameterIndex) {
            localVariableIndex += parameterTypes(currentIndex).computationalType.operandSize
            currentIndex += 1
        }
        origin(localVariableIndex)
    }

    /**
     * Maps a list of operands (e.g., as passed to the `invokeXYZ` instructions) to
     * the list of parameters for the given method. The parameters are stored in the
     * local variables ([[Locals]])/registers of the method; i.e., this method
     * creates an initial assignment for the local variables that can directly
     * be used to pass them to [[AI]]'s
     * `perform(...)(<initialOperands = Nil>,initialLocals)` method.
     *
     * @param operands The list of operands used to call the given method. The length
     *      of the list must be:
     *      {{{
     *      calledMethod.descriptor.parametersCount + { if (calledMethod.isStatic) 0 else 1 }
     *      }}}.
     *      I.e., the list of operands must contain one value per parameter and – 
     *      in case of instance methods – the receiver object. The list __must not
     *       contain additional values__. The latter is automatically ensured if this
     *      method is called (in)directly by [[AI]] and the operands were just passed
     *      through.
     *      If two or more operands are (reference) identical then the adaptation will only
     *      be performed once and the adapted value will be reused; this ensures that
     *      the relation between values remains stable.
     * @param calledMethod The method that will be evaluated using the given operands.
     * @param targetDomain The [[Domain]] that will be use to perform the abstract
     *      interpretation.
     */
    def mapOperandsToParameters[D <: ValuesDomain](
        operands: Operands[D#DomainValue],
        calledMethod: Method,
        targetDomain: ValuesDomain with ValuesFactory): Locals[targetDomain.DomainValue] = {

        import org.opalj.collection.mutable.Locals
        implicit val domainValueTag = targetDomain.DomainValueTag
        val parameters = Locals[targetDomain.DomainValue](calledMethod.body.get.maxLocals)
        var localVariableIndex = 0
        var index = 0
        val operandsInParameterOrder = operands.reverse
        for (operand ← operandsInParameterOrder) {
            val parameter = {
                // Was the same value (determined by "eq") already adapted?
                var pOperands = operandsInParameterOrder
                var p = 0
                while (p < index && (pOperands.head ne operand)) {
                    p += 1; pOperands = pOperands.tail
                }
                if (p < index)
                    parameters(p)
                else
                    // the value was not previously adapted
                    operand.adapt(targetDomain, -(index + 1))
            }
            parameters.set(localVariableIndex, parameter)
            index += 1
            localVariableIndex += operand.computationalType.operandSize
        }

        parameters
    }

    /**
     * Collects the result of a match of a partial function against an instruction's
     * operands.
     */
    def collectWithOperandsAndIndex[B](
        domain: Domain)(
            code: Code, operandsArray: domain.OperandsArray)(
                f: PartialFunction[(PC, Instruction, domain.Operands), B]): Seq[B] = {
        val instructions = code.instructions
        val max_pc = instructions.size
        var pc = 0
        var result: List[B] = List.empty
        while (pc < max_pc) {
            val instruction = instructions(pc)
            val operands = operandsArray(pc)
            if (operands ne null) {
                val params = (pc, instruction, operands)
                if (f.isDefinedAt(params)) {
                    result = f(params) :: result
                }
            }
            pc = instruction.indexOfNextInstruction(pc, code)
        }
        result.reverse
    }

    //    /**
    //     * Returns those values `V` that are used by the instruction to perform a
    //     * computation that may have an effect outside of the scope of the current method.
    //     * In particular the following computations are considered:
    //     *  - perform tests
    //     *  - passing a value to a method
    //     *  - performing an arithmetic computation
    //     *  - value conversions
    //     *  - assigning the value to a field
    //     *  - using the value as a monitor.
    //     *
    //     * Here, instructions which just move values between the locals and the operands stack
    //     * (laod, store) or just manipulate (dup..., pop, swap) the operand stack are
    //     * ''not'' considered as performing computations related to those values.
    //     *
    //     * @param operands The current operand stack used by the instruction. Note that
    //     *      this methods assumes that all values – independent of their computational
    //     *      type category – just use one operand value. E.g. a long div instruction will
    //     *      only pop the two top most operand values – as in case of the integer div
    //     *      instruction. 
    //     */
    //    def usesForComputation[V >: Null <: AnyRef](
    //    instruction : Instruction,
    //    operands: List[V], 
    //    locals: Locals[V]): List[V]
}
