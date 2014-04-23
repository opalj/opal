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
 * A specialized DependencyExtractor with added capabilities for extracting dependencies introduced
 * by invokedynamic instructions.
 *
 * @author Arne Lottmann
 */
abstract class DependencyExtractorForInvokedynamic[I](
        sourceElementIDs: SourceElementIDs,
        private val project: SomeProject
        )
        extends DependencyExtractor(sourceElementIDs) {

    def resolver: InvokedynamicResolver

    /**
     * This implementation uses an InvokedynamicResolver to try and find more information about
     * invokedynamic instructions.
     * 
     * What kind of method calls / field accesses can be resolved depends on the InvokedynamicResolver's
     * support for the specific language under analysis. Take a look at the InvokedynamicResolver
     * and the concrete ResolutionStrategy implementations for more information.
     */
    override protected def processInvokedynamic(
            methodId: Int, instruction: INVOKEDYNAMIC): Unit = {
        
        processInvokedynamicRuntimeDependencies(methodId, instruction)
        
        resolver.resolveInvokedynamic(instruction) match {
            case SingleResult(method: Method, _) ⇒ {
                val matchingClass = project.classFile(method)
                processDependency(methodId, matchingClass.thisType, method.name, method.descriptor, CALLS_METHOD)
                processDependency(methodId, method.descriptor.returnType, USES_RETURN_TYPE)
                method.descriptor.parameterTypes.foreach(t ⇒ processDependency(methodId, t, USES_PARAMETER_TYPE))
            }
            case InheritanceResult(method: Method, _, _) ⇒ {
                val matchingClass = project.classFile(method)
                processDependency(methodId, matchingClass.thisType, method.name, method.descriptor, CALLS_METHOD)
                processDependency(methodId, method.descriptor.returnType, USES_RETURN_TYPE)
                method.descriptor.parameterTypes.foreach(t ⇒ processDependency(methodId, t, USES_PARAMETER_TYPE))
            }
            case SingleResult(field: Field, _) ⇒ {
                val matchingClass = project.classFile(field)
                processDependency(methodId, matchingClass.thisType, field.name, READS_FIELD)
                processDependency(methodId, matchingClass.thisType, USES_FIELD_DECLARING_TYPE)
            }
            case InheritanceResult(field: Field, _, _) ⇒ {
                val matchingClass = project.classFile(field)
                processDependency(methodId, matchingClass.thisType, field.name, READS_FIELD)
                processDependency(methodId, matchingClass.thisType, USES_FIELD_DECLARING_TYPE)
            }
            case _ ⇒
        }
    }
}
