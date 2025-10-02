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
import org.opalj.br.fpcf.properties.immutability.FieldAssignability
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

    /**
     * Returns true when the method in the given context definitely updates the field in a way that forces it to be
     * assignable, and false when it does not, or we are not yet sure.
     *
     * @note Callers must pass ALL write accesses of this method in this context discovered so far.
     */
    override def doesMethodUpdateFieldInContext(
        context: Context,
        definedMethod: DefinedMethod,
        taCode: TACode[TACMethodParameter, V],
        writeAccesses: Iterable[(PC, AccessReceiver)],
    )(implicit state: AnalysisState): Boolean = {
        val method = definedMethod.definedMethod
        writeAccesses.exists { access =>
            val receiverVarOpt = access._2.map(uVarForDefSites(_, taCode.pcToIndex))
            if (receiverVarOpt.isDefined) {
                val receiverVar = receiverVarOpt.get
                // note that here we assume real three address code (flat hierarchy)

                // for instance fields it is okay if they are written in the
                // constructor (w.r.t. the currently initialized object!)

                // If the field that is written is not the one referred to by the
                // self reference, it is not effectively final.

                // However, a method (e.g. clone) may instantiate a new object and
                // write the field as long as that new object did not yet escape.
                (!method.isConstructor || receiverVar.definedBy != SelfReferenceParameter) &&
                    referenceHasEscaped(receiverVar, taCode.stmts, definedMethod, context)
            } else {
                !method.isStaticInitializer
            }
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
