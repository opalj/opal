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
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * Common type definitions and constants used by the fixpoint computations framework.
 *
 * @author Michael Eichberg
 */
package object fp {

    type PropertyComputation = (AnyRef) ⇒ PropertyComputationResult

    /**
     * A computation of a property that was restarted (under different results)
     * yielded the same result.
     */
    final val Unchanged = NoResult

    /**
     * Computing a property for the a specific element is not/never possible.
     */
    final val Impossible = NoResult

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
     * The underlying assumption is that we don't have a property for each available
     * property key.
     */
    private[fp]type Properties = mutable.HashMap[PropertyKey, (Property, Observers)]

    /**
     * The type of the value associated with each element (key) found in the store.
     *
     * We use one reentrant read/write lock for all properties associated with a
     * single element in the property store. We did not use one lock per property
     * and per entity.
     */
    private[fp]type PropertyStoreValue = (ReentrantReadWriteLock, Properties)

}

