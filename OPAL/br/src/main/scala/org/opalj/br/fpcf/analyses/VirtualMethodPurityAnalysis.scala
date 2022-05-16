/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package analyses

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
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.ClassifiedImpure
import org.opalj.br.fpcf.properties.CompileTimePure
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.Purity
import org.opalj.br.fpcf.properties.SimpleContexts
import org.opalj.br.fpcf.properties.SimpleContextsKey
import org.opalj.br.fpcf.properties.VirtualMethodPurity
import org.opalj.br.fpcf.properties.VirtualMethodPurity.VImpureByAnalysis
import org.opalj.br.fpcf.properties.VirtualMethodPurity.VImpureByLackOfInformation

/**
 * Determines the aggregated purity for virtual methods.
 *
 * @author Dominik Helm
 */
class VirtualMethodPurityAnalysis private[analyses] ( final val project: SomeProject) extends FPCFAnalysis {
    private[this] val declaredMethods = project.get(DeclaredMethodsKey)
    private[this] val simpleContexts = project.get(SimpleContextsKey)

    def determinePurity(context: Context): ProperPropertyComputationResult = {
        val dm = context.method
        if (!dm.hasSingleDefinedMethod && !dm.hasMultipleDefinedMethods)
            return Result(dm, VImpureByLackOfInformation);

        var maxPurity: Purity = CompileTimePure
        var dependees: Set[SomeEOptionP] = Set.empty

        val cfo = project.classFile(dm.declaringClassType)
        val methods =
            if (cfo.isDefined && cfo.get.isInterfaceDeclaration)
                project.interfaceCall(
                    dm.declaringClassType,
                    dm.declaringClassType,
                    dm.name,
                    dm.descriptor
                )
            else project.virtualCall(
                dm.declaringClassType,
                dm.declaringClassType,
                dm.name,
                dm.descriptor
            )

        for (method <- methods) {
            propertyStore(simpleContexts(declaredMethods(method)), Purity.key) match {
                case eps @ UBP(ub) =>
                    maxPurity = maxPurity meet ub
                    if (eps.isRefinable) dependees += eps
                case epk => dependees += epk
            }
        }

        def c(eps: SomeEPS): ProperPropertyComputationResult = {
            dependees = dependees.filter { _.e ne eps.e }
            maxPurity = maxPurity meet eps.ub.asInstanceOf[Purity]
            if (eps.isRefinable) {
                dependees += eps.asInstanceOf[EOptionP[DeclaredMethod, Purity]]
            }

            if (dependees.isEmpty || maxPurity.isInstanceOf[ClassifiedImpure]) {
                Result(dm, maxPurity.aggregatedProperty)
            } else {
                InterimResult(
                    dm, VImpureByAnalysis, maxPurity.aggregatedProperty,
                    dependees, c
                )
            }
        }

        if (dependees.isEmpty || maxPurity.isInstanceOf[ClassifiedImpure]) {
            Result(dm, maxPurity.aggregatedProperty)
        } else {
            InterimResult(
                dm, VImpureByAnalysis, maxPurity.aggregatedProperty,
                dependees, c
            )
        }
    }

    /** Called when the analysis is scheduled lazily. */
    def doDeterminePurity(e: Entity): ProperPropertyComputationResult = {
        e match {
            case c: Context => determinePurity(c)
            case _          => throw new IllegalArgumentException(s"$e ist not a DefinedMethod")
        }
    }

}

trait VirtualMethodPurityAnalysisScheduler extends FPCFAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys = Seq(DeclaredMethodsKey)

    final override def uses: Set[PropertyBounds] = Set(PropertyBounds.lub(Purity))

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(VirtualMethodPurity)

}

object EagerVirtualMethodPurityAnalysis
    extends VirtualMethodPurityAnalysisScheduler
    with FPCFEagerAnalysisScheduler {

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    override type InitializationData = (DeclaredMethods, SimpleContexts, ConfiguredPurity)

    override def init(
        p: SomeProject, ps: PropertyStore
    ): (DeclaredMethods, SimpleContexts, ConfiguredPurity) = {
        (p.get(DeclaredMethodsKey), p.get(SimpleContextsKey), p.get(ConfiguredPurityKey))
    }

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(
        p:        SomeProject,
        ps:       PropertyStore,
        analysis: FPCFAnalysis
    ): Unit = {}

    override def start(
        p:    SomeProject,
        ps:   PropertyStore,
        data: InitializationData
    ): FPCFAnalysis = {
        val analysis = new VirtualMethodPurityAnalysis(p)
        val (declaredMethods, simpleContexts, configuredPurity) = data

        val dms = declaredMethods.declaredMethods
        val methods = dms.collect {
            case dm if dm.hasSingleDefinedMethod && !configuredPurity.wasSet(dm) =>
                simpleContexts(dm)
        }
        ps.scheduleEagerComputationsForEntities(methods)(analysis.determinePurity)

        analysis
    }
}

object LazyVirtualMethodPurityAnalysis
    extends VirtualMethodPurityAnalysisScheduler
    with BasicFPCFLazyAnalysisScheduler {

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def register(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new VirtualMethodPurityAnalysis(p)
        ps.registerLazyPropertyComputation(VirtualMethodPurity.key, analysis.doDeterminePurity)
        analysis
    }
}
