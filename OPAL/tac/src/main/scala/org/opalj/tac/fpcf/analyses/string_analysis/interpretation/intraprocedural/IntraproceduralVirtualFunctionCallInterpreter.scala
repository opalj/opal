/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.interpretation.intraprocedural

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalEP
import org.opalj.br.cfg.CFG
import org.opalj.br.ComputationalTypeFloat
import org.opalj.br.ComputationalTypeInt
import org.opalj.br.ObjectType
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel
import org.opalj.br.fpcf.properties.string_definition.StringConstancyType
import org.opalj.br.ComputationalTypeDouble
import org.opalj.br.DoubleType
import org.opalj.br.FloatType
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.fpcf.analyses.string_analysis.V
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.AbstractStringInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler

/**
 * The `IntraproceduralVirtualFunctionCallInterpreter` is responsible for processing
 * [[VirtualFunctionCall]]s in an intraprocedural fashion.
 * The list of currently supported function calls can be seen in the documentation of [[interpret]].
 *
 * @see [[AbstractStringInterpreter]]
 *
 * @author Patrick Mell
 */
class IntraproceduralVirtualFunctionCallInterpreter(
        cfg:         CFG[Stmt[V], TACStmts[V]],
        exprHandler: IntraproceduralInterpretationHandler
) extends AbstractStringInterpreter(cfg, exprHandler) {

    override type T = VirtualFunctionCall[V]

    /**
     * Currently, this implementation supports the interpretation of the following function calls:
     * <ul>
     * <li>`append`: Calls to the `append` function of [[StringBuilder]] and [[StringBuffer]].</li>
     * <li>
     *     `toString`: Calls to the `append` function of [[StringBuilder]] and [[StringBuffer]]. As
     *     a `toString` call does not change the state of such an object, an empty list will be
     *     returned.
     * </li>
     * <li>
     *     `replace`: Calls to the `replace` function of [[StringBuilder]] and [[StringBuffer]]. For
     *     further information how this operation is processed, see
     *     [[IntraproceduralVirtualFunctionCallInterpreter.interpretReplaceCall]].
     * </li>
     * <li>
     *     Apart from these supported methods, a list with [[StringConstancyProperty.lb]]
     *     will be returned in case the passed method returns a [[java.lang.String]].
     * </li>
     * </ul>
     *
     * If none of the above-described cases match, a result containing
     * [[StringConstancyProperty.getNeutralElement]] will be returned.
     *
     * @note For this implementation, `defSite` does not play a role.
     *
     * @see [[AbstractStringInterpreter.interpret]]
     */
    override def interpret(instr: T, defSite: Int): EOptionP[Entity, StringConstancyProperty] = {
        val property = instr.name match {
            case "append"   ⇒ interpretAppendCall(instr)
            case "toString" ⇒ interpretToStringCall(instr)
            case "replace"  ⇒ interpretReplaceCall(instr)
            case _ ⇒
                instr.descriptor.returnType match {
                    case obj: ObjectType if obj.fqn == "java/lang/String" ⇒
                        StringConstancyProperty.lb
                    case FloatType | DoubleType ⇒
                        StringConstancyProperty(StringConstancyInformation(
                            StringConstancyLevel.DYNAMIC,
                            StringConstancyType.APPEND,
                            StringConstancyInformation.FloatValue
                        ))
                    case _ ⇒ StringConstancyProperty.getNeutralElement
                }
        }

        FinalEP(instr, property)
    }

    /**
     * Function for processing calls to [[StringBuilder#append]] or [[StringBuffer#append]]. Note
     * that this function assumes that the given `appendCall` is such a function call! Otherwise,
     * the expected behavior cannot be guaranteed.
     */
    private def interpretAppendCall(
        appendCall: VirtualFunctionCall[V]
    ): StringConstancyProperty = {
        val receiverSci = receiverValuesOfAppendCall(appendCall).stringConstancyInformation
        val appendSci = valueOfAppendCall(appendCall).stringConstancyInformation

        // The case can occur that receiver and append value are empty; although, it is
        // counter-intuitive, this case may occur if both, the receiver and the parameter, have been
        // processed before
        val sci = if (receiverSci.isTheNeutralElement && appendSci.isTheNeutralElement) {
            StringConstancyInformation.getNeutralElement
        } // It might be that we have to go back as much as to a New expression. As they do not
        // produce a result (= empty list), the if part
        else if (receiverSci.isTheNeutralElement) {
            appendSci
        } // The append value might be empty, if the site has already been processed (then this
        // information will come from another StringConstancyInformation object
        else if (appendSci.isTheNeutralElement) {
            receiverSci
        } // Receiver and parameter information are available => Combine them
        else {
            StringConstancyInformation(
                StringConstancyLevel.determineForConcat(
                    receiverSci.constancyLevel, appendSci.constancyLevel
                ),
                StringConstancyType.APPEND,
                receiverSci.possibleStrings + appendSci.possibleStrings
            )
        }

        StringConstancyProperty(sci)
    }

    /**
     * This function determines the current value of the receiver object of an `append` call.
     */
    private def receiverValuesOfAppendCall(
        call: VirtualFunctionCall[V]
    ): StringConstancyProperty = {
        // There might be several receivers, thus the map; from the processed sites, however, use
        // only the head as a single receiver interpretation will produce one element
        val scis = call.receiver.asVar.definedBy.toArray.sorted.map { ds ⇒
            val r = exprHandler.processDefSite(ds)
            r.asFinal.p.asInstanceOf[StringConstancyProperty].stringConstancyInformation
        }.filter { sci ⇒ !sci.isTheNeutralElement }
        val sci = if (scis.isEmpty) StringConstancyInformation.getNeutralElement else
            scis.head
        StringConstancyProperty(sci)
    }

    /**
     * Determines the (string) value that was passed to a `String{Builder, Buffer}#append` method.
     * This function can process string constants as well as function calls as argument to append.
     */
    private def valueOfAppendCall(
        call: VirtualFunctionCall[V]
    ): StringConstancyProperty = {
        val param = call.params.head.asVar
        // .head because we want to evaluate only the first argument of append
        val defSiteHead = param.definedBy.head
        var r = exprHandler.processDefSite(defSiteHead)
        var value = r.asFinal.p.asInstanceOf[StringConstancyProperty]
        // If defSiteHead points to a New, value will be the empty list. In that case, process
        // the first use site (which is the <init> call)
        if (value.isTheNeutralElement) {
            r = exprHandler.processDefSite(
                cfg.code.instructions(defSiteHead).asAssignment.targetVar.usedBy.toArray.min
            ).asFinal
            value = r.asFinal.p.asInstanceOf[StringConstancyProperty]
        }

        val sci = value.stringConstancyInformation
        val finalSci = param.value.computationalType match {
            // For some types, we know the (dynamic) values
            case ComputationalTypeInt ⇒
                // The value was already computed above; however, we need to check whether the
                // append takes an int value or a char (if it is a constant char, convert it)
                if (call.descriptor.parameterType(0).isCharType &&
                    sci.constancyLevel == StringConstancyLevel.CONSTANT) {
                    sci.copy(
                        possibleStrings = sci.possibleStrings.toInt.toChar.toString
                    )
                } else {
                    sci
                }
            case ComputationalTypeFloat | ComputationalTypeDouble ⇒
                if (sci.constancyLevel == StringConstancyLevel.CONSTANT) {
                    sci
                } else {
                    InterpretationHandler.getConstancyInfoForDynamicFloat
                }
            // Otherwise, try to compute
            case _ ⇒
                sci
        }

        StringConstancyProperty(finalSci)
    }

    /**
     * Function for processing calls to [[StringBuilder#toString]] or [[StringBuffer#toString]].
     * Note that this function assumes that the given `toString` is such a function call! Otherwise,
     * the expected behavior cannot be guaranteed.
     */
    private def interpretToStringCall(
        call: VirtualFunctionCall[V]
    ): StringConstancyProperty = {
        val finalEP = exprHandler.processDefSite(call.receiver.asVar.definedBy.head).asFinal
        finalEP.p.asInstanceOf[StringConstancyProperty]
    }

    /**
     * Function for processing calls to [[StringBuilder#replace]] or [[StringBuffer#replace]].
     * (Currently, this function simply approximates `replace` functions by returning the lower
     * bound of [[StringConstancyProperty]]).
     */
    private def interpretReplaceCall(
        instr: VirtualFunctionCall[V]
    ): StringConstancyProperty = InterpretationHandler.getStringConstancyPropertyForReplace

}
