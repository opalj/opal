/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.ide.lcp_on_fields

import org.opalj.tac.fpcf.analyses.ide.instances.lcp_on_fields.LCPOnFieldsAnalysisScheduler
import org.opalj.tac.fpcf.analyses.ide.integration.JavaIDEAnalysisSchedulerBase

object LCPOnFieldsAnalysisScheduler
    extends LCPOnFieldsAnalysisScheduler with JavaIDEAnalysisSchedulerBase.RTACallGraph
