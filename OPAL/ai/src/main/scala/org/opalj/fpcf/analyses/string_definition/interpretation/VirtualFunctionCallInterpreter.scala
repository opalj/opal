/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.interpretation

import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts
import org.opalj.br.cfg.CFG
import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.fpcf.string_definition.properties.StringConstancyInformation
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
    ): List[StringConstancyInformation] =
        exprHandler.processDefSite(appendCall.params.head.asVar.definedBy.head)

    /**
     * Function for processing calls to [[StringBuilder#toString]] or [[StringBuffer#toString]].
     * Note that this function assumes that the given `toString` is such a function call! Otherwise,
     * the expected behavior cannot be guaranteed.
     */
    private def interpretToStringCall(
        appendCall: VirtualFunctionCall[V]
    ): List[StringConstancyInformation] = List()

}
