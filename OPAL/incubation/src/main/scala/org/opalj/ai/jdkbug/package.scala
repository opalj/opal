/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import org.opalj.br.Method

/**
 * Contains definitions that are used by the elements specified in JDKBugs
 *
 * @author Lars Schulte
 */
package object jdkbug {

    /**
     * Set of ids (integer values) associated with the relevant parameters passed
     * to a method.
     */
    type RelevantParameters = Seq[Int]

    // Initialized (exactly once) by the "analyze" method of the main analysis class.
    protected[jdkbug] var restrictedPackages: Set[String] = null

    def definedInRestrictedPackage(packageName: String): Boolean = {
        restrictedPackages.exists((packageName+"/").startsWith(_))
    }
}

package jdkbug {

    case class CallStackEntry(method: Method)

}
