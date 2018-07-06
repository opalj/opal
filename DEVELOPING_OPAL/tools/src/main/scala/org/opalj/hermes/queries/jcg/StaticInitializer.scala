/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2018
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
package hermes
package queries
package jcg

import org.opalj.br.analyses.Project
import org.opalj.da.ClassFile

/**
 * Groups test case features that test the correct recognition of static initializers.
 *
 * @note The features represent the __StaticInitializer__ test cases from the Call Graph Test Project (JCG).
 *
 * @author Michael Reif
 */
class StaticInitializer(implicit hermes: HermesConfig) extends DefaultFeatureQuery {

    override def featureIDs: Seq[String] = {
        Seq(
            "SI1", /* 0 --- calls are not allowed to leave the package */
            "SI2", /* 1  --- TODO */
            "SI3", /* 2 --- TODO */
            "SI4", /* 3 --- TODO */
            "SI5", /* 4 --- TODO */
            "SI6" /* 5 --- TODO */
        )
    }

    override def evaluate[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Traversable[(ClassFile, S)]
    ): IndexedSeq[LocationsContainer[S]] = {
        val instructionsLocations = Array.fill(6)(new LocationsContainer[S])

        instructionsLocations;
    }
}
