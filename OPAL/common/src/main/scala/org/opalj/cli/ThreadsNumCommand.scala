/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import org.opalj.concurrent.NumberOfThreadsForCPUBoundTasks

import org.rogach.scallop.intConverter

object ThreadsNumCommand extends PlainCommand[Int] {
    override val name: String = "threads"
    override val argName: String = "threadsNum"
    override val description: String = "Number of threads to be used; 0 for entirely sequential execution"
    override val defaultValue: Option[Int] = Some(NumberOfThreadsForCPUBoundTasks)
    override val noshort: Boolean = false
}
