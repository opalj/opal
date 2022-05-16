/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package frb
package analyses

import br._
import br.analyses._

/**
 * This analysis reports violations of the contract defined in `java.lang.Object` w.r.t.
 * the methods `equals` and `hashcode`.
 *
 * @author Michael Eichberg
 */
class EqualsHashCodeContract[Source] extends FindRealBugsAnalysis[Source] {

    /**
     * Returns a description text for this analysis.
     * @return analysis description
     */
    override def description: String = "Finds violations of the equals-hashCode contract."

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
    ): Iterable[ClassBasedReport[Source]] = {

        val mutex = new Object
        var reports = List[ClassBasedReport[Source]]()

        for (classFile <- project.allProjectClassFiles) {
            var definesEqualsMethod = false
            var definesHashCodeMethod = false
            for (method <- classFile.methods) method match {
                case Method(_, "equals", MethodDescriptor(Seq(ObjectType.Object),
                    BooleanType)) =>
                    definesEqualsMethod = true
                case Method(_, "hashCode", MethodDescriptor(Seq(),
                    IntegerType)) =>
                    definesHashCodeMethod = true
                case _ =>
            }

            if (definesEqualsMethod != definesHashCodeMethod) {
                mutex.synchronized {
                    reports = ClassBasedReport(
                        project.source(classFile.thisType),
                        Severity.Error,
                        classFile.thisType,
                        "Does not satisfy java.lang.Object's equals-hashCode "+
                            "contract."
                    ) :: reports
                }
            }
        }
        reports
    }
}
