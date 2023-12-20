/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj

import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger.info

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

package object ifds {

    final val FrameworkName = "OPAL IFDS"

    {
        implicit val logContext: LogContext = GlobalLogContext
        try {
            assert(false) // <= test whether assertions are turned on or off...
            info(FrameworkName, "Production Build")
        } catch {
            case _: AssertionError => info(FrameworkName, "Development Build with Assertions")
        }
    }

    // We want to make sure that the class loader is used which potentially can
    // find the config files; the libraries (e.g., Typesafe Config) may have
    // been loaded using the parent class loader and, hence, may not be able to
    // find the config files at all.
    val BaseConfig: Config = ConfigFactory.load(this.getClass.getClassLoader)

    final val ConfigKeyPrefix = "org.opalj.ifds."
}
