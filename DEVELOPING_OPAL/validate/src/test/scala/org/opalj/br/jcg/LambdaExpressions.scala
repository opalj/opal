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
package org.opalj.br.jcg

import java.io.{ByteArrayOutputStream, File, PrintStream}

import org.opalj.br.FixturesTest

/**
 * Tests for resolving various lambda expressions using the Java Call Graph library.
 *
 * See: https://bitbucket.org/delors/jcg
 *
 * @author Andreas Muttscheller
 */
class LambdaExpressions extends FixturesTest {
    val fixtureFiles: File = new File(s"OPAL/bi/src/test/resources/classfiles/lambda_expressions.jar")

    describe("JCG lambda_expressions test") {
        it("should execute main successfully") {
            val c = byteArrayClassLoader.loadClass("app.ExpressionPrinter")
            val m = c.getMethod("main", classOf[Array[String]])

            // Intercept output
            val baos = new ByteArrayOutputStream()
            val defaultOut = System.out
            System.setOut(new PrintStream(baos))

            m.invoke(null, Array("lambda_expressions.jar"))
            assert(baos.toString == "Id(((1)++)²)\n")

            // Reset System.out
            System.setOut(defaultOut)
        }
    }
}
