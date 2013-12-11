/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package de.tud.cs.st.bat.resolved
package analyses
package bug_patterns.ioc

import instructions._

/**
 *
 * @author Ralf Mitschke
 */
object SIC_INNER_SHOULD_BE_STATIC_ANON extends (Project[_] ⇒ Iterable[ClassFile]) {

    val withinAnonymousClass = "[$][0-9].*[$]".r

    /**
     * A heuristic for determining whether an inner class is inside an anonymous inner class based on the class name
     */
    def isWithinAnonymousInnerClass(classFile: ClassFile): Boolean = {
        withinAnonymousClass.findFirstIn(classFile.thisType.fqn).isDefined
    }

    def lastIndexOfInnerClassEncoding(classFile: ClassFile): Int = {
        val name = classFile.thisType.fqn
        math.max(name.lastIndexOf('$'), name.lastIndexOf('+'))
    }

    /**
     * A heuristic for determining inner classes by the encoding in the name
     */
    def isInnerClass(classFile: ClassFile): Boolean = {
        lastIndexOfInnerClassEncoding(classFile) >= 0
    }

    /**
     * A heuristic for determining anonymous inner classes by the encoding in the name
     */
    def isAnonymousInnerClass(classFile: ClassFile): Boolean = {
        val lastSpecialChar = lastIndexOfInnerClassEncoding(classFile)
        isInnerClass(classFile) &&
            Character.isDigit(classFile.thisType.fqn.charAt(lastSpecialChar + 1))
    }

    /**
     * A heuristic for determining whether an inner class can be made static
     */
    def canConvertToStaticInnerClass(classFile: ClassFile): Boolean = {
        !isWithinAnonymousInnerClass(classFile)
    }

    /**
     * A heuristic for determining whether the field points to the enclosing instance
     */
    def isOuterThisField(field: Field): Boolean = {
        field.name.startsWith("this$") || field.name.startsWith("this+")
    }

    /**
     * A heuristic that determines whether the outer this field is read, by counting aload_1 instructions
     * The count must be greater than 1, because the variable will be read once for storing it
     * into the field reference for the outer this instance.
     */
    def constructorReadsOuterThisField(classFile: ClassFile): Boolean = {
        (for (
            method ← classFile.constructors if (method.name == "<init>") && method.body.isDefined;
            instr ← method.body.get.instructions if (instr.isInstanceOf[ALOAD_1.type])
        ) yield 1).sum > 1
    }

    def apply(project: Project[_]) = {
        val readFields = BaseAnalyses.readFields(project.classFiles).map(_._2)
        for (
            classFile ← project.classFiles if (isAnonymousInnerClass(classFile) &&
                canConvertToStaticInnerClass(classFile)
            );
            declaringClass = classFile.thisType;
            field @ Field(_, name, fieldType) ← classFile.fields if (isOuterThisField(field) &&
                !readFields.contains((declaringClass, name, fieldType)) &&
                !constructorReadsOuterThisField(classFile)
            )
        ) yield {
            classFile
        }
    }
}