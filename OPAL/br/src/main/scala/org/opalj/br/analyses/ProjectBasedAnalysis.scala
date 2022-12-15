/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import org.opalj.si.FPCFAnalysis

/**
 * Common super trait of all analyses that use the fixpoint
 * computations framework. In general, an analysis computes a
 * [[org.opalj.fpcf.Property]] by processing some entities, e.g.: ´classes´, ´methods´
 * or ´fields´.
 *
 * @author Michael Reif
 * @author Michael Eichberg
 */
trait ProjectBasedAnalysis extends FPCFAnalysis {

    val project: SomeProject
    override implicit def p: SomeProject = project
    implicit final def classHierarchy: ClassHierarchy = project.classHierarchy
    final def ch: ClassHierarchy = classHierarchy

}

abstract class DefaultFPCFAnalysis(val project: SomeProject) extends ProjectBasedAnalysis
