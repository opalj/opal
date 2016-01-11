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
package bugpicker
package core

/**
 * Collection of predefined issue categories where
 * each category describes '''the quality property of the software that is
 * affected''' by the issue.
 *
 * @author Michael Eichberg
 */
object IssueCategory {

    final val AllCategories = {
        Set(Bug, Smell, Performance, Comprehensibility)
    }

    /**
     * We identify something as a "Bug" if and only if we are certain that
     * the runtime execution is most likely not what the developer intended.
     *
     * @note Affected software quality attribute: Correctness
     */
    final val Bug = "bug"

    /**
     * We identify something as a "Smell" if we believe that
     * the runtime execution is probably not what the developer intended.
     *
     * @note Affected software quality attribute: Correctness
     */
    final val Smell = "smell"

    /**
     * Performance issues are generally related to code that does things that
     * are superfluous; i.e., which - except from using time and memory - have
     * no meaningful sideeffect compared to a simpler solution.
     *
     * @note Affected software quality attribute: Efficiency
     */
    final val Performance = "performance"

    /**
     * Code that most likely does what the developer wanted it to do, but
     * which is too clumsy and can be shortened.
     *
     * @note Affected software quality attribute: Efficiency
     */
    final val Comprehensibility = "comprehensibility"

}
