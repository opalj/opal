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

/**
 *
 * @author Ralf Mitschke
 */
object ITA_INEFFICIENT_TO_ARRAY extends (Project[_] ⇒ Iterable[(ClassFile, Method, Int)]) {

    import BaseAnalyses.withIndex

    val objectArrayType = ArrayType(ObjectType("java/lang/Object"))

    val toArrayDescriptor = MethodDescriptor(List(objectArrayType), objectArrayType)

    val collectionInterface = ObjectType("java/util/Collection")

    val listInterface = ObjectType("java/util/List")

    def isCollectionType(classHierarchy: ClassHierarchy)(t: ReferenceType): Boolean = {
        if (!t.isObjectType) {
            false
        } else {
            classHierarchy.isSubtypeOf(t.asInstanceOf[ObjectType], collectionInterface).getOrElse(false) ||
                t == listInterface // TODO needs more heuristic or more analysis
        }
    }

    def apply(project: Project[_]) = {
        val classHierarchy: ClassHierarchy = project.classHierarchy
        val isCollectionType = this.isCollectionType(classHierarchy) _
        for (
            classFile ← project.classFiles;
            method ← classFile.methods if method.body.isDefined;
            Seq((ICONST_0, _), (ANEWARRAY(_), _), (instr, idx)) ← withIndex(method.body.get.instructions).sliding(3) if (
                instr match {
                    case INVOKEINTERFACE(targetType, "toArray", `toArrayDescriptor`) if (isCollectionType(targetType)) ⇒ true
                    case INVOKEVIRTUAL(targetType, "toArray", `toArrayDescriptor`) if (isCollectionType(targetType)) ⇒ true
                    case _ ⇒ false
                })
        ) yield {
            (classFile, method, idx)
        }
    }
}