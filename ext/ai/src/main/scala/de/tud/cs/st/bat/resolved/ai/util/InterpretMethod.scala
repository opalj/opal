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

import domain.BaseConfigurableDomain
import de.tud.cs.st.bat.resolved.ai.tracer.MultiTracer
import de.tud.cs.st.bat.resolved.ai.tracer.ConsoleTracer
import de.tud.cs.st.bat.resolved.ai.tracer.XHTMLTracer

/**
 * A small interpreter that enables us to easily perform the abstract interpretation of a
 * specific method.
 *
 * @author Michael Eichberg
 */
object InterpretMethod {

    import language.existentials

    private object AI extends AI[Domain[_]] {

        override def isInterrupted = Thread.interrupted()

        override val tracer =
            //Some(new ConsoleTracer {})
            Some(
                new MultiTracer(new ConsoleTracer {}, new XHTMLTracer {})
            )
    }

    /**
     * Traces the interpretation of a single method and prints out the results.
     *
     * @param args The first element must be the name of a class file, a jar file
     * 		or a directory containing the former. The second element must
     *   	denote the name of a class and the third must denote the name of a method
     *    	of the respective class. If the method is overloaded the first method
     * 		is returned.
     */
    def main(args: Array[String]) {
        if (args.size < 3 || args.size > 4) {
            println("You have to specify the method that should be analyzed.")
            println("\t1: a jar/class file or a directory containing jar/class files.")
            println("\t2: the name of a class.")
            println("\t3: the simple name or signature of a method of the class.")
            println("\t4[Optional]: -domain=CLASS the name of class of the configurable domain to use.")
            return ;
        }
        val fileName = args(0)
        val className = args(1)
        val methodName = args(2)
        val domainClass = {
            if (args.length > 3)
                Class.forName(args(3).substring(8)).
                    asInstanceOf[Class[_ <: BaseConfigurableDomain[_]]]
            else
                classOf[BaseConfigurableDomain[_]]
        }
        val domainConstructor = domainClass.getConstructor(classOf[Object])

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
        val classFile = {
            def lookupClass(className: String): Option[ClassFile] =
                classFiles.map(_._1).find(_.thisClass.className == className) match {
                    case someClassFile @ Some(_)         ⇒ someClassFile
                    case None if className.contains('.') ⇒ lookupClass(className.replace('.', '/'))
                    case None                            ⇒ None
                }
            lookupClass(className) match {
                case None ⇒
                    println(Console.RED+"cannot find the class: "+className + Console.RESET)
                    return ;
                case Some(classFile) ⇒ classFile
            }
        }
        val method =
            (
                if (methodName.contains("("))
                    classFile.methods.find(_.toJava.contains(methodName))
                else
                    classFile.methods.find(_.name == methodName)
            ) match {
                    case Some(method) ⇒ method
                    case None ⇒
                        println(Console.RED+
                            "cannot find the method: "+methodName + Console.RESET+
                            " - candidates: "+
                            classFile.methods.map(_.toJava).mkString(", "))
                        return ;
                }

        import util.XHTML.{ dump, writeAndOpenDump }

        try {
            val result =
                AI(classFile,
                    method,
                    domainConstructor.newInstance((classFile, method)))
            writeAndOpenDump(dump(
                Some(classFile),
                Some(method),
                method.body.get,
                result.domain,
                result.operandsArray,
                result.localsArray,
                Some("Result("+domainClass.getName()+"): "+(new java.util.Date).toString)))
        } catch {
            case ie @ InterpreterException(throwable, domain, worklist, evaluated, operands, locals) ⇒
                writeAndOpenDump(dump(
                    Some(classFile),
                    Some(method),
                    method.body.get,
                    domain,
                    operands,
                    locals,
                    Some("<p><b>"+domainClass.getName()+"</b></p>"+
                        throwable.getLocalizedMessage()+"<br>"+
                        throwable.getStackTrace().mkString("\n<ul><li>", "</li>\n<li>", "</li></ul>\n") +
                        evaluated.reverse.mkString("Evaluated instructions:\n<br>", ",", "<br>") +
                        worklist.mkString("Remaining worklist:\n<br>", ", ", "<br>")
                    )))
                throw ie
        }
    }
}
