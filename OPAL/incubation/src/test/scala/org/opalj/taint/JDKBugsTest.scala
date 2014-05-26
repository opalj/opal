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
package org.opalj.taint

import collection.mutable.Stack
import org.scalatest._
import org.opalj.br.TestSupport
import org.opalj.ai.taint.JDKTaintAnalysis
import org.opalj.ai.taint.TaintAnalysisDomain
import org.opalj.ai.taint.RootTaintAnalysisDomain
import java.net.URL

/**
 * Simple scala test to run all test present in src/test/java.
 * It checks if TaintAnalysisDomain creates the right amount of reports
 *
 * @author Lars Schulte
 */
class JDKBugsTest extends FlatSpec with Matchers {

  "JDKBugs" should "find all bugs presentet in the corresponding files in src/test/java" in {

    val args = new Array[String](2)
    args(0) = "-cp=" + TestSupport.locateTestResources("test.jar", "").getPath()
    args(1) = "-java.security=" + TestSupport.locateTestResources("java.security", "").getPath()

    JDKTaintAnalysis.main(args)
    TaintAnalysisDomain.numberOfReports should be(18)
  }

}