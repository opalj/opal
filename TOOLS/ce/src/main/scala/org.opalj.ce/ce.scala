/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ce

import com.typesafe.config._

import java.io.File
import java.nio.file.Paths

/*
*   Standalone configuration explorer
*   This is the main method that runs the configuration explorer
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

    def LoadConfig() : Config = {
        println("Loading configuration")
        val conf = ConfigFactory.load("ce")
        println(conf.getClass)

        conf
    }
}