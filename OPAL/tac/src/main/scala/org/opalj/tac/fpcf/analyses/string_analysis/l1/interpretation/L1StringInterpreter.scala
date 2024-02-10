/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l1
package interpretation

import scala.collection.mutable.ListBuffer

import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.StringInterpreter

/**
 * @author Maximilian Rüsch
 */
trait L1StringInterpreter[State <: ComputationState[State]] extends StringInterpreter[State] {

    /**
     * @param instr The instruction that is to be interpreted. It is the responsibility of implementations to make sure
     *              that an instruction is properly and comprehensively evaluated.
     * @param defSite The definition site that corresponds to the given instruction. `defSite` is
     *                not necessary for processing `instr`, however, may be used, e.g., for
     *                housekeeping purposes. Thus, concrete implementations should indicate whether
     *                this value is of importance for (further) processing.
     * @return The interpreted instruction. A neutral StringConstancyProperty contained in the
     *         result indicates that an instruction was not / could not be interpreted (e.g.,
     *         because it is not supported or it was processed before).
     *         <p>
     *         As demanded by [[org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler]], the
     *         entity of the result should be the definition site. However, as interpreters know the instruction to
     *         interpret but not the definition site, this function returns the interpreted instruction as entity.
     *         Thus, the entity needs to be replaced by the calling client.
     */
    def interpret(instr: T, defSite: Int)(implicit state: State): EOptionP[Entity, StringConstancyProperty]

    /**
     * This function returns all methods for a given `pc` among a set of `declaredMethods`. The
     * second return value indicates whether at least one method has an unknown body (if `true`,
     * then there is such a method).
     */
    protected def getMethodsForPC(context: Context, pc: Int)(
        implicit
        ps:              PropertyStore,
        callees:         Callees,
        contextProvider: ContextProvider
    ): (List[Method], Boolean) = {
        var hasMethodWithUnknownBody = false
        val methods = ListBuffer[Method]()

        callees.callees(context, pc)(ps, contextProvider).map(_.method).foreach {
            case definedMethod: DefinedMethod => methods.append(definedMethod.definedMethod)
            case _                            => hasMethodWithUnknownBody = true
        }

        (methods.sortBy(_.classFile.fqn).toList, hasMethodWithUnknownBody)
    }
}
