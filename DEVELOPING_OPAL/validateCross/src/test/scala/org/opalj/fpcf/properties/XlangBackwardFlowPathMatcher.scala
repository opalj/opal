/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties

import org.opalj.ll.fpcf.analyses.ifds.taint.JavaBackwardTaintAnalysisScheduler

class XlangBackwardFlowPathMatcher extends AbstractBackwardFlowPathMatcher(JavaBackwardTaintAnalysisScheduler.property.key)