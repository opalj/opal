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
package ai
package project

import br._

/**
 * Encapsulates an exception that is thrown during the creation of the call graph.
 *
 * In general, we do not abort the construction of the overall call graph if an exception
 * is thrown.
 *
 * @author Michael Eichberg
 */
case class CallGraphConstructionException(
        classFile: ClassFile,
        method: Method,
        cause: Exception) {

    import Console._

    def toFullString: String = {
        var message = "While analyzing: "+classFile.thisType.toJava+"{ "+method.toJava+" }\n\t"
        val realCause =
            cause match {
                case ife: InterpretationFailedException ⇒
                    message += "[the abstract interpretation failed] reason:\n\t"
                    ife.cause
                case _ ⇒
                    cause
            }

        message += realCause.toString
        message += realCause.getStackTrace().map(_.toString()).mkString("\n\t", "\n\t", "")
        message
    }

    override def toString: String = {
        val realCause =
            cause match {
                case ife: InterpretationFailedException ⇒ ife.cause
                case _                                  ⇒ cause
            }

        val stacktrace = realCause.getStackTrace()
        val stacktraceExcerpt =
            if (stacktrace != null && stacktrace.size > 0) {
                val top = stacktrace(0)
                Some(top.getFileName()+"["+top.getClassName()+":"+top.getLineNumber()+"]")
            } else
                None
        classFile.thisType.toJava+"{ "+
            method.toJava+" ⚡ "+
            RED +
            realCause.getClass().getSimpleName()+": "+
            realCause.getMessage() +
            stacktraceExcerpt.map(": "+_).getOrElse("") +
            RESET+
            " }"
    }
}

