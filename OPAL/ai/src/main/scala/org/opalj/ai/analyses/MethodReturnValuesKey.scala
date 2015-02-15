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
package analyses

import org.opalj.ai.Domain
import org.opalj.ai.InterruptableAI
import org.opalj.br.Method
import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.SomeProject
import org.opalj.log.OPALLogger

/**
 * The ''key'' object to get information about the "more precise" return values.
 *
 * @author Michael Eichberg
 */
object MethodReturnValuesKey extends ProjectInformationKey[MethodReturnValueInformation] {

    override protected def requirements = Seq(FieldValuesKey)

    /**
     * Computes the return value information.
     */
    override protected def compute(project: SomeProject): MethodReturnValueInformation = {
        // TODO Introduce the concept of a "configuration to a project"
        implicit val logContext = project.logContext

        val fieldValueInformation = project.get(FieldValuesKey)

        OPALLogger.info("progress", "computing method return value information")
        val result = MethodReturnValuesAnalysis.doAnalyze(
            project,
            () ⇒ false, // make it configurable
            (ai: InterruptableAI[Domain], method: Method) ⇒
                new BaseMethodReturnValuesAnalysisDomain(
                    project, fieldValueInformation, ai, method)
        )
        OPALLogger.info("progress", "successfully computed the method return value information")
        result
    }
}

