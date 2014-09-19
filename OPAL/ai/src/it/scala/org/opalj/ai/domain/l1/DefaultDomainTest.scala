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
package ai
package domain
package l1

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.Matchers

import org.opalj.br.reader.Java8FrameworkWithCaching
import org.opalj.br.reader.Java8LibraryFramework.{ ClassFiles ⇒ LibraryClassFiles }
import org.opalj.br.reader.BytecodeInstructionsCache
import org.opalj.br.analyses.{ Project, ProgressManagement }
import org.opalj.ai.debug.InterpretMethodsAnalysis.interpret

/**
 * This system test(suite) just loads a very large number of class files and performs
 * an abstract interpretation of all methods. It basically tests if we can load and
 * process a large number of different classes without exceptions.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class DefaultDomainTest extends FlatSpec with Matchers {

    import org.opalj.br.analyses.ProgressManagement.None
    behavior of "the l1.DefaultDomain"

    it should ("be able to perform an abstract interpretation of the JRE's classes") in {
        val project = org.opalj.br.TestSupport.createJREProject

        val (message, source) =
            interpret(project, classOf[DefaultDomain[_]], false, None, 10)

        if (source.nonEmpty)
            fail(message+" (details: "+source+")")
        else
            info(message)
    }

    // TODO Add a test to test that we can analyze "more" projects!
//    it should ("be able to perform an abstract interpretation of the OPAL snapshot") in {
//        val reader = new Java8FrameworkWithCaching(new BytecodeInstructionsCache)
//        import reader.AllClassFiles
//        val classFilesFolder = org.opalj.bi.TestSupport.locateTestResources("classfiles", "bi")
//        val opalJARs = classFilesFolder.listFiles(new java.io.FilenameFilter() {
//            def accept(dir: java.io.File, name: String) = name.startsWith("OPAL-")
//        })
//        info(opalJARs.mkString("analyzing the following jars: ", ", ", ""))
//        opalJARs.size should not be (0)
//        val project = Project(AllClassFiles(opalJARs))
//
//        val (message, source) =
//            interpret(project, classOf[DefaultDomain[_]], false, None, 10)
//
//        if (source.nonEmpty)
//            fail(message+" (details: "+source+")")
//        else
//            info(message)
//    }
}
