/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package concurrent

/**
 * An exception that is used to signal that some exceptions occured concurrently.
 * Those exceptions are added to this exception by the underlying framework and can then be
 * queried using the standard methods.
 *
 * This exception is not associated with a stack trace, because it is not the root
 * cause of the problem.
 *
 * @author Michael Eichberg
 */
class ConcurrentExceptions extends Exception("concurrent exceptions occurred", null, true, false)
