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
package de.tud.cs.st
package bat
package resolved
package ai
package domain
package l0

import bat.reader.ClassFileReader
import reader.Java8Framework.ClassFiles

import de.tud.cs.st.bat.TestSupport

import org.junit.runner.RunWith

import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.Matchers

/**
 * This system test(suite) just loads a very large number of class files and performs
 * an abstract interpretation of all methods.
 *
 * This test suite has the following goals:
 *  - Test if seemingly independent (partial-) domain implementations are really
 *    independent by using different mixin-composition orders and comparing the
 *    results.
 *  - Test if several different domain configurations are actually working.
 *  - (Test if we can load and process a large number of different classes
 *    without exceptions.)
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class BaseConfigurableDomainTest extends FlatSpec with Matchers {

    import debug.InterpretMethods.interpret

    // The following three domains are very basic domains that – given that the
    // same domains are used – should compute the same results.

    class BasicDomain1[+I](val identifier: I)
        extends Domain[I]
        with IgnoreMethodResults
        with IgnoreSynchronization
        with DefaultDomainValueBinding[I]
        with Configuration
        with l0.DefaultReferenceValuesBinding[I]
        with l0.DefaultTypeLevelIntegerValues[I]
        with l0.DefaultTypeLevelLongValues[I]
        with l0.DefaultTypeLevelFloatValues[I]
        with l0.DefaultTypeLevelDoubleValues[I]
        with l0.DefaultIntegerValuesComparison
        with l0.TypeLevelFieldAccessInstructions
        with l0.TypeLevelInvokeInstructions
        with PredefinedClassHierarchy

    class BasicDomain2[+I](val identifier: I)
        extends Domain[I]
        with Configuration
        with IgnoreMethodResults
        with IgnoreSynchronization
        with l0.TypeLevelInvokeInstructions
        with l0.TypeLevelFieldAccessInstructions
        with l0.DefaultIntegerValuesComparison
        with PredefinedClassHierarchy
        with DefaultDomainValueBinding[I]
        with l0.DefaultTypeLevelDoubleValues[I]
        with l0.DefaultTypeLevelIntegerValues[I]
        with l0.DefaultReferenceValuesBinding[I]
        with l0.DefaultTypeLevelFloatValues[I]
        with l0.DefaultTypeLevelLongValues[I]

    class BasicDomain3[+I](val identifier: I)
        extends Domain[I]
        with l0.DefaultReferenceValuesBinding[I]
        with l0.DefaultTypeLevelIntegerValues[I]
        with l0.DefaultIntegerValuesComparison
        with l0.DefaultTypeLevelFloatValues[I]
        with l0.DefaultTypeLevelLongValues[I]
        with l0.DefaultTypeLevelDoubleValues[I]
        with l0.TypeLevelInvokeInstructions
        with l0.TypeLevelFieldAccessInstructions
        with PredefinedClassHierarchy
        with IgnoreSynchronization
        with IgnoreMethodResults
        with Configuration

    behavior of "BATAI when changing the mixin composition order of \"independent\" domains"

    // The jars of the "BAT core" project
    val directoryWithJARs = "../../../../../core/src/test/resources/classfiles"
    val folder = TestSupport.locateTestResources(directoryWithJARs, "ext/ai")

    for {
        file ← folder.listFiles()
        if file.getName().endsWith(".jar")
        zipFile = new java.util.zip.ZipFile(file)
    } {
        def processClassFile(classFile: ClassFile, source: java.net.URL): Unit = this.synchronized {
            for (method ← classFile.methods.par)
                if (method.body.isDefined) {
                    // We want a comparison at the conceptual level that is why we use stateToString
                    val r1 = BaseAI(classFile, method, new BasicDomain1).stateToString
                    val r2 = BaseAI(classFile, method, new BasicDomain2).stateToString
                    val r3 = BaseAI(classFile, method, new BasicDomain3).stateToString

                    def doFail(r1: String, r2: String): Unit = {
                        val l1l2s = r1.split('\n') zip r2.split('\n')
                        val g3l1l2s = l1l2s.grouped(3)
                        val difference =
                            for {
                                Array((l11, l21), (l12, l22), (l13, l23)) ← l1l2s.grouped(3)
                                if l11 != l21 || l12 != l22 || l13 != l23
                            } yield {
                                l11+"\n"+l12+"\n"+l13+"\n"+
                                    " is different when compared to \n"+
                                    l21+"\n"+l22+"\n"+l23
                            }

                        var message = classFile.thisType.toJava+"{ "
                        message += method.toJava+"\n"
                        message += "Result is not identical:\n"+difference.mkString("\n", "\n", "\n")
                        message += "\n}"
                        fail(message)
                    }

                    // the tests...
                    if (r1 != r2) doFail(r1, r2)
                    if (r2 != r3) doFail(r2, r3)
                }
        }

        ignore should ("compute the same results independent of the mixin order of the domains for "+
            file.getName) in {
                ClassFiles(
                        zipFile, 
                        processClassFile _,
                        ClassFileReader.defaultExceptionHandler)
            }
    }
}
