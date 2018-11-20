/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.support.debug

import scala.Console.RED
import scala.Console.RESET

import org.opalj.br.Method
import org.opalj.br.MethodSignature
import org.opalj.br.analyses.Project
import org.opalj.ai.InterpretationFailedException
import org.opalj.ai.analyses.FieldValuesKey
import org.opalj.ai.analyses.MethodReturnValuesKey
import org.opalj.ai.util.XHTML
import org.opalj.ai.common.XHTML.dump
import org.opalj.ai.analyses.cg.CHACallGraphExtractor
import org.opalj.ai.analyses.cg.CallGraphExtractor
import org.opalj.ai.analyses.cg.DefaultVTACallGraphDomain
import org.opalj.ai.analyses.cg.VTACallGraphExtractor
import org.opalj.ai.analyses.cg.CallGraphCache

/**
 * Prints out information about the callees of a specific method.
 *
 * @author Michael Eichberg
 */
object GetCallees {

    /**
     * Prints information about the callees of a method.
     */
    def main(args: Array[String]): Unit = {

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

        implicit val project =
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
            project.allClassFiles.find(_.fqn == fqn).getOrElse {
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
        val extractor: CallGraphExtractor =
            if (useVTA) {
                println("USING VTA")
                def Domain(method: Method) =
                    new DefaultVTACallGraphDomain(
                        project,
                        project.get(FieldValuesKey), project.get(MethodReturnValuesKey),
                        cache,
                        method /*, 4*/ )
                new VTACallGraphExtractor(
                    new CallGraphCache[MethodSignature, scala.collection.Set[Method]](project),
                    Domain
                )

            } else {
                println("USING CHA")

                new CHACallGraphExtractor(
                    new CallGraphCache[MethodSignature, scala.collection.Set[Method]](project)
                )
            }

        try {
            val (allCallEdges, allUnresolvableMethodCalls) = extractor.extract(method)
            val (_, callees) = allCallEdges
            for ((pc, methods) ← callees) {
                println("\n"+pc+":"+method.body.get.instructions(pc)+" calls: ")
                for (method ← methods) {
                    println(Console.GREEN+"\t\t+ "+method.toJava)
                }
                allUnresolvableMethodCalls.find(_.pc == pc).foreach { unresolvedCall ⇒
                    println(Console.RED+"\t\t- "+
                        unresolvedCall.calleeClass.toJava+
                        "{ "+unresolvedCall.calleeDescriptor.toJava(unresolvedCall.calleeName)+" }")
                }
            }

        } catch {
            case ife: InterpretationFailedException ⇒
                val header =
                    Some("<p><b>"+ife.domain.getClass().getName()+"</b></p>"+
                        ife.cause.getMessage()+"<br>"+
                        ife.getStackTrace().mkString("\n<ul><li>", "</li>\n<li>", "</li></ul>\n")+
                        "Current instruction: "+ife.pc+"<br>"+
                        XHTML.evaluatedInstructionsToXHTML(ife.evaluated) +
                        ife.worklist.mkString("Remaining worklist:\n<br>", ", ", "<br>"))
                val evaluationDump =
                    dump(
                        Some(classFile), Some(method), method.body.get, header, ife.domain
                    )(ife.cfJoins, ife.operandsArray, ife.localsArray)
                org.opalj.io.writeAndOpen(
                    evaluationDump,
                    "StateOfFailedAbstractInterpretation",
                    ".html"
                )
                throw ife
        }
    }
}
