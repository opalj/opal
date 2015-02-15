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

import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.JavaConverters._
import scala.collection.mutable.AnyRefMap
import org.opalj.collection.immutable.UIDSet
import org.opalj.concurrent.OPALExecutionContextTaskSupport
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.br.analyses.{ Analysis, OneStepAnalysis, AnalysisExecutor, BasicReport, SomeProject }
import org.opalj.br.{ ClassFile, Method }
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.Project
import org.opalj.ai.Domain
import org.opalj.ai.domain._
import org.opalj.ai.InterruptableAI
import org.opalj.ai.IsAReferenceValue
import org.opalj.ai.NoUpdate
import org.opalj.ai.SomeUpdate
import org.opalj.log.OPALLogger

/**
 * A shallow analysis that tries to refine the return types of methods.
 *
 * @author Michael Eichberg
 */
object MethodReturnValuesAnalysis {

    def title: String =
        "tries to derive more precise information about the values returned by methods"

    def description: String =
        "Identifies methods where we can – statically – derive more precise return type/value information."

    def doAnalyze(
        theProject: SomeProject,
        isInterrupted: () ⇒ Boolean,
        createDomain: (InterruptableAI[Domain], Method) ⇒ Domain with RecordReturnedValueInfrastructure): MethodReturnValueInformation = {
        implicit val logContext = theProject.logContext

        val results = new ConcurrentHashMap[Method, Option[Domain#DomainValue]]
        val candidates = new AtomicInteger(0)

        theProject.parForeachMethodWithBody(isInterrupted) { m ⇒
            val (_ /*Source*/ , classFile, method) = m
            val originalReturnType = method.returnType

            if (originalReturnType.isObjectType &&
                // ALTERNATIVE TEST: if we are only interested in type refinements but not
                // in refinements to "null" values then we can also use the following
                // check:
                // if theProject.classHierarchy.hasSubtypes(originalReturnType.asObjectType).isYesOrUnknown

                // We may still miss some refinements to "Null" but we don't care...
                theProject.classFile(originalReturnType.asObjectType).map(!_.isFinal).getOrElse(true)) {

                candidates.incrementAndGet()
                val ai = new InterruptableAI[Domain]
                val domain = createDomain(ai, method)
                ai(classFile, method, domain)
                if (!ai.isInterrupted) {
                    results.put(method, domain.returnedValue)
                }
            }
        }

        OPALLogger.info(
            "analysis result",
            s"refined the return type of ${results.size} methods out of ${candidates.get} methods")
        AnyRefMap.empty[Method, Option[Domain#DomainValue]] ++ results.asScala
    }

}

