/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bugpicker
package core
package analyses

import scala.util.control.ControlThrowable
import org.opalj.log.OPALLogger

import org.opalj.br.analyses.SomeProject
import org.opalj.br.Method
import org.opalj.br.instructions.GETSTATIC
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.br.instructions.ACONST_NULL
import org.opalj.br.instructions.ICONST_0
import org.opalj.br.instructions.DCONST_0
import org.opalj.br.instructions.LCONST_0
import org.opalj.br.instructions.FCONST_0
import org.opalj.br.instructions.StoreLocalVariableInstruction
import org.opalj.br.instructions.ICONST_M1
import org.opalj.br.instructions.IINC
import org.opalj.br.instructions.BIPUSH
import org.opalj.br.instructions.SIPUSH
import org.opalj.br.instructions.LDC
import org.opalj.br.instructions.ICONST_1
import org.opalj.br.instructions.ICONST_3
import org.opalj.br.instructions.ICONST_4
import org.opalj.br.instructions.ICONST_5
import org.opalj.br.instructions.LCONST_1
import org.opalj.br.instructions.DCONST_1
import org.opalj.br.instructions.FCONST_1
import org.opalj.br.instructions.LDC_W
import org.opalj.br.instructions.LDC2_W
import org.opalj.br.instructions.ICONST_2
import org.opalj.br.instructions.FCONST_2
import org.opalj.br.instructions.LoadConstantInstruction
import org.opalj.issues.Issue
import org.opalj.issues.Relevance
import org.opalj.issues.IssueCategory
import org.opalj.issues.IssueKind
import org.opalj.issues.InstructionLocation
import org.opalj.issues.MethodLocation
import org.opalj.ai.domain.RecordDefUse
import org.opalj.ai.AIResult
import org.opalj.ai.Domain
import org.opalj.ai.domain.TheCode
import org.opalj.ai.analyses.cg.CallGraph
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.EP
import org.opalj.fpcf.properties.LBPure
import org.opalj.fpcf.properties.Purity
import org.opalj.ai.analyses.cg.CallGraphFactory

/**
 * Identifies unused local variables in non-synthetic methods.
 *
 * @author Michael Eichberg
 */
object UnusedLocalVariables {

    def apply(
        theProject:    SomeProject,
        propertyStore: PropertyStore,
        callGraph:     CallGraph,
        method:        Method,
        result:        AIResult { val domain: Domain with TheCode with RecordDefUse }
    ): Seq[Issue] = {

        if (method.isSynthetic)
            return Nil;

        //
        //
        // IDENTIFYING RAW ISSUES; IN OTHER WORDS: "THE ANALYSIS"
        //
        //

        val operandsArray = result.operandsArray
        val allUnused = result.domain.unused()
        val unused = allUnused.withFilter { vo =>
            // IMPROVE Improve the identification of unused method parameters... check if future overridings are possible; if it already overrides a method where the parameter is used...
            (vo < 0 && (
                (
                    method.isPrivate &&
                    !CallGraphFactory.isPotentiallySerializationRelated(method)(theProject.classHierarchy)
                )
            ) ||
                    method.isStatic) ||
                    // filter unused local variables related to dead code...
                    // (we have another analysis for that)
                    (vo >= 0 && (operandsArray(vo) ne null))
        }

        if (unused.isEmpty)
            return Nil;

        //
        //
        // POST PROCESSING ISSUES TO FILTER LIKELY FALSE POSITIVES
        //
        //

        val code = result.domain.code
        val instructions = code.instructions
        val implicitParameterOffset = if (!method.isStatic) 1 else 0

        var issues = List.empty[Issue]

        // It may happen that a user defines a final local constant
        // which is then used by the compiler whenever we have a
        // reference in the code; in this case we actually have an unused
        // local variable in the bytecode...
        // E.g., given the following code:
        //     final int MAGIC = 12012;
        //     ...
        //     if (x == MAGIC) {...}
        // then the value 12012 is then directly used in the comparison
        // which makes the initial declaration (at the bytecode level)
        // unused.
        lazy val constantValues: Set[Any] = {
            import code.collectInstructions
            val allConstantsValues = collectInstructions { case LoadConstantInstruction(v) => v }
            val constantValuesOnlyUsedOnces = allConstantsValues.groupBy(v => v).collect {
                case (k, occurences) if occurences.tail.isEmpty => k
            }
            constantValuesOnlyUsedOnces.toSet
        }

        unused foreach { vo =>
            var issue: String = null
            var relevance: Relevance = Relevance.Undetermined
            if (vo < 0) {
                // we have to make sure that we do not create an issue report
                // for instance methods that can be/are inherited
                if (method.isStatic ||
                    method.isPrivate ||
                    // IMPROVE Check that in all other cases the method parameter is never used across all implementations of the method; only then report it.
                    method.name == "<init>") {
                    relevance = Relevance.High
                    if (vo == -1 && !method.isStatic) {
                        issue = "the self reference \"this\" is unused (the method could be static)"
                    } else {
                        val index = (-(vo + implicitParameterOffset))
                        code.localVariable(0, index - 1) match {
                            case Some(lv) => issue = s"the parameter ${lv.name} is unused"
                            case None     => issue = s"the $index. parameter is unused"
                        }
                    }
                }
            } else {
                val instruction = instructions(vo)

                def defaultUnusedValueHandling(): Unit = {
                    val instructionDescription = instruction.toString(vo).replace("\n", "\\n")
                    issue = "the value of "+instructionDescription+" is not used"
                    relevance = Relevance.VeryHigh
                }

                instruction.opcode match {

                    case INVOKEVIRTUAL.opcode | INVOKEINTERFACE.opcode |
                        INVOKESTATIC.opcode | INVOKESPECIAL.opcode =>
                        val invoke = instruction.asInstanceOf[MethodInvocationInstruction]
                        try {
                            val resolvedMethod: Iterable[Method] = callGraph.calls(method, vo)
                            // IMPROVE Use a more precise method to determine if a method has a side effect "pureness" is actually too strong.
                            if (resolvedMethod.exists(m => propertyStore(m, Purity.key) == EP(m, LBPure))) {
                                issue = "the return value of the call of "+invoke.declaringClass.toJava+
                                    "{ "+
                                    invoke.methodDescriptor.toJava(invoke.name)+
                                    " } is not used"
                                relevance = Relevance.OfUtmostRelevance
                            }
                        } catch {
                            case ct: ControlThrowable => throw ct
                            case t: Throwable =>
                                val message = "assessing analysis result failed; ignoring issue"
                                OPALLogger.error("error", message, t)(theProject.logContext)
                        }

                    case ACONST_NULL.opcode |
                        ICONST_0.opcode |
                        ICONST_M1.opcode |
                        LCONST_0.opcode |
                        FCONST_0.opcode |
                        DCONST_0.opcode =>
                        val nextPC = code.pcOfNextInstruction(vo)
                        instructions(nextPC) match {
                            case StoreLocalVariableInstruction((_, index)) =>
                                // The typical pattern generated by a compiler if the
                                // value is used to set "some initial value" is that
                                // after pushing the constant value on the stack, the
                                // value is immediately stored in a register...
                                //
                                // final int i = 0
                                // if (x == ...) i = j*1; else i = abc();
                                //

                                val lvOption = code.localVariable(nextPC, index)
                                if (lvOption.isDefined && lvOption.get.startPC < vo) {
                                    issue = s"the constant value ${instruction.toString(vo)} is not used"
                                    relevance = Relevance.Low
                                }
                            // else... we filter basically all issues unless we are sure that
                            // this is a real issue; i.e.,
                            //  - it is not a default value
                            //  - it is not a final local variable

                            case _ =>
                                issue = "the constant value "+
                                    instruction.toString(vo)+
                                    "is (most likely) used to initialize a local variable"
                                relevance = Relevance.TechnicalArtifact
                        }

                    case BIPUSH.opcode | SIPUSH.opcode |
                        ICONST_1.opcode | ICONST_2.opcode | ICONST_3.opcode | ICONST_4.opcode | ICONST_5.opcode |
                        LCONST_1.opcode |
                        DCONST_1.opcode |
                        FCONST_1.opcode | FCONST_2.opcode |
                        LDC.opcode | LDC_W.opcode | LDC2_W.opcode =>
                        val LoadConstantInstruction(value) = instruction
                        if (constantValues.contains(value)) {
                            // => the value is only found once in the source code and
                            // the value is not used!
                            issue = "the constant "+
                                instruction.toString(vo).replace("\n", "\\n")+
                                " is not used"
                            relevance = Relevance.TechnicalArtifact
                        }

                    case IINC.opcode =>
                        issue = "the incremented value is not used"
                        relevance = Relevance.DefaultRelevance

                    case GETSTATIC.opcode =>
                        val GETSTATIC(_, _, fieldType) = instruction
                        if (fieldType.isObjectType) {
                            val instr = instruction.toString(vo)

                            theProject.classFile(fieldType.asObjectType) match {
                                case Some(cf) =>
                                    if (cf.isEnumDeclaration) {
                                        issue = s"the enum value $instr"+
                                            "is (most likely) used to initialize a local variable"
                                        relevance = Relevance.TechnicalArtifact
                                    } else {
                                        defaultUnusedValueHandling()
                                    }
                                case None =>
                                    // we were not able to find the class
                                    issue = s"the field value $instr is not used"
                                    relevance = Relevance.DefaultRelevance
                            }
                        } else
                            defaultUnusedValueHandling()

                    case _ =>
                        defaultUnusedValueHandling()
                }

            }

            if (issue ne null) {
                issues ::= Issue(
                    "UnusedLocalVariables",
                    relevance,
                    issue,
                    Set(IssueCategory.Comprehensibility, IssueCategory.Correctness),
                    Set(IssueKind.UnusedLocalVariable),
                    List(
                        if (vo >= 0)
                            new InstructionLocation(None, theProject, method, vo)
                        else
                            new MethodLocation(None, theProject, method)
                    )
                )
            }
        }

        issues

    }

}
