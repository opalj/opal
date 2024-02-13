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
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalEP
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.DependingStringInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler
import org.opalj.value.TheIntegerValue

/**
 * Responsible for processing [[VirtualFunctionCall]]s without a call graph.
 * The list of currently supported function calls can be seen in the documentation of [[interpret]].
 *
 * @author Maximilian Rüsch
 */
case class L0VirtualFunctionCallInterpreter[State <: L0ComputationState[State]](
        exprHandler: InterpretationHandler[State]
) extends L0StringInterpreter[State] with DependingStringInterpreter[State] {

    implicit val _exprHandler: InterpretationHandler[State] = exprHandler

    override type T = VirtualFunctionCall[V]

    override def interpret(instr: T, defSite: Int)(implicit state: State): EOptionP[Entity, StringConstancyProperty] = {
        val result = handleInterpretation(instr, defSite)

        if (result.isDefined) {
            state.appendToFpe2Sci(pcOfDefSite(defSite)(state.tac.stmts), result.get)
        }
        FinalEP(defSite.asInstanceOf[Integer], StringConstancyProperty(result.getOrElse(StringConstancyInformation.lb)))
    }

    /**
     * Currently, this implementation supports the interpretation of the following function calls:
     * <ul>
     * <li>`append`: Calls to the `append` function of [[StringBuilder]] and [[StringBuffer]].</li>
     * <li>
     * `toString`: Calls to the `append` function of [[StringBuilder]] and [[StringBuffer]]. As
     * a `toString` call does not change the state of such an object, an empty list will be
     * returned.
     * </li>
     * <li>
     * `replace`: Calls to the `replace` function of [[StringBuilder]] and [[StringBuffer]]. For
     * further information how this operation is processed, see
     * [[L0VirtualFunctionCallInterpreter.interpretReplaceCall]].
     * </li>
     * <li>
     * Apart from these supported methods, a list with [[StringConstancyProperty.lb]]
     * will be returned in case the passed method returns a [[java.lang.String]].
     * </li>
     * </ul>
     *
     * If none of the above-described cases match, a result containing
     * [[StringConstancyProperty.getNeutralElement]] will be returned.
     */
    protected def handleInterpretation(instr: T, defSite: Int)(implicit
        state: State
    ): Option[StringConstancyInformation] = {
        instr.name match {
            case "append"   => interpretAppendCall(instr)
            case "toString" => interpretToStringCall(instr)
            case "replace"  => Some(interpretReplaceCall)
            case "substring" if instr.descriptor.returnType == ObjectType.String => interpretSubstringCall(instr)
            case _ =>
                instr.descriptor.returnType match {
                    case obj: ObjectType if obj == ObjectType.String => Some(StringConstancyInformation.lb)
                    case FloatType | DoubleType => Some(StringConstancyInformation(
                            StringConstancyLevel.DYNAMIC,
                            StringConstancyType.APPEND,
                            StringConstancyInformation.FloatValue
                        ))
                    case _ => Some(StringConstancyInformation.getNeutralElement)
                }
        }
    }

    /**
     * Function for processing calls to [[StringBuilder#append]] or [[StringBuffer#append]]. Note
     * that this function assumes that the given `appendCall` is such a function call! Otherwise,
     * the expected behavior cannot be guaranteed.
     */
    private def interpretAppendCall(appendCall: VirtualFunctionCall[V])(implicit
        state: State
    ): Option[StringConstancyInformation] = {
        val receiverSci = receiverValuesOfCall(appendCall)
        val appendSci = valueOfAppendCall(appendCall)

        if (appendSci.isEmpty) {
            None
        } else {
            val sci = if (receiverSci.isTheNeutralElement && appendSci.get.isTheNeutralElement) {
                // although counter-intuitive, this case may occur if both the receiver and the parameter have been
                // processed before
                StringConstancyInformation.getNeutralElement
            } else if (receiverSci.isTheNeutralElement) {
                // It might be that we have to go back as much as to a New expression. As they do not
                // produce a result (= empty list), the if part
                appendSci.get
            } else if (appendSci.get.isTheNeutralElement) {
                // The append value might be empty, if the site has already been processed (then this
                // information will come from another StringConstancyInformation object
                receiverSci
            } else {
                // Receiver and parameter information are available => combine them
                StringConstancyInformation(
                    StringConstancyLevel.determineForConcat(receiverSci.constancyLevel, appendSci.get.constancyLevel),
                    StringConstancyType.APPEND,
                    receiverSci.possibleStrings + appendSci.get.possibleStrings
                )
            }

            Some(sci)
        }
    }

    /**
     * Determines the (string) value that was passed to a `String{Builder, Buffer}#append` method.
     * This function can process string constants as well as function calls as argument to append.
     */
    private def valueOfAppendCall(call: VirtualFunctionCall[V])(implicit
        state: State
    ): Option[StringConstancyInformation] = {
        val param = call.params.head.asVar
        // .head because we want to evaluate only the first argument of append
        val defSiteHead = param.definedBy.head
        var value = handleDependentDefSite(defSiteHead)
        // If defSiteHead points to a New, value will be the empty list. In that case, process
        // the first use site (which is the <init> call)
        if (value.isDefined && value.get.isTheNeutralElement) {
            value = handleDependentDefSite(state.tac.stmts(defSiteHead).asAssignment.targetVar.usedBy.toArray.min)
        }

        if (value.isEmpty) {
            None
        } else {
            val sci = value.get
            val finalSci = param.value.computationalType match {
                // For some types, we know the (dynamic) values
                case ComputationalTypeInt =>
                    // The value was already computed above; however, we need to check whether the
                    // append takes an int value or a char (if it is a constant char, convert it)
                    if (call.descriptor.parameterType(0).isCharType &&
                        sci.constancyLevel == StringConstancyLevel.CONSTANT
                    ) {
                        sci.copy(possibleStrings = sci.possibleStrings.toInt.toChar.toString)
                    } else {
                        sci
                    }
                case ComputationalTypeFloat | ComputationalTypeDouble =>
                    if (sci.constancyLevel == StringConstancyLevel.CONSTANT) {
                        sci
                    } else {
                        InterpretationHandler.getConstancyInfoForDynamicFloat
                    }
                // Otherwise, try to compute
                case _ =>
                    sci
            }

            Some(finalSci)
        }
    }

    /**
     * Processes calls to [[String#substring]].
     */
    private def interpretSubstringCall(substringCall: T)(implicit state: State): Option[StringConstancyInformation] = {
        val receiverSci = receiverValuesOfCall(substringCall)

        if (receiverSci.isComplex) {
            // We cannot yet interpret substrings of mixed values
            Some(StringConstancyInformation.lb)
        } else {
            val parameterCount = substringCall.params.size
            parameterCount match {
                case 1 =>
                    substringCall.params.head.asVar.value match {
                        case intValue: TheIntegerValue =>
                            Some(StringConstancyInformation(
                                StringConstancyLevel.CONSTANT,
                                StringConstancyType.REPLACE,
                                receiverSci.possibleStrings.substring(intValue.value)
                            ))
                        case _ =>
                            Some(StringConstancyInformation.lb)
                    }

                case 2 =>
                    (substringCall.params.head.asVar.value, substringCall.params(1).asVar.value) match {
                        case (firstIntValue: TheIntegerValue, secondIntValue: TheIntegerValue) =>
                            Some(StringConstancyInformation(
                                StringConstancyLevel.CONSTANT,
                                StringConstancyType.APPEND,
                                receiverSci.possibleStrings.substring(firstIntValue.value, secondIntValue.value)
                            ))
                        case _ =>
                            Some(StringConstancyInformation.lb)
                    }

                case _ => throw new IllegalStateException(
                    s"Unexpected parameter count for ${substringCall.descriptor.toJava}. Expected one or two, got $parameterCount"
                )
            }
        }
    }

    /**
     * This function determines the current value of the receiver object of a call.
     */
    private def receiverValuesOfCall(call: T)(implicit state: State): StringConstancyInformation = {
        // There might be several receivers, thus the map; from the processed sites, however, use
        // only the head as a single receiver interpretation will produce one element
        val scis = call.receiver.asVar.definedBy.toArray.sorted.map { ds =>
            // IMPROVE enable handling dependees here
            val r = exprHandler.processDefSite(ds)
            r.asFinal.p.stringConstancyInformation
        }.filter { sci => !sci.isTheNeutralElement }
        scis.headOption.getOrElse(StringConstancyInformation.getNeutralElement)
    }

    /**
     * Processes calls to [[StringBuilder#toString]] or [[StringBuffer#toString]]. Note that this function assumes that
     * the given `toString` is such a function call! Otherwise, the expected behavior cannot be guaranteed.
     */
    private def interpretToStringCall(call: T)(implicit state: State): Option[StringConstancyInformation] = {
        handleInterpretationResult(exprHandler.processDefSite(call.receiver.asVar.definedBy.head))
    }

    /**
     * Processes calls to [[StringBuilder#replace]] or [[StringBuffer#replace]]. (Currently, this function simply
     * approximates `replace` functions by returning the lower bound of [[StringConstancyProperty]]).
     */
    private def interpretReplaceCall: StringConstancyInformation =
        InterpretationHandler.getStringConstancyInformationForReplace
}
