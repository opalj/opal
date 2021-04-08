/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package log

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.ConcurrentHashMap

/**
 * A log context associates log messages with a specific context and logger.
 * Using a log context facilitates the suppression of recurring message in a specific context
 * and also makes it possible to direct messages to different targets.
 * Before using a `LogContext` it has to be registered with the [[OPALLogger$]].
 *
 * OPAL uses two primary log contexts:
 *
 *  1. The [[GlobalLogContext$]] which should be used for general
 *      log messages related to OPAL, such as the number of threads used for
 *      computations.
 *
 *  1. The log context associated with [[org.opalj.br.analyses.Project]]s to log
 *      project related information (e.g., such as project related results or
 *      configuration issues.)
 *
 * @note The registration of the `LogContext` with the `OPALLogger` does not prevent
 *      the garbage collection of the `LogContext` unless a logged message explicitly
 *      references its log context. This is – however – discouraged! If no message
 *      explicitly reference the log context it is then possible to unregister the log
 *      context in the `finalize` method that references the context.
 *
 * @author Michael Eichberg
 */
trait LogContext {

    @volatile private[log] var id: Int = -1

    /**
     * The unique id associated with this log context. Each log context gets a unique id
     * when it is registered with the OPALLogger. This id will not change afterwards.
     */
    final def logContextId: Int = id

    def newInstance: LogContext

    /**
     * Creates a new log context that is the successor of this context and which will
     * automatically be associated with the same logger as this `LogContext`.
     */
    def successor: LogContext = {
        val newLogContext = newInstance;
        val logger = OPALLogger.logger(this)
        OPALLogger.register(newLogContext, logger)
        newLogContext
    }

    private[this] final val messages = new ConcurrentHashMap[LogMessage, AtomicInteger]()

    /**
     * Increments the counter for the given message and returns the new value.
     */
    final def incrementAndGetCount(message: LogMessage): Int = {
        val existingCounter = messages.putIfAbsent(message, new AtomicInteger(1))
        if (existingCounter != null)
            existingCounter.incrementAndGet()
        else
            1
    }
}
