/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package ide
package lcp_on_fields

import org.opalj.tac.fpcf.analyses.ide.instances.lcp_on_fields.LCPOnFieldsAnalysisScheduler
import org.opalj.tac.fpcf.analyses.ide.integration.JavaIDEAnalysisSchedulerBase

/**
 * Scheduler for [[org.opalj.tac.fpcf.analyses.ide.instances.lcp_on_fields.problem.LCPOnFieldsProblem]] for Java using
 * RTA call graph.
 *
 * @author Robin Körkemeier
 */
object LCPOnFieldsAnalysisScheduler
    extends LCPOnFieldsAnalysisScheduler with JavaIDEAnalysisSchedulerBase.RTACallGraph
