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
package findrealbugs
package analyses

import resolved._
import resolved.analyses._
import resolved.instructions._
import AnalysesHelpers.getReadFields

/**
 * This analysis reports anonymous inner classes that do not use their reference to the
 * parent class and as such could be made `static` in order to save some memory.
 *
 * Since anonymous inner classes cannot be declared `static`, they must be refactored to
 * named inner classes first.
 *
 * @author Ralf Mitschke
 * @author Daniel Klauer
 * @author Peter Spieler
 * @author Florian Brandherm
 */
class AnonymousInnerClassShouldBeStatic[Source]
        extends MultipleResultsAnalysis[Source, ClassBasedReport[Source]] {

    def description: String = "Reports anonymous inner classes that should be made Static"

    private val withinAnonymousClass = "[$][0-9].*[$]".r

    /**
     * A heuristic for determining whether an inner class is inside an anonymous inner
     * class based on the class name.
     *
     * @param classFile The inner class to check.
     * @return Whether the inner class is inside an anonymous inner class.
     */
    private def isWithinAnonymousInnerClass(classFile: ClassFile): Boolean = {
        withinAnonymousClass.findFirstIn(classFile.thisType.fqn).isDefined
    }

    /**
     * Finds the last occurrence of either '$' or '+' in a class name string.
     *
     * @param fqn The class name to check.
     * @return The index of the last occurring '$' or '+', whichever is closer to the end.
     */
    private def lastIndexOfInnerClassEncoding(fqn: String): Int = {
        math.max(fqn.lastIndexOf('$'), fqn.lastIndexOf('+'))
    }

    /**
     * A heuristic for determining inner classes by the encoding in the name.
     * It checks whether the class name contains '$' or '+'.
     *
     * @param classFile The class to check.
     * @return Whether the class is an inner class.
     */
    private def isInnerClass(classFile: ClassFile): Boolean = {
        lastIndexOfInnerClassEncoding(classFile.thisType.fqn) >= 0
    }

    /**
     * A heuristic for determining anonymous inner classes by the encoding in the name.
     *
     * @param classFile The class to check.
     * @return Whether the class is an anonymous inner class.
     */
    private def isAnonymousInnerClass(classFile: ClassFile): Boolean = {
        val fqn = classFile.thisType.fqn

        val lastSpecialChar = lastIndexOfInnerClassEncoding(fqn)
        if (lastSpecialChar < 0) {
            return false
        }

        val digitChar = lastSpecialChar + 1;
        digitChar < fqn.length && Character.isDigit(fqn.charAt(digitChar))
    }

    /**
     * A heuristic for determining whether an inner class can be made static
     * by checking if it is not in an anonymous inner class, based on its name.
     *
     * @param classFile The inner class to check.
     * @return Whether the inner class can be converted to a `static` inner class.
     */
    private def canConvertToStaticInnerClass(classFile: ClassFile): Boolean = {
        !isWithinAnonymousInnerClass(classFile)
    }

    /**
     * A heuristic for determining whether the field points to the enclosing instance
     * by checking if its name starts with "this".
     *
     * @param field The field to check.
     * @return Whether the field is the inner class's reference to the parent object.
     */
    private def isOuterThisField(field: Field): Boolean = {
        field.name.startsWith("this$") || field.name.startsWith("this+")
    }

    /**
     * A heuristic that determines whether the outer this field is read, by counting
     * aload_1 instructions. The count must be greater than 1, because the variable will
     * always be read at least once for storing it into the field reference for the outer
     * this instance.
     *
     * @param classFile The inner class whose constructors should be checked.
     * @return Whether a constructor of the inner class accesses the reference to the
     * inner class's parent object for more than storing the initial reference.
     */
    private def constructorReadsOuterThisField(classFile: ClassFile): Boolean = {
        classFile.constructors.filter(_.body.isDefined).exists { method ⇒
            var count = 0
            method.body.get.instructions foreach (instruction ⇒
                if (instruction == ALOAD_1) {
                    count += 1;
                    if (count > 1) {
                        return true
                    }
                })
            false
        }
    }

    /**
     * Runs this analysis on the given project.
     *
     * @param project The project to analyze.
     * @param parameters Options for the analysis. Currently unused.
     * @return A list of reports, or an empty list.
     */
    def analyze(
        project: Project[Source],
        parameters: Seq[String] = List.empty): Iterable[ClassBasedReport[Source]] = {
        val readFields = AnalysesHelpers.getReadFields(project.classFiles).map(_._2)
        for {
            classFile ← project.classFiles
            if !project.isLibraryType(classFile)
            if isAnonymousInnerClass(classFile) &&
                canConvertToStaticInnerClass(classFile)
            field @ Field(_, name, fieldType) ← classFile.fields
            if isOuterThisField(field) &&
                !readFields.exists(_ == (classFile.thisType, name, fieldType)) &&
                !constructorReadsOuterThisField(classFile)
        } yield {
            ClassBasedReport(
                project.source(classFile.thisType),
                Severity.Info,
                classFile.thisType,
                "This inner class should be made Static")
        }
    }
}
