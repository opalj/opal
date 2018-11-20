/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package cp

/**
 * Used to report that a requirement related to a constant pool entry cannot be satisfied by the
 * constant pool. E.g., an index is too large.
 *
 * @author  Michael Eichberg
 */
case class ConstantPoolException(message: String) extends Exception(message)

