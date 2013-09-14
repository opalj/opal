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
package de.tud.cs.st
package bat
package resolved
package ai
package util

/**
 * A small interpreter that enables us to easily perform the abstract interpretation of a
 * specific method.
 *
 * @author Michael Eichberg
 */
object InterpretMethod {

    import de.tud.cs.st.util.ControlAbstractions._

    private object AI extends AI {

        def isInterrupted = Thread.interrupted()

        val tracer = Some(new ConsoleTracer {})
    }

    def main(args: Array[String]) {
        if (args.size != 3) {
            println("You have to specify the method that should be analyzed.")
            println("\tFirst parameter: a jar/calss file or a directory containing jar/class files.")
            println("\tSecond parameter: the name of a class in binary notation (use \"/\" as the package separator.")
            println("\tThird parameter: the name of a method of the class.")
            return ;
        }
        val fileName = args(0)
        val className = args(1)
        val methodName = args(2)

        val file = new java.io.File(fileName)
        if (!file.exists()) {
            println(Console.RED+"file does not exist: "+fileName + Console.RESET)
            return ;
        }

        val classFiles =
            try {
                reader.Java7Framework.ClassFiles(file)
            } catch {
                case e: Exception ⇒
                    println(Console.RED+"cannot read file: "+e.getMessage() + Console.RESET)
                    return ;
            }
        val classFile =
            classFiles.map(_._1).find(_.thisClass.className == className) match {
                case Some(classFile) ⇒ classFile
                case None ⇒
                    println(Console.RED+"cannot find the class: "+className + Console.RESET)
                    return ;
            }
        val method =
            classFile.methods.find(_.name == methodName) match {
                case Some(method) ⇒ method
                case None ⇒
                    println(Console.RED+"cannot find the method: "+methodName + Console.RESET)
                    return ;
            }

        import util.Util.{ dump, writeAndOpenDump }

        try {
            val result =
                AI(classFile,
                    method,
                    new domain.ConfigurableDefaultDomain((classFile, method)))
            println(result)
        } catch {
            case ie @ InterpreterException(throwable, domain, worklist, operands, locals) ⇒
                writeAndOpenDump(dump(
                    Some(classFile),
                    Some(method),
                    method.body.get,
                    operands,
                    locals,
                    Some(
                        throwable.getLocalizedMessage()+"<br>"+
                            throwable.getStackTrace().mkString("\n<ul><li>", "</li>\n<li>", "</li></ul>\n") +
                            worklist.mkString("Remaining worklist:\n<br>", ", ", "<br>")
                    )))
                throw ie
        }
    }
}
