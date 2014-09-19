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

import AnalysesHelpers._
import br._
import br.analyses._
import br.instructions._

/**
 * This analysis reports `clone()` methods that do not contain a call to `super.clone`.
 * Such `clone()` methods are probably (but not necessarily) implemented incorrectly.
 *
 * In order to satisfy the standard `clone()` contract, `clone()` must return an object of
 * the same class it was called on. If every `clone()` method in an inheritance hierarchy
 * calls `super.clone()`, then `Object.clone()` will eventually be called and create an
 * object of the proper type.
 *
 * If a clone() method does not rely on `Object.clone()` to create the new object, but
 * instead invokes a constructor of some class, it will always return an object of that
 * class, regardless of whether it was actually called on a object of that type.
 *
 * This can cause problems if this `clone()` is inherited by a subclass, because then
 * calling `clone()` on the subclass returns an instance of the superclass, instead of
 * an instance of the subclass as it should. This violates the `clone()` contract. If the
 * `clone()` method would call `super.clone()` instead of a constructor, then this issue
 * would not happen, because `Object.clone()` would have created an object of the proper
 * type.
 *
 * Of course, if the subclass implements `clone()` by creating the proper object itself,
 * or the superclass with the suspicious `clone()` is `final` so that there cannot be any
 * subclasses, then the mentioned problem can never occur.
 *
 * TODO: Ideas to improve this analysis:
 * - It currently only checks whether a call to `super.clone()` exists inside `clone()`,
 *   but not whether it is actually reached/executed on all possible code paths.
 * - It produces false-positives when super.clone() is called indirectly through a helper
 *   method.
 * - It produces false-positives in cases where super.clone() doesn't need to be called:
 *   - if there are no subclasses,
 *   - or if the class is `final` so that there can be no subclasses,
 *   - or if all subclasses implement clone() themselves without calling super.clone(),
 *     i.e. the subclasses themselves take care of creating the proper object, so the
 *     superclass's clone() doesn't need to make sure to eventually call Object.clone() by
 *     calling super.clone().
 *
 * @author Ralf Mitschke
 * @author Daniel Klauer
 */
class CloneDoesNotCallSuperClone[Source] extends FindRealBugsAnalysis[Source] {

    override def description: String =
        "Reports clone() methods that do not contain a call to super.clone()."

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

        // For each clone() methods that doesn't contain a call to super.clone()...
        for {
            classFile ← project.classFiles
            if !project.isLibraryType(classFile)
            if !classFile.isInterfaceDeclaration && !classFile.isAnnotationDeclaration
            superClass ← classFile.superclassType.toSeq
            method @ Method(_, "clone", NoArgsAndReturnObject) ← classFile.methods
            if method.body.isDefined
            if !method.body.get.instructions.exists {
                case INVOKESPECIAL(`superClass`, "clone", NoArgsAndReturnObject) ⇒ true
                case _ ⇒ false
            }
        } yield {
            MethodBasedReport(
                project.source(classFile.thisType),
                Severity.Warning,
                classFile.thisType,
                method,
                "Missing call to super.clone()")
        }
    }
}
