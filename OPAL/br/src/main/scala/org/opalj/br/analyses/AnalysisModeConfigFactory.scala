/**
 * BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
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
package br
package analyses

import org.opalj.AnalysisModes._
import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config

/**
 * Simple factory that can create a new config by a given analysis mode. This is necessary
 * for test purposes because the analysis mode, which is configured in the configuration file,
 * has to be ignored to implement config file independent tests.
 */
object AnalysisModeConfigFactory {

    private[this] final def createConfig(mode: String) = {
        s"""org.opalj { analysisMode = "$mode"}"""
    }

    private[this] final val cpaConfig: String = {
        createConfig("library with closed packages assumption")
    }

    private[this] final val opaConfig: String = {
        createConfig("library with open packages assumption")
    }

    private[this] final val desktopAppConfig: String = {
        createConfig("desktop pplication")
    }

    private[this] final val jee6WebAppConfig: String = {
        createConfig("jee6 web application")
    }

    def createConfig(value: AnalysisMode): Config = {
        ConfigFactory.parseString(
            value match {
                case LibraryWithOpenPackagesAssumption   ⇒ opaConfig
                case LibraryWithClosedPackagesAssumption ⇒ cpaConfig
                case DesktopApplication                  ⇒ desktopAppConfig
                case JEE6WebApplication                  ⇒ jee6WebAppConfig
            }
        )
    }

    def resetAnalysisMode(project: SomeProject, mode: AnalysisMode): SomeProject = {
        val testConfig = AnalysisModeConfigFactory.createConfig(mode)
        Project.recreate(project, testConfig)
    }
}
