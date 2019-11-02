/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package xta

import org.opalj.br.DeclaredMethod
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFTriggeredAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.cg.InstantiatedTypes
import org.opalj.br.fpcf.properties.cg.OnlyCallersWithUnknownContext
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
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.fpcf.UBPS

import scala.collection.mutable

/**
 * In a library analysis scenario, this analysis complements the call graph by marking public
 * methods of instantiated types reachable by unknown callers from outside the library.
 *
 * This analysis is adapted from the RTA version. RTA only adds types to a single type set attached
 * to the Project object, which is known in advance. Because of this, the RTA version can be eager.
 * On the contrary, the XTA/... version is triggered since there are many entities with type sets
 * and the concrete entities are unknown in advance. Similarly, since a type can be added to more than
 * one type set, already processed types are remembered globally so that they are not processed twice.
 *
 * @author Dominik Helm
 * @author Andreas Bauer
 */
class InstantiatedTypesBasedEntryPointsAnalysis private[analyses] (
        final val project: SomeProject
) extends FPCFAnalysis {

    val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
    private val globallySeenTypes: mutable.Set[ObjectType] = mutable.Set[ObjectType]()

    def analyze(se: SetEntity): PropertyComputationResult = {
        val instantiatedTypes: EOptionP[SetEntity, InstantiatedTypes] =
            propertyStore(project, InstantiatedTypes.key)

        handleInstantiatedTypes(instantiatedTypes, 0)
    }

    private[this] def handleInstantiatedTypes(
        instantiatedTypes: EOptionP[SetEntity, InstantiatedTypes],
        numProcessedTypes: Int
    ): PropertyComputationResult = {
        val (newReachableMethods, isFinal, size) = instantiatedTypes match {
            case UBPS(initialTypes: InstantiatedTypes, isFinal) ⇒
                (
                    analyzeTypes(initialTypes.dropOldest(numProcessedTypes)),
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
                    eps.asInstanceOf[EOptionP[SetEntity, InstantiatedTypes]],
                    numProcessedTypes
                )
            case _ ⇒ throw new UnknownError("Unexpected update: "+eps)
        }
    }

    def analyzeTypes(types: Iterator[ReferenceType]): Iterator[DeclaredMethod] = {
        types.flatMap {
            case ot: ObjectType if !globallySeenTypes.contains(ot) ⇒
                globallySeenTypes += ot
                project.classFile(ot).map { cf ⇒
                    cf.methodsWithBody.filter(m ⇒ !m.isStatic && m.isPublic)
                }.getOrElse(RefIterator.empty)
            case _ ⇒ RefIterator.empty
        }.map(declaredMethods(_))
    }

    def resultsForReachableMethods(
        reachableMethods: Iterator[DeclaredMethod]
    ): Iterator[ProperPropertyComputationResult] = {
        reachableMethods.map { method ⇒
            PartialResult[DeclaredMethod, Callers](method, Callers.key, {
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

object LibraryInstantiatedTypesBasedEntryPointsAnalysis extends BasicFPCFTriggeredAnalysisScheduler {

    override def register(
        project:       SomeProject,
        propertyStore: PropertyStore,
        i:             Null
    ): FPCFAnalysis = {
        val analysis = new InstantiatedTypesBasedEntryPointsAnalysis(project)
        propertyStore.registerTriggeredComputation(InstantiatedTypes.key, analysis.analyze)
        analysis
    }

    override def uses: Set[PropertyBounds] = Set(
        PropertyBounds.ub(InstantiatedTypes)
    )

    override def derivesCollaboratively: Set[PropertyBounds] = Set(
        PropertyBounds.ub(Callers)
    )

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def triggeredBy: PropertyKind = InstantiatedTypes
}
