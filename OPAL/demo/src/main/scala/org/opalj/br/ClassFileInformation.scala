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
package br

import org.opalj.br.reader.Java8Framework.{ ClassFile ⇒ ClassFileReader }

/**
 * Loads class files form a JAR archive and prints the signatures of the classes.
 *
 * @author Michael Eichberg
 */
object ClassFileInformation {

    def main(args: Array[String]) {

        if (args.length < 2) {
            println("Usage: java …ClassFileInformation "+
                "<JAR file containing class files> "+
                "<Name of classfile (incl. path) contained in the JAR file>+")
            println("Example:\n\tjava …ClassFileInformation /Library/Java/JavaVirtualMachines/jdk1.7.0_15.jdk/Contents/Home/jre/lib/rt.jar java/util/ArrayList.class")
            sys.exit(-1)
        }

        for (classFileName ← args.drop(1) /* drop the name of the jar file */ ) {

            // Load class file (the class file name has to correspond to the name of 
            // the file inside the archive.)
            // The JavaXFramework defines multiple other methods that make it convenient
            // to load class files stored in folders or in jars within jars.
            val classFile = ClassFileReader(args(0), classFileName)
            import classFile._

            // print the name of the type defined by this class file
            println(thisType.toJava)

            // superclassType returns an Option, because java.lang.Object does not have a super class
            superclassType map { s ⇒ println("  extends "+s.toJava) }
            if (interfaceTypes.length > 0) {
                println(interfaceTypes.map(_.toJava).mkString("  implement ", ", ", ""))
            }

            // the source file attribute is an optional attribute and is only specified
            // if the compiler settings are such that debug information is added to the
            // compile class file.
            sourceFile map { s ⇒ println("\tSOURCEFILE: "+s) }

            // The version of the class file. Basically, every major version of the
            // JDK defines additional (new) features.
            println("\tVERSION: "+majorVersion+"."+minorVersion)

            println(fields.map(_.toJavaSignature).mkString("\tFIELDS:\n\t", "\n\t", ""))

            println(methods.map(_.toJava).mkString("\tMETHODS:\n\t", "\n\t", ""))

            println()
        }
    }
}