/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package analyses

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPS
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.IsOverridableMethodKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.collection.mutable.{TypesSet => BRMutableTypesSet}
import org.opalj.br.fpcf.properties.ThrownExceptions
import org.opalj.br.fpcf.properties.ThrownExceptions.AnalysisLimitation
import org.opalj.br.fpcf.properties.ThrownExceptions.MethodBodyIsNotAvailable
import org.opalj.br.fpcf.properties.ThrownExceptions.MethodIsAbstract
import org.opalj.br.fpcf.properties.ThrownExceptions.MethodIsNative
import org.opalj.br.fpcf.properties.ThrownExceptions.UnknownExceptionIsThrown
import org.opalj.br.fpcf.properties.ThrownExceptions.UnresolvedInvokeDynamicInstruction
import org.opalj.br.fpcf.properties.ThrownExceptionsByOverridingMethods
import org.opalj.br.fpcf.properties.ThrownExceptionsByOverridingMethods.MethodIsOverridable
import org.opalj.br.fpcf.properties.ThrownExceptionsByOverridingMethods.SomeException

/**
 * Aggregates the exceptions thrown by a method over all methods which override the respective
 * method.
 *
 * @author Andreas Muttscheller
 * @author Michael Eichberg
 */
class VirtualMethodThrownExceptionsAnalysis private[analyses] (
        final val project: SomeProject
) extends FPCFAnalysis {

    private[analyses] def lazilyAggregateExceptionsThrownByOverridingMethods(
        e: Entity
    ): ProperPropertyComputationResult = {
        e match {
            case m: Method =>
                aggregateExceptionsThrownByOverridingMethods(m)
            case e =>
                val m = s"the ThrownExceptions property is only defined for methods; found $e"
                throw new UnknownError(m)
        }
    }

    /**
     * Aggregates the exceptions thrown by a method and all overriding methods.
     */
    private[analyses] def aggregateExceptionsThrownByOverridingMethods(
        m: Method
    ): ProperPropertyComputationResult = {
        // If an unknown subclass can override this method we cannot gather information about
        // the thrown exceptions. Return the analysis immediately.
        if (project.get(IsOverridableMethodKey)(m).isYesOrUnknown) {
            return Result(m, MethodIsOverridable);
        }

        val initialExceptions = new BRMutableTypesSet(project.classHierarchy)

        var dependees = Set.empty[EOptionP[Entity, Property]]

        // Get all subtypes, including the current method
        val allSubtypes = project.classHierarchy.allSubtypes(m.classFile.thisType, reflexive = true)
        allSubtypes foreach { subType =>
            project.classFile(subType).foreach(_.findMethod(m.name, m.descriptor) match {
                case Some(subtypeMethod) =>
                    ps(subtypeMethod, ThrownExceptions.key) match {
                        case UBP(MethodIsAbstract) |
                            UBP(MethodBodyIsNotAvailable) |
                            UBP(MethodIsNative) |
                            UBP(UnknownExceptionIsThrown) |
                            UBP(AnalysisLimitation) |
                            UBP(UnresolvedInvokeDynamicInstruction) =>
                            return Result(m, SomeException)
                        case eps: EPS[Entity, Property] =>
                            initialExceptions ++= eps.ub.types.concreteTypes
                            if (eps.isRefinable) {
                                dependees += eps
                            }
                        case epk => dependees += epk
                    }
                case None =>
            })
        }

        var exceptions = initialExceptions.toImmutableTypesSet

        def c(eps: SomeEPS): ProperPropertyComputationResult = {
            dependees = dependees.filter { d =>
                d.e != eps.e || d.pk != eps.pk
            }
            // If the property is not final we want to keep updated of new values
            if (eps.isRefinable) {
                dependees = dependees + eps
            }
            eps.ub match {
                // Properties from ThrownExceptions.Key
                // They are queried if we got a static or special invokation instruction

                // Check if we got some unknown exceptions. We can terminate the analysis if
                // that's the case as we cannot compute a more precise result.
                case MethodIsAbstract |
                    MethodBodyIsNotAvailable |
                    MethodIsNative |
                    UnknownExceptionIsThrown |
                    AnalysisLimitation |
                    UnresolvedInvokeDynamicInstruction =>
                    return Result(m, SomeException)
                case te: ThrownExceptions =>
                    exceptions = exceptions ++ te.types.concreteTypes
            }
            if (dependees.isEmpty) {
                Result(m, new ThrownExceptionsByOverridingMethods(exceptions))
            } else {
                val result = new ThrownExceptionsByOverridingMethods(exceptions)
                InterimResult(m, SomeException, result, dependees, c)
            }
        }

        if (dependees.isEmpty) {
            Result(m, new ThrownExceptionsByOverridingMethods(exceptions))
        } else {
            val result = new ThrownExceptionsByOverridingMethods(exceptions)
            InterimResult(m, SomeException, result, dependees, c)
        }
    }
}

trait VirtualMethodThrownExceptionsAnalysisScheduler extends FPCFAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys = Seq(IsOverridableMethodKey)

    final override def uses: Set[PropertyBounds] = Set(PropertyBounds.lub(ThrownExceptions))

    final def derivedProperty: PropertyBounds = {
        PropertyBounds.lub(ThrownExceptionsByOverridingMethods)
    }

}

/**
 * Factory/executor of the thrown exceptions analysis.
 *
 * @author Andreas Muttscheller
 * @author Michael Eichberg
 */
object EagerVirtualMethodThrownExceptionsAnalysis
    extends VirtualMethodThrownExceptionsAnalysisScheduler
    with BasicFPCFEagerAnalysisScheduler {

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new VirtualMethodThrownExceptionsAnalysis(p)
        val allMethods = p.allMethodsWithBody // FIXME we need this information also for abstract methods ...
        ps.scheduleEagerComputationsForEntities(allMethods) {
            analysis.aggregateExceptionsThrownByOverridingMethods
        }
        analysis
    }

}

/**
 * Factory/executor of the thrown exceptions analysis.
 *
 * @author Andreas Muttscheller
 * @author Michael Eichberg
 */
object LazyVirtualMethodThrownExceptionsAnalysis
    extends VirtualMethodThrownExceptionsAnalysisScheduler
    with BasicFPCFLazyAnalysisScheduler {

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    /** Registers an analysis to compute the exceptions thrown by overriding methods lazily. */
    override def register(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new VirtualMethodThrownExceptionsAnalysis(p)
        ps.registerLazyPropertyComputation(
            ThrownExceptionsByOverridingMethods.key,
            analysis.lazilyAggregateExceptionsThrownByOverridingMethods
        )
        analysis
    }
}
