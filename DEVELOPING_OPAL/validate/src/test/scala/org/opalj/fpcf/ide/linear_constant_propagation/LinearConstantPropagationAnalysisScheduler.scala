/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.ide.linear_constant_propagation

import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.LinearConstantPropagationAnalysisScheduler
import org.opalj.tac.fpcf.analyses.ide.integration.JavaIDEAnalysisScheduler

object LinearConstantPropagationAnalysisScheduler
    extends LinearConstantPropagationAnalysisScheduler with JavaIDEAnalysisScheduler.RTACallGraph
