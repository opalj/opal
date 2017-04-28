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

import com.typesafe.config.Config
import com.typesafe.config.ConfigRenderOptions

/**
 * Global configuration settings initialized when the application configuration file is
 * read.
 *
 * @author Michael Eichberg
 */
object Globals {

    // local state
    private[this] var isInitialized: Boolean = false
    private[this] var config: Config = null

    // ---------------------------------------------------------------------------------------------
    //
    // Configuration parameters
    //
    // ---------------------------------------------------------------------------------------------

    /** The config key of the number of locations per feature pre project that is stored. */
    final val MaxLocationsKey: String = "org.opalj.hermes.maxLocations"
    private[this] var maxLocations: Int = 0

    // ---------------------------------------------------------------------------------------------
    //
    // INITIALIZATION
    //
    // ---------------------------------------------------------------------------------------------

    /**
     * Sets the used configuration object.
     */
    private[hermes] def setConfig(config: Config): Unit = {
        if (isInitialized) {
            throw new IllegalStateException("configuration is already set")
        }
        this.config = config
        isInitialized = true

        maxLocations = config.getInt(MaxLocationsKey)
    }

    private[this] def validateInitialized[@specialized(Int, Boolean, Long, Double, Float) T](
        f: ⇒ T
    ): T = {
        if (!isInitialized)
            throw new IllegalStateException("configuration is not yet set")
        else
            f
    }

    // ---------------------------------------------------------------------------------------------
    //
    // ACCESSORS
    //
    // ---------------------------------------------------------------------------------------------

    /**
     * The global configuration file.
     */
    final def Config: Config = validateInitialized { config }

    /** Textual representation of the configuration related to OPAL/Hermes.  */
    def renderConfig: String = {
        val rendererConfig = ConfigRenderOptions.defaults().setOriginComments(false)
        config.getObject("org.opalj").render(rendererConfig)
    }

    /** The number of locations per feature pre project that is stored. */
    final def MaxLocations: Int = validateInitialized { maxLocations }

}
