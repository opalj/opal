/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ce

import com.typesafe.config.Config

class FileLocator(var config: Config)  {
    println("FileLocator created. Initializing...")
    def LocateConfigurationFiles(): Unit = {

    }

    def getProjectRoot() : String = {
      val projectNames = this.config.getList("org.opalj.ce.configurationFilenames")
      val projectRoot = this.config.getString("user.dir")
      println("Searching for the following Filenames: " + projectNames)
      println("Searching in the following directory: " + projectRoot)
      return ""
    }
}