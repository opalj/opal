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
package de.tud.cs.st
package bat
package resolved
package dependency

import instructions._
import ai.invokedynamic._
import DependencyType._
import analyses.SomeProject
import scala.util.Try
import scala.util.Success
import scala.util.Failure

/**
 * A [[DependencyExtractor]] that delegates the extraction of [[Invokedynamic]] related
 * dependencies to some `InvokedynamicResolver`. The latter is responsible for
 * extracting the dependency to the source element that will called/accessed at
 * runtime.
 *
 * ==Thread Safety==
 * This trait is thread-safe as long as the configured `resolver` is also thread-safe.
 *
 * @author Arne Lottmann
 * @author Michael Eichberg
 */
class DependencyExtractorWithInvokedynamicResolution(
        dependencyProcessor: DependencyProcessor,
        val resolver: InvokedynamicResolver) extends DependencyExtractor(dependencyProcessor) {

    import DependencyType._

    /**
     * This implementation uses an InvokedynamicResolver to get the dependencies to
     * the target source element.
     *
     * @note What kind of method calls / field accesses can be resolved depends on the
     *      `InvokedynamicResolver`'s capabilities.
     */
    override protected def processInvokedynamic(
        declaringMethod: VirtualMethod,
        instruction: INVOKEDYNAMIC): Unit = {

        processInvokedynamicRuntimeDependencies(declaringMethod, instruction)

        resolver.resolveInvokedynamic(instruction).allTargets foreach { target ⇒
            target match {

                case m: VirtualMethod ⇒
                    processDependency(
                        declaringMethod, m.declaringClassType, USES_METHOD_DECLARING_TYPE
                    )
                    processDependency(
                        declaringMethod, m.descriptor.returnType, USES_RETURN_TYPE
                    )
                    m.descriptor.parameterTypes foreach { t ⇒
                        processDependency(declaringMethod, t, USES_PARAMETER_TYPE)
                    }
                    dependencyProcessor.processDependency(
                        declaringMethod,
                        dependencyProcessor.asVirtualMethod(
                            m.declaringClassType,
                            m.name,
                            m.descriptor),
                        CALLS_METHOD)

                case f: VirtualField ⇒
                    processDependency(
                        declaringMethod, f.declaringClassType, USES_FIELD_DECLARING_TYPE
                    )
                    processDependency(
                        declaringMethod, f.fieldType, USES_FIELD_READ_TYPE
                    )
                    dependencyProcessor.processDependency(
                        declaringMethod,
                        dependencyProcessor.asVirtualField(
                            f.declaringClassType,
                            f.name,
                            f.fieldType),
                        READS_FIELD)
            }
        }
    }
}
