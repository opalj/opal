/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.interpretation

import org.opalj.br.cfg.CFG
import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.fpcf.string_definition.properties.StringConstancyInformation
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts

import scala.collection.mutable.ListBuffer

/**
 * The `NonVirtualMethodCallInterpreter` is responsible for processing [[NonVirtualMethodCall]]s.
 * For supported method calls, see the documentation of the `interpret` function.
 *
 * @see [[AbstractStringInterpreter]]
 *
 * @author Patrick Mell
 */
class NonVirtualMethodCallInterpreter(
        cfg:         CFG[Stmt[V], TACStmts[V]],
        exprHandler: InterpretationHandler
) extends AbstractStringInterpreter(cfg, exprHandler) {

    override type T = NonVirtualMethodCall[V]

    /**
     * Currently, this function supports the interpretation of the following non virtual methods:
     * <ul>
     * <li>
     * `&lt;init&gt;`, when initializing an object (for this case, currently zero constructor or
     * one constructor parameter are supported; if more params are available, only the very first
     * one is interpreted).
     * </li>
     * </ul>
     * For all other calls, an empty list will be returned at the moment.
     *
     * @see [[AbstractStringInterpreter.interpret]]
     */
    override def interpret(instr: NonVirtualMethodCall[V]): List[StringConstancyInformation] = {
        instr.name match {
            case "<init>" ⇒ interpretInit(instr)
            case _        ⇒ List()
        }
    }

    /**
     * Processes an `&lt;init&gt;` method call. If it has no parameters, an empty list will be
     * returned. Otherwise, only the very first parameter will be evaluated and its result returned
     * (this is reasonable as both, [[StringBuffer]] and [[StringBuilder]], have only constructors
     * with <= 0 arguments and only these are currently interpreted).
     */
    private def interpretInit(init: NonVirtualMethodCall[V]): List[StringConstancyInformation] = {
        init.params.size match {
            case 0 ⇒
                List()
            //List(StringConstancyInformation(StringConstancyLevel.CONSTANT, ""))
            case _ ⇒
                val scis = ListBuffer[StringConstancyInformation]()
                init.params.head.asVar.definedBy.foreach { ds ⇒
                    scis.append(exprHandler.processDefSite(ds): _*)
                }
                scis.toList
        }
    }

}
