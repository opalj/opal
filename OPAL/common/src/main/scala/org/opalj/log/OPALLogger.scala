/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
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
 * @note The OPALLogger framework is not intended to be used by developers to help
 *      debug analysis, but is intended to be used to inform (end)-users about the
 *      analysis progress.
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
     * once.
     */
    def logOnce(message: LogMessage)(implicit ctx: LogContext): Unit

}

/**
 * OPAL's logging facility.
 *
 * ==Usage==
 * ===Basic===
 * E.g., using the global context and the convenience methods.
 * {{{
 * implicit val logContext = org.opalj.log.GlobalContext
 * OPALLogger.info("project", "the project is garbage collected")
 * }}}
 * or
 * {{{
 * OPALLogger.info("project", "the project is garbage collected")(org.opalj.log.GlobalContext)
 * }}}
 * ===Advanced===
 * Logging a message only once.
 * {{{
 * implicit val logContext = org.opalj.log.GlobalContext
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
        println("context will be registered: "+ctx)
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
        } else
            throw new RuntimeException("reregistration of an unregistered log context is not supported")
    }

    def unregister(ctx: LogContext): Unit = this.synchronized {
        if (ctx == GlobalLogContext) {
            val message = "unregistering the core global log context is not supported"
            throw new IllegalArgumentException(message)
        }

        val ctxId = ctx.id
        if (ctxId + 1 == nextId) nextId = ctxId
        loggers(ctxId) = null
        ctx.id = -2
    }

    def logger(ctx: LogContext): OPALLogger = this.synchronized { loggers(ctx.id) }

    def globalLogger(): OPALLogger = this.synchronized { loggers(GlobalLogContext.id) }

    // stores the next context id - access must be explicitly synchronized!
    private[log] var nextId: Int = 0

    // IMPLEMENTATION OF THE LOGGING FACILITIES

    def log(message: LogMessage)(implicit ctx: LogContext): Unit = {
        logger(ctx).log(message)
    }

    def logOnce(message: LogMessage)(implicit ctx: LogContext): Unit = {
        logger(ctx).logOnce(message)
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
     * Debug message are only included in the code if assertions are turned on and the predicate `p`
     * evaluates to `true`. If
     * debug message are logged, then they are logged as Info-level messages.
     */
    @elidable(ASSERTION)
    final def debug(
        p:        ⇒ Boolean,
        category: String,
        message:  ⇒ String
    )(implicit ctx: LogContext): Unit = {
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
    )(implicit ctx: LogContext): Unit = {
        log(Error(category, message, t))
    }
}

