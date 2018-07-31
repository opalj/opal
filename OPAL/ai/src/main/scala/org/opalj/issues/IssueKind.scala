/* BSD 2-Clause License - see OPAL/LICENSE for details. */
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
