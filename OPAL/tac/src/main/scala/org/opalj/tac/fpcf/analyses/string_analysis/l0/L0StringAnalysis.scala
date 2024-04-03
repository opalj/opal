/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l0

import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.analyses.string_analysis.l0.interpretation.L0InterpretationHandler
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.SimplePathFinder
import org.opalj.tac.fpcf.properties.TACAI

/**
 * IntraproceduralStringAnalysis processes a read operation of a local string variable at a program
 * position, ''pp'', in a way that it finds the set of possible strings that can be read at ''pp''.
 * <p>
 * This analysis takes into account only the enclosing function as a context, i.e., it is
 * intraprocedural. Values coming from other functions are regarded as dynamic values even if the
 * function returns a constant string value.
 * <p>
 * From a high-level perspective, this analysis works as follows. First, it has to be differentiated
 * whether string literals / variables or String{Buffer, Builder} are to be processed.
 * For the former, the definition sites are processed. Only one definition site is the trivial case
 * and directly corresponds to a leaf node in the string tree (such trees consist of only one node).
 * Multiple definition sites indicate > 1 possible initialization values and are transformed into a
 * string tree whose root node is an OR element and the children are the possible initialization
 * values. Note that all this is handled by
 * [[org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation.reduceMultiple]].
 * <p>
 * For the latter, String{Buffer, Builder}, lean paths from the definition sites to the usage
 * (indicated by the given DUVar) is computed. That is, all paths from all definition sites to the
 * usage where only statements are contained that include the String{Builder, Buffer} object of
 * interest in some way (like an "append" or "replace" operation for example). These paths are then
 * transformed into a string tree by making use of a
 * [[org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.PathTransformer]].
 *
 * @author Patrick Mell
 */
class L0StringAnalysis(override val project: SomeProject) extends StringAnalysis {

    override def analyze(data: SContext): ProperPropertyComputationResult = {
        val state = ComputationState(declaredMethods(data._2), data)
        val iHandler = L0InterpretationHandler()

        val tacaiEOptP = ps(data._2, TACAI.key)
        if (tacaiEOptP.isRefinable) {
            state.tacDependee = Some(tacaiEOptP)
            return getInterimResult(state, iHandler)
        }

        if (tacaiEOptP.ub.tac.isEmpty) {
            // No TAC available, e.g., because the method has no body
            return Result(state.entity, StringConstancyProperty.lb)
        }

        state.tac = tacaiEOptP.ub.tac.get
        determinePossibleStrings(state, iHandler)
    }

    override protected[string_analysis] def determinePossibleStrings(implicit
        state:    ComputationState,
        iHandler: InterpretationHandler
    ): ProperPropertyComputationResult = {
        implicit val tac: TAC = state.tac

        if (SimplePathFinder.containsComplexControlFlow(tac)) {
            return Result(state.entity, StringConstancyProperty.lb)
        }

        val puVar = state.entity._1
        val uVar = puVar.toValueOriginForm(tac.pcToIndex)
        val defSites = uVar.definedBy.toArray.sorted

        if (defSites.exists(_ < 0)) {
            if (InterpretationHandler.isStringConstExpression(uVar)) {
                // We can evaluate string const expressions as function parameters
            } else if (StringAnalysis.isSupportedPrimitiveNumberType(uVar)) {
                val numType = uVar.value.asPrimitiveValue.primitiveType.toJava
                val sci = StringAnalysis.getDynamicStringInformationForNumberType(numType)
                return Result(state.entity, StringConstancyProperty(sci))
            } else {
                // StringBuilders as parameters are currently not evaluated
                return Result(state.entity, StringConstancyProperty.lb)
            }
        }

        // Interpret a function / method parameter using the parameter information in state
        if (defSites.head < 0) {
            val ep = ps(
                InterpretationHandler.getEntityForDefSite(defSites.head, state.dm, state.tac),
                StringConstancyProperty.key
            )
            if (ep.isRefinable) {
                state.dependees = ep :: state.dependees
                return InterimResult.forUB(
                    state.entity,
                    StringConstancyProperty.ub,
                    state.dependees.toSet,
                    continuation(state, iHandler)
                )
            } else {
                return Result(state.entity, StringConstancyProperty(ep.asFinal.p.sci))
            }
        }

        if (state.computedLeanPath == null) {
            state.computedLeanPath = computeLeanPath(uVar)
        }

        val expr = tac.stmts(defSites.head).asAssignment.expr
        if (InterpretationHandler.isStringBuilderBufferToStringCall(expr)) {
            // Find DUVars that the analysis of the current entity depends on
            findDependentVars(state.computedLeanPath, puVar).keys.foreach { nextVar =>
                propertyStore((nextVar, state.entity._2), StringConstancyProperty.key) match {
                    case FinalEP(e, _) =>
                        state.dependees = state.dependees.filter(_.e != e)
                    case ep =>
                        state.dependees = ep :: state.dependees
                }
            }
        }

        getPCsInPath(state.computedLeanPath).foreach { pc =>
            propertyStore(
                InterpretationHandler.getEntityForPC(pc, state.dm, state.tac),
                StringConstancyProperty.key
            ) match {
                case FinalEP(e, _) =>
                    state.dependees = state.dependees.filter(_.e != e)
                case ep =>
                    state.dependees = ep :: state.dependees
            }
        }

        if (state.dependees.isEmpty) {
            computeFinalResult(state)
        } else {
            getInterimResult(state, iHandler)
        }
    }
}

object LazyL0StringAnalysis extends LazyStringAnalysis {

    override def init(p: SomeProject, ps: PropertyStore): InitializationData =
        (new L0StringAnalysis(p), L0InterpretationHandler()(p, ps))
}
