/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package ide
package ifds
package problem

import org.opalj.br.Method
import org.opalj.ide.ifds.problem.IFDSProblem
import org.opalj.ide.problem.IDEFact
import org.opalj.tac.fpcf.analyses.ide.solver.JavaStatement

/**
 * Specialized IFDS problem for Java programs based on an IDE problem.
 *
 * @author Robin Körkemeier
 */
abstract class JavaIFDSProblem[Fact <: IDEFact] extends IFDSProblem[Fact, JavaStatement, Method]
