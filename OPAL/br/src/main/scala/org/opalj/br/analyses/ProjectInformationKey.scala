/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import java.util.concurrent.atomic.AtomicInteger

/**
 * `ProjectInformationKey` objects are used to get/associate some
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
    /*ABSTRACT*/ def requirements(project: SomeProject): ProjectInformationKeys

    /**
     * Computes the information for the given project.
     *
     * @note Classes that inherit from this trait are ''not'' expected to
     *      make this method public. This method is only expected to be called
     *      by an instance of a `Project`.
     */
    /*ABSTRACT*/ def compute(project: SomeProject): T

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
