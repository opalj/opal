/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bugpicker
package core
package analyses

import org.opalj.collection.immutable.Naught
import org.opalj.collection.immutable.Chain
import org.opalj.br.Method
import org.opalj.br.Code
import org.opalj.br.ObjectType
import org.opalj.br.PC
import org.opalj.br.ExceptionHandler
import org.opalj.br.instructions.ATHROW
import org.opalj.br.instructions.ConditionalBranchInstruction
import org.opalj.br.instructions.CompoundConditionalBranchInstruction
import org.opalj.br.instructions.RET
import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.br.instructions.ATHROW
import org.opalj.br.instructions.NEW
import org.opalj.br.instructions.PopInstruction
import org.opalj.br.instructions.GotoInstruction
import org.opalj.br.instructions.ReturnInstruction
import org.opalj.br.analyses.SomeProject
import org.opalj.ai.BaseAI
import org.opalj.ai.Domain
import org.opalj.ai.AIResult
import org.opalj.ai.domain.l0.ZeroDomain
import org.opalj.ai.domain.RecordCFG
import org.opalj.ai.domain.ThrowNoPotentialExceptionsConfiguration
import org.opalj.ai.domain.Origin
import org.opalj.ai.analyses.cg.Callees
import org.opalj.issues.Issue
import org.opalj.issues.InstructionLocation
import org.opalj.issues.IssueOrdering
import org.opalj.issues.IssueCategory
import org.opalj.issues.IssueKind
import org.opalj.issues.Operands
import org.opalj.issues.LocalVariables
import org.opalj.issues.Relevance
import org.opalj.issues.MethodReturnValues
import org.opalj.issues.FieldValues

/**
 * Identifies dead edges in code.
 *
 * @author Michael Eichberg
 */
object DeadEdgesAnalysis {

    def apply(
        theProject: SomeProject,
        method:     Method,
        result:     AIResult { val domain: Domain with Callees with RecordCFG with Origin }
    ): Seq[Issue] = {
        if (method.isSynthetic)
            return Seq.empty;

        import result.domain
        import result.operandsArray
        import result.localsArray
        val evaluatedInstructions = result.evaluatedInstructions
        implicit val body = result.code
        val instructions = body.instructions
        import result.cfJoins
        import result.domain.regularSuccessorsOf
        import result.domain.hasMultiplePredecessors
        import result.domain.isRegularPredecessorOf

        /*
         * Helper function to test if this code will always throw – independent
         * of any data-flow - an exception if executed.
        */
        def isAlwaysExceptionThrowingMethodCall(pc: PC): Boolean = {
            instructions(pc) match {
                case MethodInvocationInstruction(receiver: ObjectType, isInterface, name, descriptor) =>
                    val callees = domain.callees(method, receiver, isInterface, name, descriptor)
                    if (callees.size != 1)
                        return false;

                    // We are only handling the special idiom where the call site is
                    // monomorphic and where the target method is used to create and throw
                    // a special exception object
                    // ...
                    // catch(Exception e) {
                    //     /*return|athrow*/ handleException(e);
                    // }
                    callees.head.body.map(code => !code.exists { (pc, i) =>
                        i.isReturnInstruction
                    }).getOrElse(false)

                case _ =>
                    false
            }
        }

        /*
         * A return/goto/athrow instruction is considered useless if the preceding
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
                nextInstruction.isInstanceOf[ReturnInstruction] || (
                    nextInstruction.isInstanceOf[PopInstruction] &&
                    body.instructions(body.pcOfNextInstruction(nextPC)).isInstanceOf[ReturnInstruction]
                ) || nextInstruction == ATHROW || (
                        nextInstruction.isInstanceOf[GotoInstruction] &&
                        evaluatedInstructions.contains(body.nextNonGotoInstruction(nextPC))
                    )
            ) &&
                        isAlwaysExceptionThrowingMethodCall(currentPC)
        }

        def mostSpecificFinallyHandlerOfPC(pc: PC): Seq[ExceptionHandler] = {
            val candidateHandlers =
                body.exceptionHandlers.filter { eh =>
                    eh.catchType.isEmpty && isRegularPredecessorOf(eh.handlerPC, pc)
                }
            if (candidateHandlers.size > 1) {
                candidateHandlers.tail.foldLeft(List(candidateHandlers.head)) { (c, n) =>
                    var mostSpecificHandlers: List[ExceptionHandler] = List.empty
                    var addN = false
                    c.foreach { c =>
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

        var issues = List.empty[Issue]
        for {
            pc <- body.programCounters.iterator
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

            // Let's check if a path is never taken:
            nextPC <- instruction.nextInstructions(pc, regularSuccessorsOnly = true).toIterator
            if !regularSuccessorsOf(pc).contains(nextPC)

            // If we are in a subroutine, we don't have sufficient information
            // to draw any conclusion; hence, we cannot derive any results.
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
                val correspondingPCs = body.collectWithIndex {
                    case (otherPC, _: ConditionalBranchInstruction) if otherPC != pc &&
                        (operandsArray(otherPC) ne null) &&
                        (operandsArray(otherPC).head eq localsArray(otherPC)(lvIndex)) &&
                        body.haveSameLineNumber(pc, otherPC).getOrElse(true) &&
                        !isRegularPredecessorOf(pc, otherPC) &&
                        !isRegularPredecessorOf(otherPC, pc) &&
                        (finallyHandler intersect mostSpecificFinallyHandlerOfPC(otherPC)).isEmpty =>
                        (otherPC)
                }
                correspondingPCs.nonEmpty
            }

            lazy val isProvenAssertion = {
                instructions(nextPC) match {
                    case NEW(AssertionError) => true
                    case _                   => false
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
                hasMultiplePredecessors(pc + instruction.asInstanceOf[CompoundConditionalBranchInstruction].defaultOffset)

            lazy val isLikelyIntendedDeadDefaultBranch = isDefaultBranchOfSwitch &&
                // this is the default branch of a switch instruction that is dead
                body.alwaysResultsInException(
                    nextPC,
                    cfJoins,
                    (invocationPC) => {
                        isAlwaysExceptionThrowingMethodCall(invocationPC)
                    },
                    (athrowPC) => {
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
                            localsArray(pc) map { l =>
                                if (l ne null)
                                    l.adapt(zDomain, Int.MinValue)
                                else
                                    null
                            }
                        BaseAI.continueInterpretation(
                            result.code,
                            result.cfJoins,
                            result.liveVariables,
                            zDomain
                        )(
                                /*initialWorkList =*/ Chain(nextPC),
                                /*alreadyEvaluated =*/ Naught,
                                /*subroutinesWereEvaluated=*/ false,
                                zOperandsArray,
                                zLocalsArray,
                                Naught, null, null // we don't care about the state of subroutines
                            )
                        val exceptionValue = zOperandsArray(athrowPC).head
                        val throwsError =
                            (
                                zDomain.asReferenceValue(exceptionValue).
                                isValueASubtypeOf(ObjectType.Error).
                                isYesOrUnknown
                            ) ||
                                zDomain.asReferenceValue(exceptionValue).
                                isValueASubtypeOf(ObjectType("java/lang/IllegalStateException")).
                                isYesOrUnknown

                        throwsError
                    }
                )

            val poppedOperandsCount =
                instruction.numberOfPoppedOperands { index =>
                    allOperands(index).computationalType.category
                }
            val operands = allOperands.take(poppedOperandsCount)

            val line = body.lineNumber(nextPC).map(l => s" (line=$l)").getOrElse("")

            val isJustDeadPath = evaluatedInstructions.contains(nextPC)
            val isTechnicalArtifact =
                isLikelyFalsePositive ||
                    isNonExistingDefaultBranchOfSwitch ||
                    isRelatedToCompilationOfFinally
            val localVariables = result.localsArray(pc)
            val relevance =
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

            val summary =
                if (isJustDeadPath)
                    s"dead path found; the direct runtime successor instruction is never immediately executed after this instruction: pc=$nextPC$line"
                else
                    s"dead code found; the successor instruction is dead: pc=$nextPC$line"
            issues ::= Issue(
                "DeadEdgesAnalysis",
                relevance,
                summary,
                Set(IssueCategory.Correctness),
                Set(IssueKind.DeadPath),
                List(new InstructionLocation(
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
                        )
                    ),
                    theProject,
                    method,
                    pc,
                    List(
                        new Operands(body, pc, operands, localVariables),
                        new LocalVariables(body, pc, localVariables)
                    )
                )),
                List(
                    new FieldValues(method, result),
                    new MethodReturnValues(method, result)
                )
            )
        }
        issues.sorted(IssueOrdering)
    }

}
