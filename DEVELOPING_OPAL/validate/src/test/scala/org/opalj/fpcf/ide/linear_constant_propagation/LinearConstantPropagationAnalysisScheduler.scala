/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package ide
package linear_constant_propagation

import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.LinearConstantPropagationAnalysisScheduler
import org.opalj.tac.fpcf.analyses.ide.integration.JavaIDEAnalysisSchedulerBase

/**
 * Scheduler for
 * [[org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.LinearConstantPropagationProblem]]
 * for Java using RTA call graph.
 *
 * @author Robin Körkemeier
 */
object LinearConstantPropagationAnalysisScheduler
    extends LinearConstantPropagationAnalysisScheduler with JavaIDEAnalysisSchedulerBase.RTACallGraph
