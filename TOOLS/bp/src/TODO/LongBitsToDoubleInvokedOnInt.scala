/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package frb
package analyses

import br._
import br.analyses._
import br.instructions._

/**
 * This analysis reports code that passes a 32bit `int` to `Double.longBitsToDouble()`,
 * which takes a 64bit `long`. Such code is broken, because an `int` is too small to hold
 * a `double`'s bit pattern.
 *
 * The conversion from `int` to `long` may change the bit pattern (sign extension), and
 * even if that doesn't happen the outcome won't necessarily be intended/expected. Perhaps
 * `float` should have been used, instead of `double` - or `long` instead of `int`.
 *
 * @author Ralf Mitschke
 * @author Daniel Klauer
 */
class LongBitsToDoubleInvokedOnInt[Source] extends FindRealBugsAnalysis[Source] {

    /**
     * Returns a description text for this analysis.
     * @return analysis description
     */
    override def description: String =
        "Reports code passing ints to Double.longBitsToDouble(long)"

    private val doubleType = ObjectType("java/lang/Double")
    private val longBitsToDoubleDescriptor =
        MethodDescriptor(IndexedSeq(LongType), DoubleType)

    /**
     * Runs this analysis on the given project.
     *
     * @param project The project to analyze.
     * @param parameters Options for the analysis. Currently unused.
     * @return A list of reports, or an empty list.
     */
    def doAnalyze(
        project:       Project[Source],
        parameters:    Seq[String]     = List.empty,
        isInterrupted: () => Boolean
    ): Iterable[LineAndColumnBasedReport[Source]] = {

        // In all method bodies, look for occurrences of (I2L, INVOKESTATIC) instruction
        // sequences, where the INVOKESTATIC is a call to
        // java.lang.Double.longBitsToDouble().
        for {
            classFile <- project.allProjectClassFiles
            method @ MethodWithBody(body) <- classFile.methods
            pc <- body.matchPair {
                case (
                    I2L,
                    INVOKESTATIC(`doubleType`, "longBitsToDouble", `longBitsToDoubleDescriptor`)
                    ) => true
                case _ => false
            }
        } yield {
            LineAndColumnBasedReport(
                project.source(classFile.thisType),
                Severity.Error,
                classFile.thisType,
                method.descriptor,
                method.name,
                body.lineNumber(pc),
                None,
                "Passing int to Double.longBitsToDouble(long)"
            )
        }
    }
}
