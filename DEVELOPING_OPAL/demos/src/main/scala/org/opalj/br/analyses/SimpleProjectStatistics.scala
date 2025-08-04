/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import java.io.File
import java.net.URL

import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig

/**
 * Demonstrates how to collect some statistics about the analyzed project
 * (created for Entwicklertag 2015 in Frankfurt).
 *
 * @author Michael Eichberg
 */
object SimpleProjectStatistics extends ProjectsAnalysisApplication {

    protected class StatisticsConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args) {
        val description = "Collects basic size metrics about a project"
    }

    protected type ConfigType = StatisticsConfig

    protected def createConfig(args: Array[String]): StatisticsConfig = new StatisticsConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: StatisticsConfig,
        execution:      Int
    ): (Project[URL], BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)

        // the following is highly inefficient

        val classFilesDistribution =
            project.allClassFiles
                .groupBy(cf => org.opalj.bi.jdkVersion(cf.majorVersion)).toSeq
                .sortWith((l, r) => l._1 < r._1)
                .map { e =>
                    val (group, es) = e
                    (group, es.size)
                }

        val maxInstanceFieldsInAClass =
            project.allClassFiles.map(_.fields.filter(f => !f.isStatic).size).max
        val classWithMaxInstanceFields =
            project.allClassFiles.find(
                _.fields.filter(f => !f.isStatic).size == maxInstanceFieldsInAClass
            ).map(_.thisType.toJava)

        val maxClassFieldsInAClass =
            project.allClassFiles.map(_.fields.filter(f => f.isStatic).size).max
        val classWithMaxClassFields =
            project.allClassFiles.find(
                _.fields.filter(f => f.isStatic).size == maxClassFieldsInAClass
            ).map(_.thisType.toJava)

        val maxMethodsInAClass =
            project.allClassFiles.map(_.methods.size).max
        val classWithMaxMethods =
            project.allClassFiles.find(
                _.methods.size == maxMethodsInAClass
            ).map(_.thisType.toJava)

        val (longestMethodInAClass, theLongestMethod) = {
            var max = 0
            var methodName: String = null
            for {
                classFile <- project.allClassFiles
                method <- classFile.methods
                if method.body.isDefined
                size = method.body.get.instructionsCount
                if size > max
            } {
                max = size
                methodName = method.toJava
            }

            (max, methodName)
        }

        val (methodWithMostRegisterVariableInAClass, theMethodWithTheMostLocalVariables) = {
            var max = 0
            var methodName: String = null

            for {
                classFile <- project.allClassFiles
                method <- classFile.methods
                if method.body.isDefined
                count = method.body.get.maxLocals
                if count > max
            } {
                max = count
                methodName = method.toJava
            }

            (max, methodName)
        }

        (
            project,
            BasicReport(
                classFilesDistribution.mkString("classFilesDistribution:\n\t", "\n\t", "\n") +
                    "maxInstanceFieldsInAClass: " +
                    maxInstanceFieldsInAClass + "(" + classWithMaxInstanceFields + ")\n" +
                    "maxClassFieldsInAClass: " +
                    maxClassFieldsInAClass + "(" + classWithMaxClassFields + ")\n" +
                    "maxMethodsInAClass: " +
                    maxMethodsInAClass + "(" + classWithMaxMethods + ")\n" +
                    "longestMethodInAClass: " +
                    longestMethodInAClass + "(" + theLongestMethod + ")\n" +
                    "methodWithMostRegisterVariableInAClass: " +
                    methodWithMostRegisterVariableInAClass + "(" + theMethodWithTheMostLocalVariables + ")\n"
            )
        )
    }
}
