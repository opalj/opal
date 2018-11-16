/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses
package cg

import org.opalj.collection.RefIterator
import org.opalj.fpcf.cg.properties.InstantiatedTypes
import org.opalj.fpcf.cg.properties.CallersProperty
import org.opalj.fpcf.cg.properties.OnlyCallersWithUnknownContext
import org.opalj.fpcf.cg.properties.LibraryEntryPointsFakeProperty
import org.opalj.fpcf.cg.properties.LibraryEntryPointsFakePropertyFinal
import org.opalj.fpcf.cg.properties.LibraryEntryPointsFakePropertyNonFinal
import org.opalj.br.ObjectType
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.DeclaredMethods

/**
 * In a library analysis scenario, this analysis complements the call graph by marking public
 * methods of instantiated types reachable by unknown callers from outside the library.
 */
class LibraryEntryPointsAnalysis private[analyses] (
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
            case ESimplePS(_, initialTypes, isFinal) ⇒
                (
                    analyzeTypes(initialTypes.getNewTypes(numProcessedTypes)),
                    isFinal,
                    initialTypes.types.size
                )
            case _ ⇒ (Nil, false, 0)
        }

        val fakeResult =
            if (isFinal) Result(project, LibraryEntryPointsFakePropertyFinal)
            else SimplePIntermediateResult(
                project,
                LibraryEntryPointsFakePropertyNonFinal,
                Traversable(instantiatedTypes),
                continuation(size)
            )

        Results(resultsForReachableMethods(newReachableMethods) ++ Iterator(fakeResult))
    }

    private[this] def continuation(
        numProcessedTypes: Int
    )(
        eps: SomeEPS
    ): PropertyComputationResult = {
        eps match {
            case ESimplePS(_, _: InstantiatedTypes, _) ⇒
                handleInstantiatedTypes(
                    eps.asInstanceOf[EOptionP[SomeProject, InstantiatedTypes]],
                    numProcessedTypes
                )
            case _ ⇒ throw new UnknownError("Unexpected update: "+eps)
        }
    }

    def analyzeTypes(types: List[ObjectType]): List[DeclaredMethod] = {
        types.flatMap { ot ⇒
            project.classFile(ot).map { cf ⇒
                cf.methodsWithBody.filter(m ⇒ !m.isStatic && m.isPublic)
            }.getOrElse(RefIterator.empty)
        }.map(declaredMethods(_))
    }

    def resultsForReachableMethods(
        reachableMethods: List[DeclaredMethod]
    ): Iterator[PropertyComputationResult] = {
        reachableMethods.iterator.map { method ⇒
            PartialResult[DeclaredMethod, CallersProperty](method, CallersProperty.key, {
                case IntermediateESimpleP(_, ub) if !ub.hasCallersWithUnknownContext ⇒
                    Some(IntermediateESimpleP(method, ub.updatedWithUnknownContext()))

                case _: IntermediateESimpleP[_, _] ⇒ None

                case _: EPK[_, _] ⇒
                    Some(IntermediateESimpleP(method, OnlyCallersWithUnknownContext))

                case r ⇒
                    throw new IllegalStateException(s"unexpected previous result $r")
            })
        }
    }
}

object EagerLibraryEntryPointsAnalysis extends FPCFEagerAnalysisScheduler {

    override type InitializationData = Null

    override def start(
        project:       SomeProject,
        propertyStore: PropertyStore,
        unused:        Null
    ): FPCFAnalysis = {
        val analysis = new LibraryEntryPointsAnalysis(project)
        propertyStore.scheduleEagerComputationsForEntities(Iterator(project))(analysis.start)
        analysis
    }

    override def uses: Set[PropertyKind] = Set(InstantiatedTypes)

    override def derives: Set[PropertyKind] = Set(CallersProperty, LibraryEntryPointsFakeProperty)

    override def init(p: SomeProject, ps: PropertyStore): Null = null

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def afterPhaseCompletion(p: SomeProject, ps: PropertyStore): Unit = {}
}
