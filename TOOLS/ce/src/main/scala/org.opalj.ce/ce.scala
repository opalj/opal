/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ce

import com.typesafe.config.{Config,ConfigFactory}

/*
*   Standalone configuration explorer
*
*/
object ce {
    def main(args: Array[String]): Unit = {
      println("Configuration Explorer Started")

      // Load config with default filename for this application
      val conf = this.LoadConfig()

      val locator = new FileLocator(conf)
      locator.SearchFiles()
    }

    def LoadConfig() : Config = {
      println("Loading configuration")
      val conf = ConfigFactory.load("ce")
      return conf
    }
}