/* License (BSD Style License):
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
package findrealbugs

import java.io._
import java.util.Properties

/**
 * Provides methods for loading and saving a list of all disabled analysis names to/from
 * a Java properties file.
 *
 * @author Florian Brandherm
 */
object ConfigurationFile {
    /**
     * Name of the property that holds the list of disabled analyses.
     */
    val disabledAnalysesPropertyName = "disabled_analyses"

    /**
     * Returns an `Iterable` of the names of all analyses that should be disabled.
     * Throws `IOException`s if the file could not be found or processed correctly,
     * or if the file name doesn't end with ".properties".
     *
     * @param filename Path to the properties file that should be loaded.
     * @return `Iterable` containing the names of all analyses that should be disabled.
     */
    def getDisabledAnalysesNamesFromFile(filename: String): Iterable[String] = {
        if (!filename.endsWith(".properties")) {
            throw new IOException("file "+filename+" is not a property file.")
        }

        var stream: InputStream = null
        var disabledAnalyses = ""

        try {
            val properties = new Properties()
            stream = new BufferedInputStream(new FileInputStream(filename))
            properties.load(stream)
            // Default value is the empty string: no analyses are disabled
            disabledAnalyses = properties.getProperty(disabledAnalysesPropertyName, "")
        } catch {
            case ex: IllegalArgumentException ⇒
                throw new IOException("file "+filename+" is not a property file.", ex)
        } finally {
            // If necessary try to close the file input stream
            if (stream != null) {
                stream.close()
            }
        }

        disabledAnalyses.split(",")
    }

    /**
     * Saves a list of analyses that should be disabled to a properties file.
     * Throws `IOException`s if the file could not be written correctly.
     *
     * @param filename Path of the new properties file that should be created.
     * @param disabledAnalyses `Iterable` containing the names of all analyses that should
     * be disabled.
     */
    def saveDisabledAnalysesToFile(filename: String, disabledAnalyses: Iterable[String]) {
        var stream: OutputStream = null

        try {
            val properties = new Properties()
            stream = new BufferedOutputStream(new FileOutputStream(filename))

            properties.setProperty(disabledAnalysesPropertyName,
                disabledAnalyses.mkString(","))
            properties.store(stream, null)
        } finally {
            // If necessary try to close the file input stream
            if (stream != null) {
                stream.close()
            }
        }
    }
}
