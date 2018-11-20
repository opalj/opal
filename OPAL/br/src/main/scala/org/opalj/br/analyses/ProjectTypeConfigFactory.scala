/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config

/**
 * Factory to create `Config` objects with a given project type
 * (see [[org.opalj.ProjectTypes]]).
 *
 * This is particularly useful for testing purposes as it facilitates tests which
 * are independent of the current (default) configuration.
 */
object ProjectTypeConfigFactory {

    private[this] final def createConfig(mode: String) = {
        s"""org.opalj.project { type = "$mode"}"""
    }

    private[this] final def libraryConfig: String = {
        createConfig("library")
    }

    private[this] final def guiApplicationConfig: String = {
        createConfig("gui application")
    }

    private[this] final def commandLineApplicationConfig: String = {
        createConfig("command-line application")
    }

    private[this] final def jee6WebApplicationConfig: String = {
        createConfig("jee6+ web application")
    }

    def createConfig(value: ProjectType): Config = {
        import ProjectTypes._
        ConfigFactory.parseString(
            value match {
                case Library                ⇒ libraryConfig
                case CommandLineApplication ⇒ commandLineApplicationConfig
                case GUIApplication         ⇒ guiApplicationConfig
                case JEE6WebApplication     ⇒ jee6WebApplicationConfig
            }
        )
    }

    def resetProjectType[Source](
        project:                Project[Source],
        newProjectType:         ProjectType,
        useOldConfigAsFallback: Boolean         = true
    ): Project[Source] = {
        val testConfig = ProjectTypeConfigFactory.createConfig(newProjectType)
        Project.recreate(project, testConfig, useOldConfigAsFallback)
    }
}
