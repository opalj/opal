/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bugpicker
package core
package analyses

import org.opalj.bugpicker.core.analyses.BugPickerAnalysis._
import org.opalj.util.Milliseconds

case class AnalysisParameters(
        maxEvalTime:                   Milliseconds = DefaultMaxEvalTime,
        maxEvalFactor:                 Double       = DefaultMaxEvalFactor,
        maxCardinalityOfIntegerRanges: Long         = DefaultMaxCardinalityOfIntegerRanges,
        maxCardinalityOfLongSets:      Int          = DefaultMaxCardinalityOfLongSets,
        maxCallChainLength:            Int          = DefaultMaxCallChainLength,
        fixpointAnalyses:              Seq[String]  = DefaultFixpointAnalyses
) {

    def toStringParameters: Seq[String] = Seq(
        s"-maxEvalFactor=$maxEvalFactor",
        s"-maxEvalTime=$maxEvalTime",
        s"-maxCardinalityOfIntegerRanges=$maxCardinalityOfIntegerRanges",
        s"-maxCardinalityOfLongSets=$maxCardinalityOfLongSets",
        s"-maxCallChainLength=$maxCallChainLength",
        s"-fixpointAnalyses=${fixpointAnalyses.mkString(";")}"
    )
}