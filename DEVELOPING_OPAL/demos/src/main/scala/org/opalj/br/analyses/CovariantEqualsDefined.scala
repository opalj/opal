/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import java.io.File
import java.net.URL

import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.issues.ClassLocation
import org.opalj.issues.Issue
import org.opalj.issues.IssueCategory
import org.opalj.issues.IssueKind
import org.opalj.issues.Relevance

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
object CovariantEqualsMethodDefined extends ProjectsAnalysisApplication {

    protected class CovariantEqualsConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args) {
        val description = "Finds classes that just define a co-variant equals method"
    }

    protected type ConfigType = CovariantEqualsConfig

    protected def createConfig(args: Array[String]): CovariantEqualsConfig = new CovariantEqualsConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: CovariantEqualsConfig,
        execution:      Int
    ): (Project[URL], BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)

        val mutex = new Object
        var reports = List[Issue]()

        project.parForeachClassFile() { classFile =>
            var definesEqualsMethod = false
            var definesCovariantEqualsMethod = false
            for (case Method(_, "equals", MethodDescriptor(Seq(ct), BooleanType)) <- classFile.methods)
                if (ct == ClassType.Object)
                    definesEqualsMethod = true
                else
                    definesCovariantEqualsMethod = true

            if (definesCovariantEqualsMethod && !definesEqualsMethod) {
                mutex.synchronized {
                    reports = Issue(
                        "CovariantEqualsMethodDefined",
                        Relevance.Moderate,
                        summary =
                            "defines a covariant equals method, but does not also define the standard equals method",
                        categories = Set(IssueCategory.Correctness),
                        kinds = Set(IssueKind.MethodMissing),
                        locations = List(new ClassLocation(None, project, classFile))
                    ) :: reports
                }
            }
        }
        (project, BasicReport(reports.map(_.toAnsiColoredString).mkString("\n")))
    }
}
