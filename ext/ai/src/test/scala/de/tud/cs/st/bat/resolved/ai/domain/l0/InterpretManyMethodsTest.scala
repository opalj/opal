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

import domain.l0
import domain.l1
import reader.Java7Framework.ClassFile

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.Matchers

/**
 * This test(suite) just loads a very large number of class files and performs
 * an abstract interpretation of the methods.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class InterpretManyMethodsTest extends FlatSpec with Matchers {

    import de.tud.cs.st.util.ControlAbstractions._
    import debug.InterpretMethods.interpret

    behavior of "BATAI"

    // The jars of the "BAT core" project
    val directoryWithJARs = "../../../../../core/src/test/resources/classfiles"
    val files =
        TestSupport.locateTestResources(directoryWithJARs, "ext/ai").listFiles.
            filter(file ⇒ file.isFile && file.canRead() && file.getName.endsWith(".jar"))

    it should (
        "be able to interpret all methods using the BaseConfigurableDomain in "+
        files.map(_.getName).mkString("\n\t\t", "\n\t\t", "\n")) in {
            interpret(classOf[l0.BaseConfigurableDomain[_]], files) map { errors ⇒
                fail(errors._1+" (details: "+errors._2.getOrElse("not available")+")")
            }
        }

    it should (
        "be able to interpret all methods using the DefaultConfigurableDomain in "+
        files.map(_.getName).mkString("\n\t\t", "\n\t\t", "\n")) in {
            interpret(classOf[l1.DefaultConfigurableDomain[_]], files) map { errors ⇒
                fail(errors._1+" (details: "+errors._2.getOrElse("not available")+")")
            }
        }
}
