/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package immutability
package field_assignability

import scala.annotation.switch

import org.opalj.br.Method
import org.opalj.br.PCs
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.BasicFPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.tac.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.EscapeProperty
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.br.analyses.cg.ClosedPackagesKey
import org.opalj.br.analyses.cg.TypeExtensibilityKey
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.FieldAccessInformationKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.fpcf.properties.immutability.FieldAssignability
import org.opalj.br.fpcf.properties.immutability.LazilyInitialized
import org.opalj.br.fpcf.properties.immutability.UnsafelyLazilyInitialized
import org.opalj.tac.common.DefinitionSitesKey

/**
 *
 * Determines the assignability of a field.
 *
 * @note Requires that the 3-address code's expressions are not deeply nested.
 * @author Tobias Roth
 * @author Dominik Helm
 * @author Florian Kübler
 * @author Michael Eichberg
 *
 */
class L2FieldAssignabilityAnalysis private[analyses] (val project: SomeProject)
    extends AbstractFieldAssignabilityAnalysisLazyInitialization
    with AbstractFieldAssignabilityAnalysis
    with FPCFAnalysis {

    val considerLazyInitialization: Boolean =
        project.config.getBoolean(
            "org.opalj.fpcf.analyses.L2FieldAssignabilityAnalysis.considerLazyInitialization"
        )

    /**
     * Analyzes field writes for a single method, returning false if the field may still be
     * effectively final and true otherwise.
     */
    def methodUpdatesField(
        method:  Method,
        taCode:  TACode[TACMethodParameter, V],
        callers: Callers,
        pcs:     PCs
    )(implicit state: AnalysisState): Boolean = {
        val field = state.field
        val stmts = taCode.stmts
        pcs.iterator.exists { pc =>
            val index = taCode.pcToIndex(pc)
            if (index > -1) { //TODO actually, unnecessary but required because there are '-1'
                val stmt = stmts(index)
                if (stmt.pc == pc) {
                    (stmt.astID: @switch) match {
                        case PutStatic.ASTID | PutField.ASTID =>
                            if (method.isInitializer) {
                                if (field.isStatic) {
                                    method.isConstructor
                                } else {
                                    val receiverDefs = stmt.asPutField.objRef.asVar.definedBy
                                    receiverDefs != SelfReferenceParameter
                                }
                            } else {
                                if (field.isStatic ||
                                    stmt.asPutField.objRef.asVar.definedBy == SelfReferenceParameter) {
                                    // We consider lazy initialization if there is only single write
                                    // outside an initializer, so we can ignore synchronization
                                    state.fieldAssignability == LazilyInitialized ||
                                        state.fieldAssignability == UnsafelyLazilyInitialized ||
                                        // A lazily initialized instance field must be initialized only
                                        // by its owning instance
                                        !field.isStatic &&
                                        stmt.asPutField.objRef.asVar.definedBy != SelfReferenceParameter ||
                                        // A field written outside an initializer must be lazily
                                        // initialized or it is assignable
                                        {
                                            if (considerLazyInitialization) {
                                                val result = isAssignable(
                                                    index,
                                                    getDefaultValues(),
                                                    method,
                                                    taCode
                                                )
                                                result
                                            } else
                                                true
                                        }
                                } else if (!referenceHasNotEscaped(stmt.asPutField.objRef.asVar, stmts, method, callers)) {
                                    // Here the clone pattern is determined among others
                                    //
                                    // note that here we assume real three address code (flat hierarchy)

                                    // for instance fields it is okay if they are written in the
                                    // constructor (w.r.t. the currently initialized object!)

                                    // If the field that is written is not the one referred to by the
                                    // self reference, it is not effectively final.

                                    // However, a method (e.g. clone) may instantiate a new object and
                                    // write the field as long as that new object did not yet escape.
                                    true
                                } else {
                                    val writes = fieldAccessInformation.writeAccesses(state.field)
                                    val reads = fieldAccessInformation.readAccesses(state.field)
                                    val writesInMethod = writes.iterator.filter(_._1 eq method).toList.head._2

                                    val fieldWriteInMethodIndex = taCode.pcToIndex(writesInMethod.head)

                                    val assignedValueObject = stmt.asPutField.objRef.asVar

                                    if (assignedValueObject.definedBy.exists(_ < 0))
                                        return true;

                                    if (writesInMethod.size > 1)
                                        return true;

                                    val assignedValueObjectVar =
                                        stmts(assignedValueObject.definedBy.head).asAssignment.targetVar.asVar

                                    if (assignedValueObjectVar != null && !assignedValueObjectVar.usedBy.forall { index =>
                                        val stmt = stmts(index)

                                        // val writeStmt  = stmts(fieldWriteInMethodIndex)
                                        fieldWriteInMethodIndex == index || //The value is itself written to another object
                                            stmt.isPutField && stmt.asPutField.name != state.field.name ||
                                            stmt.isAssignment && stmt.asAssignment.targetVar == assignedValueObjectVar ||
                                            stmt.isMethodCall && stmt.asMethodCall.name == "<init>" ||
                                            dominates(fieldWriteInMethodIndex, index, taCode)
                                    })
                                        return true;

                                    val fieldReadsInMethod = reads.iterator.filter(_._1 eq method).map(_._2).toList
                                    if (fieldReadsInMethod.size > 1 && !fieldReadsInMethod.head.forall { pc =>
                                        val index = taCode.pcToIndex(pc)
                                        fieldWriteInMethodIndex == index ||
                                            dominates(fieldWriteInMethodIndex, index, taCode)
                                    })
                                        return true;
                                    false
                                }

                            }
                        case _ => throw new RuntimeException("unexpected field access");
                    }
                } else {
                    // nothing to do as the put field is dead
                    false
                }
            } else false
        }
    }
}

trait L2FieldAssignabilityAnalysisScheduler extends FPCFAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys = Seq(
        DeclaredMethodsKey,
        FieldAccessInformationKey,
        ClosedPackagesKey,
        TypeExtensibilityKey,
        DefinitionSitesKey
    )

    final override def uses: Set[PropertyBounds] = Set(
        PropertyBounds.ub(TACAI),
        PropertyBounds.ub(EscapeProperty),
        PropertyBounds.ub(FieldAssignability),
        PropertyBounds.ub(Callers)
    )

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(FieldAssignability)
}

/**
 * Executor for the eager field assignability analysis.
 */
object EagerL2FieldAssignabilityAnalysis
    extends L2FieldAssignabilityAnalysisScheduler
    with BasicFPCFEagerAnalysisScheduler {

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    final override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new L2FieldAssignabilityAnalysis(p)
        val fields = p.allFields
        ps.scheduleEagerComputationsForEntities(fields)(analysis.determineFieldAssignability)
        analysis
    }
}

/**
 * Executor for the lazy field assignability analysis.
 */
object LazyL2FieldAssignabilityAnalysis
    extends L2FieldAssignabilityAnalysisScheduler
    with BasicFPCFLazyAnalysisScheduler {

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    final override def register(
        p:      SomeProject,
        ps:     PropertyStore,
        unused: Null
    ): FPCFAnalysis = {
        val analysis = new L2FieldAssignabilityAnalysis(p)
        ps.registerLazyPropertyComputation(
            FieldAssignability.key,
            analysis.determineFieldAssignability
        )
        analysis
    }
}
