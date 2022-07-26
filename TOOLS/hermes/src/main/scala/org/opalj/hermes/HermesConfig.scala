/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes

import java.io.File

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions

/**
 * Global configuration settings initialized when the application configuration file is
 * read.
 *
 * @author Michael Eichberg
 */
trait HermesConfig {

    // ---------------------------------------------------------------------------------------------
    //
    // INITIALIZATION
    //
    // ---------------------------------------------------------------------------------------------

    private[this] var isInitialized: Boolean = false

    private[this] var config: Config = null

    /**
     * The global configuration file.
     */
    final lazy val Config: Config = validateInitialized { config }

    /**
     * Reads the initial, overall configuration. This method or setConfig has to be called
     * before Hermes can be used.
     */
    def initialize(configFile: File): Unit = {
        import Console.err
        if (!configFile.exists || !configFile.canRead()) {
            err.println(s"The config file cannot be found or read: $configFile")
            err.println("The current folder is: "+System.getProperty("user.dir"))
            System.exit(2)
        }
        try {
            val baseConfig = ConfigFactory.load("hermes.conf")
            val config = ConfigFactory.parseFile(configFile).withFallback(baseConfig)
            setConfig(config)
        } catch {
            case t: Throwable =>
                err.println(s"Failed while reading: $configFile; ${t.getMessage()}")
                System.exit(3)
                //... if System.exit does not terminate the app; this will at least kill the
                // the current call.
                throw t;
        }
    }

    /**
     * Sets the used configuration object.
     */
    def setConfig(config: Config): Unit = {
        if (isInitialized) {
            throw new IllegalStateException("configuration is already set");
        }

        this.config = config
        isInitialized = true
    }

    private[this] def validateInitialized[@specialized(Int, Boolean, Long, Double, Float) T](
        f: => T
    ): T = {
        if (!isInitialized)
            throw new IllegalStateException("configuration is not yet set")
        else
            f
    }

    /** Textual representation of the configuration related to OPAL/Hermes.  */
    def renderConfig: String = {
        val rendererConfig = ConfigRenderOptions.defaults().setOriginComments(false)
        config.getObject("org.opalj").render(rendererConfig)
    }

    // ---------------------------------------------------------------------------------------------
    //
    // ACCESSORS
    //
    // ---------------------------------------------------------------------------------------------

    /** The config key of the number of locations per feature pre project that is stored. */
    final val MaxLocationsKey: String = "org.opalj.hermes.maxLocations"

    /** The number of locations per feature pre project that is stored. */
    final lazy val MaxLocations: Int = validateInitialized { Config.getInt(MaxLocationsKey) }

}
