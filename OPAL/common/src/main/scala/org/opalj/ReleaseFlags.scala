/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj

/**
 * Defines `inline val`s that control compile-time behavior for production builds. This file is automatically rewritten,
 * replacing all `false` flags with `true` when compiling non-SNAPSHOT builds.
 * For now, this determines whether assertions ([[org.opalj.util.elidedAssert]]) are to be elided during compilation.
 */
object ReleaseFlags {
    /** Elide assertions ([[org.opalj.util.elidedAssert]]) during compilation. */
    inline val elideAssertions = false
}
