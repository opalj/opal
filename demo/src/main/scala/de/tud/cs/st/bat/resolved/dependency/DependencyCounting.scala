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
package de.tud.cs.st
package bat
package resolved
package dependency

import analyses._

import java.net.URL

/**
 * Counts the number of dependencies found in a project.
 *
 * @author Michael Eichberg
 */
object DependencyCounting extends AnalysisExecutor with Analysis[URL, BasicReport] {

    val analysis = this

    def description: String = "Counts the number of inter-source element dependencies."

    def analyze(project: Project[URL], parameters: Seq[String]) = {
        println("Press enter to start the dependency collection.")
        scala.io.StdIn.readLine
        
        import util.debug.PerformanceEvaluation._
        val counter = time {
            val counter = new DependencyCountingDependencyProcessor with FilterSelfDependencies
            val extractor = new DependencyExtractor(counter)
            // process the class files in parallel to speed up the collection process
            project.classFiles.par foreach (extractor.process)
            counter
        } { t ⇒
            println(f"[info] Time to count the dependencies: ${ns2sec(t)}%2.2f secs.")
        }

        BasicReport(
            (f"Number of inter source-element dependencies: ${counter.currentDependencyCount}%,9d%n") +
                f"Number of dependencies on primitive types:   ${counter.currentDependencyOnPrimitivesCount}%,9d%n"+
                f"Number of dependencies on array types:       ${counter.currentDependencyOnArraysCount}%,9d%n"
        )
    }
}