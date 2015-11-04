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

import org.opalj.br.analyses.SomeProject

/**
 * The results of the analysis which the `FPCFAnalysisRunner` object run are saved
 * within the [[PropertyStore]] of the Project.
 *
 * @author Michael Reif
 * @author Michael Eichberg
 */
trait FPCFAnalysisRunner[T <: FPCFAnalysis] {

    final val uniqueId: Int = FPCFAnalysisRunner.nextId

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
     * @note A analysis has only to be added to the requirements if and only if this analysis
     * depends on the computed property of the analysis and the property key has no fallback
     * such that it is only available if the regarding analysis is executed.
     */
    def requirements: Set[FPCFAnalysisRunner[_]] = Set.empty

    /**
     * Returns the information which analyses should be executed to achieve
     * the most precise analysis result.
     *
     * This set should include all analyses that are required for the most precise result.
     * Transitively used analyses also should been added here since more than one analysis
     * could depend on the same property. In this case it has to be specified which analysis is
     * recommended to derive the properties of this analysis.
     *
     * @note These analyses are not required. Hence, the analysis will always compute a correct
     * result. If the set of recommendations is not empty, you may lose precision for every analysis
     * that is not executed in parallel.
     */
    def recommendations: Set[FPCFAnalysisRunner[_]] = Set.empty

    /**
     * Returns a set of integers that contains the id of every [[Property]] or [[SetProperty]] that is derived by
     * the underlying analysis which is described by this [[FPCFAnalysisRunner]].
     *
     * This method has to be overridden in every subclass since it is used by the [[FPCFAnalysisManager]] to guarantee the save
     * execution of all FPCFAnalysis.
     */
    protected def derivedProperties: Set[Int]

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
    protected def usedProperties: Set[Int] = Set.empty[Int]

    // Only (intended to be) used by FPCFAnalysisManager.
    final private[analysis] def doStart(project: SomeProject) = start(project)

    /**
     * Starts the analysis for the given `project`.
     *
     * @note This method is abstract and has to be overridden in any subclass.
     */
    protected def start(project: SomeProject): Unit
}

/**
 *
 * Companion object of FPCFAnalysisRunner.
 *
 * @author Michael Reif
 */
private object FPCFAnalysisRunner {

    private[this] val idGenerator = new java.util.concurrent.atomic.AtomicInteger(0)

    private[FPCFAnalysisRunner] def nextId: Int = {
        idGenerator.getAndIncrement()
    }
}