/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.support.debug

import org.opalj.ai.AIResult

/**
 * The standard XHTML tracer which optionally generates some output.
 *
 * @author Michael Eichberg
 */
class ConfigurableXHTMLTracer(openDumpOnResult: Boolean = false) extends XHTMLTracer {

    override def result(result: AIResult): Unit = if (openDumpOnResult) super.result(result)

}
