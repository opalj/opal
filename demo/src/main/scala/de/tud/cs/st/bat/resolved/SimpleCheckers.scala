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
package bat.resolved

import util.debug.{ Counting, PerformanceEvaluation }

import reader.Java6Framework

/**
  * Demonstrates how to implement two very simple checkers using BAT.
  *
  * @author Michael Eichberg
  */
object SimpleCheckers {

    private def printUsage: Unit = {
        println("Performs some extremely simple checks on class files.")
        println("Usage: java …SimpleCheckers <ZIP or JAR file containing class files>+")
        println("(c) 2011 Michael Eichberg (eichberg@informatik.tu-darmstadt.de)")
    }

    def main(args: Array[String]) {

        if (args.length == 0 || !args.forall(arg ⇒ arg.endsWith(".zip") || arg.endsWith(".jar"))) {
            printUsage
            sys.exit(1)
        }

        for (arg ← args) {
            val file = new java.io.File(arg)
            if (!file.canRead() || file.isDirectory()) {
                println(arg+" is not a valid ZIP/Jar file.");
                printUsage
                sys.exit(1)
            }
        }

        analyze(args)
        sys.exit(0)
    }

    def analyze(zipFiles: Array[String]) {
        val CountingPerformanceEvaluator = new PerformanceEvaluation with Counting
        import CountingPerformanceEvaluator._

        var problemCount = 0

        time('Overall) {
            for (
                zipFile ← zipFiles;
                classFile ← Java6Framework.ClassFiles(zipFile)
            ) {
                time('EqHcChecker) {
                    var definesEqualsMethod = false
                    var definesHashCodeMethod = false
                    for (method ← classFile.methods) method match {
                        case Method(_, "equals", MethodDescriptor(Seq(ObjectType("java/lang/Object")), BooleanType), _) ⇒ definesEqualsMethod = true
                        case Method(_, "hashCode", MethodDescriptor(Seq(), IntegerType), _) ⇒ definesHashCodeMethod = true
                        case _ ⇒
                    }

                    if (definesEqualsMethod != definesHashCodeMethod) {
                        problemCount += 1
                        println("the class: "+classFile.thisClass.className+" does not satisfy java.lang.Object's equals-hashCode contract.")
                    }
                }
                time('CovEqChecker) {
                    var definesEqualsMethod = false
                    var definesCovariantEqualsMethod = false
                    for (method ← classFile.methods) method match {
                        case Method(_, "equals", MethodDescriptor(Seq(ObjectType("java/lang/Object")), BooleanType), _) ⇒ definesEqualsMethod = true
                        case Method(_, "equals", MethodDescriptor(Seq(ObjectType(_)), BooleanType), _) ⇒ definesCovariantEqualsMethod = true
                        case _ ⇒
                    }
                    if (definesCovariantEqualsMethod && !definesEqualsMethod) {
                        problemCount += 1
                        println("the class: "+classFile.thisClass.className+" defines a covariant equals method, but does not also define the standard equals method.")
                    }
                }
            }
        }
        import de.tud.cs.st.util.debug.nsToSecs
        println("Equals-HashCode Checker: "+nsToSecs(getTime('EqHcChecker)))
        println("Covariant Equals Checker: "+nsToSecs(getTime('CovEqChecker)))
        println("Analyzed all classes in: "+nsToSecs(getTime('Overall)))
        println("Number of identified violations: "+problemCount)
    }
}