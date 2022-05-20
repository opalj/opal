/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import com.typesafe.config.Config

import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext

/**
 * Defines various settings related to reading/processing class files. To change
 * the default configuration, override the respective `val` using '''early initializers'''!
 *
 * @example
 * {{{
 * class ConfiguredFramework extends {
 *      override implicit val logContext: LogContext = theLogContext
 *      override implicit val config: Config = theConfig
 * } with Java9FrameworkWithInvokedynamicSupportAndCaching(cache)
 * new ConfiguredFramework
 * }}}
 */
trait ClassFileReaderConfiguration {

    def defaultLogContext: LogContext = GlobalLogContext
    def defaultConfig: Config = BaseConfig

    /**
     * The [[org.opalj.log.LogContext]] that should be used to log rewritings.
     *
     * @note    The [[org.opalj.log.LogContext]] is typically either the
     *          [[org.opalj.log.GlobalLogContext]] or a project specific log context.
     */
    implicit val logContext: LogContext = defaultLogContext

    /**
     * The `Config` object that will be used to read the configuration settings for
     * reading in class files.
     */
    implicit val config: Config = defaultConfig

    /**
     * If `true` method bodies are never loaded.
     */
    def loadsInterfacesOnly: Boolean

}
