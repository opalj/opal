/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package dataflow
package solver

import org.opalj.ai.domain.ValuesCoordinatingDomain

/**
 * Implements the infrastructure for solving a data-flow problem.
 *
 * @author Michael Eichberg and Ben Hermann
 */
trait NaiveSolver[Source, Params] extends DataFlowProblemSolver[Source, Params] { solver ⇒

    lazy val theDomain: Domain = new ValuesCoordinatingDomain with BaseDomain[Source] { val project = solver.project }

    def doSolve(): String = {
        "solved"
    }
}

trait BaseDomain[Source]
    extends CorrelationalDomain
    with domain.DefaultSpecialDomainValuesBinding
    with domain.TheProject
    with domain.l0.DefaultTypeLevelFloatValues
    with domain.l0.DefaultTypeLevelDoubleValues
    with domain.l0.DefaultTypeLevelLongValues
    //with domain.l1.DefaultReferenceValuesBinding
    // with domain.l1.DefaultStringValuesBinding
    with domain.l1.DefaultClassValuesBinding
    // [NOT YET SUFFICIENTLY TESTED:] with l1.DefaultArrayValuesBinding
    with domain.l1.DefaultIntegerRangeValues {
    domain: Configuration ⇒

    override def maxCardinalityOfIntegerRanges: Long = 20L

}

//class BaseDomain[Source](
//    val project: Project[Source],
//    val method: Method)
//        extends Domain
//        with domain.DefaultSpecialDomainValuesBinding
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

