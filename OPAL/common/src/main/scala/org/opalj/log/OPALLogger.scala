/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package log

import scala.annotation.elidable
import scala.annotation.elidable.ASSERTION

/**
 * Facilitates the logging of messages that are relevant for the end user.
 *
 * ==Usage==
 * To use OPAL's logging facility use the companion object ([[OPALLogger$]]).
 *
 * @note   The OPALLogger framework is not intended to be used by developers to help
 *         debug analysis; it is intended to be used to inform (end)-users about the
 *         analysis progress.
 *
 * @author Michael Eichberg
 */
trait OPALLogger {

    /**
     * The given message is logged.
     */
    def log(message: LogMessage)(implicit ctx: LogContext): Unit

    /**
     * The given message is only logged once. This is particularly useful if an analysis
     * may hit a specific problem multiple times, but it is sufficient/meaningful to log
     * it only once.
     *
     * This method should not be used if the message may only be generated at most
     * once. The respective message will be cached in the log context.
     */
    final def logOnce(message: LogMessage)(implicit ctx: LogContext): Unit = {
        if (ctx.incrementAndGetCount(message) == 1) log(message)
    }

}

/**
 * OPAL's logging facility.
 *
 * ==Usage==
 * ===Basic===
 * E.g., using the global context and the convenience methods.
 * {{{
 * implicit val logContext : LogContext = org.opalj.log.GlobalContext
 * OPALLogger.info("project", "the project is garbage collected")
 * }}}
 * or
 * {{{
 * OPALLogger.info("project", "the project is garbage collected")(org.opalj.log.GlobalContext)
 * }}}
 * ===Advanced===
 * Logging a message only once.
 * {{{
 * implicit val logContext : LogContext = org.opalj.log.GlobalContext
 * OPALLogger.logOnce(Warn("project configuration", "the method cannot be resolved"))
 * }}}
 *
 * ==Initialization==
 * If the [[GlobalLogContext]] should not use the default [[ConsoleOPALLogger]] then the
 * logger can be changed using `updateLogger`.
 *
 * ==Thread Safety==
 * Thread safe.
 *
 * @author Michael Eichberg
 */
object OPALLogger extends OPALLogger {

    @volatile private[this] var loggers: Array[OPALLogger] = new Array(32);

    def updateLogger(ctx: LogContext, logger: OPALLogger): Unit = this.synchronized {
        val id = ctx.id
        assert(id != -1, "context is not yet registered")
        assert(id != -2, "context is already unregistered")
        loggers(id) = logger
    }

    /**
     * Registers the given context with the OPAL logging facility and associates the
     * specified logger with the context.
     */
    def register(
        ctx:    LogContext,
        logger: OPALLogger = new ConsoleOPALLogger(true)
    ): Unit = this.synchronized {
        if (ctx.id == -1) {
            val id = nextId
            if (nextId == loggers.length) {
                val newLoggers: Array[OPALLogger] = new Array(loggers.length * 2 + 1)
                System.arraycopy(loggers, 0, newLoggers, 0, loggers.length)
                loggers = newLoggers
            }
            loggers(id) = logger
            ctx.id = id
            nextId += 1
        } else if (ctx.id >= 0) {
            throw new RuntimeException("reregistration of a log context is not supported")
        } else {
            throw new RuntimeException("log contexts cannot be reregistered")
        }
    }

    def unregister(ctx: LogContext): Unit = this.synchronized {
        if (ctx == GlobalLogContext) {
            val message = "unregistering the core global log context is not supported"
            throw new IllegalArgumentException(message)
        }

        val ctxId = ctx.id
        // try to reuse log context id if possible
        if (ctxId + 1 == nextId) nextId = ctxId
        loggers(ctxId) = null
        ctx.id = -2
    }

    def isUnregistered(ctx: LogContext): Boolean = this.synchronized { ctx.id == -2 }

    def logger(ctx: LogContext): OPALLogger = {
        val ctxId = ctx.id
        if (ctxId == -2) {
            if (GlobalLogContext eq ctx)
                throw new UnknownError("the global log context was unregistered")
            else
                throw new IllegalArgumentException(s"the log context $ctx is already unregistered")
        }
        this.synchronized { loggers(ctxId) }
    }

    def globalLogger(): OPALLogger = this.synchronized { loggers(GlobalLogContext.id) }

    // stores the next context id - access must be explicitly synchronized!
    private[log] var nextId: Int = 0

    // IMPLEMENTATION OF THE LOGGING FACILITIES

    def log(message: LogMessage)(implicit ctx: LogContext): Unit = {
        logger(ctx).log(message)
    }

    /**
     * Debug message are only included in the code if assertions are turned on. If
     * debug message are logged, then they are logged as Info-level messages.
     */
    @elidable(ASSERTION)
    final def debug(category: String, message: String)(implicit ctx: LogContext): Unit = {
        log(Info(category, message))
    }

    /**
     * Debug message are only included in the code if assertions are turned on and the predicate
     * `p` evaluates to `true`.
     * If debug message are logged, then they are logged as Info-level messages.
     */
    @elidable(ASSERTION)
    final def debug(
        p:        => Boolean,
        category: String,
        message:  => String
    )(
        implicit
        ctx: LogContext
    ): Unit = {
        if (p) {
            log(Info(category, message))
        }
    }

    /**
     * Log some general information. General information may be related, e.g., to the overall
     * progress of the analysis, the results of an analysis, the major configuration
     * settings.
     *
     * This method is primarily a convenience method that creates an [[Info]] message
     * which is the logged.
     *
     * @note Do not use this method if the analysis may create the same message
     *      multiple times. In this case use [[logOnce]].
     */
    final def info(category: String, message: String)(implicit ctx: LogContext): Unit = {
        log(Info(category, message))
    }

    /**
     * Logs a message in the category "`progress`".
     */
    final def progress(message: String)(implicit ctx: LogContext): Unit = {
        log(Info("progress", message))
    }

    /**
     * Log a warning. Warnings are typically related to incomplete project configurations
     * that may affect the overall precision of the analysis, but which are not rendering
     * the analysis meaningless.
     *
     * This method is primarily a convenience method that creates an [[Warn]] message
     * which is the logged.
     *
     * @note Do not use this method if the analysis may create the same message
     *      multiple times. In this case use [[logOnce]].
     */
    final def warn(category: String, message: String)(implicit ctx: LogContext): Unit = {
        log(Warn(category, message))
    }

    /**
     * Log an error message. Error message should either be related to internal errors
     * or to project configurations that are so badly broken that a meaningful analysis is
     * not possible.
     *
     * This method is primarily a convenience method that creates an [[Error]] message
     * which is the logged.
     */
    final def error(category: String, message: String)(implicit ctx: LogContext): Unit = {
        log(Error(category, message))
    }

    /**
     * Log an error message. Error message should either be related to internal errors
     * or to project configurations that are so badly broken that a meaningful analysis is
     * not possible.
     *
     * This method is primarily a convenience method that creates an [[Error]] message
     * which is the logged.
     */
    final def error(
        category: String,
        message:  String,
        t:        Throwable
    )(
        implicit
        ctx: LogContext
    ): Unit = {
        log(Error(category, message, t))
    }
}
