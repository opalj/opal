/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import org.opalj.fpcf.SomeEOptionP

trait AnalysisState {
    /**
     * Inherited classes that introduce new dependencies must override this method and call add a
     * call to super!
     */
    def hasOpenDependencies: Boolean

    /**
     * Inherited classes that introduce new dependencies must override this method and call add a
     * call to super!
     */
    def dependees: Set[SomeEOptionP]
}

trait BaseAnalysisState extends AnalysisState {
    def hasOpenDependencies: Boolean = false
    def dependees: Set[SomeEOptionP] = Set.empty
}