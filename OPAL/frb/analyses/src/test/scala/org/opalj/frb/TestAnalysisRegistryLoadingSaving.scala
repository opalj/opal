/* License (BSD Style License):
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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

import br._
import java.io.IOException
import java.io.File

/**
 * Tests for loading/saving of `AnalysisRegistry`s.
 *
 * @author Florian Brandherm
 */
@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class TestAnalysisRegistryLoadingSaving extends AnalysisTest {

    def locateResource(filename: String): File =
        TestSupport.locateTestResources(filename, "frb/analyses")

    behavior of "FindRealBugs.loadRegistry()"

    it should "throw an IOException when trying to read a non-existent file." in {
        an[IOException] should be thrownBy {
            FindRealBugs.loadRegistry(new File("nonexistent/foo.properties"))
        }
    }

    it should "throw an exception when trying to read a non-properties file." in {
        an[Exception] should be thrownBy {
            FindRealBugs.loadRegistry(locateResource("not-a-property-file.gif"))
        }
    }

    it should "be able to load an empty file." in {
        FindRealBugs.loadRegistry(locateResource("empty.properties")) should be('empty)
    }

    it should "be able to load analysis class names and states from a valid file." in {
        val registry = FindRealBugs.loadRegistry(locateResource("valid.properties"))
        registry should be(Map(
            "Analysis1" -> true,
            "Analysis2" -> true,
            "Analysis3" -> false,
            "Analysis4" -> true
        ))
    }

    it should "throw an exception when the file contains an invalid property value." in {
        (the[IllegalArgumentException] thrownBy {
            FindRealBugs.loadRegistry(locateResource("invalid-value-1.properties"))
        }).getMessage() should be(
            "invalid-value-1.properties: Analysis1: invalid value ''"+
                ", expected 'yes' or 'no'")

        (the[IllegalArgumentException] thrownBy {
            FindRealBugs.loadRegistry(locateResource("invalid-value-2.properties"))
        }).getMessage() should be(
            "invalid-value-2.properties: Analysis1: invalid value '1'"+
                ", expected 'yes' or 'no'")
    }

    behavior of "FindRealBugs.saveRegistry()"

    it should "be able to save a file such that it can be read by loadRegistry()" in {
        val file = new File(locateResource("empty.properties").getParent(), "test.temp")

        println(file.getPath())
        val registry = Map("a.Test1" -> false, "b.Test2" -> true)

        FindRealBugs.saveRegistry(file, registry)
        file should exist

        FindRealBugs.loadRegistry(file) should be(registry)

        // Clean up
        file.delete()
    }
}
