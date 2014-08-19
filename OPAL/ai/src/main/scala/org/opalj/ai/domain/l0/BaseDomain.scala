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
package domain
package l0

import org.opalj.br.analyses.Project
import org.opalj.br.{ Method, ClassFile }

/**
 * A complete domain that performs all computations at the type level and which has
 * a configurable identifier.
 *
 * @note This domain is intended to be used for '''demo purposes only'''.
 *      '''Tests should create their own domains to make sure that
 *      the test results remain stable. The configuration of this
 *      domain just reflects a reasonable configuration that may
 *      change without further notice.'''
 *
 * @author Michael Eichberg
 */
class BaseConfigurableDomain[I, Source](
    val id: I,
    val project: Project[Source],
    val classFile: ClassFile,
    val method: Method)
        extends TypeLevelDomain
        with ThrowAllPotentialExceptionsConfiguration
        with DefaultHandlingOfMethodResults
        with DefaultPrimitiveValuesConversions
        with IgnoreSynchronization
        with ProjectBasedClassHierarchy
        with TheProject[Source]
        with TheMethod
        with DomainId {

    type Id = I
}

/**
 * This is a ready to use domain which sets the domain identifier
 * to a string that identifies the method that is analyzed.
 *
 * This domain is primarily useful for demonstration purposes.
 *
 * @author Michael Eichberg
 */
class BaseDomain[Source](
    project: Project[Source],
    classFile: ClassFile,
    method: Method)
        extends BaseConfigurableDomain[String, Source](
            classFile.thisType.toJava+"{ "+method.toJava+"}",
            project,
            classFile,
            method)

