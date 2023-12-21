/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package fieldassignability

import org.opalj.br.DeclaredField
import org.opalj.br.Field
import org.opalj.br.analyses.DeclaredFields
import org.opalj.br.analyses.DeclaredFieldsKey
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.BasicFPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.ContextProviderKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.properties.fieldaccess.FieldWriteAccessInformation
import org.opalj.br.fpcf.properties.immutability.Assignable
import org.opalj.br.fpcf.properties.immutability.EffectivelyNonAssignable
import org.opalj.br.fpcf.properties.immutability.FieldAssignability
import org.opalj.br.fpcf.properties.immutability.NonAssignable
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP

/**
 * Determines if a private, static, non-final field is always initialized at most once or
 * if a field is or can be mutated after (lazy) initialization. Field read and writes at
 * initialization time (e.g., if the current class object is registered in some publicly
 * available data-store) are not considered. This is in-line with the semantics of final,
 * which also does not prevent reads of partially initialized objects.
 */
class L0FieldAssignabilityAnalysis private[analyses] (val project: SomeProject) extends FPCFAnalysis {

    final val declaredFields: DeclaredFields = project.get(DeclaredFieldsKey)
    final val contextProvider: ContextProvider = project.get(ContextProviderKey)
    implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    case class L0FieldAssignabilityAnalysisState(field: Field) {
        var fieldAssignability: FieldAssignability = EffectivelyNonAssignable // Assume this as the optimistic default
        var latestFieldWriteAccessInformation: Option[EOptionP[DeclaredField, FieldWriteAccessInformation]] = None
        def hasDependees: Boolean = latestFieldWriteAccessInformation.exists(_.isRefinable)
        def dependees: Set[SomeEOptionP] = latestFieldWriteAccessInformation.filter(_.isRefinable).toSet
    }
    type AnalysisState = L0FieldAssignabilityAnalysisState

    /**
     * Invoked for in the lazy computation case.
     * Final fields are considered [[org.opalj.br.fpcf.properties.immutability.NonAssignable]], non-final and
     * non-private fields or fields of library classes whose method bodies are not available are
     * considered [[org.opalj.br.fpcf.properties.immutability.Assignable]].
     * For all other cases the call is delegated to [[determineFieldAssignability]].
     */
    def determineFieldAssignabilityLazy(e: Entity): ProperPropertyComputationResult = {
        e match {
            case field: Field => determineFieldAssignability(field)
            case _            => throw new IllegalArgumentException(s"$e is not a Field")
        }
    }

    /**
     * Analyzes the mutability of private static non-final fields.
     *
     * This analysis is only ''defined and soundy'' if the class file does not contain native
     * methods and the method body of all non-abstract methods is available.
     * (If the analysis is scheduled using its companion object, all class files with
     * native methods are filtered.)
     *
     * @param field A field without native methods and where the method body of all
     *                  non-abstract methods is available.
     */
    def determineFieldAssignability(field: Field): ProperPropertyComputationResult = {
        implicit val state: L0FieldAssignabilityAnalysisState = L0FieldAssignabilityAnalysisState(field)

        if (field.isFinal)
            return Result(field, NonAssignable);

        if (!field.isPrivate)
            return Result(field, Assignable);

        if (!field.isStatic)
            return Result(field, Assignable);

        if (field.classFile.methods.exists(_.isNative))
            return Result(field, Assignable);

        val faiEP = propertyStore(declaredFields(field), FieldWriteAccessInformation.key)
        if (handleFieldWriteAccessInformation(faiEP))
            return Result(field, Assignable)

        createResult()
    }

    /**
     * Processes the given field access information to evaluate if the given field is written statically in the method at
     * the given PCs. Updates the state to account for the new value.
     */
    def handleFieldWriteAccessInformation(
        faiEP: EOptionP[DeclaredField, FieldWriteAccessInformation]
    )(implicit state: AnalysisState): Boolean = {
        val assignable = if (faiEP.hasUBP) {
            val (seenDirectAccesses, seenIndirectAccesses) = state.latestFieldWriteAccessInformation match {
                case Some(UBP(fai)) => (fai.numDirectAccesses, fai.numIndirectAccesses)
                case _              => (0, 0)
            }

            faiEP.ub.getNewestAccesses(
                faiEP.ub.numDirectAccesses - seenDirectAccesses,
                faiEP.ub.numIndirectAccesses - seenIndirectAccesses
            ) exists { wa =>
                    val method = contextProvider.contextFromId(wa._1).method.definedMethod
                    if (method.isStaticInitializer) {
                        if (wa._3.isDefined) {
                            // If a receiver is defined, we know that the access was not static
                            // IMPROVE: Add static information to accesses and resolve this
                            false
                        } else {
                            // As a fallback, we soundly assume assignability
                            true
                        }
                    } else
                        false
                }
        } else
            false

        state.latestFieldWriteAccessInformation = Some(faiEP)
        assignable
    }

    def createResult()(implicit state: AnalysisState): ProperPropertyComputationResult = {
        if (state.hasDependees && (state.fieldAssignability ne Assignable)) {
            InterimResult(
                state.field,
                Assignable,
                state.fieldAssignability,
                state.dependees,
                continuation
            )
        } else {
            Result(state.field, state.fieldAssignability)
        }
    }

    def continuation(eps: SomeEPS)(implicit state: AnalysisState): ProperPropertyComputationResult = {
        if (handleFieldWriteAccessInformation(eps.asInstanceOf[EOptionP[DeclaredField, FieldWriteAccessInformation]]))
            Result(state.field, Assignable)
        else
            createResult()
    }
}

trait L0FieldAssignabilityAnalysisScheduler extends FPCFAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys = Seq(DeclaredMethodsKey, DeclaredFieldsKey)

    final override def uses: Set[PropertyBounds] = PropertyBounds.ubs(FieldWriteAccessInformation)

    final def derivedProperty: PropertyBounds = {
        // currently, the analysis will derive the final result in a single step
        PropertyBounds.finalP(FieldAssignability)
    }
}

/**
 * Factory object to create instances of the FieldImmutabilityAnalysis.
 */
object EagerL0FieldAssignabilityAnalysis
    extends L0FieldAssignabilityAnalysisScheduler
    with BasicFPCFEagerAnalysisScheduler {

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new L0FieldAssignabilityAnalysis(p)
        val classFileCandidates =
            if (p.libraryClassFilesAreInterfacesOnly)
                p.allProjectClassFiles
            else
                p.allClassFiles
        val fields = {
            classFileCandidates.filter(cf => cf.methods.forall(m => !m.isNative)).flatMap(_.fields)
        }
        ps.scheduleEagerComputationsForEntities(fields)(analysis.determineFieldAssignability)
        analysis
    }
}

object LazyL0FieldAssignabilityAnalysis
    extends L0FieldAssignabilityAnalysisScheduler
    with BasicFPCFLazyAnalysisScheduler {

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def register(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new L0FieldAssignabilityAnalysis(p)
        ps.registerLazyPropertyComputation(
            FieldAssignability.key,
            (field: Field) => analysis.determineFieldAssignabilityLazy(field)
        )
        analysis
    }
}
