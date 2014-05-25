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

import java.io.File

/**
 * Common functionality required by all test.
 *
 * @author Michael Eichberg
 */
object TestSupport {

    /**
     * This function tries to locate resources (at runtime) that are used by tests and
     * which are stored in the `SUBPROJECT-ROOT-FOLDER/src/test/resources` folder.
     * I.e., when the test suite is executed, the current folder may be either Eclipse's
     * `bin` bolder or OPAL's root folder when we use sbt to build the project.
     *
     * @param resourceName The name of the resource relative to the test/resources
     *      folder. The name must not begin with a "/".
     * @param subProjectFoler The root folder of the OPAL subproject; e.g., "ext/ai".
     *      (Default: "core").
     */
    def locateTestResources(resourceName: String, subProjectFolder: String): File = {
        { // if the current path is set to OPAL's root folder
            var file = new File("OPAL/"+subProjectFolder+"/src/test/resources/"+resourceName)
            if (file.exists()) return file
        }
        { // if the current path is set to "<SUB-PROJECT>/<BIN>"
            var file = new File("../src/test/resources/"+resourceName)
            if (file.exists()) return file
        }

        {
            // if we are in the sub-project's root folder
            var file = new File("src/test/resources/"+resourceName)
            if (file.exists()) return file
        }
        {
            val userDir = System.getProperty("user.dir")
            // if the current path is set to "target/scala-.../classes"
            if ("""target/scala\-[\w\.]+/classes$""".r.findFirstIn(userDir).isDefined) {

                var file = new File("../../../src/test/resources/"+resourceName)
                if (file.exists()) return file
            }
        }

        throw new IllegalArgumentException("Cannot locate resource: "+resourceName)
    }

    /**
     * This function tries to locate the runtime path of the jre library folder (aka the 
     * location in which the rt.jar file and others can be found).
     */
    def locateJRELibraryFolder: Option[File] = {
        val bootClasspath = System.getProperty("sun.boot.class.path")
        val elements = bootClasspath.split(":")
        val rtJar = elements.find(_.endsWith("rt.jar")).map(new File(_))
        rtJar.map(_.getParentFile())
    }
}