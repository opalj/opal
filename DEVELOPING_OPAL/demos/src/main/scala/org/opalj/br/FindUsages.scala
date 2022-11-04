/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br

import org.opalj.util.PerformanceEvaluation.time
import org.opalj.br.reader.Java9Framework
import org.opalj.bi.reader.ClassFileReader.SuppressExceptionHandler

/**
 * Shows how to scan for calls of a method belonging to a specific API (here: bouncycastle.)
 *
 * @author Michael Eichberg
 */
object FindUsages {

    def main(args: Array[String]): Unit = {
        if (args.isEmpty) {
            println("Error: you have to specify the root folder.")
            return ;
        }

        val c = new java.util.concurrent.atomic.AtomicInteger
        def m(): Unit = {
            time {
                Java9Framework.processClassFiles(
                    List(new java.io.File(args(0))), // <= the root folders
                    _ => (), // <= suppress debug info
                    {
                        case (cf, url) =>
                            c.incrementAndGet()
                            if (cf.methodsWithBody.exists(_.body.get.instructionIterator.exists(i => i.isMethodInvocationInstruction && i.asMethodInvocationInstruction.declaringClass.toJava.startsWith("org.bouncycastle"))))
                                println(s"$url ${cf.thisType.toJava}")
                    },
                    SuppressExceptionHandler
                )
            } { t =>
                println(s"Done ${t.toSeconds}; analyzed class files: "+c.get)
            }
        }
        m()
    }
}
