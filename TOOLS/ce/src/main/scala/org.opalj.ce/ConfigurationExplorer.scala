/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ce

import java.io.File
import java.nio.file.Paths

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

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
    val conf = LoadConfig()

    val locator = new FileLocator(conf)
    val filepaths = locator.getConfigurationPaths

    // Bulk Imports all the configs
    val CPW = new CommentParserWrapper
    val configs = CPW.iterateConfigs(filepaths, Paths.get(locator.getProjectRoot))

    // Replace class type values
    if (conf.getBoolean("org.opalj.ce.replaceSubclasses")) {
        val se = new SubclassExtractor(locator.FindJarArchives(buildVersion).toArray)
        for (config <- configs) {
            config.replaceClasses(se)
        }
    }

    // Export
    val HE = new HTMLExporter(
        configs,
        Paths.get(locator.getProjectRoot + conf.getString("org.opalj.ce.html.template"))
    )
    HE.exportHTML(conf, new File(locator.getProjectRoot + conf.getString("org.opalj.ce.html.export")))

    /**
     * Loads default configuration of the configuration explorer
     * @return returns the configuration of the configuration explorer
     */
    def LoadConfig(): Config = {
        OPALLogger.info("Configuration Explorer", "Loading configuration")
        val conf = ConfigFactory.load("ce")

        conf
    }
}
