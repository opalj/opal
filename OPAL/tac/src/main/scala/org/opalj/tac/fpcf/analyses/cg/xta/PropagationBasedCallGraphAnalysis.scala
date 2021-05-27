/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package xta

import scala.language.existentials

import org.opalj.fpcf.EPS
import org.opalj.fpcf.EUBP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.SomeEPS
import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.cg.InstantiatedTypes
import org.opalj.tac.fpcf.properties.TACAI

/**
 * XTA, MTA, FTA and CTA are a propagation-based call graph analyses which were introduced by Tip and Palsberg.
 *
 * This analysis does not handle features such as JVM calls to static initializers or finalize
 * calls.
 * However, analyses for these features (e.g. [[org.opalj.tac.fpcf.analyses.cg.FinalizerAnalysis]]
 * or the [[org.opalj.tac.fpcf.analyses.cg.LoadedClassesAnalysis]]) can be executed within the
 * same batch and the call graph will be generated in collaboration.
 *
 * @author Andreas Bauer
 */
class PropagationBasedCallGraphAnalysis private[analyses] (
        final val project:               SomeProject,
        final val typeSetEntitySelector: TypeSetEntitySelector
) extends AbstractCallGraphAnalysis {

    // TODO maybe cache results for Object.toString, Iterator.hasNext, Iterator.next

    override type State = PropagationBasedCGState

    override def c(state: PropagationBasedCGState)(eps: SomeEPS): ProperPropertyComputationResult = eps match {
        case EUBP(typeSetEntity: TypeSetEntity, _: InstantiatedTypes) ⇒
            val seenTypes = state.instantiatedTypes(typeSetEntity).size

            state.updateInstantiatedTypesDependee(
                eps.asInstanceOf[EPS[TypeSetEntity, InstantiatedTypes]]
            )

            val newTypes = state.newInstantiatedTypes(typeSetEntity, seenTypes)

            // we only want to add the new calls, so we create a fresh object
            val calleesAndCallers = new DirectCalls()

            handleVirtualCallSites(calleesAndCallers, newTypes)(state)

            returnResult(calleesAndCallers)(state)

        case _ ⇒ super.c(state)(eps)
    }

    override def createInitialState(
        definedMethod: DefinedMethod, tacEP: EPS[Method, TACAI]
    ): PropagationBasedCGState = {
        // The set of types that are definitely initialized at this point in time.
        // An analysis may add types which are relevant for this method to multiple set entities. For example,
        // exception types are tracked globally (via the set attached to the project). XTA will attach a per-method
        // type set to the DefinedMethod itself. CTA/MTA have merged sets for all methods of a class, attached to the
        // class type. The relevant set entity is retrieved by querying the set entity selector.
        val perMethodInstantiatedTypes = propertyStore(typeSetEntitySelector(definedMethod), InstantiatedTypes.key)
        val perProjectInstantiatedTypes = propertyStore(project, InstantiatedTypes.key)

        val typeSources = Iterable(perMethodInstantiatedTypes, perProjectInstantiatedTypes)

        new PropagationBasedCGState(definedMethod, tacEP, typeSources)
    }

    /**
     * Computes the calls from the given method
     * ([[org.opalj.br.fpcf.properties.cg.Callees]] property) and updates the
     * [[org.opalj.br.fpcf.properties.cg.Callers]].
     *
     * Whenever a `declaredMethod` becomes reachable (the caller property is set initially),
     * this method is called.
     * In case the method never becomes reachable, the fallback
     * [[org.opalj.br.fpcf.properties.cg.NoCallers]] will be used by the framework and this method
     * returns [[org.opalj.fpcf.NoResult]].
     */

    // modifies state and the calleesAndCallers
    private[this] def handleVirtualCallSites(
        calleesAndCallers: DirectCalls, newTypes: TraversableOnce[ReferenceType]
    )(implicit state: PropagationBasedCGState): Unit = {
        newTypes.filter(_.isObjectType).foreach { instantiatedType ⇒
            val callSites = state.getVirtualCallSites(instantiatedType.asObjectType)
            callSites.foreach { callSite ⇒
                val CallSite(pc, name, descr, declaringClass) = callSite
                val tgtR = project.instanceCall(
                    state.method.definedMethod.classFile.thisType,
                    instantiatedType,
                    name,
                    descr
                )

                handleCall(
                    state.method,
                    name,
                    descr,
                    declaringClass,
                    pc,
                    tgtR,
                    calleesAndCallers
                )
            }

            state.removeCallSite(instantiatedType.asObjectType)
        }
    }

    @inline override protected[this] def canResolveCall(
        implicit
        state: PropagationBasedCGState
    ): ObjectType ⇒ Boolean = {
        state.instantiatedTypesContains(_)
    }

    @inline protected[this] def handleUnresolvedCall(
        possibleTgtType: ObjectType,
        call:            Call[V] with VirtualCall[V],
        pc:              Int
    )(implicit state: PropagationBasedCGState): Unit = {
        state.addVirtualCallSite(
            possibleTgtType, CallSite(pc, call.name, call.descriptor, call.declaringClass)
        )
    }
}

class PropagationBasedCallGraphAnalysisScheduler(
        final val typeSetEntitySelector: TypeSetEntitySelector
) extends CallGraphAnalysisScheduler {

    override def uses: Set[PropertyBounds] =
        super.uses ++ PropertyBounds.ubs(InstantiatedTypes, Callees)

    override def derivesCollaboratively: Set[PropertyBounds] =
        super.derivesCollaboratively ++ PropertyBounds.ubs(InstantiatedTypes)

    override def initializeAnalysis(p: SomeProject): AbstractCallGraphAnalysis =
        new PropagationBasedCallGraphAnalysis(p, typeSetEntitySelector)
}
