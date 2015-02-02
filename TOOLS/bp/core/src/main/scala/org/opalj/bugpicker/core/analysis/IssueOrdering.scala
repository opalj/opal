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
package bugpicker
package core
package analysis

/**
 * Defines a partial order on issue that sorts [[Issue]]s
 * first by their class, then by the method(signature) then
 * by the line in which the issue occurs (alternatively by
 * the pc) and at last by the summary.
 *
 * @author Michael Eichberg
 */
object IssueOrdering extends scala.math.Ordering[Issue] {

    def compare(x: Issue, y: Issue): Int = {
        if (x.classFile.fqn < y.classFile.fqn) {
            return -1;
        }
        if (x.classFile.fqn > y.classFile.fqn)
            return 1;

        if (x.method.isDefined && y.method.isEmpty)
            return 1;

        if (x.method.isEmpty && y.method.isDefined)
            return -1;

        if (x.method.isEmpty && y.method.isEmpty) {
            // if we have no method information,
            // we can just compare the summaries
            return x.summary compare (y.summary);
        }

        // both methods are defined...
        val methodComparison = x.method.compare(y.method)
        if (methodComparison != 0)
            return methodComparison;

        (x.line, y.line) match {
            case (Some(xl), None) ⇒
                return -1
            case (None, Some(yl)) ⇒
                return 1
            case (Some(xl), Some(yl)) if xl != yl ⇒
                return xl - yl
            case _ ⇒ /*go on*/

        }

        (x.pc, y.pc) match {
            case (Some(_), None) ⇒
                return -1
            case (None, Some(_)) ⇒
                return 1
            case (Some(xpc), Some(ypc)) if xpc != ypc ⇒
                return xpc - ypc
            case _ ⇒ /*go on*/

        }

        return x.summary compare (y.summary);
    }

}
