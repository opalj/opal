/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.fieldassignability

import org.opalj.br.DefinedMethod
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.BasicFPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.PutField
import org.opalj.tac.PutStatic
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.SelfReferenceParameter
import org.opalj.br.Field
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.immutability.FieldAssignability
import org.opalj.br.fpcf.ContextProviderKey

/**
 * Simple analysis that checks if a private (static or instance) field is always initialized at
 * most once or if a field is or can be mutated after (lazy) initialization.
 *
 * @note Requires that the 3-address code's expressions are not deeply nested.
 * @author Tobias Roth
 * @author Dominik Helm
 * @author Florian Kübler
 * @author Michael Eichberg
 */
class L1FieldAssignabilityAnalysis private[analyses] (val project: SomeProject)
    extends AbstractFieldAssignabilityAnalysis {

    case class State(field: Field) extends AbstractFieldAssignabilityAnalysisState
    type AnalysisState = State
    override def createState(field: Field): AnalysisState = State(field)

    /**
     * Analyzes field writes for a single method, returning false if the field may still be
     * effectively final and true otherwise.
     */
    def methodUpdatesField(
        definedMethod: DefinedMethod,
        taCode:        TACode[TACMethodParameter, V],
        callers:       Callers,
        pc:            PC
    )(implicit state: AnalysisState): Boolean = {
        val stmts = taCode.stmts
        val method = definedMethod.definedMethod

        val index = taCode.properStmtIndexForPC(pc)
        if (index >= 0) {
            val stmtCandidate = stmts(index)
            if (stmtCandidate.pc == pc) {
                stmtCandidate match {
                    case _: PutStatic[_] =>
                        if (!method.isStaticInitializer)
                            return true;
                    case stmt: PutField[V] =>
                        val objRef = stmt.objRef.asVar
                        if ((!method.isConstructor ||
                            objRef.definedBy != SelfReferenceParameter) &&
                            !referenceHasNotEscaped(objRef, stmts, definedMethod, callers)) {
                            // note that here we assume real three address code (flat hierarchy)

                            // for instance fields it is okay if they are written in the
                            // constructor (w.r.t. the currently initialized object!)

                            // If the field that is written is not the one referred to by the
                            // self reference, it is not effectively final.

                            // However, a method (e.g. clone) may instantiate a new object and
                            // write the field as long as that new object did not yet escape.
                            return true;
                        }
                    case _ => throw new RuntimeException("unexpected field access");
                }
            } else { // nothing to do as the put field is dead
            }
        }
        false
    }
}

sealed trait L1FieldAssignabilityAnalysisScheduler extends FPCFAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys = Seq(
        TypeExtensibilityKey,
        ClosedPackagesKey,
        FieldAccessInformationKey,
        DefinitionSitesKey,
        ContextProviderKey
    )

    final override def uses: Set[PropertyBounds] = PropertyBounds.lubs(TACAI, EscapeProperty)

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(FieldAssignability)
}

/**
 * Executor for the eager field assignability analysis.
 */
object EagerL1FieldAssignabilityAnalysis
    extends AbstractFieldAssignabilityAnalysisScheduler
    with BasicFPCFEagerAnalysisScheduler {

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new L1FieldAssignabilityAnalysis(p)
        val fields = p.allFields
        ps.scheduleEagerComputationsForEntities(fields)(analysis.determineFieldAssignability)
        analysis
    }
}

/**
 * Executor for the lazy field assignability analysis.
 */
object LazyL1FieldAssignabilityAnalysis
    extends AbstractFieldAssignabilityAnalysisScheduler
    with BasicFPCFLazyAnalysisScheduler {

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def register(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new L1FieldAssignabilityAnalysis(p)
        ps.registerLazyPropertyComputation(
            FieldAssignability.key, analysis.doDetermineFieldAssignability
        )
        analysis
    }
}
