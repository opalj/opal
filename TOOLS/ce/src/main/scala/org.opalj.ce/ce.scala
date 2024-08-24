/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ce

import com.typesafe.config._

/*
*   Standalone configuration explorer
*   This is the main method that runs the configuration explorer
*/
object ce {
    def main(args: Array[String]): Unit = {
        println("Configuration Explorer Started")

        // Load config with default filename for this application
        val conf = this.LoadConfig()

        println(conf.getObject("org.opalj"))

        val locator = new FileLocator(conf)
        val filepaths = locator.SearchFiles

        // Bulk Imports all the configs
        val CPW = new CommentParserWrapper
        val configs = CPW.IterateConfigs(filepaths)
        println(configs)

        println()
        println(conf.getObject("org.opalj"))

        val SubstringTest: String = "\"This is a test\""
        println(SubstringTest.substring(2))
    }

    def LoadConfig() : Config = {
        println("Loading configuration")
        val conf = ConfigFactory.load("ce")
        println(conf.getClass)

        conf
    }
}