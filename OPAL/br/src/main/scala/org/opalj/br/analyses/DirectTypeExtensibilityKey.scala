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

import org.opalj.log.GlobalLogContext
import org.opalj.log.OPALLogger.error

/**
 * The ''key'' object to get a function that determines whether a type is directly extensible or not.
 * A type is directly extensible if a developer could have defined a direct - not transitive - subtype
 * that is not part of the given application/library.
 *
 * @author Michael Reif
 */
object DirectTypeExtensibilityKey extends ProjectInformationKey[ObjectType ⇒ Answer] {

    final val ConfigKeyPrefix = "org.opalj.br.analyses.DirectTypeExtensibilityKey."

    private[this] final val _defaultExtensibilityAnalysis = "org.opalj.br.analyses.DirectTypeExtensibilityInformation"

    /**
     * The [[DirectTypeExtensibilityKey]] has the [[ClosedPackagesKey]] as prerequisite.
     */
    override protected def requirements: ProjectInformationKeys = Seq(ClosedPackagesKey)

    /**
     * Computes the information for the given project.
     *
     * @note Classes that inherit from this trait are ''not'' expected to
     *       make this method public. This method is only expected to be called
     *       by an instance of a `Project`.
     */
    override protected def compute(project: SomeProject): ObjectType ⇒ Answer = {
        val className = project.config.as[Option[String]](
            ConfigKeyPrefix+"extensibilityAnalysis"
        ).getOrElse(_defaultExtensibilityAnalysis)
        reify(project, className).get
    }

    /*
     * Reflectively instantiates a ClosedPackagesContext. The instantiated class has to satisfy the
     * interface and needs to provide a single constructor parameterized over a Project.
     */
    private[this] def reify(project: SomeProject, packageContext: String): Option[DirectTypeExtensibilityInformation] = {
        try {
            val cls = Class.forName(packageContext)
            val cons = cls.getConstructors.head
            Some(cons.newInstance(project).asInstanceOf[DirectTypeExtensibilityInformation])
        } catch {
            case t: Throwable ⇒
                error("project configuration", s"failed to load: $packageContext", t)(GlobalLogContext)
                None
        }
    }
}