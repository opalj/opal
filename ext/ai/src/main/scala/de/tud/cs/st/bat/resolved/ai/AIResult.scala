/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
        theDomain: Domain[_])(
            theWorklist: List[Int],
            theEvaluated: List[Int],
            theOperandsArray: Array[List[theDomain.DomainValue]],
            theLocalsArray: Array[Array[theDomain.DomainValue]]): AIAborted[theDomain.type] = {

        new AIAborted[theDomain.type] {
            val code: Code = theCode
            val domain: theDomain.type = theDomain
            val worklist: List[Int] = theWorklist
            val evaluated: List[Int] = theEvaluated
            val operandsArray: Array[List[theDomain.DomainValue]] = theOperandsArray
            val localsArray: Array[Array[theDomain.DomainValue]] = theLocalsArray

            def continueInterpretation(
                ai: AI[_ >: domain.type]): AIResult[domain.type] =
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
        theDomain: Domain[_])(
            theEvaluated: List[Int],
            theOperandsArray: Array[List[theDomain.DomainValue]],
            theLocalsArray: Array[Array[theDomain.DomainValue]]): AICompleted[theDomain.type] = {

        new AICompleted[theDomain.type] {
            val code: Code = theCode
            val domain: theDomain.type = theDomain
            val evaluated: List[Int] = theEvaluated
            val operandsArray: Array[List[theDomain.DomainValue]] = theOperandsArray
            val localsArray: Array[Array[theDomain.DomainValue]] = theLocalsArray

            def restartInterpretation(
                ai: AI[_ >: theDomain.type]): AIResult[theDomain.type] =
                ai.continueInterpretation(
                    code, domain)(
                        List(0), evaluated, operandsArray, localsArray)

        }
    }
}

/**
 * Encapsulates the result of the abstract interpretation of a method.
 */
/* Design - We use an explicit type parameter to avoid a path dependency on a 
 * concrete AIResult instance. I.e., if we would remove the type parameter 
 * we would introduce a path dependence to a particular AIResult's instance and the actual 
 * type would be "this.domain.type" and "this.domain.DomainValue". */
sealed abstract class AIResult[D <: SomeDomain with Singleton] {
    val code: Code
    val domain: D
    val worklist: List[Int]
    val evaluated: List[Int]
    val operandsArray: Array[List[domain.DomainValue]]
    val localsArray: Array[Array[domain.DomainValue]]

    /**
     * Returns `true` if the abstract interpretation was aborted.
     */
    def wasAborted: Boolean
}

/**
 * Encapsulates the intermediate result of the abstract interpretation of a method.
 */
sealed abstract class AIAborted[D <: SomeDomain with Singleton] extends AIResult[D] {

    def wasAborted: Boolean = true

    def continueInterpretation(ai: AI[_ >: D]): AIResult[domain.type]
}

/**
 * Encapsulates the result of the abstract interpretation of a method.
 */
sealed abstract class AICompleted[D <: SomeDomain with Singleton] extends AIResult[D] {

    val worklist: List[Int] = List.empty

    def wasAborted: Boolean = false

    def restartInterpretation(ai: AI[_ >: D]): AIResult[domain.type]
}
