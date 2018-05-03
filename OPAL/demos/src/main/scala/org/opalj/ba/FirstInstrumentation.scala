/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
package ba

import java.io.ByteArrayInputStream

import org.opalj.io.writeAndOpen
import org.opalj.da.ClassFileReader.ClassFile
import org.opalj.bc.Assembler
import org.opalj.br.ObjectType
import org.opalj.br.IntegerType
import org.opalj.br.MethodDescriptor.JustReturnsString
import org.opalj.br.MethodDescriptor.JustReturnsInteger
import org.opalj.br.MethodDescriptor.JustTakes
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.DUP
import org.opalj.br.instructions.GETSTATIC
import org.opalj.br.instructions.SWAP
import org.opalj.br.reader.Java8Framework
import org.opalj.util.InMemoryClassLoader
import org.opalj.br.PCAndInstruction

/**
 * Demonstrates how to perform a simple instrumentation; here, we just search for toString calls
 * and print out the result to the console.
 *
 * @author Michael Eichberg
 */
object FirstInstrumentation extends App {

    val PrintStreamType = ObjectType("java/io/PrintStream")
    val SystemType = ObjectType("java/lang/System")

    val TheType = ObjectType("org/opalj/ba/SimpleInstrumentationDemo")

    // let's load the class
    val in = () ⇒ this.getClass.getResourceAsStream("SimpleInstrumentationDemo.class")
    val cf = Java8Framework.ClassFile(in).head // in this case we don't have invokedynamic resolution
    // let's transform the methods
    val newMethods =
        for (m ← cf.methods) yield {
            m.body match {
                case None ⇒
                    m.copy() // methods which are native and abstract ...

                case Some(code) ⇒
                    // let's search all "toString" calls
                    val lCode = LabeledCode(code)
                    var modified = false
                    for {
                        PCAndInstruction(pc, INVOKEVIRTUAL(_, "toString", JustReturnsString)) ← code
                    } {
                        modified = true
                        lCode.insert(
                            pc, InsertionPosition.After,
                            Seq(
                                DUP,
                                GETSTATIC(SystemType, "out", PrintStreamType),
                                SWAP,
                                INVOKEVIRTUAL(PrintStreamType, "println", JustTakes(ObjectType.String))
                            )
                        )
                        // print out the receiver's hashCode (it has to be on the stack!)
                        lCode.insert(
                            pc, InsertionPosition.Before,
                            Seq(
                                DUP,
                                INVOKEVIRTUAL(ObjectType.Object, "hashCode", JustReturnsInteger),
                                GETSTATIC(SystemType, "out", PrintStreamType),
                                SWAP,
                                INVOKEVIRTUAL(PrintStreamType, "println", JustTakes(IntegerType))
                            )
                        )
                    }
                    if (modified) {
                        val (newCode, _) =
                            lCode.result(cf.version, m)( // We can use the default class hierarchy in this example
                            // as we only instrument linear methods using linear code,
                            // hence, we don't need to compute a new stack map table attribute!
                            )
                        m.copy(body = Some(newCode))
                    } else {
                        m.copy()
                    }
            }
        }
    val newRawCF = Assembler(toDA(cf.copy(methods = newMethods)))

    //
    // THE FOLLOWING IS NOT RELATED TO BYTECODE MANIPULATION, BUT SHOWS ASPECTS OF OPAL WHICH ARE
    // HELPFUL WHEN DOING BYTECODE INSTRUMENTATION.
    //

    // Let's see the old class file...
    val odlCFHTML = ClassFile(in).head.toXHTML(None)
    val oldCFHTMLFile = writeAndOpen(odlCFHTML, "SimpleInstrumentationDemo", ".html")
    println("original: "+oldCFHTMLFile)

    // Let's see the new class file...
    val newCF = ClassFile(() ⇒ new ByteArrayInputStream(newRawCF)).head.toXHTML(None)
    println("instrumented: "+writeAndOpen(newCF, "NewSimpleInstrumentationDemo", ".html"))

    // Let's test that the new class does what it is expected to do... (we execute the
    // instrumented method)
    val cl = new InMemoryClassLoader(Map((TheType.toJava, newRawCF)))
    val newClass = cl.findClass(TheType.toJava)
    val instance = newClass.newInstance()
    newClass.getMethod("callsToString").invoke(instance)
    newClass.getMethod("returnsValue", classOf[Int]).invoke(instance, new Integer(0))
    newClass.getMethod("returnsValue", classOf[Int]).invoke(instance, new Integer(1))

}
