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
package ai
package domain
package l2

import org.opalj.br.Method
import org.opalj.br.analyses.Project

/**
 * A domain that defines a common reference frame for all subsequent domains.
 */
trait SharedValuesDomain[Source]
    extends CorrelationalDomain
    with DefaultDomainValueBinding
    with TheProject
    with l0.DefaultTypeLevelFloatValues
    with l0.DefaultTypeLevelDoubleValues
    with l1.DefaultClassValuesBinding
    with l1.DefaultArrayValuesBinding
    with l1.DefaultIntegerRangeValues
    // [CURRENTLY ONLY A WASTE OF RESOURCES] with l1.ConstraintsBetweenIntegerValues
    with l1.DefaultLongValues

/**
 * Domain that uses the l1 and l2 level ''stable'' domains.
 *
 * @note This domain is intended to be used for '''demo purposes only'''.
 *      '''Tests should create their own domains to make sure that
 *      the test results remain stable. The configuration of this
 *      domain just reflects a reasonable configuration that may
 *      change without further notice.'''
 *
 * @author Michael Eichberg
 */
class SharedDefaultDomain[Source](
        val project: Project[Source],
        val method:  Method
) extends TheMethod
    with ThrowAllPotentialExceptionsConfiguration
    with DefaultHandlingOfMethodResults
    with IgnoreSynchronization
    with l0.TypeLevelFieldAccessInstructions
    with l0.TypeLevelInvokeInstructions
    with SpecialMethodsHandling
    with SharedValuesDomain[Source]
    with l1.MaxArrayLengthRefinement // OPTIONAL
    with l1.NullPropertyRefinement // OPTIONAL
    // [CURRENTLY ONLY A WASTE OF RESOURCES] with l1.ConstraintsBetweenIntegerValues
    with l1.LongValuesShiftOperators
    with l1.ConcretePrimitiveValuesConversions {

    override def toString: String = super.toString()+"("+method.toJava+")"

}
