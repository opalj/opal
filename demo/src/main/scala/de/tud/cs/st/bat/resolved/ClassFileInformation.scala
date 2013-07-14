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

import reader.Java7Framework

/**
 * Loads class files form a JAR archive and prints the signatures of the classes.
 *
 * @author Michael Eichberg
 */
object ClassFileInformation {

    def main(args: Array[String]) {

        if (args.length < 2) {
            println("Usage: java …ClassFileInformation <JAR file containing class files> <Name of classfile (incl. path information) contained in the JAR file>+")
            sys.exit(-1)
        }

        for (classFileName ← args.drop(1) /* drop the name of the jar file */ ) {
            val classFile = Java7Framework.ClassFile(args(0), classFileName)
            import classFile._

            print(thisClass.toJava)
            superClass.map(s ⇒ println(" extends "+s.toJava)) // java.lang.Object does not have a super class!
            if (interfaces.length > 0) {
                print(interfaces.map(_.toJava).mkString(" implements", " ", "\n"))
            }
            sourceFile.map(s ⇒ println(" sourcefile: "+s))
            println(" version   : "+majorVersion+"."+minorVersion)
            println
        }
        sys.exit(0)
    }
}