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
package frb
package analyses

import bi.ACC_PUBLIC

import br._
import br.analyses._

/**
 * This analysis reports `finalize()` methods that are `public` instead of `protected`.
 * `finalize()` should never be `public` as it is not intended to be called manually by
 * outside code.
 *
 * @author Ralf Mitschke
 * @author Michael Eichberg
 * @author Daniel Klauer
 */
class PublicFinalizeMethodShouldBeProtected[Source] extends FindRealBugsAnalysis[Source] {

    override def description: String = "Reports finalize() methods that are not protected."

    /**
     * Runs this analysis on the given project.
     *
     * @param project The project to analyze.
     * @param parameters Options for the analysis. Currently unused.
     * @return A list of reports, or an empty list.
     */
    def doAnalyze(
        project: Project[Source],
        parameters: Seq[String] = List.empty,
        isInterrupted: () ⇒ Boolean): Iterable[MethodBasedReport[Source]] = {

        // For all public finalize() methods...
        for {
            classFile ← project.classFiles
            if !project.isLibraryType(classFile)
            method @ Method(ACC_PUBLIC(), "finalize",
                MethodDescriptor.NoArgsAndReturnVoid) ← classFile.methods
        } yield {
            MethodBasedReport(
                project.source(classFile.thisType),
                Severity.Info,
                classFile.thisType,
                method,
                "Should be protected")
        }
    }
}
