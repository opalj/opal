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
package dataflow
package solver

import org.opalj.br.Method
import org.opalj.br.analyses.Project
import org.opalj.ai.Domain
import org.opalj.ai.domain.ValuesCoordinatingDomain

/**
 * Implements the infrastructure for solving a data-flow problem.
 *
 * @author Michael Eichberg and Ben Hermann
 */
trait NaiveSolver[Source, Params] extends DataFlowProblemSolver[Source, Params] {

    lazy val theDomain: Domain = new BaseDomain[Source](project) with ValuesCoordinatingDomain

    def doSolve() : String = {
        "solved"
    }
}

abstract class BaseDomain[Source](val project: Project[Source])
        extends Domain
        with domain.DefaultDomainValueBinding
        with domain.ThrowAllPotentialExceptionsConfiguration
        with domain.ProjectBasedClassHierarchy
        with domain.TheProject[Source]
        with domain.l0.DefaultTypeLevelFloatValues
        with domain.l0.DefaultTypeLevelDoubleValues
        with domain.l0.DefaultTypeLevelLongValues
        with domain.l1.DefaultReferenceValuesBinding
        // [NOT YET SUFFICIENTLY TESTED:] with l1.DefaultStringValuesBinding
        // [NOT YET SUFFICIENTLY TESTED:] with l1.DefaultClassValuesBinding
        // [NOT YET SUFFICIENTLY TESTED:] with l1.DefaultArrayValuesBinding
        with domain.l1.DefaultIntegerRangeValues {

    override protected def maxSizeOfIntegerRanges: Long = 25l

}


//class BaseDomain[Source](
//    val project: Project[Source],
//    val method: Method)
//        extends Domain
//        with domain.DefaultDomainValueBinding
//        with domain.ThrowAllPotentialExceptionsConfiguration
//        with domain.ProjectBasedClassHierarchy
//        with domain.TheProject[Source]
//        with domain.TheMethod
//        with domain.DefaultHandlingOfMethodResults
//        with domain.IgnoreSynchronization
//        with domain.l0.DefaultTypeLevelFloatValues
//        with domain.l0.DefaultTypeLevelDoubleValues
//        with domain.l0.DefaultTypeLevelLongValues
//        with domain.l0.TypeLevelFieldAccessInstructions
//        with domain.l0.TypeLevelInvokeInstructions
//        with domain.l1.DefaultReferenceValuesBinding
//        // [NOT YET SUFFICIENTLY TESTED:] with l1.DefaultStringValuesBinding
//        // [NOT YET SUFFICIENTLY TESTED:] with l1.DefaultClassValuesBinding
//        // [NOT YET SUFFICIENTLY TESTED:] with l1.DefaultArrayValuesBinding
//        with domain.l1.DefaultIntegerRangeValues {
//
//    type Id = String
//
//    def id = "Domain of the Naive Solver"
//
//    override protected def maxSizeOfIntegerRanges: Long = 25l
//
//}

