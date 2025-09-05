/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ce

import java.net.URL

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

import org.opalj.br.analyses.Project
import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger

/*
 *   Standalone configuration explorer.
 *   This is the main method that runs the configuration explorer.
 *   It creates a browsable HTML File out of the configuration files present in the entire OPAL project.
 */
object ConfigurationExplorer extends App {
    implicit val logContext: LogContext = GlobalLogContext

    OPALLogger.info("Configuration Explorer", "Configuration Explorer Started")
    val buildVersion = System.getProperty("build.version", "unknown")

    // Load config with default filename for this application
    val conf = loadConfig()

    val locator = new FileLocator(conf)
    val filepaths = locator.getConfigurationPaths

    // Bulk Imports all the configs
    val configs = CommentParser.iterateConfigs(filepaths, locator.projectRoot)

    implicit val project: Project[URL] =
        Project.apply(locator.findJarArchives(buildVersion).toArray, Array(org.opalj.bytecode.JavaBase))

    // Replace class type values
    if (conf.getBoolean("org.opalj.ce.replaceSubclasses")) {
        val se = new SubclassExtractor(project)
        for (config <- configs) {
            config.replaceClasses(se)
        }
    }

    // Export
    new HTMLExporter(conf).exportHTML(
        configs.reverse,
        locator.projectRoot.resolve(conf.getString("org.opalj.ce.html.template")),
        locator.projectRoot.resolve(
            conf.getString("org.opalj.ce.html.export").replace("$scalaVersion", util.ScalaMajorVersion)
        ).toFile
    )

    /**
     * Loads default configuration of the configuration explorer
     * @return returns the configuration of the configuration explorer
     */
    def loadConfig(): Config = {
        OPALLogger.info("Configuration Explorer", "Loading configuration")
        val conf = ConfigFactory.load("ce")

        conf
    }
}
