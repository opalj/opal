/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import org.opalj.log.LogContext

/**
 * Common super trait of all analyses that use the fixpoint
 * computations framework. In general, an analysis computes a
 * [[org.opalj.fpcf.Property]] by processing some entities, e.g.: ´classes´, ´methods´
 * or ´fields´.
 *
 * @author Michael Reif
 * @author Michael Eichberg
 */
trait ProjectBasedAnalysis {

    implicit val project: SomeProject
    final def p = project

    implicit final def classHierarchy = project.classHierarchy
    final def ch = classHierarchy

    implicit final def logContext: LogContext = project.logContext

}
