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
package ai

/**
 * Factory to create `AIResult` objects. Primarily used by BATAI to return the
 * result of an abstract interpretation of a method.
 *
 * @author Michael Eichberg
 */
/* Design - We need to use a kind of builder to construct a Result object in two steps. 
 * This is necessary to correctly type the data structures that store the memory 
 * layout and which depend on the given domain. */
object AIResultBuilder {

    /**
     * Creates a domain dependent `AIAborted` object which stores the results of the
     * computation.
     */
    def aborted(
        theCode: Code,
        theDomain: Domain)(
            theWorklist: List[Int],
            theEvaluated: List[Int],
            theOperandsArray: Array[List[theDomain.DomainValue]],
            theLocalsArray: Array[Array[theDomain.DomainValue]]): AIAborted { val domain: theDomain.type } = {

        new AIAborted {
            val code: Code = theCode
            val domain: theDomain.type = theDomain
            val worklist: List[Int] = theWorklist
            val evaluated: List[Int] = theEvaluated
            val operandsArray: Array[List[theDomain.DomainValue]] = theOperandsArray
            val localsArray: Array[Array[theDomain.DomainValue]] = theLocalsArray

            def continueInterpretation(
                ai: AI[_ >: domain.type]): AIResult =
                ai.continueInterpretation(
                    code, domain)(
                        worklist, evaluated, operandsArray, localsArray)

        }
    }

    /**
     * Creates a domain dependent `AICompleted` object which stores the results of the
     * computation.
     */
    def completed(
        theCode: Code,
        theDomain: Domain)(
            theEvaluated: List[Int],
            theOperandsArray: Array[List[theDomain.DomainValue]],
            theLocalsArray: Array[Array[theDomain.DomainValue]]): AICompleted { val domain: theDomain.type } = {

        new AICompleted {
            val code: Code = theCode
            val domain: theDomain.type = theDomain
            val evaluated: List[Int] = theEvaluated
            val operandsArray: Array[List[theDomain.DomainValue]] = theOperandsArray
            val localsArray: Array[Array[theDomain.DomainValue]] = theLocalsArray

            def restartInterpretation(
                ai: AI[_ >: theDomain.type]): AIResult =
                ai.continueInterpretation(
                    code, domain)(
                        List(0), evaluated, operandsArray, localsArray)

        }
    }
}

/**
 * Encapsulates the result of the abstract interpretation of a method.
 */
sealed abstract class AIResult {
    val code: Code
    val domain: Domain
    val worklist: List[Int]
    val evaluated: List[Int]
    val operandsArray: Array[List[domain.DomainValue]]
    val localsArray: Array[Array[domain.DomainValue]]

    /**
     * Returns `true` if the abstract interpretation was aborted.
     */
    def wasAborted: Boolean

    /**
     * Textual representation of the state encapsulated by this result.
     */
    def stateToString: String = {
        var result = ""
        result += evaluated.mkString("Evaluated: ", ",", "\n")
        result += worklist.mkString("(Remaining) Worklist: ", ",", "\n")
        result +=
            (
                for {
                    ((operands, locals), pc) ← (operandsArray.zip(localsArray)).zipWithIndex
                    if operands != null /*|| locals != null*/
                } yield {
                    val localsWithIndex =
                        for {
                            (local, index) ← locals.zipWithIndex
                            if (local != null)
                        } yield {
                            "("+index+":"+local+")"
                        }

                    "PC: "+pc + operands.mkString("\n\tOperands: ", " <- ", "") + localsWithIndex.mkString("\n\tLocals: [", ",", "]")
                }
            ).mkString("Operands and Locals: \n", "\n", "\n")
        result
    }
}

/**
 * Encapsulates the intermediate result of an aborted abstract interpretation of a method.
 */
sealed abstract class AIAborted extends AIResult {

    def wasAborted: Boolean = true

    def continueInterpretation(ai: AI[_ >: domain.type]): AIResult

    override def stateToString: String = "Abstract Interpretation was aborted; "+super.stateToString
}

/**
 * Encapsulates the final result of the successful abstract interpretation of a method.
 */
sealed abstract class AICompleted extends AIResult {

    val worklist: List[Int] = List.empty

    def wasAborted: Boolean = false

    def restartInterpretation(ai: AI[_ >: domain.type]): AIResult

    override def stateToString: String = "Abstract Interpretation succeeded; "+super.stateToString
}
