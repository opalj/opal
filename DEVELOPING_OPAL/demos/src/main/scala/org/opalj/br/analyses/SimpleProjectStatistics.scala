/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import java.net.URL

/**
 * Demonstrates how to collect some statistics about the analyzed project
 * (created for Entwicklertag 2015 in Frankfurt).
 *
 * @author Michael Eichberg
 */
object SimpleProjectStatistics extends ProjectAnalysisApplication {

    override def title: String = "collects project statistics"

    override def description: String = "collects basic size metrics about a project"

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {

        // the following is highly inefficient

        val classFilesDistribution =
            project.allClassFiles.
                groupBy(cf => org.opalj.bi.jdkVersion(cf.majorVersion)).toSeq.
                sortWith((l, r) => l._1 < r._1).
                map { e =>
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

        val (longestMethodInAClass, theLongestMethod) =
            {
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

        val (methodWithMostRegisterVariableInAClass, theMethodWithTheMostLocalVariables) =
            {
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

        BasicReport(
            classFilesDistribution.mkString("classFilesDistribution:\n\t", "\n\t", "\n")+
                "maxInstanceFieldsInAClass: "+
                maxInstanceFieldsInAClass+"("+classWithMaxInstanceFields+")\n"+
                "maxClassFieldsInAClass: "+
                maxClassFieldsInAClass+"("+classWithMaxClassFields+")\n"+
                "maxMethodsInAClass: "+
                maxMethodsInAClass+"("+classWithMaxMethods+")\n"+
                "longestMethodInAClass: "+
                longestMethodInAClass+"("+theLongestMethod+")\n"+
                "methodWithMostRegisterVariableInAClass: "+
                methodWithMostRegisterVariableInAClass+"("+theMethodWithTheMostLocalVariables+")\n"
        )
    }
}
