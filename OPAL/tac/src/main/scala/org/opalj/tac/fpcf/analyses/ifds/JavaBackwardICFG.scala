/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package ifds

import org.opalj.br.analyses.SomeProject

/**
 * An ICFG for a Java IFDS backwards analysis.
 *
 * @param project the project to which the ICFG belongs.
 *
 * @author Nicolas Gross
 */
class JavaBackwardICFG(project: SomeProject) extends org.opalj.tac.fpcf.analyses.ide.solver.JavaBackwardICFG(project)
    with JavaICFG {}
