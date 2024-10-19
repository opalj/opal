/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ce

import java.io.File
import java.nio.file.Paths
import scala.collection.mutable.ListBuffer

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

/*
 *   Standalone configuration explorer
 *   This is the main method that runs the configuration explorer
 *   It creates a browsable HTML File out of the configuration files present in the entire OPAL project
 */
object ce extends App {
    println("Configuration Explorer Started")
    val buildVersion = System.getProperty("build.version", "unknown")
    val appHome = System.getProperty("app.home", "not set")
    println(s"Build Version: $buildVersion")
    println(s"Application Home: $appHome")

    // Load config with default filename for this application
    val conf = this.LoadConfig()

    val locator = new FileLocator(conf)
    val filepaths = locator.getConfigurationPaths

    // Bulk Imports all the configs
    val CPW = new CommentParserWrapper
    val configs = CPW.IterateConfigs(filepaths)

    // Replace class type values
    if (conf.getBoolean("org.opalj.ce.replaceSubclasses")) {
        val se = new SubclassExtractor(locator, buildVersion)
        for (config <- configs) {
            config.replaceClasses(se)
        }
    }

    // Export
    val HE = new HTMLExporter(
        configs.asInstanceOf[ListBuffer[ConfigNode]],
        Paths.get(locator.getProjectRoot + conf.getString("org.opalj.ce.html.template"))
    )
    HE.exportHTML(
        new File(locator.getProjectRoot + conf.getString("org.opalj.ce.html.export")),
        conf.getString("org.opalj.ce.html.headline"),
        conf.getString("org.opalj.ce.html.content"),
        conf.getBoolean("org.opalj.ce.html.sort_alphabetically")
    )

    /**
     * Loads default configuration of the configuration explorer
     * @return returns the configuration of the configuration explorer
     */
    def LoadConfig(): Config = {
        println("Loading configuration")
        val conf = ConfigFactory.load("ce")
        println(conf.getClass)

        conf
    }
}
