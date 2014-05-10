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
package l1

import reader.Java8Framework.ClassFile

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.Matchers

/**
 * This system test(suite) just loads a very large number of class files and performs
 * an abstract interpretation of all methods. It basically tests if we can load and
 * process a large number of different classes without exceptions.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class DefaultConfigurableDomainTest extends FlatSpec with Matchers {

    import debug.InterpretMethods.interpret

    behavior of "OPAL-AI's l1.DefaultConfigurableDomain"

    // The jars of the "OPAL core" project
    val directoryWithJARs = "../../../../../core/src/test/resources/classfiles"
    val files =
        TestSupport.locateTestResources(directoryWithJARs, "ext/ai").listFiles.
            filter(file ⇒ file.isFile && file.canRead() && file.getName.endsWith(".jar"))
    val jarNames = files.map(_.getName).mkString("[", ", ", "]")

    it should ("be able to interpret all methods found in "+jarNames) in {
        interpret(classOf[DefaultConfigurableDomain[_]], files) map { error ⇒
            val (message, source) = error
            fail(message+" (details: "+source.getOrElse("not available")+")")
        }
    }
}
