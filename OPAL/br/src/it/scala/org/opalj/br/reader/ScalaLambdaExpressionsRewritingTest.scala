/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
package org.opalj.br.reader

import org.opalj.bi.TestResources.locateTestResources
import org.opalj.br.instructions.INVOKEDYNAMIC

/**
 * This test loads all classes found in the Scala 2.12.4 libraries and verifies that all
 * suported [[INVOKEDYNAMIC]] instructions can be resolved.
 *
 * @author Arne Lottmann
 * @author Andreas Amuttsch
 * @author Michael Eichberg
 */
class ScalaLambdaExpressionsRewritingTest extends LambdaExpressionsRewritingTest {

    test("rewriting of invokedynamic instructions in Scala 2.12.4 library") {
        val project = load(locateTestResources("classfiles/scala-2.12.4", "bi"))

        val invokedynamics = project.allMethodsWithBody.par.flatMap { method ⇒
            method.body.get.collect {
                case i: INVOKEDYNAMIC if (
                    LambdaExpressionsRewriting.isJava8LikeLambdaExpression(i) ||
                    LambdaExpressionsRewriting.isScalaLambdaDeserializeExpression(i) ||
                    LambdaExpressionsRewriting.isScalaSymbolExpression(i)
                ) ⇒ i
            }
        }

        if (invokedynamics.nonEmpty) {
            fail(invokedynamics.mkString("Could not resolve:", "\n", "\n"))
        }
    }
}
