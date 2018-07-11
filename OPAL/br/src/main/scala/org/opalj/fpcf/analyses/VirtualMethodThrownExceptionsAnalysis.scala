/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package fpcf
package analyses

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.IsOverridableMethodKey
import org.opalj.br.collection.mutable.{TypesSet ⇒ BRMutableTypesSet}
import org.opalj.fpcf.properties.ThrownExceptions
import org.opalj.fpcf.properties.ThrownExceptionsByOverridingMethods
import org.opalj.fpcf.properties.ThrownExceptions.MethodIsAbstract
import org.opalj.fpcf.properties.ThrownExceptions.MethodBodyIsNotAvailable
import org.opalj.fpcf.properties.ThrownExceptions.MethodIsNative
import org.opalj.fpcf.properties.ThrownExceptions.UnknownExceptionIsThrown
import org.opalj.fpcf.properties.ThrownExceptions.AnalysisLimitation
import org.opalj.fpcf.properties.ThrownExceptions.UnresolvedInvokeDynamicInstruction
import org.opalj.fpcf.properties.ThrownExceptionsByOverridingMethods.SomeException
import org.opalj.fpcf.properties.ThrownExceptionsByOverridingMethods.MethodIsOverridable

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
    ): PropertyComputationResult = {
        e match {
            case m: Method ⇒
                aggregateExceptionsThrownByOverridingMethods(m)
            case e ⇒
                val m = s"the ThrownExceptions property is only defined for methods; found $e"
                throw new UnknownError(m)
        }
    }

    /**
     * Aggregates the exceptions thrown by a method and all overriding methods.
     */
    private[analyses] def aggregateExceptionsThrownByOverridingMethods(
        m: Method
    ): PropertyComputationResult = {
        // If an unknown subclass can override this method we cannot gather information about
        // the thrown exceptions. Return the analysis immediately.
        if (project.get(IsOverridableMethodKey)(m).isYesOrUnknown) {
            return Result(m, MethodIsOverridable);
        }

        val initialExceptions = new BRMutableTypesSet(project.classHierarchy)

        var dependees = Set.empty[EOptionP[Entity, Property]]

        // Get all subtypes, including the current method
        val allSubtypes = project.classHierarchy.allSubtypes(m.classFile.thisType, reflexive = true)
        allSubtypes foreach { subType ⇒
            project.classFile(subType).foreach(_.findMethod(m.name, m.descriptor) match {
                case Some(subtypeMethod) ⇒
                    ps(subtypeMethod, ThrownExceptions.key) match {
                        case EPS(_, _, MethodIsAbstract) |
                            EPS(_, _, MethodBodyIsNotAvailable) |
                            EPS(_, _, MethodIsNative) |
                            EPS(_, _, UnknownExceptionIsThrown) |
                            EPS(_, _, AnalysisLimitation) |
                            EPS(_, _, UnresolvedInvokeDynamicInstruction) ⇒
                            return Result(m, SomeException)
                        case eps: EPS[Entity, Property] ⇒
                            initialExceptions ++= eps.ub.types.concreteTypes
                            if (eps.isRefinable) {
                                dependees += eps
                            }
                        case epk ⇒ dependees += epk
                    }
                case None ⇒
            })
        }

        var exceptions = initialExceptions.toImmutableTypesSet

        def c(eps: SomeEPS): PropertyComputationResult = {
            dependees = dependees.filter { d ⇒
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
                    UnresolvedInvokeDynamicInstruction ⇒
                    return Result(m, SomeException)
                case te: ThrownExceptions ⇒
                    exceptions = exceptions ++ te.types.concreteTypes
            }
            if (dependees.isEmpty) {
                Result(m, new ThrownExceptionsByOverridingMethods(exceptions))
            } else {
                val result = new ThrownExceptionsByOverridingMethods(exceptions)
                IntermediateResult(m, SomeException, result, dependees, c)
            }
        }

        if (dependees.isEmpty) {
            Result(m, new ThrownExceptionsByOverridingMethods(exceptions))
        } else {
            val result = new ThrownExceptionsByOverridingMethods(exceptions)
            IntermediateResult(m, SomeException, result, dependees, c)
        }
    }
}

trait VirtualMethodThrownExceptionsAnalysisScheduler {

    final def uses: Set[PropertyKind] = Set(ThrownExceptions)

    final def derives: Set[PropertyKind] = Set(ThrownExceptionsByOverridingMethods)

    final type InitializationData = Null
    final def init(p: SomeProject, ps: PropertyStore): Null = null

    def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    def afterPhaseCompletion(p: SomeProject, ps: PropertyStore): Unit = {}
}

/**
 * Factory/executor of the thrown exceptions analysis.
 *
 * @author Andreas Muttscheller
 * @author Michael Eichberg
 */
object EagerVirtualMethodThrownExceptionsAnalysis
    extends VirtualMethodThrownExceptionsAnalysisScheduler
    with FPCFEagerAnalysisScheduler {

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
    with FPCFLazyAnalysisScheduler {

    /** Registers an analysis to compute the exceptions thrown by overriding methods lazily. */
    override def startLazily(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new VirtualMethodThrownExceptionsAnalysis(p)
        ps.registerLazyPropertyComputation(
            ThrownExceptionsByOverridingMethods.key,
            analysis.lazilyAggregateExceptionsThrownByOverridingMethods
        )
        analysis
    }
}
