/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l0
package interpretation

import scala.util.Try

import org.opalj.br.ComputationalTypeDouble
import org.opalj.br.ComputationalTypeFloat
import org.opalj.br.ComputationalTypeInt
import org.opalj.br.DoubleType
import org.opalj.br.FloatType
import org.opalj.br.ObjectType
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel
import org.opalj.br.fpcf.properties.string_definition.StringConstancyType
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler
import org.opalj.value.TheIntegerValue

/**
 * Responsible for processing [[VirtualFunctionCall]]s without a call graph.
 *
 * @author Maximilian Rüsch
 */
case class L0VirtualFunctionCallInterpreter[State <: L0ComputationState[State]](
    exprHandler: InterpretationHandler[State]
) extends L0StringInterpreter[State] with IPResultDependingStringInterpreter[State] {

    override type T = VirtualFunctionCall[V]

    /**
     * Currently, this implementation supports the interpretation of the following function calls:
     * <ul>
     * <li>`append`: Calls to the `append` function of [[StringBuilder]] and [[StringBuffer]].</li>
     * <li>
     * `toString`: Calls to the `append` function of [[StringBuilder]] and [[StringBuffer]]. As a `toString` call does
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
    override def interpret(instr: T, defSite: Int)(implicit state: State): IPResult = {
        instr.name match {
            case "append"                                                        => interpretAppendCall(instr)
            case "toString"                                                      => interpretToStringCall(instr)
            case "replace"                                                       => interpretReplaceCall(instr)
            case "substring" if instr.descriptor.returnType == ObjectType.String => interpretSubstringCall(instr)
            case _ =>
                instr.descriptor.returnType match {
                    case obj: ObjectType if obj == ObjectType.String =>
                        interpretArbitraryCall(instr, defSite)
                    case FloatType | DoubleType => FinalIPResult(
                            StringConstancyInformation(
                                StringConstancyLevel.DYNAMIC,
                                StringConstancyType.APPEND,
                                StringConstancyInformation.FloatValue
                            ),
                            state.dm,
                            instr.pc
                        )
                    case _ => NoIPResult(state.dm, instr.pc)
                }
        }
    }

    protected def interpretArbitraryCall(call: T, defSite: Int)(implicit state: State): IPResult =
        FinalIPResult.lb(state.dm, call.pc)

    /**
     * Function for processing calls to [[StringBuilder#append]] or [[StringBuffer#append]]. Note
     * that this function assumes that the given `appendCall` is such a function call! Otherwise,
     * the expected behavior cannot be guaranteed.
     */
    private def interpretAppendCall(appendCall: T)(implicit state: State): IPResult = {
        def computeFinalAppendCallResult(receiverResults: Iterable[IPResult], appendResult: IPResult): FinalIPResult = {
            val receiverScis = receiverResults.map(_.asFinal.sci)
            val appendSci = appendResult.asFinal.sci
            val areAllReceiversNeutral = receiverScis.forall(_.isTheNeutralElement)
            val sci = if (areAllReceiversNeutral && appendSci.isTheNeutralElement) {
                // although counter-intuitive, this case occurs if both receiver and parameter have been processed before
                StringConstancyInformation.getNeutralElement
            } else if (areAllReceiversNeutral) {
                // It might be that we have to go back as much as to a New expression. As they do not
                // produce a result (= empty list), the if part
                appendSci
            } else if (appendSci.isTheNeutralElement) {
                // The append value might be empty, if the site has already been processed (then this
                // information will come from another StringConstancyInformation object
                StringConstancyInformation.reduceMultiple(receiverScis)
            } else {
                // Receiver and parameter information are available => combine them
                val receiverSci = StringConstancyInformation.reduceMultiple(receiverScis)
                StringConstancyInformation(
                    StringConstancyLevel.determineForConcat(receiverSci.constancyLevel, appendSci.constancyLevel),
                    StringConstancyType.APPEND,
                    receiverSci.possibleStrings + appendSci.possibleStrings
                )
            }

            FinalIPResult(sci, state.dm, appendCall.pc)
        }

        val receiverResults = receiverValuesOfCall(appendCall)
        val appendResult = valueOfAppendCall(appendCall)

        if (receiverResults.exists(_.isRefinable) || appendResult.isRefinable) {
            val allRefinableResults = if (appendResult.isRefinable) {
                receiverResults.filter(_.isRefinable) :+ appendResult
            } else receiverResults.filter(_.isRefinable)

            InterimIPResult.lbWithIPResultDependees(
                state.dm,
                appendCall.pc,
                allRefinableResults.asInstanceOf[Iterable[RefinableIPResult]],
                awaitAllFinalContinuation(
                    SimpleIPResultDepender(appendCall, appendCall.pc, state, receiverResults :+ appendResult),
                    (results: Iterable[IPResult]) => {
                        computeFinalAppendCallResult(
                            results.filter(_.e != appendResult.e),
                            results.find(_.e == appendResult.e).get
                        )
                    }
                )
            )
        } else {
            computeFinalAppendCallResult(receiverResults, appendResult)
        }
    }

    /**
     * Determines the (string) value that was passed to a `String{Builder, Buffer}#append` method.
     * This function can process string constants as well as function calls as argument to append.
     */
    private def valueOfAppendCall(call: T)(implicit state: State): IPResult = {
        // .head because we want to evaluate only the first argument of append
        val param = call.params.head.asVar
        val defSites = param.definedBy.toArray.sorted

        def computeFinalAppendValueResult(results: Iterable[IPResult]): FinalIPResult = {
            val sciValues = results.map(_.asFinal.sci)
            val newValueSci = StringConstancyInformation.reduceMultiple(sciValues)

            val finalSci = param.value.computationalType match {
                case ComputationalTypeInt =>
                    if (call.descriptor.parameterType(0).isCharType &&
                        newValueSci.constancyLevel == StringConstancyLevel.CONSTANT &&
                        sciValues.exists(!_.isTheNeutralElement)
                    ) {
                        val charSciValues = sciValues.filter(_.possibleStrings != "") map { sci =>
                            if (Try(sci.possibleStrings.toInt).isSuccess) {
                                sci.copy(possibleStrings = sci.possibleStrings.toInt.toChar.toString)
                            } else {
                                sci
                            }
                        }
                        StringConstancyInformation.reduceMultiple(charSciValues)
                    } else {
                        newValueSci
                    }
                case ComputationalTypeFloat | ComputationalTypeDouble =>
                    if (newValueSci.constancyLevel == StringConstancyLevel.CONSTANT) {
                        newValueSci
                    } else {
                        InterpretationHandler.getConstancyInfoForDynamicFloat
                    }
                case _ =>
                    newValueSci
            }

            FinalIPResult(finalSci, state.dm, call.pc)
        }

        if (defSites.exists(_ < 0)) {
            return FinalIPResult.lb(state.dm, call.pc)
        }

        val valueResults = defSites.map { ds =>
            state.tac.stmts(ds) match {
                // If a site points to a "New", process the first use site
                case Assignment(_, targetVar, _: New) => exprHandler.processDefSite(targetVar.usedBy.toArray.min)
                case _                                => exprHandler.processDefSite(ds)
            }
        }

        // Defer the computation if there is at least one intermediate result
        if (valueResults.exists(_.isRefinable)) {
            return InterimIPResult.lbWithIPResultDependees(
                state.dm,
                call.pc,
                valueResults.filter(_.isRefinable).asInstanceOf[Iterable[RefinableIPResult]],
                awaitAllFinalContinuation(
                    SimpleIPResultDepender(call, call.pc, state, valueResults.toIndexedSeq),
                    computeFinalAppendValueResult
                )
            )
        }

        computeFinalAppendValueResult(valueResults)
    }

    /**
     * Processes calls to [[String#substring]].
     */
    private def interpretSubstringCall(substringCall: T)(implicit state: State): IPResult = {
        def computeFinalSubstringCallResult(results: Iterable[IPResult]): FinalIPResult = {
            val receiverSci = StringConstancyInformation.reduceMultiple(results.map(_.asFinal.sci))
            if (receiverSci.isComplex) {
                // We cannot yet interpret substrings of mixed values
                FinalIPResult.lb(state.dm, substringCall.pc)
            } else {
                val parameterCount = substringCall.params.size
                parameterCount match {
                    case 1 =>
                        substringCall.params.head.asVar.value match {
                            case intValue: TheIntegerValue =>
                                FinalIPResult(
                                    StringConstancyInformation(
                                        StringConstancyLevel.CONSTANT,
                                        StringConstancyType.REPLACE,
                                        receiverSci.possibleStrings.substring(intValue.value)
                                    ),
                                    state.dm,
                                    substringCall.pc
                                )
                            case _ =>
                                FinalIPResult.lb(state.dm, substringCall.pc)
                        }

                    case 2 =>
                        (substringCall.params.head.asVar.value, substringCall.params(1).asVar.value) match {
                            case (firstIntValue: TheIntegerValue, secondIntValue: TheIntegerValue) =>
                                FinalIPResult(
                                    StringConstancyInformation(
                                        StringConstancyLevel.CONSTANT,
                                        StringConstancyType.APPEND,
                                        receiverSci.possibleStrings.substring(firstIntValue.value, secondIntValue.value)
                                    ),
                                    state.dm,
                                    substringCall.pc
                                )
                            case _ =>
                                FinalIPResult.lb(state.dm, substringCall.pc)
                        }

                    case _ => throw new IllegalStateException(
                            s"Unexpected parameter count for ${substringCall.descriptor.toJava}. Expected one or two, got $parameterCount"
                        )
                }
            }
        }

        val receiverResults = receiverValuesOfCall(substringCall)
        if (receiverResults.forall(_.isNoResult)) {
            return FinalIPResult.lb(state.dm, substringCall.pc)
        }

        if (receiverResults.exists(_.isRefinable)) {
            InterimIPResult.lbWithIPResultDependees(
                state.dm,
                substringCall.pc,
                receiverResults.filter(_.isRefinable).asInstanceOf[Iterable[RefinableIPResult]],
                awaitAllFinalContinuation(
                    SimpleIPResultDepender(substringCall, substringCall.pc, state, receiverResults),
                    computeFinalSubstringCallResult
                )
            )
        } else {
            computeFinalSubstringCallResult(receiverResults)
        }
    }

    /**
     * This function determines the current value of the receiver object of a call.
     */
    private def receiverValuesOfCall(call: T)(implicit state: State): Seq[IPResult] = {
        val defSites = call.receiver.asVar.definedBy.toArray.sorted
        val allResults = defSites.map(ds => (pcOfDefSite(ds)(state.tac.stmts), exprHandler.processDefSite(ds)))
        allResults.foreach { r => state.fpe2ipr(r._1) = r._2 }

        allResults.toIndexedSeq.map(_._2)
    }

    /**
     * Processes calls to [[StringBuilder#toString]] or [[StringBuffer#toString]]. Note that this function assumes that
     * the given `toString` is such a function call! Otherwise, the expected behavior cannot be guaranteed.
     */
    private def interpretToStringCall(call: T)(implicit state: State): IPResult = {
        FallThroughIPResult(state.dm, call.pc, call.receiver.asVar.definedBy.head)
    }

    /**
     * Processes calls to [[StringBuilder#replace]] or [[StringBuffer#replace]].
     */
    private def interpretReplaceCall(call: T)(implicit state: State): IPResult =
        FinalIPResult(InterpretationHandler.getStringConstancyInformationForReplace, state.dm, call.pc)
}
