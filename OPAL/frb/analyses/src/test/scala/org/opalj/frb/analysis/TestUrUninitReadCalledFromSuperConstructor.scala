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
package frb
package test
package analysis

import AnalysisTest._
import analyses._
import br._
import br.analyses._
import java.net.URL

/**
 * Test for UrUninitReadCalledFromSuperConstructor
 *
 * @author Roberts Kolosovs
 * @author Daniel Klauer
 * @author Peter Spieler
 * @author Florian Brandherm
 */
@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class TestUrUninitReadCalledFromSuperConstructor extends AnalysisTest {
    import TestUrUninitReadCalledFromSuperConstructor._

    behavior of "UrUninitReadCalledFromSuperConstructor"

    val superclass =
        "UrUninitReadCalledFromSuperConstructor.SuperclassWithAccessToSubclassDuringInit"

    val subclass =
        "UrUninitReadCalledFromSuperConstructor/SubclassAccessedBySuperclassBeforeInit"

    it should "find that the constructor of "+superclass+" performs a read call on "+
        "an uninitialized subclass" in {
            results should contain(
                MethodBasedReport(
                    project.source(ObjectType(subclass)),
                    Severity.Error,
                    ObjectType(subclass),
                    MethodDescriptor.NoArgsAndReturnVoid,
                    "f",
                    "Called by super constructor ("+superclass+"), while the class' "+
                        "fields are still uninitialized"))
        }

    it should "find exactly 1 issue in UrUninitReadCalledFromSuperConstructor.jar" in {
        results.size should be(1)
    }
}

object TestUrUninitReadCalledFromSuperConstructor {
    val project = makeProjectFromJar("UrUninitReadCalledFromSuperConstructor.jar")
    val results = new UrUninitReadCalledFromSuperConstructor[URL].analyze(project).toSet
}
