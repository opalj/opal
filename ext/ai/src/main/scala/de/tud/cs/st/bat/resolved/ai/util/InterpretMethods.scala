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

import de.tud.cs.st.util.ControlAbstractions._
import reader.Java7Framework.ClassFile
import java.util.zip.ZipFile
import java.io.DataInputStream
import java.io.ByteArrayInputStream

import scala.util.control.ControlThrowable

/**
 * Class that can be used to start the interpreter for some given class files.
 */
object InterpretMethods {
    import de.tud.cs.st.util.debug._
    import de.tud.cs.st.util.debug.PerformanceEvaluation._
    import collection.JavaConversions._

    def main(args: Array[String]) {
        System.out.println("Sleeping for 30secs")
        Thread.sleep(30 * 1000)
        interpret(args.map(new java.io.File(_)), true).map(System.err.println(_))
    }

    val timeLimit: Long = 250l //milliseconds

    def interpret(files: Seq[java.io.File], beVerbose: Boolean = false): Option[String] = {
        var collectedExceptions: List[(ClassFile, Method, Throwable)] = List()
        time('OVERALL) {
            for {
                file ← files
                jarFile = new ZipFile(file)
                jarEntry ← (jarFile).entries
                if !jarEntry.isDirectory && jarEntry.getName.endsWith(".class")
            } {
                val data = new Array[Byte](jarEntry.getSize().toInt)
                time('READING) {
                    process(new DataInputStream(jarFile.getInputStream(jarEntry))) { _.readFully(data) }
                }
                analyzeClassFile(file.getName(), data)
            }

            def analyzeClassFile(resource: String, data: Array[Byte]) {
                val classFile = time('PARSING) {
                    ClassFile(new DataInputStream(new ByteArrayInputStream(data)))
                }
                if (beVerbose) println(classFile.thisClass.className)
                for (method ← classFile.methods; if method.body.isDefined) {
                    if (beVerbose) println("  =>  "+method.toJava)
                    //                    val runnable = new Runnable {
                    //                        def run() {
                    try {
                        time('AI) {
                            if (AI(classFile, method, new domain.DefaultDomain()).wasAborted)
                                throw new InterruptedException();
                        }
                    } catch {
                        case ct: ControlThrowable ⇒ throw ct
                        case t: Throwable ⇒ { // we want to catch all types of exceptions, but also (assertion) error
                            collectedExceptions = (classFile, method, t) :: collectedExceptions
                        }
                    }
                    //                        }
                    //                    }
                    //                    val thread = new Thread(runnable)
                    //                    thread.start
                    //                    thread.join(timeLimit)
                    //                    thread.interrupt
                    //                    thread.join()
                }
            }
        }

        if (collectedExceptions.nonEmpty) {
            var report = "During the interpretation (overall: "+nsToSecs(getTime('OVERALL))+
                "secs. (reading: "+nsToSecs(getTime('READING))+
                "secs., parsing: "+nsToSecs(getTime('PARSING))+
                "secs., ai: "+nsToSecs(getTime('AI))+
                "secs.)) the following exceptions occured:"
            var groupedExceptions = collectedExceptions.groupBy(e ⇒ e._3.getClass().getName())
            groupedExceptions.map(ge ⇒ {
                val (exClass, exInstances) = ge
                report += "\n\t"+exClass+"("+exInstances.size+")__________________________\n"
                for ((classFile, method, ex) ← exInstances) {
                    report += "\t\t"+classFile.thisClass.className
                    report += " => "+method.toJava+"\n"
                    report += ex.getMessage().trim
                }
            })
            Some(report)
        } else {
            None
        }
    }

}
