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
package la

import scala.collection.Set

import org.opalj.br.analyses.SomeProject
import org.opalj.br.ClassFile
import org.opalj.br.Method
import org.opalj.ai.analyses.FieldValuesKey
import org.opalj.ai.analyses.FieldValueInformation
import org.opalj.ai.analyses.MethodReturnValuesKey
import org.opalj.ai.analyses.MethodReturnValueInformation
import org.opalj.ai.analyses.cg.CallGraphCache
import org.opalj.br.MethodSignature

/**
 * A default configuration of a Domain that uses - in particular - the domains implemented
 * in package `la`.
 *
 * @note This domain is intended to be used for '''demo purposes only'''.
 *      '''Tests should create their own domains to make sure that
 *      the test results remain stable. The configuration of this
 *      domain just reflects a reasonable configuration that may
 *      change without further notice.'''
 *
 * @author Michael Eichberg
 */
class DefaultDomain(
    val project:         SomeProject,
    val classFile:       ClassFile,
    override val method: Method
)
        extends CorrelationalDomain
        with domain.DefaultDomainValueBinding
        with domain.ThrowAllPotentialExceptionsConfiguration
        with domain.l0.DefaultTypeLevelFloatValues
        with domain.l0.DefaultTypeLevelDoubleValues
        //with domain.l0.TypeLevelFieldAccessInstructions
        with RefinedTypeLevelFieldAccessInstructions
        with domain.l0.TypeLevelInvokeInstructions
        with RefinedTypeLevelInvokeInstructions
        //with domain.l1.DefaultReferenceValuesBinding
        with domain.l1.DefaultClassValuesBinding
        //with domain.l1.DefaultStringValuesBinding
        with domain.l1.NullPropertyRefinement
        with domain.l1.DefaultIntegerRangeValues
        with domain.l1.MaxArrayLengthRefinement
        //[CURRENTLY ONLY A WASTE OF RESOURCES] with domain.l1.ConstraintsBetweenIntegerValues
        // with domain.l1.DefaultIntegerSetValues
        with domain.l1.DefaultLongSetValues
        with domain.l1.LongSetValuesShiftOperators
        with domain.l1.ConcretePrimitiveValuesConversions
        with domain.DefaultHandlingOfMethodResults
        with domain.IgnoreSynchronization
        with domain.TheProject
        with domain.TheMethod
        with domain.ProjectBasedClassHierarchy
        // the following two are required to detect instructions that always throw
        // an exception (such as div by zero, a failing checkcast, a method call that
        // always fails etc.)
        with domain.RecordCFG
        with domain.l1.RecordAllThrownExceptions {

    override val maxCardinalityOfIntegerRanges: Long = 16l

    val fieldValueInformation: FieldValueInformation = project.get(FieldValuesKey)

    val methodReturnValueInformation: MethodReturnValueInformation = project.get(MethodReturnValuesKey)

    val cache: CallGraphCache[MethodSignature, Set[Method]] =
        new CallGraphCache[MethodSignature, Set[Method]](project)

}
