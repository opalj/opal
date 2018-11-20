/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package dataflow

import org.opalj.value.ValueInformation

/**
 * Implements the infrastructure for solving a data-flow problem.
 *
 * @author Michael Eichberg and Ben Hermann
 */
trait DataFlowProblemSolver[Source, Params] extends DataFlowProblem[Source, Params] { solver ⇒

    /* ABSTRACT */ val theDomain: Domain

    type DomainValue = theDomain.DomainValue

    protected[this] class TaintedValue(
            override val domainValue: DomainValue
    ) extends super.TaintedValue with TaintInformation {

        def valueInformation: ValueInformation = domainValue

    }

    def ValueIsTainted: (DomainValue) ⇒ TaintInformation =
        (value: DomainValue) ⇒ new TaintedValue(value)

}
