/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package axa
package proxy

trait IFrameworkProxy[State] {
  def analyze(code: String): State

  def resume(code: String): State
}