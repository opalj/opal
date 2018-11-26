/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.interpretation

import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts
import org.opalj.br.cfg.CFG
import org.opalj.br.ComputationalTypeFloat
import org.opalj.br.ComputationalTypeInt
import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.fpcf.string_definition.properties.StringConstancyInformation
import org.opalj.fpcf.string_definition.properties.StringConstancyLevel
import org.opalj.tac.VirtualFunctionCall

/**
 * The `VirtualFunctionCallInterpreter` is responsible for processing [[VirtualFunctionCall]]s.
 * The list of currently supported function calls can be seen in the documentation of
 * [[interpret]].
 *
 * @see [[AbstractStringInterpreter]]
 *
 * @author Patrick Mell
 */
class VirtualFunctionCallInterpreter(
        cfg:         CFG[Stmt[V], TACStmts[V]],
        exprHandler: ExprHandler
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
     * </ul>
     *
     * @see [[AbstractStringInterpreter.interpret]]
     */
    override def interpret(instr: T): List[StringConstancyInformation] = {
        instr.name match {
            case "append"   ⇒ interpretAppendCall(instr)
            case "toString" ⇒ interpretToStringCall(instr)
            case _          ⇒ List()
        }
    }

    /**
     * Function for processing calls to [[StringBuilder#append]] or [[StringBuffer#append]]. Note
     * that this function assumes that the given `appendCall` is such a function call! Otherwise,
     * the expected behavior cannot be guaranteed.
     */
    private def interpretAppendCall(
        appendCall: VirtualFunctionCall[V]
    ): List[StringConstancyInformation] = {
        val receiverValue = receiverValueOfAppendCall(appendCall)
        val appendValue = valueOfAppendCall(appendCall)

        if (receiverValue.isEmpty && appendValue.isEmpty) {
            List()
        } else if (receiverValue.isDefined && appendValue.isEmpty) {
            List(receiverValue.get)
        } else if (receiverValue.isEmpty && appendValue.nonEmpty) {
            List(appendValue.get)
        } else {
            List(StringConstancyInformation(
                StringConstancyLevel.determineForConcat(
                    receiverValue.get.constancyLevel,
                    appendValue.get.constancyLevel
                ),
                receiverValue.get.possibleStrings + appendValue.get.possibleStrings
            ))
        }
    }

    /**
     * This function determines the current value of the receiver object of an `append` call.
     */
    private def receiverValueOfAppendCall(
        call: VirtualFunctionCall[V]
    ): Option[StringConstancyInformation] =
        exprHandler.processDefSite(call.receiver.asVar.definedBy.head).headOption

    /**
     * Determines the (string) value that was passed to a `String{Builder, Buffer}#append` method.
     * This function can process string constants as well as function calls as argument to append.
     */
    private def valueOfAppendCall(
        call: VirtualFunctionCall[V]
    ): Option[StringConstancyInformation] = {
        val value = exprHandler.processDefSite(call.params.head.asVar.definedBy.head)
        call.params.head.asVar.value.computationalType match {
            // For some types, we know the (dynamic) values
            case ComputationalTypeInt ⇒ Some(StringConstancyInformation(
                StringConstancyLevel.DYNAMIC, StringConstancyInformation.IntValue
            ))
            case ComputationalTypeFloat ⇒ Some(StringConstancyInformation(
                StringConstancyLevel.DYNAMIC, StringConstancyInformation.FloatValue
            ))
            // Otherwise, try to compute
            case _ ⇒
                // It might be necessary to merge the values of the receiver and of the parameter
                value.size match {
                    case 0 ⇒ None
                    case 1 ⇒ value.headOption
                    case _ ⇒ Some(StringConstancyInformation(
                        StringConstancyLevel.determineForConcat(
                            value.head.constancyLevel, value(1).constancyLevel
                        ),
                        value.head.possibleStrings + value(1).possibleStrings
                    ))
                }
        }
    }

    /**
     * Function for processing calls to [[StringBuilder#toString]] or [[StringBuffer#toString]].
     * Note that this function assumes that the given `toString` is such a function call! Otherwise,
     * the expected behavior cannot be guaranteed.
     */
    private def interpretToStringCall(
        call: VirtualFunctionCall[V]
    ): List[StringConstancyInformation] =
        exprHandler.processDefSite(call.receiver.asVar.definedBy.head)

}
