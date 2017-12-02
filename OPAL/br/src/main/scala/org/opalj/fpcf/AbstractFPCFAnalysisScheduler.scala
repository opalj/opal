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

import java.util.concurrent.atomic.AtomicInteger

/**
 * Provides the generic infrastructure that is implemented by all factories for
 * FPCF analyses.
 * Analyses that are created sing this factory will then be run using the [[PropertyStore]].
 * I.e., this trait is typically implemented by the singleton object that facilitates
 * the creation of analyses.
 *
 * @note It is possible to use an analysis that directly uses the property store and
 *      an analysis that uses this factory infrastructure at the same time.
 *
 * @author Michael Reif
 * @author Michael Eichberg
 */
private[fpcf] trait AbstractFPCFAnalysisScheduler {

    /**
     * The unique id of this factory.
     *
     * Every factory for a specific analysis is automatically associated with a unique id.
     */
    final val uniqueId: Int = AbstractFPCFAnalysisScheduler.nextId

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
     * Returns a set of integers that contains the id of every [[Property]] that is derived by
     * the underlying analysis which is described by this `AbstractFPCFAnalysisScheduler`.
     *
     * This method has to be overridden in every subclass since it is used by the
     * [[FPCFAnalysesManager]] to guarantee the save execution of all FPCFAnalysis.
     */
    def derivedProperties: Set[PropertyKind]

    /**
     * Returns the kinds of properties which are queried by this analysis.
     *
     * @note   This set consists only of property kinds which are directly used by the analysis.
     *
     * @note   Self usages don't have to be documented since the analysis will derive this
     *         property during the computation.
     */
    def usedProperties: Set[PropertyKind] = Set.empty
}

/**
 * Companion object of [[AbstractFPCFAnalysisScheduler]] that defines interal helper functions and
 * values.
 *
 * @author Michael Reif
 */
private[fpcf] object AbstractFPCFAnalysisScheduler {

    private[this] val idGenerator: AtomicInteger = new AtomicInteger(0)

    private[AbstractFPCFAnalysisScheduler] def nextId: Int = idGenerator.getAndIncrement()

}
