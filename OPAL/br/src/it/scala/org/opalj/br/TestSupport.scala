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

import org.opalj.bi.TestSupport.JRELibraryFolder
import org.opalj.bi.TestSupport.RTJar
import org.opalj.br.analyses.Project
import org.opalj.br.reader.Java8FrameworkWithCaching
import org.opalj.br.reader.BytecodeInstructionsCache

/**
 * Common functionality required by all test.
 *
 * @author Michael Eichberg
 */
object TestSupport {

    /**
     * Loads class files from JRE .jars found in the boot classpath.
     *
     * @return List of class files ready to be passed to a `IndexBasedProject`.
     */
    def readJREClassFiles(
        cache: BytecodeInstructionsCache = new BytecodeInstructionsCache): Seq[(ClassFile, java.net.URL)] = {
        val reader = new Java8FrameworkWithCaching(cache)
        val classFiles = reader.ClassFiles(JRELibraryFolder)
        if (classFiles.isEmpty)
            sys.error(s"loading the JRE (${JRELibraryFolder}) failed")

        classFiles.toSeq
    }

    def readRTJarClassFiles(
        cache: BytecodeInstructionsCache = new BytecodeInstructionsCache): Seq[(ClassFile, java.net.URL)] = {
        val reader = new Java8FrameworkWithCaching(cache)
        val classFiles = reader.ClassFiles(RTJar)
        if (classFiles.isEmpty)
            sys.error(s"loading the JRE (${JRELibraryFolder}) failed")

        classFiles.toSeq
    }

    def createJREProject: Project[java.net.URL] = Project(readJREClassFiles())

    def createRTJarProject: Project[java.net.URL] = Project(readRTJarClassFiles())

}