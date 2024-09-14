/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ce

import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config
import java.io.File
import java.nio.file.Paths

/*
*   Standalone configuration explorer
*   This is the main method that runs the configuration explorer
*   It creates a browsable HTML File out of the configuration files present in the entire OPAL project
*/
object ce {
    def main(args: Array[String]): Unit = {
        println("Configuration Explorer Started")

        // Load config with default filename for this application
        val conf = this.LoadConfig()

        val locator = new FileLocator(conf)
        val filepaths = locator.SearchFiles

        // Bulk Imports all the configs
        val CPW = new CommentParserWrapper
        val configs = CPW.IterateConfigs(filepaths)

        val HE = new HTMLExporter(configs, Paths.get(conf.getString("user.dir") + conf.getString("org.opalj.ce.html.template")))
        // Join File Paths for export

        HE.exportHTML(new File(conf.getString("user.dir") + conf.getString("org.opalj.ce.html.export")), conf.getString("org.opalj.ce.html.headline"),conf.getString("org.opalj.ce.html.content"))
    }

    /**
     * Loads default configuration of the configuration explorer
     * @return returns the configuration of the configuration explorer
     */
    def LoadConfig() : Config = {
        println("Loading configuration")
        val conf = ConfigFactory.load("ce")
        println(conf.getClass)

        conf
    }
}