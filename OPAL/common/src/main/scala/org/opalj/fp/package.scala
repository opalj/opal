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

import scala.collection.mutable
import org.opalj.collection.mutable.{ ArrayMap ⇒ OArrayMap }
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * The fixpoint computations framework is a general framework to perform fixpoint
 * computations on a fixed set of entities. The framework in particular
 * supports the development of static analyses. In this case, the fixpoint computations/
 * static analyses are generally operating on the code and need to be executed until
 * the computation has reached its (implicit) fixpoint. The fixpoint framework explicitly
 * support cyclic dependencies/computations.
 * A prime use case of the fixpoint framework
 * are all those analyses that may interact with the results of other analyses.
 *
 * For example, an analysis that analyses all field write access to determine if we can
 * refine a field's type (for the purpose of the analysis) can (reuse) the information
 * about the return types of methods, which however may depend on the refined field types.
 *
 * The framework is generic enough to facilitate the implementation of
 * anytime algorithms.
 *
 * @note The dependency relation is as follows:
 *      “A depends on B”
 *          `===`
 *      “B is depended on by A”
 *          `===`
 *      “A is the depender, B is the dependee”.
 *
 * @author Michael Eichberg
 */
package object fp {

    /**
     * A simple type alias to facilitate comprehension of the code.
     */
    type Entity = AnyRef

    /**
     * A function that takes an entity and returns a result. The result maybe:
     *  - the final derived property,
     *  - a function that will continue computing the result once the information
     *      about some other entity is available or,
     *  - an intermediate result.
     */
    type PropertyComputation = (Entity) ⇒ PropertyComputationResult

    /**
     * A function that continues the computation of a property. It takes:
     *  1. The current property that the computation computes/is going to refine.
     *  1. The entity + property of the entity on which the computation depends.
     */
    type Continuation = (Entity, Property) ⇒ PropertyComputationResult

    /**
     * A computation of a property that was restarted (under different properties)
     * yielded the same result.
     *
     * @note This is just an alias for [[NoResult]].
     */
    final val Unchanged: NoResult.type = NoResult

    /**
     * Computing a property for the a specific element is not/never possible.
     *
     * @note This is just an alias for [[NoResult]].
     */
    final val Impossible: NoResult.type = NoResult

    /**
     * The computation has no results (and there will be no results in the future!).
     *
     * @note This is just an alias for [[NoResult]].
     */
    final val Empty: NoResult.type = NoResult

    //
    //
    // IMPLEMENTATION SPECIFIC TYPES
    //
    //

    /**
     * The type of the observers that can be associated with a specific property
     * and element.
     */
    private[fp]type Observers = mutable.ListBuffer[PropertyObserver]

    /**
     * The type of the properties data structure that is associated with each
     * property.
     *
     * The property can be `null` if we have multiple analyses that are waiting for
     * the respective property.
     *
     * The underlying assumption is that not every property key is actually associated
     * with a property value for each element.
     */
    private[fp]type Properties = OArrayMap[(Property, Observers)]

    /**
     * The type of the value associated with each entity (key) found in the store.
     *
     * We use one reentrant read/write lock for all properties associated with a
     * single element in the property store.
     */
    private[fp]type PropertyStoreValue = (ReentrantReadWriteLock, Properties)

    private[fp]type UpdateType = UpdateTypes.Value

}

