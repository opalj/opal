/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package fieldassignability

import org.opalj.br.DefinedMethod
import org.opalj.br.Field
import org.opalj.br.PC
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.BasicFPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.fieldaccess.AccessReceiver
import org.opalj.br.fpcf.properties.immutability.Assignable
import org.opalj.br.fpcf.properties.immutability.FieldAssignability
import org.opalj.br.fpcf.properties.immutability.NonAssignable
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.fpcf.analyses.cg.uVarForDefSites

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

    override def determineAssignabilityFromWriteInContext(
        context: Context,
        definedMethod: DefinedMethod,
        taCode: TACode[TACMethodParameter, V],
        writePC: PC,
        receiver: AccessReceiver
    )(implicit state: AnalysisState): FieldAssignability = {
        val field = state.field
        val method = definedMethod.definedMethod

        if (field.isStatic && method.isConstructor) {
            // A static field updated in an arbitrary constructor may be updated with (at least) the first call.
            // Thus, we may see its initial value or the updated value, making the field assignable.
            return Assignable;
        }

        if (state.fieldAccesses(context).size > 1) {
            // Multi-branch access detection is not available on this level.
            return Assignable;
        }

        val receiverVarOpt = receiver.map(uVarForDefSites(_, taCode.pcToIndex))
        if (receiverVarOpt.isDefined) {
            val receiverVar = receiverVarOpt.get
            if (method.isConstructor && receiverVar.definedBy == SelfReferenceParameter) {
                // for instance fields it is okay if they are written in the
                // constructor (w.r.t. the currently initialized object!)
                NonAssignable
            } else if (!referenceHasEscaped(receiverVar, taCode.stmts, definedMethod, context)) {
                // A method (e.g. clone) may instantiate a new object and write the field as long as that new object
                // did not yet escape.
                NonAssignable
            } else {
                Assignable
            }
        } else if (!method.isStaticInitializer) {
            Assignable
        } else {
            NonAssignable
        }
    }
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
            FieldAssignability.key,
            analysis.doDetermineFieldAssignability
        )
        analysis
    }
}
