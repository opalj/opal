/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.InstantiableClasses
import org.opalj.fpcf.properties.NotInstantiable

/**
 * Analyzes which classes are instantiable.
 *
 * @author Michael Reif
 */
object LibraryInstantiableClassesAnalysis {

    def doAnalyze(project: SomeProject): InstantiableClasses = {
        val notInstantiableClasses = SimpleInstantiabilityAnalysis.run(project).collect {
            case EP(e, NotInstantiable) ⇒ e.thisType
        }

        new InstantiableClasses(project, notInstantiableClasses.toSet)
    }
}
