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
package br
package analyses

import java.util.concurrent.atomic.AtomicInteger;

/**
 * `ProjecInformationKey` objects are used to get/associate some
 * (immutable) information with a project that should be computed on demand.
 * For example, imagine that you write an analysis that requires – as a foundation –
 * the project's call graph. In this case, to get the call graph it is sufficient
 * to pass the respective key to the [[Project]] object. If the call graph was already
 * computed that one will be returned, otherwise the computation will be performed and
 * the result will be cached for future usage before it is returned.
 *
 * ==Using Project Information==
 * If access to some project information is required, it is sufficient to use
 * the (singleton) instance of the respective `ProjectInformationKey` to get
 * the respective project information.
 *
 * For example, let's assume that an index of all fields and methods is needed. In this case the
 * code to get the index would be:
 * {{{
 * import ...{ProjectIndex,ProjectIndexKey}
 * val project : Project = ???
 * val projectIndex = project.get(ProjectIndexKey)
 * // do something with the index
 * }}}
 *
 * ==Providing Project Information/Implementing `ProjectInformationKey` ==
 * Making project wide information available on demand is done as follows.
 *
 *  1. Implement the base analysis that computes the information given some project.
 *  1. Implement your `ProjectInformationKey` class that inherits from this trait and
 *    which calls the base analysis. It is recommended that the factory method ([[compute]])
 *    is side-effect free.
 *
 * ===Threading===
 * [[Project]] takes care of threading related issues. The methods [[requirements]]
 * and [[compute]] will never be called concurrently w.r.t. the same `project` object.
 * However, concurrent calls may happen w.r.t. two different project objects.
 *
 * ===Caching===
 * [[Project]] takes care of the caching of the result of the computation of the
 * information.
 *
 * @tparam T The type of the information object that is derived.
 * @tparam I The type of information used at initialization time.
 *
 * @author Michael Eichberg
 */
trait ProjectInformationKey[T <: AnyRef, I <: AnyRef] {

    /**
     * The unique id of this key. The key is used to enable efficient access and
     * is automatically assigned by OPAL and will not change after that.
     */
    final val uniqueId: Int = ProjectInformationKey.nextId

    // Only (intended to be) used by ProjectLike.
    // "Solves" the issue that Scala has no "package protected" visibility;
    // We wanted to make sure that the method "requirements" is (at least by default)
    // only visible in the subclasses as it is not intended to be called by objects
    // other than instances of `ProjectLike`.
    final private[analyses] def getRequirements: ProjectInformationKeys = requirements

    /**
     * Returns the information which other project information need to be available
     * before this analysis can be performed.
     *
     * If the analysis has no special requirements `Nil` can be returned.
     *
     * @note   '''All requirements must be listed; failing to specify a requirement can
     *         result in a deadlock.'''
     *
     * @note   Classes/Objects that implement this trait should not make the method `public`
     *         to avoid that this method is called accidentally by regular user code.
     */
    /*ABSTRACT*/ protected def requirements: ProjectInformationKeys

    // Only (intended to be) used by Project.
    // "Solves" the issue that Scala has no "package protected" visibility;
    // We wanted to make sure that the method "compute" is (at least by default)
    // only visible in the subclasses as it is not intended to be called by objects
    // other than instances of `Project`.
    final private[analyses] def doCompute(project: SomeProject): T = compute(project)

    /**
     * Computes the information for the given project.
     *
     * @note Classes that inherit from this trait are ''not'' expected to
     *      make this method public. This method is only expected to be called
     *      by an instance of a `Project`.
     */
    /*ABSTRACT*/ protected def compute(project: SomeProject): T

}

/**
 * Private companion object of ProjectInformationKey that is required to associate
 * project information objects with unique ids.
 *
 * @author Michael Eichberg
 */
private object ProjectInformationKey {

    private[this] val idGenerator = new AtomicInteger(0)

    private[ProjectInformationKey] def nextId: Int = idGenerator.getAndIncrement()

}
