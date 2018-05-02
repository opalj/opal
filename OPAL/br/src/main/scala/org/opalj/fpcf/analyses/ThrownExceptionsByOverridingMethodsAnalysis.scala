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
import org.opalj.fpcf.properties._

/**
 * Aggregates the exceptions thrown by a method over all methods which override the respective
 * method.
 *
 * @author Andreas Muttscheller
 * @author Michael Eichberg
 */
class ThrownExceptionsByOverridingMethodsAnalysis private (
        final val project: SomeProject
) extends FPCFAnalysis {

    private def lazilyAggregateExceptionsThrownByOverridingMethods(
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
    def aggregateExceptionsThrownByOverridingMethods(m: Method): PropertyComputationResult = {
        // If an unknown subclass can override this method we cannot gather information about
        // the thrown exceptions. Return the analysis immediately.
        if (project.get(IsOverridableMethodKey)(m).isYesOrUnknown) {
            return Result(m, ThrownExceptionsByOverridingMethods.MethodIsOverridable);
        }

        var exceptions = new BRMutableTypesSet(ps.context[SomeProject].classHierarchy)

        var dependees = Set.empty[EOptionP[Entity, Property]]

        // Get all subtypes, inclusive the current method as well
        project.classHierarchy.allSubtypes(m.classFile.thisType, reflexive = true) foreach { subType ⇒
            project.classFile(subType).foreach(_.findMethod(m.name, m.descriptor) match {
                case Some(subtypeMethod) ⇒
                    ps(subtypeMethod, ThrownExceptions.Key) match {
                        case EPS(_, _, ThrownExceptions.MethodIsAbstract) |
                            EPS(_, _, ThrownExceptions.MethodBodyIsNotAvailable) |
                            EPS(_, _, ThrownExceptions.MethodIsNative) |
                            EPS(_, _, ThrownExceptions.UnknownExceptionIsThrown) |
                            EPS(_, _, ThrownExceptions.AnalysisLimitation) |
                            EPS(_, _, ThrownExceptions.UnresolvedInvokeDynamicInstruction) ⇒
                            return Result(m, ThrownExceptionsByOverridingMethods.SomeException)
                        case eps: EPS[Entity, Property] ⇒
                            exceptions = eps.ub.types.concreteTypes ++: exceptions
                            if (eps.isRefinable) {
                                dependees += eps
                            }
                        case epk ⇒ dependees += epk
                    }
                case None ⇒
            })
        }

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
                case ThrownExceptions.MethodIsAbstract |
                    ThrownExceptions.MethodBodyIsNotAvailable |
                    ThrownExceptions.MethodIsNative |
                    ThrownExceptions.UnknownExceptionIsThrown |
                    ThrownExceptions.AnalysisLimitation |
                    ThrownExceptions.UnresolvedInvokeDynamicInstruction ⇒
                    return Result(m, ThrownExceptionsByOverridingMethods.SomeException)
                case te: ThrownExceptions ⇒
                    exceptions = te.types.concreteTypes ++: exceptions
            }
            if (dependees.isEmpty) {
                Result(m, new ThrownExceptionsByOverridingMethods(exceptions))
            } else {
                IntermediateResult(m, ThrownExceptionsByOverridingMethods.SomeException, new ThrownExceptionsByOverridingMethods(exceptions), dependees, c)
            }
        }

        if (dependees.isEmpty) {
            Result(m, new ThrownExceptionsByOverridingMethods(exceptions))
        } else {
            IntermediateResult(m, ThrownExceptionsByOverridingMethods.SomeException, new ThrownExceptionsByOverridingMethods(exceptions), dependees, c)
        }
    }
}

/**
 * Factory/executor of the thrown exceptions analysis.
 *
 * @author Andreas Muttscheller
 * @author Michael Eichberg
 */
object ThrownExceptionsByOverridingMethodsAnalysis extends FPCFAnalysisScheduler {

    override def uses: Set[PropertyKind] = Set(ThrownExceptions.Key)

    override def derives: Set[PropertyKind] = {
        Set(properties.ThrownExceptionsByOverridingMethods.Key)
    }

    def start(project: SomeProject, ps: PropertyStore): FPCFAnalysis = {
        val analysis = new ThrownExceptionsByOverridingMethodsAnalysis(project)
        val allMethods = project.allMethodsWithBody // FIXME we nee this information also for abstract methods ...
        ps.scheduleForEntities(allMethods)(analysis.aggregateExceptionsThrownByOverridingMethods)
        analysis
    }

    /** Registers an analysis to compute the exceptions thrown by overriding methods lazily. */
    def startLazily(project: SomeProject, ps: PropertyStore): FPCFAnalysis = {
        val analysis = new ThrownExceptionsByOverridingMethodsAnalysis(project)
        ps.registerLazyPropertyComputation[ThrownExceptionsByOverridingMethods](
            ThrownExceptionsByOverridingMethods.Key,
            analysis.lazilyAggregateExceptionsThrownByOverridingMethods
        )
        analysis
    }
}
