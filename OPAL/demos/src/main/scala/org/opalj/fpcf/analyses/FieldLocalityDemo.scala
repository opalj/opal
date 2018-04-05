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
package org.opalj
package fpcf
package analyses

import java.net.URL

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.fpcf.analyses.escape.InterProceduralEscapeAnalysis
import org.opalj.fpcf.properties.ExtensibleLocalField
import org.opalj.fpcf.properties.ExtensibleLocalFieldWithGetter
import org.opalj.fpcf.properties.FieldLocality
import org.opalj.fpcf.properties.LocalField
import org.opalj.fpcf.properties.LocalFieldWithGetter
import org.opalj.fpcf.properties.NoLocalField

/**
 * A small demo determining the return value freshness for all methods in the current project.
 *
 * @author Florian Kuebler
 */
object FieldLocalityDemo extends DefaultOneStepAnalysis {
    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {
        PropertyStoreKey.makeVirtualFormalParametersAvailable(project)
        PropertyStoreKey.makeAllocationSitesAvailable(project)
        PropertyStoreKey.makeDeclaredMethodsAvailable(project)
        val ps = project.get(PropertyStoreKey)

        InterProceduralEscapeAnalysis.startLazily(project, ps)
        VirtualCallAggregatingEscapeAnalysis.startLazily(project, ps)
        VirtualReturnValueFreshnessAnalysis.startLazily(project, ps)
        ReturnValueFreshnessAnalysis.startLazily(project, ps)

        FieldLocalityAnalysis.start(project, ps)
        ps.waitOnPropertyComputationCompletion()

        val local = ps.entities(LocalField)
        val nolocal = ps.entities(NoLocalField)
        val extLocal = ps.entities(ExtensibleLocalField)
        val getter = ps.entities(LocalFieldWithGetter)
        val extGetter = ps.entities(ExtensibleLocalFieldWithGetter)

        for (field <- project.allFields) {
            println(field.toJava + ps(field,FieldLocality.key).p)
        }

        val message =
            s"""|# of local fields: ${local.size}
                |# of not local fields: ${nolocal.size}
                |# of extensible local fields: ${extLocal.size}
                |# of local fields with getter: ${getter.size}
                |# of extensible local fields with getter: ${extGetter.size}
                |"""

        BasicReport(message.stripMargin('|'))
    }
}
