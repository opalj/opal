/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.ifds.integration

import org.opalj.br.analyses.SomeProject
import org.opalj.ide.ifds.problem.IFDSValue
import org.opalj.ide.problem.IDEFact
import org.opalj.tac.fpcf.analyses.ide.ifds.problem.JavaIFDSProblem
import org.opalj.tac.fpcf.analyses.ide.integration.JavaIDEAnalysisSchedulerBase
import org.opalj.tac.fpcf.analyses.ide.solver.JavaICFG

/**
 * Specialized IDE analysis scheduler for IFDS problems with Java programs
 */
abstract class JavaIFDSAnalysisScheduler[Fact <: IDEFact] extends JavaIDEAnalysisSchedulerBase[Fact, IFDSValue] {
    override def propertyMetaInformation: JavaIFDSPropertyMetaInformation[Fact]

    override def createProblem(project: SomeProject, icfg: JavaICFG): JavaIFDSProblem[Fact]
}
