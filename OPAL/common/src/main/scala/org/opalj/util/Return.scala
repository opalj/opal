/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org
package opalj
package util

// TODO: Replace this class by scala.util.control.NonLocalReturns in Scala 3
case class Return[T](t: T) extends Throwable
