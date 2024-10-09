/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.ifds.integration

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.ide.ifds.integration.IFDSPropertyMetaInformation
import org.opalj.ide.ifds.problem.IFDSProblem
import org.opalj.ide.ifds.problem.IFDSValue
import org.opalj.ide.problem.IDEFact
import org.opalj.tac.fpcf.analyses.ide.integration.JavaIDEAnalysisScheduler
import org.opalj.tac.fpcf.analyses.ide.solver.JavaStatement

/**
 * Specialized IDE analysis scheduler for IFDS problems with Java programs
 */
abstract class JavaIFDSAnalysisScheduler[Fact <: IDEFact] extends JavaIDEAnalysisScheduler[Fact, IFDSValue] {
    override def propertyMetaInformation: IFDSPropertyMetaInformation[Fact]

    override def createProblem(project: SomeProject): IFDSProblem[Fact, JavaStatement, Method]
}
