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
package de.tud.cs.st
package bat
package resolved

import util.debug.PerformanceEvaluation
import reader.Java7Framework.ClassFiles

/**
 * This snippet shows how to load all class files from a zip file and how to
 * associate all top-level source elements (class,
 * field and method declarations) with unique ids.
 *
 * For more advanced analysis you can use the [[analysis.AnalysisExectuor]] trait
 * as a template.
 *
 * @author Michael Eichberg
 */
object AssociateUniqueIDs extends PerformanceEvaluation {

    import de.tud.cs.st.util.debug._

    val sourceElementIDs = new SourceElementIDsMap

    import sourceElementIDs.{ sourceElementID ⇒ id }

    def main(args: Array[String]) {
        if (args.length != 1) {
            println("You have to specify a zip/jar file containing class files.")
            sys.exit(1)
        }
        val file = new java.io.File(args(0))
        if (!file.canRead() || file.isDirectory()) {
            println("No valid zip file: "+args(0))
            println("You have to specify a zip/jar file containing class files.")
            sys.exit(2);
        }

        val classFiles: Seq[ClassFile] = ClassFiles(args(0))

        val loadAllClassFiles = () ⇒ {
            for (classFile ← classFiles) {
                val classID = id(classFile)

                for (method ← classFile.methods) {
                    id(classFile, method)
                }

                for (field ← classFile.fields) {
                    id(classFile, field)
                }
            }
        }

        time(t ⇒ println("Loading all class files and associating unique ids took: "+nsToSecs(t)+" seconds.")) {
            loadAllClassFiles()
        }
    }
}
