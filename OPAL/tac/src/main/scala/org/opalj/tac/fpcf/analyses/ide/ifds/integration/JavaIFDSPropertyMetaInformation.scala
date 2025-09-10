/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package ide
package ifds
package integration

import org.opalj.br.Method
import org.opalj.ide.ifds.integration.IFDSPropertyMetaInformation
import org.opalj.ide.problem.IDEFact
import org.opalj.tac.fpcf.analyses.ide.solver.JavaStatement

/**
 * Specialized property meta information for IFDS problems with Java programs.
 *
 * @author Robin Körkemeier
 */
trait JavaIFDSPropertyMetaInformation[Fact <: IDEFact] extends IFDSPropertyMetaInformation[Fact, JavaStatement, Method]
