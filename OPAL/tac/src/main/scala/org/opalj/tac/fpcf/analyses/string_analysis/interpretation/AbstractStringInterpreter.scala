/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.interpretation

import scala.collection.mutable.ListBuffer

import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.br.cfg.CFG
import org.opalj.br.Method
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.DefinedMethod
import org.opalj.br.ReferenceType
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts
import org.opalj.tac.fpcf.analyses.string_analysis.V
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.fpcf.analyses.string_analysis.ComputationState
import org.opalj.tac.fpcf.properties.TACAI

/**
 * @param cfg The control flow graph that underlies the instruction to interpret.
 * @param exprHandler In order to interpret an instruction, it might be necessary to interpret
 *                    another instruction in the first place. `exprHandler` makes this possible.
 *
 * @note The abstract type [[InterpretationHandler]] allows the handling of different styles (e.g.,
 *       intraprocedural and interprocedural). Thus, implementation of this class are required to
 *       clearly indicate what kind of [[InterpretationHandler]] they expect in order to ensure the
 *       desired behavior and not confuse developers.
 *
 * @author Patrick Mell
 */
abstract class AbstractStringInterpreter(
        protected val cfg:         CFG[Stmt[V], TACStmts[V]],
        protected val exprHandler: InterpretationHandler
) {

    type T <: Any

    /**
     * Either returns the TAC for the given method or otherwise registers dependees.
     *
     * @param ps The property store to use.
     * @param m The method to get the TAC for.
     * @param s The computation state whose dependees might be extended in case the TAC is not
     *          immediately ready.
     * @return Returns `Some(tac)` if the TAC is already available or `None` otherwise.
     */
    protected def getTACAI(
        ps: PropertyStore,
        m:  Method,
        s:  ComputationState
    ): Option[TACode[TACMethodParameter, V]] = {
        val tacai = ps(m, TACAI.key)
        if (tacai.hasUBP) {
            tacai.ub.tac
        } else {
            if (!s.dependees.contains(m)) {
                s.dependees(m) = ListBuffer()
            }
            s.dependees(m).append(tacai)
            None
        }
    }

    /**
     * Takes `declaredMethods`, a `declaringClass`, and a method `name` and extracts the method with
     * the given `name` from `declaredMethods` where the declaring classes match. The found entry is
     * then returned as a [[Method]] instance.
     * <p>
     * It might be, that the given method cannot be found (e.g., when it is not in the scope of the
     * current project). In these cases, `None` will be returned.
     */
    protected def getDeclaredMethod(
        declaredMethods: DeclaredMethods, declaringClass: ReferenceType, methodName: String
    ): Option[Method] = {
        val dm = declaredMethods.declaredMethods.find { dm ⇒
            dm.name == methodName && dm.declaringClassType == declaringClass
        }
        if (dm.isDefined && dm.get.isInstanceOf[DefinedMethod]) {
            Some(dm.get.definedMethod)
        } else {
            None
        }
    }

    /**
     *
     * @param instr The instruction that is to be interpreted. It is the responsibility of
     *              implementations to make sure that an instruction is properly and comprehensively
     *              evaluated.
     * @param defSite The definition site that corresponds to the given instruction. `defSite` is
     *                not necessary for processing `instr`, however, may be used, e.g., for
     *                housekeeping purposes. Thus, concrete implementations should indicate whether
     *                this value is of importance for (further) processing.
     * @return The interpreted instruction. A neutral StringConstancyProperty contained in the
     *         result indicates that an instruction was not / could not be interpreted (e.g.,
     *         because it is not supported or it was processed before).
     *         <p>
     *         As demanded by [[InterpretationHandler]], the entity of the result should be the
     *         definition site. However, as interpreters know the instruction to interpret but not
     *         the definition site, this function returns the interpreted instruction as entity.
     *         Thus, the entity needs to be replaced by the calling client.
     */
    def interpret(instr: T, defSite: Int): ProperPropertyComputationResult

}
