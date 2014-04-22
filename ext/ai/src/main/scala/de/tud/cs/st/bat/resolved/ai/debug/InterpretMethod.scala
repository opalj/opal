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
package resolved
package ai
package debug

import scala.language.existentials
import domain.l0.BaseConfigurableDomain

/**
 * A small basic framework that facilitates the abstract interpretation of a
 * specific method using a configurable domain.
 *
 * @author Michael Eichberg
 */
object InterpretMethod {

    private object AI extends AI[SomeDomain] {

        override def isInterrupted = Thread.interrupted()

        override val tracer =
            //Some(new ConsoleTracer {})
            Some(
                new MultiTracer(
                        new ConsoleTracer {},
                        //new ConsoleEvaluationTracer {},
                        new XHTMLTracer {})
            )
    }

    /**
     * Traces the interpretation of a single method and prints out the results.
     *
     * @param args The first element must be the name of a class file, a jar file
     * 		or a directory containing the former. The second element must
     * 		denote the name of a class and the third must denote the name of a method
     * 		of the respective class. If the method is overloaded the first method
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
                Class.forName(args(3).substring(8)).asInstanceOf[Class[_ <: SomeDomain]]
            else
                classOf[BaseConfigurableDomain[_]]
        }
        
        
        def getConstructor[Source: reflect.ClassTag](domainClass: Class[_ <: SomeDomain], classFiles: Seq[(ClassFile, Source)])(classFile: ClassFile, method: Method): SomeDomain = {
            scala.util.control.Exception.ignoring(classOf[NoSuchMethodException]) {
                val constructor = domainClass.getConstructor(classOf[Object])
                return constructor.newInstance(classFile)
            }
            
            val constructor = domainClass.getConstructor(classOf[analyses.ProjectLike[Source]], classOf[ClassFile], classOf[Method])
            val project = analyses.IndexBasedProject(classFiles)
            return constructor.newInstance(project, classFile, method)   
        }
          

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
            
        def newInstance = getConstructor(domainClass, classFiles) _
            
        val classFile = {
            def lookupClass(fqn: String): Option[ClassFile] =
                classFiles.map(_._1).find(_.fqn == fqn) match {
                    case someClassFile @ Some(_)   ⇒ someClassFile
                    case None if fqn.contains('.') ⇒ lookupClass(fqn.replace('.', '/'))
                    case None                      ⇒ None
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

        import debug.XHTML.{ dump, writeAndOpenDump }

        try {
            val result =
                AI(classFile, method, newInstance(classFile, method))
            writeAndOpenDump(dump(
                Some(classFile),
                Some(method),
                method.body.get,
                result.domain,
                result.operandsArray,
                result.localsArray,
                Some("Result("+domainClass.getName()+"): "+(new java.util.Date).toString)))
        } catch {
            case ie @ InterpretationFailedException(cause, domain, pc, worklist, evaluated, operands, locals) ⇒
                val header =
                    Some("<p><b>"+domainClass.getName()+"</b></p>"+
                        cause.getMessage()+"<br>"+
                        ie.getStackTrace().mkString("\n<ul><li>", "</li>\n<li>", "</li></ul>\n")+
                        "Current instruction: "+pc+"<br>"+
                        evaluated.mkString("Evaluated instructions:\n<br>", ", ", "<br>") +
                        worklist.mkString("Remaining worklist:\n<br>", ", ", "<br>")
                    )
                val evaluationDump =
                    dump(
                        Some(classFile), Some(method), method.body.get,
                        domain, operands, locals, header
                    )
                writeAndOpenDump(evaluationDump)
                throw ie
        }
    }
}
