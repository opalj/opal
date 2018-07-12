/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package analyses.cg

import org.opalj.br.Method
import org.opalj.ai.InterpretationFailedException

/**
 * Encapsulates an exception that is thrown during the creation of the call graph.
 *
 * In general, we do not abort the construction of the overall call graph if an exception
 * is thrown.
 *
 * @author Michael Eichberg
 */
case class CallGraphConstructionException(method: Method, cause: Throwable) {

    import Console._

    def toFullString: String = {
        var message = s"While analyzing: ${method.toJava}\n\t"
        val realCause =
            cause match {
                case ife: InterpretationFailedException ⇒
                    message += "the abstract interpretation failed:\n\t"
                    message += "pc="+ife.pc+"\n\t"
                    message += "operands="+ife.operandsArray(ife.pc)+"\n\t"
                    message += ife.worklist.mkString("worklist=", ",", "\n\t")
                    message += ife.evaluated.mkString("evaluated=", ",", "\n\t")
                    ife.cause
                case _ ⇒
                    cause
            }

        message += realCause.toString
        message += realCause.getStackTrace.map(_.toString()).mkString("\n\t", "\n\t", "")
        message
    }

    override def toString: String = {
        val realCause =
            cause match {
                case ife: InterpretationFailedException ⇒ ife.cause
                case _                                  ⇒ cause
            }

        val stacktrace = realCause.getStackTrace
        val stacktraceExcerpt =
            if (stacktrace != null && stacktrace.size > 0) {
                val top = stacktrace(0)
                Some(top.getFileName+"["+top.getClassName+":"+top.getLineNumber+"]")
            } else
                None

        method.toJava(
            " ⚡ "+RED +
                realCause.getClass.getSimpleName+": "+
                realCause.getMessage +
                stacktraceExcerpt.map(": "+_).getOrElse("") +
                RESET
        )
    }
}
