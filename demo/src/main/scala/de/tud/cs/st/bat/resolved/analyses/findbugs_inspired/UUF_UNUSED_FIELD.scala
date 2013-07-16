/* License (BSD Style License):
 *  Copyright (c) 2009, 2011
 *  Software Technology Group
 *  Department of Computer Science
 *  Technische Universität Darmstadt
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
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
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */
package de.tud.cs.st.bat.resolved
package analyses
package findbugs_inspired

/**
 *
 * @author Ralf Mitschke
 */
object UUF_UNUSED_FIELD extends (Project[_] ⇒ Iterable[(ClassFile, Field)]) {

    def apply(project: Project[_]) = {
        var unusedFields: List[(ClassFile, Field)] = Nil

        for (classFile ← project.classFiles if !classFile.isInterfaceDeclaration) {
            val declaringClass = classFile.thisClass
            var privateFields: Map[String, (ClassFile, Field)] = Map.empty
            for (field ← classFile.fields if field.isPrivate) {
                privateFields += field.name -> (classFile, field)
            }

            for (
                method ← classFile.methods if method.body.isDefined;
                instruction ← method.body.get.instructions
            ) {
                instruction match {
                    case FieldReadAccess(`declaringClass`, name, _) ⇒ privateFields -= name
                    case GETSTATIC(`declaringClass`, name, _) ⇒ privateFields -= name
                    case _ ⇒
                }
            }
            if (privateFields.size > 0) {
                unusedFields = unusedFields ::: privateFields.values.toList
            }
        }

        unusedFields
    }
}
