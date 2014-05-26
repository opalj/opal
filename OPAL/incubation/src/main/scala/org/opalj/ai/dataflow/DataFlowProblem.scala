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

import scala.collection.{ Map, Set }

import bi.AccessFlagsMatcher

import br._
import br.instructions._
import br.analyses._

import domain._
import domain.l0._

/**
 * Characterizes a data-flow problem. The characterization consists of the specification
 * of the problem as well as the selection of the solver.
 *
 * I.e., tries to find paths from the identified sources to the identified sinks.
 *
 * ==Initialization==
 * Initialization is done in multiple steps.
 *
 *  1. The parameters are checked.
 *  1. The parameters of the analysis are set.
 *  1. The project is initialized by the framework and also set.
 *  1. Initialize [[sourceValues]] and [[sinkInstructions]] (These methods needs to be
 *     overridden by your subclass.)
 *  1. Call [[solve]]. After you have called [[solve]] you are no longer allowed
 *      to change the project or the sources and sinks.
 *
 * @author Michael Eichberg and Ben Hermann
 */
trait DataFlowProblem {

    def description: String =
        "Finds instances of the specified dataflow problem (see documentation for details)."

    // __________________________________________________________________________________
    //
    // Specify and access the analysis' configuration 
    //
    //

    /**
     * Describes the analysis specific parameters. An analysis specific parameter
     * has to start with a dash ("-") and has to contain an equals sign ("=").
     *
     * @note The parameter `-cp=` is already predefined (see general documentation).
     * @note The parameter `-library=` is already predefined (see general documentation).
     */
    def analysisParametersDescription: String = ""

    /**
     * Checks if the (additional) parameters are understood by
     * the analysis.
     *
     * This method **must be** overridden if the analysis defines additional
     * parameters. A method that overrides this method should return false if it can't
     * validate all arguments.
     * The default behavior is to check that there are no additional parameters.
     */
    def checkAnalysisParameters(parameters: Seq[String]): Boolean =
        parameters.isEmpty

    def processAnalysisParameters(parameters: Seq[String]): Unit

    // __________________________________________________________________________________
    //
    // Functionality required to specify the taint-information flow 
    //
    //

    type DomainValue <: AnyRef

    /**
     * Encapsultates taint information about a(n implicit) value.
     */
    protected[this] trait TaintInformation {
        def isTainted(): Boolean
    }

    /**
     * The (implicitly referred to) value is not tainted.
     */
    case object NotTainted extends TaintInformation {
        final override def isTainted(): Boolean = false
    }

    /**
     * Factory method that – given a `DomainValue` – creates a [[TaintInformation]]
     * object that encapsulates the information that the value is not tainted.
     */
    val ValueIsNotTainted: (DomainValue) ⇒ TaintInformation = (DomainValue) ⇒ NotTainted

    /**
     * Representation of a tainted value.
     */
    protected[this] trait TaintedValue extends TaintInformation {
        final override def isTainted(): Boolean = true

        def typeInformation: TypesAnswer
        def domainValue: DomainValue
    }

    /**
     * Returns a factory method that – given a `DomainValue` – creates a [[TaintedValue]]
     * object that encapsulates the information that the value is tainted.
     */
    def ValueIsTainted: (DomainValue) ⇒ TaintInformation

    /**
     * Extractor to match tainted values.
     */
    object Tainted {
        def unapply(value: TaintedValue): Some[TypesAnswer] = Some(value.typeInformation)
    }

    case class Invoke(
        declaringClassType: ReferenceType,
        name: String,
        descriptor: MethodDescriptor,
        context: Method,
        caller: TaintInformation,
        receiver: TaintInformation,
        parameters: IndexedSeq[TaintInformation])

    case class CallResult(
        receiver: TaintInformation,
        parameters: IndexedSeq[TaintInformation],
        result: (DomainValue) ⇒ TaintInformation)

    type OnCallTaintProcessor = PartialFunction[Invoke, CallResult]

    protected[this] var onCallTaintProcessors: List[OnCallTaintProcessor] = List.empty

    def call(f: OnCallTaintProcessor): Unit = {
        onCallTaintProcessors = f :: onCallTaintProcessors
    }

    case class FieldWrite(
        declaringClassType: ReferenceType,
        name: String,
        fieldType: Type,
        context: Method,
        caller: TaintInformation,
        value: TaintInformation,
        receiver: TaintInformation)

    type OnWriteTaintProcessor = PartialFunction[FieldWrite, (DomainValue) ⇒ TaintInformation /*about the receiver*/ ]

    protected[this] var onWriteTaintProcessors: List[OnWriteTaintProcessor] = List.empty

    def write(f: OnWriteTaintProcessor): Unit = {
        onWriteTaintProcessors = f :: onWriteTaintProcessors
    }

    // __________________________________________________________________________________
    //
    // Identifies the analysis' context
    //
    //

    private[this] var theProject: SomeProject = null
    def project = theProject
    def project_=(project: SomeProject): Unit = {

        if (this.project != null)
            throw new IllegalStateException("the project is already set")
        this.theProject = project

        initializeSourcesAndSinks()
    }

    protected[this] def initializeSourcesAndSinks(): Unit

    /**
     * Identifies the values that we want to track (by means of the origin of the
     * respective value) per relevant method.
     *
     * ''The returned map must not change, after solve was called!''
     *
     * @note The methods have to belong to the [[project]].
     *
     * @see [[org.opalj.ai.dataflow.spec.DataFlowProblemSpecification]] for the easy creation
     *      of the `sourcesValues` map.
     */
    def sourceValues(): Map[Method, Set[ValueOrigin]]

    /**
     * Identifies the program counters (PCs) of those instructions
     * that are sinks.
     *
     * ''The returned map must not change, after solve was called!''
     *
     * @note The methods have to belong to the [[project]].
     *
     * @see [[org.opalj.ai.dataflow.spec.DataFlowProblemSpecification]] for the easy creation
     *      of the `sinkInstructions` map.
     */
    def sinkInstructions(): Map[Method, Set[PC]]

    protected[this] def analyzeFeasability() {
        val sourceValuesCount = sourceValues.values.view.map(pcs ⇒ pcs.size).sum
        if (project.methodsCount / 10 < sourceValuesCount) {
            Console.out.println(
                "[info] The analysis will take long; the number of source values to analyze is: "+
                    sourceValuesCount+
                    ".")
        }
    }

    /**
     * Tries to find paths from the sources to the sinks.
     */
    def solve(): String = {
        if (project == null)
            throw new IllegalStateException("the project needs to be initialized first")

        analyzeFeasability()

        "Solved :-)"
    }
}


