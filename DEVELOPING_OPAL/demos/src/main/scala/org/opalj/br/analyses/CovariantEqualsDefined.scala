/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import java.net.URL
import org.opalj.issues.Relevance
import org.opalj.issues.Issue
import org.opalj.issues.IssueCategory
import org.opalj.issues.IssueKind
import org.opalj.issues.ClassLocation

/**
 * Finds classes that define only a co-variant `equals` method (an equals method
 * where the parameter type is a subtype of `java.lang.Object`), but which do not
 * also define a "standard" `equals` method.
 *
 * ==Implementation Note==
 * This analysis is implemented using a traditional approach where each analysis
 * analyzes the project's resources on its own and fully controls the process.
 *
 * @author Michael Eichberg
 */
object CovariantEqualsMethodDefined extends ProjectAnalysisApplication {

    //
    // Meta-data
    //

    override def description: String = "Finds classes that just define a co-variant equals method."

    //
    // Implementation
    //

    def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {

        val mutex = new Object
        var reports = List[Issue]()

        project.parForeachClassFile(isInterrupted) { classFile =>
            var definesEqualsMethod = false
            var definesCovariantEqualsMethod = false
            for (Method(_, "equals", MethodDescriptor(Seq(ot), BooleanType)) <- classFile.methods)
                if (ot == ObjectType.Object)
                    definesEqualsMethod = true
                else
                    definesCovariantEqualsMethod = true

            if (definesCovariantEqualsMethod && !definesEqualsMethod) {
                mutex.synchronized {
                    reports = Issue(
                        "CovariantEqualsMethodDefined",
                        Relevance.Moderate,
                        summary = "defines a covariant equals method, but does not also define the standard equals method",
                        categories = Set(IssueCategory.Correctness),
                        kinds = Set(IssueKind.MethodMissing),
                        locations = List(new ClassLocation(None, project, classFile))
                    ) :: reports
                }
            }
        }
        reports.map(_.toAnsiColoredString).mkString("\n")
    }
}
