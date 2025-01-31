/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.ifds.integration

import org.opalj.br.Method
import org.opalj.ide.ifds.integration.IFDSPropertyMetaInformation
import org.opalj.ide.problem.IDEFact
import org.opalj.tac.fpcf.analyses.ide.solver.JavaStatement

/**
 * Specialized property meta information for IFDS problems with Java programs
 */
trait JavaIFDSPropertyMetaInformation[Fact <: IDEFact] extends IFDSPropertyMetaInformation[Fact, JavaStatement, Method]
