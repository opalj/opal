/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ide.util

import org.opalj.br.analyses.SomeProject
import org.opalj.ide.ConfigKeyDebugLog
import org.opalj.ide.ConfigKeyTraceLog
import org.opalj.ide.FrameworkName
import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger

/**
 * Logging extension for IDE
 */
trait Logging {
    /**
     * Whether debug messages should be logged
     */
    val isDebug: Boolean
    /**
     * Whether trace messages should be logged
     */
    val isTrace: Boolean

    /**
     * Log an info message
     */
    protected def logInfo(message: => String)(implicit ctx: LogContext): Unit = {
        OPALLogger.info(FrameworkName, message)
    }

    /**
     * Log a warn message
     */
    protected def logWarn(message: => String)(implicit ctx: LogContext): Unit = {
        OPALLogger.warn(FrameworkName, message)
    }

    /**
     * Log a debug message
     */
    protected def logDebug(message: => String)(implicit ctx: LogContext): Unit = {
        OPALLogger.debug({ isDebug }, s"$FrameworkName - debug", message)
    }

    /**
     * Log a trace message
     */
    protected def logTrace(message: => String)(implicit ctx: LogContext): Unit = {
        OPALLogger.debug({ isTrace }, s"$FrameworkName - trace", message)
    }
}

object Logging {
    /**
     * Logging extension for IDE where debug and trace messages are enabled
     */
    trait EnableAll extends Logging {
        override val isDebug: Boolean = true
        override val isTrace: Boolean = true
    }

    /**
     * Logging extension for IDE where debug and trace messages are en-/disabled dynamically based on the configuration
     * of the given project
     */
    trait ByProjectConfig extends Logging {
        val project: SomeProject

        lazy val isDebug: Boolean = project.config.getBoolean(ConfigKeyDebugLog)
        lazy val isTrace: Boolean = project.config.getBoolean(ConfigKeyTraceLog)
    }

    /**
     * Drop-in of the global log context (when no log context is present)
     */
    trait GlobalLogContext extends Logging {
        implicit val logContext: LogContext = GlobalLogContext
    }
}
