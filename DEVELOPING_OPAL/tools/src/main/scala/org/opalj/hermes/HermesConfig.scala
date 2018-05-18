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
package hermes

import java.io.File

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions

/**
 * Global configuration settings initialized when the application configuration file is
 * read.
 *
 * @author Michael Eichberg
 */
trait HermesConfig {

    // ---------------------------------------------------------------------------------------------
    //
    // INITIALIZATION
    //
    // ---------------------------------------------------------------------------------------------

    private[this] var isInitialized: Boolean = false

    private[this] var config: Config = null

    /**
     * The global configuration file.
     */
    final lazy val Config: Config = validateInitialized { config }

    /**
     * Reads the initial, overall configuration. This method or setConfig has to be called
     * before Hermes can be used.
     */
    def initialize(configFile: File): Unit = {
        import Console.err
        if (!configFile.exists || !configFile.canRead()) {
            err.println(s"The config file cannot be found or read: $configFile")
            err.println("The current folder is: "+System.getProperty("user.dir"))
            System.exit(2)
        }
        try {
            val config = ConfigFactory.parseFile(configFile).withFallback(ConfigFactory.load())
            setConfig(config)
        } catch {
            case t: Throwable ⇒
                err.println(s"Failed while reading: $configFile; ${t.getMessage()}")
                System.exit(3)
                //... if System.exit does not terminate the app; this will at least kill the
                // the current call.
                throw t;
        }
    }

    /**
     * Sets the used configuration object.
     */
    def setConfig(config: Config): Unit = {
        if (isInitialized) {
            throw new IllegalStateException("configuration is already set");
        }

        this.config = config
        isInitialized = true
    }

    private[this] def validateInitialized[@specialized(Int, Boolean, Long, Double, Float) T](
        f: ⇒ T
    ): T = {
        if (!isInitialized)
            throw new IllegalStateException("configuration is not yet set")
        else
            f
    }

    /** Textual representation of the configuration related to OPAL/Hermes.  */
    def renderConfig: String = {
        val rendererConfig = ConfigRenderOptions.defaults().setOriginComments(false)
        config.getObject("org.opalj").render(rendererConfig)
    }

    // ---------------------------------------------------------------------------------------------
    //
    // ACCESSORS
    //
    // ---------------------------------------------------------------------------------------------

    /** The config key of the number of locations per feature pre project that is stored. */
    final val MaxLocationsKey: String = "org.opalj.hermes.maxLocations"

    /** The number of locations per feature pre project that is stored. */
    final lazy val MaxLocations: Int = validateInitialized { Config.getInt(MaxLocationsKey) }

}
