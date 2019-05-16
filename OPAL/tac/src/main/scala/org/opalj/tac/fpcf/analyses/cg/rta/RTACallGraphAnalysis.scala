/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package rta

import scala.language.existentials

import org.opalj.log.Error
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger.logOnce
import org.opalj.collection.ForeachRefIterator
import org.opalj.fpcf.EPK
import org.opalj.fpcf.EPS
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.br.fpcf.cg.properties.Callers
import org.opalj.br.fpcf.cg.properties.OnlyCallersWithUnknownContext
import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.InitialEntryPointsKey
import org.opalj.br.analyses.cg.IsOverridableMethodKey
import org.opalj.br.fpcf.cg.properties.InstantiatedTypes
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFTriggeredAnalysisScheduler
import org.opalj.br.fpcf.cg.properties.Callees
import org.opalj.tac.fpcf.properties.TACAI

/**
 * A rapid type call graph analysis (RTA). For a given [[org.opalj.br.Method]] it computes the set
 * of outgoing call edges ([[org.opalj.br.fpcf.cg.properties.Callees]]). Furthermore, it updates the
 * [[org.opalj.br.fpcf.cg.properties.Callers]].
 *
 * This analysis does not handle features such as JVM calls to static initializers or finalize
 * calls.
 * However, analyses for these features (e.g. [[org.opalj.tac.fpcf.analyses.cg.FinalizerAnalysis]]
 * or the [[org.opalj.tac.fpcf.analyses.cg.LoadedClassesAnalysis]]) can be executed within the
 * same batch and the call graph will be generated in collaboration.
 *
 * @author Florian Kuebler
 */
class RTACallGraphAnalysis private[analyses] (
        final val project: SomeProject
) extends CallGraphAnalysis {

    // TODO maybe cache results for Object.toString, Iterator.hasNext, Iterator.next

    private[this] val isMethodOverridable: Method ⇒ Answer = project.get(IsOverridableMethodKey)

    override type State = RTAState

    override def c(state: RTAState)(eps: SomeEPS): ProperPropertyComputationResult = eps match {
        case UBP(tacai: TACAI) if tacai.tac.isDefined ⇒
            state.updateTACDependee(eps.asInstanceOf[EPS[Method, TACAI]])

            // we only want to add the new calls, so we create a fresh object
            processMethod(state, new DirectCalls())

        case UBP(_: TACAI) ⇒
            InterimPartialResult(
                Some(eps), c(state)
            )
        case UBP(_: InstantiatedTypes) ⇒
            val seenTypes = state.instantiatedTypesUB.size

            state.updateInstantiatedTypesDependee(
                eps.asInstanceOf[EPS[SomeProject, InstantiatedTypes]]
            )

            // we only want to add the new calls, so we create a fresh object
            val calleesAndCallers = new DirectCalls()

            handleVirtualCallSites(calleesAndCallers, seenTypes)(state)

            returnResult(calleesAndCallers)(state)
    }

    override def createInitialState(
        definedMethod: DefinedMethod, tacEP: EPS[Method, TACAI]
    ): RTAState = {
        // the set of types that are definitely initialized at this point in time
        val instantiatedTypesEOptP = propertyStore(project, InstantiatedTypes.key)
        new RTAState(definedMethod, tacEP, instantiatedTypesEOptP)
    }

    override def handleImpreciseCall(
        caller:                        DefinedMethod,
        call:                          Call[V] with VirtualCall[V],
        pc:                            Int,
        specializedDeclaringClassType: ReferenceType,
        potentialTargets:              ForeachRefIterator[ObjectType],
        calleesAndCallers:             DirectCalls
    )(implicit state: RTAState): Unit = {
        for (possibleTgtType ← potentialTargets) {
            if (state.instantiatedTypesUB.contains(possibleTgtType)) {
                val tgtR = project.instanceCall(
                    caller.declaringClassType.asObjectType,
                    possibleTgtType,
                    call.name,
                    call.descriptor
                )

                handleCall(
                    caller,
                    call.name,
                    call.descriptor,
                    call.declaringClass,
                    pc,
                    tgtR,
                    calleesAndCallers
                )
            } else {
                state.addVirtualCallSite(
                    possibleTgtType, (pc, call.name, call.descriptor, call.declaringClass)
                )
            }
        }

        if (specializedDeclaringClassType.isObjectType) {
            val declType = specializedDeclaringClassType.asObjectType

            val mResult = if (classHierarchy.isInterface(declType).isYes)
                org.opalj.Result(project.resolveInterfaceMethodReference(
                    declType, call.name, call.descriptor
                ))
            else
                org.opalj.Result(project.resolveMethodReference(
                    declType,
                    call.name,
                    call.descriptor,
                    forceLookupInSuperinterfacesOnFailure = true
                ))

            if (mResult.isEmpty) {
                unknownLibraryCall(
                    caller,
                    call.name,
                    call.descriptor,
                    call.declaringClass,
                    declType,
                    caller.definedMethod.classFile.thisType.packageName,
                    pc,
                    calleesAndCallers
                )
            } else if (isMethodOverridable(mResult.value).isYesOrUnknown) {
                calleesAndCallers.addIncompleteCallSite(pc)
            }
        }
    }

    /**
     * Computes the calls from the given method
     * ([[org.opalj.br.fpcf.cg.properties.Callees]] property) and updates the
     * [[org.opalj.br.fpcf.cg.properties.Callers]].
     *
     * Whenever a `declaredMethod` becomes reachable (the caller property is set initially),
     * this method is called.
     * In case the method never becomes reachable, the fallback
     * [[org.opalj.br.fpcf.cg.properties.NoCallers]] will be used by the framework and this method
     * returns [[org.opalj.fpcf.NoResult]].
     */

    // modifies state and the calleesAndCallers
    private[this] def handleVirtualCallSites(
        calleesAndCallers: DirectCalls, seenTypes: Int
    )(implicit state: RTAState): Unit = {
        state.newInstantiatedTypes(seenTypes).foreach { instantiatedType ⇒
            val callSitesOpt = state.getVirtualCallSites(instantiatedType)
            if (callSitesOpt.isDefined) {
                callSitesOpt.get.foreach { callSite ⇒
                    val (pc, name, descr, declaringClass) = callSite
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
            }

            state.removeCallSite(instantiatedType)
        }
    }

}

object RTACallGraphAnalysisScheduler extends FPCFTriggeredAnalysisScheduler {

    override type InitializationData = Null

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(
        InstantiatedTypes,
        Callees,
        Callers,
        TACAI
    )

    override def derivesCollaboratively: Set[PropertyBounds] =
        PropertyBounds.ubs(Callees, Callers)

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    /**
     * Updates the caller properties of the initial entry points
     * ([[org.opalj.br.analyses.cg.InitialEntryPointsKey]]) to be called from an unknown context.
     * This will trigger the computation of the callees for these methods (see `processMethod`).
     */
    def processEntryPoints(p: SomeProject, ps: PropertyStore): Unit = {
        implicit val logContext: LogContext = p.logContext
        val declaredMethods = p.get(DeclaredMethodsKey)
        val entryPoints = p.get(InitialEntryPointsKey).map(declaredMethods.apply)

        if (entryPoints.isEmpty)
            logOnce(Error("project configuration", "the project has no entry points"))

        entryPoints.foreach { ep ⇒
            ps.preInitialize(ep, Callers.key) {
                case _: EPK[_, _] ⇒
                    InterimEUBP(ep, OnlyCallersWithUnknownContext)
                case InterimUBP(ub: Callers) ⇒
                    InterimEUBP(ep, ub.updatedWithUnknownContext())
                case r ⇒
                    throw new IllegalStateException(s"unexpected eps $r")
            }
        }
    }

    override def init(p: SomeProject, ps: PropertyStore): Null = {
        processEntryPoints(p, ps)
        null
    }

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def register(p: SomeProject, ps: PropertyStore, unused: Null): RTACallGraphAnalysis = {
        val analysis = new RTACallGraphAnalysis(p)
        // register the analysis for initial values for callers (i.e. methods becoming reachable)
        ps.registerTriggeredComputation(Callers.key, analysis.analyze)
        analysis
    }

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(
        p:        SomeProject,
        ps:       PropertyStore,
        analysis: FPCFAnalysis
    ): Unit = {}

    override def triggeredBy: PropertyKind = Callers
}
