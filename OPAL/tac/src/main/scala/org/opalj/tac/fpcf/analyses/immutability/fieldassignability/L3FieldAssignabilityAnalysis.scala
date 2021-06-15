/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package immutability
package fieldassignability

import scala.annotation.switch

import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.br.PCs
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.BasicFPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.properties.AtMost
//import org.opalj.br.fpcf.properties.EscapeInCallee
import org.opalj.br.fpcf.properties.EscapeProperty
import org.opalj.br.fpcf.properties.EscapeViaReturn
import org.opalj.br.fpcf.properties.FieldPrematurelyRead
import org.opalj.br.fpcf.properties.LazilyInitialized
import org.opalj.br.fpcf.properties.Assignable
import org.opalj.br.fpcf.properties.NoEscape
import org.opalj.br.fpcf.properties.FieldAssignability
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimEP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.tac.common.DefinitionSite
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.br.ObjectType
import org.opalj.br.fpcf.properties.FieldImmutability
import org.opalj.br.analyses.cg.ClosedPackagesKey
import org.opalj.br.analyses.cg.TypeExtensibilityKey
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.FieldAccessInformationKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.br.fpcf.properties.NonAssignable
import org.opalj.br.fpcf.properties.UnsafelyLazilyInitialized

/**
 *
 * Determines the assignability of a field.
 *
 * @note Requires that the 3-address code's expressions are not deeply nested.
 *
 * @author Tobias Roth
 * @author Dominik Helm
 * @author Florian Kübler
 * @author Michael Eichberg
 *
 */
class L3FieldAssignabilityAnalysis private[analyses] (val project: SomeProject)
    extends AbstractFieldAssignabilityAnalysisLazyInitialization
    with AbstractFieldAssignabilityAnalysis
    with FPCFAnalysis {

    val considerLazyInitialization: Boolean =
        project.config.getBoolean(
            "org.opalj.fpcf.analyses.L3FieldAssignabilityAnalysis.considerLazyInitialization"
        )

    def doDetermineFieldAssignability(entity: Entity): PropertyComputationResult = {
        entity match {

            case field: Field ⇒
                determineFieldAssignability(field)

            case _ ⇒
                val m = entity.getClass.getSimpleName+" is not an org.opalj.br.Field"
                throw new IllegalArgumentException(m)
        }
    }

    /**
     * Analyzes the field's assignability.
     *
     * This analysis is only ''soundy'' if the class file does not contain native methods.
     * Native methods are omitted.
     */
    private[analyses] def determineFieldAssignability(
        field: Field
    ): ProperPropertyComputationResult = {

        if (field.isFinal)
            return Result(field, NonAssignable);

        if (field.isPublic)
            return Result(field, Assignable);

        implicit val state: State = State(field)

        val thisType = field.classFile.thisType

        // Fields are not final if they are read prematurely!
        if (isPrematurelyRead(propertyStore(field, FieldPrematurelyRead.key)))
            return Result(field, Assignable);

        if (field.isPublic) {
            if (typeExtensibility(ObjectType.Object).isYesOrUnknown) {
                return Result(field, Assignable);
            }
        } else if (field.isProtected) {
            if (typeExtensibility(thisType).isYesOrUnknown) {
                return Result(field, Assignable);
            }
            if (!closedPackages(thisType.packageName)) {
                return Result(field, Assignable);
            }
        }
        if (field.isPackagePrivate) {
            if (!closedPackages(thisType.packageName)) {
                return Result(field, Assignable);
            }
        }

        for {
            (method, pcs) ← fieldAccessInformation.writeAccesses(field)
            taCode ← getTACAI(method, pcs) //TODO field accesses via this
        } {
            val result = methodUpdatesField(method, taCode, pcs)
            if (result)
                return Result(field, Assignable);
        }
        createResult()
    }

    /**
     * Prepares the PropertyComputation result, either as IntermediateResult if there are still
     * dependees or as Result otherwise.
     */
    def createResult()(implicit state: State): ProperPropertyComputationResult = {

        if (state.hasDependees && (state.fieldAssignability ne Assignable)) {
            InterimResult(
                state.field,
                Assignable,
                state.fieldAssignability,
                state.dependees,
                c
            )
        } else {
            Result(state.field, state.fieldAssignability)
        }
    }

    /**
     * Continuation function handling updates to the FieldPrematurelyRead property or to the purity
     * property of the method that initializes a (potentially) lazy initialized field. //TODO change comment
     */
    def c(eps: SomeEPS)(implicit state: State): ProperPropertyComputationResult = {

        val isNotFinal = eps.pk match {

            case EscapeProperty.key ⇒
                val newEP = eps.asInstanceOf[EOptionP[DefinitionSite, EscapeProperty]]
                state.escapeDependees = state.escapeDependees.iterator.filter(_.e ne newEP.e).toSet
                handleEscapeProperty(newEP)

            case TACAI.key ⇒
                val newEP = eps.asInstanceOf[EOptionP[Method, TACAI]]
                val method = newEP.e
                val pcs = state.tacDependees(method)._2
                state.tacDependees -= method
                if (eps.isRefinable)
                    state.tacDependees += method -> ((newEP, pcs))
                methodUpdatesField(method, newEP.ub.tac.get, pcs)

            case FieldPrematurelyRead.key ⇒ isPrematurelyRead(eps.asInstanceOf[EOptionP[Field, FieldPrematurelyRead]])
        }

        if (isNotFinal)
            state.fieldAssignability = Assignable
        createResult()
    }

    /**
     * Analyzes field writes for a single method, returning false if the field may still be
     * effectively final and true otherwise.
     */
    def methodUpdatesField(
        method: Method,
        taCode: TACode[TACMethodParameter, V],
        pcs:    PCs
    )(implicit state: State): Boolean = {
        val field = state.field
        val stmts = taCode.stmts

        pcs.iterator.exists { pc ⇒
            val index = taCode.pcToIndex(pc)
            if (index > -1) { //TODO actually, unnecessary but required because there are '-1'; dead
                val stmt = stmts(index)
                if (stmt.pc == pc) {
                    (stmt.astID: @switch) match {
                        case PutStatic.ASTID | PutField.ASTID ⇒
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
                                } else if (referenceHasEscaped(stmt.asPutField.objRef.asVar, stmts, method)) {
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

                                    val assignedValueObjectVar = stmts(assignedValueObject.definedBy.head).asAssignment.targetVar.asVar

                                    if (assignedValueObjectVar != null && !assignedValueObjectVar.usedBy.forall { index ⇒
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

                                    if (fieldReadsInMethod.size > 1 && !fieldReadsInMethod.head.forall { pc ⇒
                                        val index = taCode.pcToIndex(pc)
                                        fieldWriteInMethodIndex == index ||
                                            dominates(fieldWriteInMethodIndex, index, taCode)
                                    })
                                        return true;
                                    false
                                }

                            }
                        case _ ⇒ throw new RuntimeException("unexpected field access");
                    }
                } else {
                    // nothing to do as the put field is dead
                    false
                }
            } else false
        }
    }

    /**
     *
     * Checks whether the object reference of a PutField does escape (except for being returned).
     */
    def referenceHasEscaped(
        ref:    V,
        stmts:  Array[Stmt[V]],
        method: Method
    )(implicit state: State): Boolean = {
        ref.definedBy.forall { defSite ⇒
            if (defSite < 0) true
            else { // Must be locally created
                val definition = stmts(defSite).asAssignment
                // Must either be null or freshly allocated
                if (definition.expr.isNullExpr) false
                else if (!definition.expr.isNew) true
                else {
                    val escapeProperty = propertyStore(definitionSites(method, definition.pc), EscapeProperty.key)
                    handleEscapeProperty(escapeProperty)
                }
            }
        }
    }

    /**
     * Handles the influence of an escape property on the field mutability.
     * @return true if the object - on which a field write occurred - escapes, false otherwise.
     * @note (Re-)Adds dependees as necessary.
     */
    def handleEscapeProperty(
        ep: EOptionP[DefinitionSite, EscapeProperty]
    )(implicit state: State): Boolean = {
        import org.opalj.br.fpcf.properties.EscapeInCallee
        ep match {
            case FinalP(NoEscape | EscapeViaReturn | EscapeInCallee) ⇒ false //
            case FinalP(AtMost(_))                                   ⇒ true
            case _: FinalEP[DefinitionSite, EscapeProperty] ⇒
                true // Escape state is worse than via return

            case InterimUBP(NoEscape | EscapeViaReturn | EscapeInCallee) ⇒ //
                state.escapeDependees += ep
                false

            case InterimUBP(AtMost(_)) ⇒ true
            case _: InterimEP[DefinitionSite, EscapeProperty] ⇒
                true // Escape state is worse than via return

            case _ ⇒
                state.escapeDependees += ep
                false
        }
    }

}

trait L3FieldAssignabilityAnalysisScheduler extends FPCFAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys = Seq(
        DeclaredMethodsKey,
        FieldAccessInformationKey,
        ClosedPackagesKey,
        TypeExtensibilityKey,
        DefinitionSitesKey
    )

    final override def uses: Set[PropertyBounds] = Set(
        PropertyBounds.lub(FieldPrematurelyRead),
        PropertyBounds.ub(TACAI),
        PropertyBounds.ub(EscapeProperty),
        PropertyBounds.ub(FieldAssignability),
        PropertyBounds.ub(FieldImmutability)
    )

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(FieldAssignability)
}

/**
 * Executor for the eager field assignability analysis.
 */
object EagerL3FieldAssignabilityAnalysis
    extends L3FieldAssignabilityAnalysisScheduler
    with BasicFPCFEagerAnalysisScheduler {

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    final override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new L3FieldAssignabilityAnalysis(p)
        val fields = p.allFields // p.allProjectClassFiles.flatMap(_.fields) // p.allFields
        ps.scheduleEagerComputationsForEntities(fields)(analysis.determineFieldAssignability)
        analysis
    }
}

/**
 * Executor for the lazy field assignability analysis.
 */
object LazyL3FieldAssignabilityAnalysis
    extends L3FieldAssignabilityAnalysisScheduler
    with BasicFPCFLazyAnalysisScheduler {

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    final override def register(
        p:      SomeProject,
        ps:     PropertyStore,
        unused: Null
    ): FPCFAnalysis = {
        val analysis = new L3FieldAssignabilityAnalysis(p)
        ps.registerLazyPropertyComputation(
            FieldAssignability.key,
            analysis.determineFieldAssignability
        )
        analysis
    }
}
