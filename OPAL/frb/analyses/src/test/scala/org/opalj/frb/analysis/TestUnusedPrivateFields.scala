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
 * Test for UnusedPrivateFields
 *
 * @author Roberts Kolosovs
 * @author Daniel Klauer
 */
@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class TestUnusedPrivateFields extends AnalysisTest {
    import TestUnusedPrivateFields._

    behavior of "UnusedPrivateFields"

    it should "detect an unused private field" in {
        val declaringClass = ObjectType("UnusedPrivateFields/UnusedPrivateField")
        results should contain(FieldBasedReport(
            project.source(declaringClass),
            Severity.Info,
            declaringClass,
            Some(IntegerType),
            "field",
            "Is private and unused"))
    }

    it should "detect an unused private field called 'serialVersionUID'" in {
        val declaringClass =
            ObjectType("UnusedPrivateFields/UnusedPrivateSerialVersionUID")
        results should contain(FieldBasedReport(
            project.source(declaringClass),
            Severity.Info,
            declaringClass,
            Some(LongType),
            "serialVersionUID",
            "Is private and unused"))
    }

    it should "find only 2 issues in UnusedPrivateFields.jar" in {
        results.size should be(2)
    }
}

object TestUnusedPrivateFields {
    val project = makeProjectFromJar("UnusedPrivateFields.jar")
    val results = new UnusedPrivateFields[URL].analyze(project)
}
