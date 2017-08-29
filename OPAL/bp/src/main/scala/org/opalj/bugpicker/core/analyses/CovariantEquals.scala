/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
package bugpicker
package core
package analyses

import org.opalj.issues.Issue
import org.opalj.br.ClassFile
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.BooleanType
import org.opalj.br.IntegerType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.ObjectType
import org.opalj.issues.Issue
import org.opalj.issues.Relevance
import org.opalj.issues.IssueCategory
import org.opalj.issues.IssueKind
import org.opalj.issues.ClassLocation

/**
 * This analysis reports classes that have some `equals()` method(s), but not
 * `equals(Object)`. This is bad practice and can lead to unexpected behavior, because
 * without an `equals(Object)` method, `Object.equals(Object)` is not properly overridden.
 * However, the relevance is determined by the presence of a hashCode method.
 *
 * @author Daniel Klauer
 * @author Michael Eichberg
 */
object CovariantEquals {

    def description: String =
        "Reports classes with one (or more) equals() methods but without equals(Object)."

    /**
     * Checks whether a class has `equals()` methods but not `equals(Object)`.
     *
     * @param classFile The class to check.
     * @return Whether the class has `equals()` methods but not `equals(Object)`.
     */
    private def hasEqualsButNotEqualsObject(classFile: ClassFile): Boolean = {
        val paramTypes = classFile.methods.collect {
            case Method(_, "equals", MethodDescriptor(Seq(paramType), BooleanType)) ⇒ paramType
        }

        paramTypes.size > 0 && !paramTypes.exists(_ == ObjectType.Object)
    }

    private def hasHashCode(classFile: ClassFile): Boolean = {
        classFile.methods.exists {
            case Method(_, "hashCode", MethodDescriptor(Seq(), IntegerType)) ⇒ true
            case _ ⇒ false
        }
    }

    private def superClassHasCustomHashCode(
        classFile: ClassFile
    )(
        implicit
        project: SomeProject
    ): Boolean = {

        if (classFile.thisType eq ObjectType.Object)
            return false;

        val superclassType = classFile.superclassType.get
        if (superclassType eq ObjectType.Object)
            return false;

        import MethodDescriptor.JustReturnsInteger
        project.resolveClassMethodReference(superclassType, "hashCode", JustReturnsInteger) match {
            case Success(m) ⇒ m.classFile.thisType ne ObjectType.Object
            case _          ⇒ false
        }
    }

    /**
     * Runs this analysis on the given project.
     *
     * @param project The project to analyze.
     * @return A list of reports, or an empty list.
     */
    def apply(
        classFile: ClassFile
    )(
        implicit
        project: SomeProject
    ): Iterable[Issue] = {
        if (classFile.isInterfaceDeclaration)
            return Iterable.empty;

        if (hasEqualsButNotEqualsObject(classFile)) {
            var message = "missing equals(Object) to override Object.equals(Object)"
            val relevance =
                if (hasHashCode(classFile)) {
                    message += " (the class overrides the standard hashCode method)"
                    Relevance.VeryHigh
                } else if (superClassHasCustomHashCode(classFile)) {
                    message += " (a superclass overrides the standard hashCode method)"
                    Relevance.High
                } else {
                    Relevance.Low
                }

            Iterable(Issue(
                "CovariantEquals",
                relevance,
                message,
                Set(IssueCategory.Correctness),
                Set(IssueKind.DubiousMethodDefinition),
                List(new ClassLocation(None, project, classFile))
            ))
        } else
            Nil

    }
}
