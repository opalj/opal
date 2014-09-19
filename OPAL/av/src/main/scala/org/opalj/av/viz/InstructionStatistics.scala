/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package av
package viz

import java.net.URL

import br._
import br.instructions._
import br.analyses.{ OneStepAnalysis, AnalysisExecutor, BasicReport, Project }

/**
 * Counts the number of instructions aggregated per package.
 *
 * @see [[http://philogb.github.io/jit/ for details regarding the visualization]]
 * @author Michael Eichberg
 */

object InstructionStatistics extends AnalysisExecutor {

    val analysis = new OneStepAnalysis[URL, BasicReport] {

        override def description: String =
            "Collects information about the number of instructions per package."

        def doAnalyze(
            project: Project[URL],
            parameters: Seq[String],
            isInterrupted: () ⇒ Boolean): BasicReport = {

            import scala.collection.mutable.{ HashSet, HashMap }

            // Collect the number of instructions per package 
            // FQPN = FullyQualifiedPackageName
            val instructionsPerFQPN = HashMap.empty[String, Int]
            for {
                classFile ← project.classFiles
                packageName = classFile.thisType.packageName
                MethodWithBody(body) ← classFile.methods
            } {
                instructionsPerFQPN.update(packageName,
                    instructionsPerFQPN.getOrElse(packageName, 0) +
                        body.programCounters.size
                )
            }

            if (isInterrupted())
                return null

            def processSubPackages(
                rootFQPN: String,
                childPNs: scala.collection.Set[String]): (String, Int) = {

                println("PSP::::::::RootFQPN:"+rootFQPN+"  -  ChildPNs:"+childPNs)

                if (childPNs.nonEmpty) {
                    val childPackages =
                        for { childPN ← childPNs } yield {
                            processPackage(
                                childPN,
                                (
                                    if (rootFQPN.length() == 0)
                                        childPN
                                    else
                                        childPN.substring(rootFQPN.length() + 1)
                                ).replace('/', '.'))
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
                spn: String): (String, Int) = {

                // Find all immediate child packages. Note that a child package's name 
                // can contain multiple simple package names if the intermediate
                // packages have only one subpackage.
                var childPNs = HashSet.empty[String]
                for {
                    fqpn ← instructionsPerFQPN.keys
                    if fqpn.length > rootFQPN.length()
                    if fqpn.startsWith(rootFQPN)
                    if fqpn.charAt(rootFQPN.length()) == '/' // javax is not a subpackage of java..
                } {
                    var pnsToRemove = HashSet.empty[String]
                    var pnNeedToBeAdded = true
                    for (childPN ← childPNs) {
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