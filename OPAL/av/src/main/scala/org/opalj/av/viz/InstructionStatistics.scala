/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package av
package viz

import java.net.URL

import org.opalj.br._
import org.opalj.br.analyses.AnalysisApplication
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.OneStepAnalysis
import org.opalj.br.analyses.Project

/**
 * Counts the number of instructions aggregated per package.
 *
 * @see [[http://philogb.github.io/jit/ for details regarding the visualization]]
 * @author Michael Eichberg
 */

object InstructionStatistics extends AnalysisApplication {

    val analysis = new OneStepAnalysis[URL, BasicReport] {

        override def description: String =
            "Collects information about the number of instructions per package."

        def doAnalyze(
            project:       Project[URL],
            parameters:    Seq[String],
            isInterrupted: () => Boolean
        ): BasicReport = {

            import scala.collection.mutable.HashMap
            import scala.collection.mutable.HashSet

            // Collect the number of instructions per package
            // FQPN = FullyQualifiedPackageName
            val instructionsPerFQPN = HashMap.empty[String, Int]
            for {
                classFile <- project.allClassFiles
                packageName = classFile.thisType.packageName
                MethodWithBody(body) <- classFile.methods
            } {
                instructionsPerFQPN.update(
                    packageName,
                    instructionsPerFQPN.getOrElse(packageName, 0) + body.instructionsCount
                )
            }

            if (isInterrupted())
                return null

            def processSubPackages(
                rootFQPN: String,
                childPNs: scala.collection.Set[String]
            ): (String, Int) = {

                println("PSP::::::::RootFQPN:"+rootFQPN+"  -  ChildPNs:"+childPNs)

                if (childPNs.nonEmpty) {
                    val childPackages =
                        for { childPN <- childPNs } yield {
                            processPackage(
                                childPN,
                                (
                                    if (rootFQPN.length() == 0)
                                        childPN
                                    else
                                        childPN.substring(rootFQPN.length() + 1)
                                ).replace('/', '.')
                            )
                        }
                    (
                        childPackages.view.map(_._1).mkString(",\"children\": [{\n", "},{\n", "}]\n"),
                        childPackages.view.map(_._2).sum
                    )
                } else {
                    ("", 0)
                }
            }

            def processPackage(
                rootFQPN: String,
                spn:      String
            ): (String, Int) = {

                // Find all immediate child packages. Note that a child package's name
                // can contain multiple simple package names if the intermediate
                // packages have only one subpackage.
                val childPNs = HashSet.empty[String]
                for {
                    fqpn <- instructionsPerFQPN.keys
                    if fqpn.length > rootFQPN.length()
                    if fqpn.startsWith(rootFQPN)
                    if fqpn.charAt(rootFQPN.length()) == '/' // javax is not a subpackage of java..
                } {
                    val pnsToRemove = HashSet.empty[String]
                    var pnNeedToBeAdded = true
                    for (childPN <- childPNs) {
                        if (childPN.startsWith(fqpn)) {
                            pnsToRemove += childPN
                        } else if (fqpn.startsWith(childPN)) {
                            pnNeedToBeAdded = false
                        }
                    }
                    childPNs --= pnsToRemove
                    if (pnNeedToBeAdded) childPNs += fqpn
                }

                println("PP:::::::::RootFQPN:"+rootFQPN+"  -  SPN:"+spn+"  -  ChildPNs:"+childPNs)

                val (children, instructionsInSubPackages) =
                    processSubPackages(rootFQPN, childPNs)

                val instructionsInPackage = instructionsPerFQPN.getOrElse(rootFQPN, 0)
                val allInstructions = instructionsInPackage + instructionsInSubPackages

                val normalizedInstructions = Math.max(allInstructions, 1)
                val color = if (instructionsInPackage == 0) "#8080b0" else "#80c080"

                (s""""id": "$rootFQPN",
                   "name": "$spn ∑$allInstructions ($instructionsInPackage)",
                   "data": {
                        "$$area": $normalizedInstructions,
                        "$$dim": $normalizedInstructions,
                        "$$color": "$color"
                    }"""+children,
                    allInstructions
                )
            }

            val theProjectStatistics = {
                val rootPNs = instructionsPerFQPN.keys.map(_.split('/').head).toSet
                val (children, instructionsInSubPackages) =
                    processSubPackages("", rootPNs)
                s""""id": "<all_packages>",
                   "name": "<All Packages>:$instructionsInSubPackages",
                   "data": {
                        "$$area": ${Math.max(instructionsInSubPackages, 1)},
                        "$$dim": ${Math.max(instructionsInSubPackages, 1)},
                        "$$color": "#3030b0"
                    }"""+
                    children
            }

            // print out the JSON!
            BasicReport(theProjectStatistics)
        }
    }
}
