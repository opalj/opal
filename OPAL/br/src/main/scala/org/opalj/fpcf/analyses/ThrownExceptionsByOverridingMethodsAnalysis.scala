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
        val exceptions = new BRMutableTypesSet(ps.context[SomeProject].classHierarchy)
        var methodIsRefinable = false
        var hasUnknownExceptions = false

        var dependees = Set.empty[EOptionP[Method, Property]]

        val concreteMethod = ps(m, ThrownExceptions.Key)
        concreteMethod match {
            case EP(_, a: AllThrownExceptions) ⇒
                methodIsRefinable = a.isRefinable
                exceptions ++= a.types.concreteTypes
            case EP(_, ThrownExceptionsAreUnknown(_)) ⇒
                hasUnknownExceptions = true
            case epk ⇒ // Not yet computed
                methodIsRefinable = true
                dependees += epk
        }

        val subClasses = project.classHierarchy.directSubtypesOf(m.classFile.thisType)

        if (subClasses.isEmpty && !methodIsRefinable) {
            // If we don't have subclasses and the method result is final, return directly
            if (hasUnknownExceptions)
                return Result(m, UnknownThrownExceptionsByOverridingMethods);
            else
                return Result(m, AllThrownExceptionsByOverridingMethods());
        }

        // Complex case: the current method depends on the method of all subclasses
        // First we query the propertystore for all results of the direct subtype methods
        subClasses
            .flatMap(x ⇒ project.classFile(x).get.findMethod(m.name, m.descriptor))
            .map { subMethod ⇒
                ps(
                    subMethod,
                    properties.ThrownExceptionsByOverridingMethods.Key
                )
            }
            .filter(_.is(AllThrownExceptionsByOverridingMethods))
            .foreach {
                case EP(_, c: AllThrownExceptionsByOverridingMethods) ⇒
                    exceptions ++= c.exceptions.concreteTypes
                    methodIsRefinable |= c.isRefinable
                case EP(_, UnknownThrownExceptionsByOverridingMethods) ⇒
                    hasUnknownExceptions = true
                case epk ⇒ dependees += epk
            }

        def c(e: Entity, p: Property, isFinal: Boolean): PropertyComputationResult = {
            methodIsRefinable = false
            p match {
                case c: AllThrownExceptionsByOverridingMethods ⇒
                    exceptions ++= c.exceptions.concreteTypes
                    if (ut == FinalUpdate) {
                        dependees = dependees.filter { _.e ne e }
                    } else {
                        dependees = dependees.filter(_.e ne e) + EP(e.asInstanceOf[Method], p)
                    }
                case UnknownThrownExceptionsByOverridingMethods ⇒
                    hasUnknownExceptions = true
                    dependees = Set.empty[EOptionP[Method, Property]]
                case a: AllThrownExceptions ⇒
                    if (ut == FinalUpdate) {
                        dependees = dependees.filter { _.e ne e }
                    } else {
                        dependees = dependees.filter(_.e ne e) + EP(e.asInstanceOf[Method], p)
                    }
                    exceptions ++= a.types.concreteTypes
                case ThrownExceptionsAreUnknown(_) ⇒
                    hasUnknownExceptions = true
                    dependees = dependees.filter { _.e ne e }
            }
            val r = if (hasUnknownExceptions)
                UnknownThrownExceptionsByOverridingMethods
            else
                AllThrownExceptionsByOverridingMethods(exceptions, isRefinable = dependees.nonEmpty)
            if (dependees.isEmpty) {
                Result(m, r)
            } else {
                IntermediateResult(m, r, dependees, c)
            }
        }

        val r = if (hasUnknownExceptions)
            UnknownThrownExceptionsByOverridingMethods
        else
            AllThrownExceptionsByOverridingMethods(exceptions, isRefinable = dependees.nonEmpty)

        if (dependees.isEmpty) {
            Result(m, r)
        } else {
            IntermediateResult(m, r, dependees, c)
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
        ps.scheduleLazyPropertyComputation[ThrownExceptionsByOverridingMethods](
            ThrownExceptionsByOverridingMethods.Key,
            analysis.lazilyAggregateExceptionsThrownByOverridingMethods
        )
        analysis
    }
}
