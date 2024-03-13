/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l0
package interpretation

import org.opalj.br.ComputationalTypeDouble
import org.opalj.br.ComputationalTypeFloat
import org.opalj.br.ComputationalTypeInt
import org.opalj.br.DoubleType
import org.opalj.br.FloatType
import org.opalj.br.ObjectType
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel
import org.opalj.br.fpcf.properties.string_definition.StringConstancyType
import org.opalj.br.fpcf.properties.string_definition.StringTreeConcat
import org.opalj.br.fpcf.properties.string_definition.StringTreeConst
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimEP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.SomeFinalEP
import org.opalj.fpcf.UBP
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler
import org.opalj.value.TheIntegerValue

/**
 * Responsible for processing [[VirtualFunctionCall]]s without a call graph.
 *
 * @author Maximilian Rüsch
 */
case class L0VirtualFunctionCallInterpreter[State <: L0ComputationState](
    override val ps: PropertyStore
) extends L0StringInterpreter[State]
    with L0ArbitraryVirtualFunctionCallInterpreter[State]
    with L0AppendCallInterpreter[State]
    with L0SubstringCallInterpreter[State] {

    override type T = VirtualFunctionCall[V]

    /**
     * Currently, this implementation supports the interpretation of the following function calls:
     * <ul>
     * <li>`append`: Calls to the `append` function of [[StringBuilder]] and [[StringBuffer]].</li>
     * <li>
     * `toString`: Calls to the `toString` function of [[StringBuilder]] and [[StringBuffer]]. As a `toString` call does
     * not change the state of such an object, an empty list will be returned.
     * </li>
     * <li>
     * `replace`: Calls to the `replace` function of [[StringBuilder]] and [[StringBuffer]]. For further information how
     * this operation is processed, see [[interpretReplaceCall]].
     * </li>
     * <li>
     * Apart from these supported methods, a [[StringConstancyInformation.lb]] will be returned in case the passed
     * method returns a [[java.lang.String]].
     * </li>
     * </ul>
     *
     * If none of the above-described cases match, a [[NoResult]] will be returned.
     */
    override def interpret(instr: T, pc: Int)(implicit state: State): ProperPropertyComputationResult = {
        instr.name match {
            case "append"   => interpretAppendCall(instr, pc)
            case "toString" => interpretToStringCall(instr, pc)
            case "replace"  => interpretReplaceCall(pc)
            case "substring" if instr.descriptor.returnType == ObjectType.String =>
                interpretSubstringCall(instr, pc)
            case _ =>
                instr.descriptor.returnType match {
                    case obj: ObjectType if obj == ObjectType.String =>
                        interpretArbitraryCall(instr, pc)
                    case FloatType | DoubleType =>
                        computeFinalResult(pc, StringConstancyInformation.dynamicFloat)
                    case _ =>
                        computeFinalResult(pc, StringConstancyInformation.neutralElement)
                }
        }
    }

    /**
     * Processes calls to [[StringBuilder#toString]] or [[StringBuffer#toString]]. Note that this function assumes that
     * the given `toString` is such a function call! Otherwise, the expected behavior cannot be guaranteed.
     */
    private def interpretToStringCall(call: T, pc: Int)(implicit state: State): ProperPropertyComputationResult = {
        def computeResult(eps: SomeEOptionP): ProperPropertyComputationResult = {
            eps match {
                case FinalP(sciP: StringConstancyProperty) =>
                    computeFinalResult(pc, sciP.sci)

                case iep: InterimEP[_, _] if eps.pk == StringConstancyProperty.key =>
                    InterimResult.forLB(
                        InterpretationHandler.getEntityFromDefSitePC(pc),
                        iep.lb.asInstanceOf[StringConstancyProperty],
                        Set(eps),
                        computeResult
                    )

                case _ if eps.pk == StringConstancyProperty.key =>
                    InterimResult.forLB(
                        InterpretationHandler.getEntityFromDefSitePC(pc),
                        StringConstancyProperty.lb,
                        Set(eps),
                        computeResult
                    )

                case _ => throw new IllegalArgumentException(s"Encountered unknown eps: $eps")
            }
        }

        computeResult(ps(
            InterpretationHandler.getEntityFromDefSite(call.receiver.asVar.definedBy.head),
            StringConstancyProperty.key
        ))
    }

    /**
     * Processes calls to [[StringBuilder#replace]] or [[StringBuffer#replace]].
     */
    private def interpretReplaceCall(pc: Int)(implicit state: State): ProperPropertyComputationResult =
        computeFinalResult(pc, InterpretationHandler.getStringConstancyInformationForReplace)
}

private[string_analysis] trait L0ArbitraryVirtualFunctionCallInterpreter[State <: L0ComputationState]
    extends L0StringInterpreter[State] {

    protected def interpretArbitraryCall(call: T, pc: Int)(implicit state: State): ProperPropertyComputationResult =
        computeFinalResult(pc, StringConstancyInformation.lb)
}

/**
 * Interprets calls to [[StringBuilder#append]] or [[StringBuffer#append]].
 */
private[string_analysis] trait L0AppendCallInterpreter[State <: L0ComputationState]
    extends L0StringInterpreter[State] {

    override type T = VirtualFunctionCall[V]

    val ps: PropertyStore

    private[this] case class AppendCallState(
        appendCall:            T,
        param:                 V,
        defSitePC:             Int,
        var receiverDependees: Seq[EOptionP[DefSiteEntity, StringConstancyProperty]],
        var valueDependees:    Seq[EOptionP[DefSiteEntity, StringConstancyProperty]]
    ) {

        def updateDependee(newDependee: EOptionP[DefSiteEntity, StringConstancyProperty]): Unit = {
            if (receiverDependees.exists(_.e == newDependee.e)) {
                receiverDependees = receiverDependees.updated(
                    receiverDependees.indexWhere(_.e == newDependee.e),
                    newDependee
                )
            } else {
                valueDependees = valueDependees.updated(
                    valueDependees.indexWhere(_.e == newDependee.e),
                    newDependee
                )
            }
        }

        def hasDependees: Boolean =
            receiverDependees.exists(_.isRefinable) || valueDependees.exists(_.isRefinable)

        def dependees: Iterable[EOptionP[DefSiteEntity, StringConstancyProperty]] =
            receiverDependees.filter(_.isRefinable) ++ valueDependees.filter(_.isRefinable)
    }

    def interpretAppendCall(appendCall: T, pc: Int)(implicit
        state: State
    ): ProperPropertyComputationResult = {
        // Get receiver results
        val receiverResults = appendCall.receiver.asVar.definedBy.toList.sorted.map { ds =>
            ps(InterpretationHandler.getEntityFromDefSite(ds), StringConstancyProperty.key)
        }
        // Get parameter results
        // .head because we want to evaluate only the first argument of append
        val param = appendCall.params.head.asVar
        val valueResults = param.definedBy.toList.sorted.map { ds =>
            val usedDS = if (ds >= 0 && state.tac.stmts(ds).isAssignment && state.tac.stmts(ds).asAssignment.expr.isNew) {
                state.tac.stmts(ds).asAssignment.targetVar.usedBy.toArray.min
            } else {
                ds
            }
            ps(InterpretationHandler.getEntityFromDefSite(usedDS), StringConstancyProperty.key)
        }
        implicit val appendState: AppendCallState = AppendCallState(appendCall, param, pc, receiverResults, valueResults)

        tryComputeFinalAppendCallResult
    }

    private def continuation(
        state:       State,
        appendState: AppendCallState
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case UBP(_: StringConstancyProperty) =>
                appendState.updateDependee(eps.asInstanceOf[EOptionP[DefSiteEntity, StringConstancyProperty]])
                tryComputeFinalAppendCallResult(state, appendState)

            case _ => throw new IllegalArgumentException(s"Encountered unknown eps: $eps")
        }
    }

    private def tryComputeFinalAppendCallResult(implicit
        state:       State,
        appendState: AppendCallState
    ): ProperPropertyComputationResult = {
        if (appendState.hasDependees) {
            InterimResult.forLB(
                InterpretationHandler.getEntityFromDefSitePC(appendState.defSitePC),
                StringConstancyProperty.lb,
                appendState.dependees.toSet,
                continuation(state, appendState)
            )
        } else {
            val receiverSci = StringConstancyInformation.reduceMultiple(appendState.receiverDependees.map {
                _.asFinal.p.sci
            })
            val valueSci = transformAppendValueResult(
                appendState.valueDependees.asInstanceOf[Seq[FinalEP[DefSiteEntity, StringConstancyProperty]]]
            )

            computeFinalResult(
                appendState.defSitePC,
                StringConstancyInformation(
                    StringConstancyType.APPEND,
                    StringTreeConcat.fromNodes(receiverSci.tree, valueSci.tree)
                )
            )
        }
    }

    private def transformAppendValueResult(
        results: Seq[FinalEP[DefSiteEntity, StringConstancyProperty]]
    )(implicit appendState: AppendCallState): StringConstancyInformation = {
        val sciValues = results.map(_.p.sci)
        val newValueSci = StringConstancyInformation.reduceMultiple(sciValues)

        appendState.param.value.computationalType match {
            case ComputationalTypeInt =>
                if (appendState.appendCall.descriptor.parameterType(0).isCharType &&
                    newValueSci.constancyLevel == StringConstancyLevel.CONSTANT &&
                    sciValues.exists(!_.isTheNeutralElement)
                ) {
                    val charSciValues = sciValues map {
                        case sci @ StringConstancyInformation(_, const: StringTreeConst) if const.isIntConst =>
                            sci.copy(tree = StringTreeConst(const.string.toInt.toChar.toString))
                        case sci =>
                            sci
                    }
                    StringConstancyInformation.reduceMultiple(charSciValues)
                } else {
                    newValueSci
                }
            case ComputationalTypeFloat | ComputationalTypeDouble =>
                if (newValueSci.constancyLevel == StringConstancyLevel.CONSTANT) {
                    newValueSci
                } else {
                    StringConstancyInformation.dynamicFloat
                }
            case _ =>
                newValueSci
        }
    }
}

/**
 * Interprets calls to [[String#substring]].
 */
private[string_analysis] trait L0SubstringCallInterpreter[State <: L0ComputationState]
    extends L0StringInterpreter[State] {

    override type T = VirtualFunctionCall[V]

    val ps: PropertyStore

    def interpretSubstringCall(substringCall: T, pc: Int)(implicit
        state: State
    ): ProperPropertyComputationResult = {
        val receiverResults = substringCall.receiver.asVar.definedBy.toList.sorted.map { ds =>
            ps(InterpretationHandler.getEntityFromDefSite(ds), StringConstancyProperty.key)
        }

        if (receiverResults.exists(_.isRefinable)) {
            InterimResult.forLB(
                InterpretationHandler.getEntityFromDefSitePC(pc),
                StringConstancyProperty.lb,
                receiverResults.toSet,
                awaitAllFinalContinuation(
                    EPSDepender(substringCall, substringCall.pc, state, receiverResults),
                    computeFinalSubstringCallResult(substringCall, pc)
                )
            )
        } else {
            computeFinalSubstringCallResult(substringCall, pc)(receiverResults.asInstanceOf[Seq[SomeFinalEP]])
        }
    }

    private def computeFinalSubstringCallResult(substringCall: T, pc: Int)(
        results: Seq[SomeFinalEP]
    )(implicit state: State): Result = {
        val receiverSci = StringConstancyInformation.reduceMultiple(results.map {
            _.p.asInstanceOf[StringConstancyProperty].sci
        })
        if (!receiverSci.tree.isInstanceOf[StringTreeConst]) {
            // We cannot yet interpret substrings of mixed values
            computeFinalResult(pc, StringConstancyInformation.lb)
        } else {
            val parameterCount = substringCall.params.size
            parameterCount match {
                case 1 =>
                    substringCall.params.head.asVar.value match {
                        case intValue: TheIntegerValue =>
                            computeFinalResult(
                                pc,
                                StringConstancyInformation(
                                    StringConstancyType.REPLACE,
                                    StringTreeConst(
                                        receiverSci.tree.asInstanceOf[StringTreeConst].string.substring(intValue.value)
                                    )
                                )
                            )
                        case _ =>
                            computeFinalResult(pc, StringConstancyInformation.lb)
                    }

                case 2 =>
                    (substringCall.params.head.asVar.value, substringCall.params(1).asVar.value) match {
                        case (firstIntValue: TheIntegerValue, secondIntValue: TheIntegerValue) =>
                            computeFinalResult(
                                pc,
                                StringConstancyInformation(
                                    StringConstancyType.APPEND,
                                    StringTreeConst(
                                        receiverSci.tree.asInstanceOf[StringTreeConst].string.substring(
                                            firstIntValue.value,
                                            secondIntValue.value
                                        )
                                    )
                                )
                            )
                        case _ =>
                            computeFinalResult(pc, StringConstancyInformation.lb)
                    }

                case _ => throw new IllegalStateException(
                        s"Unexpected parameter count for ${substringCall.descriptor.toJava}. Expected one or two, got $parameterCount"
                    )
            }
        }
    }
}
