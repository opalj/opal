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
package bat.resolved

import reader.Java7Framework.ClassFiles

/**
 * Counts the number of native methods.
 *
 * @author Michael Eichberg
 */
object NativeMethodsCounter {

    private def printUsage: Unit = {
        println("Usage: java …NativeMethodsCounter <JAR files containing class files>")
    }

    def main(args: Array[String]) {

        if (args.length == 0 || !args.forall(arg ⇒ arg.endsWith(".jar"))) {
            printUsage
            sys.exit(1)
        }

        for (arg ← args) {
            val file = new java.io.File(arg)
            if (!file.canRead()) {
                println("The file: "+file+" cannot be read.");
                printUsage
                sys.exit(1)
            }
        }

        println("Reading class files: ")
        import util.debug._
        import PerformanceEvaluation._
        val nativeMethods =
            time((t) ⇒ println("Analysis took: "+nsToSecs(t)+" secs.")) {
                for {
                    file ← args if { println("\t"+file); true };
                    (classFile, _) ← ClassFiles(file)
                    method ← classFile.methods if method.isNative
                } yield {
                    method
                }
            }

        println("Number of native methods: "+nativeMethods.size)
        println(nativeMethods.map((method) ⇒ method.name+"("+method.descriptor+")").mkString("\n"))
    }

}