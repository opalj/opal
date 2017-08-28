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
package br
package analyses

import net.ceedubs.ficus.Ficus._

import org.opalj.log.OPALLogger.error

/**
 * The ''key'' object to get a function that determines whether a type is directly
 * extensible or not.
 *
 * @see [[DirectTypeExtensibilityInformation]] for further information.
 *
 * @author Michael Reif
 */
object DirectTypeExtensibilityKey extends ProjectInformationKey[ObjectType ⇒ Answer, Nothing] {

    final val ConfigKeyPrefix = "org.opalj.br.analyses.DirectTypeExtensibilityKey."

    final val DefaultExtensibilityAnalysis = {
        "org.opalj.br.analyses.DirectTypeExtensibilityInformation"
    }

    /**
     * The [[DirectTypeExtensibilityKey]] has the [[ClosedPackagesKey]] as prerequisite.
     */
    override protected def requirements: ProjectInformationKeys = Seq(ClosedPackagesKey)

    /**
     * Computes the direct type extensibility information for the given project.
     */
    override protected def compute(project: SomeProject): ObjectType ⇒ Answer = {
        implicit val logContext = project.logContext

        val configKey = ConfigKeyPrefix+"analysis"
        try {
            reify(
                project,
                project.config.as[Option[String]](configKey).getOrElse(DefaultExtensibilityAnalysis)
            )
        } catch {
            case t: Throwable ⇒
                error(
                    "project configuration",
                    "failed to compute \"direct extensibility information\""+
                        "; \"Unknown extensibility\" will now be used as the fallback",
                    t
                )
                (o: ObjectType) ⇒ Unknown
        }
    }

    /**
     * Reflectively instantiates the configured analysis. The instantiated class has to satisfy the
     * interface and has to provide a single constructor parameterized over a [[Project]].
     */
    private[this] def reify(project: SomeProject, analysisName: String): ObjectType ⇒ Answer = {
        val constructor = Class.forName(analysisName).getConstructors.head
        constructor.newInstance(project).asInstanceOf[ObjectType ⇒ Answer]
    }
}
