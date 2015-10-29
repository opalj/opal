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

import org.opalj.fpcf.analysis.FixpointAnalysis
import org.opalj.fpcf.analysis.StaticMethodAccessibilityAnalysis
import org.opalj.fpcf.analysis.FactoryMethodAnalysis

/**
 * The fixpoint analyses registry is a registry for all analyses that need
 * to be computed with the fixpoint framework because they depend on other analyses.
 *
 * All registered analyses does compute a [[org.opalj.fpcf.Property]] associated with
 * an [[org.opalj.fpcf.Entity]].
 * Instances for entities are, e.g.: ´classes´, ´methods´ or ´fields´.
 *
 * The registry was developed to support configuration purposes where the user/developer
 * choose between different domains.
 *
 * The analyses that are part of OPAL are already registered.
 *
 * @note The registry does not handle dependencies between analyses yet.
 *
 * ==Thread Safety==
 * The registry is thread safe.
 *
 * @author Michael Reif
 */
object FixpointAnalysesRegistry {

    private[this] var descriptions: Map[String, _ <: FixpointAnalysis] = Map.empty
    private[this] var theRegistry: Set[FixpointAnalysis] = Set.empty

    /**
     * Register a new fixpoint analysis that can be used to compute a property of an specific entity.
     *
     * @param analysisDescription A short description of the properties that the analysis computes; in
     *     particular w.r.t. a specific set of entities.
     * @param analysisClass The object of the analysis.
     */
    def register[FA <: FixpointAnalysis](
        analysisDescription: String,
        analysisClass:       FA
    ): Unit = {
        this.synchronized {
            descriptions += ((analysisDescription, analysisClass))
            theRegistry += analysisClass
        }

    }

    /**
     * Returns an `Iterable` to make it possible to iterate over the descriptions of
     * the analysis. Useful to show the (end-users) some meaningful descriptions.
     */
    def analysisDescriptions(): Iterable[String] = this.synchronized { descriptions.keys }

    /**
     * Returns the current view of the registry.
     */
    def registry: Set[FixpointAnalysis] = this.synchronized { theRegistry }

    /**
     * Return the [[FixpointAnalysis]] object that can be used to analyze a project later on.
     *
     * @note This registry does only support scala `object`s. Fixpoint analyses implemented in an class
     * are currently not (directly) supported by the registry.
     */
    def newFixpointAnalysis(
        analysisDescripition: String
    ): FixpointAnalysis = {
        this.synchronized {
            descriptions(analysisDescripition)
        }
    }

    // initialize the registry with the known default analyses

    register(
        "[ShadowingAnalysis] An analysis which computes the project accessibility of static melthods property w.r.t. clients.",
        StaticMethodAccessibilityAnalysis
    )

    register(
        "[FactoryMethodAnalysis] An analysis which computes whether a static melthod is an accessible factory method w.r.t. clients.",
        FactoryMethodAnalysis
    )
}