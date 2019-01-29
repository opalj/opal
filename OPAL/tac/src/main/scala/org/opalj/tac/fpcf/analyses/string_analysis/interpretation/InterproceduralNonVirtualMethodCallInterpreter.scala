/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.interpretation

import scala.collection.mutable.ListBuffer

import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Result
import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.cg.properties.Callees
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts
import org.opalj.tac.fpcf.analyses.string_analysis.V

/**
 * The `InterproceduralNonVirtualMethodCallInterpreter` is responsible for processing
 * [[NonVirtualMethodCall]]s in an interprocedural fashion.
 * For supported method calls, see the documentation of the `interpret` function.
 *
 * @see [[AbstractStringInterpreter]]
 *
 * @author Patrick Mell
 */
class InterproceduralNonVirtualMethodCallInterpreter(
        cfg:         CFG[Stmt[V], TACStmts[V]],
        exprHandler: InterproceduralInterpretationHandler,
        callees:     Callees
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
    override def interpret(instr: NonVirtualMethodCall[V]): ProperPropertyComputationResult = {
        val prop = instr.name match {
            case "<init>" ⇒ interpretInit(instr)
            case _        ⇒ StringConstancyProperty.getNeutralElement
        }
        Result(instr, prop)
    }

    /**
     * Processes an `&lt;init&gt;` method call. If it has no parameters,
     * [[StringConstancyProperty.getNeutralElement]] will be returned. Otherwise, only the very
     * first parameter will be evaluated and its result returned (this is reasonable as both,
     * [[StringBuffer]] and [[StringBuilder]], have only constructors with <= 1 arguments and only
     * these are currently interpreted).
     */
    private def interpretInit(init: NonVirtualMethodCall[V]): StringConstancyProperty = {
        init.params.size match {
            case 0 ⇒ StringConstancyProperty.getNeutralElement
            case _ ⇒
                val scis = ListBuffer[StringConstancyInformation]()
                init.params.head.asVar.definedBy.foreach { ds ⇒
                    val result = exprHandler.processDefSite(ds)
                    scis.append(
                        result.asInstanceOf[StringConstancyProperty].stringConstancyInformation
                    )
                }
                val reduced = StringConstancyInformation.reduceMultiple(scis.toList)
                StringConstancyProperty(reduced)
        }
    }

}
