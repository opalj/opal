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
package ai
package debug

import org.opalj.br.{ ClassFile, Method, MethodSignature }
import org.opalj.br.analyses.{ Project, SomeProject }
import org.opalj.ai.domain.l0.BaseDomain
import org.opalj.ai.project.VTACallGraphAlgorithmConfiguration
import org.opalj.ai.project.DefaultVTACallGraphDomain
import org.opalj.ai.project.DefaultCHACallGraphDomain
import org.opalj.ai.project.CallGraphCache
import org.opalj.ai.project.VTACallGraphExtractor
import org.opalj.ai.project.CHACallGraphExtractor
import org.opalj.ai.project.CallGraphExtractor

import org.opalj.ai.Domain
import org.opalj.ai.domain.TheProject
import org.opalj.ai.domain.TheClassFile
import org.opalj.ai.domain.TheMethod
import org.opalj.ai.domain.ClassHierarchy
import org.opalj.ai.domain.TheCode
import org.opalj.br.analyses.SomeProject

/**
 * A small basic framework that facilitates the abstract interpretation of a
 * specific method using a configurable domain.
 *
 * @author Michael Eichberg
 */
object GetCallees {

    private object AI extends AI[Domain] {

        override def isInterrupted = Thread.interrupted()

        override val tracer = Some(new ConsoleTracer {})

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
        import Console.{ RED, RESET }
        import language.existentials

        if (args.size < 3 || args.size > 4) {
            println("You have to specify the method that should be analyzed.")
            println("\t1: a jar/class file or a directory containing jar/class files.")
            println("\t2: the name of a class.")
            println("\t3: the simple name or signature of a method of the class.")
            println("\t4[Optional]: VTA or CHA (default:CHA)")
            return ;
        }
        val fileName = args(0)
        val className = args(1)
        val methodName = args(2)

        val file = new java.io.File(fileName)
        if (!file.exists()) {
            println(RED+"[error] The file does not exist: "+fileName+"."+RESET)
            return ;
        }

        val project =
            try {
                Project(file)
            } catch {
                case e: Exception ⇒
                    println(RED+"[error] Cannot process file: "+e.getMessage()+"."+RESET)
                    return ;
            }

        val classFile = {
            val fqn =
                if (className.contains('.'))
                    className.replace('.', '/')
                else
                    className
            project.classFiles.find(_.fqn == fqn).getOrElse {
                println(RED+"[error] Cannot find the class: "+className+"."+RESET)
                return ;
            }
        }

        val method =
            (
                if (methodName.contains("("))
                    classFile.methods.find(m ⇒ m.descriptor.toJava(m.name).contains(methodName))
                else
                    classFile.methods.find(_.name == methodName)
            ) match {
                    case Some(method) ⇒
                        if (method.body.isDefined)
                            method
                        else {
                            println(RED+
                                "[error] The method: "+methodName+" does not have a body"+RESET)
                            return ;
                        }
                    case None ⇒
                        println(RED+
                            "[error] Cannot find the method: "+methodName+"."+RESET +
                            classFile.methods.map(m ⇒ m.descriptor.toJava(m.name)).toSet.toSeq.sorted.mkString(" Candidates: ", ", ", "."))
                        return ;
                }

        val cache = new CallGraphCache[MethodSignature, scala.collection.Set[Method]](project)
        val useVTA = args.length == 4 && args(3) == "VTA"
        type CallGraphDomain = Domain with ReferenceValuesDomain with TheProject with ClassHierarchy with TheClassFile with TheMethod with TheCode
        val (domain: CallGraphDomain, extractor: CallGraphExtractor) =
            if (useVTA) {
                println("USING VTA")
                val domain = new DefaultVTACallGraphDomain(project, cache, classFile, method /*, 4*/ )
                (
                    domain,
                    new VTACallGraphExtractor(
                        new CallGraphCache[MethodSignature, scala.collection.Set[Method]](project)
                    )
                )
            } else {
                println("USING CHA")
                val domain = new DefaultCHACallGraphDomain(project, cache, classFile, method)
                (
                    domain,
                    new CHACallGraphExtractor(
                        new CallGraphCache[MethodSignature, scala.collection.Set[Method]](project)
                    )
                )
            }

        import org.opalj.ai.debug.XHTML.dump

        try {
            val result = AI(classFile, method, domain)
            val (allCallEdges, allUnresolvableMethodCalls) = extractor.extract(result)
            val (_, callees) = allCallEdges
            for ((pc, methods) ← callees) {
                println("\n"+pc+":"+method.body.get.instructions(pc)+" calls: ")
                for (method ← methods) {
                    println(Console.GREEN+"\t\t+ "+project.classFile(method).thisType.toJava+"{ "+method.toJava+" }")
                }
                allUnresolvableMethodCalls.find(_.pc == pc).map { unresolvedCall ⇒
                    println(Console.RED+"\t\t- "+
                        unresolvedCall.calleeClass.toJava+
                        "{ "+unresolvedCall.calleeDescriptor.toJava(unresolvedCall.calleeName)+" }")
                }
            }

        } catch {
            case ife: InterpretationFailedException ⇒
                val header =
                    Some("<p><b>"+domain.getClass().getName()+"</b></p>"+
                        ife.cause.getMessage()+"<br>"+
                        ife.getStackTrace().mkString("\n<ul><li>", "</li>\n<li>", "</li></ul>\n")+
                        "Current instruction: "+ife.pc+"<br>"+
                        XHTML.evaluatedInstructionsToXHTML(ife.evaluated) +
                        ife.worklist.mkString("Remaining worklist:\n<br>", ", ", "<br>")
                    )
                val evaluationDump =
                    dump(
                        Some(classFile), Some(method), method.body.get,
                        header,
                        ife.domain)(
                            ife.operandsArray, ife.localsArray)
                org.opalj.util.writeAndOpen(
                    evaluationDump,
                    "StateOfFailedAbstractInterpretation",
                    ".html")
                throw ife
        }
    }
}
