/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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
package issues

/**
 * An issue kind describes how '''an issue manifests itself in the source code'''.
 *
 * @author Michael Eichberg
 */
// This is not an enumeration because this set is extensible by custom analyses.
object IssueKind {

    final val AllKinds = {
        Set(
            MethodMissing,
            ConstantComputation,
            DeadPath,
            ThrowsException,
            UnguardedUse,
            UnusedField,
            UnusedMethod,
            UselessComputation,
            DubiousMethodCall,
            DubiousMethodDefinition,
            InconsistentSynchronization

        )
    }

    /**
     * A method that should be implemented is missing.
     */
    final val MethodMissing = "method missing"

    /**
     * A computation that always returns the same value.
     */
    final val ConstantComputation = "constant computation"

    /**
     * A path in a program that will never be executed.
     */
    final val DeadPath = "dead path"

    /**
     * A statement, which is not a "throw statement", which always throws an exception.
     */
    final val ThrowsException = "throws exception"

    /**
     * Use of a local variable that is not guarded though usage are also guarded.
     *
     * @example
     * {{{
     * def m(o : Object){
     *     // guarded use
     *     if (o ne null) {
     *         println(o.toString)
     *     }
     *
     *     // unguarded use
     *     o.hashCode
     * }
     * }}}
     */
    final val UnguardedUse = "unguarded use"

    /**
     * The field is not used and cannot be used by 3rd part extensions.
     */
    final val UnusedField = "unused field"

    /**
     * The method is not used and cannot be used by 3rd part extensions.
     */
    final val UnusedMethod = "unused method"

    final val UnusedLocalVariable = "unused local variable"

    /**
     * Something is currently unused and cannot be used in the future.
     *
     * Useless is in particular related to the implementation of methods.
     */
    final val UselessComputation = "useless computation"

    /**
     * The Java Collection API is not used in the correct way/as intended.
     */
    final val JavaCollectionAPIMisusage = "Java collection API Misusage"

    final val MissingStaticModifier = "static modifier missing"

    /**
     * "a method is called that may have unexpected/unwanted behavior in the given context"
     */
    final val DubiousMethodCall = "dubious method call"

    final val DubiousMethodDefinition = "dubious method definition"

    final val InconsistentSynchronization = "inconsistent synchronization"
}
