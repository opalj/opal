/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package collection

/**
 * Identifies a collection as being (guaranteed) complete or as being potentially incomplete.
 *
 * This class is typically used by analysis that derive some results and which are also able to
 * do so in case of incomplete information. But in the latter case the analysis may not be able
 * to determine whether the derived information is complete or not. For example, imagine you
 * are analyzing some library (but not the JDK). In this case the class hierarchy will be incomplete
 * and every analysis using it may compute incomplete information.
 *
 *
 * @author Michael Eichberg
 */
sealed trait PartialCollection[S] {

    /**
     * The underlying collection.
     */
    def s: S

    /**
     * Returns `true` if the underlying collection is guaranteed to contain all elements with
     * respect to some query/analysis. I.e., if the analysis is not conclusive, then `false`
     * is returned. However, it may still be the case that the underlying collection contains
     * all elements, but that cannot be deduced.
     */
    def isComplete: Boolean

    /**
     * Returns `true` if the underlying collection is not guaranteed to contain all elements (w.r.t.
     * some query/analysis/...
     */
    final def isIncomplete: Boolean = !isComplete
}

case class Complete[S](s: S) extends PartialCollection[S] { final val isComplete = true }

case class Incomplete[S](s: S) extends PartialCollection[S] { final val isComplete = false }
