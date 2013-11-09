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

/**
  * Loads class files form a JAR archive and prints the signatures of the classes.
  *
  * @author Michael Eichberg
  */
object ClassFileInformation {

    def main(args: Array[String]) {

        import reader.Java7Framework

        if (args.length < 2) {
            println("Usage: java …ClassFileInformation "+
                "<JAR file containing class files> "+
                "<Name of classfile (incl. path) contained in the JAR file>+")
            println("Example:\n\tjava …ClassFileInformation /Library/Java/JavaVirtualMachines/jdk1.7.0_45.jdk/Contents/Home/jre/lib/rt.jar java/util/ArrayList.class")
            sys.exit(-1)
        }

        for (classFileName ← args.drop(1) /* drop the name of the jar file */ ) {

            // Load class file (the class file name has to correspond to the name of 
            // the file inside the archive.)
            val classFile = Java7Framework.ClassFile(args(0), classFileName)
            import classFile._

            // print the name of the type defined by this class file
            println(thisClass.toJava)

            superClass.map(s ⇒ println("  extends "+s.toJava)) // java.lang.Object does not have a super class!
            if (interfaces.length > 0) {
                println(interfaces.map(_.toJava).mkString("  implement ", ", ", ""))
            }

            sourceFile map { s ⇒ println("\tSOURCEFILE: "+s) }

            println("\tVERSION: "+majorVersion+"."+minorVersion)

            println(fields.mkString("\tFIELDS:\n\t","\n\t",""))

            println(methods.mkString("\tMETHODS:\n\t","\n\t",""))

            println
        }
    }
}