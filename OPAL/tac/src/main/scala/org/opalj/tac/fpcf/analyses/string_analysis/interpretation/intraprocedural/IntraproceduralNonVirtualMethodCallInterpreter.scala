/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.interpretation.intraprocedural

import scala.collection.mutable.ListBuffer

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalEP
import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts
import org.opalj.tac.fpcf.analyses.string_analysis.V
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.AbstractStringInterpreter

/**
 * The `IntraproceduralNonVirtualMethodCallInterpreter` is responsible for processing
 * [[NonVirtualMethodCall]]s in an intraprocedural fashion.
 * For supported method calls, see the documentation of the `interpret` function.
 *
 * @see [[AbstractStringInterpreter]]
 *
 * @author Patrick Mell
 */
class IntraproceduralNonVirtualMethodCallInterpreter(
        cfg:         CFG[Stmt[V], TACStmts[V]],
        exprHandler: IntraproceduralInterpretationHandler
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
     *
     * For all other calls, a result containing [[StringConstancyProperty.getNeutralElement]] will
     * be returned.
     *
     * @note For this implementation, `defSite` does not play a role.
     *
     * @see [[AbstractStringInterpreter.interpret]]
     */
    override def interpret(
        instr: NonVirtualMethodCall[V], defSite: Int
    ): EOptionP[Entity, StringConstancyProperty] = {
        val prop = instr.name match {
            case "<init>" ⇒ interpretInit(instr)
            case _        ⇒ StringConstancyProperty.getNeutralElement
        }
        FinalEP(instr, prop)
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
                    val r = exprHandler.processDefSite(ds).asFinal
                    scis.append(
                        r.p.asInstanceOf[StringConstancyProperty].stringConstancyInformation
                    )
                }
                val reduced = StringConstancyInformation.reduceMultiple(scis)
                StringConstancyProperty(reduced)
        }
    }

}
