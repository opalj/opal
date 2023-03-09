/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.fieldassignability

import org.opalj.br.Field
import org.opalj.br.analyses.FieldAccessInformationKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.BasicFPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.properties.immutability.Assignable
import org.opalj.br.fpcf.properties.immutability.EffectivelyNonAssignable
import org.opalj.br.fpcf.properties.immutability.FieldAssignability
import org.opalj.br.fpcf.properties.immutability.NonAssignable
import org.opalj.br.instructions.PUTSTATIC
import org.opalj.fpcf.Entity
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result

/**
 * Determines if a private, static, non-final field is always initialized at most once or
 * if a field is or can be mutated after (lazy) initialization. Field read and writes at
 * initialization time (e.g., if the current class object is registered in some publicly
 * available data-store) are not considered. This is in-line with the semantics of final,
 * which also does not prevent reads of partially initialized objects.
 */
class L0FieldAssignabilityAnalysis private[analyses] (val project: SomeProject) extends FPCFAnalysis {

    final val fieldAccessInformation = project.get(FieldAccessInformationKey)

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

        if (field.isFinal)
            return Result(field, NonAssignable);

        if (!field.isPrivate)
            return Result(field, Assignable);

        if (!field.isStatic)
            return Result(field, Assignable);

        if (field.classFile.methods.exists(_.isNative))
            return Result(field, Assignable);

        val classFile = field.classFile
        val thisType = classFile.thisType

        for {
            (method, pcs) <- fieldAccessInformation.writeAccesses(field)
            if !method.isStaticInitializer
            pc <- pcs
        } {
            method.body.get.instructions(pc) match {
                case PUTSTATIC(`thisType`, fieldName, fieldType) =>
                    // We don't need to lookup the field in the class
                    // hierarchy since we are only concerned about private
                    // fields so far... so we don't have to do a full
                    // resolution of the field reference.
                    val field = classFile.findField(fieldName, fieldType)
                    if (field.isDefined) {
                        return Result(field.get, Assignable);
                    }

                case _ =>
            }
        }

        Result(field, EffectivelyNonAssignable)
    }
}

trait L0FieldAssignabilityAnalysisScheduler extends FPCFAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys = Seq(FieldAccessInformationKey)

    final override def uses: Set[PropertyBounds] = Set.empty

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
