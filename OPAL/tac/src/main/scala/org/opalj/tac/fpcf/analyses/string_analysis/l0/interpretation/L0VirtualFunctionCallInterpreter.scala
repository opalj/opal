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
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformationConst
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformationFunction
import org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel
import org.opalj.br.fpcf.properties.string_definition.StringTreeConcat
import org.opalj.br.fpcf.properties.string_definition.StringTreeConst
import org.opalj.br.fpcf.properties.string_definition.StringTreeDynamicString
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
case class L0VirtualFunctionCallInterpreter(
    override val ps: PropertyStore
) extends StringInterpreter
    with L0ArbitraryVirtualFunctionCallInterpreter
    with L0AppendCallInterpreter
    with L0SubstringCallInterpreter {

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
    override def interpret(instr: T, pc: Int)(implicit state: DUSiteState): ProperPropertyComputationResult = {
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

    private def interpretToStringCall(call: T, pc: Int)(implicit
        state: DUSiteState
    ): ProperPropertyComputationResult = {
        def computeResult(eps: SomeEOptionP): ProperPropertyComputationResult = {
            eps match {
                case FinalP(sciP: StringConstancyProperty) =>
                    computeFinalResult(pc, sciP.sci)

                case iep: InterimEP[_, _] if eps.pk == StringConstancyProperty.key =>
                    InterimResult.forUB(
                        InterpretationHandler.getEntityForPC(pc),
                        iep.ub.asInstanceOf[StringConstancyProperty],
                        Set(eps),
                        computeResult
                    )

                case _ if eps.pk == StringConstancyProperty.key =>
                    InterimResult.forUB(
                        InterpretationHandler.getEntityForPC(pc),
                        StringConstancyProperty.ub,
                        Set(eps),
                        computeResult
                    )

                case _ => throw new IllegalArgumentException(s"Encountered unknown eps: $eps")
            }
        }

        computeResult(ps(
            InterpretationHandler.getEntityForDefSite(call.receiver.asVar.definedBy.head),
            StringConstancyProperty.key
        ))
    }

    /**
     * Processes calls to [[StringBuilder#replace]] or [[StringBuffer#replace]].
     */
    private def interpretReplaceCall(pc: Int)(implicit state: DUSiteState): ProperPropertyComputationResult =
        computeFinalResult(pc, StringConstancyInformationConst(StringTreeDynamicString))
}

private[string_analysis] trait L0ArbitraryVirtualFunctionCallInterpreter extends StringInterpreter {

    protected def interpretArbitraryCall(call: T, pc: Int)(implicit
        state: DUSiteState
    ): ProperPropertyComputationResult =
        computeFinalResult(pc, StringConstancyInformation.lb)
}

/**
 * Interprets calls to [[StringBuilder#append]] or [[StringBuffer#append]].
 */
private[string_analysis] trait L0AppendCallInterpreter extends StringInterpreter {

    override type T = VirtualFunctionCall[V]

    val ps: PropertyStore

    private[this] case class AppendCallState(
        appendCall:               T,
        param:                    V,
        defSitePC:                Int,
        state:                    DUSiteState,
        var receiverDependeesOpt: Option[Seq[EOptionP[DUSiteEntity, StringConstancyProperty]]],
        var valueDependees:       Seq[EOptionP[DUSiteEntity, StringConstancyProperty]]
    ) {

        def updateDependee(newDependee: EOptionP[DUSiteEntity, StringConstancyProperty]): Unit = {
            if (receiverDependeesOpt.isDefined && receiverDependeesOpt.get.exists(_.e == newDependee.e)) {
                val receiverDependees = receiverDependeesOpt.get
                receiverDependeesOpt = Some(receiverDependees.updated(
                    receiverDependees.indexWhere(_.e == newDependee.e),
                    newDependee
                ))
            } else {
                valueDependees = valueDependees.updated(
                    valueDependees.indexWhere(_.e == newDependee.e),
                    newDependee
                )
            }
        }

        def hasDependees: Boolean =
            (receiverDependeesOpt.isDefined && receiverDependeesOpt.get.exists(_.isRefinable)) || valueDependees.exists(
                _.isRefinable
            )

        def dependees: Iterable[EOptionP[DUSiteEntity, StringConstancyProperty]] =
            receiverDependeesOpt.iterator.toSeq.flatMap(Seq.from).filter(_.isRefinable) ++ valueDependees.filter(
                _.isRefinable
            )
    }

    def interpretAppendCall(appendCall: T, pc: Int)(implicit
        state: DUSiteState
    ): ProperPropertyComputationResult = {
        val receiverVar = appendCall.receiver.asVar

        val receiverResults = if (receiverVar.toPersistentForm(state.tac.stmts).equals(state.entity)) {
            // Previous state of the receiver will be handled by path resolution
            System.out.println("DAMN WE ACHIEVED IT!")
            None
        } else {
            // Get receiver results
            Some(
                receiverVar.definedBy.toList.sorted.map { ds =>
                    ps(InterpretationHandler.getEntityForDefSite(ds), StringConstancyProperty.key)
                }
            )
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
            ps(InterpretationHandler.getEntityForDefSite(usedDS), StringConstancyProperty.key)
        }
        implicit val appendState: AppendCallState =
            AppendCallState(appendCall, param, pc, state, receiverResults, valueResults)

        tryComputeFinalAppendCallResult
    }

    private def continuation(appendState: AppendCallState)(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case UBP(_: StringConstancyProperty) =>
                appendState.updateDependee(eps.asInstanceOf[EOptionP[DUSiteEntity, StringConstancyProperty]])
                tryComputeFinalAppendCallResult(appendState)

            case _ => throw new IllegalArgumentException(s"Encountered unknown eps: $eps")
        }
    }

    private def tryComputeFinalAppendCallResult(implicit
        appendState: AppendCallState
    ): ProperPropertyComputationResult = {
        if (appendState.hasDependees) {
            InterimResult.forUB(
                InterpretationHandler.getEntityForPC(appendState.defSitePC)(appendState.state),
                StringConstancyProperty.ub,
                appendState.dependees.toSet,
                continuation(appendState)
            )
        } else {
            val valueSci = transformAppendValueResult(
                appendState.valueDependees.asInstanceOf[Seq[FinalEP[DUSiteEntity, StringConstancyProperty]]]
            )

            val resultSci = if (appendState.receiverDependeesOpt.isDefined) {
                val receiverSci = StringConstancyInformation.reduceMultiple(appendState.receiverDependeesOpt.get.map {
                    _.asFinal.p.sci
                })
                StringConstancyInformationConst(StringTreeConcat.fromNodes(receiverSci.tree, valueSci.tree))
            } else {
                StringConstancyInformationFunction(pv => StringTreeConcat.fromNodes(pv, valueSci.tree))
            }

            computeFinalResult(appendState.defSitePC, resultSci)(appendState.state)
        }
    }

    private def transformAppendValueResult(
        results: Seq[FinalEP[DUSiteEntity, StringConstancyProperty]]
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
                        case StringConstancyInformationConst(const: StringTreeConst) if const.isIntConst =>
                            StringConstancyInformationConst(StringTreeConst(const.string.toInt.toChar.toString))
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
private[string_analysis] trait L0SubstringCallInterpreter extends StringInterpreter {

    override type T = VirtualFunctionCall[V]

    val ps: PropertyStore

    def interpretSubstringCall(substringCall: T, pc: Int)(implicit
        state: DUSiteState
    ): ProperPropertyComputationResult = {
        val receiverResults = substringCall.receiver.asVar.definedBy.toList.sorted.map { ds =>
            ps(InterpretationHandler.getEntityForDefSite(ds), StringConstancyProperty.key)
        }

        if (receiverResults.exists(_.isRefinable)) {
            InterimResult.forUB(
                InterpretationHandler.getEntityForPC(pc),
                StringConstancyProperty.ub,
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
    )(implicit state: DUSiteState): Result = {
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
                                StringConstancyInformationConst(
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
                                StringConstancyInformationConst(
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
