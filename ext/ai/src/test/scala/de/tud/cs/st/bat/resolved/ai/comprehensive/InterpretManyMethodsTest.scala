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
package comprehensive

import de.tud.cs.st.util.ControlAbstractions._
import reader.Java7Framework.ClassFile

import java.util.zip.ZipFile
import java.io.DataInputStream
import java.io.ByteArrayInputStream

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.concurrent.TimeLimitedTests
import org.scalatest.time._

/**
 * This test(suite) just loads a very large number of class files and performs
 * an abstract interpretation of the methods.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class InterpretManyMethodsTest
        extends FlatSpec
        with ShouldMatchers
        with TimeLimitedTests {

    import collection.JavaConversions._

    val timeLimit = Span(250, Millis)
    val directoryWithJARs = "../../../../../core/src/test/resources/classfiles"

    behavior of "BAT"

    for {
        file ← TestSupport.locateTestResources(directoryWithJARs, "ext/ai").listFiles
        if file.isFile && file.canRead && file.getName.endsWith(".jar")
        jarFile = new ZipFile(file)
        jarEntry ← (jarFile).entries
        if !jarEntry.isDirectory && jarEntry.getName.endsWith(".class")
    } {
        val data = new Array[Byte](jarEntry.getSize().toInt)
        process(new DataInputStream(jarFile.getInputStream(jarEntry))) { _.readFully(data) }
        analyzeClassFile(file.getName(), data)
    }

    def analyzeClassFile(resource: String, data: Array[Byte]) {
        val classFile = ClassFile(new DataInputStream(new ByteArrayInputStream(data)))
        for (method ← classFile.methods; if method.body.isDefined) {

            it should ("be able to perform an abstract interpretation of the method "+classFile.thisClass.toJava + method.toJava+" in "+resource) in {
                val domain = new DefaultDomain()
                util.Util.dumpOnFailure[Unit](classFile, method, domain) {
                    result: AIResult[domain.type] ⇒
                        {
                            // Nothing else to do? Checkt that all instructions are interpreted?    
                        }
                }
            }
        }
    }
}
