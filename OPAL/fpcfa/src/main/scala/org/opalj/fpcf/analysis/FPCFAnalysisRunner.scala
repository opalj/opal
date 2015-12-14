/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
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
package analysis

import java.util.concurrent.atomic.AtomicInteger
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.SourceElementsPropertyStoreKey
import org.opalj.br.ClassFile
import org.opalj.br.Method

/**
 * Provides the generic infrastructure that is implemented by all factories for
 * FPCF analyses.
 * Analyses that are created sing this
 * factory will then be run using the [[PropertyStore]].
 * I.e., this trait is typically implemented by the singleton object that facilitates
 * the creation of analyses.
 * @example
 * {{{
 * }}}
 *
 * @note It is possible to use an analysis that directly uses the property store and
 * 		an analysis that uses this factory infrastructure at the same time.
 *
 * @author Michael Reif
 * @author Michael Eichberg
 */
trait FPCFAnalysisRunner {

    /**
     * The unique id of this factory.
     *
     * Every factory for a specific analysis is automatically associated with a unique id.
     */
    final val uniqueId: Int = FPCFAnalysisRunner.nextId

    /**
     * Returns a short descriptive name of the analysis for which this is the factory.
     *
     * The default name is the name of this class.
     *
     * '''This method should be overridden.'''
     */
    def name: String = {
        val nameCandidate = this.getClass.getSimpleName
        if (nameCandidate.endsWith("$"))
            nameCandidate.substring(0, nameCandidate.length() - 1)
        else
            nameCandidate
    }

    /**
     * Returns the information which other analyses strictly need to be executed
     * before this analysis can be performed.
     *
     * @note
     * 		An analysis should be listed as a requirement if and only if the analysis
     * 		strictly depends on the computed property of the analysis and the property
     * 		has no fallback (which is generally not the case!).
     * 		If no strict requirements are defined then this analysis can be run even
     * 		if no other analyses are executed. This provides the end user more leeway
     * 		in specifying the analyses that should be analyzed.
     */
    def requirements: Set[FPCFAnalysisRunner] = Set.empty

    /**
     * Returns the information which kind of analyses should be executed to achieve
     * more precise analysis results.
     *
     * This set should include all analyses that are required to get the most precise
     * result.
     * FIXME Transitively used analyses also should be added here since more than one analysis
     * could depend on the same property.
     *
     * Real requirements of the analysis should be added to the requirements definition. The set for
     * recommendations should be disjunct to the requirements set.
     *
     * @note These analyses are not required. Hence, the analysis will always compute a correct
     * result. If the set of recommendations is not empty, you may lose precision for every analysis
     * that is not executed in parallel.
     */
    def recommendations: Set[FPCFAnalysisRunner] = Set.empty

    /**
     * Returns a set of integers that contains the id of every [[Property]] or [[SetProperty]] that is derived by
     * the underlying analysis which is described by this [[FPCFAnalysisRunner]].
     *
     * This method has to be overridden in every subclass since it is used by the [[FPCFAnalysesManager]] to guarantee the save
     * execution of all FPCFAnalysis.
     */
    protected[analysis] def derivedProperties: Set[PropertyKind]

    /**
     * Returns a set of integers that contains the id of every [[Property]] or [SetProperty] that
     * is used by the underlying analysis which is described by this [[FPCFAnalysisRunner]].
     *
     * The analyses with this id's are not explicitly required which is the case when the used properties
     * define a (save) fallback value which is set by the [[PropertyStore]] if required.
     *
     * This set consists only of property id's which are directly used by the analysis.
     *
     * Self usages don't have to be documented since the analysis will derive this property during
     * the computation.
     */
    protected[analysis] def usedProperties: Set[PropertyKind] = Set.empty

    /**
     * Starts the analysis for the given `project`. This method is typically implicitly
     * called by the [[FPCFAnalysesManager]].
     */
    final protected[analysis] def start(project: SomeProject): FPCFAnalysis = {
        start(project, project.get(SourceElementsPropertyStoreKey))
    }

    /**
     * Starts the analysis for the given `project`. This method is typically implicitly
     * called by the [[FPCFAnalysesManager]].
     */
    protected[analysis] def start(
        project:       SomeProject,
        propertyStore: PropertyStore
    ): FPCFAnalysis
}

/**
 * Companion object of FPCFAnalysisRunner that defines common helper functions and
 * values.
 *
 * @author Michael Reif
 */
object FPCFAnalysisRunner {

    private[this] val idGenerator: AtomicInteger = new AtomicInteger(0)

    private[FPCFAnalysisRunner] def nextId: Int = idGenerator.getAndIncrement()

    final val ClassFileSelector: PartialFunction[Entity, ClassFile] = {
        case cf: ClassFile ⇒ cf
    }

    final val MethodSelector: PartialFunction[Entity, Method] = {
        case m: Method ⇒ m
    }

    final val NonAbstractMethodSelector: PartialFunction[Entity, Method] = {
        case m: Method if !m.isAbstract ⇒ m
    }
}
