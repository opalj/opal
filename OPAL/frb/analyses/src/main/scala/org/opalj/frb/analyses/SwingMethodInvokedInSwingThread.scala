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

import br._
import br.analyses._
import br.instructions._

/**
 * This analysis reports calls to `show()`, `pack()` or `setVisible()` methods on
 * `javax/swing/` objects.
 *
 * These methods should only be called from inside the Swing background thread [1], since
 * they themselves can cause events to be delivered. If they were called from outside the
 * Swing thread, there could be dead locks etc.
 *
 * The proper way to call these methods is to implement a helper `java.lang.Runnable`
 * class that does it, and pass an instance of it to `java.awt.EventQueue.invokeLater()`.
 *
 * [1]: [[http://en.wikipedia.org/wiki/Event_dispatching_thread]]
 *
 * @author Ralf Mitschke
 * @author Peter Spieler
 */
class SwingMethodInvokedInSwingThread[Source]
        extends MultipleResultsAnalysis[Source, MethodBasedReport[Source]] {

    def description: String =
        "Reports calls to certain Swing methods made from outside of the Swing thread."

    /**
     * Runs this analysis on the given project. Reports the method calling swing functions
     * outside of swing thread.
     *
     * @param project The project to analyze.
     * @param parameters Options for the analysis. Currently unused.
     * @return A list of reports, or an empty list.
     */
    def analyze(
        project: Project[Source],
        parameters: Seq[String] = List.empty): Iterable[MethodBasedReport[Source]] = {

        // Look for INVOKEVIRTUAL calls to show/pack/setVisible() methods on javax/swing/
        // objects from inside public static main() or methods containing "benchmark" in
        // their name.
        for {
            classFile ← project.classFiles
            if !project.isLibraryType(classFile)
            method @ MethodWithBody(body) ← classFile.methods
            if (method.isPublic &&
                method.isStatic &&
                method.name == "main") ||
                (classFile.thisType.fqn.toLowerCase.indexOf("benchmark") >= 0)
            (idx, INVOKEVIRTUAL(targetType, name, desc)) ← body.associateWithIndex
            if targetType.isObjectType &&
                targetType.asObjectType.fqn.startsWith("javax/swing/")
            if ((name, desc) match {
                case ("show" | "pack", MethodDescriptor.NoArgsAndReturnVoid) ⇒ true
                case ("setVisible", MethodDescriptor(IndexedSeq(BooleanType),
                    VoidType)) ⇒ true
                case _ ⇒ false
            })
        } yield {
            MethodBasedReport(
                project.source(classFile.thisType),
                Severity.Error,
                classFile.thisType,
                method.descriptor,
                method.name,
                "Calls Swing methods while outside Swing thread")
        }
    }
}
