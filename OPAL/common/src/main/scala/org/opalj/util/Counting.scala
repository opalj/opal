/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package util

import org.opalj.concurrent.Locking

import scala.collection.mutable

/**
 * A simple class that enables the counting of something.
 *
 * Examples:
 *   - How often is a function called during runtime?
 *   - How often runs the program in a specific case?
 *
 * ==Thread Safety==
 * This class is thread safe.
 *
 * @author Michael Reif
 */
trait Counting extends Locking {

    private[this] val count = mutable.Map.empty[Symbol, Long]

    final def incrementCount(s: Symbol): Unit = {
        withWriteLock { updateCount(s, 1L) }
    }

    /**
     * Updates the count related to the entity identified by the given symbol.
     *
     * If the passed `value` is positive the count will be increased whereas it will be
     * decreased when a negative number is passed.
     *
     * @param   s Symbol used to correlate values related to the same entity.
     * @param   value The value that will be added to the entity's current value.
     */
    final def updateCount(s: Symbol, value: Long): Unit = {
        withWriteLock { doUpdateCount(s, value) }
    }

    /**
     * Called by the `updateCount(Symbol, Int)` method.
     *
     * ==Thread Safety==
     * The `updateCount` method takes care of the synchronization.
     */
    protected[this] def doUpdateCount(s: Symbol, value: Long): Unit = {
        val oldValue = count.getOrElseUpdate(s, 0L)
        count.update(s, oldValue + value)
    }

    /**
     * Returns the overall `count` that has been summed up with the given symbol `s`.
     */
    def getCount(s: Symbol): Long = withReadLock { doGetCount(s) }

    /**
     * Called by the `getCount(Symbol)` method.
     *
     * ==Thread Safety==
     * The `getCount` method takes care of the synchronization.
     */
    protected[this] def doGetCount(s: Symbol): Long = count.getOrElse(s, 0L)

    /**
     * Resets the overall count of the given symbol.
     */
    def reset(s: Symbol): Unit = withWriteLock { doReset(s) }

    /**
     * Called by the `reset(Symbol)` method.
     *
     * ==Thread Safety==
     * The `reset` method takes care of the synchronization.
     */
    private[this] def doReset(s: Symbol): Unit = count.remove(s)

    /**
     * Resets everything. The effect is comparable to creating a new
     * instance, but is more efficient.
     */
    def resetAll(): Unit = withWriteLock { doResetAll() }

    /**
     * Called by the `resetAll` method.
     *
     * ==Thread Safety==
     * The `resetAll` method takes care of the synchronization.
     */
    private[this] def doResetAll(): Unit = count.clear()

}

class IntStatistics extends Counting
