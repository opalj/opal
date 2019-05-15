/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import org.opalj.collection.RefIterator
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.InterimEP
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.fpcf.UBPS
import org.opalj.br.fpcf.cg.properties.CallersProperty
import org.opalj.br.fpcf.cg.properties.OnlyCallersWithUnknownContext
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.DeclaredMethod
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.cg.properties.InstantiatedTypes

/**
 * In a library analysis scenario, this analysis complements the call graph by marking public
 * methods of instantiated types reachable by unknown callers from outside the library.
 *
 * @author Dominik Helm
 */
class LibraryInstantiatedTypesBasedEntryPointsAnalysis private[analyses] (
        final val project: SomeProject
) extends FPCFAnalysis {

    val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    def start(p: SomeProject): PropertyComputationResult = {
        val instantiatedTypes: EOptionP[SomeProject, InstantiatedTypes] =
            propertyStore(project, InstantiatedTypes.key)

        handleInstantiatedTypes(instantiatedTypes, 0)
    }

    private[this] def handleInstantiatedTypes(
        instantiatedTypes: EOptionP[SomeProject, InstantiatedTypes],
        numProcessedTypes: Int
    ): PropertyComputationResult = {
        val (newReachableMethods, isFinal, size) = instantiatedTypes match {
            case UBPS(initialTypes: InstantiatedTypes, isFinal) ⇒
                (
                    analyzeTypes(initialTypes.getNewTypes(numProcessedTypes)),
                    isFinal,
                    initialTypes.types.size
                )
            case _ ⇒ (Iterator.empty, false, 0)
        }

        val c = if (!isFinal)
            Some(InterimPartialResult(
                Nil,
                Some(instantiatedTypes),
                continuation(size)
            ))
        else
            None

        Results(c, resultsForReachableMethods(newReachableMethods))
    }

    private[this] def continuation(
        numProcessedTypes: Int
    )(
        eps: SomeEPS
    ): PropertyComputationResult = {
        eps match {
            case UBP(_: InstantiatedTypes) ⇒
                handleInstantiatedTypes(
                    eps.asInstanceOf[EOptionP[SomeProject, InstantiatedTypes]],
                    numProcessedTypes
                )
            case _ ⇒ throw new UnknownError("Unexpected update: "+eps)
        }
    }

    def analyzeTypes(types: Iterator[ObjectType]): Iterator[DeclaredMethod] = {
        types.flatMap { ot ⇒
            project.classFile(ot).map { cf ⇒
                cf.methodsWithBody.filter(m ⇒ !m.isStatic && m.isPublic)
            }.getOrElse(RefIterator.empty)
        }.map(declaredMethods(_))
    }

    def resultsForReachableMethods(
        reachableMethods: Iterator[DeclaredMethod]
    ): Iterator[ProperPropertyComputationResult] = {
        reachableMethods.map { method ⇒
            PartialResult[DeclaredMethod, CallersProperty](method, CallersProperty.key, {
                case InterimUBP(ub) if !ub.hasCallersWithUnknownContext ⇒
                    Some(InterimEUBP(method, ub.updatedWithUnknownContext()))

                case _: InterimEP[_, _] ⇒ None

                case _: EPK[_, _] ⇒
                    Some(InterimEUBP(method, OnlyCallersWithUnknownContext))

                case r ⇒
                    throw new IllegalStateException(s"unexpected previous result $r")
            })
        }
    }
}

object EagerLibraryInstantiatedTypesBasedEntryPointsAnalysis extends BasicFPCFEagerAnalysisScheduler {

    override def start(
        project:       SomeProject,
        propertyStore: PropertyStore,
        unused:        Null
    ): FPCFAnalysis = {
        val analysis = new LibraryInstantiatedTypesBasedEntryPointsAnalysis(project)
        propertyStore.scheduleEagerComputationsForEntities(Iterator(project))(analysis.start)
        analysis
    }

    override def uses: Set[PropertyBounds] = Set(
        PropertyBounds.ub(InstantiatedTypes)
    )

    override def derivesCollaboratively: Set[PropertyBounds] = Set(
        PropertyBounds.ub(CallersProperty)
    )

    override def derivesEagerly: Set[PropertyBounds] = Set.empty
}
