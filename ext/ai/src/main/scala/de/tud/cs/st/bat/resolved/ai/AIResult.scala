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
 * Factory to creating `AIResult` objects.
 */
/* Design - We use a builder to construct a Result object in two steps. This is necessary
 * to correctly type the data structures that store the memory layout, which depends on the given domain. */
object AIResultBuilder {

    def aborted[D <: Domain](
        theCode: Code,
        theDomain: D): (List[Int], Array[List[theDomain.DomainValue]], Array[IndexedSeq[theDomain.DomainValue]]) ⇒ AIResult[theDomain.type] = {

        (theWorkList: List[Int], theOperandsArray: Array[List[theDomain.DomainValue]], theLocalsArray: Array[IndexedSeq[theDomain.DomainValue]]) ⇒
            new AIAborted[theDomain.type] {
                val code: Code = theCode
                val domain: theDomain.type = theDomain
                val operandsArray: Array[List[theDomain.DomainValue]] = theOperandsArray
                val localsArray: Array[IndexedSeq[theDomain.DomainValue]] = theLocalsArray
                val workList: List[Int] = theWorkList
                def continueInterpretation(): AIResult[domain.type] = {
                    AI.continueInterpretation(code, domain)(workList, operandsArray, localsArray)
                }
            }
    }

    def complete[D <: Domain](
        theCode: Code,
        theDomain: D): (Array[List[theDomain.DomainValue]], Array[IndexedSeq[theDomain.DomainValue]]) ⇒ AIResult[theDomain.type] = {

        (theOperandsArray: Array[List[theDomain.DomainValue]], theLocalsArray: Array[IndexedSeq[theDomain.DomainValue]]) ⇒
            new AICompleted[theDomain.type] {
                val code: Code = theCode
                val domain: theDomain.type = theDomain
                val operandsArray: Array[List[theDomain.DomainValue]] = theOperandsArray
                val localsArray: Array[IndexedSeq[theDomain.DomainValue]] = theLocalsArray
                def restartInterpretation(): AIResult[theDomain.type] = {
                    AI.continueInterpretation(code, domain)(workList, operandsArray, localsArray)
                }
            }
    }
}

/* Design - We use an explicit type parameter to avoid a path dependency on a concrete AIResult
 * instance. I.e., if we remove the type parameter and redefine the method BATErrors
 * to "memoryLayouts: IndexedSeq[MemoryLayout[domain.type, domain.DomainValue]]" 
 * we would introduce a path dependence to a particular AIResult's instance and the actual 
 * type would be "this.domain.type" and "this.domain.DomainValue". */
sealed abstract class AIResult[D <: Domain] {
    val code: Code
    val domain: D
    val operandsArray: Array[List[domain.DomainValue]]
    val localsArray: Array[IndexedSeq[domain.DomainValue]]
    val workList: List[Int]

    def wasAborted: Boolean

    type BoundAIResult = AIResult[domain.type]
}

abstract class AIAborted[D <: Domain] extends AIResult[D] {

    final def wasAborted: Boolean = true

    def continueInterpretation(): BoundAIResult
}

abstract class AICompleted[D <: Domain] extends AIResult[D] {

    final def wasAborted: Boolean = false

    val workList: List[Int] = List(0)

    def restartInterpretation(): BoundAIResult
}
