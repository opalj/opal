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
package bugpicker
package core
package analysis

import scala.language.existentials
import java.net.URL
import scala.xml.Node
import scala.xml.UnprefixedAttribute
import scala.xml.Unparsed
import scala.Console.BLUE
import scala.Console.RED
import scala.Console.BOLD
import scala.Console.GREEN
import scala.Console.RESET
import scala.collection.SortedMap
import org.opalj.br.analyses.{ Analysis, AnalysisExecutor, BasicReport, Project, SomeProject }
import org.opalj.br.analyses.ProgressManagement
import org.opalj.br.{ ClassFile, Method }
import org.opalj.br.MethodWithBody
import org.opalj.ai.common.XHTML
import org.opalj.ai.BaseAI
import org.opalj.ai.Domain
import org.opalj.br.Code
import org.opalj.ai.collectPCWithOperands
import org.opalj.ai.BoundedInterruptableAI
import org.opalj.ai.domain
import org.opalj.ai.domain.l0.ZeroDomain
import org.opalj.br.AnalysisFailedException
import org.opalj.br.ObjectType
import org.opalj.br.ComputationalTypeInt
import org.opalj.br.ComputationalTypeLong
import org.opalj.br.instructions.ATHROW
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.ConditionalBranchInstruction
import org.opalj.br.instructions.SimpleConditionalBranchInstruction
import org.opalj.br.instructions.CompoundConditionalBranchInstruction
import org.opalj.br.instructions.ArithmeticInstruction
import org.opalj.br.instructions.BinaryArithmeticInstruction
import org.opalj.br.instructions.UnaryArithmeticInstruction
import org.opalj.br.instructions.LNEG
import org.opalj.br.instructions.INEG
import org.opalj.br.instructions.IINC
import org.opalj.br.instructions.RET
import org.opalj.br.instructions.ShiftInstruction
import org.opalj.br.instructions.INSTANCEOF
import org.opalj.br.instructions.ISTORE
import org.opalj.br.instructions.IFNULL
import org.opalj.br.instructions.IFNONNULL
import org.opalj.br.instructions.IStoreInstruction
import org.opalj.br.instructions.MethodCompletionInstruction
import org.opalj.ai.AIResult
import org.opalj.ai.InterpretationFailedException
import org.opalj.ai.domain.ConcreteIntegerValues
import org.opalj.ai.domain.ConcreteLongValues
import org.opalj.ai.domain.RecordCFG
import org.opalj.ai.domain.l1.RecordAllThrownExceptions
import org.opalj.ai.domain.l1.ReferenceValues
import org.opalj.br.instructions.FieldReadAccess
import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.br.instructions.GOTO
import org.opalj.br.instructions.ATHROW
import org.opalj.br.instructions.GOTO_W
import org.opalj.br.instructions.ReturnInstruction
import org.opalj.br.PC
import org.opalj.ai.analyses.MethodReturnValuesKey
import org.opalj.ai.analyses.cg.Callees
import org.opalj.br.instructions.GotoInstruction
import org.opalj.br.instructions.ReturnInstructions
import org.opalj.ai.IsAReferenceValue
import org.opalj.ai.domain.ThrowNoPotentialExceptionsConfiguration
import org.opalj.br.instructions.NEW
import org.opalj.br.cfg.ControlFlowGraph
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.ai.domain.Origin
import org.opalj.br.ExceptionHandler

/**
 * Identifies dead edges in code.
 *
 * @author Michael Eichberg
 */
object DeadPathAnalysis {

    final val AssertionError = ObjectType("java/lang/AssertionError")

    def analyze(
        theProject: SomeProject, classFile: ClassFile, method: Method,
        result: AIResult { val domain: Domain with Callees with RecordCFG with Origin }): Seq[StandardIssue] = {
        if (method.isSynthetic)
            return Seq.empty;

        import result.domain
        import result.operandsArray
        import result.localsArray
        val evaluatedInstructions = result.evaluatedInstructions
        implicit val body = result.code
        val instructions = body.instructions
        import result.joinInstructions
        import result.domain.regularSuccessorsOf
        import result.domain.exceptionHandlerSuccessorsOf
        import result.domain.hasMultipleRegularPredecessors
        import result.domain.isRegularPredecessorOf
        import result.domain.hasRegularSuccessor

        /*
         * Helper function to test if this code will always throw – independent
         * of any data-flow - an exception if executed.
        */
        def isAlwaysExceptionThrowingMethodCall(pc: PC): Boolean = {
            instructions(pc) match {
                case MethodInvocationInstruction(receiver: ObjectType, name, descriptor) ⇒
                    val callees = domain.callees(receiver, name, descriptor)
                    if (callees.size != 1)
                        return false;

                    // We are only handling the special idiom where the call site is
                    // monomorphic and where the target method is used to create and throw
                    // a special exception object
                    // ...
                    // catch(Exception e) {
                    //     /*return|athrow*/ handleException(e);
                    // }
                    callees.head.body.map(code ⇒ !code.exists { (pc, i) ⇒
                        ReturnInstruction.isReturnInstruction(i)
                    }).getOrElse(false)

                case _ ⇒
                    false
            }
        }

        /*
         *  A return/goto/athrow instruction is considered useless if the preceding
         * instruction is a method call with a single target that _context-independently_
         * always just throws (an) exception(s). This is common pattern found in the JDK.
         */
        // We want to suppress false warning as found in JDK 1.8.0_25
        //     javax.xml.bind.util.JAXBResult
        // when the method
        //     assertFailed
        // is called and afterwards a jump is done to a non-dead instruction.
        def requiredUselessJumpOrReturnFromMethod(currentPC: PC, nextPC: PC): Boolean = {
            val nextInstruction = body.instructions(nextPC)
            (
                nextInstruction.isInstanceOf[ReturnInstruction] ||
                nextInstruction == ATHROW ||
                (
                    nextInstruction.isInstanceOf[GotoInstruction] &&
                    evaluatedInstructions.contains(body.nextNonGotoInstruction(nextPC))
                )
            ) &&
                    isAlwaysExceptionThrowingMethodCall(currentPC)
        }

        def mostSpecificFinallyHandlerOfPC(pc: PC): Seq[ExceptionHandler] = {
            val candidateHandlers =
                body.exceptionHandlers.filter { eh ⇒
                    eh.catchType.isEmpty && isRegularPredecessorOf(eh.handlerPC, pc)
                }
            if (candidateHandlers.size > 1) {
                candidateHandlers.tail.foldLeft(List(candidateHandlers.head)) { (c, n) ⇒
                    var mostSpecificHandlers: List[ExceptionHandler] = List.empty
                    var addN = false
                    c.foreach { c ⇒
                        if (isRegularPredecessorOf(c.handlerPC, n.handlerPC)) {
                            addN = true
                        } else if (isRegularPredecessorOf(n.handlerPC, c.handlerPC)) {
                            mostSpecificHandlers = c :: mostSpecificHandlers
                        } else {
                            mostSpecificHandlers = c :: mostSpecificHandlers
                            addN = true
                        }
                    }
                    if (addN) {
                        mostSpecificHandlers = n :: mostSpecificHandlers
                    }
                    mostSpecificHandlers
                }
            } else
                candidateHandlers
        }

        var issues = List.empty[StandardIssue]
        for {
            pc ← body.programCounters
            if evaluatedInstructions.contains(pc)
            instruction = instructions(pc)
            opcode = instruction.opcode
            // Let's filter those instructions for which we cannot statically determine
            // the set of meaningful successors:
            // (I) In case of RET we don't know the next one, but the next one
            // is not dead... (ever) if we reach the RET instruction
            if opcode != RET.opcode
            // (II) We don't have an analysis in place that enables us to determine
            // the meaningful set of successors.
            if opcode != ATHROW.opcode

            // Let's check if a path is not taken:
            (nextPC: PC) ← instruction.nextInstructions(pc, body, regularSuccessorsOnly = true)
            if !regularSuccessorsOf(pc).contains(nextPC)

            // If we are in a subroutine, we don't have sufficient information
            // to draw any conclusion; hence,
            allOperands = operandsArray(pc)
            if allOperands ne null // null if we are in a subroutine (java < 1.5)
        } {
            // identify those dead edges that are pure technical artifacts
            val isLikelyFalsePositive = requiredUselessJumpOrReturnFromMethod(pc, nextPC)

            def isRelatedToCompilationOfFinally: Boolean = {
                // There has to be at least one finally statement.
                if (!body.exceptionHandlers.exists(_.catchType.isEmpty))
                    return false;

                // All issues related to the compilation of finally manifest
                // themselves in dead edges related to conditional branch instructions
                if (!instruction.isInstanceOf[ConditionalBranchInstruction])
                    return false;

                // Let's determine the index of the local variable that is evaluated
                val lvIndexOption = localsArray(pc).indexOf(allOperands.head)
                if (lvIndexOption.isEmpty)
                    return false;

                // Let's find all other if instructions that also access the same
                // local variable and which are not in a predecessor /successor relation
                val finallyHandler = mostSpecificFinallyHandlerOfPC(pc)
                val lvIndex = lvIndexOption.get
                //                print(method.toJava(classFile) + s"$pc($lvIndex)(eh=$finallyHandler)")
                val correspondingPCs = body.collectWithIndex {
                    case (otherPC, cbi: ConditionalBranchInstruction) if otherPC != pc &&
                        (operandsArray(otherPC) ne null) &&
                        (operandsArray(otherPC).head eq localsArray(otherPC)(lvIndex)) &&
                        body.haveSameLineNumber(pc, otherPC).getOrElse(true) &&
                        !isRegularPredecessorOf(pc, otherPC) &&
                        !isRegularPredecessorOf(otherPC, pc) &&
                        (finallyHandler intersect mostSpecificFinallyHandlerOfPC(otherPC)).isEmpty ⇒
                        (otherPC)
                }
                correspondingPCs.nonEmpty
            }

            lazy val isProvenAssertion = {
                instructions(nextPC) match {
                    case NEW(AssertionError) ⇒ true
                    case _                   ⇒ false
                }
            }

            // Identify those dead edges that are the result of common programming
            // idioms; e.g.,
            // switch(v){
            // ...
            // default:
            //   1: throw new XYZError(...);
            //   2: throw new IllegalStateException(...);
            //   3: assert(false);
            //   4: stateError();
            //         AN ALWAYS (PRIVATE AND/OR STATIC) EXCEPTION
            //         THROWING METHOD
            //         HERE, THE DEFAULT CASE MAY EVEN FALL THROUGH!
            // }
            //
            lazy val isDefaultBranchOfSwitch =
                instruction.isInstanceOf[CompoundConditionalBranchInstruction] &&
                    nextPC == pc + instruction.asInstanceOf[CompoundConditionalBranchInstruction].defaultOffset

            lazy val isNonExistingDefaultBranchOfSwitch = isDefaultBranchOfSwitch &&
                hasMultipleRegularPredecessors(pc + instruction.asInstanceOf[CompoundConditionalBranchInstruction].defaultOffset)

            lazy val isLikelyIntendedDeadDefaultBranch = isDefaultBranchOfSwitch &&
                // this is the default branch of a switch instruction that is dead
                body.alwaysResultsInException(
                    nextPC,
                    joinInstructions,
                    (invocationPC) ⇒ {
                        isAlwaysExceptionThrowingMethodCall(invocationPC)
                    },
                    (athrowPC) ⇒ {
                        // Let's do a basic analysis to determine the type of
                        // the thrown exception.
                        // What we do next is basic a local data-flow analysis that
                        // starts with the first instruction of the default branch
                        // of the switch instruction and which uses
                        // the most basic domain available.
                        val codeLength = body.instructions.length
                        class ZDomain extends { // we need the "early initializer
                            val project: SomeProject = theProject
                            val code: Code = body
                        } with ZeroDomain with ThrowNoPotentialExceptionsConfiguration
                        val zDomain = new ZDomain
                        val zOperandsArray = new zDomain.OperandsArray(codeLength)
                        val zInitialOperands =
                            operandsArray(pc).tail.map(_.adapt(zDomain, Int.MinValue))
                        zOperandsArray(nextPC) = zInitialOperands
                        val zLocalsArray = new zDomain.LocalsArray(codeLength)
                        zLocalsArray(nextPC) =
                            localsArray(pc) map { l ⇒
                                if (l ne null)
                                    l.adapt(zDomain, Int.MinValue)
                                else
                                    null
                            }
                        BaseAI.continueInterpretation(
                            result.strictfp,
                            result.code,
                            result.joinInstructions,
                            zDomain)(
                                /*initialWorkList =*/ List(nextPC),
                                /*alreadyEvaluated =*/ List(),
                                zOperandsArray,
                                zLocalsArray,
                                List.empty
                            )
                        val exceptionValue = zOperandsArray(athrowPC).head
                        val throwsError =
                            (
                                zDomain.asReferenceValue(exceptionValue).
                                isValueSubtypeOf(ObjectType.Error).
                                isYesOrUnknown
                            ) ||
                                zDomain.asReferenceValue(exceptionValue).
                                isValueSubtypeOf(ObjectType("java/lang/IllegalStateException")).
                                isYesOrUnknown

                        throwsError
                    }
                )

            val poppedOperandsCount =
                instruction.numberOfPoppedOperands { index ⇒
                    allOperands(index).computationalType.computationalTypeCategory
                }
            val operands = allOperands.take(poppedOperandsCount)

            val line = body.lineNumber(nextPC).map(l ⇒ s" (line=$l)").getOrElse("")

            val hints =
                body.collectWithIndex {
                    case (pc, instr @ FieldReadAccess(_ /*declaringClassType*/ , _ /* name*/ , fieldType)) if {
                        val nextPC = instr.indexOfNextInstruction(pc)
                        val operands = operandsArray(nextPC)
                        operands != null &&
                            operands.head.isMorePreciseThan(result.domain.TypedValue(pc, fieldType))
                    } ⇒
                        (pc, s"${operandsArray(instr.indexOfNextInstruction(pc)).head} ← $instr")

                    case (pc, instr @ MethodInvocationInstruction(declaringClassType, name, descriptor)) if !descriptor.returnType.isVoidType && {
                        val nextPC = instr.indexOfNextInstruction(pc, body)
                        val operands = operandsArray(nextPC)
                        operands != null &&
                            operands.head.isMorePreciseThan(result.domain.TypedValue(pc, descriptor.returnType))
                    } ⇒
                        val modifier = if (instr.isInstanceOf[INVOKESTATIC]) "static " else ""
                        (
                            pc,
                            s"${operandsArray(instr.indexOfNextInstruction(pc, body)).head} ← ${declaringClassType.toJava}{ $modifier ${descriptor.toJava(name)} }"
                        )
                }

            val isJustDeadPath = evaluatedInstructions.contains(nextPC)
            val isTechnicalArtifact =
                isLikelyFalsePositive ||
                    isNonExistingDefaultBranchOfSwitch ||
                    isRelatedToCompilationOfFinally
            issues ::= StandardIssue(
                theProject, classFile, Some(method), Some(pc),
                Some(operands), Some(result.localsArray(pc)),
                if (isJustDeadPath)
                    s"[dead path] the direct runtime successor instruction is never immediately executed after this instruction: pc=$nextPC$line"
                else
                    s"[dead code] the successor instruction is dead: pc=$nextPC$line",
                Some(
                    "The evaluation of the instruction never leads to the evaluation of the specified instruction."+(
                        if (isTechnicalArtifact)
                            "\n(This seems to be a technical artifact that cannot be avoided; i.e., there is nothing to fix.)"
                        else if (isProvenAssertion)
                            "\n(We seem to have proven that an assertion always holds (unless an exeption is throw).)"
                        else if (isLikelyIntendedDeadDefaultBranch)
                            "\n(This seems to be a deliberately dead default branch of a switch instruction; i.e., there is probably nothing to fix!)"
                        else
                            ""
                    )),
                Set(IssueCategory.Flawed, IssueCategory.Comprehensibility),
                Set(IssueKind.DeadPath),
                hints,
                if (isTechnicalArtifact)
                    Relevance.TechnicalArtifact
                else if (isProvenAssertion)
                    Relevance.ProvenAssertion
                else if (isLikelyIntendedDeadDefaultBranch)
                    Relevance.CommonIdiom
                else if (isJustDeadPath)
                    Relevance.UselessDefensiveProgramming
                else
                    Relevance.OfUtmostRelevance
            )
        }
        issues.sortBy { _.line }
    }

}
