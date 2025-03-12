/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package ide
package lcp_on_fields

import org.opalj.tac.fpcf.analyses.ide.instances.lcp_on_fields.LinearConstantPropagationAnalysisSchedulerExtended
import org.opalj.tac.fpcf.analyses.ide.integration.JavaIDEAnalysisSchedulerBase

object LinearConstantPropagationAnalysisSchedulerExtended
    extends LinearConstantPropagationAnalysisSchedulerExtended with JavaIDEAnalysisSchedulerBase.RTACallGraph
