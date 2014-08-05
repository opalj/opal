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
package frb
package analyses

import org.opalj.util._

import org.opalj.br._
import org.opalj.br.analyses._
import org.opalj.br.instructions._

import org.opalj.ai._
import org.opalj.ai.domain._
import org.opalj.ai.domain.l0._

/**
 * Minimal domain that keeps track of IINC result values, and no others. Values produced
 * by IINC are represented as `IincResults`, all others as `SomeValue`s. `IincResults`
 * are propagated further when used as operand in another instruction or during doJoin().
 *
 * After AI has run, all instructions using the IINC result will have IINC's result value
 * in their operand. All other instructions are known to not use the IINC result.
 *
 * TODO (ideas for future improvement):
 * - Improve to handle array/reference values and field accesses and method invocations.
 *   For now the code to handle these things is imported from OPAL-AI's type-level domain.
 *
 * @author Daniel Klauer
 */
class IincTracingDomain
        extends Domain
        with DefaultDomainValueBinding
        with ThrowAllPotentialExceptionsConfiguration
        with DefaultReferenceValuesBinding
        with TypeLevelFieldAccessInstructions
        with SimpleTypeLevelInvokeInstructions // FIXME We should use the regular TypeLevel...Domain
        with PredefinedClassHierarchy
        with DefaultHandlingOfMethodResults
        with IgnoreSynchronization { thisDomain ⇒

    /**
     * The DomainValue implementation used to represent IINC result values. It tracks
     * the PCs of one or more previous IINC instructions, allowing IINC results to be
     * identified as operands of later instructions.
     */
    case class IincResults(
        val iincPcs: Set[PC],
        override final val computationalType: ComputationalType)
            extends DomainValue {

        /**
         * Called by the AI when joining values from separate code paths. Our goal here
         * is to preserve `IincResults values as well as possible.
         */
        override def doJoin(pc: PC, value: DomainValue): Update[DomainValue] = {
            if (value.isInstanceOf[IincResults]) {
                val other = value.asInstanceOf[IincResults]
                if (this.iincPcs == other.iincPcs) {
                    // They're the same, so following code paths don't need to be updated
                    NoUpdate
                } else {
                    // Combine both lists of IINC results
                    StructuralUpdate(IincResults(this.iincPcs ++ other.iincPcs,
                        computationalType))
                }
            } else {
                // Joining IINC result with an unknown value - we want to trace the IINC
                // result, so preserve it and forget the other.
                StructuralUpdate(this)
            }
        }

        override def summarize(pc: PC): DomainValue = this
    }

    /**
     * Helper method for creating an `IincResults` value from a single PC.
     */
    private def IincResult(pc: PC): IincResults = {
        IincResults(Set(pc), ComputationalTypeInt)
    }

    /**
     * The DomainValue implementation used to represent other values (those that are not
     * IINC results), that are not interesting to this analysis.
     */
    case class SomeValue(override final val computationalType: ComputationalType)
            extends DomainValue {

        /**
         * Called by the AI when joining values from separate code paths. Our goal here
         * is to preserve (pass through) `IincResults` values when they appear; otherwise
         * no update is needed since we don't care about `SomeValue`s.
         */
        override def doJoin(pc: PC, value: DomainValue): Update[DomainValue] = {
            assert(this.computationalType == value.computationalType)

            if (value.isInstanceOf[IincResults]) {
                // Preserve IINC results only
                StructuralUpdate(value)
            } else {
                // We don't care about SomeValues
                NoUpdate
            }
        }

        override def summarize(pc: PC): DomainValue = this
    }

    /**
     * One object of SomeValue for each computational type is good enough for us: because
     * we don't care about SomeValues, we don't need to have one SomeValue instance for
     * every uninteresting value.
     */
    object SomeInteger extends SomeValue(ComputationalTypeInt)
    object SomeLong extends SomeValue(ComputationalTypeLong)
    object SomeFloat extends SomeValue(ComputationalTypeFloat)
    object SomeDouble extends SomeValue(ComputationalTypeDouble)

    /**
     * Helper method that is used to create the result value for the instruction at the
     * given PC. This domain uses this for all instructions except IINC (which always
     * returns an `IincResults` value).
     *
     * If the instruction's source operands contain `IincResults`, the PCs stored in them
     * are preserved in a `IincResults` value returned as the result of this instruction.
     * This way, `IincResults` values are propagated through all instructions that use
     * them, either directly or indirectly.
     *
     * Otherwise, if the source operands simply consist of `SomeValue`s (or if there are
     * none at all), then we return a `SomeValue` as result of this instruction. Since it
     * does not use `IincResults`, we don't care about its result.
     */
    private def MakeResult(
        pc: PC,
        sources: Set[DomainValue],
        computationalType: ComputationalType): DomainValue = {

        val tsources = sources.filter(_.isInstanceOf[IincResults])

        if (tsources.nonEmpty) {
            IincResults(tsources.map(_.asInstanceOf[IincResults].iincPcs).flatten,
                computationalType)
        } else {
            computationalType match {
                case ComputationalTypeInt    ⇒ SomeInteger
                case ComputationalTypeLong   ⇒ SomeLong
                case ComputationalTypeFloat  ⇒ SomeFloat
                case ComputationalTypeDouble ⇒ SomeDouble
                case _                       ⇒ new SomeValue(computationalType)
            }
        }
    }

    /**
     * Helper methods to easily allow each instruction to create the desired result value.
     */
    private def MakeIntegerResult(pc: PC) = MakeResult(pc, Set.empty,
        ComputationalTypeInt)
    private def MakeIntegerResult(pc: PC, l: DomainValue) = MakeResult(pc, Set(l),
        ComputationalTypeInt)
    private def MakeIntegerResult(pc: PC, l: DomainValue, r: DomainValue) =
        MakeResult(pc, Set(l, r), ComputationalTypeInt)
    private def MakeLongResult(pc: PC) = MakeResult(pc, Set.empty, ComputationalTypeLong)
    private def MakeLongResult(pc: PC, l: DomainValue) = MakeResult(pc, Set(l),
        ComputationalTypeLong)
    private def MakeLongResult(pc: PC, l: DomainValue, r: DomainValue) =
        MakeResult(pc, Set(l, r), ComputationalTypeLong)
    private def MakeFloatResult(pc: PC) = MakeResult(pc, Set.empty,
        ComputationalTypeFloat)
    private def MakeFloatResult(pc: PC, l: DomainValue) =
        MakeResult(pc, Set(l), ComputationalTypeFloat)
    private def MakeFloatResult(pc: PC, l: DomainValue, r: DomainValue) =
        MakeResult(pc, Set(l, r), ComputationalTypeFloat)
    private def MakeDoubleResult(pc: PC) = MakeResult(pc, Set.empty,
        ComputationalTypeDouble)
    private def MakeDoubleResult(pc: PC, l: DomainValue) = MakeResult(pc, Set(l),
        ComputationalTypeDouble)
    private def MakeDoubleResult(pc: PC, l: DomainValue, r: DomainValue) =
        MakeResult(pc, Set(l, r), ComputationalTypeDouble)

    override def BooleanValue(pc: PC): DomainValue = MakeIntegerResult(pc)
    override def BooleanValue(pc: PC, value: Boolean): DomainValue = MakeIntegerResult(pc)
    override def ByteValue(pc: PC): DomainValue = MakeIntegerResult(pc)
    override def ByteValue(pc: PC, value: Byte): DomainValue = MakeIntegerResult(pc)
    override def ShortValue(pc: PC): DomainValue = MakeIntegerResult(pc)
    override def ShortValue(pc: PC, value: Short): DomainValue = MakeIntegerResult(pc)
    override def CharValue(pc: PC): DomainValue = MakeIntegerResult(pc)
    override def CharValue(pc: PC, value: Char): DomainValue = MakeIntegerResult(pc)
    override def IntegerValue(pc: PC): DomainValue = MakeIntegerResult(pc)
    override def IntegerValue(pc: PC, value: Int): DomainValue = MakeIntegerResult(pc)
    override def LongValue(pc: PC): DomainValue = MakeLongResult(pc)
    override def LongValue(pc: PC, value: Long): DomainValue = MakeLongResult(pc)
    override def FloatValue(pc: PC): DomainValue = MakeFloatResult(pc)
    override def FloatValue(pc: PC, value: Float): DomainValue = MakeFloatResult(pc)
    override def DoubleValue(pc: PC): DomainValue = MakeDoubleResult(pc)
    override def DoubleValue(pc: PC, value: Double): DomainValue = MakeDoubleResult(pc)

    override def intAreEqual(pc: PC, value1: DomainValue, value2: DomainValue): Answer = Unknown
    override def intIsSomeValueInRange(
        pc: PC,
        value: DomainValue,
        lowerBound: Int, upperBound: Int): Answer = Unknown
    override def intIsSomeValueNotInRange(
        pc: PC,
        value: DomainValue,
        lowerBound: Int, upperBound: Int): Answer = Unknown
    override def intIsLessThan(
        pc: PC,
        smallerValue: DomainValue,
        largerValue: DomainValue): Answer = Unknown
    override def intIsLessThanOrEqualTo(
        pc: PC,
        smallerOrEqualValue: DomainValue,
        equalOrLargerValue: DomainValue): Answer = Unknown

    override def ineg(pc: PC, value: DomainValue) = MakeIntegerResult(pc, value)
    override def iadd(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        MakeIntegerResult(pc, value1, value2)
    override def iand(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        MakeIntegerResult(pc, value1, value2)
    override def idiv(pc: PC, value1: DomainValue,
                      value2: DomainValue): IntegerValueOrArithmeticException =
        ComputedValue(MakeIntegerResult(pc, value1, value2))
    override def imul(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        MakeIntegerResult(pc, value1, value2)
    override def ior(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        MakeIntegerResult(pc, value1, value2)
    override def irem(pc: PC, value1: DomainValue,
                      value2: DomainValue): IntegerValueOrArithmeticException =
        ComputedValue(MakeIntegerResult(pc, value1, value2))
    override def ishl(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        MakeIntegerResult(pc, value1, value2)
    override def ishr(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        MakeIntegerResult(pc, value1, value2)
    override def isub(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        MakeIntegerResult(pc, value1, value2)
    override def iushr(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        MakeIntegerResult(pc, value1, value2)
    override def ixor(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        MakeIntegerResult(pc, value1, value2)
    override def iinc(pc: PC, value: DomainValue, increment: Int) = IincResult(pc)
    override def i2b(pc: PC, value: DomainValue): DomainValue =
        MakeIntegerResult(pc, value)
    override def i2c(pc: PC, value: DomainValue): DomainValue =
        MakeIntegerResult(pc, value)
    override def i2d(pc: PC, value: DomainValue): DomainValue =
        MakeDoubleResult(pc, value)
    override def i2f(pc: PC, value: DomainValue): DomainValue = MakeFloatResult(pc, value)
    override def i2l(pc: PC, value: DomainValue): DomainValue = MakeLongResult(pc, value)
    override def i2s(pc: PC, value: DomainValue): DomainValue =
        MakeIntegerResult(pc, value)

    override def lcmp(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        MakeLongResult(pc, value1, value2)
    override def lneg(pc: PC, value: DomainValue) = MakeLongResult(pc, value)
    override def ladd(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        MakeLongResult(pc, value1, value2)
    override def land(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        MakeLongResult(pc, value1, value2)
    override def ldiv(pc: PC, value1: DomainValue,
                      value2: DomainValue): LongValueOrArithmeticException =
        ComputedValue(MakeLongResult(pc, value1, value2))
    override def lmul(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        MakeLongResult(pc, value1, value2)
    override def lor(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        MakeLongResult(pc, value1, value2)
    override def lrem(pc: PC, value1: DomainValue,
                      value2: DomainValue): LongValueOrArithmeticException =
        ComputedValue(MakeLongResult(pc, value1, value2))
    override def lshl(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        MakeLongResult(pc, value1, value2)
    override def lshr(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        MakeLongResult(pc, value1, value2)
    override def lsub(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        MakeLongResult(pc, value1, value2)
    override def lushr(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        MakeLongResult(pc, value1, value2)
    override def lxor(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        MakeLongResult(pc, value1, value2)
    override def l2d(pc: PC, value: DomainValue): DomainValue =
        MakeDoubleResult(pc, value)
    override def l2f(pc: PC, value: DomainValue): DomainValue = MakeFloatResult(pc, value)
    override def l2i(pc: PC, value: DomainValue): DomainValue =
        MakeIntegerResult(pc, value)

    override def fcmpg(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        MakeIntegerResult(pc, value1, value2)
    override def fcmpl(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        MakeIntegerResult(pc, value1, value2)
    override def fneg(pc: PC, value: DomainValue) = MakeFloatResult(pc, value)
    override def fadd(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        MakeFloatResult(pc, value1, value2)
    override def fdiv(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        MakeFloatResult(pc, value1, value2)
    override def fmul(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        MakeFloatResult(pc, value1, value2)
    override def frem(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        MakeFloatResult(pc, value1, value2)
    override def fsub(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        MakeFloatResult(pc, value1, value2)
    override def f2d(pc: PC, value: DomainValue): DomainValue =
        MakeDoubleResult(pc, value)
    override def f2i(pc: PC, value: DomainValue): DomainValue =
        MakeIntegerResult(pc, value)
    override def f2l(pc: PC, value: DomainValue): DomainValue = MakeLongResult(pc, value)

    override def dcmpg(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        MakeIntegerResult(pc, value1, value2)
    override def dcmpl(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        MakeIntegerResult(pc, value1, value2)
    override def dneg(pc: PC, value: DomainValue) = MakeDoubleResult(pc, value)
    override def dadd(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        MakeDoubleResult(pc, value1, value2)
    override def ddiv(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        MakeDoubleResult(pc, value1, value2)
    override def dmul(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        MakeDoubleResult(pc, value1, value2)
    override def drem(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        MakeDoubleResult(pc, value1, value2)
    override def dsub(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        MakeDoubleResult(pc, value1, value2)
    override def d2f(pc: PC, value: DomainValue): DomainValue = MakeFloatResult(pc, value)
    override def d2i(pc: PC, value: DomainValue): DomainValue =
        MakeIntegerResult(pc, value)
    override def d2l(pc: PC, value: DomainValue): DomainValue = MakeLongResult(pc, value)
}

/**
 * This analysis looks for useless increment operations used in `return` statements,
 * for example:
 *
 * {{{
 * int f(int i) {
 * 	return i++;
 * }
 * }}}
 *
 * The basic solution is to look for an IINC instruction followed by an IRETURN
 * instruction, and furthermore, whether the IINC's operand is also the value that will
 * ultimately be used by the IRETURN.
 *
 * Afterall, this analysis should not report code like this:
 *
 * {{{
 * int f(int i) {
 * 	i++;
 * 	return i;
 * }
 * }}}
 *
 * In such cases the IINC's operand is a different value than the one used by the IRETURN,
 * because there are intermediate load/store operations.
 *
 * Currently this analysis uses AI and a custom Domain to trace the operand values.
 *
 * Besides IINC and IRETURN, there can be other instructions used, if the variable type or
 * function result type are different. For example:
 *
 * {{{
 * float f(int i) {
 * 	return i++;
 * }
 * }}}
 *
 * In this case, there will be IINC, I2F, and FRETURN instructions, and we need to detect
 * that a certain value is incremented, passed through the I2F, to the FRETURN. This can
 * no longer be done using AI's existing l0.TypeLevelDomain or l1.DefaultDomain (precise
 * values) domains, because they will create new DomainValue instances if a value changes,
 * and provide no easy way to find the previous value(s) from which the new one was
 * produced.
 *
 * TODO (ideas for future improvement):
 * - Could easily be extended to detect useless decrement too
 * - Should also detect useless increments of longs. There is no LINC (increment for
 *   longs) instruction though, javac produces LCONST_1,LADD,LSTORE_1 for long++. Can we
 *   detect this nonetheless? The LSTORE_1 should appear behind the *RETURN or else it
 *   wouldn't be dead code.
 *
 * @author Daniel Klauer
 */
class UselessIncrementInReturn[Source] extends FindRealBugsAnalysis[Source] {

    override def description: String = "Reports useless increments after a return statement."

    /**
     * Runs this analysis on the given project.
     *
     * @param project The project to analyze.
     * @param parameters Options for the analysis. Currently unused.
     * @return A list of reports, or an empty list.
     */
    def analyze(
        project: Project[Source],
        parameters: Seq[String] = List.empty): Iterable[LineAndColumnBasedReport[Source]] = {

        var reports: List[LineAndColumnBasedReport[Source]] = List.empty

        for {
            classFile ← project.classFiles
            if !project.isLibraryType(classFile)
            method @ MethodWithBody(body) ← classFile.methods
        } {
            val domain = new IincTracingDomain
            val result = BaseAI(classFile, method, domain)
            val code = body.associateWithIndex

            /**
             * Check whether the result of the IINC instruction (given as PC) is used
             * as operands to any other instructions.
             */
            def incrementResultIsUsed(iincPc: PC): Boolean = {
                for {
                    operands ← result.operandsArray
                    if operands != null
                    operand ← operands
                    if operand.isInstanceOf[domain.IincResults]
                    if operand.asInstanceOf[domain.IincResults].iincPcs.contains(iincPc)
                } {
                    return true
                }
                false
            }

            for { (iincPc, IINC(_, _)) ← code if !incrementResultIsUsed(iincPc) } {
                reports =
                    LineAndColumnBasedReport(
                        project.source(classFile.thisType),
                        Severity.Info,
                        classFile.thisType,
                        method.descriptor,
                        method.name,
                        body.lineNumber(iincPc),
                        None,
                        "Increment during return statement is dead code"
                    ) :: reports
            }
        }

        reports
    }
}
