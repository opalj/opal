/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import scala.language.existentials

import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.EPS
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimEP
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEPS
import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.tac.fpcf.properties.cg.Callers
import org.opalj.tac.fpcf.properties.cg.LoadedClasses
import org.opalj.tac.fpcf.properties.cg.OnlyVMLevelCallers

/**
 * Extends the call graph analysis to include calls to static initializers from within the JVM for
 * each loaded class ([[LoadedClasses]]).
 * This requires the [[LoadedClasses]] to be computed, e.g. by the
 * [[LoadedClassesAnalysis]].
 *
 * @author Florian Kuebler
 */
// TODO Instead of added the clinits for all super types, add all super types to be loaded
class StaticInitializerAnalysis(val project: SomeProject) extends FPCFAnalysis {

    private val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    private case class LCState(
            // only present for non-final values
            var lcDependee:      Option[EOptionP[SomeProject, LoadedClasses]],
            var loadedClassesUB: Option[LoadedClasses],
            var seenClasses:     Int
    )

    /**
     * For the given project, it registers to the [[LoadedClasses]]
     * and ensures that for each loaded class, its static initializer is called (see
     * [[Callers]])
     */
    def analyze(p: SomeProject): PropertyComputationResult = {
        val (lcDependee, loadedClassesUB) = propertyStore(project, LoadedClasses.key) match {
            case FinalP(loadedClasses)                          => None -> Some(loadedClasses)
            case eps @ InterimUBP(loadedClasses: LoadedClasses) => Some(eps) -> Some(loadedClasses)
            case epk                                            => Some(epk) -> None
        }

        implicit val state: LCState = LCState(
            lcDependee, loadedClassesUB, 0
        )

        handleLoadedClasses()
    }

    private[this] def handleLoadedClasses()(
        implicit
        state: LCState
    ): PropertyComputationResult = {
        val (unseenLoadedClasses, seenClasses) =
            if (state.loadedClassesUB.isDefined) {
                val lcUB = state.loadedClassesUB.get
                (lcUB.dropOldest(state.seenClasses), lcUB.size)
            } else {
                (Iterator.empty, 0)
            }
        state.seenClasses = seenClasses

        val emptyResult = if (state.lcDependee.isDefined)
            Some(InterimPartialResult(
                None,
                state.lcDependee.toSet,
                continuation
            ))
        else
            None

        val callersResult =
            unseenLoadedClasses
                .flatMap { lc => retrieveStaticInitializers(lc) }
                .map { clInit =>
                    PartialResult[DeclaredMethod, Callers](
                        clInit,
                        Callers.key,
                        {
                            case InterimUBP(ub: Callers) if !ub.hasVMLevelCallers =>
                                Some(InterimEUBP(clInit, ub.updatedWithVMLevelCall()))

                            case _: InterimEP[_, _]  => None

                            case _: EPK[_, _]        => Some(InterimEUBP(clInit, OnlyVMLevelCallers))
                            case epk @ FinalEP(_, _) => throw new MatchError(epk)

                        }
                    )
                }

        Results(emptyResult, callersResult)
    }

    private[this] def continuation(
        someEPS: SomeEPS
    )(
        implicit
        state: LCState
    ): PropertyComputationResult = {
        (someEPS: @unchecked) match {

            case FinalP(loadedClasses: LoadedClasses) =>
                state.lcDependee = None
                state.loadedClassesUB = Some(loadedClasses)
                handleLoadedClasses()
            case InterimUBP(loadedClasses: LoadedClasses) =>
                state.lcDependee = Some(someEPS.asInstanceOf[EPS[SomeProject, LoadedClasses]])
                state.loadedClassesUB = Some(loadedClasses)
                handleLoadedClasses()
        }
    }

    private[this] def retrieveStaticInitializers(
        declaringClassType: ObjectType
    ): Iterator[DefinedMethod] = {
        // TODO only for interfaces with default methods
        ch.allSuperclassesIterator(declaringClassType, reflexive = true).flatMap { cf =>
            // IMPROVE Only return the static initializer if it is not already present
            cf.staticInitializer map { clInit => declaredMethods(clInit) }
        }
    }

}

object StaticInitializerAnalysisScheduler extends BasicFPCFEagerAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys = Seq(DeclaredMethodsKey)

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(LoadedClasses)

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(
        Callers
    )

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new StaticInitializerAnalysis(p)
        ps.scheduleEagerComputationForEntity(p)(
            analysis.analyze
        )
        analysis
    }

}
