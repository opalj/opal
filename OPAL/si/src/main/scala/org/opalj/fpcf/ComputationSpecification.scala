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

case class SpecificationViolation(message: String) extends Exception(message)

/**
 * Specification of the properties of a fix-point computation (FPC) that are relevant
 * when computing the correct scheduling order.
 *
 * @author Michael Eichberg
 */
trait ComputationSpecification {

    /**
     * Returns a short descriptive name of the analysis which is described by this specification.
     *
     * The default name is the name of `this` class.
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
     * Returns the kinds of properties which are queried by this analysis.
     *
     * @note   This set consists only of property kinds which are directly used by the analysis.
     *
     * @note   Self usages don't have to be documented since the analysis will derive this
     *         property during the computation.
     */
    def uses: Set[PropertyKind]

    /**
     * Returns the set of property kinds derived by the underlying analysis.
     */
    def derives: Set[PropertyKind]

    require(derives.nonEmpty, "the computation does not derive any information")

    /**
     * Has to be true if a computation is performed lazily. This is used to check that we
     * never schedule multiple analyses which compute the same kind of property.
     */
    def isLazy: Boolean

    /**
     * Called by the scheduler before `schedule` is called to enable further initialization
     * of the to be scheduled computations. For example to initialize global configuration
     * information.
     *
     * If an [[AnalysisScenario]] is used to compute a schedule and execute it later on, `init`
     * will be called before any analysis – independent – of the batch in which it will run,
     * is called.
     *
     * A computation specification MUST NOT call any methods of the property store that
     * may trigger or schedule computations; i.e.., it must – in particular – not call
     * the methods `apply`, `schedule*`, `register*` or `waitOnPhaseCompletion`.
     *
     *
     * @note This method is intended to be overwritten by sub classes. The default implementation
     *       does nothing.
     */
    def init(ps: PropertyStore): Unit = {}

    /**
     * Called by the scheduler to start execution of this analysis.
     *
     * The analysis may very well be a lazy computation.
     */
    def schedule(ps: PropertyStore): Unit

    override def toString: String = {
        val uses =
            this.uses.iterator.map(u ⇒ PropertyKey.name(u)).mkString("uses={", ", ", "}")
        val derives =
            this.derives.iterator.map(d ⇒ PropertyKey.name(d)).mkString("derives={", ", ", "}")
        s"FPC(name=$name,$uses,$derives)"
    }

}
