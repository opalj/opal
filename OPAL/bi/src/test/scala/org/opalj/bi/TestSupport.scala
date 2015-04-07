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
package bi

import java.io.File

/**
 * Common functionality required by many tests.
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
     * @param subProjectFoler The root folder of the OPAL subproject; e.g., "ai".
     *      (Default: "core").
     */
    def locateTestResources(resourceName: String, subProjectFolder: String): File = {
        val resourceFile = subProjectFolder+"/src/test/resources/"+resourceName

        { // if the current path is set to OPAL's root folder
            val file = new File("OPAL/"+resourceFile)
            if (file.exists()) return file
        }
        { // if the current path is set to "<SUB-PROJECT>/<BIN>"
            val file = new File("../../"+resourceFile)
            if (file.exists()) return file
        }
        { // if the current path is set to "DEVELOPING_OPAL/<SUB-PROJECT>/<BIN>"
            val file = new File("../../../OPAL/"+resourceFile)
            if (file.exists()) return file
        }
        {
            // if we are in the sub-project's root folder
            val file = new File("../"+subProjectFolder + resourceFile)
            if (file.exists()) return file
        }
        {
            // if we are in a "developing opal" sub-project's root folder
            val file = new File("../../OPAL/"+resourceFile)
            if (file.exists()) return file
        }
        {
            val userDir = System.getProperty("user.dir")
            // if the current path is set to "target/scala-.../classes"
            if ("""target/scala\-[\w\.]+/classes$""".r.findFirstIn(userDir).isDefined) {

                val file = new File("../../../src/test/resources/"+resourceName)
                if (file.exists()) return file
            }
        }

        throw new IllegalArgumentException("Cannot locate resource: "+resourceName)
    }

}
