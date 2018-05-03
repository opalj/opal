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

import net.ceedubs.ficus.Ficus._
import org.opalj.br.analyses.{ProjectInformationKey, SomeProject}
import org.opalj.concurrent.NumberOfThreadsForCPUBoundTasks
import org.opalj.log.{LogContext, OPALLogger}

/**
 * The ''key'' object to get the project's [[org.opalj.fpcf.PropertyStore]].
 *
 * @note   It is possible to set the project's `debug` flag using the project's
 *         `org.opalj.br.analyses.PropertyStore.debug` config key.
 *
 * @author Michael Eichberg
 */
object PropertyStoreKey extends ProjectInformationKey[PropertyStore, Nothing] {

    final val ConfigKeyPrefix = "org.opalj.fpcf."
    final val DefaultPropertyStoreImplementation = "org.opalj.fpcf.ReactiveAsyncPropertyStore"

    /**
     * Used to specify the number of threads the property store should use. This
     * value is read only once when the property store is created.
     *
     * The value must be larger than 0 and should be smaller or equal to the number
     * of (hyperthreaded) cores.
     */
    @volatile var parallelismLevel: Int = Math.max(NumberOfThreadsForCPUBoundTasks, 2)

    /**
     * The [[PropertyStoreKey]] has no special prerequisites.
     *
     * @return `Nil`.
     */
    override protected def requirements: Seq[ProjectInformationKey[Nothing, Nothing]] = Nil

    /**
     * Creates a new empty property store using the current [[parallelismLevel]].
     */
    override protected def compute(project: SomeProject): PropertyStore = {
        val context: List[PropertyStoreContext[AnyRef]] = List(
            PropertyStoreContext[org.opalj.br.analyses.SomeProject](project)
        )

        val key = ConfigKeyPrefix+"PropertyStoreImplementation"
        val configuredPropertyStore = project.config.as[Option[String]](key)
        val propertyStoreCompanion = configuredPropertyStore.getOrElse(DefaultPropertyStoreImplementation)+"$"

        OPALLogger.info("PropertyStoreKey", s"Using PropertyStore $propertyStoreCompanion")(project.logContext)

        val propertyStoreCompanionClass = Class.forName(propertyStoreCompanion)
        val apply = propertyStoreCompanionClass.getMethod(
            "apply",
            classOf[Int],
            classOf[Seq[PropertyStoreContext[AnyRef]]],
            classOf[LogContext]
        )
        apply.invoke(
            propertyStoreCompanionClass.getField("MODULE$").get(null),
            new Integer(parallelismLevel),
            context,
            project.logContext
        ).asInstanceOf[PropertyStore]
    }
}
